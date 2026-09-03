package com.godfrey.ai_immigration_document_analyzer.dto.response;

import com.godfrey.ai_immigration_document_analyzer.entity.Document;

import java.time.LocalDateTime;


/**
 * ============================================================================
 * DOCUMENT RESPONSE DTO
 * ============================================================================
 *
 * Public API representation of a document.
 *
 * IMPORTANT:
 *
 * Internal storage information such as the S3 object key is intentionally
 * not exposed directly to the frontend.
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
     * Create API response from entity.
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