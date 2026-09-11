package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.ConflictResolutionType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.ConflictStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConflict;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.service.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FactConflictService} - the field-level Conflict
 * Auto-Resolution Policy engine.
 *
 * A real {@link FactLifecycleService} and {@link ConfidenceCalculator} are
 * used (both are cheap, dependency-light collaborators) so these tests
 * exercise the genuine interaction between conflict policy, the lifecycle
 * state machine and the confidence model, while the repository/notification
 * boundary stays mocked.
 *
 * The one invariant every test here ultimately protects: CONFLICT DETECTED
 * is never FRAUD DETECTED - a losing Fact is always REJECTED (lost this
 * specific comparison), never deleted, never labelled fraudulent.
 */
@ExtendWith(MockitoExtension.class)
class FactConflictServiceTest {

    private static final Long SUBJECT_ID = 10L;

    @Mock
    private FactRepository factRepository;

    @Mock
    private FactConflictRepository conflictRepository;

    @Mock
    private FactEvidenceRepository evidenceRepository;

    @Mock
    private FactTimelineService timelineService;

    @Mock
    private NotificationService notificationService;

    private FactConflictService conflictService;

    @BeforeEach
    void setUp() {

        FactLifecycleService lifecycleService = new FactLifecycleService(factRepository, timelineService);
        ConfidenceCalculator confidenceCalculator = new ConfidenceCalculator();

        conflictService = new FactConflictService(
                factRepository, conflictRepository, evidenceRepository, timelineService,
                lifecycleService, confidenceCalculator, notificationService
        );

        org.mockito.Mockito.lenient().when(factRepository.save(any(Fact.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(conflictRepository.save(any(FactConflict.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(evidenceRepository.countByFactId(anyLong())).thenReturn(1L);
    }

    private Fact.FactBuilder baseFact(Long id, String factKey, FactProvenanceType provenance) {

        LocalDateTime now = LocalDateTime.now();

        return Fact.builder()
                .id(id)
                .subjectUserId(SUBJECT_ID)
                .factKey(factKey)
                .valueType(FactValueType.STRING)
                .stringValue("value-" + id)
                .provenanceType(provenance)
                .createdByAccessorType(AccessorType.SUBJECT)
                .createdByUserId(SUBJECT_ID)
                .isVerified(false)
                .observedAt(now)
                .lastObservedAt(now)
                .recordedAt(now)
                .status(FactStatus.PROPOSED);
    }

    // =========================================================================
    // PERMANENTLY MULTI-VALUED - NO CONFLICT FRAMEWORK APPLIES
    // =========================================================================

    @Test
    void permanentlyMultiValuedFactIsAcceptedWithoutCheckingForExisting() {

        Fact incoming = baseFact(1L, "TRAVEL_HISTORY.ENTRY_EXIT_RECORD", FactProvenanceType.USER_INPUT).build();

        Fact result = conflictService.processIncomingFact(incoming);

        assertThat(result.getStatus()).isEqualTo(FactStatus.ACCEPTED);
        verify(factRepository, never()).findBySubjectUserIdAndFactKeyAndStatus(any(), any(), any());
    }

    // =========================================================================
    // SINGLE_CURRENT - NO EXISTING FACT
    // =========================================================================

    @Test
    void singleCurrentFactWithNoExistingIsAcceptedOutright() {

        when(factRepository.findBySubjectUserIdAndFactKeyAndStatus(SUBJECT_ID, "IDENTITY.FULL_NAME", FactStatus.ACCEPTED))
                .thenReturn(List.of());

        Fact incoming = baseFact(1L, "IDENTITY.FULL_NAME", FactProvenanceType.USER_INPUT).build();

        Fact result = conflictService.processIncomingFact(incoming);

        assertThat(result.getStatus()).isEqualTo(FactStatus.ACCEPTED);
    }

    // =========================================================================
    // HISTORICAL_MULTI_VALUED - NON-OVERLAPPING SUCCESSION IS NOT A CONFLICT
    // =========================================================================

    @Test
    void nonOverlappingHistoricalSuccessionSupersedesRatherThanConflicts() {

        Fact existing = baseFact(1L, "EMPLOYMENT.CURRENT_EMPLOYER", FactProvenanceType.USER_INPUT)
                .status(FactStatus.ACCEPTED)
                .effectiveFrom(LocalDateTime.of(2025, 1, 1, 0, 0))
                .effectiveTo(null)
                .build();

        Fact incoming = baseFact(2L, "EMPLOYMENT.CURRENT_EMPLOYER", FactProvenanceType.USER_INPUT)
                .effectiveFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .build();

        when(factRepository.findBySubjectUserIdAndFactKeyAndStatusAndEffectiveToIsNull(
                SUBJECT_ID, "EMPLOYMENT.CURRENT_EMPLOYER", FactStatus.ACCEPTED
        )).thenReturn(Optional.of(existing));

        Fact result = conflictService.processIncomingFact(incoming);

        assertThat(result.getStatus()).isEqualTo(FactStatus.ACCEPTED);
        assertThat(existing.getStatus()).isEqualTo(FactStatus.SUPERSEDED);
        assertThat(existing.getEffectiveTo()).isEqualTo(incoming.getEffectiveFrom());

        // A routine succession must never open a conflict record.
        verify(conflictRepository, never()).save(any());
        verify(notificationService, never()).notify(any(), anyString(), anyString(), anyString(), any());
    }

    // =========================================================================
    // ALWAYS_HUMAN_RESOLUTION - MUST NEVER AUTO-RESOLVE
    // =========================================================================

    @Test
    void conflictOnAlwaysHumanResolutionFieldNeverAutoResolvesAndNotifiesForReview() {

        Fact existing = baseFact(1L, "IDENTITY.FULL_NAME", FactProvenanceType.USER_INPUT)
                .status(FactStatus.ACCEPTED)
                .build();

        Fact incoming = baseFact(2L, "IDENTITY.FULL_NAME", FactProvenanceType.EXTERNAL_AUTHORITATIVE_SOURCE).build();

        when(factRepository.findBySubjectUserIdAndFactKeyAndStatus(SUBJECT_ID, "IDENTITY.FULL_NAME", FactStatus.ACCEPTED))
                .thenReturn(List.of(existing));
        when(factRepository.findById(2L)).thenReturn(Optional.of(incoming));

        Fact result = conflictService.processIncomingFact(incoming);

        // Neither side is silently promoted - both remain CONTESTED, and the
        // conflict itself is never characterized as fraud.
        assertThat(result.getStatus()).isEqualTo(FactStatus.CONTESTED);
        assertThat(existing.getStatus()).isEqualTo(FactStatus.CONTESTED);

        verify(conflictRepository).save(any(FactConflict.class));
        verify(notificationService).notify(eq(SUBJECT_ID), eq("FACT_CONFLICT_DETECTED"), anyString(), anyString(), anyString());

        // No automatic winner/loser was ever declared.
        verify(notificationService, never()).notify(eq(SUBJECT_ID), eq("FACT_AUTO_RESOLVED"), anyString(), anyString(), anyString());
    }

    // =========================================================================
    // SAFE_AUTO_RESOLUTION_ALLOWED - MOST RECENT WINS WITHOUT A CONFIDENCE GAP
    // =========================================================================

    @Test
    void safeAutoResolutionFieldPicksMostRecentlyRecordedFactRegardlessOfConfidence() {

        LocalDateTime earlier = LocalDateTime.now().minusMinutes(10);
        LocalDateTime later = LocalDateTime.now();

        Fact existing = baseFact(1L, "RESIDENCE.CURRENT_ADDRESS", FactProvenanceType.USER_INPUT)
                .status(FactStatus.ACCEPTED)
                .effectiveFrom(LocalDateTime.now())
                .effectiveTo(null)
                .recordedAt(earlier)
                .build();

        Fact incoming = baseFact(2L, "RESIDENCE.CURRENT_ADDRESS", FactProvenanceType.USER_INPUT)
                .effectiveFrom(LocalDateTime.now().minusDays(1)) // earlier start -> overlapping, treated as conflict
                .recordedAt(later)
                .build();

        when(factRepository.findBySubjectUserIdAndFactKeyAndStatusAndEffectiveToIsNull(
                SUBJECT_ID, "RESIDENCE.CURRENT_ADDRESS", FactStatus.ACCEPTED
        )).thenReturn(Optional.of(existing));
        when(factRepository.findById(2L)).thenReturn(Optional.of(incoming));

        conflictService.processIncomingFact(incoming);

        assertThat(incoming.getStatus()).isEqualTo(FactStatus.ACCEPTED); // more recently recorded
        assertThat(existing.getStatus()).isEqualTo(FactStatus.REJECTED);

        ArgumentCaptorFactConflict captured = captureConflict(2);
        assertThat(captured.conflict.getStatus()).isEqualTo(ConflictStatus.RESOLVED);
        assertThat(captured.conflict.getResolutionType()).isEqualTo(ConflictResolutionType.SAFE_AUTO);
        assertThat(captured.conflict.getWinningFactId()).isEqualTo(2L);
    }

    // =========================================================================
    // EVIDENCE_WEIGHTED_AUTO_RESOLUTION_ALLOWED
    // =========================================================================

    @Test
    void evidenceWeightedFieldAutoResolvesWhenConfidenceGapClearsThePolicyThresholds() {

        Fact existing = baseFact(1L, "RESIDENCE.CURRENT_COUNTRY", FactProvenanceType.USER_INPUT) // ~0.50
                .status(FactStatus.ACCEPTED)
                .effectiveFrom(LocalDateTime.now())
                .effectiveTo(null)
                .build();

        Fact incoming = baseFact(2L, "RESIDENCE.CURRENT_COUNTRY", FactProvenanceType.EXTERNAL_AUTHORITATIVE_SOURCE) // ~0.90
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .build();

        when(factRepository.findBySubjectUserIdAndFactKeyAndStatusAndEffectiveToIsNull(
                SUBJECT_ID, "RESIDENCE.CURRENT_COUNTRY", FactStatus.ACCEPTED
        )).thenReturn(Optional.of(existing));
        when(factRepository.findById(2L)).thenReturn(Optional.of(incoming));

        conflictService.processIncomingFact(incoming);

        assertThat(incoming.getStatus()).isEqualTo(FactStatus.ACCEPTED); // stronger provenance
        assertThat(existing.getStatus()).isEqualTo(FactStatus.REJECTED);

        ArgumentCaptorFactConflict captured = captureConflict(2);
        assertThat(captured.conflict.getResolutionType()).isEqualTo(ConflictResolutionType.EVIDENCE_WEIGHTED_AUTO);

        verify(notificationService).notify(eq(SUBJECT_ID), eq("FACT_AUTO_RESOLVED"), anyString(), anyString(), anyString());
    }

    @Test
    void evidenceWeightedFieldFallsBackToHumanReviewWhenConfidenceIsComparablyWeighted() {

        // Same provenance on both sides -> zero confidence gap -> the field's
        // default auto-resolution class is permitted but the shared
        // confidence-floor/minimum-gap check is not met, so this must fall
        // back to human review even though the category allows automation.
        Fact existing = baseFact(1L, "RESIDENCE.CURRENT_COUNTRY", FactProvenanceType.USER_INPUT)
                .status(FactStatus.ACCEPTED)
                .effectiveFrom(LocalDateTime.now())
                .effectiveTo(null)
                .build();

        Fact incoming = baseFact(2L, "RESIDENCE.CURRENT_COUNTRY", FactProvenanceType.USER_INPUT)
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .build();

        when(factRepository.findBySubjectUserIdAndFactKeyAndStatusAndEffectiveToIsNull(
                SUBJECT_ID, "RESIDENCE.CURRENT_COUNTRY", FactStatus.ACCEPTED
        )).thenReturn(Optional.of(existing));
        when(factRepository.findById(2L)).thenReturn(Optional.of(incoming));

        conflictService.processIncomingFact(incoming);

        assertThat(incoming.getStatus()).isEqualTo(FactStatus.CONTESTED);
        assertThat(existing.getStatus()).isEqualTo(FactStatus.CONTESTED);

        ArgumentCaptorFactConflict captured = captureConflict(1);
        assertThat(captured.conflict.getStatus()).isEqualTo(ConflictStatus.OPEN);

        verify(notificationService).notify(eq(SUBJECT_ID), eq("FACT_CONFLICT_DETECTED"), anyString(), anyString(), anyString());
    }

    // =========================================================================
    // MANUAL (HUMAN) RESOLUTION
    // =========================================================================

    @Test
    void manualResolutionAcceptsTheWinnerAndRejectsTheLoser() {

        Fact factA = baseFact(1L, "IDENTITY.FULL_NAME", FactProvenanceType.USER_INPUT).status(FactStatus.CONTESTED).build();
        Fact factB = baseFact(2L, "IDENTITY.FULL_NAME", FactProvenanceType.EXTERNAL_AUTHORITATIVE_SOURCE).status(FactStatus.CONTESTED).build();

        FactConflict conflict = FactConflict.builder()
                .id(50L)
                .subjectUserId(SUBJECT_ID)
                .factKey("IDENTITY.FULL_NAME")
                .factAId(1L)
                .factBId(2L)
                .status(ConflictStatus.OPEN)
                .build();

        when(factRepository.findById(1L)).thenReturn(Optional.of(factA));
        when(factRepository.findById(2L)).thenReturn(Optional.of(factB));

        FactConflict resolved = conflictService.resolveManually(conflict, 2L, 999L, "Confirmed via passport.");

        assertThat(factB.getStatus()).isEqualTo(FactStatus.ACCEPTED);
        assertThat(factA.getStatus()).isEqualTo(FactStatus.REJECTED);
        assertThat(factA.getRejectionReason()).contains("not a fraud determination");

        assertThat(resolved.getStatus()).isEqualTo(ConflictStatus.RESOLVED);
        assertThat(resolved.getResolutionType()).isEqualTo(ConflictResolutionType.HUMAN_DECISION);
        assertThat(resolved.getWinningFactId()).isEqualTo(2L);
        assertThat(resolved.getResolvedByUserId()).isEqualTo(999L);
    }

    @Test
    void manualResolutionRejectsAWinningFactIdNotPartOfTheConflict() {

        FactConflict conflict = FactConflict.builder()
                .id(50L).subjectUserId(SUBJECT_ID).factKey("IDENTITY.FULL_NAME")
                .factAId(1L).factBId(2L).status(ConflictStatus.OPEN)
                .build();

        assertThatThrownBy(() -> conflictService.resolveManually(conflict, 999L, 5L, "n/a"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void manualResolutionRejectsAnAlreadyResolvedConflict() {

        FactConflict conflict = FactConflict.builder()
                .id(50L).subjectUserId(SUBJECT_ID).factKey("IDENTITY.FULL_NAME")
                .factAId(1L).factBId(2L).status(ConflictStatus.RESOLVED)
                .build();

        assertThatThrownBy(() -> conflictService.resolveManually(conflict, 1L, 5L, "n/a"))
                .isInstanceOf(IllegalStateException.class);
    }

    // =========================================================================
    // APPLICANT CONFIRMATION - SELF-REPORTED, NEVER VERIFICATION
    // =========================================================================

    @Test
    void applicantConfirmationAcceptsTheConfirmedValueAndRejectsTheOtherWithoutVerifying() {

        Fact factA = baseFact(1L, "IDENTITY.FULL_NAME", FactProvenanceType.USER_INPUT).status(FactStatus.CONTESTED).build();
        Fact factB = baseFact(2L, "IDENTITY.FULL_NAME", FactProvenanceType.USER_INPUT).status(FactStatus.CONTESTED).build();

        FactConflict conflict = FactConflict.builder()
                .id(50L)
                .subjectUserId(SUBJECT_ID)
                .factKey("IDENTITY.FULL_NAME")
                .factAId(1L)
                .factBId(2L)
                .status(ConflictStatus.OPEN)
                .build();

        when(factRepository.findById(1L)).thenReturn(Optional.of(factA));
        when(factRepository.findById(2L)).thenReturn(Optional.of(factB));

        FactConflict resolved = conflictService.applicantConfirm(conflict, 2L, SUBJECT_ID, "This is the correct value.");

        assertThat(factB.getStatus()).isEqualTo(FactStatus.ACCEPTED);
        assertThat(factA.getStatus()).isEqualTo(FactStatus.REJECTED);
        assertThat(factA.getRejectionReason()).contains("not a fraud determination");

        // The critical semantic invariant: self-confirmation never sets the
        // independent verification overlay on either fact.
        assertThat(factB.getIsVerified()).isFalse();
        assertThat(factA.getIsVerified()).isFalse();

        assertThat(resolved.getStatus()).isEqualTo(ConflictStatus.RESOLVED);
        assertThat(resolved.getResolutionType()).isEqualTo(ConflictResolutionType.APPLICANT_CONFIRMATION);
        assertThat(resolved.getWinningFactId()).isEqualTo(2L);
        assertThat(resolved.getResolvedByUserId()).isEqualTo(SUBJECT_ID);

        // Neither fact is ever deleted - competing evidence/history is retained.
        verify(factRepository, never()).delete(any());
        verify(factRepository, never()).deleteById(any());
    }

    @Test
    void applicantConfirmationRejectsAConfirmedFactIdNotPartOfTheConflict() {

        FactConflict conflict = FactConflict.builder()
                .id(50L).subjectUserId(SUBJECT_ID).factKey("IDENTITY.FULL_NAME")
                .factAId(1L).factBId(2L).status(ConflictStatus.OPEN)
                .build();

        assertThatThrownBy(() -> conflictService.applicantConfirm(conflict, 999L, SUBJECT_ID, "n/a"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applicantConfirmationRejectsAnAlreadyResolvedConflict() {

        FactConflict conflict = FactConflict.builder()
                .id(50L).subjectUserId(SUBJECT_ID).factKey("IDENTITY.FULL_NAME")
                .factAId(1L).factBId(2L).status(ConflictStatus.RESOLVED)
                .build();

        assertThatThrownBy(() -> conflictService.applicantConfirm(conflict, 1L, SUBJECT_ID, "n/a"))
                .isInstanceOf(IllegalStateException.class);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private ArgumentCaptorFactConflict captureConflict(int expectedSaveCount) {

        org.mockito.ArgumentCaptor<FactConflict> captor = org.mockito.ArgumentCaptor.forClass(FactConflict.class);
        verify(conflictRepository, times(expectedSaveCount)).save(captor.capture());

        return new ArgumentCaptorFactConflict(captor.getValue());
    }

    private record ArgumentCaptorFactConflict(FactConflict conflict) {
    }
}
