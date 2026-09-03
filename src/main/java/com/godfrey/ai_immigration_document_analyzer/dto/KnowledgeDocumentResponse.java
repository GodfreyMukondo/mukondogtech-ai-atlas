package com.godfrey.ai_immigration_document_analyzer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.godfrey.ai_immigration_document_analyzer.entity.KnowledgeDocumentStatus;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * KNOWLEDGE DOCUMENT RESPONSE DTO
 * ============================================================================
 *
 * Immutable API response representing a complete knowledge-base document.
 *
 * Used by:
 *
 * - Knowledge Base administration
 * - Knowledge document CRUD operations
 * - Immigration policy management
 * - AI/RAG indexing
 * - Knowledge document search
 * - Document review workflows
 *
 * This DTO is intentionally implemented as a Java record.
 *
 * Advantages:
 *
 * - Immutable
 * - Thread-safe
 * - Minimal boilerplate
 * - Clear API contract
 * - Native Jackson/Spring Boot support
 *
 * IMPORTANT:
 *
 * This record does NOT expose a Lombok builder.
 *
 * Create instances using:
 *
 *     new KnowledgeDocumentResponse(...)
 *
 * Example:
 *
 *     return new KnowledgeDocumentResponse(
 *             document.getId(),
 *             document.getTitle(),
 *             document.getCategory(),
 *             document.getCountry(),
 *             document.getStatus(),
 *             document.getVersion(),
 *             document.getContent(),
 *             document.getSourceUrl(),
 *             Boolean.TRUE.equals(document.getAiIndexed()),
 *             document.getAiIndexedAt(),
 *             document.getCreatedBy(),
 *             document.getUpdatedBy(),
 *             document.getCreatedAt(),
 *             document.getUpdatedAt()
 *     );
 *
 * ============================================================================
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "title",
        "category",
        "country",
        "status",
        "version",
        "content",
        "sourceUrl",
        "aiIndexed",
        "aiIndexedAt",
        "createdBy",
        "updatedBy",
        "createdAt",
        "updatedAt"
})
public record KnowledgeDocumentResponse(

        /**
         * =====================================================================
         * DOCUMENT ID
         * =====================================================================
         *
         * Unique database identifier.
         */
        Long id,

        /**
         * =====================================================================
         * DOCUMENT TITLE
         * =====================================================================
         *
         * Human-readable title of the knowledge document.
         */
        String title,

        /**
         * =====================================================================
         * DOCUMENT CATEGORY
         * =====================================================================
         *
         * Example values:
         *
         * - Work Visa
         * - Student Visa
         * - Visitor Visa
         * - Family Visa
         * - Residency
         * - Compliance
         */
        String category,

        /**
         * =====================================================================
         * COUNTRY
         * =====================================================================
         *
         * Country to which the immigration information applies.
         */
        String country,

        /**
         * =====================================================================
         * DOCUMENT STATUS
         * =====================================================================
         *
         * Lifecycle state of the knowledge document.
         *
         * Typical values:
         *
         * - DRAFT
         * - REVIEW
         * - PUBLISHED
         */
        KnowledgeDocumentStatus status,

        /**
         * =====================================================================
         * DOCUMENT VERSION
         * =====================================================================
         *
         * Version of the knowledge document.
         *
         * Example:
         *
         * - 1.0
         * - 1.1
         * - 2.0
         */
        String version,

        /**
         * =====================================================================
         * DOCUMENT CONTENT
         * =====================================================================
         *
         * Complete content of the knowledge document.
         *
         * This content can be processed by:
         *
         * - RAG pipelines
         * - Embedding models
         * - Vector databases
         * - LangChain
         * - IBM watsonx
         * - AI search
         */
        String content,

        /**
         * =====================================================================
         * SOURCE URL
         * =====================================================================
         *
         * Optional official source URL for the immigration information.
         */
        String sourceUrl,

        /**
         * =====================================================================
         * AI INDEX STATUS
         * =====================================================================
         *
         * Indicates whether the current version of the document has been
         * successfully indexed by the AI/RAG pipeline.
         *
         * true:
         *     Document is currently indexed.
         *
         * false:
         *     Document requires indexing or re-indexing.
         */
        Boolean aiIndexed,

        /**
         * =====================================================================
         * AI INDEXED AT
         * =====================================================================
         *
         * Timestamp of the most recent successful AI/RAG indexing operation.
         *
         * Null when the document has not been indexed successfully.
         */
        LocalDateTime aiIndexedAt,

        /**
         * =====================================================================
         * CREATED BY
         * =====================================================================
         *
         * Username or email of the user who created the document.
         */
        String createdBy,

        /**
         * =====================================================================
         * UPDATED BY
         * =====================================================================
         *
         * Username or email of the user who last modified the document.
         */
        String updatedBy,

        /**
         * =====================================================================
         * CREATED AT
         * =====================================================================
         *
         * Timestamp when the document was created.
         */
        LocalDateTime createdAt,

        /**
         * =====================================================================
         * UPDATED AT
         * =====================================================================
         *
         * Timestamp when the document was last modified.
         */
        LocalDateTime updatedAt

) {
}

