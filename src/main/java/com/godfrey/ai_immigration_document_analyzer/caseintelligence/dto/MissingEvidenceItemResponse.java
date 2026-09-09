package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

import java.util.List;

/**
 * One requirement whose evaluation could not proceed for lack of
 * sufficient, accepted evidence (outcome INSUFFICIENT_EVIDENCE - never
 * NOT_SATISFIED). {@code missingFactKeys} is derived by diffing this
 * requirement's declared {@code RequirementFactBinding}s against the Fact
 * keys the evaluation actually found contributing evidence for - the exact
 * same binding data {@code RequirementEvaluationService} itself reads,
 * never a second, separately-authored expectation list.
 *
 * {@code necessity} distinguishes evidence bound to a MANDATORY requirement
 * (REQUIRED) from evidence bound only to a non-mandatory one (SUPPORTING).
 * A third tier - evidence that is merely useful/recommended, not tied to
 * any specific requirement binding - is surfaced separately as
 * {@code CaseIntelligenceResponse.recommendedEvidenceGuidance}, reusing the
 * Pathway's own pre-existing {@code evidenceExpectations} field rather than
 * inventing a new one.
 */
public record MissingEvidenceItemResponse(
        Long requirementId,
        String requirementKey,
        String requirementTitle,
        Boolean mandatory,
        EvidenceNecessity necessity,
        CaseIssueSeverity priority,
        List<String> missingFactKeys,
        String reason
) {
}
