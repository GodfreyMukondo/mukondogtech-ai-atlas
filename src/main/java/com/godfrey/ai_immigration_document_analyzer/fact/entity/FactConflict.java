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
 * ============================================================================
 * FACT CONFLICT
 * ============================================================================
 *
 * Two comparable-standing Facts of the same subject+factKey disagree.
 * Modeled pairwise for this stage (factA/factB), not as an N-way join -
 * every scenario validated in the approved specification was pairwise;
 * extending to N-way later would add a join table without breaking this
 * schema.
 *
 * CRITICAL INVARIANT: a conflict is CONFLICT DETECTED, never
 * FRAUD DETECTED. Nothing in this entity or the services that operate on it
 * may set a Fact's status to anything resembling fraud - that determination,
 * if it ever exists, belongs to a separate Risk process with its own
 * evidence and human sign-off.
 * ============================================================================
 */
@Entity
@Table(
        name = "FACT_CONFLICTS",
        indexes = {
                @Index(name = "IDX_FACT_CONFLICT_SUBJECT", columnList = "SUBJECT_USER_ID"),
                @Index(name = "IDX_FACT_CONFLICT_STATUS", columnList = "STATUS"),
                @Index(name = "IDX_FACT_CONFLICT_FACT_A", columnList = "FACT_A_ID"),
                @Index(name = "IDX_FACT_CONFLICT_FACT_B", columnList = "FACT_B_ID")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FactConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "SUBJECT_USER_ID", nullable = false)
    private Long subjectUserId;

    @Column(name = "FACT_KEY", nullable = false, length = 150)
    private String factKey;

    @Column(name = "FACT_A_ID", nullable = false)
    private Long factAId;

    @Column(name = "FACT_B_ID", nullable = false)
    private Long factBId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "STATUS", nullable = false, length = 20)
    private ConflictStatus status = ConflictStatus.OPEN;

    @CreationTimestamp
    @Column(name = "DETECTED_AT", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "RESOLVED_AT")
    private LocalDateTime resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "RESOLUTION_TYPE", length = 30)
    private ConflictResolutionType resolutionType;

    @Column(name = "RESOLVED_BY_USER_ID")
    private Long resolvedByUserId;

    @Column(name = "WINNING_FACT_ID")
    private Long winningFactId;

    @Column(name = "RESOLUTION_NOTES", length = 1000)
    private String resolutionNotes;
}
