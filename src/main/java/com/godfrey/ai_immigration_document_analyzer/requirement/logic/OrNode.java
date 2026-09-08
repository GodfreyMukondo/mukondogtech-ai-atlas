package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import java.util.List;

/** Any child resolving TRUE wins outright (Kleene OR - see {@code LogicEvaluationService}). */
public record OrNode(List<LogicNode> children) implements LogicNode {
}
