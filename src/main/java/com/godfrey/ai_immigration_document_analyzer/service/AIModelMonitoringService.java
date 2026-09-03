package com.godfrey.ai_immigration_document_analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
public class AIModelMonitoringService {

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
}

