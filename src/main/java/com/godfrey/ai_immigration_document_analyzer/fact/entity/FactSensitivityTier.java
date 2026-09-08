package com.godfrey.ai_immigration_document_analyzer.fact.entity;

/**
 * Sensitivity classification (approved specification, Table B1). Assigned
 * per {@code factKey} via {@code FactTypeRegistry}, and copied onto the
 * Fact at creation for query performance and audit stability.
 */
public enum FactSensitivityTier {

    /** Low-consequence administrative facts. */
    T1_ROUTINE,

    /** Ordinary case-building facts. */
    T2_STANDARD_PERSONAL,

    /** Materially affects legal outcome or is identity-theft-relevant. */
    T3_SENSITIVE,

    /** Legal/health-adjacent, highest harm if mishandled. */
    T4_HIGHLY_SENSITIVE
}
