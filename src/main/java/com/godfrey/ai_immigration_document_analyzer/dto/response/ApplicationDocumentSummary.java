package com.godfrey.ai_immigration_document_analyzer.dto.response;

/**
 * ============================================================================
 * APPLICATION DOCUMENT SUMMARY
 * ============================================================================
 *
 * Lightweight representation of a document attached to an application.
 * Intentionally excludes storage internals (S3 key, extracted text) - the
 * full document can still be fetched via GET /api/documents/{id} when
 * needed.
 * ============================================================================
 */
public record ApplicationDocumentSummary(

        Long id,

        String fileName,

        String documentType,

        String status

) {
}
