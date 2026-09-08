package com.godfrey.ai_immigration_document_analyzer.fact.entity;

/** Canonical timeline event taxonomy (approved specification, section 10). */
public enum TimelineEventType {
    FACT_CREATED,
    FACT_VALIDATION_FAILED,
    FACT_SUSPICIOUS_CONTENT_DETECTED,
    FACT_VERIFIED,
    FACT_REJECTED,
    FACT_CONFLICTED,
    FACT_RESOLVED,
    FACT_SUPERSEDED,
    FACT_RETRACTED,
    FACT_EXPIRED
}
