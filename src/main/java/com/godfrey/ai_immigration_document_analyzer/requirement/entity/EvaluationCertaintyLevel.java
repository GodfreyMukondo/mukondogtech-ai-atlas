package com.godfrey.ai_immigration_document_analyzer.requirement.entity;

/**
 * How confidently a {@code RequirementEvaluation}/{@code PathwayAssessment}
 * outcome holds - deliberately a separate type from
 * {@code FactConfidenceLevel}, even though the band semantics are similar,
 * so a Requirement Evaluation can never be mistaken for a Fact and the two
 * confidence concepts can evolve independently (approved specification,
 * section 15).
 */
public enum EvaluationCertaintyLevel {

    LOW,
    MODERATE,
    HIGH,

    /** Reserved for non-definitive outcomes (INSUFFICIENT_EVIDENCE, UNKNOWN, CONFLICTED, NOT_APPLICABLE, EXPIRED, PENDING_REVIEW). */
    NOT_APPLICABLE
}
