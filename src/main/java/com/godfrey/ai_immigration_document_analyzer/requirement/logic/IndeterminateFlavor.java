package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

/**
 * Which specific reason a {@link KleeneValue#INDETERMINATE} node result
 * carries. When combining nodes, the highest-severity flavor present wins -
 * declared here in descending severity order and consumed via
 * {@link #ordinal()} by {@code LogicEvaluationService}: a data-integrity or
 * policy block ({@code CONFLICTED}/{@code PENDING_REVIEW}) outranks "just
 * need more evidence" ({@code INSUFFICIENT_EVIDENCE}), which outranks the
 * internal catch-all ({@code UNKNOWN}).
 */
public enum IndeterminateFlavor {
    CONFLICTED,
    PENDING_REVIEW,
    INSUFFICIENT_EVIDENCE,
    UNKNOWN
}
