package com.godfrey.ai_immigration_document_analyzer.fact.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Authorizes a specific CASE_WORKER to access a specific subject's Facts.
 *
 * This is the mechanism that makes the CASE_WORKER / SYSTEM_ADMINISTRATOR
 * separation real rather than cosmetic: holding the {@code ADMIN} role
 * grants NO Fact access on its own (see {@code FactAuthorizationService}) -
 * only an active CaseAssignment (held by a CASE_WORKER-role user) does.
 */
@Entity
@Table(
        name = "CASE_ASSIGNMENTS",
        indexes = {
                @Index(name = "IDX_ASSIGNMENT_WORKER", columnList = "CASE_WORKER_USER_ID"),
                @Index(name = "IDX_ASSIGNMENT_SUBJECT", columnList = "SUBJECT_USER_ID"),
                @Index(name = "IDX_ASSIGNMENT_WORKER_SUBJECT", columnList = "CASE_WORKER_USER_ID, SUBJECT_USER_ID")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CaseAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "CASE_WORKER_USER_ID", nullable = false)
    private Long caseWorkerUserId;

    @Column(name = "SUBJECT_USER_ID", nullable = false)
    private Long subjectUserId;

    @Column(name = "ASSIGNED_BY_USER_ID")
    private Long assignedByUserId;

    @Builder.Default
    @Column(name = "ACTIVE", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "ASSIGNED_AT", nullable = false, updatable = false)
    private LocalDateTime assignedAt;
}
