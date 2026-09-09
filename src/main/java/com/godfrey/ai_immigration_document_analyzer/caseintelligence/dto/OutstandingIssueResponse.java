package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;

/**
 * One requirement standing between the subject and a fully-ready
 * PathwayAssessment - composed entirely from an already-computed
 * {@code ExplanationResponse.RequirementExplanation}, never a second
 * evaluation pass.
 */
public record OutstandingIssueResponse(
        Long requirementId,
        String requirementKey,
        String requirementTitle,
        Boolean mandatory,
        RequirementEvaluationOutcome outcome,
        CaseIssueSeverity severity,
        String reason
) {
}
