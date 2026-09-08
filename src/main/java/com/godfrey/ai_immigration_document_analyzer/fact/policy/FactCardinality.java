package com.godfrey.ai_immigration_document_analyzer.fact.policy;

/**
 * How many concurrently-valid Facts a given factKey may have for one
 * subject, and what "current" means for it (Fact Model specification,
 * temporal validation turn).
 */
public enum FactCardinality {

    /** Exactly one truth at a time (e.g. date of birth). A new value supersedes the old one. */
    SINGLE_CURRENT,

    /** Many over time, but at most one "current" (e.g. employer). Non-overlapping windows are succession, not conflict. */
    HISTORICAL_MULTI_VALUED,

    /** Many, permanently, none superseding another (e.g. qualifications earned). */
    PERMANENTLY_MULTI_VALUED
}
