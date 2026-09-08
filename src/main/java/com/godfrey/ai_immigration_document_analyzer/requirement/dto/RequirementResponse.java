package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;

/**
 * Catalogue projection of a Requirement DEFINITION - impersonal, reusable
 * regulatory knowledge, never a result. See {@link RequirementEvaluationResponse}
 * for the personalized outcome of applying this to a subject.
 */
public record RequirementResponse(
        Long id,
        String requirementKey,
        RequirementType requirementType,
        String title,
        String description,
        String jurisdiction,
        String immigrationContext,
        Long regulatoryVersionId,
        Boolean mandatory,
        RequirementStatus status
) {

    public static RequirementResponse from(Requirement requirement) {

        return new RequirementResponse(
                requirement.getId(),
                requirement.getRequirementKey(),
                requirement.getRequirementType(),
                requirement.getTitle(),
                requirement.getDescription(),
                requirement.getJurisdiction(),
                requirement.getImmigrationContext(),
                requirement.getRegulatoryVersionId(),
                requirement.getMandatory(),
                requirement.getStatus()
        );
    }
}
