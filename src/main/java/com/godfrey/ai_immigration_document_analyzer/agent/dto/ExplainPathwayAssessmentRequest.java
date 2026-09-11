package com.godfrey.ai_immigration_document_analyzer.agent.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

/**
 * Request to explain one whole pathway assessment's current overall outcome
 * (Phase 5.2). Deliberately carries no {@code subjectUserId} of any kind -
 * the subject is always re-derived from the PathwayAssessment itself and
 * authorized against, exactly like {@code ExplainRequirementRequest}.
 */
@Data
public class ExplainPathwayAssessmentRequest {

    @NotNull(message = "Pathway assessment ID is required")
    @Positive(message = "Pathway assessment ID must be a positive value")
    private Long pathwayAssessmentId;
}
