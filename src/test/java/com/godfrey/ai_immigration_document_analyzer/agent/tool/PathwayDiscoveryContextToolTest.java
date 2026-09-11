package com.godfrey.ai_immigration_document_analyzer.agent.tool;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayDiscoveryResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayDiscoveryService;
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
 * Unit tests for {@link PathwayDiscoveryContextTool} - a thin, typed wrapper
 * over {@link PathwayDiscoveryService#discover}. Focus: the tool genuinely
 * delegates (never fabricates or reshapes the ranking), authorization
 * denials propagate unchanged, and the tool declares itself read-only with a
 * stable name.
 */
@ExtendWith(MockitoExtension.class)
class PathwayDiscoveryContextToolTest {

    private static final Long SUBJECT_ID = 10L;

    @Mock
    private PathwayDiscoveryService pathwayDiscoveryService;

    private PathwayDiscoveryContextTool tool;

    @BeforeEach
    void setUp() {
        tool = new PathwayDiscoveryContextTool(pathwayDiscoveryService);
    }

    private AuthenticatedUser user(Long id) {
        return new AuthenticatedUser(
                id, "user" + id + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())), true, true, true, true
        );
    }

    private PathwayDiscoveryResponse discovery() {
        return new PathwayDiscoveryResponse(
                SUBJECT_ID, LocalDateTime.now(), 0, List.of(), "disclaimer text"
        );
    }

    @Test
    void invokeReturnsExactlyWhatPathwayDiscoveryServiceReturns() {

        AuthenticatedUser actor = user(SUBJECT_ID);
        PathwayDiscoveryResponse expected = discovery();

        when(pathwayDiscoveryService.discover(actor, SUBJECT_ID)).thenReturn(expected);

        PathwayDiscoveryResponse actual = tool.invoke(actor, new PathwayDiscoveryContextTool.Input(SUBJECT_ID));

        assertThat(actual).isSameAs(expected);
        verify(pathwayDiscoveryService).discover(actor, SUBJECT_ID);
    }

    @Test
    void invokePropagatesAuthorizationDenialUnchanged() {

        AuthenticatedUser stranger = user(999L);

        when(pathwayDiscoveryService.discover(stranger, SUBJECT_ID))
                .thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> tool.invoke(stranger, new PathwayDiscoveryContextTool.Input(SUBJECT_ID)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void toolIsExplicitlyReadOnly() {
        assertThat(tool.accessLevel()).isEqualTo(AgentToolAccessLevel.READ_ONLY);
    }

    @Test
    void toolHasAStableName() {
        assertThat(tool.name()).isEqualTo(AgentToolName.GET_PATHWAY_DISCOVERY_CONTEXT);
    }
}
