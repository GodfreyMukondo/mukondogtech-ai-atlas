package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementFactBinding;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;

import java.util.List;

/**
 * Admin-only detail projection of a Requirement, including its raw logic
 * JSON and fact bindings - deliberately separate from the applicant-facing
 * {@link RequirementResponse} (which intentionally omits these internals)
 * so authoring/editing has what it needs without changing that existing,
 * already-consumed response shape.
 */
public record RequirementAdminDetailResponse(
        Long id,
        String requirementKey,
        RequirementType requirementType,
        String title,
        String description,
        String jurisdiction,
        String immigrationContext,
        Long regulatoryVersionId,
        Boolean mandatory,
        RequirementStatus status,
        String applicabilityLogicJson,
        String satisfactionLogicJson,
        List<FactBinding> factBindings
) {

    public record FactBinding(
            String factKey,
            Boolean requiresVerification,
            FactProvenanceType minimumProvenanceType
    ) {

        public static FactBinding from(RequirementFactBinding binding) {

            return new FactBinding(
                    binding.getFactKey(),
                    binding.getRequiresVerification(),
                    binding.getMinimumProvenanceType()
            );
        }
    }

    public static RequirementAdminDetailResponse from(Requirement requirement, List<RequirementFactBinding> bindings) {

        return new RequirementAdminDetailResponse(
                requirement.getId(),
                requirement.getRequirementKey(),
                requirement.getRequirementType(),
                requirement.getTitle(),
                requirement.getDescription(),
                requirement.getJurisdiction(),
                requirement.getImmigrationContext(),
                requirement.getRegulatoryVersionId(),
                requirement.getMandatory(),
                requirement.getStatus(),
                requirement.getApplicabilityLogic(),
                requirement.getSatisfactionLogic(),
                bindings.stream().map(FactBinding::from).toList()
        );
    }
}
