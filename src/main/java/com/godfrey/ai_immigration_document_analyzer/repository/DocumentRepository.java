package com.godfrey.ai_immigration_document_analyzer.repository;

import com.godfrey.ai_immigration_document_analyzer.entity.Document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


/**
 * ============================================================================
 * DOCUMENT REPOSITORY
 * ============================================================================
 */
@Repository
public interface DocumentRepository
        extends JpaRepository<Document, Long> {


    // =========================================================================
    // USER DOCUMENTS
    // =========================================================================

    List<Document> findByUserIdOrderByUploadedAtDesc(
            Long userId
    );


    /**
     * Secure ownership-aware document lookup.
     *
     * This is preferable to:
     *
     * findById(...)
     *
     * followed by a manual ownership check.
     */
    Optional<Document> findByIdAndUserId(
            Long id,
            Long userId
    );


    long countByUserId(
            Long userId
    );


    // =========================================================================
    // DOCUMENT TYPE
    // =========================================================================

    long countByDocumentType(
            String documentType
    );


    // =========================================================================
    // UPLOAD STATUS
    // =========================================================================

    long countByUploadStatus(
            String uploadStatus
    );


    default long countPendingDocuments() {

        return countByUploadStatus(
                "PENDING"
        );
    }


    @Query("""
            SELECT COUNT(d)
            FROM Document d
            WHERE UPPER(d.uploadStatus) IN (
                'PENDING',
                'PROCESSING',
                'ANALYZING'
            )
            """)
    long countAwaitingDocuments();


    @Query("""
            SELECT COUNT(d)
            FROM Document d
            WHERE UPPER(d.uploadStatus) IN (
                'SUCCESS',
                'COMPLETED'
            )
            """)
    long countCompletedDocuments();


    @Query("""
            SELECT COUNT(d)
            FROM Document d
            WHERE UPPER(d.uploadStatus) = 'FAILED'
            """)
    long countFailedDocuments();


    // =========================================================================
    // FRAUD
    // =========================================================================

    long countByFraudDetectedTrue();


    long countByFraudDetectedFalse();


    long countByFraudDetected(
            Boolean fraudDetected
    );


    // =========================================================================
    // RISK
    // =========================================================================

    long countByRiskLevel(
            String riskLevel
    );


    default long countHighRiskDocuments() {

        return countByRiskLevel(
                "HIGH"
        );
    }


    default long countMediumRiskDocuments() {

        return countByRiskLevel(
                "MEDIUM"
        );
    }


    default long countLowRiskDocuments() {

        return countByRiskLevel(
                "LOW"
        );
    }


    // =========================================================================
    // DATE ANALYTICS
    // =========================================================================

    long countByUploadedAtAfter(
            LocalDateTime date
    );


    long countByUploadedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );


    long countByUpdatedAtAfter(
            LocalDateTime date
    );


    long countByUserIdAndUploadedAtBetween(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );


    // =========================================================================
    // TODAY
    // =========================================================================

    default long countToday() {

        LocalDateTime startOfToday =
                LocalDateTime
                        .now()
                        .toLocalDate()
                        .atStartOfDay();


        LocalDateTime startOfTomorrow =
                startOfToday.plusDays(1);


        return countByUploadedAtBetween(
                startOfToday,
                startOfTomorrow
        );
    }


    // =========================================================================
    // STATUS + DATE
    // =========================================================================

    long countByUploadStatusAndUploadedAtAfter(
            String uploadStatus,
            LocalDateTime date
    );


    // =========================================================================
    // DOCUMENT TYPE + STATUS
    // =========================================================================

    long countByDocumentTypeAndUploadStatus(
            String documentType,
            String uploadStatus
    );


    // =========================================================================
    // SLA
    // =========================================================================

    @Query("""
            SELECT COUNT(d)
            FROM Document d
            WHERE 1 = 0
            """)
    long countSLABreaches();


    // =========================================================================
    // OCR / EXTRACTED TEXT
    // =========================================================================

    @Query("""
            SELECT d.extractedText
            FROM Document d
            WHERE d.id = :documentId
            """)
    Optional<String> findExtractedTextById(
            @Param("documentId")
            Long documentId
    );


    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Transactional
    @Query("""
            UPDATE Document d
            SET d.extractedText = :extractedText
            WHERE d.id = :documentId
            """)
    int updateExtractedText(
            @Param("documentId")
            Long documentId,

            @Param("extractedText")
            String extractedText
    );


    // =========================================================================
    // AI SUMMARY
    // =========================================================================

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Transactional
    @Query("""
            UPDATE Document d
            SET d.summary = :summary
            WHERE d.id = :documentId
            """)
    int updateSummary(
            @Param("documentId")
            Long documentId,

            @Param("summary")
            String summary
    );


    // =========================================================================
    // RECENT DOCUMENTS
    // =========================================================================

    List<Document>
    findTop10ByOrderByUploadedAtDesc();


    List<Document>
    findTop10ByUserIdOrderByUploadedAtDesc(
            Long userId
    );


    // =========================================================================
    // SEARCH
    // =========================================================================

    List<Document>
    findByFileNameContainingIgnoreCaseOrderByUploadedAtDesc(
            String fileName
    );


    List<Document>
    findByUserIdAndFileNameContainingIgnoreCaseOrderByUploadedAtDesc(
            Long userId,
            String fileName
    );
}