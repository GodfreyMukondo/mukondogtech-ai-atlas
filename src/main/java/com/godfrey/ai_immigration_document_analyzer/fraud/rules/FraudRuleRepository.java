package com.godfrey.ai_immigration_document_analyzer.fraud.rules;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FraudRuleRepository extends JpaRepository<FraudRuleEntity, String> {

    List<FraudRuleEntity> findByActiveTrue();
}