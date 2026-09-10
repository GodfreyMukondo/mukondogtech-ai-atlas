package com.godfrey.ai_immigration_document_analyzer.agent.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

/**
 * Request to explain one requirement's current status within one pathway
 * assessment. Deliberately carries no {@code subjectUserId} of any kind -
 * the subject is always re-derived from the PathwayAssessment itself and
 * authorized against, exactly like every other case-scoped endpoint in this
 * codebase (there is no parameter here a caller could tamper with to reach
 * another subject's case).
 */
@Data
public class ExplainRequirementRequest {

    @NotNull(message = "Pathway assessment ID is required")
    @Positive(message = "Pathway assessment ID must be a positive value")
    private Long pathwayAssessmentId;

    @NotNull(message = "Requirement ID is required")
    @Positive(message = "Requirement ID must be a positive value")
    private Long requirementId;
}
