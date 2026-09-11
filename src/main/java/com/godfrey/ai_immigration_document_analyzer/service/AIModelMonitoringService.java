package com.godfrey.ai_immigration_document_analyzer.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ============================================================
 * AI MODEL MONITORING SERVICE
 * ============================================================
 *
 * Central service responsible for collecting and exposing
 * AI model monitoring metrics.
 *
 * Current implementation:
 *
 * - Thread-safe in-memory metrics
 * - Live model metrics
 * - Aggregated summary metrics
 * - Request counters
 * - Success/failure counters
 * - Latency tracking
 * - Token usage tracking
 * - Error tracking
 * - Model status tracking
 * - Last activity tracking
 *
 * Designed so that a persistent monitoring data source can
 * later replace or supplement the in-memory implementation.
 *
 * IMPORTANT:
 *
 * This service does NOT currently connect to a database,
 * Prometheus, OpenTelemetry, or an external AI provider.
 *
 * Therefore metrics are application-runtime metrics and will
 * reset whenever the application restarts.
 *
 * ============================================================
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIModelMonitoringService {

    /**
     * Used to report real JDBC connection pool health on the admin
     * dashboard (see {@link #buildInfrastructureMetrics()}).
     */
    private final DataSource dataSource;

    /**
     * Backs the "Run AI Audit" administration action (see
     * {@link #executeAction(String, Long)}) - reuses the same real
     * document/application scan as the admin dashboard's own audit
     * action, rather than a second, different implementation.
     */
    private final AiAuditService aiAuditService;

    /**
     * ========================================================
     * MODEL METRICS STORAGE
     * ========================================================
     * <p>
     * ConcurrentHashMap allows multiple AI requests to update
     * monitoring information safely.
     */
    private final Map<String, ModelMetrics> modelMetrics =
            new ConcurrentHashMap<>();

    /**
     * ========================================================
     * GLOBAL METRICS
     * ========================================================
     */

    private final AtomicLong totalRequests =
            new AtomicLong(0);

    private final AtomicLong successfulRequests =
            new AtomicLong(0);

    private final AtomicLong failedRequests =
            new AtomicLong(0);

    private final AtomicLong totalTokens =
            new AtomicLong(0);

    private final AtomicLong totalLatencyMs =
            new AtomicLong(0);

    private final AtomicLong totalErrors =
            new AtomicLong(0);

    /**
     * Last monitoring activity.
     */
    private final AtomicReference<Instant> lastActivity =
            new AtomicReference<>(Instant.now());

    /**
     * Application start time.
     */
    private final Instant applicationStartedAt =
            Instant.now();


    /**
     * ========================================================
     * RECORD AI REQUEST
     * ========================================================
     * <p>
     * Call this method whenever an AI model request starts or
     * completes.
     * <p>
     * Example:
     * <p>
     * monitoringService.recordRequest(
     * "granite-3-8b-instruct",
     * 450,
     * 1200,
     * true
     * );
     *
     * @param modelName  model identifier
     * @param latencyMs  request latency in milliseconds
     * @param tokens     tokens consumed
     * @param successful whether request succeeded
     */
    public void recordRequest(
            String modelName,
            long latencyMs,
            long tokens,
            boolean successful
    ) {

        String normalizedModel =
                normalizeModelName(modelName);

        long safeLatency =
                Math.max(latencyMs, 0);

        long safeTokens =
                Math.max(tokens, 0);

        totalRequests.incrementAndGet();

        totalLatencyMs.addAndGet(safeLatency);

        totalTokens.addAndGet(safeTokens);

        if (successful) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
            totalErrors.incrementAndGet();
        }

        lastActivity.set(Instant.now());

        ModelMetrics metrics =
                modelMetrics.computeIfAbsent(
                        normalizedModel,
                        key -> new ModelMetrics(key)
                );

        metrics.recordRequest(
                safeLatency,
                safeTokens,
                successful
        );

        log.debug(
                "AI request recorded. model={} latencyMs={} tokens={} successful={}",
                normalizedModel,
                safeLatency,
                safeTokens,
                successful
        );
    }


    /**
     * ========================================================
     * RECORD ERROR
     * ========================================================
     * <p>
     * Records an AI model error without necessarily recording
     * a request.
     *
     * @param modelName model identifier
     */
    public void recordError(
            String modelName
    ) {

        String normalizedModel =
                normalizeModelName(modelName);

        totalErrors.incrementAndGet();

        lastActivity.set(Instant.now());

        ModelMetrics metrics =
                modelMetrics.computeIfAbsent(
                        normalizedModel,
                        ModelMetrics::new
                );

        metrics.recordError();

        log.warn(
                "AI model error recorded. model={}",
                normalizedModel
        );
    }


    /**
     * ========================================================
     * REGISTER MODEL
     * ========================================================
     * <p>
     * Registers a model with the monitoring system.
     * <p>
     * This is useful even before the model has processed a
     * request.
     *
     * @param modelName model identifier
     */
    public void registerModel(
            String modelName
    ) {

        String normalizedModel =
                normalizeModelName(modelName);

        modelMetrics.computeIfAbsent(
                normalizedModel,
                ModelMetrics::new
        );

        lastActivity.set(Instant.now());

        log.info(
                "AI model registered for monitoring: {}",
                normalizedModel
        );
    }


    /**
     * ========================================================
     * SET MODEL STATUS
     * ========================================================
     * <p>
     * Supported statuses are intentionally represented as
     * strings so future providers can use statuses such as:
     * <p>
     * ONLINE
     * OFFLINE
     * DEGRADED
     * ERROR
     * UNKNOWN
     *
     * @param modelName model identifier
     * @param status    model status
     */
    public void setModelStatus(
            String modelName,
            String status
    ) {

        String normalizedModel =
                normalizeModelName(modelName);

        String normalizedStatus =
                normalizeStatus(status);

        ModelMetrics metrics =
                modelMetrics.computeIfAbsent(
                        normalizedModel,
                        ModelMetrics::new
                );

        metrics.setStatus(normalizedStatus);

        lastActivity.set(Instant.now());

        log.debug(
                "AI model status updated. model={} status={}",
                normalizedModel,
                normalizedStatus
        );
    }


    /**
     * ========================================================
     * GET LIVE METRICS
     * ========================================================
     * <p>
     * Returns real-time application-level monitoring metrics.
     *
     * @return live monitoring response
     */
    public AIModelMonitoringResponse getLiveMetrics() {

        Instant now =
                Instant.now();

        long requests =
                totalRequests.get();

        long successful =
                successfulRequests.get();

        long failed =
                failedRequests.get();

        long latency =
                totalLatencyMs.get();

        double averageLatency =
                requests == 0
                        ? 0.0
                        : (double) latency / requests;

        double successRate =
                requests == 0
                        ? 0.0
                        : ((double) successful / requests) * 100.0;

        double errorRate =
                requests == 0
                        ? 0.0
                        : ((double) failed / requests) * 100.0;

        return new AIModelMonitoringResponse(
                now,
                applicationStartedAt,
                lastActivity.get(),
                requests,
                successful,
                failed,
                totalErrors.get(),
                totalTokens.get(),
                round(averageLatency),
                round(successRate),
                round(errorRate),
                getModelMetrics()
        );
    }


    /**
     * ========================================================
     * GET SUMMARY
     * ========================================================
     * <p>
     * Returns a high-level summary suitable for dashboard KPI
     * cards.
     *
     * @return monitoring summary
     */
    public AIModelMonitoringSummary getSummary() {

        long requests =
                totalRequests.get();

        long successful =
                successfulRequests.get();

        long failed =
                failedRequests.get();

        double successRate =
                requests == 0
                        ? 0.0
                        : ((double) successful / requests) * 100.0;

        double failureRate =
                requests == 0
                        ? 0.0
                        : ((double) failed / requests) * 100.0;

        double averageLatency =
                requests == 0
                        ? 0.0
                        : (double) totalLatencyMs.get()
                        / requests;

        return new AIModelMonitoringSummary(
                requests,
                successful,
                failed,
                totalErrors.get(),
                totalTokens.get(),
                round(averageLatency),
                round(successRate),
                round(failureRate),
                modelMetrics.size(),
                countHealthyModels(),
                countUnhealthyModels(),
                lastActivity.get()
        );
    }


    /**
     * ========================================================
     * GET DASHBOARD VIEW
     * ========================================================
     * <p>
     * Backs the admin AI Model Monitoring page. Everything here is
     * derived from real, currently-tracked data:
     * <p>
     * - metrics/secondaryMetrics/models come from actual recorded AI
     *   requests (see {@link #recordRequest}, called by
     *   {@link LlmService#ask}).
     * - infrastructure comes from the live JVM heap and the real JDBC
     *   connection pool.
     * - alerts are derived from the current model/error state, not
     *   canned text - an empty list means nothing is actually wrong.
     * - administrationActions only lists actions this service can
     *   really perform (see {@link #executeAction}).
     */
    public AIModelMonitoringDashboardResponse getDashboardView() {

        AIModelMonitoringSummary summary =
                getSummary();

        List<AIModelMetricsResponse> models =
                getModelMetrics();

        return new AIModelMonitoringDashboardResponse(
                buildPrimaryMetrics(summary),
                buildSecondaryMetrics(summary),
                buildDashboardModels(models),
                buildInfrastructureMetrics(),
                buildAlerts(summary, models),
                buildAdministrationActions(),
                Instant.now()
        );
    }


    private List<DashboardMetricCard> buildPrimaryMetrics(
            AIModelMonitoringSummary summary
    ) {

        return List.of(

                new DashboardMetricCard(
                        "total-requests",
                        "activity",
                        "Total AI Requests",
                        String.valueOf(summary.totalRequests()),
                        "Since application start",
                        "bg-blue-500/10 text-blue-300",
                        null
                ),

                new DashboardMetricCard(
                        "success-rate",
                        "check",
                        "Success Rate",
                        round(summary.successRate()) + "%",
                        summary.successfulRequests() + " successful requests",
                        "bg-emerald-500/10 text-emerald-300",
                        null
                ),

                new DashboardMetricCard(
                        "avg-latency",
                        "clock",
                        "Average Latency",
                        formatMillis(summary.averageLatencyMs()),
                        "Across all AI requests",
                        "bg-amber-500/10 text-amber-300",
                        null
                ),

                new DashboardMetricCard(
                        "total-tokens",
                        "zap",
                        "Tokens Consumed",
                        String.valueOf(summary.totalTokens()),
                        "Reported by the AI provider",
                        "bg-violet-500/10 text-violet-300",
                        null
                )
        );
    }


    private List<DashboardMetricCard> buildSecondaryMetrics(
            AIModelMonitoringSummary summary
    ) {

        return List.of(

                new DashboardMetricCard(
                        "healthy-models",
                        "shield",
                        "Healthy Models",
                        String.valueOf(summary.healthyModels()),
                        summary.totalModels() + " model(s) tracked",
                        "bg-emerald-500/10 text-emerald-300",
                        null
                ),

                new DashboardMetricCard(
                        "unhealthy-models",
                        "alertTriangle",
                        "Degraded Models",
                        String.valueOf(summary.unhealthyModels()),
                        "Currently failing requests",
                        "bg-red-500/10 text-red-300",
                        null
                ),

                new DashboardMetricCard(
                        "total-errors",
                        "server",
                        "Total Errors",
                        String.valueOf(summary.totalErrors()),
                        "Since application start",
                        "bg-orange-500/10 text-orange-300",
                        null
                )
        );
    }


    private List<DashboardAiModel> buildDashboardModels(
            List<AIModelMetricsResponse> models
    ) {

        return models.stream()
                .map(model -> new DashboardAiModel(
                        model.modelName(),
                        model.modelName(),
                        mapModelStatus(model.status()),
                        model.successRate(),
                        model.averageLatencyMs(),
                        model.totalRequests(),
                        model.modelName()
                ))
                .toList();
    }


    /**
     * Maps this service's internal status vocabulary (ONLINE/DEGRADED/
     * ERROR/UNKNOWN) to the frontend's display vocabulary.
     */
    private String mapModelStatus(
            String internalStatus
    ) {

        if (internalStatus == null) {
            return "Unknown";
        }

        return switch (internalStatus) {
            case "ONLINE" -> "Healthy";
            case "DEGRADED" -> "Monitoring";
            case "ERROR" -> "Degraded";
            default -> "Unknown";
        };
    }


    /**
     * Real JVM heap usage and real JDBC connection pool health - not
     * synthetic infrastructure data.
     */
    private List<InfrastructureMetric> buildInfrastructureMetrics() {

        List<InfrastructureMetric> metrics =
                new ArrayList<>();

        Runtime runtime =
                Runtime.getRuntime();

        long maxMemory =
                runtime.maxMemory();

        long usedMemory =
                runtime.totalMemory() - runtime.freeMemory();

        double memoryPercentage =
                maxMemory == 0
                        ? 0.0
                        : round((usedMemory * 100.0) / maxMemory);

        metrics.add(
                new InfrastructureMetric(
                        "jvm-heap",
                        "JVM Heap Usage",
                        formatMegabytes(usedMemory)
                                + " / "
                                + formatMegabytes(maxMemory),
                        memoryPercentage >= 90
                                ? "Degraded"
                                : "Operational",
                        memoryPercentage
                )
        );

        metrics.add(buildDatabasePoolMetric());

        Duration uptime =
                Duration.between(
                        applicationStartedAt,
                        Instant.now()
                );

        metrics.add(
                new InfrastructureMetric(
                        "uptime",
                        "Application Uptime",
                        formatDuration(uptime),
                        "Active",
                        null
                )
        );

        long requests =
                totalRequests.get();

        metrics.add(
                new InfrastructureMetric(
                        "ai-provider",
                        "AI Provider Connectivity",
                        requests == 0
                                ? "Not yet used"
                                : round(getSummary().successRate()) + "% success rate",
                        requests == 0
                                ? "Unknown"
                                : getSummary().successRate() >= 90
                                ? "Connected"
                                : "Degraded",
                        requests == 0 ? null : round(getSummary().successRate())
                )
        );

        return metrics;
    }


    private InfrastructureMetric buildDatabasePoolMetric() {

        if (!(dataSource instanceof HikariDataSource hikariDataSource)) {

            return new InfrastructureMetric(
                    "database-pool",
                    "Database Connections",
                    "Unavailable",
                    "Unknown",
                    null
            );
        }

        HikariPoolMXBean poolBean =
                hikariDataSource.getHikariPoolMXBean();

        if (poolBean == null) {

            return new InfrastructureMetric(
                    "database-pool",
                    "Database Connections",
                    "Not yet initialized",
                    "Unknown",
                    null
            );
        }

        int active =
                poolBean.getActiveConnections();

        int total =
                hikariDataSource.getMaximumPoolSize();

        double percentage =
                total == 0
                        ? 0.0
                        : round((active * 100.0) / total);

        return new InfrastructureMetric(
                "database-pool",
                "Database Connections",
                active + " / " + total + " active",
                percentage >= 90
                        ? "Degraded"
                        : "Connected",
                percentage
        );
    }


    /**
     * Real alerts derived from actual model/error state - an empty
     * list (no alerts rendered) means nothing is actually wrong, rather
     * than always showing a canned "all clear" entry.
     */
    private List<MonitoringAlert> buildAlerts(
            AIModelMonitoringSummary summary,
            List<AIModelMetricsResponse> models
    ) {

        List<MonitoringAlert> alerts =
                new ArrayList<>();

        if (summary.totalRequests() > 0 && summary.failureRate() >= 10.0) {

            alerts.add(
                    new MonitoringAlert(
                            "high-failure-rate",
                            "critical",
                            "Elevated AI failure rate",
                            round(summary.failureRate())
                                    + "% of AI requests have failed since application start.",
                            Instant.now()
                    )
            );
        }

        for (AIModelMetricsResponse model : models) {

            if ("ERROR".equals(model.status()) ||
                    "DEGRADED".equals(model.status())) {

                alerts.add(
                        new MonitoringAlert(
                                "model-" + model.modelName(),
                                "ERROR".equals(model.status())
                                        ? "critical"
                                        : "warning",
                                model.modelName() + " is experiencing issues",
                                model.failedRequests()
                                        + " failed request(s) out of "
                                        + model.totalRequests()
                                        + ".",
                                model.lastRequest()
                        )
                );
            }
        }

        return alerts;
    }


    /**
     * Only lists actions this service can genuinely execute - see
     * {@link #executeAction(String, Long)}.
     */
    private List<AdministrationAction> buildAdministrationActions() {

        return List.of(

                new AdministrationAction(
                        "run-ai-audit",
                        "shield",
                        "Run AI Audit",
                        "Re-scan documents and applications for issues needing review.",
                        "AI_AUDIT"
                ),

                new AdministrationAction(
                        "reset-metrics",
                        "refresh",
                        "Reset Metrics",
                        "Clear in-memory AI monitoring counters.",
                        "RESET_METRICS"
                )
        );
    }


    /**
     * ========================================================
     * EXECUTE ADMINISTRATION ACTION
     * ========================================================
     * <p>
     * Only recognizes the actions actually listed by
     * {@link #buildAdministrationActions()} - each one maps to a real
     * operation, never a no-op.
     *
     * @param action       one of the action codes from {@link #buildAdministrationActions()}
     * @param adminUserId  the requesting administrator, for the AI audit's notification
     * @return a human-readable result message
     */
    public String executeAction(
            String action,
            Long adminUserId
    ) {

        if (action == null || action.isBlank()) {

            throw new IllegalArgumentException(
                    "Action is required."
            );
        }

        return switch (action.trim().toUpperCase()) {

            case "AI_AUDIT" ->
                    aiAuditService.runAudit(adminUserId).message();

            case "RESET_METRICS" -> {
                resetMetrics();
                yield "AI monitoring metrics have been reset.";
            }

            default -> throw new IllegalArgumentException(
                    "Unknown administration action: " + action
            );
        };
    }


    /*
    |--------------------------------------------------------------------------
    | FORMATTING
    |--------------------------------------------------------------------------
    */

    private String formatMillis(
            double millis
    ) {

        if (millis >= 1000) {
            return round(millis / 1000.0) + "s";
        }

        return Math.round(millis) + "ms";
    }


    private String formatMegabytes(
            long bytes
    ) {

        return (bytes / (1024 * 1024)) + " MB";
    }


    private String formatDuration(
            Duration duration
    ) {

        long hours =
                duration.toHours();

        long minutes =
                duration.toMinutesPart();

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        return minutes + "m";
    }


    /**
     * ========================================================
     * GET MODEL METRICS
     * ========================================================
     */
    public List<AIModelMetricsResponse> getModelMetrics() {

        return modelMetrics.values()
                .stream()
                .map(ModelMetrics::toResponse)
                .sorted(
                        Comparator.comparing(
                                AIModelMetricsResponse::modelName
                        )
                )
                .toList();
    }


    /**
     * ========================================================
     * GET SINGLE MODEL METRICS
     * ========================================================
     */
    public AIModelMetricsResponse getModelMetrics(
            String modelName
    ) {

        String normalizedModel =
                normalizeModelName(modelName);

        ModelMetrics metrics =
                modelMetrics.get(normalizedModel);

        if (metrics == null) {
            return null;
        }

        return metrics.toResponse();
    }


    /**
     * ========================================================
     * RESET METRICS
     * ========================================================
     * <p>
     * Useful for administrative testing and development.
     * <p>
     * This method should NOT be exposed publicly unless the
     * controller explicitly protects it with ADMIN security.
     */
    public void resetMetrics() {

        modelMetrics.clear();

        totalRequests.set(0);

        successfulRequests.set(0);

        failedRequests.set(0);

        totalTokens.set(0);

        totalLatencyMs.set(0);

        totalErrors.set(0);

        lastActivity.set(Instant.now());

        log.warn(
                "AI monitoring metrics have been reset."
        );
    }


    /**
     * ========================================================
     * COUNT HEALTHY MODELS
     * ========================================================
     */
    private long countHealthyModels() {

        return modelMetrics.values()
                .stream()
                .filter(
                        metrics ->
                                "ONLINE".equals(
                                        metrics.getStatus()
                                )
                )
                .count();
    }


    /**
     * ========================================================
     * COUNT UNHEALTHY MODELS
     * ========================================================
     */
    private long countUnhealthyModels() {

        return modelMetrics.values()
                .stream()
                .filter(
                        metrics ->
                                !"ONLINE".equals(
                                        metrics.getStatus()
                                )
                )
                .count();
    }


    /**
     * ========================================================
     * MODEL NAME NORMALIZATION
     * ========================================================
     */
    private String normalizeModelName(
            String modelName
    ) {

        if (modelName == null ||
                modelName.isBlank()) {

            return "UNKNOWN";
        }

        return modelName
                .trim()
                .replaceAll("\\s+", " ");
    }


    /**
     * ========================================================
     * STATUS NORMALIZATION
     * ========================================================
     */
    private String normalizeStatus(
            String status
    ) {

        if (status == null ||
                status.isBlank()) {

            return "UNKNOWN";
        }

        return status
                .trim()
                .toUpperCase();
    }


    /**
     * ========================================================
     * ROUND DECIMAL
     * ========================================================
     */
    private double round(
            double value
    ) {

        return Math.round(value * 100.0) / 100.0;
    }


    /**
     * ========================================================
     * INTERNAL MODEL METRICS
     * ========================================================
     * <p>
     * Thread-safe metrics holder.
     */
    private static final class ModelMetrics {

        private final String modelName;

        private final AtomicLong requests =
                new AtomicLong(0);

        private final AtomicLong successfulRequests =
                new AtomicLong(0);

        private final AtomicLong failedRequests =
                new AtomicLong(0);

        private final AtomicLong errors =
                new AtomicLong(0);

        private final AtomicLong totalTokens =
                new AtomicLong(0);

        private final AtomicLong totalLatencyMs =
                new AtomicLong(0);

        private final AtomicReference<String> status =
                new AtomicReference<>("UNKNOWN");

        private final AtomicReference<Instant> lastRequest =
                new AtomicReference<>();


        private ModelMetrics(
                String modelName
        ) {

            this.modelName =
                    Objects.requireNonNull(
                            modelName
                    );
        }


        private void recordRequest(
                long latencyMs,
                long tokens,
                boolean successful
        ) {

            requests.incrementAndGet();

            totalLatencyMs.addAndGet(
                    latencyMs
            );

            totalTokens.addAndGet(
                    tokens
            );

            if (successful) {

                successfulRequests
                        .incrementAndGet();

                status.set("ONLINE");

            } else {

                failedRequests
                        .incrementAndGet();

                status.set("DEGRADED");
            }

            lastRequest.set(
                    Instant.now()
            );
        }


        private void recordError() {

            errors.incrementAndGet();

            failedRequests.incrementAndGet();

            status.set("ERROR");

            lastRequest.set(
                    Instant.now()
            );
        }


        private void setStatus(
                String status
        ) {

            this.status.set(status);
        }


        private String getStatus() {

            return status.get();
        }


        private AIModelMetricsResponse toResponse() {

            long total =
                    requests.get();

            long successful =
                    successfulRequests.get();

            long failed =
                    failedRequests.get();

            double averageLatency =
                    total == 0
                            ? 0.0
                            : (double)
                            totalLatencyMs.get()
                            / total;

            double successRate =
                    total == 0
                            ? 0.0
                            : ((double)
                            successful
                            / total)
                            * 100.0;

            return new AIModelMetricsResponse(
                    modelName,
                    status.get(),
                    total,
                    successful,
                    failed,
                    errors.get(),
                    totalTokens.get(),
                    roundValue(
                            averageLatency
                    ),
                    roundValue(
                            successRate
                    ),
                    lastRequest.get()
            );
        }


        private static double roundValue(
                double value
        ) {

            return Math.round(
                    value * 100.0
            ) / 100.0;
        }
    }


    /**
     * ========================================================
     * LIVE MONITORING RESPONSE
     * ========================================================
     */
    public record AIModelMonitoringResponse(

            Instant timestamp,

            Instant applicationStartedAt,

            Instant lastActivity,

            long totalRequests,

            long successfulRequests,

            long failedRequests,

            long totalErrors,

            long totalTokens,

            double averageLatencyMs,

            double successRate,

            double errorRate,

            List<AIModelMetricsResponse> models

    ) {

        public AIModelMonitoringResponse {

            models =
                    models == null
                            ? List.of()
                            : List.copyOf(models);
        }
    }


    /**
     * ========================================================
     * MONITORING SUMMARY
     * ========================================================
     */
    public record AIModelMonitoringSummary(

            long totalRequests,

            long successfulRequests,

            long failedRequests,

            long totalErrors,

            long totalTokens,

            double averageLatencyMs,

            double successRate,

            double failureRate,

            long totalModels,

            long healthyModels,

            long unhealthyModels,

            Instant lastActivity

    ) {
    }


    /**
     * ========================================================
     * INDIVIDUAL MODEL METRICS
     * ========================================================
     */
    public record AIModelMetricsResponse(

            String modelName,

            String status,

            long totalRequests,

            long successfulRequests,

            long failedRequests,

            long errors,

            long totalTokens,

            double averageLatencyMs,

            double successRate,

            Instant lastRequest

    ) {
    }


    /**
     * ========================================================
     * DASHBOARD VIEW DTOs
     * ========================================================
     * <p>
     * Shape consumed by the admin AI Model Monitoring page
     * (AIModelMonitoringPage.tsx). {@code color}/{@code icon} are
     * presentation keys the frontend already knows how to render -
     * matching its existing contract exactly, rather than introducing
     * a second shape it would need new code to consume.
     */
    public record AIModelMonitoringDashboardResponse(

            List<DashboardMetricCard> metrics,

            List<DashboardMetricCard> secondaryMetrics,

            List<DashboardAiModel> models,

            List<InfrastructureMetric> infrastructure,

            List<MonitoringAlert> alerts,

            List<AdministrationAction> administrationActions,

            Instant lastUpdated

    ) {
    }


    public record DashboardMetricCard(

            String id,

            String icon,

            String title,

            String value,

            String subtitle,

            String color,

            MetricTrend trend

    ) {
    }


    /**
     * Always {@code null} today - computing a real trend arrow would
     * require persisting historical snapshots of these metrics, which
     * this in-memory service does not do. Left as a real type (rather
     * than removed) so it can be populated honestly if that lands
     * later.
     */
    public record MetricTrend(

            double value,

            String label,

            boolean positive

    ) {
    }


    public record DashboardAiModel(

            String id,

            String name,

            String status,

            double accuracy,

            double latency,

            long requests,

            String version

    ) {
    }


    public record InfrastructureMetric(

            String id,

            String label,

            String value,

            String status,

            Double percentage

    ) {
    }


    public record MonitoringAlert(

            String id,

            String type,

            String title,

            String description,

            Instant timestamp

    ) {
    }


    public record AdministrationAction(

            String id,

            String icon,

            String label,

            String description,

            String action

    ) {
    }
}

