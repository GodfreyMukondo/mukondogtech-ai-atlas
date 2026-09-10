package com.godfrey.ai_immigration_document_analyzer.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
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
 * AGENT RUN
 * ============================================================================
 *
 * The one persisted record of an agent invocation (Phase 5.1). Doubles as
 * the audit trail (what ran, which tools it invoked, what it found) and as
 * the durable result (so a completed run can be re-fetched without
 * re-executing the agent) - a separate "AgentState" entity was considered
 * and rejected: there is no in-progress, resumable state to track for a
 * single synchronous request/response goal like EXPLAIN_REQUIREMENT.
 *
 * Never itself a source of truth about a case: {@link #resultJson} is a
 * point-in-time explanation of Facts/Evidence/Requirements that remain
 * authoritative elsewhere. Nothing here is ever read back into the
 * Fact/Requirement/Pathway system - this table is write-once (a row's
 * {@code status}/{@code completedAt}/{@code resultJson} are set exactly once,
 * when the run finishes), purely so its history can be reviewed.
 * ============================================================================
 */
@Entity
@Table(
        name = "AGENT_RUNS",
        indexes = {
                @Index(name = "IDX_AGENT_RUN_SUBJECT", columnList = "SUBJECT_USER_ID"),
                @Index(name = "IDX_AGENT_RUN_REQUIREMENT", columnList = "REQUIREMENT_ID"),
                @Index(name = "IDX_AGENT_RUN_STATUS", columnList = "STATUS")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AgentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "GOAL", nullable = false, length = 50)
    private AgentGoalType goal;

    /** The case this run is about - always re-derived from the PathwayAssessment, never trusted from the caller. */
    @Column(name = "SUBJECT_USER_ID", nullable = false)
    private Long subjectUserId;

    /** Who actually asked - self, or an assigned case worker. Never granted elevated access for being an AI request. */
    @Column(name = "REQUESTED_BY_USER_ID", nullable = false)
    private Long requestedByUserId;

    @Column(name = "PATHWAY_ASSESSMENT_ID")
    private Long pathwayAssessmentId;

    @Column(name = "REQUIREMENT_ID")
    private Long requirementId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private AgentRunStatus status;

    /** The fixed, human-readable plan this run executed - never hidden chain-of-thought. */
    @Column(name = "PLAN_SUMMARY", length = 2000)
    private String planSummary;

    /** Comma-separated {@code AgentToolName} values actually invoked, in order - never a fabricated log entry. */
    @Column(name = "TOOLS_INVOKED", length = 500)
    private String toolsInvoked;

    /** The real model that produced the explanation, from the provider's own response metadata. Null when the LLM was never invoked. */
    @Column(name = "MODEL", length = 100)
    private String model;

    /** Concise, auditable reasoning summary - never raw chain-of-thought. */
    @Column(name = "REASONING_SUMMARY", length = 2000)
    private String reasoningSummary;

    /** The full structured {@code ExplainRequirementResult}, serialized, so a completed run can be re-fetched. */
    @Lob
    @Column(name = "RESULT_JSON")
    private String resultJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "GROUNDING_STATE", length = 30)
    private AgentGroundingState groundingState;

    @Builder.Default
    @Column(name = "HUMAN_REVIEW_REQUIRED", nullable = false)
    private Boolean humanReviewRequired = false;

    @Column(name = "ERROR_MESSAGE", length = 1000)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "STARTED_AT", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;
}
