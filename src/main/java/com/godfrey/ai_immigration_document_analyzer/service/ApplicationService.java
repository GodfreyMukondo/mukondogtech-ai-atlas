package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.dto.request.ApplicationSubmitRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.response.AdminApplicationListResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.AdminApplicationResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.ApplicationDocumentSummary;
import com.godfrey.ai_immigration_document_analyzer.dto.response.ApplicationResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.ApplicationStatisticsResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.DocumentResponse;

import com.godfrey.ai_immigration_document_analyzer.entity.Application;
import com.godfrey.ai_immigration_document_analyzer.entity.Document;

import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;

import com.godfrey.ai_immigration_document_analyzer.repository.ApplicationRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * APPLICATION SERVICE
 * ============================================================================
 *
 * Handles immigration application submission and review.
 *
 * Responsibilities:
 *
 * - Validate and persist new applications submitted by applicants
 * - Link previously-uploaded documents (by ID) to the new application
 * - Derive a risk level from the linked documents' own fraud/risk analysis
 * - List an authenticated user's own applications
 * - List all applications and aggregate statistics for administrators
 * - Approve applications, notifying the applicant (in-app + email)
 * - Reject applications, notifying the applicant (in-app + email) with the
 *   reason
 *
 * SECURITY:
 *
 * Every user-facing method requires the authenticated user's ID and enforces
 * ownership. Admin methods are only reachable through admin-only controller
 * endpoints (enforced by SecurityConfig).
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private static final String HIGH_RISK = "HIGH";

    private static final String MEDIUM_RISK = "MEDIUM";

    private static final String LOW_RISK = "LOW";

    private static final String APPLICATION_APPROVED_NOTIFICATION_TYPE =
            "APPLICATION_APPROVED";

    private static final String APPLICATION_REJECTED_NOTIFICATION_TYPE =
            "APPLICATION_REJECTED";

    private static final String APPLICATION_SUBMITTED_NOTIFICATION_TYPE =
            "APPLICATION_SUBMITTED";

    private static final String APPLICATIONS_LINK =
            "/dashboard/applications";

    private static final String ADMIN_APPLICATIONS_LINK =
            "/admin/applications";

    private final ApplicationRepository applicationRepository;

    private final DocumentRepository documentRepository;

    private final UserRepository userRepository;

    private final EmailService emailService;

    private final NotificationService notificationService;

    // =========================================================================
    // SUBMIT APPLICATION
    // =========================================================================

    /**
     * Submits a new immigration application for the authenticated user.
     *
     * @param userId  authenticated backend user ID
     * @param request applicant details and previously-uploaded document IDs
     * @return the created application, including its attached documents
     */
    @Transactional
    public ApplicationResponse submitApplication(
            Long userId,
            ApplicationSubmitRequest request
    ) {

        if (!userRepository.existsById(userId)) {

            throw new ResourceNotFoundException(
                    "User not found with id: " + userId
            );
        }

        List<Document> documents =
                resolveOwnedUnlinkedDocuments(
                        userId,
                        request.getDocumentIds()
                );

        Application application =
                Application.builder()
                        .userId(userId)
                        .fullName(request.getFullName())
                        .email(request.getEmail())
                        .phone(request.getPhone())
                        .dateOfBirth(request.getDateOfBirth())
                        .country(request.getCountry())
                        .visaType(request.getVisaType())
                        .notes(request.getNotes())
                        .status("PENDING")
                        .riskLevel(
                                deriveRiskLevel(documents)
                        )
                        .build();

        Application saved =
                applicationRepository.save(application);

        for (Document document : documents) {

            document.setApplicationId(
                    saved.getId()
            );

            documentRepository.save(document);
        }

        log.info(
                "Application submitted | applicationId={} | userId={} | documentCount={}",
                saved.getId(),
                userId,
                documents.size()
        );

        notifyAdminsOfNewApplication(saved);

        return ApplicationResponse.from(
                saved,
                toDocumentSummaries(documents)
        );
    }

    /**
     * Notifies every administrator that a new application needs review.
     *
     * Best-effort: the application has already been committed, so a
     * notification failure must never fail the submission itself.
     */
    private void notifyAdminsOfNewApplication(
            Application application
    ) {

        try {

            notificationService.notifyAdmins(
                    APPLICATION_SUBMITTED_NOTIFICATION_TYPE,
                    "New Application Submitted",
                    application.getFullName()
                            + " submitted a "
                            + application.getVisaType()
                            + " application for "
                            + application.getCountry()
                            + ".",
                    ADMIN_APPLICATIONS_LINK
            );

        } catch (RuntimeException ex) {

            log.error(
                    "Application submitted but admin notification failed | applicationId={}",
                    application.getId(),
                    ex
            );
        }
    }

    // =========================================================================
    // USER APPLICATIONS
    // =========================================================================

    /**
     * Returns all applications submitted by the authenticated user, most
     * recent first.
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByUserId(
            Long userId
    ) {

        return applicationRepository
                .findByUserIdOrderBySubmittedAtDesc(userId)
                .stream()
                .map(application ->
                        ApplicationResponse.from(
                                application,
                                (int) documentRepository.countByApplicationId(
                                        application.getId()
                                )
                        )
                )
                .collect(Collectors.toList());
    }

    /**
     * Returns a single application belonging to the authenticated user,
     * including its attached documents.
     */
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationByIdForUser(
            Long id,
            Long userId
    ) {

        Application application =
                applicationRepository
                        .findByIdAndUserId(id, userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Application not found."
                                )
                        );

        List<Document> documents =
                documentRepository.findByApplicationId(
                        application.getId()
                );

        return ApplicationResponse.from(
                application,
                toDocumentSummaries(documents)
        );
    }

    // =========================================================================
    // ADMIN: LIST APPLICATIONS
    // =========================================================================

    /**
     * Returns every submitted application (most recent first) together with
     * aggregate review statistics, for administrator review.
     */
    @Transactional(readOnly = true)
    public AdminApplicationListResponse getApplicationsForAdmin() {

        List<AdminApplicationResponse> content =
                applicationRepository
                        .findAllByOrderBySubmittedAtDesc()
                        .stream()
                        .map(application ->
                                AdminApplicationResponse.from(
                                        application,
                                        (int) documentRepository.countByApplicationId(
                                                application.getId()
                                        )
                                )
                        )
                        .collect(Collectors.toList());

        ApplicationStatisticsResponse statistics =
                new ApplicationStatisticsResponse(
                        applicationRepository.countByStatus("PENDING"),
                        applicationRepository.countByStatus("APPROVED"),
                        applicationRepository.countByStatus("REJECTED"),
                        documentRepository
                                .countDistinctApplicationsWithFraudDetectedDocument()
                );

        return new AdminApplicationListResponse(
                content,
                statistics
        );
    }

    // =========================================================================
    // ADMIN: LIST A USER'S AVAILABLE DOCUMENTS
    // =========================================================================

    /**
     * Returns the given user's uploaded documents that are not yet attached
     * to any application, so an administrator creating a case on that
     * user's behalf can choose which ones to attach as supporting evidence.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAvailableDocumentsForAdmin(
            Long userId
    ) {

        if (!userRepository.existsById(userId)) {

            throw new ResourceNotFoundException(
                    "User not found with id: " + userId
            );
        }

        return documentRepository
                .findByUserIdAndApplicationIdIsNullOrderByUploadedAtDesc(userId)
                .stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // ADMIN: LIST APPLICATION DOCUMENTS
    // =========================================================================

    /**
     * Returns every document attached to the given application, for
     * administrator review.
     *
     * Unlike the applicant-facing document endpoints, this is intentionally
     * not scoped to a document owner: the caller is an administrator
     * reviewing a specific applicant's submitted evidence, which
     * {@code SecurityConfig} already restricts to the ADMIN role.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getApplicationDocumentsForAdmin(
            Long applicationId
    ) {

        if (!applicationRepository.existsById(applicationId)) {

            throw new ResourceNotFoundException(
                    "Application not found."
            );
        }

        return documentRepository
                .findByApplicationId(applicationId)
                .stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // ADMIN: APPROVE APPLICATION
    // =========================================================================

    /**
     * Approves a submitted application and notifies the applicant, both as
     * an in-app notification and by email.
     */
    @Transactional
    public void approveApplication(
            Long id
    ) {

        Application application =
                applicationRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Application not found."
                                )
                        );

        application.setStatus("APPROVED");

        /*
         * Clear any earlier rejection reason so a previously rejected
         * application that is later approved does not keep displaying a
         * stale rejection reason to the applicant.
         */
        application.setRejectionReason(null);

        applicationRepository.save(application);

        log.info(
                "Application approved | applicationId={}",
                id
        );

        notifyApplicantOfApproval(
                application
        );
    }

    // =========================================================================
    // ADMIN: REJECT APPLICATION
    // =========================================================================

    /**
     * Rejects a submitted application and notifies the applicant of the
     * reason, both as an in-app notification and by email.
     *
     * @param id     the application to reject
     * @param reason the reason shown to the applicant; already validated as
     *               non-blank by {@code RejectApplicationRequest}
     */
    @Transactional
    public void rejectApplication(
            Long id,
            String reason
    ) {

        Application application =
                applicationRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Application not found."
                                )
                        );

        String trimmedReason =
                reason.trim();

        application.setStatus("REJECTED");
        application.setRejectionReason(trimmedReason);

        applicationRepository.save(application);

        log.info(
                "Application rejected | applicationId={}",
                id
        );

        notifyApplicantOfRejection(
                application,
                trimmedReason
        );
    }

    // =========================================================================
    // REJECTION NOTIFICATIONS
    // =========================================================================

    /**
     * Notifies the applicant that their application was rejected, both as an
     * in-app notification and by email.
     *
     * Notification delivery is best-effort: the application has already been
     * rejected and committed, so a notification/email failure must never
     * roll back or fail the rejection itself.
     */
    private void notifyApplicantOfRejection(
            Application application,
            String reason
    ) {

        try {

            notificationService.notify(
                    application.getUserId(),
                    APPLICATION_REJECTED_NOTIFICATION_TYPE,
                    "Application Rejected",
                    "Your "
                            + application.getVisaType()
                            + " application for "
                            + application.getCountry()
                            + " was rejected: "
                            + reason,
                    APPLICATIONS_LINK
            );

        } catch (RuntimeException ex) {

            log.error(
                    "Application rejected but in-app notification failed | applicationId={}",
                    application.getId(),
                    ex
            );
        }

        try {

            emailService.send(
                    application.getEmail(),
                    "Your immigration application has been rejected",
                    "Dear "
                            + application.getFullName()
                            + ",\n\n"
                            + "We regret to inform you that your immigration application "
                            + "(visa type: "
                            + application.getVisaType()
                            + ", destination: "
                            + application.getCountry()
                            + ") has been rejected.\n\n"
                            + "Reason:\n"
                            + reason
                            + "\n\n"
                            + "If you believe this decision was made in error, or you are "
                            + "able to address the issue above, you are welcome to submit a "
                            + "new application with updated information and documents.\n\n"
                            + "Regards,\n"
                            + "The Immigration Team"
            );

        } catch (RuntimeException ex) {

            log.error(
                    "Application rejected but notification email failed to send | applicationId={}",
                    application.getId(),
                    ex
            );
        }
    }

    // =========================================================================
    // APPROVAL NOTIFICATIONS
    // =========================================================================

    /**
     * Notifies the applicant that their application was approved, both as an
     * in-app notification and by email.
     *
     * Notification delivery is best-effort: the application has already been
     * approved and committed, so a notification/email failure must never
     * roll back or fail the approval itself.
     */
    private void notifyApplicantOfApproval(
            Application application
    ) {

        try {

            notificationService.notify(
                    application.getUserId(),
                    APPLICATION_APPROVED_NOTIFICATION_TYPE,
                    "Application Approved",
                    "Your "
                            + application.getVisaType()
                            + " application for "
                            + application.getCountry()
                            + " has been approved.",
                    APPLICATIONS_LINK
            );

        } catch (RuntimeException ex) {

            log.error(
                    "Application approved but in-app notification failed | applicationId={}",
                    application.getId(),
                    ex
            );
        }

        try {

            emailService.send(
                    application.getEmail(),
                    "Your immigration application has been approved",
                    "Dear "
                            + application.getFullName()
                            + ",\n\n"
                            + "Congratulations. Your immigration application "
                            + "(visa type: "
                            + application.getVisaType()
                            + ", destination: "
                            + application.getCountry()
                            + ") has been approved.\n\n"
                            + "You can view the full details of your application at any "
                            + "time from your dashboard.\n\n"
                            + "Regards,\n"
                            + "The Immigration Team"
            );

        } catch (RuntimeException ex) {

            log.error(
                    "Application approved but notification email failed to send | applicationId={}",
                    application.getId(),
                    ex
            );
        }
    }

    // =========================================================================
    // DOCUMENT OWNERSHIP / LINKAGE VALIDATION
    // =========================================================================

    /**
     * Resolves the given document IDs, ensuring each one exists, belongs to
     * the authenticated user, and is not already attached to a different
     * application.
     */
    private List<Document> resolveOwnedUnlinkedDocuments(
            Long userId,
            List<Long> documentIds
    ) {

        return documentIds.stream()
                .map(documentId -> {

                    Document document =
                            documentRepository
                                    .findByIdAndUserId(documentId, userId)
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "One or more selected documents could not be found."
                                            )
                                    );

                    if (document.getApplicationId() != null) {

                        throw new IllegalArgumentException(
                                "One or more selected documents are already attached to another application."
                        );
                    }

                    return document;
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // RISK DERIVATION
    // =========================================================================

    /**
     * Derives the application's initial risk level from the highest risk
     * level already computed for its attached documents (via the existing
     * document fraud/risk analysis pipeline), rather than fabricating a new
     * score.
     */
    private String deriveRiskLevel(
            List<Document> documents
    ) {

        boolean hasHighRisk = documents.stream()
                .anyMatch(document ->
                        HIGH_RISK.equalsIgnoreCase(document.getRiskLevel())
                                || Boolean.TRUE.equals(document.getFraudDetected())
                );

        if (hasHighRisk) {
            return HIGH_RISK;
        }

        boolean hasMediumRisk = documents.stream()
                .anyMatch(document ->
                        MEDIUM_RISK.equalsIgnoreCase(document.getRiskLevel())
                );

        if (hasMediumRisk) {
            return MEDIUM_RISK;
        }

        return LOW_RISK;
    }

    // =========================================================================
    // DOCUMENT SUMMARY MAPPING
    // =========================================================================

    private List<ApplicationDocumentSummary> toDocumentSummaries(
            List<Document> documents
    ) {

        return documents.stream()
                .map(document ->
                        new ApplicationDocumentSummary(
                                document.getId(),
                                document.getFileName(),
                                document.getDocumentType(),
                                document.getUploadStatus()
                        )
                )
                .collect(Collectors.toList());
    }
}
