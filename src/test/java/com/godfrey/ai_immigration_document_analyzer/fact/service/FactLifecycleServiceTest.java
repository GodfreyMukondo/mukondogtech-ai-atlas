package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.TimelineEventType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.VerificationMethod;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FactLifecycleService} - the only place a Fact's
 * status is ever changed.
 *
 * Covers: valid transitions succeed, invalid ones are rejected outright
 * (a caller can never force PROPOSED straight into a status the state
 * machine does not allow), terminal statuses accept nothing further,
 * verification is an overlay distinct from status, and every state change
 * is paired with a timeline event using the same actor/reason.
 */
@ExtendWith(MockitoExtension.class)
class FactLifecycleServiceTest {

    @Mock
    private FactRepository factRepository;

    @Mock
    private FactTimelineService timelineService;

    private FactLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {

        lifecycleService = new FactLifecycleService(factRepository, timelineService);

        org.mockito.Mockito.lenient().when(factRepository.save(any(Fact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Fact fact(FactStatus status) {

        return Fact.builder()
                .id(1L)
                .subjectUserId(10L)
                .factKey("IDENTITY.FULL_NAME")
                .status(status)
                .isVerified(false)
                .observedAt(LocalDateTime.now())
                .lastObservedAt(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // VALID TRANSITIONS
    // =========================================================================

    @Test
    void proposedToAcceptedIsAllowedAndRecordsTimeline() {

        Fact proposed = fact(FactStatus.PROPOSED);

        Fact result = lifecycleService.transition(
                proposed, FactStatus.ACCEPTED, AccessorType.SUBJECT, 10L, TimelineEventType.FACT_CREATED, "no conflict"
        );

        assertThat(result.getStatus()).isEqualTo(FactStatus.ACCEPTED);

        verify(timelineService).record(
                eq(10L), eq(TimelineEventType.FACT_CREATED), eq(1L), isNull(),
                eq(AccessorType.SUBJECT), eq(10L), eq("PROPOSED"), eq("ACCEPTED"), eq("no conflict")
        );
    }

    @Test
    void acceptedToContestedIsAllowed() {

        Fact accepted = fact(FactStatus.ACCEPTED);

        Fact result = lifecycleService.transition(
                accepted, FactStatus.CONTESTED, AccessorType.SYSTEM, null, TimelineEventType.FACT_CONFLICTED, "conflict"
        );

        assertThat(result.getStatus()).isEqualTo(FactStatus.CONTESTED);
    }

    // =========================================================================
    // INVALID TRANSITIONS
    // =========================================================================

    @Test
    void proposedCannotJumpDirectlyToPendingReview() {

        Fact proposed = fact(FactStatus.PROPOSED);

        assertThatThrownBy(() ->
                lifecycleService.transition(
                        proposed, FactStatus.PENDING_REVIEW, AccessorType.SYSTEM, null, TimelineEventType.FACT_CREATED, "n/a"
                )
        ).isInstanceOf(IllegalStateException.class);

        // No state change and no timeline entry may be produced on a
        // rejected transition attempt.
        verify(factRepository, never()).save(any());
        verify(timelineService, never()).record(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectedIsTerminalAndAcceptsNoFurtherTransitions() {

        Fact rejected = fact(FactStatus.REJECTED);

        assertThatThrownBy(() ->
                lifecycleService.transition(
                        rejected, FactStatus.ACCEPTED, AccessorType.SYSTEM, null, TimelineEventType.FACT_RESOLVED, "n/a"
                )
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void supersededIsTerminal() {

        Fact superseded = fact(FactStatus.SUPERSEDED);

        assertThatThrownBy(() ->
                lifecycleService.transition(
                        superseded, FactStatus.ACCEPTED, AccessorType.SYSTEM, null, TimelineEventType.FACT_RESOLVED, "n/a"
                )
        ).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pendingReviewCannotJumpDirectlyToAccepted() {

        // PENDING_REVIEW (suspicious-content review) may only return to
        // PROPOSED or REJECTED - never straight to ACCEPTED, which would
        // bypass re-validation.
        Fact pendingReview = fact(FactStatus.PENDING_REVIEW);

        assertThatThrownBy(() ->
                lifecycleService.transition(
                        pendingReview, FactStatus.ACCEPTED, AccessorType.CASE_WORKER, 5L, TimelineEventType.FACT_RESOLVED, "n/a"
                )
        ).isInstanceOf(IllegalStateException.class);
    }

    // =========================================================================
    // REJECTION / RETRACTION REASON CAPTURE
    // =========================================================================

    @Test
    void transitionToRejectedCapturesReason() {

        Fact contested = fact(FactStatus.CONTESTED);

        Fact result = lifecycleService.transition(
                contested, FactStatus.REJECTED, AccessorType.CASE_WORKER, 5L, TimelineEventType.FACT_RESOLVED,
                "Lost human resolution. CONFLICT DETECTED - this is not a fraud determination."
        );

        assertThat(result.getStatus()).isEqualTo(FactStatus.REJECTED);
        assertThat(result.getRejectionReason()).contains("not a fraud determination");
    }

    @Test
    void transitionToRetractedCapturesReason() {

        Fact accepted = fact(FactStatus.ACCEPTED);

        Fact result = lifecycleService.transition(
                accepted, FactStatus.RETRACTED, AccessorType.SUBJECT, 10L, TimelineEventType.FACT_RETRACTED, "withdrawn by subject"
        );

        assertThat(result.getStatus()).isEqualTo(FactStatus.RETRACTED);
        assertThat(result.getRetractionReason()).isEqualTo("withdrawn by subject");
    }

    // =========================================================================
    // SUPERSESSION
    // =========================================================================

    @Test
    void supersedeClosesEffectiveToAndMarksSuperseded() {

        Fact oldFact = fact(FactStatus.ACCEPTED);
        Fact newFact = Fact.builder()
                .id(2L)
                .subjectUserId(10L)
                .factKey("EMPLOYMENT.CURRENT_EMPLOYER")
                .status(FactStatus.PROPOSED)
                .effectiveFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .observedAt(LocalDateTime.now())
                .lastObservedAt(LocalDateTime.now())
                .build();

        Fact result = lifecycleService.supersede(oldFact, newFact, AccessorType.SUBJECT, 10L);

        assertThat(result.getStatus()).isEqualTo(FactStatus.SUPERSEDED);
        assertThat(result.getEffectiveTo()).isEqualTo(newFact.getEffectiveFrom());

        verify(timelineService).record(
                eq(10L), eq(TimelineEventType.FACT_SUPERSEDED), eq(1L), isNull(),
                eq(AccessorType.SUBJECT), eq(10L), eq("ACCEPTED"), eq("SUPERSEDED"), anyString()
        );
    }

    // =========================================================================
    // VERIFICATION - OVERLAY, NOT A STATUS
    // =========================================================================

    @Test
    void onlyAnAcceptedFactMayBeVerified() {

        Fact proposed = fact(FactStatus.PROPOSED);

        assertThatThrownBy(() ->
                lifecycleService.verify(proposed, 5L, VerificationMethod.HUMAN_REVIEW, "confirmed")
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only an ACCEPTED fact may be verified");

        verify(factRepository, never()).save(any());
    }

    @Test
    void verifyingAnAcceptedFactSetsOverlayFieldsWithoutChangingStatus() {

        Fact accepted = fact(FactStatus.ACCEPTED);

        Fact result = lifecycleService.verify(accepted, 5L, VerificationMethod.HUMAN_REVIEW, "confirmed by case worker");

        assertThat(result.getStatus()).isEqualTo(FactStatus.ACCEPTED);
        assertThat(result.getIsVerified()).isTrue();
        assertThat(result.getVerifiedByUserId()).isEqualTo(5L);
        assertThat(result.getVerificationMethod()).isEqualTo(VerificationMethod.HUMAN_REVIEW);
        assertThat(result.getVerifiedAt()).isNotNull();

        verify(timelineService).record(
                eq(10L), eq(TimelineEventType.FACT_VERIFIED), eq(1L), isNull(),
                eq(AccessorType.CASE_WORKER), eq(5L), eq("ACCEPTED"), eq("ACCEPTED"), anyString()
        );
    }

    // =========================================================================
    // TRANSITION GUARD IS QUERYABLE INDEPENDENTLY
    // =========================================================================

    @Test
    void assertValidTransitionDoesNotThrowForAllowedTransition() {
        lifecycleService.assertValidTransition(FactStatus.PROPOSED, FactStatus.ACCEPTED);
    }

    @Test
    void assertValidTransitionThrowsForDisallowedTransition() {

        assertThatThrownBy(() -> lifecycleService.assertValidTransition(FactStatus.VALIDATION_FAILED, FactStatus.ACCEPTED))
                .isInstanceOf(IllegalStateException.class);
    }
}
