import type {
  EvidenceDirectness,
  EvidenceItemStatus,
  GraphNodeType,
  GraphRelationshipType,
} from "../types/evidenceGraph";

/**
 * ============================================================================
 * EVIDENCE GRAPH LABELS
 * ============================================================================
 *
 * Display helpers for the Evidence Intelligence Graph. Mirrors the
 * conventions in ./factLabels - deliberately not shared with it, since a
 * graph node's display needs (a short label + a node-type chip) differ
 * from a Fact card's needs.
 * ============================================================================
 */

const NODE_TYPE_LABEL: Record<GraphNodeType, string> = {
  DOCUMENT: "Document",
  DOCUMENT_VERSION: "Document Version",
  EVIDENCE: "Evidence",
  FACT: "Fact",
  REQUIREMENT: "Requirement",
  REQUIREMENT_EVALUATION: "Requirement Evaluation",
  PATHWAY: "Pathway",
  PATHWAY_ASSESSMENT: "Pathway Assessment",
  REGULATORY_VERSION: "Regulatory Version",
  CONFLICT: "Potential Conflict",
};

export function getNodeTypeLabel(type: GraphNodeType): string {
  return NODE_TYPE_LABEL[type] ?? type;
}

const RELATIONSHIP_LABEL: Record<GraphRelationshipType, string> = {
  HAS_VERSION: "has version",
  PRODUCES: "produced",
  SUPPORTS_FACT: "supports",
  CONFLICTS_WITH: "conflicts with",
  DERIVED_FROM: "derived from",
  DEPENDS_ON_FACT: "depends on",
  EVALUATED_UNDER: "evaluated under",
  BLOCKED_BY_CONFLICT: "blocked by conflict",
  CONTAINS_EVALUATION: "contains evaluation",
  VERSIONED_UNDER: "versioned under",
  COMPOSED_OF: "composed of",
};

export function getRelationshipLabel(relationship: GraphRelationshipType): string {
  return RELATIONSHIP_LABEL[relationship] ?? relationship;
}

const EVIDENCE_STATUS_LABEL: Record<EvidenceItemStatus, string> = {
  DISCOVERED: "Discovered",
  EXTRACTED: "Extracted",
  VALIDATION_FAILED: "Failed Validation",
  CANDIDATE: "Candidate",
  LINKED: "Linked to a Fact",
  SUPERSEDED: "Superseded",
  REJECTED: "Rejected by a Case Worker",
};

/** Evidence lifecycle status - never to be read as a Fact's own status. */
export function getEvidenceStatusLabel(status: EvidenceItemStatus): string {
  return EVIDENCE_STATUS_LABEL[status] ?? status;
}

const DIRECTNESS_LABEL: Record<EvidenceDirectness, string> = {
  DIRECT: "Directly states this value",
  INDIRECT: "Supports this value by implication",
  INFERRED: "Reasoned from other evidence",
};

export function getDirectnessLabel(directness: EvidenceDirectness): string {
  return DIRECTNESS_LABEL[directness] ?? directness;
}
