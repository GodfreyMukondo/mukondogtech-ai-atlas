package com.godfrey.ai_immigration_document_analyzer.dto.response;

/**
 * A single point on an admin analytics trend chart (e.g. one day's count).
 */
public record AnalyticsPointResponse(

        String label,

        long value

) {
}
