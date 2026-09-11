package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;

/**
 * One Requirement's BEFORE/AFTER comparison within a Scenario Simulation
 * (Phase 5.5). {@code beforeOutcome}/{@code afterOutcome} and both
 * explanations are copied verbatim from the two real {@code
 * RequirementEvaluation} objects the existing, unmodified evaluation engine
 * produced (one real, one over the hypothetical overlay) - never invented,
 * never AI-generated, never a third evaluation.
 *
 * {@code evaluatedInBefore}/{@code evaluatedInAfter} distinguish "this
 * requirement was evaluated and found X" from "this requirement was never
 * reached by the composition/applicability logic in this run at all" - a
 * hypothetical change can itself alter which requirements even apply,
 * which is real, meaningful simulation information and must never be
 * silently collapsed into a false "unchanged".
 */
public record RequirementDeltaRow(
        Long requirementId,
        String requirementKey,
        String requirementTitle,
        boolean mandatory,
        boolean evaluatedInBefore,
        RequirementEvaluationOutcome beforeOutcome,
        String beforeExplanation,
        boolean evaluatedInAfter,
        RequirementEvaluationOutcome afterOutcome,
        String afterExplanation,
        boolean changed
) {
}
