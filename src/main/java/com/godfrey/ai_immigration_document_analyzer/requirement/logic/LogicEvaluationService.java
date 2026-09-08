package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ============================================================================
 * LOGIC EVALUATION SERVICE
 * ============================================================================
 *
 * The single trusted interpreter for {@link LogicNode} trees (approved
 * Requirement/Pathway Architecture Specification, section 5). Pure function
 * of (node, context) - no side effects, no persistence, no authorization
 * decisions. Requirement logic is DATA; this is the only place it is ever
 * interpreted, and it never executes anything beyond the closed
 * operator/function vocabulary defined in {@link PredicateOperator} and
 * {@link DerivedFunction}.
 *
 * Combination uses strong Kleene three-valued logic:
 *
 *   AND: any FALSE wins outright; else any INDETERMINATE wins; else TRUE.
 *   OR:  any TRUE wins outright; else any INDETERMINATE wins; else FALSE.
 *   NOT: flips TRUE/FALSE; INDETERMINATE and NOT_APPLICABLE pass through.
 *
 * NOT_APPLICABLE (a referenced Requirement that does not pertain to this
 * person) is excluded from AND/OR aggregation entirely - it is the identity
 * element for both, so it never forces a false pass or a vacuous true on
 * its own account. If AND/OR is a real 4-way state, and every input is
 * NOT_APPLICABLE, the whole node is NOT_APPLICABLE.
 *
 * When the result is INDETERMINATE, the reported flavor is the
 * highest-severity one present (CONFLICTED > PENDING_REVIEW >
 * INSUFFICIENT_EVIDENCE > UNKNOWN) - see {@link IndeterminateFlavor}.
 * ============================================================================
 */
@Component
public class LogicEvaluationService {

    public NodeResult evaluate(LogicNode node, EvaluationContext context) {

        return switch (node) {
            case AndNode and -> combineAnd(evaluateChildren(and.children(), context));
            case OrNode or -> combineOr(evaluateChildren(or.children(), context));
            case NotNode not -> combineNot(evaluate(not.child(), context));
            case FactPredicateNode factPredicate -> evaluateFactPredicate(factPredicate, context);
            case DerivedPredicateNode derivedPredicate -> evaluateDerivedPredicate(derivedPredicate, context);
            case RequirementRefNode ref -> context.requirementRefResolver().apply(ref.requirementId());
        };
    }

    private List<NodeResult> evaluateChildren(List<LogicNode> children, EvaluationContext context) {
        return children.stream().map(child -> evaluate(child, context)).toList();
    }

    // =========================================================================
    // COMBINATION (Kleene AND / OR / NOT)
    // =========================================================================

    private NodeResult combineAnd(List<NodeResult> children) {

        List<NodeResult> applicable = filterApplicable(children);

        if (applicable.isEmpty()) {
            return NodeResult.notApplicable();
        }

        List<NodeResult> falseChildren = filterByValue(applicable, KleeneValue.FALSE);

        if (!falseChildren.isEmpty()) {
            return NodeResult.falseResult(
                    minCertainty(falseChildren),
                    NodeResult.unionFacts(applicable),
                    NodeResult.unionDerived(applicable)
            );
        }

        List<NodeResult> indeterminateChildren = filterByValue(applicable, KleeneValue.INDETERMINATE);

        if (!indeterminateChildren.isEmpty()) {
            return new NodeResult(
                    KleeneValue.INDETERMINATE,
                    strongestFlavor(indeterminateChildren),
                    null,
                    NodeResult.unionFacts(applicable),
                    NodeResult.unionConflicts(indeterminateChildren),
                    NodeResult.unionDerived(applicable)
            );
        }

        return NodeResult.trueResult(
                minCertainty(applicable),
                NodeResult.unionFacts(applicable),
                NodeResult.unionDerived(applicable)
        );
    }

    private NodeResult combineOr(List<NodeResult> children) {

        List<NodeResult> applicable = filterApplicable(children);

        if (applicable.isEmpty()) {
            return NodeResult.notApplicable();
        }

        List<NodeResult> trueChildren = filterByValue(applicable, KleeneValue.TRUE);

        if (!trueChildren.isEmpty()) {
            return NodeResult.trueResult(
                    maxCertainty(trueChildren),
                    NodeResult.unionFacts(applicable),
                    NodeResult.unionDerived(applicable)
            );
        }

        List<NodeResult> indeterminateChildren = filterByValue(applicable, KleeneValue.INDETERMINATE);

        if (!indeterminateChildren.isEmpty()) {
            return new NodeResult(
                    KleeneValue.INDETERMINATE,
                    strongestFlavor(indeterminateChildren),
                    null,
                    NodeResult.unionFacts(applicable),
                    NodeResult.unionConflicts(indeterminateChildren),
                    NodeResult.unionDerived(applicable)
            );
        }

        return NodeResult.falseResult(
                minCertainty(applicable),
                NodeResult.unionFacts(applicable),
                NodeResult.unionDerived(applicable)
        );
    }

