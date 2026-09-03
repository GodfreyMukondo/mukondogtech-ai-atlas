package com.godfrey.ai_immigration_document_analyzer.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**

 * Represents the result of a fraud analysis operation.
 * Used for internal processing and API responses.
 */
@Getter
@ToString
@Builder
public class FraudAnalysisResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean fraudDetected;

    private final int score;

    private final String riskLevel;

    @Builder.Default
    private final List<String> matchedRules = Collections.emptyList();

    /**

     * Returns true if fraud score is considered high risk.
     */
    public boolean isHighRisk() {
        return "HIGH".equalsIgnoreCase(riskLevel);
    }

    /**

     * Returns true if fraud score is considered medium risk.
     */
    public boolean isMediumRisk() {
        return "MEDIUM".equalsIgnoreCase(riskLevel);
    }

    /**

     * Returns true if fraud score is considered low risk.
     */
    public boolean isLowRisk() {
        return "LOW".equalsIgnoreCase(riskLevel);
    }

    /**

     * Creates a safe default clean result.
     */
    public static FraudAnalysisResult clean() {
        return FraudAnalysisResult.builder()
                .fraudDetected(false)
                .score(0)
                .riskLevel("LOW")
                .matchedRules(Collections.emptyList())
                .build();
    }

    /**

     * Creates a fraud-detected result.
     */
    public static FraudAnalysisResult flagged(
            int score,
            String riskLevel,
            List<String> matchedRules
    ) {
        return FraudAnalysisResult.builder()
                .fraudDetected(true)
                .score(score)
                .riskLevel(riskLevel)
                .matchedRules(
                        matchedRules == null
                                ? Collections.emptyList()
                                : matchedRules
                )
                .build();
    }
}
