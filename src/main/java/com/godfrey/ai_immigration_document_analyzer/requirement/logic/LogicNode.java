package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * ============================================================================
 * LOGIC NODE
 * ============================================================================
 *
 * The closed grammar a Requirement's {@code applicabilityLogic}/
 * {@code satisfactionLogic}, and a Pathway's {@code compositionLogic}, are
 * expressed in (approved specification, section 5):
 *
 * <pre>
 * LogicExpression :=
 *     Predicate
 *   | AND(LogicExpression, LogicExpression, ...)
 *   | OR(LogicExpression, LogicExpression, ...)
 *   | NOT(LogicExpression)
 *   | REQUIREMENT_REF(requirementId)
 *
 * Predicate :=
 *     FactPredicate(factKey, operator, operand)
 *   | DerivedPredicate(function, args..., operator, operand)
 * </pre>
 *
 * This is DATA, persisted as JSON (see {@code Requirement.applicabilityLogic}/
 * {@code satisfactionLogic}, {@code Pathway.compositionLogic}), interpreted
 * by exactly one trusted evaluator ({@code LogicEvaluationService}) - never
 * executable code, never a scripting language, never something a requirement
 * author can inject arbitrary behavior through.
 * ============================================================================
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "node")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AndNode.class, name = "AND"),
        @JsonSubTypes.Type(value = OrNode.class, name = "OR"),
        @JsonSubTypes.Type(value = NotNode.class, name = "NOT"),
        @JsonSubTypes.Type(value = FactPredicateNode.class, name = "FACT_PREDICATE"),
        @JsonSubTypes.Type(value = DerivedPredicateNode.class, name = "DERIVED_PREDICATE"),
        @JsonSubTypes.Type(value = RequirementRefNode.class, name = "REQUIREMENT_REF")
})
public sealed interface LogicNode
        permits AndNode, OrNode, NotNode, FactPredicateNode, DerivedPredicateNode, RequirementRefNode {
}
