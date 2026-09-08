package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.ConflictResolutionType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.ConflictStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConflict;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.TimelineEventType;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.ConflictPolicyRegistry;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.ConflictResolutionClass;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactCardinality;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactTypeDefinition;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactTypeRegistry;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * ============================================================================
 * FACT CONFLICT SERVICE
 * ============================================================================
 *
 * Implements the approved field-level Conflict Auto-Resolution Policy.
 *
 * CRITICAL, non-negotiable invariant: a conflict produces CONFLICT DETECTED,
 * never FRAUD DETECTED. Nothing in this service ever sets a fraud-like
 * label - a losing fact in a resolved conflict is marked REJECTED (lost
 * this specific comparison) and permanently retained, never deleted and
 * never characterized as false or fraudulent.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FactConflictService {

    private final FactRepository factRepository;
    private final FactConflictRepository conflictRepository;
    private final FactEvidenceRepository evidenceRepository;
    private final FactTimelineService timelineService;
    private final FactLifecycleService lifecycleService;
    private final ConfidenceCalculator confidenceCalculator;
    private final NotificationService notificationService;

    /**
     * Called once a newly proposed Fact has passed validation and its
     * Evidence has been persisted. Decides whether it can be accepted
     * outright, is a routine temporal succession, or is a genuine conflict
     * requiring resolution.
     */
    @Transactional
    public Fact processIncomingFact(Fact incoming) {

        FactTypeDefinition definition = FactTypeRegistry.get(incoming.getFactKey());

        if (definition.cardinality() == FactCardinality.PERMANENTLY_MULTI_VALUED) {

            return lifecycleService.transition(
                    incoming,
                    FactStatus.ACCEPTED,
                    incoming.getCreatedByAccessorType(),
                    incoming.getCreatedByUserId(),
                    TimelineEventType.FACT_CREATED,
                    "Permanently multi-valued fact type - no supersession or conflict applies."
            );
        }

        Optional<Fact> existingOpt = findComparableExisting(incoming, definition);

        if (existingOpt.isEmpty()) {

            return lifecycleService.transition(
                    incoming,
                    FactStatus.ACCEPTED,
                    incoming.getCreatedByAccessorType(),
                    incoming.getCreatedByUserId(),
                    TimelineEventType.FACT_CREATED,
                    "No existing fact of this key for this subject."
            );
        }

        Fact existing = existingOpt.get();

        if (definition.cardinality() == FactCardinality.HISTORICAL_MULTI_VALUED
                && isNonOverlappingSuccession(existing, incoming)) {

            lifecycleService.supersede(existing, incoming, incoming.getCreatedByAccessorType(), incoming.getCreatedByUserId());

            return lifecycleService.transition(
                    incoming,
                    FactStatus.ACCEPTED,
                    incoming.getCreatedByAccessorType(),
                    incoming.getCreatedByUserId(),
                    TimelineEventType.FACT_CREATED,
                    "Supersedes fact " + existing.getId() + " (non-overlapping period)."
            );
        }

        return handleConflict(existing, incoming, definition);
    }

    // =========================================================================
    // CONFLICT HANDLING
    // =========================================================================

    private Fact handleConflict(Fact existing, Fact incoming, FactTypeDefinition definition) {

        ConflictResolutionClass policyClass =
                ConflictPolicyRegistry.resolutionClassFor(definition.category(), incoming.getFactKey());

        Fact contestedExisting = lifecycleService.transition(
                existing,
                FactStatus.CONTESTED,
                AccessorType.SYSTEM,
                null,
                TimelineEventType.FACT_CONFLICTED,
                "Conflicting fact " + incoming.getId() + " received."
        );

        Fact contestedIncoming = lifecycleService.transition(
                incoming,
                FactStatus.CONTESTED,
                incoming.getCreatedByAccessorType(),
                incoming.getCreatedByUserId(),
                TimelineEventType.FACT_CONFLICTED,
                "Conflicts with existing fact " + existing.getId() + "."
        );

        FactConflict conflict = FactConflict.builder()
                .subjectUserId(incoming.getSubjectUserId())
                .factKey(incoming.getFactKey())
                .factAId(contestedExisting.getId())
                .factBId(contestedIncoming.getId())
                .status(ConflictStatus.OPEN)
                .build();

        conflict = conflictRepository.save(conflict);

        timelineService.record(
                incoming.getSubjectUserId(),
                TimelineEventType.FACT_CONFLICTED,
                null,
                conflict.getId(),
                AccessorType.SYSTEM,
                null,
                null,
                ConflictStatus.OPEN.name(),
                "Conflict opened between facts " + contestedExisting.getId() + " and " + contestedIncoming.getId()
        );

        boolean autoResolved = attemptAutoResolve(conflict, contestedExisting, contestedIncoming, policyClass);

        if (!autoResolved) {

            notificationService.notify(
                    incoming.getSubjectUserId(),
                    "FACT_CONFLICT_DETECTED",
                    "We found conflicting information",
                    "We found two different values for " + incoming.getFactKey()
                            + ". Please review and confirm which is correct, or provide additional evidence.",
                    "/dashboard/facts/conflicts/" + conflict.getId()
            );

            return factRepository.findById(contestedIncoming.getId()).orElseThrow();
        }

        return factRepository.findById(contestedIncoming.getId()).orElseThrow();
    }

    /**
     * @return true if the conflict was auto-resolved, false if it must fall
     * back to human review (either because the policy class requires it, or
     * because the confidence-floor/minimum-gap check was not met).
     */
    private boolean attemptAutoResolve(
            FactConflict conflict,
            Fact factA,
            Fact factB,
            ConflictResolutionClass policyClass
    ) {

        if (policyClass == ConflictResolutionClass.ALWAYS_HUMAN_RESOLUTION
                || policyClass == ConflictResolutionClass.NEVER_AUTO_RESOLVE
                || policyClass == ConflictResolutionClass.NOT_APPLICABLE) {

            return false;
        }

        Fact winner;
        Fact loser;
        ConflictResolutionType resolutionType;

        if (policyClass == ConflictResolutionClass.SAFE_AUTO_RESOLUTION_ALLOWED) {

            // Low materiality: most recently recorded value wins without requiring a confidence gap.
            winner = factB.getRecordedAt().isAfter(factA.getRecordedAt()) ? factB : factA;
            loser = winner == factA ? factB : factA;
            resolutionType = ConflictResolutionType.SAFE_AUTO;

        } else {

            double scoreA = rawConfidenceIgnoringContested(factA);
            double scoreB = rawConfidenceIgnoringContested(factB);

            double stronger = Math.max(scoreA, scoreB);
            double gap = Math.abs(scoreA - scoreB);

            if (stronger < ConflictPolicyRegistry.AUTO_RESOLUTION_CONFIDENCE_FLOOR
                    || gap < ConflictPolicyRegistry.AUTO_RESOLUTION_MIN_CONFIDENCE_GAP) {

                // Evidence comparably weighted (or both weak) - falls back to human review
                // even though the field's default policy permits auto-resolution.
                return false;
            }

            winner = scoreA >= scoreB ? factA : factB;
            loser = winner == factA ? factB : factA;

            resolutionType = policyClass == ConflictResolutionClass.TEMPORAL_AUTO_RESOLUTION_ALLOWED
                    ? ConflictResolutionType.TEMPORAL_AUTO
                    : ConflictResolutionType.EVIDENCE_WEIGHTED_AUTO;
        }

        lifecycleService.transition(
                winner,
                FactStatus.ACCEPTED,
                AccessorType.SYSTEM,
                null,
                TimelineEventType.FACT_RESOLVED,
                "Auto-resolved (" + resolutionType + ") in favor of fact " + winner.getId()
        );

        lifecycleService.transition(
                loser,
                FactStatus.REJECTED,
                AccessorType.SYSTEM,
                null,
                TimelineEventType.FACT_RESOLVED,
                "Lost automatic resolution (" + resolutionType + ") to fact " + winner.getId()
                        + ". CONFLICT DETECTED - this is not a fraud determination."
        );

        conflict.setStatus(ConflictStatus.RESOLVED);
        conflict.setResolvedAt(LocalDateTime.now());
        conflict.setResolutionType(resolutionType);
        conflict.setWinningFactId(winner.getId());
        conflictRepository.save(conflict);

        timelineService.record(
                winner.getSubjectUserId(),
                TimelineEventType.FACT_RESOLVED,
                null,
                conflict.getId(),
                AccessorType.SYSTEM,
                null,
                ConflictStatus.OPEN.name(),
                ConflictStatus.RESOLVED.name(),
                "Automatically resolved via " + resolutionType
        );

        boolean prominent = resolutionType == ConflictResolutionType.EVIDENCE_WEIGHTED_AUTO;

        notificationService.notify(
                winner.getSubjectUserId(),
                "FACT_AUTO_RESOLVED",
                prominent ? "We resolved conflicting information on your case" : "We updated your information",
                "Based on "
                        + (resolutionType == ConflictResolutionType.SAFE_AUTO ? "your most recent update" : "the available evidence")
                        + ", we've updated " + winner.getFactKey()
                        + ". Let us know if this is incorrect.",
                "/dashboard/facts/" + winner.getId()
        );

        return true;
    }

    // =========================================================================
    // MANUAL (HUMAN) RESOLUTION
    // =========================================================================

    @Transactional
    public FactConflict resolveManually(
            FactConflict conflict,
            Long winningFactId,
            Long resolvedByUserId,
            String notes
    ) {

        if (conflict.getStatus() != ConflictStatus.OPEN) {

            throw new IllegalStateException("Conflict " + conflict.getId() + " is not open.");
        }

        if (!winningFactId.equals(conflict.getFactAId()) && !winningFactId.equals(conflict.getFactBId())) {

            throw new IllegalArgumentException(
                    "Winning fact must be one of the two conflicting facts."
            );
        }

        Long losingFactId = winningFactId.equals(conflict.getFactAId())
                ? conflict.getFactBId()
                : conflict.getFactAId();

        Fact winner = factRepository.findById(winningFactId).orElseThrow();
        Fact loser = factRepository.findById(losingFactId).orElseThrow();

        lifecycleService.transition(
                winner,
                FactStatus.ACCEPTED,
                AccessorType.CASE_WORKER,
                resolvedByUserId,
                TimelineEventType.FACT_RESOLVED,
                notes
        );

        lifecycleService.transition(
                loser,
                FactStatus.REJECTED,
                AccessorType.CASE_WORKER,
                resolvedByUserId,
                TimelineEventType.FACT_RESOLVED,
                "Lost human resolution to fact " + winner.getId()
                        + ". CONFLICT DETECTED - this is not a fraud determination. " + notes
        );

        conflict.setStatus(ConflictStatus.RESOLVED);
        conflict.setResolvedAt(LocalDateTime.now());
        conflict.setResolutionType(ConflictResolutionType.HUMAN_DECISION);
        conflict.setResolvedByUserId(resolvedByUserId);
        conflict.setWinningFactId(winningFactId);
        conflict.setResolutionNotes(notes);

        FactConflict saved = conflictRepository.save(conflict);

        timelineService.record(
                winner.getSubjectUserId(),
                TimelineEventType.FACT_RESOLVED,
                null,
                conflict.getId(),
                AccessorType.CASE_WORKER,
                resolvedByUserId,
                ConflictStatus.OPEN.name(),
                ConflictStatus.RESOLVED.name(),
                notes
        );

        return saved;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Optional<Fact> findComparableExisting(Fact incoming, FactTypeDefinition definition) {

        if (definition.cardinality() == FactCardinality.SINGLE_CURRENT) {

            return factRepository.findBySubjectUserIdAndFactKeyAndStatus(
                            incoming.getSubjectUserId(),
                            incoming.getFactKey(),
                            FactStatus.ACCEPTED
                    )
                    .stream()
                    .findFirst();
        }

        // HISTORICAL_MULTI_VALUED
        return factRepository.findBySubjectUserIdAndFactKeyAndStatusAndEffectiveToIsNull(
                incoming.getSubjectUserId(),
                incoming.getFactKey(),
                FactStatus.ACCEPTED
        );
    }

    /**
     * Two Historical-Multi-Valued facts are a temporal succession (not a
     * conflict) when the incoming fact's window starts at or after the
     * existing fact's window started - the common "new employer" pattern.
     * If the incoming fact's start precedes the existing one's, both claim
     * the same period and it is treated as a genuine conflict instead.
     */
    private boolean isNonOverlappingSuccession(Fact existing, Fact incoming) {

        LocalDateTime incomingStart = incoming.getEffectiveFrom() != null
                ? incoming.getEffectiveFrom()
                : incoming.getObservedAt();

        LocalDateTime existingStart = existing.getEffectiveFrom() != null
                ? existing.getEffectiveFrom()
                : existing.getRecordedAt();

        return incomingStart != null && existingStart != null && !incomingStart.isBefore(existingStart);
    }

    private double rawConfidenceIgnoringContested(Fact fact) {

        long evidenceCount = evidenceRepository.countByFactId(fact.getId());

        long daysSinceLastObserved = ChronoUnit.DAYS.between(fact.getLastObservedAt(), LocalDateTime.now());

        FactTypeDefinition definition = FactTypeRegistry.get(fact.getFactKey());

        ConfidenceCalculator.Result result = confidenceCalculator.compute(
                new ConfidenceCalculator.Input(
                        fact.getProvenanceType(),
                        (int) Math.max(evidenceCount, 1),
                        Boolean.TRUE.equals(fact.getIsVerified()),
                        fact.getVerificationMethod(),
                        false, // ignore the contested cap for this comparison
                        daysSinceLastObserved,
                        definition.stalenessDays()
                )
        );

        return result.score();
    }
}
