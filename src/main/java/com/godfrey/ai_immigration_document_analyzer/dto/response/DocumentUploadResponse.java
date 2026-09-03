package com.godfrey.ai_immigration_document_analyzer.dto.response;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * DOCUMENT UPLOAD RESPONSE
 * ============================================================================
 *
 * Immutable API response.
 *
 * Sensitive raw OCR content is intentionally not returned by default.
 */
public record DocumentUploadResponse(

        Long documentId,

        Long userId,

        String documentType,

        String fileName,

        String summary,

        boolean fraudDetected,

        String riskLevel,

        String uploadStatus,

        LocalDateTime uploadedAt

) {

    public static DocumentUploadResponse success(
            Long documentId,
            Long userId,
            String documentType,
            String fileName,
            String summary,
            boolean fraudDetected,
            String riskLevel,
            LocalDateTime uploadedAt
    ) {

        return new DocumentUploadResponse(
                documentId,
                userId,
                documentType,
                fileName,
                summary,
                fraudDetected,
                riskLevel,
                "COMPLETED",
                uploadedAt
        );
    }
}