package com.godfrey.ai_immigration_document_analyzer.analytics.projection;

public interface FraudScoreStats {

    String getRiskLevel();

    Long getCount();
}