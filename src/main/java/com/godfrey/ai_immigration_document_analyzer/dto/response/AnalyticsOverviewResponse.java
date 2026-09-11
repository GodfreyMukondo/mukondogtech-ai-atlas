package com.godfrey.ai_immigration_document_analyzer.dto.response;

/**
 * Combined payload for the admin analytics overview page.
 */
public record AnalyticsOverviewResponse(

        DocumentAnalyticsResponse documents,

        ChatAnalyticsResponse chat,

        ProcessingAnalyticsResponse processing

) {
}
