package com.godfrey.ai_immigration_document_analyzer.agent.tool;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseContradictionsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRiskBand;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseSignalsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseTimelineResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.service.CaseOverviewService;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.DigitalTwinResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.service.DigitalTwinProjectionService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CaseOverviewContextTool} - a thin, typed wrapper over
 * {@link CaseOverviewService#getContradictions}/{@link
 * CaseOverviewService#getTimeline}/{@link CaseOverviewService#getSignals}.
 * Focus: the tool genuinely delegates to all three methods (never fabricates
 * or reshapes any of the three DTOs), authorization denials propagate
 * unchanged, {@code hasAnyCaseHistory} is read directly from the Digital
 * Twin's fact list rather than computed independently, and the tool declares
 * itself read-only with a stable name.
 */
@ExtendWith(MockitoExtension.class)
class CaseOverviewContextToolTest {

    private static final Long SUBJECT_ID = 10L;

    @Mock
    private CaseOverviewService caseOverviewService;

    @Mock
    private DigitalTwinProjectionService digitalTwinProjectionService;

    private CaseOverviewContextTool tool;

    @BeforeEach
    void setUp() {
        tool = new CaseOverviewContextTool(caseOverviewService, digitalTwinProjectionService);
    }

    private AuthenticatedUser user(Long id) {
        return new AuthenticatedUser(
                id, "user" + id + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())), true, true, true, true
        );
    }

    private CaseContradictionsResponse contradictions() {
        return new CaseContradictionsResponse(SUBJECT_ID, List.of(), List.of(), LocalDateTime.now(), "note");
    }

    private CaseTimelineResponse timeline() {
        return new CaseTimelineResponse(SUBJECT_ID, List.of(), LocalDateTime.now(), "note");
    }

    private CaseSignalsResponse signals() {
        return new CaseSignalsResponse(SUBJECT_ID, CaseRiskBand.NORMAL, 0, 0, 0, 0, 0, 0, 0, LocalDateTime.now(), "note");
    }

    private FactResponse fact() {
        return new FactResponse(
                1L, SUBJECT_ID, FactCategory.EMPLOYMENT, "EMPLOYMENT.CURRENT_EMPLOYER",
                FactValueType.STRING, "Acme Corp", null, null, null,
                FactStatus.ACCEPTED, FactProvenanceType.USER_INPUT, FactSensitivityTier.T2_STANDARD_PERSONAL,
                0.9, FactConfidenceLevel.HIGH, null, false, null, null,
                LocalDateTime.now().minusYears(1), null, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now(), null, List.of()
        );
    }

    private DigitalTwinResponse twin(List<FactResponse> currentFacts) {
        return new DigitalTwinResponse(SUBJECT_ID, LocalDateTime.now(), currentFacts, List.of(), "note");
    }

    @Test
    void invokeCombinesAllThreeCaseOverviewServiceResponsesUnchanged() {

        AuthenticatedUser actor = user(SUBJECT_ID);
        CaseContradictionsResponse expectedContradictions = contradictions();
        CaseTimelineResponse expectedTimeline = timeline();
        CaseSignalsResponse expectedSignals = signals();

        when(caseOverviewService.getContradictions(actor, SUBJECT_ID)).thenReturn(expectedContradictions);
        when(caseOverviewService.getTimeline(actor, SUBJECT_ID)).thenReturn(expectedTimeline);
        when(caseOverviewService.getSignals(actor, SUBJECT_ID)).thenReturn(expectedSignals);
        when(digitalTwinProjectionService.getDigitalTwin(actor, SUBJECT_ID)).thenReturn(twin(List.of(fact())));

        CaseOverviewContextTool.Output actual = tool.invoke(actor, new CaseOverviewContextTool.Input(SUBJECT_ID));

        assertThat(actual.subjectUserId()).isEqualTo(SUBJECT_ID);
        assertThat(actual.contradictions()).isSameAs(expectedContradictions);
        assertThat(actual.timeline()).isSameAs(expectedTimeline);
        assertThat(actual.signals()).isSameAs(expectedSignals);
        assertThat(actual.hasAnyCaseHistory()).isTrue();

        verify(caseOverviewService).getContradictions(actor, SUBJECT_ID);
        verify(caseOverviewService).getTimeline(actor, SUBJECT_ID);
        verify(caseOverviewService).getSignals(actor, SUBJECT_ID);
    }

    @Test
    void invokeReportsNoCaseHistoryWhenTheSubjectHasNoCurrentFacts() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(caseOverviewService.getContradictions(actor, SUBJECT_ID)).thenReturn(contradictions());
        when(caseOverviewService.getTimeline(actor, SUBJECT_ID)).thenReturn(timeline());
        when(caseOverviewService.getSignals(actor, SUBJECT_ID)).thenReturn(signals());
        when(digitalTwinProjectionService.getDigitalTwin(actor, SUBJECT_ID)).thenReturn(twin(List.of()));

        CaseOverviewContextTool.Output actual = tool.invoke(actor, new CaseOverviewContextTool.Input(SUBJECT_ID));

        assertThat(actual.hasAnyCaseHistory()).isFalse();
    }

    @Test
    void invokePropagatesAuthorizationDenialUnchanged() {

        AuthenticatedUser stranger = user(999L);

        when(caseOverviewService.getContradictions(stranger, SUBJECT_ID))
                .thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> tool.invoke(stranger, new CaseOverviewContextTool.Input(SUBJECT_ID)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void toolIsExplicitlyReadOnly() {
        assertThat(tool.accessLevel()).isEqualTo(AgentToolAccessLevel.READ_ONLY);
    }

    @Test
    void toolHasAStableName() {
        assertThat(tool.name()).isEqualTo(AgentToolName.GET_CASE_OVERVIEW_CONTEXT);
    }
}
