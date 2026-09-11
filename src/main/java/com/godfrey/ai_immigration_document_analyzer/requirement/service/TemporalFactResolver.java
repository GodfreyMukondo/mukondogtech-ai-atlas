package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactConflictResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConflict;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.service.DigitalTwinProjectionService;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationFactView;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * TEMPORAL FACT RESOLVER
 * ============================================================================
 *
 * Resolves the Fact data a Requirement/Pathway evaluation reads, for one
 * assessment date (Requirement/Pathway Architecture Specification, section
 * 13). {@code assessmentDate == null} means "now" - evaluate against the
 * live, authorized Digital Twin projection exactly as built (never
 * bypassed, never reimplemented). A non-null {@code assessmentDate} means a
 * point-in-time question ("was this person eligible on date X") that the
 * Digital Twin's contract (live/current only) does not cover, so this
 * reconstructs the answer directly from Fact temporal windows
 * (effectiveFrom/effectiveTo) via the EXISTING, unmodified
 * {@code FactRepository.findBySubjectUserId}.
 *
 * DELIBERATE ARCHITECTURAL DECISION: {@code DigitalTwinProjectionService}
 * was not modified to add an "as of" variant, since that would mean editing
 * already-committed Fact Foundation code without a genuine defect driving
 * it. This class is new, additive, and enforces the identical authorization
 * boundary itself ({@code FactAuthorizationService.assertCanView}) before
 * ever reading a Fact directly - the historical path never becomes a
 * second, less-guarded way to read someone's Facts.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class TemporalFactResolver {

    private static final Set<FactStatus> TEMPORALLY_VALID_STATUSES =
            EnumSet.of(FactStatus.ACCEPTED, FactStatus.SUPERSEDED, FactStatus.CONTESTED);

    private final FactRepository factRepository;
    private final FactConflictRepository factConflictRepository;
    private final DigitalTwinProjectionService digitalTwinProjectionService;
    private final FactAuthorizationService factAuthorizationService;

    @Transactional(readOnly = true)
    public EvaluationFactView resolve(AuthenticatedUser actor, Long subjectUserId, LocalDateTime assessmentDate) {

        if (assessmentDate == null) {
            return fromDigitalTwin(digitalTwinProjectionService.getDigitalTwin(actor, subjectUserId));
        }

        return fromHistoricalReconstruction(actor, subjectUserId, assessmentDate);
    }

    // =========================================================================
    // LIVE PATH - THE AUTHORIZED DIGITAL TWIN, UNCHANGED
    // =========================================================================

    private EvaluationFactView fromDigitalTwin(
            com.godfrey.ai_immigration_document_analyzer.fact.dto.DigitalTwinResponse twin
    ) {

        Map<String, List<FactResponse>> byKey = twin.currentFacts().stream()
                .collect(Collectors.groupingBy(FactResponse::factKey));

        Map<String, Long> conflictByKey = new HashMap<>();

        for (FactConflictResponse conflict : twin.openConflicts()) {
            conflictByKey.put(conflict.factKey(), conflict.id());
        }

        return new EvaluationFactView(byKey, conflictByKey);
    }

    // =========================================================================
    // HISTORICAL PATH - POINT-IN-TIME RECONSTRUCTION FROM FACT WINDOWS
    // =========================================================================

    private EvaluationFactView fromHistoricalReconstruction(
            AuthenticatedUser actor,
            Long subjectUserId,
            LocalDateTime assessmentDate
    ) {

        factAuthorizationService.assertCanView(actor, subjectUserId, null, null, "temporal fact reconstruction");

        List<Fact> allFacts = factRepository.findBySubjectUserId(subjectUserId);

        Map<String, List<Fact>> factsAsOfByKey = allFacts.stream()
                .filter(fact -> TEMPORALLY_VALID_STATUSES.contains(fact.getStatus()))
                .filter(fact -> coversDate(fact, assessmentDate))
                .collect(Collectors.groupingBy(Fact::getFactKey));

        Map<String, List<FactResponse>> acceptedByKey = new HashMap<>();
        Map<String, Long> conflictByKey = new HashMap<>();

        for (Map.Entry<String, List<Fact>> entry : factsAsOfByKey.entrySet()) {

            String factKey = entry.getKey();
            List<Fact> facts = entry.getValue();

            List<Fact> contested = facts.stream().filter(f -> f.getStatus() == FactStatus.CONTESTED).toList();

            if (contested.size() >= 2) {

                Long conflictId = findConflictId(contested);

                if (conflictId != null) {
                    conflictByKey.put(factKey, conflictId);
                    continue;
                }
            }

            List<Fact> usable = facts.stream().filter(f -> f.getStatus() != FactStatus.CONTESTED).toList();

            if (!usable.isEmpty()) {
                acceptedByKey.put(factKey, usable.stream().map(f -> FactResponse.from(f, List.of())).toList());
            }
        }

        return new EvaluationFactView(acceptedByKey, conflictByKey);
    }

    /**
     * Whether {@code fact} was the valid claim as of {@code assessmentDate},
     * using its temporal window. Falls back to {@code observedAt} (never
     * null on a Fact) when {@code effectiveFrom} was not set - the same
     * fallback {@code FactConflictService} already uses for succession
     * detection.
     */
    private boolean coversDate(Fact fact, LocalDateTime assessmentDate) {

        LocalDateTime from = fact.getEffectiveFrom() != null ? fact.getEffectiveFrom() : fact.getObservedAt();
        LocalDateTime to = fact.getEffectiveTo();

        boolean afterStart = from == null || !assessmentDate.isBefore(from);
        boolean beforeEnd = to == null || assessmentDate.isBefore(to);

        return afterStart && beforeEnd;
    }

    private Long findConflictId(List<Fact> contestedFacts) {

        for (Fact fact : contestedFacts) {

            List<FactConflict> matches = factConflictRepository.findByFactAIdOrFactBId(fact.getId(), fact.getId());

            if (!matches.isEmpty()) {
                return matches.get(0).getId();
            }
        }

        return null;
    }

    // =========================================================================
    // HYPOTHETICAL OVERLAY - Scenario Simulation (Phase 5.5), ADDITIVE ONLY
    // =========================================================================

    /**
     * Builds a NEW, in-memory-only {@link EvaluationFactView} by overlaying
     * hypothetical values on top of an already-resolved REAL view - never a
     * second Fact-resolution path, never a database write, never a change to
     * {@link #resolve}. The real {@code EvaluationFactView} passed in is
     * itself untouched (records are immutable); this method returns a
     * distinct object.
     *
     * Every entry in {@code hypotheticalFacts} MUST already carry {@code
     * FactProvenanceType.SIMULATION} (enforced by the caller - see {@code
     * ScenarioSimulationService}) - this method does not itself construct or
     * validate hypothetical values, it only merges already-built ones into a
     * view shape the existing evaluation engine already understands.
     *
     * A hypothetical value for a factKey that currently has an open
     * conflict deliberately SUPERSEDES that conflict for evaluation
     * purposes only - the user is explicitly telling the evaluator to
     * assume this value, so a simulated CONFLICTED outcome would just
     * restate a known real-case issue rather than answer "what if." The
     * real {@code FactConflict} row is never read, resolved, or otherwise
     * touched by this method.
     */
    public EvaluationFactView overlayHypothetical(EvaluationFactView real, List<FactResponse> hypotheticalFacts) {

        Map<String, List<FactResponse>> merged = new HashMap<>(real.acceptedFactsByKey());
        Map<String, Long> conflicts = new HashMap<>(real.openConflictIdByFactKey());

        for (FactResponse hypothetical : hypotheticalFacts) {
            merged.put(hypothetical.factKey(), List.of(hypothetical));
            conflicts.remove(hypothetical.factKey());
        }

        return new EvaluationFactView(Map.copyOf(merged), Map.copyOf(conflicts));
    }
}
