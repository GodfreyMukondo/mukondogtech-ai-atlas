package com.godfrey.ai_immigration_document_analyzer.stream;

import com.godfrey.ai_immigration_document_analyzer.analytics.entity.FraudAnalyticsEntity;
import com.godfrey.ai_immigration_document_analyzer.analytics.repository.FraudAnalyticsRepository;
import com.godfrey.ai_immigration_document_analyzer.events.DocumentUploadedEvent;
import com.godfrey.ai_immigration_document_analyzer.fraud.FraudScoreResult;
import com.godfrey.ai_immigration_document_analyzer.fraud.FraudScoringEngine;
import com.godfrey.ai_immigration_document_analyzer.service.OcrService;
import com.godfrey.ai_immigration_document_analyzer.service.storage.S3FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * ============================================================================
 * FRAUD ANALYTICS KAFKA CONSUMER
 * ============================================================================
 *
 * Consumes document.uploaded events and performs asynchronous fraud analysis.
 *
 * Processing pipeline:
 *
 * Kafka
 *   ↓
 * Validate event
 *   ↓
 * Download document from S3
 *   ↓
 * Extract document text
 *   ↓
 * Run fraud scoring
 *   ↓
 * Persist fraud analytics
 *
 * ============================================================================
 *
 * IMPORTANT
 * ============================================================================
 *
 * The existing FraudAnalyticsEntity is intentionally NOT modified.
 *
 * It currently stores:
 *
 * - ruleId
 * - fraudScore
 * - fraudFlag
 * - confidence
 * - createdAt
 *
 * Therefore this consumer does not call:
 *
 *     entity.setDocumentId(...)
 *
 * because that property does not exist in the entity/database schema.
 *
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAnalyticsConsumer {

    private final FraudAnalyticsRepository repository;

    private final FraudScoringEngine scoringEngine;

    private final OcrService ocrService;

    private final S3FileStorageService s3FileStorageService;


    /**
     * =========================================================================
     * KAFKA LISTENER
     * =========================================================================
     *
     * Consumes document.uploaded events.
     */
    @KafkaListener(
            topics = "document.uploaded",
            groupId = "fraud-analytics-group"
    )
    public void process(
            DocumentUploadedEvent event
    ) {

        /*
         * -------------------------------------------------------------
         * Validate event
         * -------------------------------------------------------------
         */
        if (!isValidEvent(event)) {

            log.warn(
                    "Ignoring invalid document.uploaded event"
            );

            return;
        }


        final Long documentId =
                event.documentId();

        final String filePath =
                event.filePath();


        try {

            log.info(
                    "Starting fraud analytics | documentId={} | filePath={}",
                    documentId,
                    filePath
            );


            /*
             * =============================================================
             * 1. DOWNLOAD DOCUMENT FROM S3
             * =============================================================
             *
             * filePath is treated as the S3 object key.
             *
             * Example:
             *
             * documents/2026/08/abc-123/passport.pdf
             */
            byte[] documentBytes =
                    s3FileStorageService.downloadFile(
                            filePath
                    );


            if (documentBytes == null ||
                    documentBytes.length == 0) {

                throw new IllegalStateException(
                        "Downloaded document is empty."
                );
            }


            log.debug(
                    "Document downloaded successfully | documentId={} | size={} bytes",
                    documentId,
                    documentBytes.length
            );


            /*
             * =============================================================
             * 2. RESOLVE FILE NAME
             * =============================================================
             */
            String fileName =
                    extractFileName(
                            filePath
                    );


            /*
             * =============================================================
             * 3. RESOLVE MIME TYPE
             * =============================================================
             */
            String mimeType =
                    resolveMimeType(
                            fileName
                    );


            /*
             * =============================================================
             * 4. EXTRACT TEXT
             * =============================================================
             *
             * New OcrService contract:
             *
             *     extractText(
             *         byte[],
             *         String,
             *         String
             *     )
             */
            String text =
                    ocrService.extractText(
                            documentBytes,
                            fileName,
                            mimeType
                    );


            if (text == null) {

                text = "";
            }


            log.info(
                    "Document text extraction completed | documentId={} | characters={}",
                    documentId,
                    text.length()
            );


            /*
             * =============================================================
             * 5. FRAUD ANALYSIS
             * =============================================================
             */
            FraudScoreResult result =
                    scoringEngine.analyze(
                            text
                    );


            if (result == null) {

                throw new IllegalStateException(
                        "Fraud scoring engine returned null result."
                );
            }


            log.info(
                    "Fraud analysis completed | documentId={} | score={} | fraudDetected={} | confidence={}",
                    documentId,
                    result.score(),
                    result.fraudDetected(),
                    result.confidence()
            );


            /*
             * =============================================================
             * 6. CREATE FRAUD ANALYTICS ENTITY
             * =============================================================
             *
             * IMPORTANT:
             *
             * Do not call:
             *
             *     entity.setDocumentId(...)
             *
             * because FraudAnalyticsEntity does not currently contain
             * a documentId property.
             */
            FraudAnalyticsEntity entity =
                    new FraudAnalyticsEntity();


            entity.setRuleId(
                    result.ruleId()
            );


            entity.setFraudScore(
                    result.score()
            );


            entity.setFraudFlag(
                    result.fraudDetected()
            );


            entity.setConfidence(
                    result.confidence()
            );


            /*
             * createdAt is intentionally not manually assigned.
             *
             * FraudAnalyticsEntity uses @PrePersist to populate it.
             */


            /*
             * =============================================================
             * 7. PERSIST RESULT
             * =============================================================
             */
            repository.save(
                    entity
            );


            log.info(
                    "Fraud analytics persisted successfully | documentId={} | score={}",
                    documentId,
                    result.score()
            );


        } catch (Exception exception) {

            log.error(
                    "Fraud analytics processing failed | documentId={} | filePath={}",
                    documentId,
                    filePath,
                    exception
            );


            /*
             * Re-throw the exception so Spring Kafka's configured error
             * handler can perform retry/DLT processing.
             */
            throw exception;
        }
    }


    /**
     * =========================================================================
     * EVENT VALIDATION
     * =========================================================================
     */
    private boolean isValidEvent(
            DocumentUploadedEvent event
    ) {

        return event != null
                && event.documentId() != null
                && StringUtils.hasText(
                event.filePath()
        );
    }


    /**
     * =========================================================================
     * FILE NAME EXTRACTION
     * =========================================================================
     *
     * Extracts the filename from the S3 object key.
     *
     * Example:
     *
     * documents/2026/08/passport.pdf
     *
     * becomes:
     *
     * passport.pdf
     */
    private String extractFileName(
            String filePath
    ) {

        if (!StringUtils.hasText(filePath)) {

            return "document";
        }


        String normalizedPath =
                filePath.trim();


        int lastSlash =
                normalizedPath.lastIndexOf('/');


        if (lastSlash >= 0 &&
                lastSlash < normalizedPath.length() - 1) {

            return normalizedPath.substring(
                    lastSlash + 1
            );
        }


        return normalizedPath;
    }


    /**
     * =========================================================================
     * MIME TYPE RESOLUTION
     * =========================================================================
     *
     * Resolves MIME type from the filename.
     *
     * OcrService uses MIME type and filename to determine how the document
     * should be processed.
     */
    private String resolveMimeType(
            String fileName
    ) {

        if (!StringUtils.hasText(fileName)) {

            return "application/octet-stream";
        }


        String normalizedFileName =
                fileName
                        .trim()
                        .toLowerCase(Locale.ROOT);


        if (normalizedFileName.endsWith(".pdf")) {

            return "application/pdf";
        }


        if (normalizedFileName.endsWith(".png")) {

            return "image/png";
        }


        if (normalizedFileName.endsWith(".jpg") ||
                normalizedFileName.endsWith(".jpeg")) {

            return "image/jpeg";
        }


        if (normalizedFileName.endsWith(".doc")) {

            return "application/msword";
        }


        if (normalizedFileName.endsWith(".docx")) {

            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }


        return "application/octet-stream";
    }
}

