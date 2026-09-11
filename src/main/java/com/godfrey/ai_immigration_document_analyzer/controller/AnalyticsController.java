package com.godfrey.ai_immigration_document_analyzer.controller;

import com.godfrey.ai_immigration_document_analyzer.analytics.projection.*;
import com.godfrey.ai_immigration_document_analyzer.dto.response.AnalyticsOverviewResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.ChatAnalyticsResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.DocumentAnalyticsResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.ProcessingAnalyticsResponse;
import com.godfrey.ai_immigration_document_analyzer.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public DashboardMetrics dashboard() {
        return analyticsService.getDashboardMetrics();
    }

    @GetMapping("/fraud/rules")
    public List<FraudRuleStats> fraudByRule() {
        return analyticsService.getFraudByRule();
    }

    @GetMapping("/fraud/risk")
    public List<FraudScoreStats> fraudRisk() {
        return analyticsService.getFraudRiskDistribution();
    }

    @GetMapping("/events")
    public List<EventStats> events() {
        return analyticsService.getEventStats();
    }

    // =========================================================================
    // ANALYTICS OVERVIEW PAGE
    // =========================================================================
    //
    // Backs the admin Analytics page (AnalyticsPage.tsx). Each sub-resource
    // is also reachable individually since the frontend fetches them
    // separately in a couple of places.

    @GetMapping("/overview")
    public AnalyticsOverviewResponse overview() {
        return analyticsService.getAnalyticsOverview();
    }

    @GetMapping("/documents")
    public DocumentAnalyticsResponse documentAnalytics() {
        return analyticsService.getDocumentAnalyticsOverview();
    }

    @GetMapping("/chat")
    public ChatAnalyticsResponse chatAnalytics() {
        return analyticsService.getChatAnalyticsOverview();
    }

    @GetMapping("/processing")
    public ProcessingAnalyticsResponse processingAnalytics() {
        return analyticsService.getProcessingAnalyticsOverview();
    }
}