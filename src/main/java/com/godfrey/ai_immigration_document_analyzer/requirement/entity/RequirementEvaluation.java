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

import java.time.LocalDateTime;

/**
 * ============================================================================
 * REQUIREMENT EVALUATION
 * ============================================================================
 *
 * The RESULT of applying one {@code Requirement} (at one
 * {@code RegulatoryVersion}) to one subject's Facts, as of one
 * {@link #assessmentDate} (Requirement/Pathway Architecture Specification,
 * sections 1 and 13). Personal, disposable, always re-derivable from
 * {@code (Requirement, RegulatoryVersion, Facts-as-of-assessmentDate)} -
 * never itself treated as a Fact, and never mutated once recorded (a
 * changed answer is a new row, exactly like Fact supersession).
 *
 * {@link #derivedValuesJson} carries only transient computation artifacts
 * (e.g. "age = 27") scoped to this one evaluation's explanation - never a
 * second copy of a Fact's value.
 * ============================================================================
 */
@Entity
@Table(
        name = "REQUIREMENT_EVALUATIONS",
        indexes = {
                @Index(name = "IDX_REQ_EVAL_REQUIREMENT", columnList = "REQUIREMENT_ID"),
                @Index(name = "IDX_REQ_EVAL_SUBJECT", columnList = "SUBJECT_USER_ID"),
                @Index(name = "IDX_REQ_EVAL_SUBJECT_REQ", columnList = "SUBJECT_USER_ID, REQUIREMENT_ID"),
                @Index(name = "IDX_REQ_EVAL_OUTCOME", columnList = "OUTCOME")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RequirementEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "REQUIREMENT_ID", nullable = false)
    private Long requirementId;

    @Column(name = "REGULATORY_VERSION_ID", nullable = false)
    private Long regulatoryVersionId;

    /** The person this evaluation is about. Never confused with who ran/requested the evaluation. */
    @Column(name = "SUBJECT_USER_ID", nullable = false)
    private Long subjectUserId;

    /** The "as of" date this evaluation is valid for - not necessarily "now" (section 13). */
    @Column(name = "ASSESSMENT_DATE", nullable = false)
    private LocalDateTime assessmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "OUTCOME", nullable = false, length = 30)
    private RequirementEvaluationOutcome outcome;

    @Column(name = "CERTAINTY_SCORE", columnDefinition = "NUMBER(5,4)")
    private Double certaintyScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "CERTAINTY_LEVEL", nullable = false, length = 20)
    private EvaluationCertaintyLevel certaintyLevel;

    @Column(name = "EXPLANATION", length = 1000)
    private String explanation;

    /** JSON list of DerivedValueRecord - explanation-support only, never a Fact. */
    @Lob
    @Column(name = "DERIVED_VALUES_JSON")
    private String derivedValuesJson;

    @CreationTimestamp
    @Column(name = "EVALUATED_AT", nullable = false, updatable = false)
    private LocalDateTime evaluatedAt;
}
