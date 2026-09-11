package com.godfrey.ai_immigration_document_analyzer.dto.response;

/**
 * Document processing (OCR/fraud analysis) outcomes for the admin
 * analytics page.
 *
 * {@code averageProcessingTime} is 0 - no per-document processing
 * duration is currently recorded anywhere in the application (unlike
 * chat responses, which {@link com.godfrey.ai_immigration_document_analyzer.entity.ChatLog}
 * now instruments). Returning a fabricated number here would be worse
 * than an honest zero.
 */
public record ProcessingAnalyticsResponse(

        double averageProcessingTime,

        long successfulAnalyses,

        long failedAnalyses,

        double accuracyRate

) {
}
