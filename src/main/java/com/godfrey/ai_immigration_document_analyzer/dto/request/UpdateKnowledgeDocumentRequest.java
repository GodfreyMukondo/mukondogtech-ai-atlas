package com.godfrey.ai_immigration_document_analyzer.dto.request;

import com.godfrey.ai_immigration_document_analyzer.entity.KnowledgeDocumentStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ============================================================================
 * UPDATE KNOWLEDGE DOCUMENT REQUEST
 * ============================================================================
 *
 * Request payload used by the admin knowledge-base API when updating an
 * existing immigration knowledge document.
 *
 * All editable document fields are supplied by the client.
 *
 * The following fields are intentionally excluded because they are controlled
 * exclusively by the backend:
 *
 * - id
 * - createdBy
 * - updatedBy
 * - createdAt
 * - updatedAt
 * - aiIndexed
 * - aiIndexedAt
 *
 * When document content or other knowledge metadata changes, the service layer
 * should determine whether the document needs to be re-indexed by the AI/RAG
 * pipeline.
 *
 * ============================================================================
 */
public record UpdateKnowledgeDocumentRequest(

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
         * Optional at the DTO level.
         *
         * The service layer should preserve the existing status when this
         * value is null, unless the application's business rules specify
         * another default.
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
         * The complete knowledge-base content.
         *
         * If this value changes, the service layer should normally invalidate
         * the existing AI indexing state so that the updated content can be
         * processed by the RAG/embedding pipeline again.
         */
        @NotBlank(message = "Content is required")
        String content,

        /**
         * =====================================================================
         * SOURCE URL
         * =====================================================================
         *
         * Optional official immigration source.
         */
        @Size(
                max = 1000,
                message = "Source URL cannot exceed 1000 characters"
        )
        String sourceUrl
) {
}

