package com.godfrey.ai_immigration_document_analyzer.fact.entity;

/**
 * The strongly typed shape of a Fact's value. Exactly one of the
 * corresponding typed columns on {@link Fact} is populated, selected by
 * this value - deliberately not a generic JSON payload.
 */
public enum FactValueType {
    STRING,
    DATE,
    NUMBER,
    BOOLEAN
}
