import type { ConflictStatus, ConflictResolutionType, FactCategory, FactConfidenceLevel, FactProvenanceType, FactStatus } from "./fact";
import type { PathwayExplanation } from "./pathwayExplanation";

/**
 * ============================================================================
 * EVIDENCE INTELLIGENCE GRAPH TYPES
 * ============================================================================
 *
 * Mirrors the backend's evidencegraph.dto package exactly
 * (GET /api/evidence-graph/**). This is a read-projection layer over the
 * existing Fact/Requirement/Pathway domain - these types hold no data and
 * are not a store; every screen that imports them still reads live from
 * the backend on each request.
 * ============================================================================
 */

export type GraphNodeType =
  | "DOCUMENT"
  | "DOCUMENT_VERSION"
  | "EVIDENCE"
  | "FACT"
  | "REQUIREMENT"
  | "REQUIREMENT_EVALUATION"
  | "PATHWAY"
  | "PATHWAY_ASSESSMENT"
  | "REGULATORY_VERSION"
  | "CONFLICT";

export type GraphRelationshipType =
  | "HAS_VERSION"
  | "PRODUCES"
  | "SUPPORTS_FACT"
  | "CONFLICTS_WITH"
  | "DERIVED_FROM"
  | "DEPENDS_ON_FACT"
  | "EVALUATED_UNDER"
  | "BLOCKED_BY_CONFLICT"
  | "CONTAINS_EVALUATION"
  | "VERSIONED_UNDER"
  | "COMPOSED_OF";

export interface GraphNode {
  id: string;
  type: GraphNodeType;
  label: string;
  metadata: Record<string, unknown>;
}

export interface GraphEdge {
  id: string;
  sourceNodeId: string;
  targetNodeId: string;
  relationship: GraphRelationshipType;
  metadata: Record<string, unknown>;
}

export interface EvidenceGraph {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

/** Fact-node metadata shape, as populated by EvidenceGraphService.factNode. */
export interface FactNodeMetadata {
  factKey: string;
  category: FactCategory;
  status: FactStatus;
  provenanceType: FactProvenanceType;
  confidenceLevel: FactConfidenceLevel;
  confidenceScore: number | null;
  isVerified: boolean;
  valueSummary: string | null;
}

/** Conflict edge metadata, as populated by EvidenceGraphService.addConflictSubgraph. */
export interface ConflictEdgeMetadata {
  conflictId: number;
  status: ConflictStatus;
  resolutionType: ConflictResolutionType | null;
}

/**
 * Document-node metadata (Phase 3). `fraudDetected`/`riskLevel` are the
 * legacy, explicitly non-forensic risk indicator - a signal only, never a
 * fraud determination.
 */
export interface DocumentNodeMetadata {
  fileName: string;
  documentType: string;
  uploadedAt: string;
  fraudDetected: boolean | null;
  riskLevel: string | null;
}

/** RegulatoryVersion-node metadata (Phase 3 regulatory traceability). */
export interface RegulatoryVersionNodeMetadata {
  regulationIdentity: string;
  jurisdiction: string;
  sourceAuthority: string;
  sourceReference: string | null;
  verificationStatus: "UNVERIFIED_INGESTION" | "HUMAN_VERIFIED" | "AUTHORITATIVE_CONFIRMED";
  effectiveFrom: string | null;
  effectiveTo: string | null;
}

/**
 * Conflict-node metadata (Phase 3) - a {@code FactConflict} rendered as its
 * own node. CONFLICT DETECTED only, never a fraud determination.
 */
export interface ConflictNodeMetadata {
  factKey: string;
  status: ConflictStatus;
  resolutionType: ConflictResolutionType | null;
  detectedAt: string;
  resolvedAt: string | null;
}

export type EvidenceSourceType =
  | "DOCUMENT"
  | "USER_STATEMENT"
  | "ADMIN_ATTESTATION"
  | "EXTERNAL_SOURCE"
  | "DERIVATION";

export type EvidenceItemStatus =
  | "DISCOVERED"
  | "EXTRACTED"
  | "VALIDATION_FAILED"
  | "CANDIDATE"
  | "LINKED"
  | "SUPERSEDED"
  | "REJECTED";

export type EvidenceDirectness = "DIRECT" | "INDIRECT" | "INFERRED";

/**
 * ============================================================================
 * EVIDENCE ITEM DETAIL (three deliberately separate sections)
 * ============================================================================
 *
 * SOURCE DATA, SYSTEM INTERPRETATION and DERIVED CONCLUSION are never
 * merged into one flat object - this mirrors the backend's
 * EvidenceItemResponse exactly, so a UI can never accidentally render an
 * AI's interpretation as if it were the original source, or a derived Fact
 * as if it were independently verified.
 */
export interface EvidenceItemSourceData {
  documentId: number | null;
  documentFileName: string | null;
  documentVersionId: number | null;
  documentVersionNumber: number | null;
  sourceSnippet: string | null;
  documentIssueDate: string | null;
  documentExpiryDate: string | null;
}

export interface EvidenceItemSystemInterpretation {
  extractionMethod: string | null;
  extractionConfidence: number | null;
  extractedAt: string;
}

export interface EvidenceItemDerivedConclusion {
  factId: number;
  factKey: string;
  valueSummary: string | null;
  isVerified: boolean;
  factConfidenceScore: number | null;
}

export interface EvidenceItem {
  id: number;
  sourceType: EvidenceSourceType;
  status: EvidenceItemStatus;
  directness: EvidenceDirectness | null;
  rejectionReason: string | null;
  sourceData: EvidenceItemSourceData;
  systemInterpretation: EvidenceItemSystemInterpretation;
  derivedConclusion: EvidenceItemDerivedConclusion | null;
}

/**
 * The full Pathway -> ... -> Document chain for one PathwayAssessment.
 * `explanation` is exactly the existing PathwayExplanation shape;
 * `evidenceByFactId` is the one addition this feature makes.
 */
export interface EvidenceFullTrace {
  explanation: PathwayExplanation;
  evidenceByFactId: Record<number, EvidenceItem[]>;
}
