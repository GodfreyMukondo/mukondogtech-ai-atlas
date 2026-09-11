package com.godfrey.ai_immigration_document_analyzer.dto.response;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * DOCUMENT UPLOAD RESPONSE
 * ============================================================================
 *
 * Immutable response returned after a document upload has been successfully
 * completed.
 *
 * SECURITY
 * ----------------------------------------------------------------------------
 * Raw OCR/extracted document content is deliberately excluded.
 *
 * Storage implementation details such as S3 keys, bucket names, internal
 * filesystem paths, and processing metadata should never be returned here.
 *
 * ============================================================================
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

    /**
     * Creates a successful upload response.
     *
     * The upload status is deliberately fixed to COMPLETED rather than being
     * accepted from the client.
     */
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

