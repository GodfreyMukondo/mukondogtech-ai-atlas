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
 * PATHWAY
 * ============================================================================
 *
 * Knowledge about a possible immigration ROUTE (Requirement/Pathway
 * Architecture Specification, section 7) - never a person's application.
 * Identical for every applicant ever evaluated against it: no
 * {@code subjectUserId}, no status of anyone's progress, no copy of any
 * Fact. See {@code PathwayAssessment} for the personalized, disposable
 * result of evaluating this Pathway against one subject's Digital Twin, and
 * the pre-existing {@code Application} entity for a person's actual,
 * stateful process.
 *
 * {@link #compositionLogic} is a JSON {@code LogicNode} tree whose leaves
 * are {@code RequirementRefNode}s - Requirements are referenced, never
 * duplicated, so one regulatory change to a shared Requirement (e.g. a
 * language test threshold) updates every Pathway that references it.
 * ============================================================================
 */
@Entity
@Table(
        name = "PATHWAYS",
        indexes = {
                @Index(name = "IDX_PATHWAY_KEY", columnList = "PATHWAY_KEY"),
                @Index(name = "IDX_PATHWAY_JURISDICTION", columnList = "JURISDICTION"),
                @Index(name = "IDX_PATHWAY_STATUS", columnList = "STATUS")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pathway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "PATHWAY_KEY", nullable = false, length = 150)
    private String pathwayKey;

    @Column(name = "NAME", nullable = false, length = 255)
    private String name;

    @Column(name = "DESCRIPTION", length = 2000)
    private String description;

    @Column(name = "JURISDICTION", nullable = false, length = 100)
    private String jurisdiction;

    @Column(name = "CATEGORY", nullable = false, length = 100)
    private String category;

    /** JSON LogicNode tree over RequirementRefNode leaves. */
    @Lob
    @Column(name = "COMPOSITION_LOGIC", nullable = false)
    private String compositionLogic;

    /** Descriptive only - non-gating guidance about what evidence a complete application typically includes. */
    @Lob
    @Column(name = "EVIDENCE_EXPECTATIONS")
    private String evidenceExpectations;

    @Column(name = "VALID_FROM")
    private LocalDateTime validFrom;

    @Column(name = "VALID_TO")
    private LocalDateTime validTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private PathwayStatus status;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
