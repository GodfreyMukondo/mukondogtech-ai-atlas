package com.godfrey.ai_immigration_document_analyzer.evidencegraph.service;

import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItem;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItemStatus;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.EvidenceSourceType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EvidenceItemLifecycleService} - the only place an
 * EvidenceItem's status is ever changed, mirroring
 * {@code FactLifecycleServiceTest}'s coverage shape: valid transitions
 * succeed, invalid ones are rejected outright, terminal statuses accept
 * nothing further, and rejection captures its reason.
 */
@ExtendWith(MockitoExtension.class)
class EvidenceItemLifecycleServiceTest {

    @Mock
    private EvidenceItemRepository evidenceItemRepository;

    private EvidenceItemLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {

        lifecycleService = new EvidenceItemLifecycleService(evidenceItemRepository);

        org.mockito.Mockito.lenient().when(evidenceItemRepository.save(any(EvidenceItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private EvidenceItem item(EvidenceItemStatus status) {

        return EvidenceItem.builder()
                .id(1L)
                .sourceType(EvidenceSourceType.DOCUMENT)
                .status(status)
                .build();
    }

    // =========================================================================
    // VALID TRANSITIONS
    // =========================================================================

    @Test
    void discoveredToExtractedIsAllowed() {

        EvidenceItem discovered = item(EvidenceItemStatus.DISCOVERED);

        EvidenceItem result = lifecycleService.transition(discovered, EvidenceItemStatus.EXTRACTED);

        assertThat(result.getStatus()).isEqualTo(EvidenceItemStatus.EXTRACTED);
    }

    @Test
    void extractedToCandidateIsAllowed() {

        EvidenceItem extracted = item(EvidenceItemStatus.EXTRACTED);

        EvidenceItem result = lifecycleService.transition(extracted, EvidenceItemStatus.CANDIDATE);

        assertThat(result.getStatus()).isEqualTo(EvidenceItemStatus.CANDIDATE);
    }

    @Test
    void candidateToLinkedIsAllowed() {

        EvidenceItem candidate = item(EvidenceItemStatus.CANDIDATE);

        EvidenceItem result = lifecycleService.transition(candidate, EvidenceItemStatus.LINKED);

        assertThat(result.getStatus()).isEqualTo(EvidenceItemStatus.LINKED);
    }

    @Test
    void linkedToSupersededIsAllowed() {

        EvidenceItem linked = item(EvidenceItemStatus.LINKED);

        EvidenceItem result = lifecycleService.transition(linked, EvidenceItemStatus.SUPERSEDED);

        assertThat(result.getStatus()).isEqualTo(EvidenceItemStatus.SUPERSEDED);
    }

    // =========================================================================
    // INVALID TRANSITIONS
    // =========================================================================

    @Test
    void discoveredCannotJumpDirectlyToLinked() {

        EvidenceItem discovered = item(EvidenceItemStatus.DISCOVERED);

        assertThatThrownBy(() -> lifecycleService.transition(discovered, EvidenceItemStatus.LINKED))
                .isInstanceOf(IllegalStateException.class);

        verify(evidenceItemRepository, never()).save(any());
    }

    @Test
    void validationFailedIsTerminal() {

        EvidenceItem validationFailed = item(EvidenceItemStatus.VALIDATION_FAILED);

        assertThatThrownBy(() -> lifecycleService.transition(validationFailed, EvidenceItemStatus.CANDIDATE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void supersededIsTerminal() {

        EvidenceItem superseded = item(EvidenceItemStatus.SUPERSEDED);

        assertThatThrownBy(() -> lifecycleService.transition(superseded, EvidenceItemStatus.LINKED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectedIsTerminal() {

        EvidenceItem rejected = item(EvidenceItemStatus.REJECTED);

        assertThatThrownBy(() -> lifecycleService.transition(rejected, EvidenceItemStatus.LINKED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void extractedCannotJumpDirectlyToLinked() {

        // No endpoint may accept a raw status - CANDIDATE -> LINKED is only
        // ever reached as a side effect of the Fact it supports being
        // accepted, never a directly requested transition.
        EvidenceItem extracted = item(EvidenceItemStatus.EXTRACTED);

        assertThatThrownBy(() -> lifecycleService.transition(extracted, EvidenceItemStatus.LINKED))
                .isInstanceOf(IllegalStateException.class);
    }

    // =========================================================================
    // REJECTION REASON CAPTURE
    // =========================================================================

    @Test
    void rejectCapturesReasonFromCandidate() {

        EvidenceItem candidate = item(EvidenceItemStatus.CANDIDATE);

        EvidenceItem result = lifecycleService.reject(candidate, "Illegible scan - not usable as evidence.");

        assertThat(result.getStatus()).isEqualTo(EvidenceItemStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Illegible scan - not usable as evidence.");
    }

    @Test
    void rejectCapturesReasonFromLinked() {

        EvidenceItem linked = item(EvidenceItemStatus.LINKED);

        EvidenceItem result = lifecycleService.reject(linked, "Superseded by a clearer copy.");

        assertThat(result.getStatus()).isEqualTo(EvidenceItemStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("Superseded by a clearer copy.");
    }

    // =========================================================================
    // TRANSITION GUARD IS QUERYABLE INDEPENDENTLY
    // =========================================================================

    @Test
    void assertValidTransitionDoesNotThrowForAllowedTransition() {
        lifecycleService.assertValidTransition(EvidenceItemStatus.DISCOVERED, EvidenceItemStatus.EXTRACTED);
    }

    @Test
    void assertValidTransitionThrowsForDisallowedTransition() {

        assertThatThrownBy(() ->
                lifecycleService.assertValidTransition(EvidenceItemStatus.VALIDATION_FAILED, EvidenceItemStatus.CANDIDATE)
        ).isInstanceOf(IllegalStateException.class);
    }
}
