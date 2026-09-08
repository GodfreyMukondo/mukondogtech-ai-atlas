package com.godfrey.ai_immigration_document_analyzer.requirement.entity;

/**
 * ============================================================================
 * REQUIREMENT EVALUATION OUTCOME
 * ============================================================================
 *
 * Approved Requirement/Pathway Architecture Specification, section 3.
 *
 * These nine states are deliberately NOT collapsible into a boolean. In
 * particular:
 *
 * - {@link #INSUFFICIENT_EVIDENCE} is never treated as {@link #NOT_SATISFIED}:
 *   it means the Digital Twin has no (or no sufficiently evidenced) Fact for
 *   something this requirement needs - the logic was never even evaluated
 *   against an absent value.
 * - {@link #UNKNOWN} is never treated as {@link #NOT_SATISFIED} either: it
 *   means the evaluation itself could not be completed (e.g. an unresolved
 *   dependency), not that the Facts are missing or that the answer is no.
 * - {@link #CONFLICTED} means competing evidence exists for a Fact this
 *   requirement depends on - it is never interpreted as fraud, and the
 *   evaluator never silently picks a side.
 * ============================================================================
 */
public enum RequirementEvaluationOutcome {

    /** The relevant Facts, evaluated under the requirement's logic, resolve to true with adequate evidence. */
    SATISFIED,

    /** The relevant Facts are known, accepted, sufficiently evidenced, and definitively fail the logic. */
    NOT_SATISFIED,

    /** Composite-only: some branches of an AND are SATISFIED, others are not yet resolvable (not a definite failure). */
    PARTIALLY_SATISFIED,

    /** No (or not sufficiently evidenced) ACCEPTED Fact exists for something this requirement needs. Never NOT_SATISFIED. */
    INSUFFICIENT_EVIDENCE,

    /** The evaluation cannot be resolved for a reason other than missing Facts (e.g. an unresolved dependency). */
    UNKNOWN,

    /** A Fact this requirement depends on has an open, unresolved conflict. Never interpreted as fraud. */
    CONFLICTED,

    /** The requirement's applicability condition evaluates false - it does not pertain to this person at all. */
    NOT_APPLICABLE,

    /** The requirement was satisfiable, but a supporting Fact's validity window has lapsed relative to the assessment date. */
    EXPIRED,

    /** Policy requires human sign-off before an automatic SATISFIED/NOT_SATISFIED may be recorded. */
    PENDING_REVIEW
}
