package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import java.util.List;

/**
 * A leaf predicate over a computed (derived) value - e.g. age from a date of
 * birth. The computation itself is one of the closed {@link DerivedFunction}s,
 * never arbitrary code; its result is recorded as a transient
 * {@code DerivedValueRecord} on the owning evaluation, never as a Fact.
 */
public record DerivedPredicateNode(
        DerivedFunction function,
        List<String> functionArgs,
        PredicateOperator operator,
        String operandValue,
        String operandLow,
        String operandHigh
) implements LogicNode {
}
