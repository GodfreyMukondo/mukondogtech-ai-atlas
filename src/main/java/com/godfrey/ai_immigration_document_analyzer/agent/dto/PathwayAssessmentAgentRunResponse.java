package com.godfrey.ai_immigration_document_analyzer.agent.dto;

import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGoalType;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * The API-facing view of one {@link AgentRun} for the Phase 5.2
 * EXPLAIN_PATHWAY_ASSESSMENT goal - a deliberate sibling of {@code
 * AgentRunResponse} rather than a change to it, since that record's {@code
 * result} field is hard-typed to {@code ExplainRequirementResult} and is
 * already relied upon by Phase 5.1's tests and frontend. Same shape,
 * same {@code from(...)} convention, different result type.
 */
public record PathwayAssessmentAgentRunResponse(
        Long id,
        AgentGoalType goal,
        AgentRunStatus status,
        Long subjectUserId,
        Long pathwayAssessmentId,
        String planSummary,
        List<String> toolsInvoked,
        String model,
        String reasoningSummary,
        ExplainPathwayAssessmentResult result,
        AgentGroundingState groundingState,
        boolean humanReviewRequired,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public static PathwayAssessmentAgentRunResponse from(AgentRun run, ExplainPathwayAssessmentResult result) {

        List<String> tools = run.getToolsInvoked() == null || run.getToolsInvoked().isBlank()
                ? List.of()
                : Arrays.asList(run.getToolsInvoked().split(","));

        return new PathwayAssessmentAgentRunResponse(
                run.getId(),
                run.getGoal(),
                run.getStatus(),
                run.getSubjectUserId(),
                run.getPathwayAssessmentId(),
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
