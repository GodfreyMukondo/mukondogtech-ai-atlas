package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

/**
 * Reuses another Requirement's evaluation OUTCOME as a boolean input - this
 * is how a Requirement's own COMPOSITE logic, and every Pathway's
 * compositionLogic, reference other Requirements without duplicating their
 * definitions (approved specification, sections 5 and 8).
 */
public record RequirementRefNode(Long requirementId) implements LogicNode {
}
