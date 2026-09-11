package com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity;

/**
 * Evidence lifecycle status (Evidence Intelligence Graph design, section
 * 12), modelled after {@code FactStatus} but deliberately distinct - an
 * EvidenceItem may exist, and be reasoned about, before any Fact does.
 *
 * CORROBORATING/CONTESTED/EXPIRED are deliberately NOT members of this
 * enum - they are computed, read-time labels derived by joining an
 * EvidenceItem's linked Fact status and temporal window at query time
 * (see EvidenceGraphService), never persisted columns that could drift out
 * of sync with the Fact they describe. This mirrors why the Digital Twin
 * and Explainability views are pure read projections rather than cached
 * tables.
 */
public enum EvidenceItemStatus {

    /** A candidate piece of evidentiary material has been identified, not yet structured. */
    DISCOVERED,

    /** A structured candidate value has been produced (OCR/LLM extraction, or manual entry). */
    EXTRACTED,

    /** Failed deterministic shape/type validation. Never reaches Fact proposal. Terminal. */
    VALIDATION_FAILED,

    /** Passed shape validation; not yet linked to any Fact. */
    CANDIDATE,

    /** Attached, via FactEvidence, to at least one Fact. */
    LINKED,

    /** A newer DocumentVersion produced replacement evidence for the same claim. Terminal. */
    SUPERSEDED,

    /**
     * A case worker or administrator determined this specific evidence item
     * is not usable - distinct from the Fact it may still be linked to
     * being rejected. Never reachable by the applicant (no self-adjudication,
     * mirroring FactAuthorizationService.assertCanVerifyOrResolve). Terminal.
     */
    REJECTED
}
