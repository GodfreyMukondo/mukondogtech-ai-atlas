package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;

import java.util.List;

/**
 * One row of the Requirement-to-Evidence Matrix (Master Platform Expansion,
 * Case Intelligence Engine). Requirements map to Facts/evidence, never to
 * document filenames. {@code supportingFacts} is the same
 * {@code ExplanationResponse.FactExplanation} list {@code ExplainabilityService}
 * already assembles, reshaped into matrix form rather than re-queried -
 * each entry already carries its own provenance/verification/confidence.
 *
 * {@code supportStatus} is a Case-Intelligence-specific reclassification of
 * {@code rawOutcome} (see {@code CaseRequirementSupportStatus}) - the raw
 * nine-state outcome is always included alongside it so nothing is hidden
 * behind the simplified label.
 */
public record RequirementEvidenceMatrixRowResponse(
        Long requirementId,
        String requirementKey,
        String requirementTitle,
        RequirementType requirementType,
        Boolean mandatory,
        CaseRequirementSupportStatus supportStatus,
        RequirementEvaluationOutcome rawOutcome,
        EvaluationCertaintyLevel certaintyLevel,
        Long regulatoryVersionId,
        String regulatorySourceAuthority,
        RegulatoryVerificationStatus regulatoryVerificationStatus,
        List<ExplanationResponse.FactExplanation> supportingFacts,
        List<String> missingFactKeys,
        List<Long> conflictingFactConflictIds,
        String explanation
) {
}
