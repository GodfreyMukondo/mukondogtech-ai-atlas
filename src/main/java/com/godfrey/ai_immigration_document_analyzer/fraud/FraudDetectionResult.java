package com.godfrey.ai_immigration_document_analyzer.fraud;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FraudDetectionResult {

    private boolean fraudulent;

    private double fraudScore; // aggregated score (0–1)

    private List<RiskFinding> risks;
}