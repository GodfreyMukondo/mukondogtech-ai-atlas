package com.godfrey.ai_immigration_document_analyzer.agent.dto;

import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGoalType;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * The API-facing view of one {@link AgentRun} - the audit record plus its
 * structured result (null when the run failed or when evidence was
 * insufficient before any explanation could be grounded).
 */
public record AgentRunResponse(
        Long id,
        AgentGoalType goal,
        AgentRunStatus status,
        Long subjectUserId,
        Long pathwayAssessmentId,
        Long requirementId,
        String planSummary,
        List<String> toolsInvoked,
        String model,
        String reasoningSummary,
        ExplainRequirementResult result,
        AgentGroundingState groundingState,
        boolean humanReviewRequired,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public static AgentRunResponse from(AgentRun run, ExplainRequirementResult result) {

        List<String> tools = run.getToolsInvoked() == null || run.getToolsInvoked().isBlank()
                ? List.of()
                : Arrays.asList(run.getToolsInvoked().split(","));

        return new AgentRunResponse(
                run.getId(),
                run.getGoal(),
                run.getStatus(),
                run.getSubjectUserId(),
                run.getPathwayAssessmentId(),
                run.getRequirementId(),
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
