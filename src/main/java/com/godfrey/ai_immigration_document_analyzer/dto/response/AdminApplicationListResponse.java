package com.godfrey.ai_immigration_document_analyzer.dto.response;

import java.util.List;

/**
 * ============================================================================
 * ADMIN APPLICATION LIST RESPONSE
 * ============================================================================
 *
 * Response envelope for GET /api/admin/applications, matching the shape
 * ApplicationReviewPage.tsx already expects:
 *
 *     { content: Application[], statistics: ApplicationStatistics }
 * ============================================================================
 */
public record AdminApplicationListResponse(

        List<AdminApplicationResponse> content,

        ApplicationStatisticsResponse statistics

) {
}
