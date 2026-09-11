package com.godfrey.ai_immigration_document_analyzer.dto.response;

/**
 * ============================================================================
 * APPLICATION STATISTICS RESPONSE
 * ============================================================================
 *
 * Matches the frontend's ApplicationReviewPage.tsx ApplicationStatistics
 * contract exactly.
 * ============================================================================
 */
public record ApplicationStatisticsResponse(

        long pendingReviews,

        long approvedCases,

        long rejectedCases,

        long fraudAlerts

) {
}
