package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

/**
 * Strong Kleene three-valued logic used to combine {@link LogicNode} results
 * (approved specification, section 5's resolved combination rule). Mapped to
 * a {@code RequirementEvaluationOutcome} only once combination is complete -
 * see {@code LogicEvaluationService}.
 */
public enum KleeneValue {
    TRUE,
    FALSE,
    INDETERMINATE,

    /**
     * Identity element for both AND and OR: a referenced Requirement whose
     * own outcome is NOT_APPLICABLE contributes nothing to its parent's
     * aggregation. If every child of an AND/OR is NOT_APPLICABLE, the
     * parent itself becomes NOT_APPLICABLE rather than vacuously TRUE/FALSE.
     */
    NOT_APPLICABLE
}
