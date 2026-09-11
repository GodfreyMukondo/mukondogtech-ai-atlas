package com.godfrey.ai_immigration_document_analyzer.agent.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

/**
 * Request to explain a subject's current case-wide overview - contradictions,
 * timeline gaps/overlaps, and evidence/anomaly signals (Phase 5.4). Mirrors
 * {@code ExplainPathwayDiscoveryRequest}'s shape exactly: like Discovery,
 * Case Overview is subject-scoped rather than assessment/requirement-scoped,
 * so there is no existing PathwayAssessment/RequirementEvaluation row to
 * re-derive the subject from. {@code subjectUserId} is never trusted as an
 * authority on its own - the backend independently re-verifies it via {@code
 * FactAuthorizationService.assertCanView} (through {@code
 * CaseOverviewContextTool} -&gt; {@code CaseOverviewService} -&gt; {@code
 * DigitalTwinProjectionService}) before any data is read, exactly like {@code
 * GET /api/cases/{subjectUserId}/{contradictions,timeline,signals}} already
 * does. This preserves the existing pattern where a case worker with an
 * active assignment may request another subject's overview - authorization
 * is enforced server-side regardless of what is supplied here.
 */
@Data
public class ExplainCaseOverviewRequest {

    @NotNull(message = "Subject user ID is required")
    @Positive(message = "Subject user ID must be a positive value")
    private Long subjectUserId;
}
