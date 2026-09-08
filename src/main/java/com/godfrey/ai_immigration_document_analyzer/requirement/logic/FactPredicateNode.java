package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import java.util.List;

/**
 * A leaf predicate over one Fact key. Exactly one of
 * {@code operandValue}/{@code operandValues}/({@code operandLow},{@code operandHigh})
 * is populated, selected by {@code operator} - the same "strongly typed, no
 * generic payload" discipline as {@code Fact} itself. All operands are
 * carried as strings and parsed/compared according to the resolved Fact's
 * actual {@code FactValueType} at evaluation time.
 */
public record FactPredicateNode(
        String factKey,
        PredicateOperator operator,
        String operandValue,
        List<String> operandValues,
        String operandLow,
        String operandHigh
) implements LogicNode {
}
