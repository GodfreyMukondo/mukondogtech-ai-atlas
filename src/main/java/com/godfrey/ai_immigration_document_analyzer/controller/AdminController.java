package com.godfrey.ai_immigration_document_analyzer.controller;


import com.godfrey.ai_immigration_document_analyzer.dto.response.AdminDashboardResponse;
import com.godfrey.ai_immigration_document_analyzer.service.DashboardService;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


import jakarta.validation.constraints.NotNull;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;




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
            Authentication authentication
    ){


        String adminUsername =
                authentication != null
                        ?
                        authentication.getName()
                        :
                        "UNKNOWN";



        log.info(
                "Admin dashboard accessed by {}",
                adminUsername
        );



        AdminDashboardResponse dashboard =
                dashboardService.getDashboard();



        return ResponseEntity.ok(
                dashboard
        );


    }







}