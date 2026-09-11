package com.godfrey.ai_immigration_document_analyzer.service;


import com.godfrey.ai_immigration_document_analyzer.dto.response.AdminDashboardResponse;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.stereotype.Service;


import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * ============================================================================
 * ADMIN REPORT SERVICE
 * ============================================================================
 *
 * Backs the admin dashboard's "Generate Report" action.
 *
 * The report is a live CSV snapshot built from the same real repository
 * data as the admin dashboard (via {@link DashboardService}) - it is not a
 * canned file.
 *
 * DOWNLOAD LINK DESIGN
 * ----------------------------------------------------------------------------
 * The frontend opens the returned download link with `window.open(...)`, a
 * plain browser navigation that cannot carry the app's JWT bearer token.
 * The download endpoint is therefore intentionally NOT under the JWT/role
 * protected /api/admin/** URL space; instead this service issues a
 * short-lived, single-use, random opaque token when the report is
 * requested, and the public download endpoint (see
 * {@link com.godfrey.ai_immigration_document_analyzer.controller.ReportDownloadController})
 * accepts a request only if it presents that exact token, consuming it
 * immediately. This is the standard "signed download link" pattern and
 * keeps the report itself gated behind ADMIN authentication (only the
 * protected /api/admin/reports/generate endpoint can mint a token).
 * ============================================================================
 */


@Service
@RequiredArgsConstructor
@Slf4j
public class AdminReportService {


    private final DashboardService dashboardService;


    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);


    /**
     * In-memory single-use download tokens.
     *
     * A dedicated persisted table would be needed for a multi-instance
     * deployment; for this application's current single-instance
     * deployment model, an in-memory store is sufficient and avoids
     * persisting throwaway, minutes-lived tokens to the database.
     */
    private final Map<String, PendingReport> pendingReports =
            new ConcurrentHashMap<>();




    /**
     * =========================================================================
     * ISSUE DOWNLOAD TOKEN
     * =========================================================================
     *
     * @param adminUserId the requesting administrator, used to build the
     *                    report content
     * @return an opaque, single-use token valid for {@link #TOKEN_TTL}
     */
    public String issueDownloadToken(
            Long adminUserId
    ) {

        purgeExpiredTokens();

        String token =
                UUID.randomUUID().toString();

        pendingReports.put(
                token,
                new PendingReport(
                        adminUserId,
                        LocalDateTime.now().plus(TOKEN_TTL)
                )
        );

        log.info(
                "Report download token issued | adminUserId={}",
                adminUserId
        );

        return token;
    }




    /**
     * =========================================================================
     * CONSUME DOWNLOAD TOKEN
     * =========================================================================
     *
     * Validates and immediately invalidates the given token (single use),
     * then builds the report CSV fresh from live data.
     *
     * @return the report CSV bytes, or {@code null} if the token is
     *         missing, already used, or expired
     */
    public byte[] consumeTokenAndBuildReport(
            String token
    ) {

        if (token == null || token.isBlank()) {

            return null;
        }

        PendingReport pending =
                pendingReports.remove(token);

        if (pending == null) {

            return null;
        }

        if (pending.expiresAt().isBefore(LocalDateTime.now())) {

            log.warn(
                    "Report download token expired | adminUserId={}",
                    pending.adminUserId()
            );

            return null;
        }

        AdminDashboardResponse dashboard =
                dashboardService.getDashboard(
                        pending.adminUserId()
                );

        String csv =
                buildCsv(dashboard);

        return csv.getBytes(StandardCharsets.UTF_8);
    }




    /*
    |--------------------------------------------------------------------------
    | TOKEN HOUSEKEEPING
    |--------------------------------------------------------------------------
    */


    private void purgeExpiredTokens() {

        LocalDateTime now =
                LocalDateTime.now();

        pendingReports.values().removeIf(
                pending -> pending.expiresAt().isBefore(now)
        );
    }




    /*
    |--------------------------------------------------------------------------
    | CSV GENERATION
    |--------------------------------------------------------------------------
    */


    private String buildCsv(
            AdminDashboardResponse dashboard
    ) {

        StringBuilder csv =
                new StringBuilder();

        csv.append("Enterprise Platform Report\n");
        csv.append("Generated At,")
                .append(LocalDateTime.now())
                .append("\n\n");

        AdminDashboardResponse.Executive executive =
                dashboard.getExecutive();

        if (executive != null) {

            csv.append("Executive Summary\n");
            csv.append("Metric,Value\n");
            csv.append("Total Users,")
                    .append(executive.getActiveUsers())
                    .append("\n");
            csv.append("Active Cases,")
                    .append(executive.getActiveCases())
                    .append("\n");
            csv.append("AI Accuracy,")
                    .append(executive.getAiAccuracy())
                    .append("%\n");
            csv.append("\n");
        }

        AdminDashboardResponse.Operations operations =
                dashboard.getOperations();

        if (operations != null) {

            csv.append("Immigration Operations\n");
            csv.append("Metric,Value\n");
            csv.append("Pending Applications,")
                    .append(operations.getPendingApplications())
                    .append("\n");
            csv.append("Approved Cases,")
                    .append(operations.getApprovedCases())
                    .append("\n");
            csv.append("Rejected Applications,")
                    .append(operations.getRejectedApplications())
                    .append("\n");
            csv.append("Awaiting Documents,")
                    .append(operations.getAwaitingDocuments())
                    .append("\n");
            csv.append("SLA Breaches,")
                    .append(operations.getSlaBreaches())
                    .append("\n");
            csv.append("\n");
        }

        AdminDashboardResponse.Security security =
                dashboard.getSecurity();

        if (security != null) {

            csv.append("Security Overview\n");
            csv.append("Metric,Value\n");
            csv.append("Status,")
                    .append(security.getStatus())
                    .append("\n");
            csv.append("Alerts,")
                    .append(security.getAlerts())
                    .append("\n");
            csv.append("Blocked Attempts,")
                    .append(security.getBlockedAttacks())
                    .append("\n");
            csv.append("\n");
        }

        List<AdminDashboardResponse.RegionMetric> regions =
                dashboard.getRegions();

        if (regions != null && !regions.isEmpty()) {

            csv.append("Regional Breakdown\n");
            csv.append("Country,Applications\n");

            for (AdminDashboardResponse.RegionMetric region : regions) {

                csv.append(csvField(region.getRegion()))
                        .append(",")
                        .append(region.getValue())
                        .append("\n");
            }
        }

        return csv.toString();
    }




    /**
     * Escapes a single CSV field (RFC 4180).
     */
    private String csvField(
            String value
    ) {

        if (value == null) {

            return "";
        }

        String escaped =
                value.replace("\"", "\"\"");

        if (
                escaped.contains(",")
                        || escaped.contains("\"")
                        || escaped.contains("\n")
        ) {

            return "\"" + escaped + "\"";
        }

        return escaped;
    }




    private record PendingReport(

            Long adminUserId,

            LocalDateTime expiresAt

    ) {}
}
