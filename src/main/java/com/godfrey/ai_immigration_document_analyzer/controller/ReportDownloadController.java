package com.godfrey.ai_immigration_document_analyzer.controller;


import com.godfrey.ai_immigration_document_analyzer.service.AdminReportService;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;


/**
 * ============================================================================
 * REPORT DOWNLOAD CONTROLLER
 * ============================================================================
 *
 * Serves an admin report CSV to a single-use, short-lived download token
 * minted by {@code POST /api/admin/reports/generate}.
 *
 * SECURITY MODEL
 * ----------------------------------------------------------------------------
 * This endpoint is intentionally NOT under /api/admin/** (see
 * SecurityConfig): the frontend reaches it via a plain browser navigation
 * (window.open), which cannot carry the app's JWT bearer token, so the
 * usual role-based filter would reject it before this method ever ran.
 *
 * Authorization instead comes entirely from possessing a valid token, which
 * can only ever have been minted by the ADMIN-protected generate endpoint
 * above. The token is single-use (consumed on first read) and expires after
 * a few minutes, so a leaked/logged URL has a narrow, one-shot window of
 * exposure - the same trade-off any "signed download link" design makes.
 *
 * Endpoint:
 *
 * GET /api/reports/download?token=...
 * ============================================================================
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportDownloadController {

    private final AdminReportService adminReportService;

    @GetMapping(
            value = "/download",
            produces = "text/csv"
    )
    public ResponseEntity<byte[]> download(

            @RequestParam("token")
            String token
    ) {

        byte[] csv =
                adminReportService.consumeTokenAndBuildReport(token);

        if (csv == null) {

            log.warn(
                    "Rejected report download with invalid or expired token"
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        String filename =
                "platform-report-" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .contentType(
                        MediaType.parseMediaType("text/csv")
                )
                .body(csv);
    }
}
