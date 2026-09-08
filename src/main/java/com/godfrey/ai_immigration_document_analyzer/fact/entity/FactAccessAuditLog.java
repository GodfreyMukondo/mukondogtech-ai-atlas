package com.godfrey.ai_immigration_document_analyzer.fact.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Records WHO read WHICH Facts, WHEN, and for what declared purpose -
 * distinct from {@code FactTimelineEvent}, which records what is known, not
 * who looked at it. Both granted and denied attempts are logged (denials
 * are the evidence base for IDOR/least-privilege testing and governance
 * review of over-broad access requests).
 */
@Entity
@Table(
        name = "FACT_ACCESS_AUDIT_LOG",
        indexes = {
                @Index(name = "IDX_ACCESS_AUDIT_SUBJECT", columnList = "SUBJECT_USER_ID"),
                @Index(name = "IDX_ACCESS_AUDIT_ACCESSOR", columnList = "ACCESSED_BY_USER_ID"),
                @Index(name = "IDX_ACCESS_AUDIT_AT", columnList = "ACCESSED_AT")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FactAccessAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "SUBJECT_USER_ID", nullable = false)
    private Long subjectUserId;

    @Column(name = "FACT_ID")
    private Long factId;

    @Column(name = "ACCESSED_BY_USER_ID")
    private Long accessedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACCESSOR_TYPE", nullable = false, length = 20)
    private AccessorType accessorType;

    @Column(name = "PURPOSE", length = 255)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "SENSITIVITY_TIER_ACCESSED", length = 30)
    private FactSensitivityTier sensitivityTierAccessed;

    @Column(name = "GRANTED", nullable = false)
    private Boolean granted;

    @Column(name = "DENIAL_REASON", length = 255)
    private String denialReason;

    @CreationTimestamp
    @Column(name = "ACCESSED_AT", nullable = false, updatable = false)
    private LocalDateTime accessedAt;
}
