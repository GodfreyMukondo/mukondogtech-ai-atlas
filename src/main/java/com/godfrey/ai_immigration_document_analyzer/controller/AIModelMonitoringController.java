package com.godfrey.ai_immigration_document_analyzer.controller;

import com.godfrey.ai_immigration_document_analyzer.service.AIModelMonitoringService;
import com.godfrey.ai_immigration_document_analyzer.service.AIModelMonitoringService.AIModelMetricsResponse;
import com.godfrey.ai_immigration_document_analyzer.service.AIModelMonitoringService.AIModelMonitoringResponse;
import com.godfrey.ai_immigration_document_analyzer.service.AIModelMonitoringService.AIModelMonitoringSummary;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * ============================================================
 * AI MODEL MONITORING CONTROLLER
 * ============================================================
 *
 * Administrative REST API for AI model monitoring.
 *
 * Base URL:
 *
 *     /api/admin/ai-monitoring
 *
 * Security:
 *
 *     ROLE_ADMIN required.
 *
 * Endpoints:
 *
 *     GET /
 *     GET /live
 *     GET /summary
 *     GET /models
 *     GET /models/{modelName}
 *
 * Responsibilities:
 *
 * - Expose AI monitoring metrics
 * - Expose live application metrics
 * - Expose dashboard summary metrics
 * - Expose individual model metrics
 * - Validate request parameters
 * - Prevent browser/proxy caching
 * - Provide structured HTTP responses
 *
 * IMPORTANT:
 *
 * The current monitoring service uses in-memory metrics.
 * Metrics therefore reset when the application restarts.
 *
 * ============================================================
 */
