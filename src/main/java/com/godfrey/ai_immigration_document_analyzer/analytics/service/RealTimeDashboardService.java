package com.godfrey.ai_immigration_document_analyzer.analytics.service;

import com.godfrey.ai_immigration_document_analyzer.analytics.repository.RealTimeAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealTimeDashboardService {

    private final RealTimeAnalyticsRepository repository;

    public long getHighRiskCount() {
        return repository.getHighRiskCount();
    }
}
