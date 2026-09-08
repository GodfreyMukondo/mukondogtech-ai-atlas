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
 * An open {@code FactConflict} that blocked a {@code RequirementEvaluation}
 * from reaching a definitive answer (CONFLICTED outcome). References the
 * existing Fact Foundation conflict record - never a second copy of it, and
 * never a fraud determination.
 */
@Entity
@Table(
        name = "REQUIREMENT_EVALUATION_CONFLICTS",
        indexes = {
                @Index(name = "IDX_REQ_EVAL_CONFLICT_EVAL", columnList = "EVALUATION_ID"),
                @Index(name = "IDX_REQ_EVAL_CONFLICT_CONFLICT", columnList = "CONFLICT_ID")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RequirementEvaluationConflict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "EVALUATION_ID", nullable = false)
    private Long evaluationId;

    @Column(name = "CONFLICT_ID", nullable = false)
    private Long conflictId;
}
