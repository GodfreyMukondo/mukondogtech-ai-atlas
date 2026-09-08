package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The result of evaluating one {@link LogicNode} - a Kleene truth value plus
 * everything needed to explain and combine it: which Facts contributed,
 * which open conflicts blocked it, which derived values were computed, and
 * (only when the value is TRUE/FALSE) a certainty score propagated
 * alongside the boolean using the same AND=min/OR=max/NOT=passthrough
 * combinator as the boolean logic itself (approved specification, section
 * 15's non-inflating propagation rule).
 */
public record NodeResult(
        KleeneValue value,
        IndeterminateFlavor flavor,
        Double certaintyScore,
        Set<Long> contributingFactIds,
        Set<Long> unresolvedConflictIds,
        List<DerivedValueRecord> derivedValues
) {

    public static NodeResult trueResult(double certaintyScore, Set<Long> factIds, List<DerivedValueRecord> derived) {
        return new NodeResult(KleeneValue.TRUE, null, certaintyScore, factIds, Set.of(), derived);
    }

    public static NodeResult falseResult(double certaintyScore, Set<Long> factIds, List<DerivedValueRecord> derived) {
        return new NodeResult(KleeneValue.FALSE, null, certaintyScore, factIds, Set.of(), derived);
    }

    public static NodeResult indeterminate(IndeterminateFlavor flavor, Set<Long> factIds, Set<Long> conflictIds) {
        return new NodeResult(KleeneValue.INDETERMINATE, flavor, null, factIds, conflictIds, List.of());
    }

    public static NodeResult notApplicable() {
        return new NodeResult(KleeneValue.NOT_APPLICABLE, null, null, Set.of(), Set.of(), List.of());
    }

    /** Union of contributing Fact ids and unresolved conflict ids across a set of child results - used by AND/OR/NOT. */
    public static Set<Long> unionFacts(List<NodeResult> children) {
        Set<Long> union = new LinkedHashSet<>();
        for (NodeResult child : children) {
            union.addAll(child.contributingFactIds());
        }
        return union;
    }

    public static Set<Long> unionConflicts(List<NodeResult> children) {
        Set<Long> union = new LinkedHashSet<>();
        for (NodeResult child : children) {
            union.addAll(child.unresolvedConflictIds());
        }
        return union;
    }

    public static List<DerivedValueRecord> unionDerived(List<NodeResult> children) {
        return children.stream()
                .flatMap(child -> child.derivedValues().stream())
                .toList();
    }
}
