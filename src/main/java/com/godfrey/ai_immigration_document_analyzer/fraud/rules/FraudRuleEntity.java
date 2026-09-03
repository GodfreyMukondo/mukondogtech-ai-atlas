package com.godfrey.ai_immigration_document_analyzer.fraud.rules;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "FRAUD_RULES")
public class FraudRuleEntity {

    @Id
    @Column(name = "RULE_ID", length = 50)
    private String id;

    @Column(name = "RULE_NAME", nullable = false, length = 200)
    private String name;

    @Column(name = "PATTERN", nullable = false, length = 500)
    private String pattern;

    @Column(name = "SEVERITY")
    private int severity;

    @Column(name = "ACTIVE")
    private boolean active;
}