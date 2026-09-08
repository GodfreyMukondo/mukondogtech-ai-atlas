package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.VerificationMethod;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.ConflictPolicyRegistry;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * CONFIDENCE CALCULATOR
 * ============================================================================
 *
 * Implements the approved confidence model. Deliberately NOT "AI confidence
 * = a raw percentage": the score is derived from named, inspectable inputs
 * (provenance tier, evidence corroboration, verification, staleness,
 * contested state), and every result carries a human-readable explanation
 * of which of those inputs applied - the explanation, not the number, is
 * the primary artifact a user should see.
 *
 * Confidence is kept structurally distinct from verification (an overlay
 * this calculator only reads, never writes), authority/truth (this
 * computes trust in a claim, not whether it is objectively correct),
 * eligibility (a Requirement/Assessment concern, out of scope here), and
 * recommendation (a Next-Best-Action concern, out of scope here).
 *
 * A CONTESTED fact's score is force-capped regardless of the formula -
 * lifecycle state constrains confidence, confidence never overrides
 * lifecycle state.
 * ============================================================================
 */
@Component
public class ConfidenceCalculator {

    private static final double MAX_SCORE = 0.97;
    private static final double MIN_SCORE = 0.05;

    private static final double EVIDENCE_BONUS_PER_ITEM = 0.05;
    private static final double EVIDENCE_BONUS_CAP = 0.15;

    private static final double VERIFIED_HUMAN_REVIEW_FLOOR = 0.90;
    private static final double VERIFIED_AUTHORITATIVE_FLOOR = 0.90;
    private static final double VERIFIED_EVIDENCE_MATCH_FLOOR = 0.80;

    private static final double STALENESS_DECAY_FACTOR = 0.85;
    private static final double STALENESS_FLOOR = 0.20;

    public record Input(
            FactProvenanceType provenanceType,
            int independentEvidenceCount,
            boolean verified,
            VerificationMethod verificationMethod,
            boolean contested,
            Long daysSinceLastObserved,
            Integer stalenessThresholdDays
    ) {
    }

    public record Result(
            double score,
            FactConfidenceLevel level,
            String explanation
    ) {
    }

    public Result compute(Input input) {

        if (input.provenanceType() == FactProvenanceType.SIMULATION) {

            return new Result(
                    0.0,
                    FactConfidenceLevel.NOT_APPLICABLE,
                    "Hypothetical fact - not scored for real-world confidence."
            );
        }

        List<String> reasons = new ArrayList<>();

        double score = baseScoreFor(input.provenanceType(), reasons);

        score += corroborationBonus(input.independentEvidenceCount(), reasons);

        score = Math.min(score, MAX_SCORE);

        if (input.verified()) {
            score = applyVerificationFloor(score, input.verificationMethod(), reasons);
        }

        boolean stale = isStale(input.daysSinceLastObserved(), input.stalenessThresholdDays());

        if (stale) {
            score = Math.max(score * STALENESS_DECAY_FACTOR, STALENESS_FLOOR);
            reasons.add(
                    "last confirmed " + input.daysSinceLastObserved()
                            + " days ago and may be outdated"
            );
        }

        if (input.contested()) {

            score = Math.min(score, ConflictPolicyRegistry.CONTESTED_CONFIDENCE_CAP);
            reasons.add("disputed - conflicting information exists and requires review");
        }

        score = clamp(score);

        return new Result(score, levelFor(score), buildExplanation(reasons));
    }

    private double baseScoreFor(FactProvenanceType provenanceType, List<String> reasons) {

        return switch (provenanceType) {

            case EXTERNAL_AUTHORITATIVE_SOURCE -> {
                reasons.add("sourced from an authoritative source");
                yield 0.90;
            }

            case HUMAN_VERIFICATION -> {
                reasons.add("directly attested by a human reviewer");
                yield 0.85;
            }

            case DOCUMENT_EXTRACTION -> {
                reasons.add("extracted from an uploaded document, not yet independently confirmed");
                yield 0.60;
            }

            case USER_INPUT -> {
                reasons.add("self-reported, not yet confirmed by evidence");
                yield 0.50;
            }

            case DERIVED_FACT -> {
                reasons.add("derived by reasoning over other facts");
                yield 0.50;
            }

            case SYSTEM_PROCESS -> {
                reasons.add("produced by an internal system process");
                yield 0.55;
            }

            case ASSUMPTION -> {
                reasons.add("temporarily assumed in the absence of real information");
                yield 0.30;
            }

            case SIMULATION -> 0.0; // unreachable - handled earlier
        };
    }

    private double corroborationBonus(int independentEvidenceCount, List<String> reasons) {

        int corroborating = Math.max(independentEvidenceCount - 1, 0);

        if (corroborating <= 0) {
            return 0.0;
        }

        double bonus = Math.min(corroborating * EVIDENCE_BONUS_PER_ITEM, EVIDENCE_BONUS_CAP);

        reasons.add("corroborated by " + independentEvidenceCount + " independent sources");

        return bonus;
    }

    private double applyVerificationFloor(
            double score,
            VerificationMethod method,
            List<String> reasons
    ) {

        if (method == null) {
            return score;
        }

        double floor = switch (method) {
            case HUMAN_REVIEW -> VERIFIED_HUMAN_REVIEW_FLOOR;
            case AUTHORITATIVE_CROSS_CHECK -> VERIFIED_AUTHORITATIVE_FLOOR;
            case INDEPENDENT_EVIDENCE_MATCH -> VERIFIED_EVIDENCE_MATCH_FLOOR;
        };

        reasons.add("verified via " + method.name().toLowerCase().replace('_', ' '));

        return Math.max(score, floor);
    }

    private boolean isStale(Long daysSinceLastObserved, Integer stalenessThresholdDays) {

        return stalenessThresholdDays != null
                && daysSinceLastObserved != null
                && daysSinceLastObserved > stalenessThresholdDays;
    }

    private FactConfidenceLevel levelFor(double score) {

        if (score >= 0.8) {
            return FactConfidenceLevel.HIGH;
        }

        if (score >= 0.5) {
            return FactConfidenceLevel.MODERATE;
        }

        return FactConfidenceLevel.LOW;
    }

    private double clamp(double score) {
        return Math.min(Math.max(score, MIN_SCORE), MAX_SCORE);
    }

    private String buildExplanation(List<String> reasons) {

        if (reasons.isEmpty()) {
            return "Confidence basis unavailable.";
        }

        String joined = String.join("; ", reasons);

        return Character.toUpperCase(joined.charAt(0)) + joined.substring(1) + ".";
    }
}
