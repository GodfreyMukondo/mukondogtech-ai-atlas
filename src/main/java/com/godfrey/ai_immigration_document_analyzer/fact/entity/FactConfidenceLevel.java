package com.godfrey.ai_immigration_document_analyzer.fact.entity;

/**
 * A qualitative summary of {@link Fact#getConfidenceScore()}, computed by
 * {@code ConfidenceCalculator}. Always paired with
 * {@link Fact#getConfidenceExplanation()} - never shown as a bare number.
 */
public enum FactConfidenceLevel {
    LOW,
    MODERATE,
    HIGH,

    /** Confidence scoring does not apply - reserved for SIMULATION-provenance Facts. */
    NOT_APPLICABLE
}
