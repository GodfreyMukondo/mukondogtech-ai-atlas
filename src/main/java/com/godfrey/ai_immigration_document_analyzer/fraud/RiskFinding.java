package com.godfrey.ai_immigration_document_analyzer.fraud;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class RiskFinding {

    private String ruleId;

    private String message;

    private int severity; // 1 = low, 2 = medium, 3 = high

    private double confidence; // 0.0 - 1.0
}