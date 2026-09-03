package com.godfrey.ai_immigration_document_analyzer.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result object for fraud analysis.
 * Contains detection status, score, risk level,
 * and matched fraud rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudResult {

    /**
     * Whether fraud was detected.
     * Lombok generates:
     * isFraudDetected()
     */
    private boolean fraudDetected;

    /**
     * Fraud confidence score (0–100)
     */
    private double score;

    /**
     * LOW / MEDIUM / HIGH
     */
    private String riskLevel;

    /**
     * Rules or patterns matched during analysis
     */
    private List<String> matchedRules;

    /**
     * Optional human-readable explanation
     */
    private String reason;
}