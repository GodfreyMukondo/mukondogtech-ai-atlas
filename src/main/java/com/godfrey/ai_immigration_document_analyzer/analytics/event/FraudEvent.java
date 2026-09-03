package com.godfrey.ai_immigration_document_analyzer.analytics.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudEvent {

    private String ruleId;
    private double fraudScore;
    private boolean fraudFlag;
    private double confidence;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Convenience constructor to ensure timestamp is always set
     * when object is created manually (not via builder)
     */
    public FraudEvent(String ruleId, double fraudScore, boolean fraudFlag, double confidence) {
        this.ruleId = ruleId;
        this.fraudScore = fraudScore;
        this.fraudFlag = fraudFlag;
        this.confidence = confidence;
        this.timestamp = LocalDateTime.now();
    }
}