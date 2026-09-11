package com.godfrey.ai_immigration_document_analyzer.controller;

import com.godfrey.ai_immigration_document_analyzer.dto.request.AdminCreateApplicationRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.request.RejectApplicationRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.response.AdminApplicationListResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.ApplicationResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.DocumentResponse;
import com.godfrey.ai_immigration_document_analyzer.service.ApplicationService;
import com.godfrey.ai_immigration_document_analyzer.service.NotificationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ============================================================================
 * ADMIN APPLICATION CONTROLLER
 * ============================================================================
 *
 * REST API for administrator review of submitted immigration applications.
 *
 * Security model
 * ----------------------------------------------------------------------------
 *
 * Every endpoint under /api/admin/** requires the ADMIN role, enforced by
 * SecurityConfig.
 *
 * Endpoints
 * ----------------------------------------------------------------------------
 *
 * GET   /api/admin/applications
 * GET   /api/admin/applications/{applicationId}/documents
 * PATCH /api/admin/applications/{applicationId}/approve
 * PATCH /api/admin/applications/{applicationId}/reject
 * ============================================================================
 */
@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminApplicationController {

    private final ApplicationService applicationService;

    private final NotificationService notificationService;

    // =========================================================================
    // CREATE CASE
    // =========================================================================

    /**
     * Creates a new immigration application ("case") on behalf of an
     * existing user - the admin dashboard's "Create Case" action.
     *
     * The target user must already have uploaded the supporting document(s)
     * referenced (see {@link #getAvailableDocuments}); this endpoint does
     * not accept file uploads directly, mirroring the applicant-facing
     * submission flow.
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApplicationResponse> createCase(

            @Valid
            @RequestBody
            AdminCreateApplicationRequest request
    ) {

        log.info(
                "Admin case creation requested | targetUserId={}",
                request.getUserId()
        );

        final ApplicationResponse response =
                applicationService.submitApplication(
                        request.getUserId(),
                        request.toApplicationSubmitRequest()
                );

        log.info(
                "Admin case creation completed | targetUserId={} | applicationId={}",
                request.getUserId(),
                response.id()
        );

        notifyUserOfCaseCreation(
                request.getUserId(),
                response
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Best-effort: the case has already been created and committed, so a
     * notification failure must never fail the creation itself.
     */
    private void notifyUserOfCaseCreation(
            Long userId,
            ApplicationResponse application
    ) {

        try {

            notificationService.notify(
                    userId,
                    "APPLICATION_SUBMITTED",
                    "Application Created",
                    "An administrator created a "
                            + application.visaType()
                            + " application on your behalf for "
                            + application.country()
                            + ".",
                    "/dashboard/applications"
            );

        } catch (RuntimeException ex) {

            log.error(
                    "Case created but user notification failed | userId={} | applicationId={}",
                    userId,
                    application.id(),
                    ex
            );
        }
    }

    // =========================================================================
    // LIST A USER'S AVAILABLE DOCUMENTS
    // =========================================================================

    /**
     * Lists the given user's uploaded documents that are not yet attached
     * to any application, for the "Create Case" document picker.
     */
    @GetMapping(
            value = "/documents/available",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<DocumentResponse>> getAvailableDocuments(

            @RequestParam("userId")
            @Positive(message = "User ID must be a positive value")
            Long userId
    ) {

        final List<DocumentResponse> documents =
                applicationService.getAvailableDocumentsForAdmin(userId);

        return ResponseEntity.ok(documents);
    }

    // =========================================================================
    // LIST APPLICATIONS
    // =========================================================================

    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AdminApplicationListResponse> getApplications() {

        final AdminApplicationListResponse response =
                applicationService.getApplicationsForAdmin();

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // LIST APPLICATION DOCUMENTS
    // =========================================================================

    @GetMapping(
            value = "/{applicationId}/documents",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<DocumentResponse>> getApplicationDocuments(

            @PathVariable("applicationId")
            Long applicationId
    ) {

        validateApplicationId(applicationId);

        final List<DocumentResponse> documents =
                applicationService.getApplicationDocumentsForAdmin(applicationId);

        return ResponseEntity.ok(documents);
    }

    // =========================================================================
    // APPROVE APPLICATION
    // =========================================================================

    @PatchMapping(
            value = "/{applicationId}/approve"
    )
    public ResponseEntity<Void> approveApplication(

            @PathVariable("applicationId")
            Long applicationId
    ) {

        validateApplicationId(applicationId);

        log.info(
                "Application approval requested | applicationId={}",
                applicationId
        );

        applicationService.approveApplication(applicationId);

        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // REJECT APPLICATION
    // =========================================================================

    @PatchMapping(
            value = "/{applicationId}/reject",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> rejectApplication(

            @PathVariable("applicationId")
            Long applicationId,

            @Valid
            @RequestBody
            RejectApplicationRequest request
    ) {

        validateApplicationId(applicationId);

        log.info(
                "Application rejection requested | applicationId={}",
                applicationId
        );

        applicationService.rejectApplication(
                applicationId,
                request.getReason()
        );

        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // APPLICATION ID VALIDATION
    // =========================================================================

    private void validateApplicationId(
            Long applicationId
    ) {

        if (applicationId == null || applicationId <= 0) {

            throw new IllegalArgumentException(
                    "Application ID must be a positive value."
            );
        }
    }
}
