package com.godfrey.ai_immigration_document_analyzer.analytics.repository;

import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

@Repository
public class RealTimeAnalyticsRepository {

    private final ConcurrentHashMap<String, AtomicLong> fraudCountByRule = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DoubleAdder> fraudScoreByRule = new ConcurrentHashMap<>();

    private final AtomicLong highRiskCounter = new AtomicLong(0);

    public void incrementFraudCount(String ruleId) {
        fraudCountByRule
                .computeIfAbsent(ruleId, k -> new AtomicLong(0))
                .incrementAndGet();
    }

    public void updateFraudScore(String ruleId, double score) {
        fraudScoreByRule
                .computeIfAbsent(ruleId, k -> new DoubleAdder())
                .add(score);
    }

    public void incrementHighRiskCount() {
        highRiskCounter.incrementAndGet();
    }

    public long getHighRiskCount() {
        return highRiskCounter.get();
    }
}