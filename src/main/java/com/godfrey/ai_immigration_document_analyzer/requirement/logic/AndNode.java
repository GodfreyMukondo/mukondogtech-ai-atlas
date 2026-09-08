package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import java.util.List;

/** All children must resolve TRUE (Kleene AND - see {@code LogicEvaluationService}). */
public record AndNode(List<LogicNode> children) implements LogicNode {
}
