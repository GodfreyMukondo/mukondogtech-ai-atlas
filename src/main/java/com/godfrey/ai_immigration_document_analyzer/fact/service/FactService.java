package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.DocumentVersion;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItem;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItemStatus;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.DocumentVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.service.EvidenceItemLifecycleService;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.ConflictResolutionRequest;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.DigitalTwinResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactConflictResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactEvidenceResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactProposalRequest;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactTimelineEntryResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactVerificationRequest;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.EvidenceSourceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConflict;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactEvidence;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactTypeDefinition;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactTypeRegistry;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactTimelineEventRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ============================================================================
 * FACT SERVICE
 * ============================================================================
 *
 * The single orchestration entry point for the Fact foundation. Every
 * public method here:
 *
 *   1. Resolves the fact type definition from {@code FactTypeRegistry}.
 *   2. Enforces authorization via {@code FactAuthorizationService} BEFORE
 *      any repository read/write.
 *   3. Delegates lifecycle/conflict decisions to
 *      {@code FactLifecycleService}/{@code FactConflictService} - this
 *      class never sets a Fact's status directly.
 *
 * No method here accepts a raw status from the caller - see
 * {@code FactLifecycleService} for why ACCEPTED/VERIFIED can never be
 * reached directly.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class FactService {

    private static final Set<FactProvenanceType> USER_FACING_ALLOWED_PROVENANCE = EnumSet.of(
            FactProvenanceType.USER_INPUT,
            FactProvenanceType.HUMAN_VERIFICATION,
            FactProvenanceType.DOCUMENT_EXTRACTION
    );

    private final FactRepository factRepository;
    private final FactEvidenceRepository evidenceRepository;
    private final FactConflictRepository conflictRepository;
    private final FactTimelineEventRepository timelineEventRepository;

    private final FactAuthorizationService authorizationService;
    private final FactConflictService factConflictService;
    private final FactLifecycleService lifecycleService;
    private final ConfidenceCalculator confidenceCalculator;

    /**
     * Evidence Intelligence Graph enrichment link (Phase 4 gap closure) -
     * resolves the optional, already-populated {@code EvidenceItem} for a
     * proposed Fact's source document, if one exists, and completes its
     * lifecycle when the Fact it supports is accepted. Never a second
     * Fact/Evidence linkage mechanism - {@code FactEvidence} remains the one
     * Document&lt;-&gt;Fact link; this only wires its optional
     * {@code evidenceItemId} enrichment now that a write path produces it.
     */
    private final DocumentVersionRepository documentVersionRepository;
    private final EvidenceItemRepository evidenceItemRepository;
    private final EvidenceItemLifecycleService evidenceItemLifecycleService;

    // =========================================================================
    // PROPOSE
    // =========================================================================

    @Transactional
    public FactResponse proposeFact(AuthenticatedUser actor, FactProposalRequest request) {

        FactTypeDefinition definition = resolveDefinition(request);

        authorizationService.assertCanCreate(
                actor,
                request.getSubjectUserId(),
                definition.sensitivityTier(),
                "propose fact"
        );

        validateProvenance(request.getProvenanceType());
        validateValueShape(request);

        AccessorType createdBy = actor.getUserId().equals(request.getSubjectUserId())
                ? AccessorType.SUBJECT
                : AccessorType.CASE_WORKER;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime observedAt = request.getObservedAt() != null ? request.getObservedAt() : now;

        int initialEvidenceCount = request.getSourceDocumentId() != null ? 1 : 0;

        ConfidenceCalculator.Result confidence = confidenceCalculator.compute(
                new ConfidenceCalculator.Input(
                        request.getProvenanceType(),
                        Math.max(initialEvidenceCount, 1),
                        false,
                        null,
                        false,
                        0L,
                        definition.stalenessDays()
                )
        );

        Fact fact = Fact.builder()
                .subjectUserId(request.getSubjectUserId())
                .category(definition.category())
                .factKey(definition.factKey())
                .valueType(request.getValueType())
                .stringValue(request.getStringValue())
                .dateValue(request.getDateValue())
                .numberValue(request.getNumberValue())
                .booleanValue(request.getBooleanValue())
                .status(FactStatus.PROPOSED)
                .provenanceType(request.getProvenanceType())
                .createdByAccessorType(createdBy)
                .createdByUserId(actor.getUserId())
                .sourceDocumentId(request.getSourceDocumentId())
                .sourceLocator(request.getSourceLocator())
                .sourceSnippet(truncate(request.getSourceSnippet(), 500))
                .sourceDescription(request.getSourceDescription())
                .sensitivityTier(definition.sensitivityTier())
                .confidenceScore(confidence.score())
                .confidenceLevel(confidence.level())
                .confidenceExplanation(confidence.explanation())
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(null)
                .observedAt(observedAt)
                .lastObservedAt(observedAt)
                .build();

        Fact saved = factRepository.save(fact);

        EvidenceItem linkableEvidenceItem = null;

        if (request.getSourceDocumentId() != null) {

            linkableEvidenceItem = findLinkableEvidenceItem(request.getSourceDocumentId());

            evidenceRepository.save(
                    FactEvidence.builder()
                            .factId(saved.getId())
                            .sourceType(EvidenceSourceType.DOCUMENT)
                            .documentId(request.getSourceDocumentId())
                            .sourceLocator(request.getSourceLocator())
                            .sourceSnippet(truncate(request.getSourceSnippet(), 500))
                            .evidenceItemId(linkableEvidenceItem != null ? linkableEvidenceItem.getId() : null)
                            .build()
            );
        }

        Fact finalState = factConflictService.processIncomingFact(saved);

        if (linkableEvidenceItem != null
                && linkableEvidenceItem.getStatus() == EvidenceItemStatus.CANDIDATE
                && finalState.getStatus() == FactStatus.ACCEPTED) {

            // The one place CANDIDATE -> LINKED is reached - as a side
            // effect of the Fact it supports being accepted, exactly as
            // EvidenceItemLifecycleService's own contract requires. Never a
            // directly callable transition.
            evidenceItemLifecycleService.transition(linkableEvidenceItem, EvidenceItemStatus.LINKED);
        }

        return toResponse(finalState);
    }

    /**
     * The Evidence Intelligence Graph's {@code EvidenceItem} for this
     * document, if the ingestion write path has already produced one and it
     * is still a genuine candidate (or already linked by an earlier Fact
     * sharing the same document). Never resolves a VALIDATION_FAILED/
     * REJECTED/SUPERSEDED item - linking one of those would misrepresent
     * evidence the system has already determined is not usable. Returns
     * {@code null} when none exists yet, in which case {@code FactEvidence}
     * is still recorded exactly as before - graceful degradation, not a
     * required dependency.
     */
    private EvidenceItem findLinkableEvidenceItem(Long documentId) {

        Optional<DocumentVersion> latestVersion =
                documentVersionRepository.findFirstByDocumentIdOrderByVersionNumberDesc(documentId);

        if (latestVersion.isEmpty()) {
            return null;
        }

        return evidenceItemRepository.findByDocumentVersionId(latestVersion.get().getId())
                .stream()
                .filter(item -> item.getStatus() == EvidenceItemStatus.CANDIDATE
                        || item.getStatus() == EvidenceItemStatus.LINKED)
                .findFirst()
                .orElse(null);
    }

    // =========================================================================
    // RETRIEVE
    // =========================================================================

    @Transactional(readOnly = true)
    public FactResponse getFact(AuthenticatedUser actor, Long factId) {

        Fact fact = requireFact(factId);

        authorizationService.assertCanView(
                actor,
                fact.getSubjectUserId(),
                fact.getSensitivityTier(),
                fact.getId(),
                "view fact"
        );

        return toResponse(fact);
    }

    @Transactional(readOnly = true)
    public List<FactResponse> listFactsForSubject(AuthenticatedUser actor, Long subjectUserId) {

        authorizationService.assertCanView(actor, subjectUserId, null, null, "list facts");

        return factRepository.findBySubjectUserId(subjectUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================================
    // VERIFY
    // =========================================================================

    @Transactional
    public FactResponse verifyFact(AuthenticatedUser actor, Long factId, FactVerificationRequest request) {

        Fact fact = requireFact(factId);

        authorizationService.assertCanVerifyOrResolve(
                actor,
                fact.getSubjectUserId(),
                fact.getSensitivityTier(),
                fact.getId(),
                "verify fact"
        );

        Fact verified = lifecycleService.verify(
                fact,
                actor.getUserId(),
                request.getMethod(),
                request.getNotes()
        );

        Fact withRecomputedConfidence = recomputeConfidence(verified);

        return toResponse(withRecomputedConfidence);
    }

    // =========================================================================
    // TIMELINE
    // =========================================================================

    @Transactional(readOnly = true)
    public List<FactTimelineEntryResponse> getTimeline(AuthenticatedUser actor, Long subjectUserId) {

        authorizationService.assertCanView(actor, subjectUserId, null, null, "view timeline");

        return timelineEventRepository.findBySubjectUserIdOrderByOccurredAtDesc(subjectUserId)
                .stream()
                .map(FactTimelineEntryResponse::from)
                .toList();
    }

    // =========================================================================
    // CONFLICTS
    // =========================================================================

    @Transactional(readOnly = true)
    public FactConflictResponse getConflict(AuthenticatedUser actor, Long conflictId) {

        FactConflict conflict = conflictRepository.findById(conflictId)
                .orElseThrow(() -> new ResourceNotFoundException("Conflict not found."));

        authorizationService.assertCanView(actor, conflict.getSubjectUserId(), null, null, "view conflict");

        return FactConflictResponse.from(conflict);
    }

    @Transactional
    public FactConflictResponse resolveConflict(
            AuthenticatedUser actor,
            Long conflictId,
            ConflictResolutionRequest request
    ) {

        FactConflict conflict = conflictRepository.findById(conflictId)
                .orElseThrow(() -> new ResourceNotFoundException("Conflict not found."));

        authorizationService.assertCanVerifyOrResolve(
                actor,
                conflict.getSubjectUserId(),
                null,
                null,
                "resolve conflict"
        );

        FactConflict resolved = factConflictService.resolveManually(
                conflict,
                request.getWinningFactId(),
                actor.getUserId(),
                request.getNotes()
        );

        return FactConflictResponse.from(resolved);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Fact requireFact(Long factId) {

        return factRepository.findById(factId)
                .orElseThrow(() -> new ResourceNotFoundException("Fact not found."));
    }

    private FactTypeDefinition resolveDefinition(FactProposalRequest request) {

        if (!FactTypeRegistry.isKnown(request.getFactKey())) {

            throw new IllegalArgumentException(
                    "Unknown fact key: " + request.getFactKey()
            );
        }

        FactTypeDefinition definition = FactTypeRegistry.get(request.getFactKey());

        if (definition.category() != request.getCategory()) {

            throw new IllegalArgumentException(
                    "Category does not match the registered fact key: expected "
                            + definition.category() + " but received " + request.getCategory()
            );
        }

        if (definition.valueType() != request.getValueType()) {

            throw new IllegalArgumentException(
                    "Value type does not match the registered fact key: expected "
                            + definition.valueType() + " but received " + request.getValueType()
            );
        }

        return definition;
    }

    private void validateProvenance(FactProvenanceType provenanceType) {

        if (!USER_FACING_ALLOWED_PROVENANCE.contains(provenanceType)) {

            throw new IllegalArgumentException(
                    "Provenance type " + provenanceType
                            + " cannot be submitted directly - it is reserved for internal system/derivation processes."
            );
        }
    }

    private void validateValueShape(FactProposalRequest request) {

        long populated = 0;

        populated += request.getStringValue() != null ? 1 : 0;
        populated += request.getDateValue() != null ? 1 : 0;
        populated += request.getNumberValue() != null ? 1 : 0;
        populated += request.getBooleanValue() != null ? 1 : 0;

        if (populated != 1) {

            throw new IllegalArgumentException(
                    "Exactly one value field must be populated, matching valueType " + request.getValueType()
            );
        }

        boolean matchesType = switch (request.getValueType()) {
            case STRING -> request.getStringValue() != null;
            case DATE -> request.getDateValue() != null;
            case NUMBER -> request.getNumberValue() != null;
            case BOOLEAN -> request.getBooleanValue() != null;
        };

        if (!matchesType) {

            throw new IllegalArgumentException(
                    "The populated value field does not match valueType " + request.getValueType()
            );
        }
    }

    private String truncate(String value, int maxLength) {

        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    private FactResponse toResponse(Fact fact) {

        List<FactEvidenceResponse> evidence = evidenceRepository.findByFactId(fact.getId())
                .stream()
                .map(FactEvidenceResponse::from)
                .toList();

        return FactResponse.from(fact, evidence);
    }

    /**
     * Recomputes confidence after a fact-level change (verification) that
     * does not itself go through {@code FactConflictService}.
     */
    private Fact recomputeConfidence(Fact fact) {

        long evidenceCount = Math.max(evidenceRepository.countByFactId(fact.getId()), 1);

        long daysSinceLastObserved = ChronoUnit.DAYS.between(fact.getLastObservedAt(), LocalDateTime.now());

        FactTypeDefinition definition = FactTypeRegistry.get(fact.getFactKey());

        ConfidenceCalculator.Result result = confidenceCalculator.compute(
                new ConfidenceCalculator.Input(
                        fact.getProvenanceType(),
                        (int) evidenceCount,
                        Boolean.TRUE.equals(fact.getIsVerified()),
                        fact.getVerificationMethod(),
                        fact.getStatus() == FactStatus.CONTESTED,
                        daysSinceLastObserved,
                        definition.stalenessDays()
                )
        );

        fact.setConfidenceScore(result.score());
        fact.setConfidenceLevel(result.level());
        fact.setConfidenceExplanation(result.explanation());

        return factRepository.save(fact);
    }
}
