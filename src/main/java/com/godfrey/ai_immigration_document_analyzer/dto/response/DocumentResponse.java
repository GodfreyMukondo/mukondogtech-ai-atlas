package com.godfrey.ai_immigration_document_analyzer.dto.response;

import com.godfrey.ai_immigration_document_analyzer.entity.Document;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * DOCUMENT RESPONSE DTO
 * ============================================================================
 *
 * Public API representation of a stored document.
 *
 * SECURITY
 * ----------------------------------------------------------------------------
 * Internal storage information such as:
 *
 * - S3 object keys
 * - internal file paths
 * - extracted OCR text
 * - storage provider details
 *
 * is intentionally not exposed.
 *
 * This DTO is safe for normal document listing/detail endpoints.
 *
 * ============================================================================
 */
public record DocumentResponse(

        Long id,

        String fileName,

        String documentType,

        Long fileSize,

        String mimeType,

        String summary,

        Boolean fraudDetected,

        String riskLevel,

        String status,

        LocalDateTime uploadedAt,

        LocalDateTime updatedAt

) {

    /**
     * Creates a public API response from a Document entity.
     *
     * @param document document entity
     * @return immutable API response
     * @throws IllegalArgumentException if document is null
     */
    public static DocumentResponse from(
            Document document
    ) {

        if (document == null) {
            throw new IllegalArgumentException(
                    "Document cannot be null."
            );
        }

        return new DocumentResponse(
                document.getId(),
                document.getFileName(),
                document.getDocumentType(),
                document.getFileSize(),
                document.getMimeType(),
                document.getSummary(),
                Boolean.TRUE.equals(
                        document.getFraudDetected()
                ),
                document.getRiskLevel(),
                document.getUploadStatus(),
                document.getUploadedAt(),
                document.getUpdatedAt()
        );
    }
}

