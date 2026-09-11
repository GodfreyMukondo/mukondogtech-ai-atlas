package com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto;

/**
 * The relationship types the Evidence Intelligence Graph renders (Evidence
 * Intelligence Graph design, section 6) - every one backed by an existing
 * foreign key/JSON reference or the two new tables' FKs. Deliberately no
 * generic "RELATED_TO" - every edge has a precise semantic meaning.
 *
 * No {@code CONTRADICTS_FACT} edge runs directly from an EvidenceItem to a
 * Fact it did not produce - by design, contradiction is always mediated
 * through two Facts already in a {@code FactConflict}
 * ({@link #CONFLICTS_WITH}); see the design document, section 6, for the
 * reasoning.
 */
public enum GraphRelationshipType {

    /** Document -> DocumentVersion. */
    HAS_VERSION,

    /** DocumentVersion -> EvidenceItem. */
    PRODUCES,

    /** EvidenceItem <-> Fact, via (evolved) FactEvidence. */
    SUPPORTS_FACT,

    /** Fact <-> Fact, via the existing FactConflict record. Never a fraud determination. */
    CONFLICTS_WITH,

    /** Fact -> Fact, via the existing (currently unpopulated) FactDerivation table. */
    DERIVED_FROM,

    /** RequirementEvaluation -> Fact, via the existing RequirementEvaluationFact table. */
    DEPENDS_ON_FACT,

    /** RequirementEvaluation -> Requirement, via the existing RequirementEvaluation.requirementId FK. */
    EVALUATED_UNDER,

    /** RequirementEvaluation -> FactConflict, via the existing RequirementEvaluationConflict table. */
    BLOCKED_BY_CONFLICT,

    /** PathwayAssessment -> RequirementEvaluation, via the existing PathwayAssessmentRequirementEvaluation table. */
    CONTAINS_EVALUATION,

    /** Requirement -> RegulatoryVersion, via the existing Requirement.regulatoryVersionId FK. */
    VERSIONED_UNDER,

    /** Pathway -> Requirement, via Pathway.compositionLogic's JSON RequirementRefNode leaves. */
    COMPOSED_OF
}
