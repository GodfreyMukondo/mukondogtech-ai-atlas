package com.godfrey.ai_immigration_document_analyzer.analytics.service;

import com.godfrey.ai_immigration_document_analyzer.analytics.entity.FraudAnalyticsEntity;
import com.godfrey.ai_immigration_document_analyzer.analytics.event.FraudEvent;
import com.godfrey.ai_immigration_document_analyzer.analytics.publisher.FraudEventPublisher;
import com.godfrey.ai_immigration_document_analyzer.analytics.repository.FraudAnalyticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAnalyticsService {

    private final FraudAnalyticsRepository repository;
    private final FraudEventPublisher fraudEventPublisher;

    public void record(String ruleId,
                       double fraudScore,
                       boolean fraudFlag,
                       double confidence) {

        validateInput(ruleId, fraudScore, confidence);

        try {
            FraudAnalyticsEntity entity = new FraudAnalyticsEntity();
            entity.setRuleId(ruleId);
            entity.setFraudScore(fraudScore);
            entity.setFraudFlag(fraudFlag);
            entity.setConfidence(confidence);
            entity.setCreatedAt(LocalDateTime.now());

            repository.save(entity);

            log.info("Fraud analytics saved for ruleId={}, score={}, flag={}",
                    ruleId, fraudScore, fraudFlag);

            // 🔥 REAL-TIME EVENT PUSH
            fraudEventPublisher.publish( new FraudEvent(ruleId, fraudScore, fraudFlag, confidence)

            );

            log.info("Fraud event published for ruleId={}", ruleId);

        } catch (Exception ex) {
            log.error("Failed to record fraud analytics for ruleId={}", ruleId, ex);
            throw ex;
        }
    }

    private void validateInput(String ruleId,
                               double fraudScore,
                               double confidence) {

        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("ruleId cannot be empty");
        }

        if (fraudScore < 0 || fraudScore > 100) {
            throw new IllegalArgumentException("fraudScore must be between 0 and 100");
        }

        if (confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("confidence must be between 0 and 1");
        }
    }
}