package com.godfrey.ai_immigration_document_analyzer.controller;

import com.godfrey.ai_immigration_document_analyzer.dto.request.ApplicationSubmitRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.response.ApplicationResponse;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;
import com.godfrey.ai_immigration_document_analyzer.service.ApplicationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ============================================================================
 * APPLICATION CONTROLLER
 * ============================================================================
 *
 * REST API for authenticated applicants submitting and viewing their own
 * immigration applications.
 *
 * Security model
 * ----------------------------------------------------------------------------
 *
 * - The authenticated database user is obtained exclusively from
 *   {@link AuthenticatedUser}.
 * - The frontend must NEVER provide userId.
 * - Ownership is enforced by the service/repository layer.
 *
 * Endpoints
 * ----------------------------------------------------------------------------
 *
 * POST /api/applications
 * GET  /api/applications
 * GET  /api/applications/{applicationId}
 *
 * Submission contract
 * ----------------------------------------------------------------------------
 *
 * Supporting documents must already have been uploaded via
 * POST /api/documents/upload (which returns each document's ID). This
 * endpoint then references those documents by ID rather than accepting
 * file uploads directly.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ApplicationController {

    private final ApplicationService applicationService;

    // =========================================================================
    // SUBMIT APPLICATION
    // =========================================================================

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApplicationResponse> submitApplication(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @Valid
            @RequestBody
            ApplicationSubmitRequest request
    ) {

        final Long userId =
                requireAuthenticatedUserId(authenticatedUser);

        log.info(
                "Application submission received | userId={}",
                userId
        );

        final ApplicationResponse response =
                applicationService.submitApplication(
                        userId,
                        request
                );

        log.info(
                "Application submission completed | userId={} | applicationId={}",
                userId,
                response.id()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================================
    // GET MY APPLICATIONS
    // =========================================================================

    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
    ) {

        final Long userId =
                requireAuthenticatedUserId(authenticatedUser);

        final List<ApplicationResponse> applications =
                applicationService.getApplicationsByUserId(userId);

        return ResponseEntity.ok(applications);
    }

    // =========================================================================
    // GET SINGLE APPLICATION
    // =========================================================================

    @GetMapping(
            value = "/{applicationId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApplicationResponse> getApplication(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable("applicationId")
            Long applicationId
    ) {

        final Long userId =
                requireAuthenticatedUserId(authenticatedUser);

        validateApplicationId(applicationId);

        final ApplicationResponse response =
                applicationService.getApplicationByIdForUser(
                        applicationId,
                        userId
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // AUTHENTICATED USER VALIDATION
    // =========================================================================

    private Long requireAuthenticatedUserId(
            AuthenticatedUser authenticatedUser
    ) {

        if (authenticatedUser == null) {

            log.warn(
                    "Application endpoint accessed without authenticated principal."
            );

            throw new IllegalStateException(
                    "Authenticated user could not be determined."
            );
        }

        final Long userId =
                authenticatedUser.getUserId();

        if (userId == null || userId <= 0) {

            log.error(
                    "Authenticated principal contains invalid database user ID."
            );

            throw new IllegalStateException(
                    "Authenticated user could not be determined."
            );
        }

        return userId;
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
