package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

/**
 * Distinguishes, for one missing piece of evidence, whether it is bound to
 * a MANDATORY requirement (legally required for this pathway as currently
 * catalogued) or to an OPTIONAL one (needed only to support that specific,
 * non-mandatory requirement). A third, non-item-specific tier exists
 * alongside this enum: {@code Pathway.evidenceExpectations} - descriptive,
 * non-gating guidance about evidence a complete application typically
 * includes - surfaced separately as
 * {@code CaseIntelligenceResponse.recommendedEvidenceGuidance} rather than
 * as a per-item necessity value, since it is not tied to any one
 * Requirement's binding.
 */
public enum EvidenceNecessity {

    /** Bound to a mandatory requirement of the pathway. */
    REQUIRED,

    /** Bound to a non-mandatory (optional) requirement of the pathway. */
    SUPPORTING
}
