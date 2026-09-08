package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;

import java.time.LocalDateTime;

/**
 * Catalogue projection of a Pathway - knowledge about a possible
 * immigration route, never a person's application. See
 * {@link PathwayAssessmentResponse} for the personalized outcome of
 * evaluating this Pathway against a subject.
 */
public record PathwayResponse(
        Long id,
        String pathwayKey,
        String name,
        String description,
        String jurisdiction,
        String category,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        PathwayStatus status
) {

    public static PathwayResponse from(Pathway pathway) {

        return new PathwayResponse(
                pathway.getId(),
                pathway.getPathwayKey(),
                pathway.getName(),
                pathway.getDescription(),
                pathway.getJurisdiction(),
                pathway.getCategory(),
                pathway.getValidFrom(),
                pathway.getValidTo(),
                pathway.getStatus()
        );
    }
}
