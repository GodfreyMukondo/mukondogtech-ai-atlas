package com.godfrey.ai_immigration_document_analyzer.service;


import com.godfrey.ai_immigration_document_analyzer.repository.ApplicationRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * ============================================================================
 * AI AUDIT SERVICE
 * ============================================================================
 *
 * Backs the admin dashboard's "Run AI Audit" action.
 *
 * This application has no standalone monitoring model to "audit" against -
 * what it does have is the fraud/risk analysis already computed for every
 * uploaded document (see DocumentService/OcrService) and the immigration
 * applications built from them. Running an audit re-scans that live data
 * for anything needing administrator attention:
 *
 * - Documents already flagged high-risk or fraudulent
 * - Applications stuck PENDING well past a reasonable review window
 *
 * rather than fabricating an "audit" result with no real inspection behind
 * it.
 * ============================================================================
 */


@Service
@RequiredArgsConstructor
@Slf4j
public class AiAuditService {


    private final DocumentRepository documentRepository;

    private final ApplicationRepository applicationRepository;

    private final NotificationService notificationService;


    private static final int STALE_APPLICATION_THRESHOLD_HOURS = 48;




    /**
     * =========================================================================
     * RUN AUDIT
     * =========================================================================
     *
     * @param adminUserId the requesting administrator, notified with the
     *                    audit result (best-effort)
     */
    @Transactional(readOnly = true)
    public AuditResult runAudit(
            Long adminUserId
    ) {

        log.info(
                "Running AI audit"
        );

        long highRiskDocuments =
                documentRepository.countHighRiskDocuments();

        long fraudFlaggedDocuments =
                documentRepository.countByFraudDetectedTrue();

        long staleApplications =
                applicationRepository.countByStatusAndSubmittedAtBefore(
                        "PENDING",
                        LocalDateTime.now().minusHours(
                                STALE_APPLICATION_THRESHOLD_HOURS
                        )
                );

        String message =
                buildSummaryMessage(
                        highRiskDocuments,
                        fraudFlaggedDocuments,
                        staleApplications
                );

        log.info(
                "AI audit completed | highRiskDocuments={} | fraudFlaggedDocuments={} | staleApplications={}",
                highRiskDocuments,
                fraudFlaggedDocuments,
                staleApplications
        );

        notifyAdmin(
                adminUserId,
                message
        );

        return new AuditResult(
                message,
                highRiskDocuments,
                fraudFlaggedDocuments,
                staleApplications
        );
    }




    /*
    |--------------------------------------------------------------------------
    | SUMMARY MESSAGE
    |--------------------------------------------------------------------------
    */


    private String buildSummaryMessage(
            long highRiskDocuments,
            long fraudFlaggedDocuments,
            long staleApplications
    ) {

        List<String> findings = new ArrayList<>();

        if (highRiskDocuments > 0) {

            findings.add(
                    highRiskDocuments
                            + " high-risk document(s)"
            );
        }

        if (fraudFlaggedDocuments > 0) {

            findings.add(
                    fraudFlaggedDocuments
                            + " fraud-flagged document(s)"
            );
        }

        if (staleApplications > 0) {

            findings.add(
                    staleApplications
                            + " application(s) pending over "
                            + STALE_APPLICATION_THRESHOLD_HOURS
                            + "h"
            );
        }

        if (findings.isEmpty()) {

            return "AI audit completed. No issues detected across documents or applications.";
        }

        return "AI audit completed. Found "
                + String.join(
                        ", ",
                        findings
                )
                + " requiring review.";
    }




    /*
    |--------------------------------------------------------------------------
    | ADMIN NOTIFICATION
    |--------------------------------------------------------------------------
    */


    /**
     * Best-effort: the audit has already completed, so a notification
     * failure must never fail the audit itself.
     */
    private void notifyAdmin(
            Long adminUserId,
            String message
    ) {

        if (adminUserId == null || adminUserId <= 0) {

            return;
        }

        try {

            notificationService.notify(
                    adminUserId,
                    "AI_AUDIT_COMPLETED",
                    "AI Audit Completed",
                    message,
                    "/admin"
            );

        } catch (RuntimeException ex) {

            log.error(
                    "AI audit completed but admin notification failed | adminUserId={}",
                    adminUserId,
                    ex
            );
        }
    }




    /*
    |--------------------------------------------------------------------------
    | RESULT
    |--------------------------------------------------------------------------
    */


    public record AuditResult(

            String message,

            long highRiskDocuments,

            long fraudFlaggedDocuments,

            long staleApplications

    ) {}
}
