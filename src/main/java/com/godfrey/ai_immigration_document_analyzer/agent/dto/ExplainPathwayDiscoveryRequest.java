package com.godfrey.ai_immigration_document_analyzer.agent.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

/**
 * Request to explain/recommend from a subject's current Pathway Discovery
 * ranking (Phase 5.3). {@code subjectUserId} mirrors the existing, already
 * security-reviewed shape of {@code GET /api/pathways/discovery} - it is
 * never trusted as an authority on its own; the backend independently
 * re-verifies it via {@code FactAuthorizationService.assertCanView} (through
 * {@code PathwayDiscoveryService} -&gt; {@code TemporalFactResolver} -&gt;
 * {@code DigitalTwinProjectionService}) before any data is read, exactly
 * like that endpoint already does. Unlike {@code ExplainRequirementRequest}/
 * {@code ExplainPathwayAssessmentRequest}, there is no existing PathwayAssessment/
 * RequirementEvaluation row to re-derive the subject from here - Discovery is
 * deliberately pre-assessment and reads directly from the subject's live
 * Digital Twin, so an explicit, independently-authorized subjectUserId is the
 * correct input, not a security regression.
 */
@Data
public class ExplainPathwayDiscoveryRequest {

    @NotNull(message = "Subject user ID is required")
    @Positive(message = "Subject user ID must be a positive value")
    private Long subjectUserId;
}
