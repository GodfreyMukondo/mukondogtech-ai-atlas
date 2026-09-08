package com.godfrey.ai_immigration_document_analyzer.requirement.entity;

/**
 * Trust tier of a {@link RegulatoryVersion} (approved specification, section
 * 14). An {@code UNVERIFIED_INGESTION} version may still be evaluated
 * against - the system should be useful before every rule is manually
 * re-confirmed - but every RequirementEvaluation/PathwayAssessment derived
 * from it must carry that fact forward into its certainty computation and
 * explanation.
 */
public enum RegulatoryVerificationStatus {

    /** Ingested (including AI-assisted extraction) but not yet confirmed by a human against the primary source. */
    UNVERIFIED_INGESTION,

    /** A human has confirmed this version's content against its source. */
    HUMAN_VERIFIED,

    /** Directly confirmed against the regulator's own authoritative publication. */
    AUTHORITATIVE_CONFIRMED
}
