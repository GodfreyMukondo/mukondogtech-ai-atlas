package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.dto.response.FraudAnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**

 * Fraud detection service responsible for analyzing
 * document text for suspicious indicators.
 *
 * This implementation uses a rule-based scoring engine
 * and can later be extended with:
 * * ML fraud models
 * * OCR confidence analysis
 * * metadata validation
 * * external document verification APIs
 */
@Service
@Slf4j
public class FraudDetectionService {

    private static final int FRAUD_THRESHOLD = 50;
    private static final int HIGH_RISK_THRESHOLD = 70;
    private static final int MEDIUM_RISK_THRESHOLD = 30;

    private static final List<FraudRule> FRAUD_RULES = List.of(
            new FraudRule("fake", 40),
            new FraudRule("forged", 50),
            new FraudRule("altered", 35),
            new FraudRule("tampered", 45),
            new FraudRule("counterfeit", 50),
            new FraudRule("modified", 20),
            new FraudRule("replica", 25),
            new FraudRule("duplicate", 15)
    );

    /**

     * Performs full fraud analysis.
     *
     * @param text extracted document text
     * @return fraud analysis result
     */
    public FraudAnalysisResult analyze(String text) {

        if (isEmpty(text)) {
            log.warn("Fraud analysis received empty text");
            return FraudAnalysisResult.clean();
        }

        String normalizedText = normalize(text);
        List<String> matchedRules = new ArrayList<>();

        int score = calculateFraudScore(normalizedText, matchedRules);
        boolean fraudDetected = score >= FRAUD_THRESHOLD;
        String riskLevel = determineRiskLevel(score);

        log.info(
                "Fraud analysis completed | fraudDetected={} | score={} | riskLevel={}",
                fraudDetected,
                score,
                riskLevel
        );

        if (fraudDetected) {
            return FraudAnalysisResult.flagged(
                    score,
                    riskLevel,
                    matchedRules
            );
        }

        return FraudAnalysisResult.builder()
                .fraudDetected(false)
                .score(score)
                .riskLevel(riskLevel)
                .matchedRules(matchedRules)
                .build();
    }

    /**

     * Returns only fraud status.
     */
    public boolean detect(String text) {
        return analyze(text).isFraudDetected();
    }

    /**

     * Returns only calculated risk level.
     */
    public String calculateRiskLevel(String text) {
        return analyze(text).getRiskLevel();
    }

    /**

     * Calculates fraud score based on matching rules.
     */
    private int calculateFraudScore(
            String normalizedText,
            List<String> matchedRules
    ) {
        int score = 0;

        for (FraudRule rule : FRAUD_RULES) {
            if (normalizedText.contains(rule.keyword())) {
                score += rule.weight();
                matchedRules.add(rule.keyword());
            }
        }

        return score;
    }

    /**

     * Determines risk level based on score.
     */
    private String determineRiskLevel(int score) {

        if (score >= HIGH_RISK_THRESHOLD) {
            return "HIGH";
        }

        if (score >= MEDIUM_RISK_THRESHOLD) {
            return "MEDIUM";
        }

        return "LOW";
    }

    /**

     * Normalizes text for consistent rule matching.
     */
    private String normalize(String text) {
        return text.trim().toLowerCase(Locale.ROOT);
    }

    /**

     * Checks if input text is empty.
     */
    private boolean isEmpty(String text) {
        return text == null || text.isBlank();
    }

    /**

     * Internal fraud rule model.
     */
    private record FraudRule(String keyword, int weight) {
    }
}
