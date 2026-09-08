package com.godfrey.ai_immigration_document_analyzer.requirement.entity;

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
 * PATHWAY ASSESSMENT
 * ============================================================================
 *
 * The personalized, terminal artifact of evaluating one {@code Pathway}
 * against one subject's Facts as of one {@link #assessmentDate}
 * (Requirement/Pathway Architecture Specification, section 9). The
 * {@code Pathway} itself is never mutated or copied into by this process -
 * this row only references it by id. Never a Fact, never authoritative
 * truth about eligibility - a computed, re-derivable opinion.
 * ============================================================================
 */
@Entity
@Table(
        name = "PATHWAY_ASSESSMENTS",
        indexes = {
                @Index(name = "IDX_PATHWAY_ASSESSMENT_PATHWAY", columnList = "PATHWAY_ID"),
                @Index(name = "IDX_PATHWAY_ASSESSMENT_SUBJECT", columnList = "SUBJECT_USER_ID"),
                @Index(name = "IDX_PATHWAY_ASSESSMENT_SUBJECT_PATHWAY", columnList = "SUBJECT_USER_ID, PATHWAY_ID")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PathwayAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "PATHWAY_ID", nullable = false)
    private Long pathwayId;

    @Column(name = "SUBJECT_USER_ID", nullable = false)
    private Long subjectUserId;

    @Column(name = "ASSESSMENT_DATE", nullable = false)
    private LocalDateTime assessmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "OUTCOME", nullable = false, length = 30)
    private RequirementEvaluationOutcome outcome;

    @Column(name = "ASSESSMENT_CONFIDENCE_SCORE", columnDefinition = "NUMBER(5,4)")
    private Double assessmentConfidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "ASSESSMENT_CONFIDENCE_LEVEL", nullable = false, length = 20)
    private EvaluationCertaintyLevel assessmentConfidenceLevel;

    @CreationTimestamp
    @Column(name = "COMPUTED_AT", nullable = false, updatable = false)
    private LocalDateTime computedAt;
}
