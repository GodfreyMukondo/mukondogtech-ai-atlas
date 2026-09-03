package com.godfrey.ai_immigration_document_analyzer.analytics.listener;

import com.godfrey.ai_immigration_document_analyzer.analytics.event.FraudEvent;
import com.godfrey.ai_immigration_document_analyzer.analytics.repository.RealTimeAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudEventListener {

    private final RealTimeAnalyticsRepository analyticsRepository;

    @Async
    @EventListener
    public void handleFraudEvent(FraudEvent event) {

        if (event == null) {
            log.warn("Received null FraudEvent");
            return;
        }

        try {
            log.info("Processing real-time fraud event: ruleId={}, score={}, flag={}",
                    event.getRuleId(),
                    event.getFraudScore(),
                    event.isFraudFlag());

            analyticsRepository.incrementFraudCount(event.getRuleId());

            analyticsRepository.updateFraudScore(
                    event.getRuleId(),
                    event.getFraudScore()
            );

            if (event.isFraudFlag()) {
                analyticsRepository.incrementHighRiskCount();
            }

        } catch (Exception ex) {
            log.error("Failed to process FraudEvent for ruleId={}", event.getRuleId(), ex);
        }
    }
}