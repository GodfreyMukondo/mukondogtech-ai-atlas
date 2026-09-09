package com.godfrey.ai_immigration_document_analyzer.caseintelligence.service;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * ============================================================================
 * REQUIREMENT READINESS CALCULATOR
 * ============================================================================
 *
 * The ONE place a {@code RequirementEvaluationOutcome} is reclassified into
 * the UI-facing {@link CaseRequirementSupportStatus} vocabulary, and the ONE
 * place the three-dimension readiness percentage formula lives - extracted
 * unchanged from {@code CaseIntelligenceService.deriveSupportStatus}/{@code
 * buildReadiness} (Phase 4) so {@code PathwayDiscoveryService}'s transient,
 * not-yet-persisted evaluation can share the EXACT SAME classification and
 * the EXACT SAME readiness formula Case Intelligence already uses for a
 * persisted assessment - never a second, competing formula.
 *
 * {@code CaseIntelligenceService} now delegates here too - same inputs, same
 * output, zero behavior change.
 *
 * PATHWAY-AGNOSTIC BY CONSTRUCTION, exactly like the code this was extracted
 * from: nothing below branches on a pathwayKey, requirementKey, or
 * jurisdiction - only the generic outcome/mandatory vocabulary.
 * ============================================================================
 */
@Component
public class RequirementReadinessCalculator {

    /** One evaluated requirement, reduced to only what the readiness formula and support-status mapping need. */
    public record MandatoryOutcome(boolean mandatory, RequirementEvaluationOutcome outcome) {
    }

    public record ReadinessMetrics(
            double requirementCoveragePercent,
            double evidenceCoveragePercent,
            double consistencyPercent,
            double overallReadinessPercent
    ) {
    }

    /**
     * See {@code CaseRequirementSupportStatus}'s own Javadoc for the full
     * mapping table and the documented, approved deviation
     * (NOT_SATISFIED as its own state).
     */
    public CaseRequirementSupportStatus deriveSupportStatus(RequirementEvaluationOutcome outcome, boolean hasContributingFacts) {

        return switch (outcome) {
            case SATISFIED -> CaseRequirementSupportStatus.SATISFIED;
            case PARTIALLY_SATISFIED -> CaseRequirementSupportStatus.PARTIALLY_SUPPORTED;
            case NOT_SATISFIED -> CaseRequirementSupportStatus.NOT_SATISFIED;
            case CONFLICTED -> CaseRequirementSupportStatus.CONFLICTING;
            case EXPIRED, PENDING_REVIEW -> CaseRequirementSupportStatus.NEEDS_VERIFICATION;
            case UNKNOWN, NOT_APPLICABLE -> CaseRequirementSupportStatus.NOT_ASSESSABLE;
            case INSUFFICIENT_EVIDENCE -> hasContributingFacts
                    ? CaseRequirementSupportStatus.NEEDS_VERIFICATION
                    : CaseRequirementSupportStatus.MISSING;
        };
    }

    /** Empty input means "nothing to evaluate yet" - reported as 100% on every dimension, never a divide-by-zero. */
    public ReadinessMetrics computeReadiness(List<MandatoryOutcome> requirements) {

        if (requirements.isEmpty()) {
            return new ReadinessMetrics(100.0, 100.0, 100.0, 100.0);
        }

        List<MandatoryOutcome> mandatory = requirements.stream().filter(MandatoryOutcome::mandatory).toList();

        double requirementCoverage = mandatory.isEmpty()
                ? 100.0
                : percentage(mandatory, r -> r.outcome() == RequirementEvaluationOutcome.SATISFIED);

        double evidenceCoverage =
                percentage(requirements, r -> r.outcome() != RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE);

        double consistency =
                percentage(requirements, r -> r.outcome() != RequirementEvaluationOutcome.CONFLICTED);

        double overall = round((requirementCoverage + evidenceCoverage + consistency) / 3.0);

        return new ReadinessMetrics(round(requirementCoverage), round(evidenceCoverage), round(consistency), overall);
    }

    private double percentage(List<MandatoryOutcome> requirements, Predicate<MandatoryOutcome> predicate) {
        long matching = requirements.stream().filter(predicate).count();
        return (matching * 100.0) / requirements.size();
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
