package com.godfrey.ai_immigration_document_analyzer.events;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event published when a document is successfully uploaded.
 * Used for:
 * - Fraud detection pipeline
 * - OCR processing
 * - RAG indexing
 * - Audit logging
 */
@Builder
public record DocumentUploadedEvent(

        /**
         * Unique event ID for idempotency in Kafka consumers
         */
        String eventId,

        /**
         * Document identifier in system
         */
        Long documentId,

        /**
         * User who uploaded the document
         */
        Long userId,

        /**
         * Storage location (S3 / local / blob storage)
         */
        String filePath,

        /**
         * Type of document (passport, visa, etc.)
         */
        String documentType,

        /**
         * Event creation timestamp
         */
        LocalDateTime timestamp,

        /**
         * Service origin (useful in microservices architecture)
         */
        String source
) {

    /**
     * Factory method for safe event creation
     */
    public static DocumentUploadedEvent create(
            Long documentId,
            Long userId,
            String filePath,
            String documentType
    ) {
        return DocumentUploadedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .documentId(documentId)
                .userId(userId)
                .filePath(filePath)
                .documentType(documentType)
                .timestamp(LocalDateTime.now())
                .source("document-service")
                .build();
    }

    /**
     * Safety helper for logging (avoids null pointer issues in consumers)
     */
    public boolean isValid() {
        return documentId != null
                && filePath != null
                && !filePath.isBlank();
    }
}