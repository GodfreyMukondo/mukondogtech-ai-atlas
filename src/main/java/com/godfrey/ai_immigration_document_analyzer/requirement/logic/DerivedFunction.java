package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

/**
 * Closed set of temporal/derivation functions a {@link DerivedPredicateNode}
 * may invoke (approved specification, section 4/5). A derived value computed
 * by one of these functions is never persisted as if it were a Fact - it
 * exists only as a {@code DerivedValueRecord} scoped to one evaluation.
 */
public enum DerivedFunction {

    /** Age in whole years of a DATE-valued Fact, as of the evaluation's assessment date. */
    AGE_AT,

    /** Duration in whole years between a Fact's effective window and the assessment date. */
    DURATION_BETWEEN,

    /** Whether a Fact's observation is still within a validity window (days) as of the assessment date. */
    WITHIN_VALIDITY_WINDOW,

    /** Whether a Fact's effective window covers the assessment date at all. */
    EFFECTIVE_AS_OF
}
