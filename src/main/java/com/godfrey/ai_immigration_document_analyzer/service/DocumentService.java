package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.dto.response.DocumentResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.DocumentUploadResponse;
import com.godfrey.ai_immigration_document_analyzer.entity.Document;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.DocumentVersion;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItem;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItemStatus;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.DocumentVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.service.EvidenceItemLifecycleService;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.EvidenceSourceType;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.service.storage.S3FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * ============================================================================
 * DOCUMENT SERVICE
 * ============================================================================
 *
 * Central application service for document lifecycle management.
 *
 * Responsibilities:
 *
 * - Validate document uploads
 * - Store documents in S3
 * - Retrieve documents from S3 for processing
 * - Extract text through OCR
 * - Generate AI summaries
 * - Perform rule-based fraud indicators
 * - Calculate document risk
 * - Persist document metadata in Oracle
 * - Retrieve authenticated user's documents
 * - Retrieve a single owned document
 * - Delete owned documents
 *
 * SECURITY MODEL
 * ============================================================================
 *
 * This service NEVER determines the authenticated user itself.
 *
 * The caller/controller is responsible for resolving the authenticated
 * backend user ID and passing that ID into this service.
 *
 * The frontend must never be trusted to determine document ownership.
 *
 * Recommended flow:
 *
 * SecurityContext
 *       ↓
 * Controller
 *       ↓
 * authenticated user ID
 *       ↓
 * DocumentService
 *       ↓
 * DocumentRepository
 *
 * ============================================================================
 *
 * DATABASE
 * ============================================================================
 *
 * The Document entity maps to the Oracle DOCUMENTS table.
 *
 * Important fields include:
 *
 * ID
 * USER_ID
 * DOCUMENT_TYPE
 * FILE_NAME
 * FILE_PATH
 * FILE_SIZE
 * MIME_TYPE
 * EXTRACTED_TEXT
 * SUMMARY
 * FRAUD_DETECTED
 * RISK_LEVEL
 * UPLOAD_STATUS
 * UPLOADED_AT
 * UPDATED_AT
 *
 * ============================================================================
 *
 * ERROR HANDLING
 * ============================================================================
 *
 * Database failures are NOT converted into AI failures.
 *
 * OCR and AI failures are intentionally degraded where possible because
 * successful storage of the original document is still useful.
 *
 * Ownership failures are represented as ResourceNotFoundException so that
 * callers cannot determine whether another user's document exists.
 *
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    /**
     * Maximum application-level upload size.
     *
     * Spring multipart limits should also enforce this at the HTTP layer.
     */
    private static final long MAX_FILE_SIZE_BYTES =
            10L * 1024L * 1024L;

    private static final String MAX_FILE_SIZE_MESSAGE =
            "File size exceeds the maximum allowed size of 10 MB.";

    private static final String DEFAULT_UPLOAD_STATUS =
            "COMPLETED";

    private static final String LOW_RISK =
            "LOW";

    private static final String MEDIUM_RISK =
            "MEDIUM";

    private static final String HIGH_RISK =
            "HIGH";

    private static final String EMPTY_DOCUMENT_MESSAGE =
            "No readable content was extracted from this document.";

    private static final String SUMMARY_UNAVAILABLE_MESSAGE =
            "Summary is temporarily unavailable.";

    private static final String[] ALLOWED_EXTENSIONS = {
            "pdf",
            "png",
            "jpg",
            "jpeg"
    };

    private static final String[] ALLOWED_MIME_TYPES = {
            "application/pdf",
            "image/png",
            "image/jpeg"
    };

    private static final int MAX_DOCUMENT_TYPE_LENGTH = 100;

    private static final int MAX_FILE_NAME_LENGTH = 255;

    private static final int MIN_READABLE_TEXT_LENGTH_FOR_LOW_RISK = 50;

    // =========================================================================
    // DEPENDENCIES
    // =========================================================================

    private final S3FileStorageService s3FileStorageService;

    private final OcrService ocrService;

    private final DocumentRepository documentRepository;

    private final LlmService llmService;

    /**
     * Evidence Intelligence Graph write path (Phase 4 gap closure - see
     * docs/evidence-intelligence-graph.md). Populating these is optional
     * enrichment over the document this method already persists - never a
     * second document-processing pipeline, never a source of truth of its
     * own. See {@link #recordEvidenceIntelligence(Document, String, String)}.
     */
    private final DocumentVersionRepository documentVersionRepository;

    private final EvidenceItemRepository evidenceItemRepository;

    private final EvidenceItemLifecycleService evidenceItemLifecycleService;


    // =========================================================================
    // UPLOAD DOCUMENT
    // =========================================================================

    /**
     * Uploads and processes a document.
     *
     * Processing pipeline:
     *
     * 1. Validate input
     * 2. Normalize metadata
     * 3. Upload file to S3
     * 4. Download stored object
     * 5. OCR
     * 6. AI summary
     * 7. Fraud analysis
     * 8. Risk calculation
     * 9. Persist document metadata
     * 10. Return response
     *
     * @param userId authenticated backend user ID
     * @param documentType document classification
     * @param file uploaded multipart document
     * @return persisted document upload response
     */
    @Transactional
    public DocumentUploadResponse uploadDocument(
            Long userId,
            String documentType,
            MultipartFile file
    ) {

        validateInput(
                userId,
                documentType,
                file
        );

        final String normalizedDocumentType =
                normalizeDocumentType(documentType);

        final String originalFileName =
                sanitizeFileName(
                        file.getOriginalFilename()
                );

        final String mimeType =
                normalizeMimeType(
                        file.getContentType()
                );

        String s3ObjectKey = null;

        try {

            log.info(
                    "Starting document upload | userId={} | documentType={} | fileName={} | size={} | mimeType={}",
                    userId,
                    normalizedDocumentType,
                    originalFileName,
                    file.getSize(),
                    mimeType
            );

            // ================================================================
            // 1. UPLOAD TO S3
            // ================================================================

            s3ObjectKey =
                    s3FileStorageService.saveFile(
                            file,
                            userId
                    );

            if (!hasText(s3ObjectKey)) {

                throw new IllegalStateException(
                        "File storage service returned an invalid object key."
                );
            }

            log.debug(
                    "Document uploaded to storage | userId={} | objectKey={}",
                    userId,
                    s3ObjectKey
            );

            // ================================================================
            // 2. RETRIEVE STORED FILE
            // ================================================================

            byte[] fileBytes =
                    s3FileStorageService.downloadFile(
                            s3ObjectKey
                    );

            if (
                    fileBytes == null ||
                            fileBytes.length == 0
            ) {

                throw new IllegalStateException(
                        "Stored document could not be retrieved for processing."
                );
            }

            // ================================================================
            // 3. OCR
            // ================================================================

            String extractedText =
                    extractTextSafely(
                            fileBytes,
                            originalFileName,
                            mimeType
                    );

            log.info(
                    "Document text extraction completed | userId={} | fileName={} | characters={}",
                    userId,
                    originalFileName,
                    extractedText.length()
            );

            // ================================================================
            // 4. AI SUMMARY
            // ================================================================

            String summary =
                    generateSummarySafely(
                            extractedText
                    );

            // ================================================================
            // 5. FRAUD INDICATORS
            // ================================================================

            FraudResult fraudResult =
                    analyzeFraud(
                            extractedText
                    );

            // ================================================================
            // 6. RISK CLASSIFICATION
            // ================================================================

            String riskLevel =
                    calculateRiskLevel(
                            fraudResult,
                            extractedText
                    );

            // ================================================================
            // 7. BUILD DOCUMENT ENTITY
            // ================================================================

            LocalDateTime now =
                    LocalDateTime.now();

            Document document =
                    Document.builder()

                            .userId(
                                    userId
                            )

                            .documentType(
                                    normalizedDocumentType
                            )

                            .fileName(
                                    originalFileName
                            )

                            .filePath(
                                    s3ObjectKey
                            )

                            .fileSize(
                                    file.getSize()
                            )

                            .mimeType(
                                    mimeType
                            )

                            .extractedText(
                                    extractedText
                            )

                            .summary(
                                    summary
                            )

                            .fraudDetected(
                                    fraudResult.fraudDetected()
                            )

                            .riskLevel(
                                    riskLevel
                            )

                            .uploadStatus(
                                    DEFAULT_UPLOAD_STATUS
                            )

                            .uploadedAt(
                                    now
                            )

                            .updatedAt(
                                    now
                            )

                            .build();

            // ================================================================
            // 8. PERSIST IN ORACLE
            // ================================================================

            Document savedDocument =
                    documentRepository.save(
                            document
                    );

            if (savedDocument == null) {

                throw new IllegalStateException(
                        "Document persistence returned an empty result."
                );
            }

            log.info(
                    "Document persisted successfully | documentId={} | userId={} | risk={} | fraudDetected={}",
                    savedDocument.getId(),
                    userId,
                    savedDocument.getRiskLevel(),
                    savedDocument.getFraudDetected()
            );

            // ================================================================
            // 8b. EVIDENCE INTELLIGENCE GRAPH: DOCUMENT VERSION + EVIDENCE ITEM
            // ================================================================

            recordEvidenceIntelligence(
                    savedDocument,
                    extractedText,
                    mimeType
            );

            // ================================================================
            // 9. RESPONSE
            // ================================================================

            return DocumentUploadResponse.success(

                    savedDocument.getId(),

                    savedDocument.getUserId(),

                    savedDocument.getDocumentType(),

                    savedDocument.getFileName(),

                    savedDocument.getSummary(),

                    Boolean.TRUE.equals(
                            savedDocument.getFraudDetected()
                    ),

                    savedDocument.getRiskLevel(),

                    savedDocument.getUploadedAt()
            );

        } catch (DataAccessException exception) {

            /*
             * Oracle/JPA/database failure.
             *
             * Preserve the original DataAccessException so that the global
             * exception handler can distinguish database failures from
             * storage, OCR and AI failures.
             */

            cleanupUploadedFile(
                    s3ObjectKey
            );

            log.error(
                    "Database failure during document upload | userId={} | fileName={} | objectKey={}",
                    userId,
                    originalFileName,
                    s3ObjectKey,
                    exception
            );

            throw exception;

        } catch (IllegalArgumentException exception) {

            cleanupUploadedFile(
                    s3ObjectKey
            );

            log.warn(
                    "Document upload validation failure | userId={} | fileName={} | reason={}",
                    userId,
                    originalFileName,
                    exception.getMessage()
            );

            throw exception;

        } catch (IllegalStateException exception) {

            cleanupUploadedFile(
                    s3ObjectKey
            );

            log.error(
                    "Document processing state failure | userId={} | documentType={} | fileName={} | objectKey={}",
                    userId,
                    normalizedDocumentType,
                    originalFileName,
                    s3ObjectKey,
                    exception
            );

            throw exception;

        } catch (Exception exception) {

            cleanupUploadedFile(
                    s3ObjectKey
            );

            log.error(
                    "Unexpected document processing failure | userId={} | documentType={} | fileName={} | objectKey={}",
                    userId,
                    normalizedDocumentType,
                    originalFileName,
                    s3ObjectKey,
                    exception
            );

            throw new IllegalStateException(
                    "Unable to process the uploaded document.",
                    exception
            );
        }
    }


    // =========================================================================
    // GET USER DOCUMENTS
    // =========================================================================

    /**
     * Retrieves all documents owned by the authenticated user.
     *
     * Repository query:
     *
     * findByUserIdOrderByUploadedAtDesc(...)
     *
     * This ensures documents are returned newest first.
     *
     * @param userId authenticated backend user ID
     * @return document DTOs
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByUserId(
            Long userId
    ) {

        validateUserId(userId);

        try {

            List<Document> documents =
                    documentRepository
                            .findByUserIdOrderByUploadedAtDesc(
                                    userId
                            );

            if (
                    documents == null ||
                            documents.isEmpty()
            ) {

                log.debug(
                        "No documents found | userId={}",
                        userId
                );

                return List.of();
            }

            return documents
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::toDocumentResponse)
                    .toList();

        } catch (DataAccessException exception) {

            /*
             * IMPORTANT:
             *
             * This exception must remain a database exception.
             *
             * Do not convert Oracle/JPA failures into an AI error.
             */

            log.error(
                    "Database error retrieving documents | userId={}",
                    userId,
                    exception
            );

            throw exception;

        } catch (Exception exception) {

            log.error(
                    "Unexpected error retrieving documents | userId={}",
                    userId,
                    exception
            );

            throw new IllegalStateException(
                    "Unable to retrieve documents.",
                    exception
            );
        }
    }


    // =========================================================================
    // GET SINGLE DOCUMENT
    // =========================================================================

    /**
     * Retrieves one document belonging to the authenticated user.
     *
     * Ownership is enforced directly in the database query.
     *
     * This prevents insecure patterns such as:
     *
     * findById(documentId)
     *
     * followed by a client-side ownership check.
     *
     * @param documentId document ID
     * @param userId authenticated user ID
     * @return document response
     */
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(
            Long documentId,
            Long userId
    ) {

        validateDocumentId(documentId);

        validateUserId(userId);

        try {

            Document document =
                    documentRepository
                            .findByIdAndUserId(
                                    documentId,
                                    userId
                            )
                            .orElseThrow(
                                    () -> new ResourceNotFoundException(
                                            "Document not found."
                                    )
                            );

            return toDocumentResponse(
                    document
            );

        } catch (ResourceNotFoundException exception) {

            throw exception;

        } catch (DataAccessException exception) {

            log.error(
                    "Database error retrieving document | documentId={} | userId={}",
                    documentId,
                    userId,
                    exception
            );

            throw exception;

        } catch (Exception exception) {

            log.error(
                    "Unexpected error retrieving document | documentId={} | userId={}",
                    documentId,
                    userId,
                    exception
            );

            throw new IllegalStateException(
                    "Unable to retrieve document.",
                    exception
            );
        }
    }


    // =========================================================================
    // DELETE DOCUMENT
    // =========================================================================

    /**
     * Deletes a document owned by the authenticated user.
     *
     * Ownership is enforced through:
     *
     * findByIdAndUserId(documentId, userId)
     *
     * The S3 object is deleted before the database record.
     *
     * @param documentId document ID
     * @param userId authenticated user ID
     */
    @Transactional
    public void deleteDocument(
            Long documentId,
            Long userId
    ) {

        validateDocumentId(documentId);

        validateUserId(userId);

        final Document document;

        try {

            document =
                    documentRepository
                            .findByIdAndUserId(
                                    documentId,
                                    userId
                            )
                            .orElseThrow(
                                    () -> new ResourceNotFoundException(
                                            "Document not found."
                                    )
                            );

        } catch (ResourceNotFoundException exception) {

            throw exception;

        } catch (DataAccessException exception) {

            log.error(
                    "Database error locating document for deletion | documentId={} | userId={}",
                    documentId,
                    userId,
                    exception
            );

            throw exception;
        }

        final String s3ObjectKey =
                document.getFilePath();

        try {

            // ================================================================
            // DELETE STORAGE OBJECT
            // ================================================================

            if (hasText(s3ObjectKey)) {

                s3FileStorageService.deleteFile(
                        s3ObjectKey
                );

                log.debug(
                        "Document storage object deleted | documentId={} | objectKey={}",
                        documentId,
                        s3ObjectKey
                );
            }

            // ================================================================
            // DELETE DATABASE RECORD
            // ================================================================

            documentRepository.delete(
                    document
            );

            log.info(
                    "Document deleted successfully | documentId={} | userId={}",
                    documentId,
                    userId
            );

        } catch (DataAccessException exception) {

            log.error(
                    "Database error deleting document | documentId={} | userId={}",
                    documentId,
                    userId,
                    exception
            );

            throw exception;

        } catch (Exception exception) {

            log.error(
                    "Document deletion failed | documentId={} | userId={}",
                    documentId,
                    userId,
                    exception
            );

            throw new IllegalStateException(
                    "Unable to delete document.",
                    exception
            );
        }
    }


    // =========================================================================
    // EVIDENCE INTELLIGENCE GRAPH
    // =========================================================================

    /**
     * Closes the Phase 4 gap identified during the Evidence Graph audit: the
     * only production write path that can populate the Evidence Intelligence
     * Graph's additive {@code DocumentVersion}/{@code EvidenceItem} records.
     *
     * Reuses this document's own already-verified ownership (documentId,
     * userId) and the OCR result already computed above - never a second
     * OCR/extraction pipeline.
     *
     * Consistent with this method's existing treatment of OCR/AI failures:
     * a failure here is logged and swallowed, never allowed to fail the
     * upload, because the document itself is already safely stored and
     * remains useful without this optional graph enrichment (see this
     * class's SECURITY MODEL/ERROR HANDLING Javadoc above, and
     * {@code FactEvidence.evidenceItemId}'s own "optional enrichment, never
     * a replacement" contract). Whatever was already written before a
     * failure (e.g. a DocumentVersion with no EvidenceItem yet) is left as
     * a partial-but-honest graph fragment, exactly the same shape
     * {@code EvidenceGraphService} already renders correctly today when no
     * richer record exists yet - never a fabricated or misleading one.
     *
     * Idempotent: a {@code DocumentVersion} is created only when none exists
     * yet for this document id. The current upload flow always creates a
     * brand new {@code Document} row (there is no "reprocess an existing
     * document" entry point), so this guard is defensive rather than
     * routinely exercised - but it means a future retry/reprocessing path
     * can call this method safely without producing duplicates.
     */
    private void recordEvidenceIntelligence(
            Document document,
            String extractedText,
            String mimeType
    ) {

        try {

            List<DocumentVersion> existingVersions =
                    documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(
                            document.getId()
                    );

            if (!existingVersions.isEmpty()) {

                log.debug(
                        "DocumentVersion already exists for this document - skipping to avoid a duplicate | documentId={}",
                        document.getId()
                );

                return;
            }

            DocumentVersion version =
                    documentVersionRepository.save(
                            DocumentVersion.builder()
                                    .documentId(document.getId())
                                    .versionNumber(1)
                                    .extractionMethod(extractionMethodFor(mimeType))
                                    .build()
                    );

            EvidenceItem item =
                    evidenceItemRepository.save(
                            EvidenceItem.builder()
                                    .documentVersionId(version.getId())
                                    .sourceType(EvidenceSourceType.DOCUMENT)
                                    .status(EvidenceItemStatus.DISCOVERED)
                                    .build()
                    );

            item = evidenceItemLifecycleService.transition(
                    item,
                    EvidenceItemStatus.EXTRACTED
            );

            boolean hasReadableText =
                    extractedText != null && !extractedText.isBlank();

            if (hasReadableText) {

                item.setSourceSnippet(
                        truncate(extractedText, 500)
                );
            }

            item = evidenceItemLifecycleService.transition(
                    item,
                    hasReadableText
                            ? EvidenceItemStatus.CANDIDATE
                            : EvidenceItemStatus.VALIDATION_FAILED
            );

            log.info(
                    "Evidence Intelligence Graph populated | documentId={} | documentVersionId={} | evidenceItemId={} | status={}",
                    document.getId(),
                    version.getId(),
                    item.getId(),
                    item.getStatus()
            );

        } catch (Exception exception) {

            log.error(
                    "Failed to populate the Evidence Intelligence Graph for this document - the document itself remains stored | documentId={}",
                    document.getId(),
                    exception
            );
        }
    }

    /**
     * A truthful label for how this document's text was produced - mirrors
     * {@code OcrService}'s own PDF-vs-image branching (PDFBox native text
     * extraction vs. Tesseract OCR) without re-implementing or duplicating
     * that extraction itself.
     */
    private String extractionMethodFor(
            String mimeType
    ) {

        if ("application/pdf".equals(mimeType)) {
            return "PDFBOX_NATIVE_TEXT";
        }

        if ("image/png".equals(mimeType) || "image/jpeg".equals(mimeType)) {
            return "TESSERACT_OCR";
        }

        return "UNKNOWN";
    }

    /**
     * Same data-minimization discipline as {@code FactService.truncate} -
     * never stores more than a short excerpt.
     */
    private String truncate(
            String value,
            int maxLength
    ) {

        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }


    // =========================================================================
    // ENTITY → DTO
    // =========================================================================

    /**
     * Converts the JPA entity to the API DTO.
     *
     * Keeping conversion in the service prevents entities from leaking
     * directly into controller responses.
     */
    private DocumentResponse toDocumentResponse(
            Document document
    ) {

        if (document == null) {

            throw new IllegalStateException(
                    "Document entity cannot be null."
            );
        }

        return DocumentResponse.from(
                document
        );
    }


    // =========================================================================
    // OCR
    // =========================================================================

    /**
     * Performs OCR/text extraction.
     *
     * OCR failure does not invalidate the uploaded file.
     *
     * An empty extracted text result allows the document to remain stored
     * while clearly indicating that readable content was unavailable.
     */
    private String extractTextSafely(
            byte[] fileBytes,
            String fileName,
            String mimeType
    ) {

        if (
                fileBytes == null ||
                        fileBytes.length == 0
        ) {

            return "";
        }

        try {

            String extractedText =
                    ocrService.extractText(
                            fileBytes,
                            fileName,
                            mimeType
                    );

            if (
                    extractedText == null ||
                            extractedText.isBlank()
            ) {

                log.warn(
                        "OCR returned no readable content | fileName={} | mimeType={}",
                        fileName,
                        mimeType
                );

                return "";
            }

            return extractedText.trim();

        } catch (Exception exception) {

            log.error(
                    "OCR processing failed | fileName={} | mimeType={}",
                    fileName,
                    mimeType,
                    exception
            );

            return "";
        }
    }


    // =========================================================================
    // AI SUMMARY
    // =========================================================================

    /**
     * Generates an AI summary from extracted document text.
     *
     * AI failures are deliberately isolated from document persistence.
     */
    private String generateSummarySafely(
            String text
    ) {

        if (
                text == null ||
                        text.isBlank()
        ) {

            return EMPTY_DOCUMENT_MESSAGE;
        }

        try {

            String summary =
                    llmService.generateSummary(
                            text
                    );

            if (
                    summary == null ||
                            summary.isBlank()
            ) {

                log.warn(
                        "AI summary service returned empty content."
                );

                return SUMMARY_UNAVAILABLE_MESSAGE;
            }

            return summary.trim();

        } catch (Exception exception) {

            log.error(
                    "AI summary generation failed.",
                    exception
            );

            return SUMMARY_UNAVAILABLE_MESSAGE;
        }
    }


    // =========================================================================
    // FRAUD ANALYSIS
    // =========================================================================

    /**
     * Performs rule-based fraud indicator analysis.
     *
     * IMPORTANT:
     *
     * This is a risk-indicator system and NOT a forensic document
     * authenticity determination.
     */
    private FraudResult analyzeFraud(
            String text
    ) {

        if (
                text == null ||
                        text.isBlank()
        ) {

            return new FraudResult(
                    false,
                    0
            );
        }

        String normalizedText =
                text.toLowerCase(
                        Locale.ROOT
                );

        int score = 0;

        score +=
                keywordScore(
                        normalizedText,
                        "fake",
                        40
                );

        score +=
                keywordScore(
                        normalizedText,
                        "forged",
                        40
                );

        score +=
                keywordScore(
                        normalizedText,
                        "altered",
                        30
                );

        score +=
                keywordScore(
                        normalizedText,
                        "counterfeit",
                        40
                );

        score +=
                keywordScore(
                        normalizedText,
                        "fraudulent",
                        40
                );

        score +=
                keywordScore(
                        normalizedText,
                        "tampered",
                        35
                );

        score +=
                keywordScore(
                        normalizedText,
                        "invalid document",
                        30
                );

        score +=
                keywordScore(
                        normalizedText,
                        "copy",
                        10
                );

        score =
                Math.min(
                        score,
                        100
                );

        return new FraudResult(
                score >= 50,
                score
        );
    }


    /**
     * Returns the configured score when a keyword is present.
     */
    private int keywordScore(
            String text,
            String keyword,
            int score
    ) {

        return text.contains(keyword)
                ? score
                : 0;
    }


    // =========================================================================
    // RISK CALCULATION
    // =========================================================================

    /**
     * Calculates a document risk indicator.
     *
     * HIGH:
     *     Explicit fraud indicators detected.
     *
     * MEDIUM:
     *     Suspicious indicators exist or insufficient text was extracted.
     *
     * LOW:
     *     No fraud indicators and sufficient readable content exists.
     */
    private String calculateRiskLevel(
            FraudResult fraudResult,
            String text
    ) {

        if (
                fraudResult != null &&
                        fraudResult.fraudDetected()
        ) {

            return HIGH_RISK;
        }

        if (
                fraudResult != null &&
                        fraudResult.score() > 20
        ) {

            return MEDIUM_RISK;
        }

        if (
                text == null ||
                        text.length() < MIN_READABLE_TEXT_LENGTH_FOR_LOW_RISK
        ) {

            return MEDIUM_RISK;
        }

        return LOW_RISK;
    }


    // =========================================================================
    // INPUT VALIDATION
    // =========================================================================

    private void validateInput(
            Long userId,
            String documentType,
            MultipartFile file
    ) {

        validateUserId(
                userId
        );

        // ---------------------------------------------------------------------
        // DOCUMENT TYPE
        // ---------------------------------------------------------------------

        if (
                !hasText(documentType)
        ) {

            throw new IllegalArgumentException(
                    "Document type cannot be empty."
            );
        }

        if (
                documentType.trim().length()
                        > MAX_DOCUMENT_TYPE_LENGTH
        ) {

            throw new IllegalArgumentException(
                    "Document type is too long."
            );
        }

        // ---------------------------------------------------------------------
        // FILE
        // ---------------------------------------------------------------------

        if (file == null) {

            throw new IllegalArgumentException(
                    "Uploaded file is required."
            );
        }

        if (file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Uploaded file cannot be empty."
            );
        }

        if (
                file.getSize()
                        > MAX_FILE_SIZE_BYTES
        ) {

            throw new IllegalArgumentException(
                    MAX_FILE_SIZE_MESSAGE
            );
        }

        validateFileName(
                file.getOriginalFilename()
        );

        validateFileType(
                file
        );
    }


    // =========================================================================
    // FILE TYPE VALIDATION
    // =========================================================================

    private void validateFileType(
            MultipartFile file
    ) {

        String extension =
                extractExtension(
                        file.getOriginalFilename()
                );

        String mimeType =
                normalizeMimeType(
                        file.getContentType()
                );

        boolean validExtension =
                isAllowedExtension(
                        extension
                );

        boolean validMimeType =
                isAllowedMimeType(
                        mimeType
                );

        if (
                !validExtension ||
                        !validMimeType
        ) {

            throw new IllegalArgumentException(
                    "Unsupported file type. Supported formats: PDF, PNG, JPG and JPEG."
            );
        }

        if (
                !extensionMatchesMime(
                        extension,
                        mimeType
                )
        ) {

            throw new IllegalArgumentException(
                    "File extension does not match the declared content type."
            );
        }
    }


    private boolean isAllowedExtension(
            String extension
    ) {

        if (!hasText(extension)) {
            return false;
        }

        for (
                String allowedExtension :
                ALLOWED_EXTENSIONS
        ) {

            if (
                    allowedExtension.equals(
                            extension
                    )
            ) {

                return true;
            }
        }

        return false;
    }


    private boolean isAllowedMimeType(
            String mimeType
    ) {

        if (!hasText(mimeType)) {
            return false;
        }

        for (
                String allowedMimeType :
                ALLOWED_MIME_TYPES
        ) {

            if (
                    allowedMimeType.equals(
                            mimeType
                    )
            ) {

                return true;
            }
        }

        return false;
    }


    private boolean extensionMatchesMime(
            String extension,
            String mimeType
    ) {

        return switch (extension) {

            case "pdf" ->
                    "application/pdf".equals(
                            mimeType
                    );

            case "png" ->
                    "image/png".equals(
                            mimeType
                    );

            case "jpg",
                 "jpeg" ->
                    "image/jpeg".equals(
                            mimeType
                    );

            default ->
                    false;
        };
    }


    // =========================================================================
    // FILE NAME VALIDATION
    // =========================================================================

    private void validateFileName(
            String fileName
    ) {

        if (
                !hasText(fileName)
        ) {

            throw new IllegalArgumentException(
                    "Uploaded file must have a valid filename."
            );
        }

        String normalized =
                fileName
                        .trim()
                        .replace(
                                '\\',
                                '/'
                        );

        if (
                normalized.equals("..") ||
                        normalized.contains("../")
        ) {

            throw new IllegalArgumentException(
                    "Invalid file name."
            );
        }

        /*
         * A browser should normally send only a filename. Reject suspicious
         * absolute/path-like values instead of attempting to trust them.
         */
        if (
                normalized.startsWith("/") ||
                        normalized.contains(":")
        ) {

            throw new IllegalArgumentException(
                    "Invalid file name."
            );
        }
    }


    // =========================================================================
    // FILE EXTENSION
    // =========================================================================

    private String extractExtension(
            String fileName
    ) {

        if (
                !hasText(fileName)
        ) {

            return "";
        }

        String normalized =
                fileName
                        .trim()
                        .replace(
                                '\\',
                                '/'
                        );

        int slashIndex =
                normalized.lastIndexOf('/');

        if (slashIndex >= 0) {

            normalized =
                    normalized.substring(
                            slashIndex + 1
                    );
        }

        int dotIndex =
                normalized.lastIndexOf('.');

        if (
                dotIndex < 0 ||
                        dotIndex == normalized.length() - 1
        ) {

            return "";
        }

        return normalized
                .substring(
                        dotIndex + 1
                )
                .toLowerCase(
                        Locale.ROOT
                );
    }


    // =========================================================================
    // DOCUMENT TYPE NORMALIZATION
    // =========================================================================

    private String normalizeDocumentType(
            String value
    ) {

        if (
                !hasText(value)
        ) {

            throw new IllegalArgumentException(
                    "Document type cannot be empty."
            );
        }

        return value
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                )
                .toUpperCase(
                        Locale.ROOT
                );
    }


    // =========================================================================
    // MIME TYPE NORMALIZATION
    // =========================================================================

    private String normalizeMimeType(
            String value
    ) {

        if (
                !hasText(value)
        ) {

            return "application/octet-stream";
        }

        String normalized =
                value
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        int semicolonIndex =
                normalized.indexOf(';');

        if (semicolonIndex >= 0) {

            normalized =
                    normalized.substring(
                            0,
                            semicolonIndex
                    ).trim();
        }

        return normalized;
    }


    // =========================================================================
    // FILE NAME SANITIZATION
    // =========================================================================

    private String sanitizeFileName(
            String fileName
    ) {

        if (
                !hasText(fileName)
        ) {

            return "unknown";
        }

        String normalized =
                fileName
                        .trim()
                        .replace(
                                '\\',
                                '/'
                        );

        int slashIndex =
                normalized.lastIndexOf('/');

        if (slashIndex >= 0) {

            normalized =
                    normalized.substring(
                            slashIndex + 1
                    );
        }

        /*
         * Remove control characters.
         */
        normalized =
                normalized.replaceAll(
                        "[\\p{Cntrl}]",
                        ""
                );

        /*
         * Remove traversal fragments.
         */
        normalized =
                normalized.replace(
                        "..",
                        ""
                );

        /*
         * Keep filename within common filesystem/database limits.
         */
        if (
                normalized.length()
                        > MAX_FILE_NAME_LENGTH
        ) {

            normalized =
                    normalized.substring(
                            0,
                            MAX_FILE_NAME_LENGTH
                    );
        }

        return hasText(normalized)
                ? normalized
                : "unknown";
    }


    // =========================================================================
    // USER ID VALIDATION
    // =========================================================================

    private void validateUserId(
            Long userId
    ) {

        if (
                userId == null ||
                        userId <= 0
        ) {

            throw new IllegalArgumentException(
                    "Invalid authenticated user ID."
            );
        }
    }


    // =========================================================================
    // DOCUMENT ID VALIDATION
    // =========================================================================

    private void validateDocumentId(
            Long documentId
    ) {

        if (
                documentId == null ||
                        documentId <= 0
        ) {

            throw new IllegalArgumentException(
                    "Invalid document ID."
            );
        }
    }


    // =========================================================================
    // STRING VALIDATION
    // =========================================================================

    private boolean hasText(
            String value
    ) {

        return value != null &&
                !value.isBlank();
    }


    // =========================================================================
    // S3 CLEANUP
    // =========================================================================

    /**
     * Best-effort cleanup of an S3 object created during a failed upload.
     *
     * Cleanup errors never replace the original application failure.
     */
    private void cleanupUploadedFile(
            String objectKey
    ) {

        if (
                !hasText(objectKey)
        ) {

            return;
        }

        try {

            s3FileStorageService.deleteFile(
                    objectKey
            );

            log.debug(
                    "Uploaded S3 object cleaned up successfully | objectKey={}",
                    objectKey
            );

        } catch (Exception cleanupException) {

            log.error(
                    "Failed to clean up S3 object after document failure | objectKey={}",
                    objectKey,
                    cleanupException
            );
        }
    }


    // =========================================================================
    // FRAUD RESULT
    // =========================================================================

    private record FraudResult(
            boolean fraudDetected,
            int score
    ) {
    }
}