package com.godfrey.ai_immigration_document_analyzer.dto.request;

import com.godfrey.ai_immigration_document_analyzer.entity.KnowledgeDocumentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ============================================================================
 * CREATE KNOWLEDGE DOCUMENT REQUEST
 * ============================================================================
 *
 * Request payload used by the admin knowledge-base API when creating a new
 * immigration knowledge document.
 *
 * Validation responsibilities:
 * - title      : required, maximum 500 characters
 * - category   : required, maximum 150 characters
 * - country    : required, maximum 150 characters
 * - status     : optional; service layer applies the default status
 * - version    : required, maximum 50 characters
 * - content    : required
 * - sourceUrl  : optional, maximum 1000 characters
 *
 * The request intentionally does not contain:
 * - id
 * - createdBy
 * - updatedBy
 * - createdAt
 * - updatedAt
 * - aiIndexed
 * - aiIndexedAt
 *
 * These values are controlled by the backend and must not be supplied by
 * clients.
 *
 * ============================================================================
 */
public record CreateKnowledgeDocumentRequest(

        /**
         * =====================================================================
         * DOCUMENT TITLE
         * =====================================================================
         */
        @NotBlank(message = "Title is required")
        @Size(
                max = 500,
                message = "Title cannot exceed 500 characters"
        )
        String title,

        /**
         * =====================================================================
         * DOCUMENT CATEGORY
         * =====================================================================
         */
        @NotBlank(message = "Category is required")
        @Size(
                max = 150,
                message = "Category cannot exceed 150 characters"
        )
        String category,

        /**
         * =====================================================================
         * COUNTRY
         * =====================================================================
         */
        @NotBlank(message = "Country is required")
        @Size(
                max = 150,
                message = "Country cannot exceed 150 characters"
        )
        String country,

        /**
         * =====================================================================
         * DOCUMENT STATUS
         * =====================================================================
         *
         * Optional from the API perspective.
         *
         * If omitted, the service layer should default the document to:
         *
         *     KnowledgeDocumentStatus.DRAFT
         *
         * This prevents clients from accidentally creating documents without
         * a valid status.
         */
        KnowledgeDocumentStatus status,

        /**
         * =====================================================================
         * DOCUMENT VERSION
         * =====================================================================
         */
        @NotBlank(message = "Version is required")
        @Size(
                max = 50,
                message = "Version cannot exceed 50 characters"
        )
        String version,

        /**
         * =====================================================================
         * DOCUMENT CONTENT
         * =====================================================================
         *
         * The actual immigration knowledge-base content.
         *
         * This content may subsequently be processed by the AI/RAG indexing
         * pipeline.
         */
        @NotBlank(message = "Content is required")
        String content,

        /**
         * =====================================================================
         * SOURCE URL
         * =====================================================================
         *
         * Optional official source associated with the document.
         */
        @Size(
                max = 1000,
                message = "Source URL cannot exceed 1000 characters"
        )
        String sourceUrl
) {
}

