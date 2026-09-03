package com.godfrey.ai_immigration_document_analyzer.fraud;

import com.godfrey.ai_immigration_document_analyzer.fraud.audit.FraudAuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudService {

    private final FraudRuleEngine ruleEngine;
    private final FraudScoringEngine scoringEngine;
    private final FraudAuditLogger auditLogger;

    public FraudDetectionResult analyze(String text) {

        List<RiskFinding> risks = ruleEngine.evaluate(text);

        double score = scoringEngine.calculateScore(risks);

        boolean fraud = score >= 0.6;

        auditLogger.log(
                String.valueOf(text.hashCode()),
                fraud,
                score
        );

        return FraudDetectionResult.builder()
                .fraudulent(fraud)
                .fraudScore(score)
                .risks(risks)
                .build();
    }
}