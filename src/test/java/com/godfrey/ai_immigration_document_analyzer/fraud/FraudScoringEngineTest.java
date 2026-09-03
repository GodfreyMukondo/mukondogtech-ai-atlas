package com.godfrey.ai_immigration_document_analyzer.fraud;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link FraudScoringEngine}, the rule-based keyword scorer
 * used as a fast, dependency-free fraud signal.
 */
class FraudScoringEngineTest {

    private final FraudScoringEngine engine = new FraudScoringEngine();

    @Test
    void blankTextProducesZeroScoreAndNoFraudFlag() {

        FraudScoreResult result = engine.analyze("  ");

        assertThat(result.score()).isZero();
        assertThat(result.fraudDetected()).isFalse();
        assertThat(result.ruleId()).isEqualTo("NONE");
    }

    @Test
    void nullTextProducesZeroScoreAndNoFraudFlag() {

        FraudScoreResult result = engine.analyze(null);

        assertThat(result.score()).isZero();
        assertThat(result.fraudDetected()).isFalse();
    }

    @Test
    void cleanTextProducesZeroScore() {

        FraudScoreResult result = engine.analyze(
                "This is a genuine passport issued by the national authority."
        );

        assertThat(result.score()).isZero();
        assertThat(result.fraudDetected()).isFalse();
    }

    @Test
    void singleStrongKeywordCrossesTheFraudThreshold() {

        FraudScoreResult result = engine.analyze("This document appears forged.");

        assertThat(result.score()).isCloseTo(0.5, within(0.0001));
        assertThat(result.fraudDetected()).isTrue();
    }

    @Test
    void singleWeakKeywordAloneDoesNotCrossTheThreshold() {

        FraudScoreResult result = engine.analyze("This looks like a fake watermark.");

        assertThat(result.score()).isCloseTo(0.4, within(0.0001));
        assertThat(result.fraudDetected()).isFalse();
    }

    @Test
    void combinedKeywordsAccumulateScore() {

        FraudScoreResult result = engine.analyze(
                "The seal looks fake and the text appears altered."
        );

        // fake (0.4) + altered (0.3)
        assertThat(result.score()).isCloseTo(0.7, within(0.0001));
        assertThat(result.fraudDetected()).isTrue();
    }

    @Test
    void scoreIsCappedAtOne() {

        FraudScoreResult result = engine.analyze(
                "fake forged altered counterfeit fake forged altered counterfeit"
        );

        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.fraudDetected()).isTrue();
    }

    @Test
    void keywordMatchingIsCaseInsensitive() {

        FraudScoreResult result = engine.analyze("Clearly COUNTERFEIT document.");

        assertThat(result.score()).isCloseTo(0.6, within(0.0001));
        assertThat(result.fraudDetected()).isTrue();
    }

    @Test
    void emptyFindingListProducesZeroWeightedScore() {

        assertThat(engine.calculateScore(List.of())).isZero();
        assertThat(engine.calculateScore(null)).isZero();
    }

    @Test
    void weightedScoreCombinesSeverityAndConfidence() {

        List<RiskFinding> findings = List.of(
                RiskFinding.builder().ruleId("R1").severity(3).confidence(1.0).build(),
                RiskFinding.builder().ruleId("R2").severity(1).confidence(0.5).build()
        );

        // (0.9 * 1.0) + (0.2 * 0.5) = 1.0, capped at 1.0
        assertThat(engine.calculateScore(findings)).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void weightedScoreIsCappedAtOne() {

        List<RiskFinding> findings = List.of(
                RiskFinding.builder().ruleId("R1").severity(3).confidence(1.0).build(),
                RiskFinding.builder().ruleId("R2").severity(3).confidence(1.0).build(),
                RiskFinding.builder().ruleId("R3").severity(3).confidence(1.0).build()
        );

        assertThat(engine.calculateScore(findings)).isEqualTo(1.0);
    }
}
