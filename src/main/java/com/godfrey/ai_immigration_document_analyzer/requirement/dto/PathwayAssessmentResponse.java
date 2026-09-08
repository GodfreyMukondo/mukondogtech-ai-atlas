package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessment;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The personalized, terminal result of evaluating one Pathway against one
 * subject. {@code requirementEvaluations} is the explainability chain
 * (section 10) rendered flat - every Requirement Evaluation that fed this
 * outcome, each still carrying its own Fact/evidence trail.
 */
public record PathwayAssessmentResponse(
        Long id,
        Long pathwayId,
        String pathwayKey,
        String pathwayName,
        Long subjectUserId,
        LocalDateTime assessmentDate,
        RequirementEvaluationOutcome outcome,
        Double assessmentConfidenceScore,
        EvaluationCertaintyLevel assessmentConfidenceLevel,
        List<RequirementEvaluationResponse> requirementEvaluations,
        LocalDateTime computedAt
) {

    public static PathwayAssessmentResponse from(
            PathwayAssessment assessment,
            String pathwayKey,
            String pathwayName,
            List<RequirementEvaluationResponse> requirementEvaluations
    ) {

        return new PathwayAssessmentResponse(
                assessment.getId(),
                assessment.getPathwayId(),
                pathwayKey,
                pathwayName,
                assessment.getSubjectUserId(),
                assessment.getAssessmentDate(),
                assessment.getOutcome(),
                assessment.getAssessmentConfidenceScore(),
                assessment.getAssessmentConfidenceLevel(),
                requirementEvaluations,
                assessment.getComputedAt()
        );
    }
}
