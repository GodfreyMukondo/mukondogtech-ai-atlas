package com.godfrey.ai_immigration_document_analyzer.fraud.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FraudAuditLogger {

    private final FraudAuditRepository repository;

    public void log(String inputHash, boolean fraud, double score) {

        FraudAuditEntity entity = new FraudAuditEntity();
        entity.setInputHash(inputHash);
        entity.setFraud(fraud);
        entity.setFraudScore(score);
        entity.setCreatedAt(LocalDateTime.now());

        repository.save(entity);
    }
}