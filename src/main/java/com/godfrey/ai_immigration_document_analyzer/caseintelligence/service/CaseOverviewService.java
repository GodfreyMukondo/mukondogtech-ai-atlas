package com.godfrey.ai_immigration_document_analyzer.caseintelligence.service;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseContradictionsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRiskBand;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseSignalsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseTimelineEventResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseTimelineResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.PotentialOverlapContradiction;
import com.godfrey.ai_immigration_document_analyzer.entity.Document;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.DocumentVersion;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItem;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItemStatus;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.DocumentVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.DigitalTwinResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactCardinality;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactTypeDefinition;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactTypeRegistry;
import com.godfrey.ai_immigration_document_analyzer.fact.service.DigitalTwinProjectionService;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * CASE OVERVIEW SERVICE
 * ============================================================================
 *
 * Case-wide (subjectUserId-scoped, not tied to any one PathwayAssessment)
 * views: cross-document contradictions, immigration timeline intelligence,
 * and evidence/anomaly signals (Master Platform Expansion sections 3-6).
 *
 * "Case" is never a new entity here - it is the same subjectUserId the
 * Digital Twin and every Fact/PathwayAssessment query already use. Every
 * method below is a pure, read-time projection: nothing is persisted, and
 * nothing here performs Fact-conflict detection or fraud adjudication of
 * its own - it only surfaces what {@code FactConflictService} and the
 * legacy {@code DocumentService.analyzeFraud} indicator already recorded,
 * plus one additive, read-time-only overlap check (see
 * {@link #computeTimeline}) that fills a real gap neither of those cover:
 * ingestion-time conflict detection only ever compares an incoming Fact
 * against the single currently open-ended Fact of the same key, never
 * every pair of a subject's already-closed historical Facts against each
 * other.
 *
 * PATHWAY-AGNOSTIC: nothing here reads a Requirement, Pathway, or
 * regulatory content at all - this class only ever reasons about Facts,
 * Documents, and Evidence Items already scoped to one subjectUserId.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class CaseOverviewService {

    private static final List<FactCategory> TIMELINE_CATEGORIES = List.of(
            FactCategory.EMPLOYMENT,
            FactCategory.EDUCATION,
            FactCategory.RESIDENCE,
            FactCategory.TRAVEL_HISTORY,
            FactCategory.IMMIGRATION_STATUS,
            FactCategory.IMMIGRATION_HISTORY
    );

    private final DigitalTwinProjectionService digitalTwinProjectionService;
    private final FactAuthorizationService factAuthorizationService;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final EvidenceItemRepository evidenceItemRepository;

    // =========================================================================
    // CONTRADICTIONS
    // =========================================================================

    @Transactional(readOnly = true)
    public CaseContradictionsResponse getContradictions(AuthenticatedUser actor, Long subjectUserId) {

        DigitalTwinResponse twin = digitalTwinProjectionService.getDigitalTwin(actor, subjectUserId);

        TimelineComputation computation = computeTimeline(twin.currentFacts());

        return new CaseContradictionsResponse(
                subjectUserId,
                twin.openConflicts(),
                computation.overlaps(),
                LocalDateTime.now(),
                "\"openContradictions\" are CONFLICT DETECTED records between two pieces of information for "
                        + "this case; \"potentialOverlaps\" are two of your own records claiming overlapping time "
                        + "periods. Both require verification - neither is a finding of fraud."
        );
    }

    // =========================================================================
    // TIMELINE
    // =========================================================================

    @Transactional(readOnly = true)
    public CaseTimelineResponse getTimeline(AuthenticatedUser actor, Long subjectUserId) {

        DigitalTwinResponse twin = digitalTwinProjectionService.getDigitalTwin(actor, subjectUserId);

        TimelineComputation computation = computeTimeline(twin.currentFacts());

        return new CaseTimelineResponse(
                subjectUserId,
                computation.events(),
                LocalDateTime.now(),
                "A gap or overlap is a plain day count between successive records of the same kind of fact - "
                        + "it carries no legal significance on its own and is never presented as an unexplained "
                        + "absence or a finding of fraud."
        );
    }

    /**
     * Shared, pathway-agnostic core: builds the chronological timeline AND
     * detects period overlaps in one pass, so both public methods above
     * report identical figures rather than two independently-drifting
     * computations of the same thing.
     */
    private record TimelineComputation(List<CaseTimelineEventResponse> events, List<PotentialOverlapContradiction> overlaps) {
    }

    private TimelineComputation computeTimeline(List<FactResponse> currentFacts) {

        List<FactResponse> timelineFacts = currentFacts.stream()
                .filter(fact -> TIMELINE_CATEGORIES.contains(fact.category()))
                .filter(fact -> fact.effectiveFrom() != null)
                .sorted(Comparator.comparing(FactResponse::effectiveFrom))
                .toList();

        Map<String, FactResponse> lastFactByKey = new HashMap<>();
        List<CaseTimelineEventResponse> events = new ArrayList<>();
        List<PotentialOverlapContradiction> overlaps = new ArrayList<>();

        for (FactResponse fact : timelineFacts) {

            FactResponse previous = lastFactByKey.get(fact.factKey());
            Long gapDays = null;
            Long overlapDays = null;

            if (previous != null && previous.effectiveTo() != null && isHistoricalMultiValued(fact.factKey())) {

                long days = Duration.between(previous.effectiveTo(), fact.effectiveFrom()).toDays();

                if (days > 0) {
                    gapDays = days;
                } else if (days < 0) {

                    overlapDays = -days;

                    overlaps.add(new PotentialOverlapContradiction(
                            fact.factKey(),
                            previous.id(), previous.effectiveFrom(), previous.effectiveTo(),
                            fact.id(), fact.effectiveFrom(), fact.effectiveTo(),
                            overlapDays,
                            "Two " + fact.factKey() + " records claim overlapping time - a potential "
                                    + "contradiction that requires verification, not a finding of fraud."
                    ));
                }
            }

            events.add(new CaseTimelineEventResponse(fact, gapDays, overlapDays));
            lastFactByKey.put(fact.factKey(), fact);
        }

        return new TimelineComputation(events, overlaps);
    }

    /**
     * {@code FactTypeRegistry.get} throws for a key it does not recognise -
     * checked with {@code isKnown} first so an unregistered factKey is
     * simply treated as "not eligible for overlap detection" rather than
     * crashing this entire read-time projection.
     */
    private boolean isHistoricalMultiValued(String factKey) {

        if (!FactTypeRegistry.isKnown(factKey)) {
            return false;
        }

        FactTypeDefinition definition = FactTypeRegistry.get(factKey);
        return definition.cardinality() == FactCardinality.HISTORICAL_MULTI_VALUED;
    }

    // =========================================================================
    // EVIDENCE / ANOMALY SIGNALS
    // =========================================================================

    /**
     * Aggregates existing, already non-forensic indicators into one
     * transparent band. NEVER a fraud determination - an elevated band
     * means "a case worker should look closer", nothing more. The
     * thresholds below are INTERNAL MukondoGTech AI heuristics only, never
     * represented as government-defined or immigration-law thresholds.
     */
    @Transactional(readOnly = true)
    public CaseSignalsResponse getSignals(AuthenticatedUser actor, Long subjectUserId) {

        factAuthorizationService.assertCanView(actor, subjectUserId, null, null, "view case evidence signals");

        List<Document> documents = documentRepository.findByUserIdOrderByUploadedAtDesc(subjectUserId);

        long fraudFlagged = documents.stream().filter(d -> Boolean.TRUE.equals(d.getFraudDetected())).count();
        long highRisk = documents.stream().filter(d -> "HIGH".equalsIgnoreCase(d.getRiskLevel())).count();
        long mediumRisk = documents.stream().filter(d -> "MEDIUM".equalsIgnoreCase(d.getRiskLevel())).count();

        DigitalTwinResponse twin = digitalTwinProjectionService.getDigitalTwin(actor, subjectUserId);
        long openConflicts = twin.openConflicts().size();
        long overlapCount = computeTimeline(twin.currentFacts()).overlaps().size();

        long rejectedEvidence = 0;
        long validationFailedEvidence = 0;

        for (Document document : documents) {
            for (DocumentVersion version : documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(document.getId())) {
                for (EvidenceItem item : evidenceItemRepository.findByDocumentVersionId(version.getId())) {
                    if (item.getStatus() == EvidenceItemStatus.REJECTED) {
                        rejectedEvidence++;
                    } else if (item.getStatus() == EvidenceItemStatus.VALIDATION_FAILED) {
                        validationFailedEvidence++;
                    }
                }
            }
        }

        CaseRiskBand band = computeRiskBand(fraudFlagged, highRisk, mediumRisk, openConflicts, overlapCount, rejectedEvidence, validationFailedEvidence);

        return new CaseSignalsResponse(
                subjectUserId,
                band,
                fraudFlagged,
                highRisk,
                mediumRisk,
                openConflicts,
                overlapCount,
                rejectedEvidence,
                validationFailedEvidence,
                LocalDateTime.now(),
                "These are non-forensic indicators only, aggregated from existing legacy risk flags, open "
                        + "conflicts, potential overlaps, and rejected evidence. An elevated band is never a "
                        + "fraud determination and never proves an anomaly - it means a case worker should look "
                        + "closer. These bands are internal MukondoGTech AI heuristics, not government-defined "
                        + "thresholds."
        );
    }

    /**
     * ============================================================================
     * RISK BAND RULES (internal heuristic only - see class Javadoc)
     * ============================================================================
     * CRITICAL: 2 or more documents flagged by the legacy fraud indicator.
     * HIGH:     any fraud-flagged or high-risk document, or any rejected
     *           evidence item.
     * MEDIUM:   any medium-risk document, any open contradiction, or any
     *           potential overlap.
     * LOW:      any evidence item that failed validation, with nothing
     *           above triggered.
     * NORMAL:   none of the above.
     *
     * Evaluated top-down; the first matching band wins. None of these
     * thresholds are defined by, or attributed to, any immigration
     * authority.
     * ============================================================================
     */
    private CaseRiskBand computeRiskBand(
            long fraudFlagged,
            long highRisk,
            long mediumRisk,
            long openConflicts,
            long overlapCount,
            long rejectedEvidence,
            long validationFailedEvidence
    ) {

        if (fraudFlagged >= 2) {
            return CaseRiskBand.CRITICAL;
        }

        if (fraudFlagged >= 1 || highRisk >= 1 || rejectedEvidence >= 1) {
            return CaseRiskBand.HIGH;
        }

        if (mediumRisk >= 1 || openConflicts >= 1 || overlapCount >= 1) {
            return CaseRiskBand.MEDIUM;
        }

        if (validationFailedEvidence >= 1) {
            return CaseRiskBand.LOW;
        }

        return CaseRiskBand.NORMAL;
    }
}
