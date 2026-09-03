package com.godfrey.ai_immigration_document_analyzer.service.impl;

import com.godfrey.ai_immigration_document_analyzer.dto.KnowledgeDocumentResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.KnowledgeDocumentSummaryResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.request.CreateKnowledgeDocumentRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.request.UpdateKnowledgeDocumentRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.response.KnowledgeBaseStatsResponse;
import com.godfrey.ai_immigration_document_analyzer.entity.KnowledgeDocument;
import com.godfrey.ai_immigration_document_analyzer.entity.KnowledgeDocumentStatus;
import com.godfrey.ai_immigration_document_analyzer.repository.KnowledgeDocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.service.KnowledgeBaseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ============================================================================
 * KNOWLEDGE BASE SERVICE IMPLEMENTATION
 * ============================================================================
 *
 * Production implementation of {@link KnowledgeBaseService}.
 *
 * Responsibilities:
 *
 * - Knowledge document CRUD
 * - Search and filtering
 * - Pagination
 * - Duplicate title protection
 * - AI/RAG indexing state management
 * - Knowledge-base statistics
 * - Audit information
 * - Transaction management
 * - Entity/DTO mapping
 * - Input normalization
 * - Defensive validation
 *
 * Database:
 *
 *     Oracle
 *
 * Table:
 *
 *     KNOWLEDGE_DOCUMENTS
 *
 * Sequence:
 *
 *     KNOWLEDGE_DOCUMENTS_SEQ
 *
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final String DEFAULT_VERSION = "1.0";

    private static final int MAX_TITLE_LENGTH = 500;
    private static final int MAX_CATEGORY_LENGTH = 150;
    private static final int MAX_COUNTRY_LENGTH = 150;
    private static final int MAX_VERSION_LENGTH = 50;
    private static final int MAX_SOURCE_URL_LENGTH = 2000;

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    /**
     * =========================================================================
     * SEARCH KNOWLEDGE DOCUMENTS
     * =========================================================================
     */
    @Override
    public Page<KnowledgeDocumentSummaryResponse> search(
            String search,
            String category,
            KnowledgeDocumentStatus status,
            Pageable pageable
    ) {

        if (pageable == null) {
            throw new IllegalArgumentException(
                    "Pageable cannot be null."
            );
        }

        String normalizedSearch =
                normalizeOptional(search);

        String normalizedCategory =
                normalizeOptional(category);

        Page<KnowledgeDocument> documents =
                knowledgeDocumentRepository.search(
                        normalizedSearch,
                        normalizedCategory,
                        status,
                        pageable
                );

        return documents.map(
                this::toSummaryResponse
        );
    }

    /**
     * =========================================================================
     * GET DOCUMENT BY ID
     * =========================================================================
     */
    @Override
    public KnowledgeDocumentResponse getById(
            Long id
    ) {

        validateId(id);

        KnowledgeDocument document =
                findDocumentById(id);

        return toResponse(document);
    }

    /**
     * =========================================================================
     * CREATE KNOWLEDGE DOCUMENT
     * =========================================================================
     */
    @Override
    @Transactional
    public KnowledgeDocumentResponse create(
            CreateKnowledgeDocumentRequest request,
            String username
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Create knowledge document request cannot be null."
            );
        }

        String normalizedUsername =
                normalizeRequired(
                        username,
                        "Username"
                );

        String title =
                normalizeRequired(
                        request.title(),
                        "Title"
                );

        String category =
                normalizeRequired(
                        request.category(),
                        "Category"
                );

        String country =
                normalizeRequired(
                        request.country(),
                        "Country"
                );

        String content =
                normalizeRequired(
                        request.content(),
                        "Content"
                );

        String version =
                normalizeVersion(
                        request.version()
                );

        String sourceUrl =
                normalizeSourceUrl(
                        request.sourceUrl()
                );

        validateLength(
                title,
                MAX_TITLE_LENGTH,
                "Title"
        );

        validateLength(
                category,
                MAX_CATEGORY_LENGTH,
                "Category"
        );

        validateLength(
                country,
                MAX_COUNTRY_LENGTH,
                "Country"
        );

        validateLength(
                version,
                MAX_VERSION_LENGTH,
                "Version"
        );

        /*
         * Application-level duplicate protection.
         *
         * The database unique constraint remains the final protection
         * against concurrent duplicate inserts.
         */
        if (knowledgeDocumentRepository.existsByTitleIgnoreCase(title)) {

            throw new KnowledgeDocumentAlreadyExistsException(
                    "A knowledge document with title '" +
                            title +
                            "' already exists."
            );
        }

        KnowledgeDocumentStatus status =
                request.status() != null
                        ? request.status()
                        : KnowledgeDocumentStatus.DRAFT;

        LocalDateTime now =
                LocalDateTime.now();

        KnowledgeDocument document =
                KnowledgeDocument.builder()
                        .title(title)
                        .category(category)
                        .country(country)
                        .status(status)
                        .version(version)
                        .content(content)
                        .sourceUrl(sourceUrl)
                        .aiIndexed(false)
                        .aiIndexedAt(null)
                        .createdBy(normalizedUsername)
                        .updatedBy(normalizedUsername)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        try {

            KnowledgeDocument saved =
                    knowledgeDocumentRepository.save(
                            document
                    );

            log.info(
                    "Knowledge document created successfully. " +
                            "id={}, title={}, category={}, country={}, status={}, createdBy={}",
                    saved.getId(),
                    saved.getTitle(),
                    saved.getCategory(),
                    saved.getCountry(),
                    saved.getStatus(),
                    normalizedUsername
            );

            return toResponse(saved);

        } catch (DataIntegrityViolationException exception) {

            log.error(
                    "Database constraint violation while creating knowledge document. " +
                            "title={}",
                    title,
                    exception
            );

            throw new KnowledgeDocumentAlreadyExistsException(
                    "Unable to create the knowledge document. " +
                            "The title may already exist or a database constraint was violated."
            );
        }
    }

    /**
     * =========================================================================
     * UPDATE KNOWLEDGE DOCUMENT
     * =========================================================================
     */
    @Override
    @Transactional
    public KnowledgeDocumentResponse update(
            Long id,
            UpdateKnowledgeDocumentRequest request,
            String username
    ) {

        validateId(id);

        if (request == null) {
            throw new IllegalArgumentException(
                    "Update knowledge document request cannot be null."
            );
        }

        String normalizedUsername =
                normalizeRequired(
                        username,
                        "Username"
                );

        KnowledgeDocument document =
                findDocumentById(id);

        boolean aiIndexMustBeInvalidated =
                false;

        /*
         * =====================================================================
         * TITLE
         * =====================================================================
         */
        if (request.title() != null) {

            String newTitle =
                    normalizeRequired(
                            request.title(),
                            "Title"
                    );

            validateLength(
                    newTitle,
                    MAX_TITLE_LENGTH,
                    "Title"
            );

            boolean titleChanged =
                    !newTitle.equalsIgnoreCase(
                            document.getTitle()
                    );

            if (titleChanged) {

                knowledgeDocumentRepository
                        .findByTitleIgnoreCase(newTitle)
                        .ifPresent(existing -> {

                            if (!Objects.equals(
                                    existing.getId(),
                                    id
                            )) {

                                throw new KnowledgeDocumentAlreadyExistsException(
                                        "A knowledge document with title '" +
                                                newTitle +
                                                "' already exists."
                                );
                            }
                        });

                document.setTitle(
                        newTitle
                );

                aiIndexMustBeInvalidated =
                        true;
            }
        }

        /*
         * =====================================================================
         * CATEGORY
         * =====================================================================
         */
        if (request.category() != null) {

            String newCategory =
                    normalizeRequired(
                            request.category(),
                            "Category"
                    );

            validateLength(
                    newCategory,
                    MAX_CATEGORY_LENGTH,
                    "Category"
            );

            if (!newCategory.equals(
                    document.getCategory()
            )) {

                document.setCategory(
                        newCategory
                );

                aiIndexMustBeInvalidated =
                        true;
            }
        }

        /*
         * =====================================================================
         * COUNTRY
         * =====================================================================
         */
        if (request.country() != null) {

            String newCountry =
                    normalizeRequired(
                            request.country(),
                            "Country"
                    );

            validateLength(
                    newCountry,
                    MAX_COUNTRY_LENGTH,
                    "Country"
            );

            if (!newCountry.equals(
                    document.getCountry()
            )) {

                document.setCountry(
                        newCountry
                );

                aiIndexMustBeInvalidated =
                        true;
            }
        }

        /*
         * =====================================================================
         * STATUS
         * =====================================================================
         */
        if (request.status() != null) {

            if (request.status() != document.getStatus()) {

                document.setStatus(
                        request.status()
                );

                aiIndexMustBeInvalidated =
                        true;
            }
        }

        /*
         * =====================================================================
         * VERSION
         * =====================================================================
         */
        if (request.version() != null) {

            String newVersion =
                    normalizeVersion(
                            request.version()
                    );

            if (!newVersion.equals(
                    document.getVersion()
            )) {

                document.setVersion(
                        newVersion
                );

                aiIndexMustBeInvalidated =
                        true;
            }
        }

        /*
         * =====================================================================
         * CONTENT
         * =====================================================================
         */
        if (request.content() != null) {

            String newContent =
                    normalizeRequired(
                            request.content(),
                            "Content"
                    );

            if (!newContent.equals(
                    document.getContent()
            )) {

                document.setContent(
                        newContent
                );

                aiIndexMustBeInvalidated =
                        true;
            }
        }

        /*
         * =====================================================================
         * SOURCE URL
         * =====================================================================
         */
        if (request.sourceUrl() != null) {

            String newSourceUrl =
                    normalizeSourceUrl(
                            request.sourceUrl()
                    );

            if (!Objects.equals(
                    newSourceUrl,
                    document.getSourceUrl()
            )) {

                document.setSourceUrl(
                        newSourceUrl
                );
            }
        }

        /*
         * =====================================================================
         * AI INDEX STATE
         * =====================================================================
         */
        if (aiIndexMustBeInvalidated) {

            document.setAiIndexed(false);
            document.setAiIndexedAt(null);

            log.info(
                    "AI index invalidated because knowledge document " +
                            "content or searchable metadata changed. id={}",
                    id
            );
        }

        /*
         * =====================================================================
         * AUDIT INFORMATION
         * =====================================================================
         */
        document.setUpdatedBy(
                normalizedUsername
        );

        document.setUpdatedAt(
                LocalDateTime.now()
        );

        try {

            KnowledgeDocument saved =
                    knowledgeDocumentRepository.save(
                            document
                    );

            log.info(
                    "Knowledge document updated successfully. " +
                            "id={}, title={}, status={}, aiIndexed={}, updatedBy={}",
                    saved.getId(),
                    saved.getTitle(),
                    saved.getStatus(),
                    saved.getAiIndexed(),
                    normalizedUsername
            );

            return toResponse(saved);

        } catch (DataIntegrityViolationException exception) {

            log.error(
                    "Database constraint violation while updating knowledge document. " +
                            "id={}",
                    id,
                    exception
            );

            throw new KnowledgeDocumentAlreadyExistsException(
                    "Unable to update the knowledge document. " +
                            "The title may already exist or a database constraint was violated."
            );
        }
    }

    /**
     * =========================================================================
     * DELETE KNOWLEDGE DOCUMENT
     * =========================================================================
     */
    @Override
    @Transactional
    public void delete(
            Long id
    ) {

        validateId(id);

        KnowledgeDocument document =
                findDocumentById(id);

        try {

            knowledgeDocumentRepository.delete(
                    document
            );

            /*
             * Force synchronization with Oracle while still inside
             * the transaction so constraint violations are detected here.
             */
            knowledgeDocumentRepository.flush();

            log.info(
                    "Knowledge document deleted successfully. " +
                            "id={}, title={}",
                    document.getId(),
                    document.getTitle()
            );

        } catch (DataIntegrityViolationException exception) {

            log.error(
                    "Unable to delete knowledge document because dependent " +
                            "records or database constraints exist. id={}",
                    id,
                    exception
            );

            throw new KnowledgeDocumentDeletionException(
                    "Unable to delete knowledge document with id " +
                            id +
                            ". It may have dependent records."
            );
        }
    }

    /**
     * =========================================================================
     * MARK DOCUMENT AS AI INDEXED
     * =========================================================================
     */
    @Override
    @Transactional
    public KnowledgeDocumentResponse markAsIndexed(
            Long id
    ) {

        validateId(id);

        KnowledgeDocument document =
                findDocumentById(id);

        LocalDateTime indexedAt =
                LocalDateTime.now();

        document.setAiIndexed(true);
        document.setAiIndexedAt(indexedAt);
        document.setUpdatedAt(indexedAt);

        KnowledgeDocument saved =
                knowledgeDocumentRepository.save(
                        document
                );

        log.info(
                "Knowledge document marked as AI indexed. " +
                        "id={}, title={}, indexedAt={}",
                saved.getId(),
                saved.getTitle(),
                indexedAt
        );

        return toResponse(saved);
    }

    /**
     * =========================================================================
     * KNOWLEDGE BASE STATISTICS
     * =========================================================================
     */
    @Override
    public KnowledgeBaseStatsResponse getStatistics() {

        long totalDocuments =
                knowledgeDocumentRepository.count();

        long countriesCovered =
                knowledgeDocumentRepository
                        .countDistinctCountries();

        long categoriesCovered =
                knowledgeDocumentRepository
                        .countDistinctCategories();

        long aiIndexedDocuments =
                knowledgeDocumentRepository
                        .countByAiIndexedTrue();

        long pendingAiIndexDocuments =
                knowledgeDocumentRepository
                        .countByAiIndexedFalse();

        long publishedDocuments =
                knowledgeDocumentRepository
                        .countByStatus(
                                KnowledgeDocumentStatus.PUBLISHED
                        );

        long reviewDocuments =
                knowledgeDocumentRepository
                        .countByStatus(
                                KnowledgeDocumentStatus.REVIEW
                        );

        long draftDocuments =
                knowledgeDocumentRepository
                        .countByStatus(
                                KnowledgeDocumentStatus.DRAFT
                        );

        return new KnowledgeBaseStatsResponse(
                totalDocuments,
                aiIndexedDocuments,
                countriesCovered,
                categoriesCovered,
                publishedDocuments,
                reviewDocuments,
                draftDocuments,
                pendingAiIndexDocuments
        );
    }

    /**
     * =========================================================================
     * FIND DOCUMENT BY ID
     * =========================================================================
     */
    private KnowledgeDocument findDocumentById(
            Long id
    ) {

        return knowledgeDocumentRepository
                .findById(id)
                .orElseThrow(() ->
                        new KnowledgeDocumentNotFoundException(
                                "Knowledge document not found with id: " +
                                        id
                        )
                );
    }

    /**
     * =========================================================================
     * ENTITY -> FULL RESPONSE DTO
     * =========================================================================
     *
     * IMPORTANT:
     *
     * KnowledgeDocumentResponse is a Java record.
     * Records do not provide Lombok's builder() method.
     *
     * Therefore the DTO is constructed using its canonical constructor.
     * =========================================================================
     */
    private KnowledgeDocumentResponse toResponse(
            KnowledgeDocument document
    ) {

        if (document == null) {
            return null;
        }

        return new KnowledgeDocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getCategory(),
                document.getCountry(),
                document.getStatus(),
                document.getVersion(),
                document.getContent(),
                document.getSourceUrl(),
                Boolean.TRUE.equals(
                        document.getAiIndexed()
                ),
                document.getAiIndexedAt(),
                document.getCreatedBy(),
                document.getUpdatedBy(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    /**
     * =========================================================================
     * ENTITY -> SUMMARY RESPONSE DTO
     * =========================================================================
     *
     * KnowledgeDocumentSummaryResponse must also be constructed according
     * to its actual DTO type.
     *
     * This implementation assumes it is a Java record matching the fields
     * below, which is consistent with KnowledgeDocumentResponse.
     * =========================================================================
     */
    private KnowledgeDocumentSummaryResponse toSummaryResponse(
            KnowledgeDocument document
    ) {

        if (document == null) {
            return null;
        }

        return new KnowledgeDocumentSummaryResponse(
                document.getId(),
                document.getTitle(),
                document.getCategory(),
                document.getCountry(),
                document.getStatus(),
                document.getVersion(),
                document.getCreatedBy(),
                document.getCreatedAt(),
                Boolean.TRUE.equals(
                        document.getAiIndexed()
                )
        );
    }

    /**
     * =========================================================================
     * NORMALIZE OPTIONAL VALUE
     * =========================================================================
     */
    private String normalizeOptional(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    /**
     * =========================================================================
     * NORMALIZE REQUIRED VALUE
     * =========================================================================
     */
    private String normalizeRequired(
            String value,
            String fieldName
    ) {

        String normalized =
                normalizeOptional(value);

        if (normalized == null) {

            throw new IllegalArgumentException(
                    fieldName +
                            " cannot be null, empty, or blank."
            );
        }

        return normalized;
    }

    /**
     * =========================================================================
     * NORMALIZE VERSION
     * =========================================================================
     */
    private String normalizeVersion(
            String version
    ) {

        String normalized =
                normalizeOptional(version);

        if (normalized == null) {
            return DEFAULT_VERSION;
        }

        validateLength(
                normalized,
                MAX_VERSION_LENGTH,
                "Version"
        );

        return normalized;
    }

    /**
     * =========================================================================
     * NORMALIZE SOURCE URL
     * =========================================================================
     */
    private String normalizeSourceUrl(
            String sourceUrl
    ) {

        String normalized =
                normalizeOptional(sourceUrl);

        if (normalized == null) {
            return null;
        }

        validateLength(
                normalized,
                MAX_SOURCE_URL_LENGTH,
                "Source URL"
        );

        if (!isValidSourceUrl(normalized)) {

            throw new IllegalArgumentException(
                    "Source URL must be a valid HTTP or HTTPS URL."
            );
        }

        return normalized;
    }

    /**
     * =========================================================================
     * BASIC SOURCE URL VALIDATION
     * =========================================================================
     */
    private boolean isValidSourceUrl(
            String sourceUrl
    ) {

        try {

            URI uri =
                    URI.create(
                            sourceUrl
                    );

            String scheme =
                    uri.getScheme();

            return (
                    "http".equalsIgnoreCase(scheme)
                            ||
                            "https".equalsIgnoreCase(scheme)
            )
                    &&
                    uri.getHost() != null;

        } catch (IllegalArgumentException exception) {

            return false;
        }
    }

    /**
     * =========================================================================
     * STRING LENGTH VALIDATION
     * =========================================================================
     */
    private void validateLength(
            String value,
            int maximumLength,
            String fieldName
    ) {

        if (value != null &&
                value.length() > maximumLength) {

            throw new IllegalArgumentException(
                    fieldName +
                            " cannot exceed " +
                            maximumLength +
                            " characters."
            );
        }
    }

    /**
     * =========================================================================
     * VALIDATE DOCUMENT ID
     * =========================================================================
     */
    private void validateId(
            Long id
    ) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException(
                    "Knowledge document ID must be greater than zero."
            );
        }
    }

    /**
     * =========================================================================
     * KNOWLEDGE DOCUMENT NOT FOUND
     * =========================================================================
     */
    public static class KnowledgeDocumentNotFoundException
            extends RuntimeException {

        public KnowledgeDocumentNotFoundException(
                String message
        ) {
            super(message);
        }
    }

    /**
     * =========================================================================
     * DUPLICATE KNOWLEDGE DOCUMENT
     * =========================================================================
     */
    public static class KnowledgeDocumentAlreadyExistsException
            extends RuntimeException {

        public KnowledgeDocumentAlreadyExistsException(
                String message
        ) {
            super(message);
        }
    }

    /**
     * =========================================================================
     * KNOWLEDGE DOCUMENT DELETION FAILURE
     * =========================================================================
     */
    public static class KnowledgeDocumentDeletionException
            extends RuntimeException {

        public KnowledgeDocumentDeletionException(
                String message
        ) {
            super(message);
        }
    }
}
