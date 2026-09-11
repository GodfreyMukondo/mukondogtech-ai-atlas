package com.godfrey.ai_immigration_document_analyzer.agent.tool;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIntelligenceResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseOverviewSummaryResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseReadinessResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRiskBand;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.service.CaseIntelligenceService;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
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
 * Unit tests for {@link PathwayIntelligenceContextTool} - a thin, typed
 * wrapper over {@link CaseIntelligenceService#getCaseIntelligence}. Focus:
 * the tool genuinely delegates (never fabricates or reshapes the bundle),
 * authorization denials propagate unchanged, and the tool declares itself
 * read-only with a stable name.
 */
@ExtendWith(MockitoExtension.class)
class PathwayIntelligenceContextToolTest {

    private static final Long ASSESSMENT_ID = 500L;
    private static final Long SUBJECT_ID = 10L;

    @Mock
    private CaseIntelligenceService caseIntelligenceService;

    private PathwayIntelligenceContextTool tool;

    @BeforeEach
    void setUp() {
        tool = new PathwayIntelligenceContextTool(caseIntelligenceService);
    }

    private AuthenticatedUser user(Long id) {
        return new AuthenticatedUser(
                id, "user" + id + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())), true, true, true, true
        );
    }

    private CaseIntelligenceResponse caseIntelligence() {
        return new CaseIntelligenceResponse(
                ASSESSMENT_ID, SUBJECT_ID, "TEST_PATHWAY", "Test Pathway",
                RequirementEvaluationOutcome.SATISFIED,
                new CaseOverviewSummaryResponse(100.0, 100.0, 100.0, 0, 0, 0, 0, CaseRiskBand.NORMAL, "note"),
                new CaseReadinessResponse(100.0, 100.0, 100.0, 100.0, List.of()),
                List.of(), List.of(), "Evidence guidance text", LocalDateTime.now()
        );
    }

    @Test
    void invokeReturnsExactlyWhatCaseIntelligenceServiceReturns() {

        AuthenticatedUser actor = user(SUBJECT_ID);
        CaseIntelligenceResponse expected = caseIntelligence();

        when(caseIntelligenceService.getCaseIntelligence(actor, ASSESSMENT_ID)).thenReturn(expected);

        CaseIntelligenceResponse actual = tool.invoke(actor, new PathwayIntelligenceContextTool.Input(ASSESSMENT_ID));

        assertThat(actual).isSameAs(expected);
        verify(caseIntelligenceService).getCaseIntelligence(actor, ASSESSMENT_ID);
    }

    @Test
    void invokePropagatesAuthorizationDenialUnchanged() {

        AuthenticatedUser stranger = user(999L);

        when(caseIntelligenceService.getCaseIntelligence(stranger, ASSESSMENT_ID))
                .thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> tool.invoke(stranger, new PathwayIntelligenceContextTool.Input(ASSESSMENT_ID)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void toolIsExplicitlyReadOnly() {
        assertThat(tool.accessLevel()).isEqualTo(AgentToolAccessLevel.READ_ONLY);
    }

    @Test
    void toolHasAStableName() {
        assertThat(tool.name()).isEqualTo(AgentToolName.GET_PATHWAY_INTELLIGENCE_CONTEXT);
    }
}
