package com.godfrey.ai_immigration_document_analyzer.agent.tool;

import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayDiscoveryResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayDiscoveryService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * PATHWAY DISCOVERY CONTEXT TOOL
 * ============================================================================
 *
 * Wraps {@link PathwayDiscoveryService#discover} - the SAME already-
 * authorized, already-deterministic, transient ranking {@code
 * PathwayAssessmentController.discoverPathways} exposes at {@code GET
 * /api/pathways/discovery} (Phase 5.3). Never a second ranking formula,
 * never a second evaluation engine, never a persisted row.
 *
 * Authorization happens entirely inside {@code discover} (via {@code
 * TemporalFactResolver.resolve} -&gt; {@code DigitalTwinProjectionService
 * .getDigitalTwin} -&gt; {@code FactAuthorizationService.assertCanView} on
 * the live path this tool always takes); this tool performs no repository
 * read of its own and makes no additional authorization decision.
 * {@code subjectUserId} arriving here is never trusted as an authority - it
 * is only ever used as the id independently re-verified by that boundary,
 * exactly like the existing {@code GET /api/pathways/discovery} endpoint.
 * ============================================================================
 */
@Component
@RequiredArgsConstructor
public class PathwayDiscoveryContextTool
        implements AgentTool<PathwayDiscoveryContextTool.Input, PathwayDiscoveryResponse> {

    private final PathwayDiscoveryService pathwayDiscoveryService;

    @Override
    public AgentToolName name() {
        return AgentToolName.GET_PATHWAY_DISCOVERY_CONTEXT;
    }

    @Override
    public AgentToolAccessLevel accessLevel() {
        return AgentToolAccessLevel.READ_ONLY;
    }

    @Override
    public PathwayDiscoveryResponse invoke(AuthenticatedUser actor, Input input) {
        return pathwayDiscoveryService.discover(actor, input.subjectUserId());
    }

    public record Input(
            Long subjectUserId
    ) {
    }
}
