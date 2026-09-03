package com.godfrey.ai_immigration_document_analyzer.fraud;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fraud scoring engine (rule-based MVP version).
 */
@Component
public class FraudScoringEngine {

    /**
     * MAIN ENTRY POINT (fixes your missing method error)
     */
    public FraudScoreResult analyze(String text) {

        if (text == null || text.isBlank()) {
            return FraudScoreResult.builder()
                    .ruleId("NONE")
                    .score(0.0)
                    .fraudDetected(false)
                    .confidence(1.0)
                    .build();
        }

        double score = calculateScoreFromText(text);

        boolean fraudDetected = score >= 0.5;

        return FraudScoreResult.builder()
                .ruleId("RULE_BASED")
                .score(score)
                .fraudDetected(fraudDetected)
                .confidence(0.85)
                .build();
    }

    /**
     * Your existing logic (kept, but adapted to text input)
     */
    private double calculateScoreFromText(String text) {

        String lower = text.toLowerCase();

        double score = 0.0;

        if (lower.contains("fake")) score += 0.4;
        if (lower.contains("forged")) score += 0.5;
        if (lower.contains("altered")) score += 0.3;
        if (lower.contains("counterfeit")) score += 0.6;

        return Math.min(score, 1.0);
    }

    /**
     * OPTIONAL (kept for future ML expansion)
     */
    public double calculateScore(List<RiskFinding> risks) {

        if (risks == null || risks.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;

        for (RiskFinding risk : risks) {

            double severityWeight = switch (risk.getSeverity()) {
                case 1 -> 0.2;
                case 2 -> 0.5;
                case 3 -> 0.9;
                default -> 0.1;
            };

            score += severityWeight * risk.getConfidence();
        }

        return Math.min(score, 1.0);
    }
}