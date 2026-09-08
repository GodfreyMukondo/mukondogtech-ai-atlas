package com.godfrey.ai_immigration_document_analyzer.requirement.entity;

/**
 * Lifecycle of a Requirement DEFINITION itself - distinct from
 * {@link RequirementEvaluationOutcome}, which is the lifecycle of applying
 * one definition to one subject.
 */
public enum RequirementStatus {

    /** Authored, not yet trusted for evaluation. */
    DRAFT,

    /** Usable by evaluation and by Pathway composition. */
    PUBLISHED,

    /** No longer current, but retained for historical/temporal evaluation (section 13). */
    DEPRECATED,

    /** Withdrawn - never evaluated, retained only for audit. */
    RETRACTED
}
