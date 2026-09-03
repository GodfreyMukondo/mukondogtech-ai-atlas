package com.godfrey.ai_immigration_document_analyzer.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RuleFraudStats {

    private String ruleId;
    private long fraudCount;
}