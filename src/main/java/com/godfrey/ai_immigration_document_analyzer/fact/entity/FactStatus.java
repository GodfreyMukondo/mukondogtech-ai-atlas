package com.godfrey.ai_immigration_document_analyzer.fact.entity;

/**
 * Fact lifecycle status (approved specification, Table F).
 *
 * VERIFIED is deliberately NOT a member of this enum - it is represented as
 * an overlay on {@link Fact} ({@code isVerified}/{@code verifiedAt}/etc.),
 * not a mutually exclusive status, because a Fact must be able to be
 * simultaneously ACCEPTED and verified. Collapsing the two into one status
 * would make that combination unrepresentable.
 */
public enum FactStatus {

    /** Cleared extraction validation and suspicious-content checks; not yet reasoned over. */
    PROPOSED,

    /** Failed deterministic extraction validation. Never becomes visible case data. */
    VALIDATION_FAILED,

    /** Tripped suspicious-content signals; quarantined pending human review. */
    SUSPICIOUS,

    /** Awaiting human action (suspicious-content review, or an ALWAYS_HUMAN/NEVER_AUTO conflict). */
    PENDING_REVIEW,

    /** Two comparable-standing Facts of the same subject+key disagree. */
    CONTESTED,

    /** Active and usable in reasoning, at its stated confidence. */
    ACCEPTED,

    /** Explicitly decided against (lost a conflict, or confirmed invalid on review). Terminal. */
    REJECTED,

    /** Replaced by a newer Fact of the same subject+key. Retained for history. */
    SUPERSEDED,

    /** Explicitly withdrawn by its subject or an authorized reviewer. Retained for audit. */
    RETRACTED
}
