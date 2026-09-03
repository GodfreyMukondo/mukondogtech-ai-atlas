package com.godfrey.ai_immigration_document_analyzer.dto;

import com.godfrey.ai_immigration_document_analyzer.entity.KnowledgeDocumentStatus;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * KNOWLEDGE DOCUMENT SUMMARY
 * ============================================================================
 *
 * Used by the paginated admin table.
 */
public record KnowledgeDocumentSummaryResponse(

        Long id,

        String title,

        String category,

        String country,

        KnowledgeDocumentStatus status,

        String version,

        String updatedBy,

        LocalDateTime updatedAt,

        Boolean aiIndexed
) {
}
