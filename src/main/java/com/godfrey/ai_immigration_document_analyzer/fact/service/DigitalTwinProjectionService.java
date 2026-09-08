package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.fact.dto.DigitalTwinResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactConflictResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactEvidenceResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.ConflictStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * DIGITAL TWIN PROJECTION SERVICE
 * ============================================================================
 *
 * The Digital Twin is a READ projection, never a second source of truth.
 * There is no stored "twin" table and nothing to keep in sync - every call
 * computes the answer fresh from {@code FACTS}/{@code FACT_EVIDENCE}/
 * {@code FACT_CONFLICTS} at read time. This is also the answer to "how is
 * it refreshed": there is nothing to refresh, because it is always
 * recomputed live.
 *
 * "Current facts" = every Fact currently in ACCEPTED status for the
 * subject. A SUPERSEDED/RETRACTED/REJECTED/CONTESTED fact is never
 * silently presented as current truth - CONTESTED facts surface instead
 * through {@link #getDigitalTwin}'s open-conflicts list, so an unresolved
 * disagreement remains visible rather than being hidden behind whichever
 * value happened to be accepted first.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class DigitalTwinProjectionService {

    private final FactRepository factRepository;
    private final FactEvidenceRepository evidenceRepository;
    private final FactConflictRepository conflictRepository;
    private final FactAuthorizationService authorizationService;

    @Transactional(readOnly = true)
    public DigitalTwinResponse getDigitalTwin(AuthenticatedUser actor, Long subjectUserId) {

        authorizationService.assertCanView(actor, subjectUserId, null, null, "view digital twin");

        List<FactResponse> currentFacts = factRepository
                .findBySubjectUserIdAndStatus(subjectUserId, FactStatus.ACCEPTED)
                .stream()
                .map(this::toResponse)
                .toList();

        List<FactConflictResponse> openConflicts = conflictRepository
                .findBySubjectUserIdAndStatus(subjectUserId, ConflictStatus.OPEN)
                .stream()
                .map(FactConflictResponse::from)
                .toList();

        return new DigitalTwinResponse(
                subjectUserId,
                LocalDateTime.now(),
                currentFacts,
                openConflicts,
                "Computed live from current Fact/Evidence/Conflict records - not a separately stored copy."
        );
    }

    private FactResponse toResponse(Fact fact) {

        List<FactEvidenceResponse> evidence = evidenceRepository.findByFactId(fact.getId())
                .stream()
                .map(FactEvidenceResponse::from)
                .toList();

        return FactResponse.from(fact, evidence);
    }
}
