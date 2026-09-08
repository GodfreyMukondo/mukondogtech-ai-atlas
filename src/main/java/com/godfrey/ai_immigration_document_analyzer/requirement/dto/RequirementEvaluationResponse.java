package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Safe, fully-typed projection of a RequirementEvaluation. Never a raw
 * entity leaked through the API. Outcome, certainty and explanation are
 * kept as clearly separate fields - the outcome vocabulary here is the
 * SATISFIED/NOT_SATISFIED/INSUFFICIENT_EVIDENCE/UNKNOWN/CONFLICTED/... set,
 * never collapsed into a boolean.
 */
public record RequirementEvaluationResponse(
        Long id,
        Long requirementId,
        String requirementKey,
        String requirementTitle,
        Long regulatoryVersionId,
        Long subjectUserId,
        LocalDateTime assessmentDate,
        RequirementEvaluationOutcome outcome,
        Double certaintyScore,
        EvaluationCertaintyLevel certaintyLevel,
        String explanation,
        List<Long> contributingFactIds,
        List<Long> unresolvedConflictIds,
        LocalDateTime evaluatedAt
) {

    public static RequirementEvaluationResponse from(
            RequirementEvaluation evaluation,
            String requirementKey,
            String requirementTitle,
            List<Long> contributingFactIds,
            List<Long> unresolvedConflictIds
    ) {

        return new RequirementEvaluationResponse(
                evaluation.getId(),
                evaluation.getRequirementId(),
                requirementKey,
                requirementTitle,
                evaluation.getRegulatoryVersionId(),
                evaluation.getSubjectUserId(),
                evaluation.getAssessmentDate(),
                evaluation.getOutcome(),
                evaluation.getCertaintyScore(),
                evaluation.getCertaintyLevel(),
                evaluation.getExplanation(),
                contributingFactIds,
                unresolvedConflictIds,
                evaluation.getEvaluatedAt()
        );
    }
}
