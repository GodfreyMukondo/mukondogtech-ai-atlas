package com.godfrey.ai_immigration_document_analyzer.agent.tool;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceGraphResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.GraphNodeResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.GraphNodeType;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.service.EvidenceGraphService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EvidenceGraphContextTool} - a thin, typed wrapper
 * over {@link EvidenceGraphService#graphForRequirementEvaluation}. Focus:
 * the tool genuinely delegates (never fabricates a graph), authorization
 * denials propagate unchanged, and the tool declares itself read-only.
 */
@ExtendWith(MockitoExtension.class)
class EvidenceGraphContextToolTest {

    private static final Long EVALUATION_ID = 4001L;

    @Mock
    private EvidenceGraphService evidenceGraphService;

    private EvidenceGraphContextTool tool;

    @BeforeEach
    void setUp() {
        tool = new EvidenceGraphContextTool(evidenceGraphService);
    }

    private AuthenticatedUser user(Long id) {
        return new AuthenticatedUser(
                id, "user" + id + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())), true, true, true, true
        );
    }

    @Test
    void invokeReturnsExactlyWhatEvidenceGraphServiceReturns() {

        AuthenticatedUser actor = user(10L);

        GraphNodeResponse node = new GraphNodeResponse("fact:1", GraphNodeType.FACT, "Fact", Map.of());
        EvidenceGraphResponse expected = new EvidenceGraphResponse(List.of(node), List.of());

        when(evidenceGraphService.graphForRequirementEvaluation(actor, EVALUATION_ID)).thenReturn(expected);

        EvidenceGraphResponse actual = tool.invoke(actor, new EvidenceGraphContextTool.Input(EVALUATION_ID));

        assertThat(actual).isSameAs(expected);
        verify(evidenceGraphService).graphForRequirementEvaluation(actor, EVALUATION_ID);
    }

    @Test
    void invokePropagatesAuthorizationDenialUnchanged() {

        AuthenticatedUser stranger = user(999L);

        when(evidenceGraphService.graphForRequirementEvaluation(stranger, EVALUATION_ID))
                .thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> tool.invoke(stranger, new EvidenceGraphContextTool.Input(EVALUATION_ID)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void toolIsExplicitlyReadOnly() {
        assertThat(tool.accessLevel()).isEqualTo(AgentToolAccessLevel.READ_ONLY);
    }

    @Test
    void toolHasAStableName() {
        assertThat(tool.name()).isEqualTo(AgentToolName.GET_EVIDENCE_GRAPH_CONTEXT);
    }
}
