package com.godfrey.ai_immigration_document_analyzer.dto.response;

import java.util.List;

/**
 * Document upload volume for the admin analytics page.
 */
public record DocumentAnalyticsResponse(

        long totalDocuments,

        long uploadedToday,

        long uploadedThisMonth,

        List<AnalyticsPointResponse> uploadTrend

) {
}
