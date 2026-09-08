package com.godfrey.ai_immigration_document_analyzer.requirement.entity;

/**
 * Canonical Requirement taxonomy (Requirement/Pathway Architecture
 * Specification, section 2) - the smallest set of categories that actually
 * changes evaluation behavior or evidence expectations, not an exhaustive
 * list of every immigration concept.
 */
public enum RequirementType {

    /** Fact-derived attribute test: age, nationality, residence status, identity. */
    ELIGIBILITY_ATTRIBUTE,

    /** Education, professional licensing, certification - evidence-heavy, document-backed. */
    CREDENTIAL,

    /** Employment/occupation history with a temporal-duration component. */
    EXPERIENCE,

    /** Financial capacity, salary/income thresholds - numeric-threshold, currency/jurisdiction-sensitive. */
    CAPACITY,

    /** Language or skills testing - score-band logic, naturally staleness-sensitive. */
    PROFICIENCY,

    /**
     * Family/sponsorship relationships. Deferred in this implementation
     * phase: evaluating it correctly needs a second subject's Facts, which
     * the single-subject Fact model does not yet resolve (see architectural
     * decisions in the implementation report).
     */
    RELATIONSHIP,

    /** Documentation/filing/timing conditions tied to process, not personal attributes. */
    PROCEDURAL,

    /** Admissibility, prior refusals, legal/administrative history - highest sensitivity. */
    LEGAL_STANDING,

    /** A requirement whose entire content is a LogicExpression over other Requirements. */
    COMPOSITE
}
