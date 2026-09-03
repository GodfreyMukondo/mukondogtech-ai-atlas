package com.godfrey.ai_immigration_document_analyzer.analytics.projection;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardMetrics {

    private long totalUsers;

    private long activeUsers;

    private long totalDocuments;

    private long documentsToday;

    private long fraudCases;

    private double averageFraudScore;

    private double approvalRate;
}