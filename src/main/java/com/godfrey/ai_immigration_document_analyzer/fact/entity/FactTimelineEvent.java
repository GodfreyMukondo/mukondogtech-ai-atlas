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
 * FACT TIMELINE EVENT
 * ============================================================================
 *
 * The transactional, append-only audit record of what changed, when, by
 * whom, and why. Every writer in the Fact foundation must produce its event
 * in the SAME transaction as the state change it describes.
 *
 * UNLIKE {@code NotificationService} (deliberately best-effort - a
 * notification failing must never block the action it describes), a
 * Timeline write is NEVER best-effort: if it fails, the whole transaction -
 * including the Fact/Conflict state change - rolls back. A dropped audit
 * entry would silently undermine the auditability this entire model exists
 * to provide. This is enforced structurally: {@code FactTimelineService}
 * never catches/swallows an exception from its own save.
 * ============================================================================
 */
@Entity
@Table(
        name = "FACT_TIMELINE_EVENTS",
        indexes = {
                @Index(name = "IDX_TIMELINE_SUBJECT", columnList = "SUBJECT_USER_ID"),
                @Index(name = "IDX_TIMELINE_FACT", columnList = "FACT_ID"),
                @Index(name = "IDX_TIMELINE_OCCURRED_AT", columnList = "OCCURRED_AT")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FactTimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "SUBJECT_USER_ID", nullable = false)
    private Long subjectUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", nullable = false, length = 40)
    private TimelineEventType eventType;

    @Column(name = "FACT_ID")
    private Long factId;

    @Column(name = "CONFLICT_ID")
    private Long conflictId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACTOR_TYPE", nullable = false, length = 20)
    private AccessorType actorType;

    @Column(name = "ACTOR_USER_ID")
    private Long actorUserId;

    @Column(name = "PREVIOUS_STATUS", length = 30)
    private String previousStatus;

    @Column(name = "NEW_STATUS", length = 30)
    private String newStatus;

    @Column(name = "REASON", length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "OCCURRED_AT", nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
