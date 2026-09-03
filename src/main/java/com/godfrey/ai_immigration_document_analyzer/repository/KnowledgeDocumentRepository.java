package com.godfrey.ai_immigration_document_analyzer.repository;

import com.godfrey.ai_immigration_document_analyzer.entity.KnowledgeDocument;
import com.godfrey.ai_immigration_document_analyzer.entity.KnowledgeDocumentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * ============================================================================
 * KNOWLEDGE DOCUMENT REPOSITORY
 * ============================================================================
 *
 * Spring Data JPA repository for the immigration knowledge base.
 *
 * Oracle table:
 *
 *     KNOWLEDGE_DOCUMENTS
 *
 * Responsibilities:
 *
 * - CRUD operations
 * - Paginated knowledge-base search
 * - Category filtering
 * - Country filtering through search
 * - Status filtering
 * - AI indexing statistics
 * - Knowledge-base statistics
 * - Duplicate-title detection
 *
 * The repository works with the Java entity properties of
 * {@link KnowledgeDocument}. It does NOT use Oracle column names directly
 * inside JPQL queries.
 *
 * ============================================================================
 */
public interface KnowledgeDocumentRepository
        extends JpaRepository<KnowledgeDocument, Long> {

    /**
     * =========================================================================
     * SEARCH / FILTER KNOWLEDGE DOCUMENTS
     * =========================================================================
     *
     * Supports:
     *
     * - Free-text search against:
     *      title
     *      country
     *      category
     *
     * - Optional category filtering
     * - Optional status filtering
     * - Pagination
     * - Most recently updated documents first
     *
     * The service layer is responsible for trimming and normalizing
     * incoming parameters.
     *
     * Example:
     *
     *     search = "work"
     *     category = "Visa"
     *     status = PUBLISHED
     *
     * =========================================================================
     */
    @Query("""
            SELECT d
            FROM KnowledgeDocument d
            WHERE
                (
                    :search IS NULL
                    OR :search = ''
                    OR LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(d.country) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(d.category) LIKE LOWER(CONCAT('%', :search, '%'))
                )
                AND
                (
                    :category IS NULL
                    OR :category = ''
                    OR LOWER(d.category) = LOWER(:category)
                )
                AND
                (
                    :status IS NULL
                    OR d.status = :status
                )
            ORDER BY d.updatedAt DESC
            """)
    Page<KnowledgeDocument> search(
            @Param("search") String search,
            @Param("category") String category,
            @Param("status") KnowledgeDocumentStatus status,
            Pageable pageable
    );

    /**
     * =========================================================================
     * COUNT DISTINCT CATEGORIES
     * =========================================================================
     *
     * Returns the number of unique knowledge-base categories.
     *
     * Example:
     *
     *     Work Visa
     *     Student Visa
     *     Family Visa
     *     Residency
     *
     * If the table is empty, this returns 0.
     *
     * =========================================================================
     */
    @Query("""
            SELECT COUNT(DISTINCT d.category)
            FROM KnowledgeDocument d
            """)
    long countDistinctCategories();

    /**
     * =========================================================================
     * COUNT DISTINCT COUNTRIES
     * =========================================================================
     *
     * Returns the number of unique countries represented in the
     * knowledge base.
     *
     * =========================================================================
     */
    @Query("""
            SELECT COUNT(DISTINCT d.country)
            FROM KnowledgeDocument d
            """)
    long countDistinctCountries();

    /**
     * =========================================================================
     * COUNT AI-INDEXED DOCUMENTS
     * =========================================================================
     *
     * Counts documents that have successfully been indexed by the
     * AI/RAG pipeline.
     *
     * Entity property:
     *
     *     aiIndexed
     *
     * Oracle column:
     *
     *     AI_INDEXED
     *
     * =========================================================================
     */
    long countByAiIndexedTrue();

    /**
     * =========================================================================
     * COUNT NON-INDEXED DOCUMENTS
     * =========================================================================
     *
     * Counts documents that still require AI/RAG indexing.
     *
     * =========================================================================
     */
    long countByAiIndexedFalse();

    /**
     * =========================================================================
     * COUNT DOCUMENTS BY STATUS
     * =========================================================================
     *
     * Supported statuses:
     *
     *     DRAFT
     *     REVIEW
     *     PUBLISHED
     *
     * =========================================================================
     */
    long countByStatus(
            KnowledgeDocumentStatus status
    );

    /**
     * =========================================================================
     * FIND DOCUMENT BY TITLE
     * =========================================================================
     *
     * Case-insensitive title lookup.
     *
     * Example:
     *
     *     "Zimbabwe Work Visa"
     *
     * will match:
     *
     *     "zimbabwe work visa"
     *
     * =========================================================================
     */
    Optional<KnowledgeDocument> findByTitleIgnoreCase(
            String title
    );

    /**
     * =========================================================================
     * CHECK WHETHER TITLE EXISTS
     * =========================================================================
     *
     * Used by the service layer before creating a new document.
     *
     * This provides application-level duplicate protection.
     *
     * The database UNIQUE constraint on TITLE remains the final
     * protection against race-condition duplicates.
     *
     * =========================================================================
     */
    boolean existsByTitleIgnoreCase(
            String title
    );

    /**
     * =========================================================================
     * CHECK WHETHER TITLE EXISTS FOR ANOTHER DOCUMENT
     * =========================================================================
     *
     * Useful when updating a document.
     *
     * Example:
     *
     * Existing:
     *
     *     ID = 5
     *     TITLE = "Zimbabwe Work Visa"
     *
     * Updating ID 5 with the same title should be allowed.
     *
     * Updating ID 6 with that title should be rejected.
     *
     * =========================================================================
     */
    boolean existsByTitleIgnoreCaseAndIdNot(
            String title,
            Long id
    );

    /**
     * =========================================================================
     * FIND DOCUMENTS REQUIRING AI INDEXING
     * =========================================================================
     *
     * Returns documents that have not yet been indexed.
     *
     * Useful for:
     *
     * - RAG ingestion
     * - Embedding generation
     * - Scheduled indexing jobs
     * - Manual AI indexing
     *
     * =========================================================================
     */
    Page<KnowledgeDocument> findByAiIndexedFalse(
            Pageable pageable
    );

    /**
     * =========================================================================
     * FIND PUBLISHED DOCUMENTS
     * =========================================================================
     *
     * Useful when the public-facing AI assistant should only retrieve
     * approved knowledge.
     *
     * =========================================================================
     */
    Page<KnowledgeDocument> findByStatus(
            KnowledgeDocumentStatus status,
            Pageable pageable
    );

    /**
     * =========================================================================
     * FIND PUBLISHED + AI-INDEXED DOCUMENTS
     * =========================================================================
     *
     * This is particularly useful for RAG retrieval pipelines.
     *
     * Only documents satisfying both conditions are returned:
     *
     *     STATUS = PUBLISHED
     *     AI_INDEXED = TRUE
     *
     * =========================================================================
     */
    Page<KnowledgeDocument> findByStatusAndAiIndexedTrue(
            KnowledgeDocumentStatus status,
            Pageable pageable
    );

    /**
     * =========================================================================
     * FIND BY CATEGORY
     * =========================================================================
     *
     * Case-insensitive category lookup with pagination.
     *
     * =========================================================================
     */
    Page<KnowledgeDocument> findByCategoryIgnoreCase(
            String category,
            Pageable pageable
    );

    /**
     * =========================================================================
     * FIND BY COUNTRY
     * =========================================================================
     *
     * Case-insensitive country lookup with pagination.
     *
     * =========================================================================
     */
    Page<KnowledgeDocument> findByCountryIgnoreCase(
            String country,
            Pageable pageable
    );
}

