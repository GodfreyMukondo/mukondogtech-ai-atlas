package com.godfrey.ai_immigration_document_analyzer.controller;


import com.godfrey.ai_immigration_document_analyzer.dto.response.AdminDashboardResponse;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;
import com.godfrey.ai_immigration_document_analyzer.service.AiAuditService;
import com.godfrey.ai_immigration_document_analyzer.service.DashboardService;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import jakarta.validation.constraints.NotNull;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

import java.util.Map;




/**
 * ============================================================================
 * ADMIN CONTROLLER
 * ============================================================================
 *
 * Enterprise administration API.
 *
 * Responsibilities:
 *
 * - Platform analytics
 * - User statistics
 * - Immigration workflow monitoring
 * - AI system monitoring
 * - Security overview
 *
 * Security:
 *
 * Requires ROLE_ADMIN authority.
 *
 * Base URL:
 *
 * /api/admin
 *
 * ============================================================================
 */


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Admin Management",
        description = "Administrator dashboard and platform management APIs"
)
public class AdminController {



    private final DashboardService dashboardService;

    private final AiAuditService aiAuditService;





    /**
     * =========================================================================
     * ADMIN DASHBOARD
     * =========================================================================
     *
     * Returns real-time platform metrics.
     *
     * Includes:
     *
     * - User statistics
     * - Immigration applications
     * - Uploaded documents
     * - AI analytics
     * - Revenue information
     * - System health
     *
     * Endpoint:
     *
     * GET /api/admin/dashboard
     *
     * Authority:
     *
     * ROLE_ADMIN
     *
     * =========================================================================
     */


    @Operation(
            summary = "Get admin dashboard",
            description =
                    "Returns enterprise dashboard statistics for administrators"
    )
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardResponse> getDashboard(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
    ){


        String adminUsername =
                authenticatedUser != null
                        ?
                        authenticatedUser.getUsername()
                        :
                        "UNKNOWN";



        log.info(
                "Admin dashboard accessed by {}",
                adminUsername
        );



        AdminDashboardResponse dashboard =
                dashboardService.getDashboard(
                        authenticatedUser != null
                                ? authenticatedUser.getUserId()
                                : null
                );



        return ResponseEntity.ok(
                dashboard
        );


    }




    /**
     * =========================================================================
     * RUN AI AUDIT
     * =========================================================================
     *
     * Re-scans live document and application data for anything needing
     * administrator attention (high-risk/fraud-flagged documents, stale
     * pending applications) and records the result as a notification for
     * the requesting administrator.
     *
     * Endpoint:
     *
     * POST /api/admin/audit/run
     *
     * Authority:
     *
     * ROLE_ADMIN
     *
     * =========================================================================
     */
    @Operation(
            summary = "Run AI audit",
            description =
                    "Re-scans documents and applications for issues needing administrator attention"
    )
    @PostMapping("/audit/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> runAiAudit(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
    ) {

        log.info(
                "AI audit triggered by {}",
                authenticatedUser != null
                        ? authenticatedUser.getUsername()
                        : "UNKNOWN"
        );

        AiAuditService.AuditResult result =
                aiAuditService.runAudit(
                        authenticatedUser != null
                                ? authenticatedUser.getUserId()
                                : null
                );

        return ResponseEntity.ok(
                Map.of(
                        "message", result.message(),
                        "highRiskDocuments", result.highRiskDocuments(),
                        "fraudFlaggedDocuments", result.fraudFlaggedDocuments(),
                        "staleApplications", result.staleApplications()
                )
        );
    }







}