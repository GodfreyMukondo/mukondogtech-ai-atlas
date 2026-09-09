package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

/**
 * ============================================================================
 * CASE REQUIREMENT SUPPORT STATUS
 * ============================================================================
 *
 * A Case Intelligence-specific classification of how well the subject's
 * CURRENT evidence supports one Requirement of a Pathway - deliberately a
 * SEPARATE vocabulary from {@code RequirementEvaluationOutcome}, never a
 * renaming of it. {@code RequirementEvaluationOutcome} remains the single
 * source of truth produced by {@code RequirementEvaluationService}/
 * {@code LogicEvaluationService}; this enum is a read-time, UI-facing
 * reclassification derived FROM that outcome (see
 * {@code CaseIntelligenceService.deriveSupportStatus}), never a second
 * evaluation of anything.
 *
 * Mapping from the nine-state {@code RequirementEvaluationOutcome}:
 *
 *   SATISFIED            -> SATISFIED
 *   PARTIALLY_SATISFIED   -> PARTIALLY_SUPPORTED
 *   NOT_SATISFIED         -> NOT_SATISFIED (see note below)
 *   INSUFFICIENT_EVIDENCE -> MISSING (no fact recorded for a bound key) or
 *                            NEEDS_VERIFICATION (a fact exists but does not
 *                            meet the requirement's declared verification/
 *                            provenance bar) - distinguished by whether the
 *                            requirement's own contributing-facts list
 *                            already contains an entry for the bound key.
 *   CONFLICTED            -> CONFLICTING
 *   EXPIRED, PENDING_REVIEW -> NEEDS_VERIFICATION
 *   UNKNOWN, NOT_APPLICABLE -> NOT_ASSESSABLE
 *
 * DELIBERATE DEVIATION FROM THE REQUESTED SIX-VALUE LIST: {@code NOT_SATISFIED}
 * is an addition beyond the six statuses originally specified
 * (SATISFIED/PARTIALLY_SUPPORTED/MISSING/CONFLICTING/NEEDS_VERIFICATION/
 * NOT_ASSESSABLE). It was added because Phase 1's architecture treats
 * "the evidence is sufficient and definitively fails the logic"
 * (NOT_SATISFIED) as categorically different from "evidence is missing or
 * insufficient" (INSUFFICIENT_EVIDENCE/MISSING) - collapsing the two would
 * violate the explicit, approved invariant that INSUFFICIENT_EVIDENCE must
 * NEVER be treated as NOT_SATISFIED, and would let a genuine, evidenced
 * non-satisfaction hide inside a vaguer "MISSING" label. This was flagged
 * to the requester as an assumption requiring approval rather than applied
 * silently.
 * ============================================================================
 */
public enum CaseRequirementSupportStatus {

    /** The available evidence fully meets this requirement. */
    SATISFIED,

    /** Some, but not all, parts of a composite requirement are currently supported. */
    PARTIALLY_SUPPORTED,

    /** The evidence is sufficient, accepted, and definitively does not meet this requirement. */
    NOT_SATISFIED,

    /** No fact has been recorded yet for evidence this requirement depends on. */
    MISSING,

    /** A fact this requirement depends on has an open, unresolved conflict. Never fraud. */
    CONFLICTING,

    /** Evidence exists but does not yet meet the declared verification/provenance/currency bar. */
    NEEDS_VERIFICATION,

    /** This requirement does not apply to this person, or its evaluation could not be completed. */
    NOT_ASSESSABLE
}
