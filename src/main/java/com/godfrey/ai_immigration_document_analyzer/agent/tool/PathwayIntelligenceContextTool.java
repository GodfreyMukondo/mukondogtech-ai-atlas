package com.godfrey.ai_immigration_document_analyzer.agent.tool;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIntelligenceResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.service.CaseIntelligenceService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * PATHWAY INTELLIGENCE CONTEXT TOOL
 * ============================================================================
 *
 * Wraps {@link CaseIntelligenceService#getCaseIntelligence} - the SAME
 * already-authorized, already-deterministic bundle {@code
 * CaseIntelligenceController} exposes (overall outcome, readiness, the full
 * Requirement-to-Evidence Matrix, missing evidence). Never a second
 * evaluation, never a new authorization decision, never a second readiness
 * formula (Phase 5.2 reuses {@code RequirementReadinessCalculator}
 * transitively through this same call).
 *
 * Authorization happens entirely inside {@code getCaseIntelligence} (via
 * {@code ExplainabilityService} -&gt; {@code FactAuthorizationService},
 * checked once against the PathwayAssessment's own subject); this tool
 * performs no repository read of its own.
 * ============================================================================
 */
@Component
@RequiredArgsConstructor
public class PathwayIntelligenceContextTool
        implements AgentTool<PathwayIntelligenceContextTool.Input, CaseIntelligenceResponse> {

    private final CaseIntelligenceService caseIntelligenceService;

    @Override
    public AgentToolName name() {
        return AgentToolName.GET_PATHWAY_INTELLIGENCE_CONTEXT;
    }

    @Override
    public AgentToolAccessLevel accessLevel() {
        return AgentToolAccessLevel.READ_ONLY;
    }

    @Override
    public CaseIntelligenceResponse invoke(AuthenticatedUser actor, Input input) {
        return caseIntelligenceService.getCaseIntelligence(actor, input.pathwayAssessmentId());
    }

    public record Input(
            Long pathwayAssessmentId
    ) {
    }
}
