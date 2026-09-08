package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactEvidenceResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;

import java.util.List;

/**
 * The full explainability chain (Requirement/Pathway Architecture
 * Specification, section 10):
 *
 * <pre>
 * PATHWAY -> REQUIREMENT -> REQUIREMENT EVALUATION -> FACT -> EVIDENCE
 *         -> SOURCE DOCUMENT -> REGULATORY SOURCE
 * </pre>
 *
 * Composed entirely by read-time queries over existing references - never a
 * second, persisted copy of the explanation.
 */
public record ExplanationResponse(
        Long pathwayAssessmentId,
        String pathwayKey,
        String pathwayName,
        RequirementEvaluationOutcome overallOutcome,
        List<RequirementExplanation> requirements
) {

    public record RequirementExplanation(
            Long requirementId,
            String requirementKey,
            String requirementTitle,
            RequirementType requirementType,
            Boolean mandatory,
            RequirementEvaluationOutcome outcome,
            String explanation,
            EvaluationCertaintyLevel certaintyLevel,
            Long regulatoryVersionId,
            String regulatorySourceAuthority,
            String regulatorySourceReference,
            RegulatoryVerificationStatus regulatoryVerificationStatus,
            List<FactExplanation> contributingFacts,
            List<Long> unresolvedConflictIds
    ) {
    }

    public record FactExplanation(
            Long factId,
            String factKey,
            FactCategory category,
            String valueSummary,
            FactProvenanceType provenanceType,
            Boolean isVerified,
            Double confidenceScore,
            FactConfidenceLevel confidenceLevel,
            List<FactEvidenceResponse> evidence
    ) {
    }
}
