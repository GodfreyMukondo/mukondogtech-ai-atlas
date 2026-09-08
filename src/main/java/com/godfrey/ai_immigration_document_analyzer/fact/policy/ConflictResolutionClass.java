package com.godfrey.ai_immigration_document_analyzer.fact.policy;

/**
 * The five conflict-handling classes from the approved Conflict
 * Auto-Resolution Policy. A class defines what CAN happen for a field, not
 * what always happens - {@code FactConflictService} still applies the
 * shared confidence-floor/minimum-gap check before any *_AUTO class is
 * allowed to actually auto-resolve; if that check fails, resolution always
 * falls back to human review regardless of class.
 */
public enum ConflictResolutionClass {

    ALWAYS_HUMAN_RESOLUTION,
    EVIDENCE_WEIGHTED_AUTO_RESOLUTION_ALLOWED,
    TEMPORAL_AUTO_RESOLUTION_ALLOWED,
    SAFE_AUTO_RESOLUTION_ALLOWED,
    NEVER_AUTO_RESOLVE,

    /** CASE_PROCESS_INTERACTION-style facts: not competing claims, exempt from the conflict framework. */
    NOT_APPLICABLE
}
