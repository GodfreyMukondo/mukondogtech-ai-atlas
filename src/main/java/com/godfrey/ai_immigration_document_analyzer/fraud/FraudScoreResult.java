package com.godfrey.ai_immigration_document_analyzer.fraud;

import lombok.Builder;

/**
 * Output of fraud scoring engine.
 */
@Builder
public record FraudScoreResult(
        String ruleId,
        double score,
        boolean fraudDetected,
        double confidence
) {}