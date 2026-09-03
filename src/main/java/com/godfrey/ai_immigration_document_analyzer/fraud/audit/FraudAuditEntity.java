package com.godfrey.ai_immigration_document_analyzer.fraud.audit;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "FRAUD_AUDIT_LOGS",
        indexes = {
                @Index(name = "idx_input_hash", columnList = "INPUT_HASH"),
                @Index(name = "idx_fraud_flag", columnList = "FRAUD_FLAG"),
                @Index(name = "idx_fraud_score", columnList = "FRAUD_SCORE"),
                @Index(name = "idx_created_at", columnList = "CREATED_AT"),
                @Index(name = "idx_document_id", columnList = "DOCUMENT_ID"),
                @Index(name = "idx_user_id", columnList = "USER_ID")
        }
)
public class FraudAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "INPUT_HASH", length = 100)
    private String inputHash;

    /**
     * Fraud flag stored in DB
     */
    @Column(name = "FRAUD_FLAG")
    private boolean fraudFlag;

    @Column(name = "FRAUD_SCORE")
    private double fraudScore;

    /**
     * NEW: required by repository methods
     */
    @Column(name = "DOCUMENT_ID")
    private Long documentId;

    /**
     * NEW: required by repository methods
     */
    @Column(name = "USER_ID")
    private Long userId;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically sets creation timestamp before insert
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Backward compatibility getter
     */
    public boolean isFraud() {
        return fraudFlag;
    }

    /**
     * Backward compatibility setter
     */
    public void setFraud(boolean fraud) {
        this.fraudFlag = fraud;
    }
}