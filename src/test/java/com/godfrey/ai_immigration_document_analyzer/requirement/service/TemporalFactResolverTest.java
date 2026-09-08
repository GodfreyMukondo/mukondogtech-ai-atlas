package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.DigitalTwinResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactConflictResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.ConflictStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConflict;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.service.DigitalTwinProjectionService;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationFactView;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TemporalFactResolver} - Scenarios D and G from the
 * approved architecture: a Fact's temporal window, not its current status
 * alone, determines whether it was the valid claim as of a past assessment
 * date. 2024 employment must never be read as 2026 employment.
 */
@ExtendWith(MockitoExtension.class)
class TemporalFactResolverTest {

    private static final Long SUBJECT_ID = 10L;

    @Mock private FactRepository factRepository;
    @Mock private FactConflictRepository factConflictRepository;
    @Mock private DigitalTwinProjectionService digitalTwinProjectionService;
    @Mock private FactAuthorizationService factAuthorizationService;

    private TemporalFactResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TemporalFactResolver(factRepository, factConflictRepository, digitalTwinProjectionService, factAuthorizationService);
    }

    private AuthenticatedUser user(Long id, Role... roles) {
        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority())).toList();
        return new AuthenticatedUser(id, "u" + id + "@example.com", "hash", authorities, true, true, true, true);
    }

    private Fact fact(Long id, String factKey, FactStatus status, LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {
        return Fact.builder()
                .id(id).subjectUserId(SUBJECT_ID).category(FactCategory.EMPLOYMENT).factKey(factKey)
                .valueType(FactValueType.STRING).stringValue("value-" + id).status(status)
                .provenanceType(FactProvenanceType.USER_INPUT).effectiveFrom(effectiveFrom).effectiveTo(effectiveTo)
                .observedAt(effectiveFrom != null ? effectiveFrom : LocalDateTime.now())
                .lastObservedAt(LocalDateTime.now()).recordedAt(LocalDateTime.now())
                .confidenceScore(0.6).isVerified(false)
                .build();
    }

    // =========================================================================
    // LIVE PATH (assessmentDate == null) -> THE AUTHORIZED DIGITAL TWIN
    // =========================================================================

    @Test
    void nullAssessmentDateDelegatesToTheLiveDigitalTwin() {

        FactResponse current = new FactResponse(
                1L, SUBJECT_ID, FactCategory.EMPLOYMENT, "EMPLOYMENT.CURRENT_EMPLOYER", FactValueType.STRING,
                "Company B", null, null, null, FactStatus.ACCEPTED, FactProvenanceType.USER_INPUT, null,
                0.6, null, null, false, null, null, null, null, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now(), null, List.of()
        );

        DigitalTwinResponse twin = new DigitalTwinResponse(SUBJECT_ID, LocalDateTime.now(), List.of(current), List.of(), "note");

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);
        when(digitalTwinProjectionService.getDigitalTwin(actor, SUBJECT_ID)).thenReturn(twin);

        EvaluationFactView view = resolver.resolve(actor, SUBJECT_ID, null);

        assertThat(view.factsFor("EMPLOYMENT.CURRENT_EMPLOYER")).containsExactly(current);
        verify(factRepository, never()).findBySubjectUserId(any());
    }

    // =========================================================================
    // HISTORICAL PATH - POINT-IN-TIME RECONSTRUCTION
    // =========================================================================

    @Test
    void pastAssessmentDateReconstructsTheSupersededFactNotTheCurrentOne() {

        // 2024 employment (superseded), 2026 employment (current) - a query
        // for mid-2025 must return the 2024 fact, never the 2026 one.
        Fact employer2024 = fact(
                1L, "EMPLOYMENT.CURRENT_EMPLOYER", FactStatus.SUPERSEDED,
                LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        Fact employer2026 = fact(
                2L, "EMPLOYMENT.CURRENT_EMPLOYER", FactStatus.ACCEPTED,
                LocalDateTime.of(2026, 1, 1, 0, 0), null
        );

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);
        when(factRepository.findBySubjectUserId(SUBJECT_ID)).thenReturn(List.of(employer2024, employer2026));

        LocalDateTime assessmentDate = LocalDateTime.of(2025, 6, 1, 0, 0);
        EvaluationFactView view = resolver.resolve(actor, SUBJECT_ID, assessmentDate);

        verify(factAuthorizationService).assertCanView(eq(actor), eq(SUBJECT_ID), isNull(), isNull(), anyString());

        List<FactResponse> resolved = view.factsFor("EMPLOYMENT.CURRENT_EMPLOYER");
        assertThat(resolved).hasSize(1);
        assertThat(resolved.get(0).id()).isEqualTo(1L); // the 2024 fact, not the 2026 one
    }

    @Test
    void currentDateStillSeesTheCurrentEmployerNotTheHistoricalOne() {

        Fact employer2024 = fact(
                1L, "EMPLOYMENT.CURRENT_EMPLOYER", FactStatus.SUPERSEDED,
                LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        Fact employer2026 = fact(
                2L, "EMPLOYMENT.CURRENT_EMPLOYER", FactStatus.ACCEPTED,
                LocalDateTime.of(2026, 1, 1, 0, 0), null
        );

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);
        when(factRepository.findBySubjectUserId(SUBJECT_ID)).thenReturn(List.of(employer2024, employer2026));

        LocalDateTime assessmentDate = LocalDateTime.of(2026, 6, 1, 0, 0);
        EvaluationFactView view = resolver.resolve(actor, SUBJECT_ID, assessmentDate);

        List<FactResponse> resolved = view.factsFor("EMPLOYMENT.CURRENT_EMPLOYER");
        assertThat(resolved).hasSize(1);
        assertThat(resolved.get(0).id()).isEqualTo(2L);
    }

    @Test
    void twoContestedFactsCoveringTheDateSurfaceAsAConflictNotAsAcceptedFacts() {

        Fact contestedA = fact(1L, "EMPLOYMENT.CURRENT_EMPLOYER", FactStatus.CONTESTED,
                LocalDateTime.of(2025, 1, 1, 0, 0), null);
        Fact contestedB = fact(2L, "EMPLOYMENT.CURRENT_EMPLOYER", FactStatus.CONTESTED,
                LocalDateTime.of(2025, 1, 1, 0, 0), null);

        FactConflict conflict = FactConflict.builder()
                .id(55L).subjectUserId(SUBJECT_ID).factKey("EMPLOYMENT.CURRENT_EMPLOYER")
                .factAId(1L).factBId(2L).status(ConflictStatus.OPEN).build();

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);
        when(factRepository.findBySubjectUserId(SUBJECT_ID)).thenReturn(List.of(contestedA, contestedB));
        when(factConflictRepository.findByFactAIdOrFactBId(1L, 1L)).thenReturn(List.of(conflict));

        EvaluationFactView view = resolver.resolve(actor, SUBJECT_ID, LocalDateTime.of(2025, 6, 1, 0, 0));

        assertThat(view.isConflicted("EMPLOYMENT.CURRENT_EMPLOYER")).isTrue();
        assertThat(view.openConflictIdByFactKey().get("EMPLOYMENT.CURRENT_EMPLOYER")).isEqualTo(55L);
        assertThat(view.factsFor("EMPLOYMENT.CURRENT_EMPLOYER")).isEmpty();
    }

    @Test
    void unauthorizedActorIsDeniedBeforeAnyFactIsRead() {

        AuthenticatedUser stranger = user(999L, Role.USER);

        doThrow(new AccessDeniedException("denied")).when(factAuthorizationService)
                .assertCanView(eq(stranger), eq(SUBJECT_ID), isNull(), isNull(), anyString());

        assertThatThrownBy(() -> resolver.resolve(stranger, SUBJECT_ID, LocalDateTime.now().minusDays(1)))
                .isInstanceOf(AccessDeniedException.class);

        verify(factRepository, never()).findBySubjectUserId(any());
    }
}