@RestController
@RequestMapping("/api/admin/ai-monitoring")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AIModelMonitoringController {

    private final AIModelMonitoringService monitoringService;


    /**
     * ========================================================
     * CACHE CONTROL
     * ========================================================
     *
     * Monitoring data is dynamic and should not be served from
     * a stale browser or intermediary cache.
     */
    private static final CacheControl NO_CACHE =
            CacheControl.noCache()
                    .noStore()
                    .mustRevalidate()
                    .cachePrivate();


    /**
     * ========================================================
     * GET COMPLETE MONITORING DATA
     * ========================================================
     *
     * Returns:
     *
     * - Global request metrics
     * - Success/failure statistics
     * - Error statistics
     * - Token usage
     * - Average latency
     * - Success rate
     * - Error rate
     * - Per-model metrics
     *
     * Endpoint:
     *
     * GET /api/admin/ai-monitoring
     *
     * Example:
     *
     * GET /api/admin/ai-monitoring?windowMinutes=60
     *
     * NOTE:
     *
     * The current in-memory monitoring service does not yet
     * persist historical windows. The windowMinutes parameter is
     * therefore accepted for API compatibility and future
     * time-windowed monitoring support.
     *
     * @param windowMinutes requested monitoring window
     * @return complete monitoring metrics
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AIModelMonitoringResponse> getMonitoring(
            @RequestParam(
                    name = "windowMinutes",
                    defaultValue = "60"
            )
            @Min(
                    value = 1,
                    message = "windowMinutes must be at least 1"
            )
            @Max(
                    value = 1440,
                    message = "windowMinutes cannot exceed 1440"
            )
            Integer windowMinutes
    ) {

        log.debug(
                "AI monitoring request received. windowMinutes={}",
                windowMinutes
        );

        /*
         * The current service exposes live application metrics.
         *
         * windowMinutes is intentionally retained in the API so
         * that historical/time-window monitoring can be added
         * later without changing the frontend contract.
         */
        AIModelMonitoringResponse response =
                monitoringService.getLiveMetrics();

        return ResponseEntity
                .ok()
                .cacheControl(NO_CACHE)
                .body(response);
    }


    /**
     * ========================================================
     * GET LIVE AI MODEL METRICS
     * ========================================================
     *
     * Lightweight endpoint intended for dashboard polling.
     *
     * Endpoint:
     *
     * GET /api/admin/ai-monitoring/live
     *
     * Recommended frontend polling interval:
     *
     * 30 seconds.
     *
     * @return live monitoring metrics
     */
    @GetMapping("/live")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AIModelMonitoringResponse> getLiveMetrics() {

        log.debug(
                "Live AI monitoring request received."
        );

        AIModelMonitoringResponse response =
                monitoringService.getLiveMetrics();

        return ResponseEntity
                .ok()
                .cacheControl(NO_CACHE)
                .body(response);
    }


    /**
     * ========================================================
     * GET MONITORING SUMMARY
     * ========================================================
     *
     * Optimized endpoint for dashboard KPI cards.
     *
     * Endpoint:
     *
     * GET /api/admin/ai-monitoring/summary
     *
     * Returns:
     *
     * - Total requests
     * - Successful requests
     * - Failed requests
     * - Total errors
     * - Token usage
     * - Average latency
     * - Success rate
     * - Failure rate
     * - Total models
     * - Healthy models
     * - Unhealthy models
     *
     * @param windowMinutes requested monitoring window
     * @return monitoring summary
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AIModelMonitoringSummary> getSummary(
            @RequestParam(
                    name = "windowMinutes",
                    defaultValue = "60"
            )
            @Min(
                    value = 1,
                    message = "windowMinutes must be at least 1"
            )
            @Max(
                    value = 1440,
                    message = "windowMinutes cannot exceed 1440"
            )
            Integer windowMinutes
    ) {

        log.debug(
                "AI monitoring summary request received. windowMinutes={}",
                windowMinutes
        );

        AIModelMonitoringSummary response =
                monitoringService.getSummary();

        return ResponseEntity
                .ok()
                .cacheControl(NO_CACHE)
                .body(response);
    }


    /**
     * ========================================================
     * GET ALL MODEL METRICS
     * ========================================================
     *
     * Returns monitoring metrics for every model currently
     * registered with the monitoring service.
     *
     * Endpoint:
     *
     * GET /api/admin/ai-monitoring/models
     *
     * @return list of model metrics
     */
    @GetMapping("/models")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AIModelMetricsResponse>> getModels() {

        log.debug(
                "AI model metrics request received."
        );

        List<AIModelMetricsResponse> response =
                monitoringService.getModelMetrics();

        return ResponseEntity
                .ok()
                .cacheControl(NO_CACHE)
                .body(response);
    }


    /**
     * ========================================================
     * GET SINGLE MODEL METRICS
     * ========================================================
     *
     * Endpoint:
     *
     * GET /api/admin/ai-monitoring/models/{modelName}
     *
     * Example:
     *
     * GET /api/admin/ai-monitoring/models/granite-3-8b-instruct
     *
     * @param modelName model identifier
     * @return model metrics
     */
    @GetMapping("/models/{modelName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AIModelMetricsResponse> getModel(
            @PathVariable String modelName
    ) {

        if (modelName == null ||
                modelName.isBlank()) {

            log.warn(
                    "AI model metrics requested with empty model name."
            );

            return ResponseEntity.badRequest().build();
        }

        String normalizedModelName =
                modelName.trim();

        log.debug(
                "AI model metrics request received. model={}",
                normalizedModelName
        );

        AIModelMetricsResponse response =
                monitoringService.getModelMetrics(
                        normalizedModelName
                );

        if (response == null) {

            log.debug(
                    "AI model not found in monitoring registry. model={}",
                    normalizedModelName
            );

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity
                .ok()
                .cacheControl(NO_CACHE)
                .body(response);
    }


    /**
     * ========================================================
     * HTTP CACHE POLICY
     * ========================================================
     *
     * Prevent sensitive operational monitoring information
     * from being cached by browsers or intermediary proxies.
     *
     * The controller currently uses CacheControl directly on
     * every response so that this behavior remains explicit.
     */
}

