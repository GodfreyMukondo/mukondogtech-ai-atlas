package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin-only detail projection of a Pathway, additionally exposing the
 * {@code requirementIds} already encoded in its {@code compositionLogic} -
 * deliberately separate from the applicant-facing {@link PathwayResponse}
 * (which intentionally omits this internal), so the admin edit form can
 * pre-select a pathway's existing requirements without a second,
 * duplicated representation of the composition. {@code requirementIds} is
 * always derived by parsing the pathway's own, already-persisted
 * {@code compositionLogic} JSON - never a separately stored/maintained
 * list that could drift out of sync with it.
 */
public record PathwayAdminDetailResponse(
        Long id,
        String pathwayKey,
        String name,
        String description,
        String jurisdiction,
        String category,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        PathwayStatus status,
        List<Long> requirementIds
) {

    public static PathwayAdminDetailResponse from(Pathway pathway, List<Long> requirementIds) {

        return new PathwayAdminDetailResponse(
                pathway.getId(),
                pathway.getPathwayKey(),
                pathway.getName(),
                pathway.getDescription(),
                pathway.getJurisdiction(),
                pathway.getCategory(),
                pathway.getValidFrom(),
                pathway.getValidTo(),
                pathway.getStatus(),
                requirementIds
        );
    }
}
