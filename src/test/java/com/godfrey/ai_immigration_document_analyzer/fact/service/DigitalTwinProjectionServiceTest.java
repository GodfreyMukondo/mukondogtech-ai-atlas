package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.DigitalTwinResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.ConflictStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConflict;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DigitalTwinProjectionService} - the read-only
 * "what does MukondoGTech AI currently know about this case" projection.
 *
 * These tests protect the projection's core invariant: it is never a
 * second source of truth. Every call must be authorized independently and
 * recomputed live from current ACCEPTED Facts and OPEN conflicts - a
 * PROPOSED, REJECTED, SUPERSEDED or still-CONTESTED Fact must never appear
 * as current truth, and an open disagreement must never be silently
 * dropped.
 */
@ExtendWith(MockitoExtension.class)
class DigitalTwinProjectionServiceTest {

    private static final Long SUBJECT_ID = 10L;
    private static final Long STRANGER_ID = 20L;

    @Mock
    private FactRepository factRepository;

    @Mock
    private FactEvidenceRepository evidenceRepository;

    @Mock
    private FactConflictRepository conflictRepository;

    @Mock
    private FactAuthorizationService authorizationService;

    private DigitalTwinProjectionService projectionService;

    @BeforeEach
    void setUp() {
        projectionService = new DigitalTwinProjectionService(
                factRepository, evidenceRepository, conflictRepository, authorizationService
        );
    }

    private AuthenticatedUser user(Long id, Role... roles) {

        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .toList();

        return new AuthenticatedUser(id, "user" + id + "@example.com", "hash", authorities, true, true, true, true);
    }

    private Fact acceptedFact(Long id) {
        return Fact.builder().id(id).subjectUserId(SUBJECT_ID).status(FactStatus.ACCEPTED)
                .factKey("IDENTITY.FULL_NAME").build();
    }

    @Test
    void unauthorizedActorIsDeniedBeforeAnyFactIsRead() {

        AuthenticatedUser stranger = user(STRANGER_ID, Role.USER);

        doThrow(new AccessDeniedException("denied")).when(authorizationService)
                .assertCanView(eq(stranger), eq(SUBJECT_ID), isNull(), isNull(), anyString());

        assertThatThrownBy(() -> projectionService.getDigitalTwin(stranger, SUBJECT_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(factRepository, never()).findBySubjectUserIdAndStatus(any(), any());
        verify(conflictRepository, never()).findBySubjectUserIdAndStatus(any(), any());
    }

    @Test
    void projectionReflectsOnlyAcceptedFacts() {

        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        Fact accepted = acceptedFact(1L);

        when(factRepository.findBySubjectUserIdAndStatus(SUBJECT_ID, FactStatus.ACCEPTED))
                .thenReturn(List.of(accepted));
        when(evidenceRepository.findByFactId(1L)).thenReturn(List.of());
        when(conflictRepository.findBySubjectUserIdAndStatus(SUBJECT_ID, ConflictStatus.OPEN))
                .thenReturn(List.of());

        DigitalTwinResponse response = projectionService.getDigitalTwin(subject, SUBJECT_ID);

        assertThat(response.currentFacts()).hasSize(1);
        assertThat(response.currentFacts().get(0).id()).isEqualTo(1L);

        // The repository call itself is scoped to ACCEPTED - a PROPOSED,
        // CONTESTED, REJECTED or SUPERSEDED fact was never fetched, so it
        // cannot leak into the twin as if it were current truth.
        verify(factRepository).findBySubjectUserIdAndStatus(SUBJECT_ID, FactStatus.ACCEPTED);
    }

    @Test
    void unresolvedConflictsRemainVisibleOnTheProjection() {

        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        FactConflict openConflict = FactConflict.builder()
                .id(5L).subjectUserId(SUBJECT_ID).factKey("IDENTITY.FULL_NAME")
                .factAId(1L).factBId(2L).status(ConflictStatus.OPEN)
                .build();

        when(factRepository.findBySubjectUserIdAndStatus(SUBJECT_ID, FactStatus.ACCEPTED)).thenReturn(List.of());
        when(conflictRepository.findBySubjectUserIdAndStatus(SUBJECT_ID, ConflictStatus.OPEN))
                .thenReturn(List.of(openConflict));

        DigitalTwinResponse response = projectionService.getDigitalTwin(subject, SUBJECT_ID);

        assertThat(response.currentFacts()).isEmpty();
        assertThat(response.openConflicts()).hasSize(1);
        assertThat(response.openConflicts().get(0).id()).isEqualTo(5L);
    }

    @Test
    void theProjectionIsRecomputedLiveOnEveryCallRatherThanCached() {

        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        when(factRepository.findBySubjectUserIdAndStatus(SUBJECT_ID, FactStatus.ACCEPTED)).thenReturn(List.of());
        when(conflictRepository.findBySubjectUserIdAndStatus(SUBJECT_ID, ConflictStatus.OPEN)).thenReturn(List.of());

        projectionService.getDigitalTwin(subject, SUBJECT_ID);
        projectionService.getDigitalTwin(subject, SUBJECT_ID);

        verify(factRepository, times(2)).findBySubjectUserIdAndStatus(SUBJECT_ID, FactStatus.ACCEPTED);
        verify(conflictRepository, times(2)).findBySubjectUserIdAndStatus(SUBJECT_ID, ConflictStatus.OPEN);
    }
}
