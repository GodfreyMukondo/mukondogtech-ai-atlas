package com.godfrey.ai_immigration_document_analyzer.controller;


import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;
import com.godfrey.ai_immigration_document_analyzer.service.AdminReportService;


import jakarta.servlet.http.HttpServletRequest;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;


/**
 * ============================================================================
 * ADMIN REPORT CONTROLLER
 * ============================================================================
 *
 * Issues a live enterprise report for the admin dashboard's "Generate
 * Report" action.
 *
 * Endpoint:
 *
 * POST /api/admin/reports/generate
 *
 * Authority:
 *
 * ROLE_ADMIN (enforced by SecurityConfig for /api/admin/**)
 *
 * The actual file download happens against the separate, public
 * {@link ReportDownloadController} - see {@link AdminReportService} for why.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Slf4j
public class AdminReportController {

    private final AdminReportService adminReportService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> generateReport(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            HttpServletRequest request
    ) {

        Long adminUserId =
                authenticatedUser != null
                        ? authenticatedUser.getUserId()
                        : null;

        log.info(
                "Report generation requested by userId={}",
                adminUserId
        );

        String token =
                adminReportService.issueDownloadToken(adminUserId);

        String downloadUrl =
                UriComponentsBuilder.newInstance()
                        .scheme(request.getScheme())
                        .host(request.getServerName())
                        .port(request.getServerPort())
                        .path("/api/reports/download")
                        .queryParam("token", token)
                        .build()
                        .toUriString();

        return ResponseEntity.ok(
                Map.of(
                        "message", "Report generated successfully.",
                        "downloadUrl", downloadUrl
                )
        );
    }
}