    private NodeResult combineNot(NodeResult child) {

        return switch (child.value()) {
            case TRUE -> NodeResult.falseResult(child.certaintyScore(), child.contributingFactIds(), child.derivedValues());
            case FALSE -> NodeResult.trueResult(child.certaintyScore(), child.contributingFactIds(), child.derivedValues());
            case INDETERMINATE -> new NodeResult(
                    KleeneValue.INDETERMINATE, child.flavor(), null,
                    child.contributingFactIds(), child.unresolvedConflictIds(), child.derivedValues()
            );
            // A requirement that does not apply still does not apply when negated -
            // an exclusion clause that is silent for this person stays silent.
            case NOT_APPLICABLE -> NodeResult.notApplicable();
        };
    }

    private List<NodeResult> filterApplicable(List<NodeResult> children) {
        return children.stream().filter(c -> c.value() != KleeneValue.NOT_APPLICABLE).toList();
    }

    private List<NodeResult> filterByValue(List<NodeResult> children, KleeneValue value) {
        return children.stream().filter(c -> c.value() == value).toList();
    }

    private IndeterminateFlavor strongestFlavor(List<NodeResult> indeterminateChildren) {
        return indeterminateChildren.stream()
                .map(NodeResult::flavor)
                .min((a, b) -> Integer.compare(a.ordinal(), b.ordinal()))
                .orElse(IndeterminateFlavor.UNKNOWN);
    }

