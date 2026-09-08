package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;

import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * EVALUATION CERTAINTY CALCULATOR
 * ============================================================================
 *
 * Requirement/Pathway Architecture Specification, section 15: Requirement
 * evaluation certainty is a DISTINCT axis from Fact confidence, not a copy
 * of it. {@code LogicEvaluationService} already propagates the minimum (for
 * AND) / maximum (for OR) Fact confidence along the winning branch of a
 * logic tree - this calculator applies the one additional, requirement-level
 * penalty that a pure Fact-confidence propagation cannot know about: how
 * trustworthy the REGULATORY VERSION behind the rule itself is. An
 * AI-generated, not-yet-verified reading of a regulation can never produce
 * the same certainty as a directly-confirmed one, no matter how strong the
 * underlying Facts are.
 * ============================================================================
 */
@Component
public class EvaluationCertaintyCalculator {

    private static final double AUTHORITATIVE_CONFIRMED_FACTOR = 1.0;
    private static final double HUMAN_VERIFIED_FACTOR = 0.95;
    private static final double UNVERIFIED_INGESTION_FACTOR = 0.75;

    public record Result(Double score, EvaluationCertaintyLevel level) {
    }

    /** For a definitive (SATISFIED/NOT_SATISFIED) outcome only. */
    public Result forDefiniteOutcome(double rawCertaintyScore, RegulatoryVerificationStatus verificationStatus) {

        double factor = switch (verificationStatus) {
            case AUTHORITATIVE_CONFIRMED -> AUTHORITATIVE_CONFIRMED_FACTOR;
            case HUMAN_VERIFIED -> HUMAN_VERIFIED_FACTOR;
            case UNVERIFIED_INGESTION -> UNVERIFIED_INGESTION_FACTOR;
        };

        double adjusted = clamp(rawCertaintyScore * factor);

        return new Result(adjusted, levelFor(adjusted));
    }

    /** For every other outcome (INSUFFICIENT_EVIDENCE, UNKNOWN, CONFLICTED, NOT_APPLICABLE, EXPIRED, PENDING_REVIEW). */
    public Result notApplicable() {
        return new Result(null, EvaluationCertaintyLevel.NOT_APPLICABLE);
    }

    private EvaluationCertaintyLevel levelFor(double score) {

        if (score >= 0.8) {
            return EvaluationCertaintyLevel.HIGH;
        }

        if (score >= 0.5) {
            return EvaluationCertaintyLevel.MODERATE;
        }

        return EvaluationCertaintyLevel.LOW;
    }

    private double clamp(double value) {
        return Math.min(Math.max(value, 0.0), 1.0);
    }
}
