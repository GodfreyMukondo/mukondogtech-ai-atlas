package com.godfrey.ai_immigration_document_analyzer.requirement.entity;

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

/**
 * Links a {@code PathwayAssessment} to every {@code RequirementEvaluation}
 * that fed it - the middle link in the explainability chain PATHWAY ->
 * REQUIREMENT -> REQUIREMENT EVALUATION (section 10).
 */
@Entity
@Table(
        name = "PATHWAY_ASSESSMENT_REQ_EVALS",
        indexes = {
                @Index(name = "IDX_PA_REQ_EVAL_ASSESSMENT", columnList = "ASSESSMENT_ID"),
                @Index(name = "IDX_PA_REQ_EVAL_EVALUATION", columnList = "EVALUATION_ID")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PathwayAssessmentRequirementEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "ASSESSMENT_ID", nullable = false)
    private Long assessmentId;

    @Column(name = "EVALUATION_ID", nullable = false)
    private Long evaluationId;
}
