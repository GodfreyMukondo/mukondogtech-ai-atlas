package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;

/**
 * The single place a {@link NodeResult} (internal, boolean-algebra-shaped)
 * is translated into the outward-facing nine-state
 * {@link RequirementEvaluationOutcome} vocabulary - shared by
 * {@code RequirementEvaluationService} (one Requirement's own logic tree)
 * and {@code PathwayAssessmentService} (a Pathway's composition tree), so
 * the two can never silently diverge on what a given Kleene result means.
 */
public final class OutcomeMapping {

    private OutcomeMapping() {
    }

    public static RequirementEvaluationOutcome fromNodeResult(NodeResult result) {

        return switch (result.value()) {
            case TRUE -> RequirementEvaluationOutcome.SATISFIED;
            case FALSE -> RequirementEvaluationOutcome.NOT_SATISFIED;
            case NOT_APPLICABLE -> RequirementEvaluationOutcome.NOT_APPLICABLE;
            case INDETERMINATE -> switch (result.flavor()) {
                case CONFLICTED -> RequirementEvaluationOutcome.CONFLICTED;
                case PENDING_REVIEW -> RequirementEvaluationOutcome.PENDING_REVIEW;
                case INSUFFICIENT_EVIDENCE -> RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE;
                case UNKNOWN -> RequirementEvaluationOutcome.UNKNOWN;
            };
        };
    }
}
