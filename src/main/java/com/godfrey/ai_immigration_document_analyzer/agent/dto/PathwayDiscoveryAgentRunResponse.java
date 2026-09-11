package com.godfrey.ai_immigration_document_analyzer.agent.dto;

import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGoalType;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * The API-facing view of one {@link AgentRun} for the Phase 5.3
 * EXPLAIN_PATHWAY_DISCOVERY goal - a deliberate sibling of {@code
 * AgentRunResponse}/{@code PathwayAssessmentAgentRunResponse} rather than a
 * change to either, since both those records' {@code result} fields are
 * hard-typed to their own Phase's result shape. Same shape, same {@code
 * from(...)} convention, different result type.
 */
public record PathwayDiscoveryAgentRunResponse(
        Long id,
        AgentGoalType goal,
        AgentRunStatus status,
        Long subjectUserId,
        String planSummary,
        List<String> toolsInvoked,
        String model,
        String reasoningSummary,
        ExplainPathwayDiscoveryResult result,
        AgentGroundingState groundingState,
        boolean humanReviewRequired,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public static PathwayDiscoveryAgentRunResponse from(AgentRun run, ExplainPathwayDiscoveryResult result) {

        List<String> tools = run.getToolsInvoked() == null || run.getToolsInvoked().isBlank()
                ? List.of()
                : Arrays.asList(run.getToolsInvoked().split(","));

        return new PathwayDiscoveryAgentRunResponse(
                run.getId(),
                run.getGoal(),
                run.getStatus(),
                run.getSubjectUserId(),
                run.getPlanSummary(),
                tools,
                run.getModel(),
                run.getReasoningSummary(),
                result,
                run.getGroundingState(),
                Boolean.TRUE.equals(run.getHumanReviewRequired()),
                run.getErrorMessage(),
                run.getStartedAt(),
                run.getCompletedAt()
        );
    }
}
