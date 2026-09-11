package com.godfrey.ai_immigration_document_analyzer.dto.response;

import java.util.List;

/**
 * AI chat usage volume for the admin analytics page, backed by
 * {@link com.godfrey.ai_immigration_document_analyzer.entity.ChatLog}.
 */
public record ChatAnalyticsResponse(

        long totalQueries,

        long queriesToday,

        double averageResponseTime,

        List<AnalyticsPointResponse> queryTrend

) {
}
