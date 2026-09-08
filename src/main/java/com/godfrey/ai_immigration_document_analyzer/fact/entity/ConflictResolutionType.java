package com.godfrey.ai_immigration_document_analyzer.fact.entity;

/**
 * How a {@link FactConflict} was resolved. Recorded permanently for audit -
 * never omitted, regardless of how "routine" the resolution felt.
 */
public enum ConflictResolutionType {
    EVIDENCE_WEIGHTED_AUTO,
    TEMPORAL_AUTO,
    SAFE_AUTO,
    HUMAN_DECISION
}
