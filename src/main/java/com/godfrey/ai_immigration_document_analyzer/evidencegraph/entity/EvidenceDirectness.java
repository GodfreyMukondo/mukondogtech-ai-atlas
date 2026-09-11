package com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity;

/**
 * How directly a piece of evidence bears on the Fact it supports (Evidence
 * Intelligence Graph design, section 8) - one dimension of the evidence
 * strength vector, never collapsed into a single confidence number.
 */
public enum EvidenceDirectness {

    /** The evidence explicitly states the value being claimed (e.g. a passport's nationality field). */
    DIRECT,

    /** The evidence supports the value only by implication (e.g. an address on an unrelated document). */
    INDIRECT,

    /** The evidence was reasoned/computed from other evidence, not read directly from a source. */
    INFERRED
}
