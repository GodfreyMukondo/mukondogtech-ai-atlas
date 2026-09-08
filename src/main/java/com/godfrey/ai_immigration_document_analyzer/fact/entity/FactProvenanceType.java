package com.godfrey.ai_immigration_document_analyzer.fact.entity;

/**
 * The single, immutable origin of a Fact. Set once at creation and never
 * rewritten - corroboration is captured as a verification overlay, never a
 * provenance mutation.
 *
 * Reconciles the terminology used across the approved design iterations:
 * ADMIN-entered facts are represented as USER_INPUT with
 * {@code createdByAccessorType = CASE_WORKER} on {@link Fact}, rather than a
 * separate enum value - the actor is who entered it, provenance is how the
 * claim originated. AUTHORITATIVE_REGULATORY and INFERRED/DERIVED_FROM_FACTS
 * from earlier design notes map to EXTERNAL_AUTHORITATIVE_SOURCE and
 * DERIVED_FACT respectively.
 */
public enum FactProvenanceType {

    /** Directly typed/selected by the applicant (self-report). */
    USER_INPUT,

    /** Derived from a Document by OCR/LLM extraction, not independently confirmed. */
    DOCUMENT_EXTRACTION,

    /** The Fact's very existence came from a human directly attesting/confirming it as new. */
    HUMAN_VERIFICATION,

    /** Sourced from an official regulatory or other authoritative external source. */
    EXTERNAL_AUTHORITATIVE_SOURCE,

    /** Derived by AI/system reasoning over other Facts - always carries a derivation link. */
    DERIVED_FACT,

    /** Produced by an internal system process (e.g. bookkeeping, not a claim about the world). */
    SYSTEM_PROCESS,

    /** Exists only inside a Simulation/Scenario overlay. Must never be readable by real-case reasoning. */
    SIMULATION,

    /** Temporarily held where the real value is unknown; carries a mandatory resolution expectation. */
    ASSUMPTION
}
