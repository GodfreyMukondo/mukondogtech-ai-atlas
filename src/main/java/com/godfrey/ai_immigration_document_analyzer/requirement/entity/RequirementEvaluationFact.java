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
 * A Fact actually read while producing one {@code RequirementEvaluation} -
 * the evidence trail explainability (section 10) walks from Pathway down to
 * Fact. A reference only: this table never copies the Fact's value.
 */
@Entity
@Table(
        name = "REQUIREMENT_EVALUATION_FACTS",
        indexes = {
                @Index(name = "IDX_REQ_EVAL_FACT_EVAL", columnList = "EVALUATION_ID"),
                @Index(name = "IDX_REQ_EVAL_FACT_FACT", columnList = "FACT_ID")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RequirementEvaluationFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "EVALUATION_ID", nullable = false)
    private Long evaluationId;

    @Column(name = "FACT_ID", nullable = false)
    private Long factId;
}
