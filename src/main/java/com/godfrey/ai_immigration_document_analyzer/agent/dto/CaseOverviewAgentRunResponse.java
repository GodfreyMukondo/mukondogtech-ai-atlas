package com.godfrey.ai_immigration_document_analyzer.agent.dto;

import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGoalType;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * The API-facing view of one {@link AgentRun} for the Phase 5.4
 * EXPLAIN_CASE_OVERVIEW goal - a deliberate sibling of {@code
 * AgentRunResponse}/{@code PathwayAssessmentAgentRunResponse}/{@code
 * PathwayDiscoveryAgentRunResponse} rather than a change to any of them,
 * since every one of those records' {@code result} field is hard-typed to
 * its own Phase's result shape. Same shape, same {@code from(...)}
 * convention, different result type.
 */
public record CaseOverviewAgentRunResponse(
        Long id,
        AgentGoalType goal,
        AgentRunStatus status,
        Long subjectUserId,
        String planSummary,
        List<String> toolsInvoked,
        String model,
        String reasoningSummary,
        ExplainCaseOverviewResult result,
        AgentGroundingState groundingState,
        boolean humanReviewRequired,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public static CaseOverviewAgentRunResponse from(AgentRun run, ExplainCaseOverviewResult result) {

        List<String> tools = run.getToolsInvoked() == null || run.getToolsInvoked().isBlank()
                ? List.of()
                : Arrays.asList(run.getToolsInvoked().split(","));

        return new CaseOverviewAgentRunResponse(
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
