package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

import java.util.List;

/**
 * A multi-dimensional readiness breakdown for one PathwayAssessment -
 * deliberately distinct from, and never a replacement for,
 * {@code PathwayAssessment.assessmentConfidenceScore} (a single aggregate
 * certainty-in-the-outcome figure). This is a purely computed, never
 * persisted, projection over the assessment's own RequirementEvaluations:
 *
 * - requirementCoveragePercent: share of MANDATORY requirements SATISFIED.
 * - evidenceCoveragePercent: share of ALL requirements not sitting on
 *   INSUFFICIENT_EVIDENCE.
 * - consistencyPercent: share of ALL requirements not sitting on CONFLICTED.
 * - overallReadinessPercent: the simple average of the three above.
 *
 * None of these figures are, or imply, a probability of visa approval.
 */
public record CaseReadinessResponse(
        Double requirementCoveragePercent,
        Double evidenceCoveragePercent,
        Double consistencyPercent,
        Double overallReadinessPercent,
        List<OutstandingIssueResponse> outstandingIssues
) {
}
