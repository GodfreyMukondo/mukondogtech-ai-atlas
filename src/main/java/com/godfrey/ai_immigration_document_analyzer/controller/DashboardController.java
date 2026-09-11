package com.godfrey.ai_immigration_document_analyzer.controller;


import com.godfrey.ai_immigration_document_analyzer.dto.response.AdminDashboardResponse;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;
import com.godfrey.ai_immigration_document_analyzer.service.DashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * DASHBOARD CONTROLLER
 * ============================================================================
 *
 * REST API controller for the MukondoGTech AI Platform dashboard.
 *
 * Base URL:
 *
 *     /api/dashboard
 *
 * Endpoints:
 *
 *     GET /api/dashboard
 *     GET /api/dashboard/summary
 *     GET /api/dashboard/activity
 *     GET /api/dashboard/status
 *
 * Authentication:
 *
 *     All endpoints require an authenticated ADMIN user.
 *
 * Architecture:
 *
 *     React Dashboard
 *            |
 *            v
 *     DashboardController
 *            |
 *            v
 *     DashboardService
 *            |
 *            v
 *     Repositories
 *            |
 *            v
 *          Oracle
 *
 * Responsibilities:
 *
 *     - Validate dashboard access through Spring Security
 *     - Delegate dashboard operations to DashboardService
 *     - Return strongly typed HTTP responses
 *     - Provide structured request logging
 *     - Avoid database/business logic inside the controller
 *
 * The controller intentionally does NOT:
 *
 *     - Query repositories directly
 *     - Perform analytics calculations
 *     - Access Oracle directly
 *     - Construct dashboard business data
 *
 * ============================================================================
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * =========================================================================
     * GET COMPLETE DASHBOARD
     * =========================================================================
     *
     * Endpoint:
     *
     *     GET /api/dashboard
     *
     * Returns the complete administrator dashboard.
     *
     * This endpoint is useful when the frontend requires the complete
     * dashboard payload in one request.
     *
     * @param authentication authenticated Spring Security principal
     * @return complete administrator dashboard
     */
    @GetMapping
    public ResponseEntity<AdminDashboardResponse> getDashboard(
            Authentication authentication,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        String username = getAuthenticatedUsername(authentication);

        log.info(
                "Dashboard request received | endpoint=/api/dashboard | user={}",
                username
        );

        AdminDashboardResponse response =
                dashboardService.getDashboard(
                        authenticatedUser != null
                                ? authenticatedUser.getUserId()
                                : null
                );

        log.info(
                "Dashboard request completed | endpoint=/api/dashboard | user={}",
                username
        );

        return ResponseEntity.ok(response);
    }

    /**
     * =========================================================================
     * GET DASHBOARD SUMMARY
     * =========================================================================
     *
     * Endpoint:
     *
     *     GET /api/dashboard/summary
     *
     * The current DashboardService produces an AdminDashboardResponse.
     *
     * Until a dedicated DashboardSummaryResponse is introduced, the complete
     * dashboard response is returned here to preserve compatibility with the
     * existing React frontend.
     *
     * @param authentication authenticated Spring Security principal
     * @return dashboard summary payload
     */
    @GetMapping("/summary")
    public ResponseEntity<AdminDashboardResponse> getDashboardSummary(
            Authentication authentication,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        String username = getAuthenticatedUsername(authentication);

        log.info(
                "Dashboard summary request received | endpoint=/api/dashboard/summary | user={}",
                username
        );

        AdminDashboardResponse response =
                dashboardService.getDashboard(
                        authenticatedUser != null
                                ? authenticatedUser.getUserId()
                                : null
                );

        log.info(
                "Dashboard summary request completed | endpoint=/api/dashboard/summary | user={}",
                username
        );

        return ResponseEntity.ok(response);
    }

    /**
     * =========================================================================
     * GET DASHBOARD ACTIVITY
     * =========================================================================
     *
     * Endpoint:
     *
     *     GET /api/dashboard/activity
     *
     * The current AdminDashboardResponse already contains audit/activity
     * information.
     *
     * Until a dedicated activity DTO/service method is introduced, the
     * existing dashboard response is returned to preserve compatibility
     * with the current frontend.
     *
     * @param authentication authenticated Spring Security principal
     * @return dashboard activity payload
     */
    @GetMapping("/activity")
    public ResponseEntity<AdminDashboardResponse> getDashboardActivity(
            Authentication authentication,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        String username = getAuthenticatedUsername(authentication);

        log.info(
                "Dashboard activity request received | endpoint=/api/dashboard/activity | user={}",
                username
        );

        AdminDashboardResponse response =
                dashboardService.getDashboard(
                        authenticatedUser != null
                                ? authenticatedUser.getUserId()
                                : null
                );

        log.info(
                "Dashboard activity request completed | endpoint=/api/dashboard/activity | user={}",
                username
        );

        return ResponseEntity.ok(response);
    }

    /**
     * =========================================================================
     * GET DASHBOARD STATUS
     * =========================================================================
     *
     * Endpoint:
     *
     *     GET /api/dashboard/status
     *
     * The current AdminDashboardResponse contains platform and security
     * information.
     *
     * Until a dedicated DashboardStatusResponse is introduced, the complete
     * dashboard response is returned to preserve compatibility with the
     * existing frontend.
     *
     * @param authentication authenticated Spring Security principal
     * @return dashboard status payload
     */
    @GetMapping("/status")
    public ResponseEntity<AdminDashboardResponse> getDashboardStatus(
            Authentication authentication,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        String username = getAuthenticatedUsername(authentication);

        log.info(
                "Dashboard status request received | endpoint=/api/dashboard/status | user={}",
                username
        );

        AdminDashboardResponse response =
                dashboardService.getDashboard(
                        authenticatedUser != null
                                ? authenticatedUser.getUserId()
                                : null
                );

        log.info(
                "Dashboard status request completed | endpoint=/api/dashboard/status | user={}",
                username
        );

        return ResponseEntity.ok(response);
    }

    /**
     * =========================================================================
     * AUTHENTICATION HELPER
     * =========================================================================
     *
     * Safely obtains the authenticated username.
     *
     * Normally Spring Security guarantees that Authentication is populated
     * for these protected endpoints.
     *
     * The defensive checks prevent accidental NullPointerExceptions and make
     * the controller safer during testing and future integrations.
     *
     * @param authentication Spring Security authentication object
     * @return authenticated username or safe fallback value
     */
    private String getAuthenticatedUsername(
            Authentication authentication) {

        if (authentication == null) {

            log.warn(
                    "Dashboard request received without Authentication"
            );

            return "anonymous";
        }

        if (!authentication.isAuthenticated()) {

            log.warn(
                    "Dashboard request received from unauthenticated principal"
            );

            return "anonymous";
        }

        String username = authentication.getName();

        if (username == null || username.isBlank()) {

            log.warn(
                    "Dashboard request received with empty authenticated username"
            );

            return "unknown";
        }

        return username;
    }
}

