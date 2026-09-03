package com.godfrey.ai_immigration_document_analyzer.fraud;

import com.godfrey.ai_immigration_document_analyzer.fraud.rules.FraudRuleEntity;
import com.godfrey.ai_immigration_document_analyzer.fraud.rules.FraudRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FraudRuleEngine {

    private final FraudRuleRepository ruleRepository;

    public List<RiskFinding> evaluate(String text) {

        List<RiskFinding> risks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return risks;
        }

        String normalized = text.toLowerCase();

        List<FraudRuleEntity> rules = ruleRepository.findByActiveTrue();

        for (FraudRuleEntity rule : rules) {

            if (rule.getPattern() == null) continue;

            boolean match = normalized.matches(".*" + rule.getPattern().toLowerCase() + ".*");

            if (match) {
                risks.add(RiskFinding.builder()
                        .ruleId(rule.getId())
                        .message("Matched rule: " + rule.getName())
                        .severity(rule.getSeverity())
                        .confidence(0.75) // baseline (can be AI later)
                        .build());
            }
        }

        return risks;
    }
}