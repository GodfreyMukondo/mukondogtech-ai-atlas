package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

/**
 * Closed operator vocabulary for {@link FactPredicateNode}/{@link DerivedPredicateNode}
 * (approved specification, section 5). Requirement logic is data interpreted
 * by {@code LogicEvaluationService}, never executable code - adding an
 * operator is a reviewed code change here, not something a requirement
 * author can inject.
 */
public enum PredicateOperator {

    EQUALS,
    NOT_EQUALS,
    AT_LEAST,
    AT_MOST,
    BETWEEN,
    IN,
    CONTAINS,
    EXISTS,
    NOT_EXISTS
}
