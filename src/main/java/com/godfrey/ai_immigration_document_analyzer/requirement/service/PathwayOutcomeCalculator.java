package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.NodeResult;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.OutcomeMapping;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * ============================================================================
 * PATHWAY OUTCOME CALCULATOR
 * ============================================================================
 *
 * The ONE place a Pathway's composition-tree {@link NodeResult} is turned
 * into a final {@link RequirementEvaluationOutcome} - extracted unchanged
 * from {@code PathwayAssessmentService.resolvePathwayOutcome}/{@code
 * isMandatory} (Phase 4) so {@code PathwayDiscoveryService}'s transient
 * evaluation can share the exact same rule instead of a second copy of it.
 * {@code PathwayAssessmentService.assess()} now delegates here too - same
 * inputs, same output, zero behavior change.
 * ============================================================================
 */
@Component
@RequiredArgsConstructor
public class PathwayOutcomeCalculator {

    private final RequirementRepository requirementRepository;

    /**
     * PARTIALLY_SATISFIED is a presentation-only refinement applied here,
     * never inside the boolean logic algebra itself (approved specification,
     * section 3): when the tree's genuine result is indeterminate but at
     * least one MANDATORY referenced Requirement is independently SATISFIED,
     * the assessment is presented as partial progress rather than a bare
     * "we don't know".
     */
    public RequirementEvaluationOutcome resolvePathwayOutcome(NodeResult root, Collection<RequirementEvaluation> computedEvaluations) {

        RequirementEvaluationOutcome baseOutcome = OutcomeMapping.fromNodeResult(root);

        boolean indeterminate = baseOutcome == RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE
                || baseOutcome == RequirementEvaluationOutcome.UNKNOWN
                || baseOutcome == RequirementEvaluationOutcome.CONFLICTED
                || baseOutcome == RequirementEvaluationOutcome.PENDING_REVIEW;

        if (!indeterminate) {
            return baseOutcome;
        }

        boolean anyMandatorySatisfied = computedEvaluations.stream()
                .anyMatch(evaluation -> evaluation.getOutcome() == RequirementEvaluationOutcome.SATISFIED
                        && isMandatory(evaluation.getRequirementId()));

        return anyMandatorySatisfied ? RequirementEvaluationOutcome.PARTIALLY_SATISFIED : baseOutcome;
    }

    private boolean isMandatory(Long requirementId) {
        return requirementRepository.findById(requirementId)
                .map(Requirement::getMandatory)
                .map(Boolean.TRUE::equals)
                .orElse(false);
    }
}
