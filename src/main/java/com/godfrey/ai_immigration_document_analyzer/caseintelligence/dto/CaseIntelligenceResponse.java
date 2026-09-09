package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The Case Intelligence bundle for one PathwayAssessment - a Case Overview
 * summary, readiness, the Requirement-to-Evidence Matrix, and missing-
 * evidence prioritization in a single call, so a consumer never has to
 * make several round trips or re-derive any of this from raw evaluation
 * rows itself. Everything here is computed fresh, at read time, from the
 * assessment's own RequirementEvaluations (plus the subject's case-wide
 * contradictions/timeline/signals for {@link #overview}) - never a second,
 * persisted copy of any of it.
 *
 * {@code recommendedEvidenceGuidance} surfaces the Pathway's own
 * pre-existing, non-gating {@code evidenceExpectations} field - the
 * "merely useful/recommended" evidence tier, distinct from the
 * requirement-bound "required"/"supporting" tiers on each
 * {@link MissingEvidenceItemResponse}.
 */
public record CaseIntelligenceResponse(
        Long pathwayAssessmentId,
        Long subjectUserId,
        String pathwayKey,
        String pathwayName,
        RequirementEvaluationOutcome overallOutcome,
        CaseOverviewSummaryResponse overview,
        CaseReadinessResponse readiness,
        List<RequirementEvidenceMatrixRowResponse> evidenceMatrix,
        List<MissingEvidenceItemResponse> missingEvidence,
        String recommendedEvidenceGuidance,
        LocalDateTime generatedAt
) {
}
