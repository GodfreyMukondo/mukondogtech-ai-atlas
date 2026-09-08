package com.godfrey.ai_immigration_document_analyzer.fact.entity;

/**
 * How a Fact was verified. Never the same mechanism that created the Fact
 * re-confirming itself (no self-verification).
 */
public enum VerificationMethod {
    INDEPENDENT_EVIDENCE_MATCH,
    HUMAN_REVIEW,
    AUTHORITATIVE_CROSS_CHECK
}
