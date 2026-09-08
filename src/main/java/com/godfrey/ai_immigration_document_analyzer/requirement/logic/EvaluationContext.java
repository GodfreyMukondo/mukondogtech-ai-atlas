package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;

/**
 * Everything {@code LogicEvaluationService} needs to evaluate one
 * {@link LogicNode} tree, threaded through recursive evaluation without the
 * interpreter ever depending on a persistence/service type directly.
 *
 * {@code requirementRefResolver} is how {@link RequirementRefNode} is
 * resolved: it is supplied by the orchestrating service (never implemented
 * here), which owns cycle detection and memoization for reused
 * Requirements - the interpreter itself stays a pure function of
 * (node, context).
 */
public record EvaluationContext(
        Long subjectUserId,
        LocalDateTime assessmentDate,
        EvaluationFactView factView,
        Map<String, FactEvidenceExpectation> activeEvidenceExpectations,
        Function<Long, NodeResult> requirementRefResolver
) {

    /** A copy of this context scoped to a different Requirement's own evidence expectations. */
    public EvaluationContext withEvidenceExpectations(Map<String, FactEvidenceExpectation> expectations) {
        return new EvaluationContext(subjectUserId, assessmentDate, factView, expectations, requirementRefResolver);
    }

    public FactEvidenceExpectation expectationFor(String factKey) {
        return activeEvidenceExpectations().getOrDefault(factKey, FactEvidenceExpectation.none());
    }
}
