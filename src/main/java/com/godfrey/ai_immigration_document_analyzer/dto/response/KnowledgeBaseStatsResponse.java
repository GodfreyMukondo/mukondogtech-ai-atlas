package com.godfrey.ai_immigration_document_analyzer.dto.response;



/**

 * ============================================================================
 * KNOWLEDGE BASE STATISTICS RESPONSE
 * ============================================================================
 *
 * Immutable response DTO containing aggregated statistics for the
 * immigration knowledge base.
 *
 * Used by:
 *
 * * Admin Knowledge Base dashboard
 * * Knowledge Base analytics
 * * AI/RAG indexing monitoring
 * * Administrative reporting
 *
 * All values are calculated from the KNOWLEDGE_DOCUMENTS table.
 *
 * ============================================================================
 */
public record KnowledgeBaseStatsResponse(


  /**
   * =====================================================================
   * TOTAL DOCUMENTS
   * =====================================================================
   *
   * Total number of knowledge documents stored in the system.
   */
  long totalDocuments,

  /**
   * =====================================================================
   * AI INDEXED DOCUMENTS
   * =====================================================================
   *
   * Number of documents that have successfully been indexed
   * for AI/RAG processing.
   */
  long aiIndexedDocuments,

  /**
   * =====================================================================
   * COUNTRIES COVERED
   * =====================================================================
   *
   * Number of distinct countries represented in the knowledge base.
   */
  long countriesCovered,

  /**
   * =====================================================================
   * CATEGORIES COVERED
   * =====================================================================
   *
   * Number of distinct knowledge-document categories.
   */
  long categoriesCovered,

  /**
   * =====================================================================
   * PUBLISHED DOCUMENTS
   * =====================================================================
   *
   * Number of documents currently marked as PUBLISHED.
   */
  long publishedDocuments,

  /**
   * =====================================================================
   * DOCUMENTS UNDER REVIEW
   * =====================================================================
   *
   * Number of documents currently marked as REVIEW.
   */
  long reviewDocuments,

  /**
   * =====================================================================
   * DRAFT DOCUMENTS
   * =====================================================================
   *
   * Number of documents currently marked as DRAFT.
   */
  long draftDocuments,

  /**
   * =====================================================================
   * PENDING AI INDEX DOCUMENTS
   * =====================================================================
   *
   * Number of documents that have not yet been indexed for AI/RAG.
   */
  long pendingAiIndexDocuments


) {


    /**
     * =========================================================================
     * COMPACT CONSTRUCTOR
     * =========================================================================
     *
     * Protects the response from negative statistical values.
     *
     * Repository count queries should normally never return negative values,
     * but validating at the DTO boundary makes the API contract safer.
     */
    public KnowledgeBaseStatsResponse {

        if (totalDocuments < 0) {
            throw new IllegalArgumentException(
                    "Total documents cannot be negative."
            );
        }

        if (aiIndexedDocuments < 0) {
            throw new IllegalArgumentException(
                    "AI indexed documents cannot be negative."
            );
        }

        if (countriesCovered < 0) {
            throw new IllegalArgumentException(
                    "Countries covered cannot be negative."
            );
        }

        if (categoriesCovered < 0) {
            throw new IllegalArgumentException(
                    "Categories covered cannot be negative."
            );
        }

        if (publishedDocuments < 0) {
            throw new IllegalArgumentException(
                    "Published documents cannot be negative."
            );
        }

        if (reviewDocuments < 0) {
            throw new IllegalArgumentException(
                    "Review documents cannot be negative."
            );
        }

        if (draftDocuments < 0) {
            throw new IllegalArgumentException(
                    "Draft documents cannot be negative."
            );
        }

        if (pendingAiIndexDocuments < 0) {
            throw new IllegalArgumentException(
                    "Pending AI index documents cannot be negative."
            );
        }
    }


}