    private double minCertainty(List<NodeResult> results) {
        return results.stream()
                .map(NodeResult::certaintyScore)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);
    }

    private double maxCertainty(List<NodeResult> results) {
        return results.stream()
                .map(NodeResult::certaintyScore)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
    }

    // =========================================================================
    // FACT PREDICATE
    // =========================================================================

    private NodeResult evaluateFactPredicate(FactPredicateNode node, EvaluationContext context) {

        String factKey = node.factKey();

        if (context.factView().isConflicted(factKey)) {
            return conflictedResult(context, factKey);
        }

        List<FactResponse> facts = context.factView().factsFor(factKey);

        if (node.operator() == PredicateOperator.NOT_EXISTS) {

            if (facts.isEmpty()) {
                return NodeResult.trueResult(1.0, Set.of(), List.of());
            }

            return NodeResult.falseResult(minConfidence(facts), idsOf(facts), List.of());
        }

        if (facts.isEmpty()) {
            return NodeResult.indeterminate(IndeterminateFlavor.INSUFFICIENT_EVIDENCE, Set.of(), Set.of());
        }

        FactEvidenceExpectation expectation = context.expectationFor(factKey);

        List<FactResponse> qualifying = facts.stream()
                .filter(f -> !expectation.requiresVerification() || Boolean.TRUE.equals(f.isVerified()))
                .filter(f -> ProvenanceTrust.meetsMinimum(f.provenanceType(), expectation.minimumProvenanceType()))
                .toList();

        if (qualifying.isEmpty()) {
            // The Fact exists but does not meet this requirement's declared
            // evidence-quality bar - insufficient evidence, never a "no".
            return NodeResult.indeterminate(IndeterminateFlavor.INSUFFICIENT_EVIDENCE, idsOf(facts), Set.of());
        }

        if (node.operator() == PredicateOperator.EXISTS) {
            return NodeResult.trueResult(minConfidence(qualifying), idsOf(qualifying), List.of());
        }

        List<FactResponse> matching = qualifying.stream()
                .filter(f -> matchesPredicate(f, node))
                .toList();

        if (!matching.isEmpty()) {
            return NodeResult.trueResult(minConfidence(matching), idsOf(matching), List.of());
        }

        return NodeResult.falseResult(minConfidence(qualifying), idsOf(qualifying), List.of());
    }

    private boolean matchesPredicate(FactResponse fact, FactPredicateNode node) {

        return switch (fact.valueType()) {
            case STRING -> matchesString(fact.stringValue(), node);
            case NUMBER -> matchesNumber(fact.numberValue(), node);
            case DATE -> matchesDate(fact.dateValue(), node);
            case BOOLEAN -> matchesBoolean(fact.booleanValue(), node);
        };
    }

    private boolean matchesString(String value, FactPredicateNode node) {

        if (value == null) {
            return false;
        }

        return switch (node.operator()) {
            case EQUALS -> value.equalsIgnoreCase(node.operandValue());
            case NOT_EQUALS -> !value.equalsIgnoreCase(node.operandValue());
            case IN -> node.operandValues() != null && node.operandValues().stream().anyMatch(value::equalsIgnoreCase);
            case CONTAINS -> node.operandValue() != null && value.toLowerCase().contains(node.operandValue().toLowerCase());
            default -> throw new IllegalArgumentException("Operator " + node.operator() + " is not valid for a STRING fact.");
        };
    }

    private boolean matchesNumber(Double value, FactPredicateNode node) {

        if (value == null) {
            return false;
        }

        return matchesNumericOperand(value, node.operator(), node.operandValue(), node.operandLow(), node.operandHigh(), node.operandValues());
    }

    private boolean matchesNumericOperand(
            double value,
            PredicateOperator operator,
            String operandValue,
            String operandLow,
            String operandHigh,
            List<String> operandValues
    ) {

        return switch (operator) {
            case EQUALS -> value == Double.parseDouble(operandValue);
            case NOT_EQUALS -> value != Double.parseDouble(operandValue);
            case AT_LEAST -> value >= Double.parseDouble(operandValue);
            case AT_MOST -> value <= Double.parseDouble(operandValue);
            case BETWEEN -> value >= Double.parseDouble(operandLow) && value <= Double.parseDouble(operandHigh);
            case IN -> operandValues != null && operandValues.stream().anyMatch(v -> Double.parseDouble(v) == value);
            default -> throw new IllegalArgumentException("Operator " + operator + " is not valid for a numeric value.");
        };
    }

    private boolean matchesDate(LocalDateTime value, FactPredicateNode node) {

        if (value == null) {
            return false;
        }

        LocalDate date = value.toLocalDate();

        return switch (node.operator()) {
            case EQUALS -> date.isEqual(LocalDate.parse(node.operandValue()));
            case NOT_EQUALS -> !date.isEqual(LocalDate.parse(node.operandValue()));
            case AT_LEAST -> !date.isBefore(LocalDate.parse(node.operandValue()));
            case AT_MOST -> !date.isAfter(LocalDate.parse(node.operandValue()));
            case BETWEEN -> !date.isBefore(LocalDate.parse(node.operandLow())) && !date.isAfter(LocalDate.parse(node.operandHigh()));
            default -> throw new IllegalArgumentException("Operator " + node.operator() + " is not valid for a DATE fact.");
        };
    }

    private boolean matchesBoolean(Boolean value, FactPredicateNode node) {

        if (value == null) {
            return false;
        }

        return switch (node.operator()) {
            case EQUALS -> value == Boolean.parseBoolean(node.operandValue());
            case NOT_EQUALS -> value != Boolean.parseBoolean(node.operandValue());
            default -> throw new IllegalArgumentException("Operator " + node.operator() + " is not valid for a BOOLEAN fact.");
        };
    }

    // =========================================================================
    // DERIVED PREDICATE
    // =========================================================================

    private NodeResult evaluateDerivedPredicate(DerivedPredicateNode node, EvaluationContext context) {

        return switch (node.function()) {
            case AGE_AT -> evaluateAgeAt(node, context);
            case DURATION_BETWEEN -> evaluateDurationBetween(node, context);
            case WITHIN_VALIDITY_WINDOW -> evaluateWithinValidityWindow(node, context);
            case EFFECTIVE_AS_OF -> evaluateEffectiveAsOf(node, context);
        };
    }

    private NodeResult evaluateAgeAt(DerivedPredicateNode node, EvaluationContext context) {

        String factKey = node.functionArgs().get(0);

        if (context.factView().isConflicted(factKey)) {
            return conflictedResult(context, factKey);
        }

        List<FactResponse> facts = context.factView().factsFor(factKey);

        if (facts.isEmpty()) {
            return NodeResult.indeterminate(IndeterminateFlavor.INSUFFICIENT_EVIDENCE, Set.of(), Set.of());
        }

        FactResponse fact = facts.get(0);

        if (fact.dateValue() == null) {
            return NodeResult.indeterminate(IndeterminateFlavor.UNKNOWN, idsOf(facts), Set.of());
        }

        int age = Period.between(fact.dateValue().toLocalDate(), context.assessmentDate().toLocalDate()).getYears();

        DerivedValueRecord derived = new DerivedValueRecord(
                "AGE_AT(" + factKey + ", " + context.assessmentDate().toLocalDate() + ")",
                List.of(fact.id()),
                String.valueOf(age),
                LocalDateTime.now()
        );

        boolean matches = matchesNumericOperand(age, node.operator(), node.operandValue(), node.operandLow(), node.operandHigh(), null);
        double certainty = minConfidence(List.of(fact));

        return matches
                ? NodeResult.trueResult(certainty, Set.of(fact.id()), List.of(derived))
                : NodeResult.falseResult(certainty, Set.of(fact.id()), List.of(derived));
    }

    private NodeResult evaluateDurationBetween(DerivedPredicateNode node, EvaluationContext context) {

        String factKey = node.functionArgs().get(0);

        if (context.factView().isConflicted(factKey)) {
            return conflictedResult(context, factKey);
        }

        List<FactResponse> facts = context.factView().factsFor(factKey);

        if (facts.isEmpty()) {
            return NodeResult.indeterminate(IndeterminateFlavor.INSUFFICIENT_EVIDENCE, Set.of(), Set.of());
        }

        FactResponse fact = facts.get(0);
        LocalDateTime start = fact.effectiveFrom() != null ? fact.effectiveFrom() : fact.observedAt();

        if (start == null) {
            return NodeResult.indeterminate(IndeterminateFlavor.UNKNOWN, idsOf(facts), Set.of());
        }

        LocalDateTime end = (fact.effectiveTo() != null && fact.effectiveTo().isBefore(context.assessmentDate()))
                ? fact.effectiveTo()
                : context.assessmentDate();

        double years = Duration.between(start, end).toDays() / 365.25;

        DerivedValueRecord derived = new DerivedValueRecord(
                "DURATION_BETWEEN(" + factKey + ", " + context.assessmentDate().toLocalDate() + ")",
                List.of(fact.id()),
                String.format("%.2f years", years),
                LocalDateTime.now()
        );

        boolean matches = matchesNumericOperand(years, node.operator(), node.operandValue(), node.operandLow(), node.operandHigh(), null);
        double certainty = minConfidence(List.of(fact));

        return matches
                ? NodeResult.trueResult(certainty, Set.of(fact.id()), List.of(derived))
                : NodeResult.falseResult(certainty, Set.of(fact.id()), List.of(derived));
    }

    private NodeResult evaluateWithinValidityWindow(DerivedPredicateNode node, EvaluationContext context) {

        String factKey = node.functionArgs().get(0);
        long windowDays = Long.parseLong(node.functionArgs().get(1));

        if (context.factView().isConflicted(factKey)) {
            return conflictedResult(context, factKey);
        }

        List<FactResponse> facts = context.factView().factsFor(factKey);

        if (facts.isEmpty()) {
            return NodeResult.indeterminate(IndeterminateFlavor.INSUFFICIENT_EVIDENCE, Set.of(), Set.of());
        }

        FactResponse fact = facts.get(0);
        long daysSince = ChronoUnit.DAYS.between(fact.observedAt(), context.assessmentDate());
        boolean withinWindow = daysSince <= windowDays;

        DerivedValueRecord derived = new DerivedValueRecord(
                "WITHIN_VALIDITY_WINDOW(" + factKey + ", " + windowDays + "d)",
                List.of(fact.id()),
                daysSince + "d",
                LocalDateTime.now()
        );

        double certainty = minConfidence(List.of(fact));

        return withinWindow
                ? NodeResult.trueResult(certainty, Set.of(fact.id()), List.of(derived))
                : NodeResult.falseResult(certainty, Set.of(fact.id()), List.of(derived));
    }

    private NodeResult evaluateEffectiveAsOf(DerivedPredicateNode node, EvaluationContext context) {

        String factKey = node.functionArgs().get(0);

        if (context.factView().isConflicted(factKey)) {
            return conflictedResult(context, factKey);
        }

        List<FactResponse> facts = context.factView().factsFor(factKey);

        if (facts.isEmpty()) {
            return NodeResult.indeterminate(IndeterminateFlavor.INSUFFICIENT_EVIDENCE, Set.of(), Set.of());
        }

        return NodeResult.trueResult(minConfidence(facts), idsOf(facts), List.of());
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private NodeResult conflictedResult(EvaluationContext context, String factKey) {

        Long conflictId = context.factView().openConflictIdByFactKey().get(factKey);

        return NodeResult.indeterminate(
                IndeterminateFlavor.CONFLICTED,
                Set.of(),
                conflictId == null ? Set.of() : Set.of(conflictId)
        );
    }

    private Set<Long> idsOf(List<FactResponse> facts) {

        Set<Long> ids = new LinkedHashSet<>();

        for (FactResponse fact : facts) {
            ids.add(fact.id());
        }

        return ids;
    }

    private double minConfidence(List<FactResponse> facts) {

        return facts.stream()
                .map(FactResponse::confidenceScore)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);
    }
}
