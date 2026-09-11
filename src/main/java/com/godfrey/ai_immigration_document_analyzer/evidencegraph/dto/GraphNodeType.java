package com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto;

/**
 * The node types the Evidence Intelligence Graph renders - a superset drawn
 * entirely from types that already exist elsewhere in the domain
 * (Evidence Intelligence Graph design, sections 4-5). No node type here
 * introduces a new persisted entity beyond {@code EvidenceItem}/
 * {@code DocumentVersion}; every other node is a read-time projection of an
 * existing row.
 */
public enum GraphNodeType {
    DOCUMENT,
    DOCUMENT_VERSION,
    EVIDENCE,
    FACT,
    REQUIREMENT,
    REQUIREMENT_EVALUATION,
    PATHWAY,
    PATHWAY_ASSESSMENT,
    REGULATORY_VERSION,

    /**
     * A {@code FactConflict} rendered as its own node (Phase 3) - lets the
     * graph show "Fact A -> Conflict -> Fact B" as a distinct, inspectable
     * object (status, resolution type) rather than only edge metadata.
     * CONFLICT DETECTED only, exactly like everywhere else this domain
     * models a conflict - never a fraud determination.
     */
    CONFLICT
}
