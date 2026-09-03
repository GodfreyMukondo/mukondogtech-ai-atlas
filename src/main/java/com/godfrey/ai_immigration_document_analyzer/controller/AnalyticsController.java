package com.godfrey.ai_immigration_document_analyzer.controller;

import com.godfrey.ai_immigration_document_analyzer.analytics.projection.*;
import com.godfrey.ai_immigration_document_analyzer.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
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
}