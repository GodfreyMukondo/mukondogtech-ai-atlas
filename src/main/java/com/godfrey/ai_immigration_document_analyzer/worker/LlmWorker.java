package com.godfrey.ai_immigration_document_analyzer.worker;

import com.godfrey.ai_immigration_document_analyzer.events.DocumentUploadedEvent;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.service.LlmService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * ============================================================================
 * LLM DOCUMENT WORKER
 * ============================================================================
 *
 * Kafka-driven asynchronous worker responsible for generating AI summaries
 * from text previously extracted from uploaded immigration documents.
 *
 * Processing pipeline:
 *
 *     document.uploaded
 *             |
 *             v
 *     Retrieve extracted text
 *             |
 *             v
 *     Validate text
 *             |
 *             v
 *     Generate AI summary
 *             |
 *             v
 *     Validate summary
 *             |
 *             v
 *     Persist summary
 *
 * ============================================================================
 *
 * Production responsibilities:
 *
 * - Consume document.uploaded Kafka events.
 * - Validate incoming events.
 * - Retrieve extracted text safely using Optional.
 * - Avoid database transactions during long-running LLM requests.
 * - Generate an AI summary.
 * - Validate the LLM response.
 * - Persist the generated summary.
 * - Detect unsuccessful database updates.
 * - Log processing lifecycle and failures.
 * - Propagate failures to Spring Kafka error handling.
 *
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmWorker {

    /**
     * Repository responsible for document persistence and extracted-text
     * retrieval.
     */
    private final DocumentRepository repository;

    /**
     * Service responsible for communicating with the configured LLM provider.
     */
    private final LlmService llmService;

    /**
     * Kafka topic consumed by this worker.
     */
    private static final String DOCUMENT_UPLOADED_TOPIC =
            "document.uploaded";

    /**
     * Dedicated consumer group for LLM processing.
     *
     * This allows the LLM worker to consume the same document-upload event
     * independently from OCR and fraud-analysis consumers.
     */
    private static final String LLM_CONSUMER_GROUP =
            "llm-group";

    /**
     * =========================================================================
     * KAFKA MESSAGE CONSUMER
     * =========================================================================
     *
     * Consumes document.uploaded events.
     *
     * IMPORTANT:
     *
     * No @Transactional annotation is placed on this method because the
     * LLM operation can be slow and should not hold a database transaction
     * open while waiting for an external AI provider.
     *
     * Database persistence should be transactional at the repository/service
     * layer.
     * =========================================================================
     */
    @KafkaListener(
            topics = DOCUMENT_UPLOADED_TOPIC,
            groupId = LLM_CONSUMER_GROUP
    )
    public void process(
            DocumentUploadedEvent event
    ) {

        /*
         * =====================================================================
         * 1. VALIDATE EVENT
         * =====================================================================
         */
        if (!isValidEvent(event)) {

            log.warn(
                    "Ignoring invalid document.uploaded event received by " +
                            "LLM worker"
            );

            return;
        }

        final Long documentId =
                event.documentId();

        try {

            log.info(
                    "Starting LLM document processing | documentId={}",
                    documentId
            );

            /*
             * =================================================================
             * 2. RETRIEVE EXTRACTED TEXT
             * =================================================================
             *
             * DocumentRepository intentionally returns Optional<String>.
             *
             * This correctly handles situations where:
             *
             * - The document does not exist.
             * - extractedText is NULL.
             * - The query returns no result.
             */
            Optional<String> extractedText =
                    repository.findExtractedTextById(
                            documentId
                    );

            /*
             * =================================================================
             * 3. DOCUMENT / TEXT NOT FOUND
             * =================================================================
             */
            if (extractedText.isEmpty()) {

                log.warn(
                        "No extracted text found for LLM processing | " +
                                "documentId={}",
                        documentId
                );

                return;
            }

            /*
             * =================================================================
             * 4. NORMALIZE EXTRACTED TEXT
             * =================================================================
             */
            final String normalizedText =
                    extractedText
                            .get()
                            .trim();

            /*
             * =================================================================
             * 5. VALIDATE EXTRACTED TEXT
             * =================================================================
             */
            if (!StringUtils.hasText(normalizedText)) {

                log.warn(
                        "Extracted text is empty or blank | documentId={}",
                        documentId
                );

                return;
            }

            log.debug(
                    "Extracted text retrieved successfully | " +
                            "documentId={} | characters={}",
                    documentId,
                    normalizedText.length()
            );

            /*
             * =================================================================
             * 6. GENERATE AI SUMMARY
             * =================================================================
             *
             * This is potentially a network-bound and relatively expensive
             * operation.
             *
             * No database transaction is held open while this operation
             * executes.
             */
            final String summary =
                    llmService.generateSummary(
                            normalizedText
                    );

            /*
             * =================================================================
             * 7. VALIDATE LLM RESPONSE
             * =================================================================
             */
            if (!StringUtils.hasText(summary)) {

                log.warn(
                        "LLM returned an empty summary | documentId={}",
                        documentId
                );

                return;
            }

            final String normalizedSummary =
                    summary.trim();

            log.debug(
                    "LLM summary generated successfully | " +
                            "documentId={} | characters={}",
                    documentId,
                    normalizedSummary.length()
            );

            /*
             * =================================================================
             * 8. PERSIST SUMMARY
             * =================================================================
             *
             * updateSummary(...) should return the number of affected rows.
             *
             * Expected result:
             *
             *     1 = document updated successfully
             *     0 = document could not be updated
             */
            final int updatedRows =
                    repository.updateSummary(
                            documentId,
                            normalizedSummary
                    );

            /*
             * =================================================================
             * 9. VERIFY DATABASE UPDATE
             * =================================================================
             */
            if (updatedRows <= 0) {

                log.warn(
                        "LLM summary generated but no document was updated | " +
                                "documentId={} | updatedRows={}",
                        documentId,
                        updatedRows
                );

                /*
                 * Treat this as a processing failure rather than silently
                 * reporting success.
                 *
                 * Throwing allows Spring Kafka's configured error handler
                 * to perform retry/dead-letter processing.
                 */
                throw new IllegalStateException(
                        "Unable to persist LLM summary for documentId="
                                + documentId
                );
            }

            /*
             * =================================================================
             * 10. SUCCESS
             * =================================================================
             */
            log.info(
                    "LLM document processing completed successfully | " +
                            "documentId={} | updatedRows={}",
                    documentId,
                    updatedRows
            );

        } catch (Exception exception) {

            /*
             * =================================================================
             * ERROR HANDLING
             * =================================================================
             *
             * Do not silently swallow processing failures.
             *
             * Re-throwing the exception allows the configured Spring Kafka
             * CommonErrorHandler / DefaultErrorHandler to perform retries
             * and, when configured, dead-letter processing.
             */
            log.error(
                    "LLM document processing failed | documentId={}",
                    documentId,
                    exception
            );

            throw exception;
        }
    }

    /**
     * =========================================================================
     * EVENT VALIDATION
     * =========================================================================
     *
     * Validates the minimum information required for LLM processing.
     *
     * The document ID is mandatory because the worker uses it to retrieve
     * extracted text and persist the generated summary.
     */
    private boolean isValidEvent(
            DocumentUploadedEvent event
    ) {

        return event != null
                && event.documentId() != null;
    }
}

