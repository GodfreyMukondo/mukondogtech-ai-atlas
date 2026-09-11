package com.godfrey.ai_immigration_document_analyzer.fact.entity;

/**
 * How a {@link FactConflict} was resolved. Recorded permanently for audit -
 * never omitted, regardless of how "routine" the resolution felt.
 *
 * {@link #APPLICANT_CONFIRMATION} and {@link #HUMAN_DECISION} are
 * deliberately distinct values, never interchangeable: HUMAN_DECISION is a
 * staff (CASE_WORKER) adjudication reached via
 * {@code FactAuthorizationService.assertCanVerifyOrResolve}; APPLICANT_
 * CONFIRMATION is the subject's own statement of which value they believe
 * is correct, reached via the separate {@code assertCanApplicantConfirmConflict}
 * grant. Neither ever sets a Fact's {@code isVerified} overlay - that is
 * only ever set by the independent, staff-only verifyFact operation. A Fact
 * whose conflict was resolved by APPLICANT_CONFIRMATION therefore remains
 * distinguishable from independently verified data for as long as
 * {@code isVerified} stays false.
 */
public enum ConflictResolutionType {
    EVIDENCE_WEIGHTED_AUTO,
    TEMPORAL_AUTO,
    SAFE_AUTO,
    HUMAN_DECISION,

    /**
     * The applicant (subject) stated which of the two competing values they
     * believe is correct. Self-reported provenance, never independent
     * documentary verification, never a case-worker/administrator decision.
     */
    APPLICANT_CONFIRMATION
}
