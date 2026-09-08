package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.VerificationMethod;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.ConflictPolicyRegistry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link ConfidenceCalculator}.
 *
 * The central property under test is that confidence is a distinct axis
 * from verification, authority/truth, eligibility and recommendation: a
 * Fact can have high confidence without being verified, and verification
 * only ever raises a floor - it never substitutes for the underlying
 * evidence-based score. A CONTESTED fact's score is always capped
 * regardless of how strong its other inputs are.
 */
class ConfidenceCalculatorTest {

    private final ConfidenceCalculator calculator = new ConfidenceCalculator();

    private ConfidenceCalculator.Input input(
            FactProvenanceType provenanceType,
            int evidenceCount,
            boolean verified,
            VerificationMethod method,
            boolean contested,
            Long daysSinceLastObserved,
            Integer stalenessThresholdDays
    ) {
        return new ConfidenceCalculator.Input(
                provenanceType, evidenceCount, verified, method, contested, daysSinceLastObserved, stalenessThresholdDays
        );
    }

    @Test
    void selfReportedUnverifiedFactIsModerateConfidence() {

        ConfidenceCalculator.Result result = calculator.compute(
                input(FactProvenanceType.USER_INPUT, 1, false, null, false, 0L, null)
        );

        assertThat(result.score()).isCloseTo(0.50, within(0.001));
        assertThat(result.level()).isEqualTo(FactConfidenceLevel.MODERATE);
        assertThat(result.explanation()).containsIgnoringCase("self-reported");
    }

    @Test
    void externalAuthoritativeSourceIsHighConfidenceByDefault() {

        ConfidenceCalculator.Result result = calculator.compute(
                input(FactProvenanceType.EXTERNAL_AUTHORITATIVE_SOURCE, 1, false, null, false, 0L, null)
        );

        assertThat(result.score()).isCloseTo(0.90, within(0.001));
        assertThat(result.level()).isEqualTo(FactConfidenceLevel.HIGH);
    }

    @Test
    void documentExtractionAloneIsNotAutomaticallyVerifiedOrHighConfidence() {

        // A "high-confidence extraction" must never be conflated with a
        // verified Fact: this result carries no verified flag at all (the
        // calculator only ever reads verification state, never sets it),
        // and an unverified extraction alone lands at MODERATE, not HIGH.
        ConfidenceCalculator.Result result = calculator.compute(
                input(FactProvenanceType.DOCUMENT_EXTRACTION, 1, false, null, false, 0L, null)
        );

        assertThat(result.score()).isCloseTo(0.60, within(0.001));
        assertThat(result.level()).isEqualTo(FactConfidenceLevel.MODERATE);
    }

    @Test
    void corroboratingEvidenceAddsABoundedBonus() {

        ConfidenceCalculator.Result oneSource = calculator.compute(
                input(FactProvenanceType.USER_INPUT, 1, false, null, false, 0L, null)
        );

        ConfidenceCalculator.Result threeSources = calculator.compute(
                input(FactProvenanceType.USER_INPUT, 3, false, null, false, 0L, null)
        );

        assertThat(threeSources.score()).isGreaterThan(oneSource.score());
        assertThat(threeSources.score()).isCloseTo(0.60, within(0.001)); // 0.50 + min(2*0.05, 0.15)
    }

    @Test
    void verificationRaisesAFloorButNeverLowersAnAlreadyHigherScore() {

        ConfidenceCalculator.Result verifiedLowBase = calculator.compute(
                input(FactProvenanceType.USER_INPUT, 1, true, VerificationMethod.HUMAN_REVIEW, false, 0L, null)
        );

        assertThat(verifiedLowBase.score()).isCloseTo(0.90, within(0.001));

        ConfidenceCalculator.Result verifiedHighBase = calculator.compute(
                input(FactProvenanceType.EXTERNAL_AUTHORITATIVE_SOURCE, 1, true, VerificationMethod.INDEPENDENT_EVIDENCE_MATCH, false, 0L, null)
        );

        // The 0.80 floor for INDEPENDENT_EVIDENCE_MATCH must not drag a 0.90 base down.
        assertThat(verifiedHighBase.score()).isCloseTo(0.90, within(0.001));
    }

    @Test
    void contestedFactScoreIsAlwaysCappedRegardlessOfOtherInputs() {

        ConfidenceCalculator.Result result = calculator.compute(
                input(FactProvenanceType.EXTERNAL_AUTHORITATIVE_SOURCE, 5, true, VerificationMethod.AUTHORITATIVE_CROSS_CHECK, true, 0L, null)
        );

        assertThat(result.score()).isLessThanOrEqualTo(ConflictPolicyRegistry.CONTESTED_CONFIDENCE_CAP);
        assertThat(result.explanation()).containsIgnoringCase("disputed");
    }

    @Test
    void staleFactScoreDecaysBelowItsFreshValue() {

        ConfidenceCalculator.Result fresh = calculator.compute(
                input(FactProvenanceType.USER_INPUT, 1, false, null, false, 10L, 365)
        );

        ConfidenceCalculator.Result stale = calculator.compute(
                input(FactProvenanceType.USER_INPUT, 1, false, null, false, 400L, 365)
        );

        assertThat(stale.score()).isLessThan(fresh.score());
        assertThat(stale.explanation()).containsIgnoringCase("outdated");
    }

    @Test
    void simulationProvenanceIsNeverScoredForRealWorldConfidence() {

        ConfidenceCalculator.Result result = calculator.compute(
                input(FactProvenanceType.SIMULATION, 5, true, VerificationMethod.HUMAN_REVIEW, false, 0L, null)
        );

        assertThat(result.score()).isZero();
        assertThat(result.level()).isEqualTo(FactConfidenceLevel.NOT_APPLICABLE);
    }
}
