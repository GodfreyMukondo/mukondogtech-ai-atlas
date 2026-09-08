package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

/** Flips TRUE/FALSE; INDETERMINATE and NOT_APPLICABLE pass through unchanged. */
public record NotNode(LogicNode child) implements LogicNode {
}
