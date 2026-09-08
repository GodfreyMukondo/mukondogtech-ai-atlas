package com.godfrey.ai_immigration_document_analyzer.requirement.entity;

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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * REQUIREMENT
 * ============================================================================
 *
 * A RULE, never a result (Requirement/Pathway Architecture Specification,
 * section 1). A Requirement row never mentions a subject, never stores an
 * outcome, and never stores a personal Fact value - see
 * {@code RequirementEvaluation} for the personalized, disposable result of
 * applying one Requirement to one subject.
 *
 * Version-scoped: one row per {@link #regulatoryVersionId}. A 2027 change to
 * the same rule is a NEW row sharing {@link #requirementKey}, never an
 * in-place edit of this one - see {@code RegulatoryVersion}.
 *
 * {@link #applicabilityLogic} and {@link #satisfactionLogic} are JSON-
 * serialized {@code LogicNode} trees - data interpreted by
 * {@code LogicEvaluationService}, never executable code.
 * ============================================================================
 */
@Entity
@Table(
        name = "REQUIREMENTS",
        indexes = {
                @Index(name = "IDX_REQUIREMENT_KEY", columnList = "REQUIREMENT_KEY"),
                @Index(name = "IDX_REQUIREMENT_TYPE", columnList = "REQUIREMENT_TYPE"),
                @Index(name = "IDX_REQUIREMENT_STATUS", columnList = "STATUS"),
                @Index(name = "IDX_REQUIREMENT_REG_VERSION", columnList = "REGULATORY_VERSION_ID")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Requirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    /** Stable across every version of this rule, e.g. "EDUCATION.BACHELORS_OR_HIGHER". */
    @Column(name = "REQUIREMENT_KEY", nullable = false, length = 150)
    private String requirementKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "REQUIREMENT_TYPE", nullable = false, length = 40)
    private RequirementType requirementType;

    @Column(name = "TITLE", nullable = false, length = 255)
    private String title;

    @Column(name = "DESCRIPTION", length = 2000)
    private String description;

    @Column(name = "JURISDICTION", nullable = false, length = 100)
    private String jurisdiction;

    @Column(name = "IMMIGRATION_CONTEXT", length = 100)
    private String immigrationContext;

    @Column(name = "REGULATORY_VERSION_ID", nullable = false)
    private Long regulatoryVersionId;

    @Builder.Default
    @Column(name = "MANDATORY", nullable = false)
    private Boolean mandatory = true;

    /** JSON LogicNode tree. Null = always applicable. */
    @Lob
    @Column(name = "APPLICABILITY_LOGIC")
    private String applicabilityLogic;

    /** JSON LogicNode tree - the rule itself. */
    @Lob
    @Column(name = "SATISFACTION_LOGIC", nullable = false)
    private String satisfactionLogic;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private RequirementStatus status;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
