import type {
  EvaluationCertaintyLevel,
  RequirementEvaluationOutcome,
} from "./pathwayAssessment";

import type {
  EvidenceSourceType,
  FactCategory,
  FactConfidenceLevel,
  FactEvidence,
  FactProvenanceType,
} from "./fact";

/**
 * ============================================================================
 * PATHWAY EXPLANATION TYPES
 * ============================================================================
 *
 * Mirrors the backend's ExplanationResponse exactly
 * (GET /api/pathways/assessments/{assessmentId}/explanation):
 *
 *   PATHWAY -> REQUIREMENT -> REQUIREMENT EVALUATION -> FACT -> EVIDENCE
 *           -> (DOCUMENT via evidence.documentId) -> REGULATORY VERSION
 *
 * Deliberately reuses RequirementEvaluationOutcome/EvaluationCertaintyLevel
 * from ./pathwayAssessment, and the Fact vocabulary from ./fact, rather
 * than redefining either - one canonical Fact/assessment vocabulary, never
 * two drifting copies.
 *
 * This file holds shapes only. The frontend never caches or re-derives Fact
 * data from this response beyond what is needed to render one explanation
 * view - it is not a second source of truth for Facts.
 * ============================================================================
 */

export type { EvidenceSourceType, FactCategory, FactConfidenceLevel, FactEvidence, FactProvenanceType };

export type RequirementType =
  | "ELIGIBILITY_ATTRIBUTE"
  | "CREDENTIAL"
  | "EXPERIENCE"
  | "CAPACITY"
  | "PROFICIENCY"
  | "RELATIONSHIP"
  | "PROCEDURAL"
  | "LEGAL_STANDING"
  | "COMPOSITE";

/**
 * Trust tier of the REGULATORY VERSION behind a requirement - never to be
 * confused with whether a Fact is verified. AI-generated/ingested
 * regulatory content never becomes authoritative merely by existing.
 */
export type RegulatoryVerificationStatus =
  | "UNVERIFIED_INGESTION"
  | "HUMAN_VERIFIED"
  | "AUTHORITATIVE_CONFIRMED";

/**
 * ============================================================================
 * FACT (as surfaced for explainability only - not a cached Fact store)
 * ============================================================================
 */
export interface FactExplanation {
  factId: number;
  factKey: string;
  category: FactCategory;
  valueSummary?: string | null;
  provenanceType: FactProvenanceType;
  isVerified: boolean;
  confidenceScore?: number | null;
  confidenceLevel: FactConfidenceLevel;
  evidence: FactEvidence[];
}

/**
 * ============================================================================
 * REQUIREMENT EXPLANATION (Requirement + its Evaluation + Regulatory Version)
 * ============================================================================
 */
export interface RequirementExplanation {
  requirementId: number;
  requirementKey: string;
  requirementTitle: string;
  requirementType: RequirementType;
  mandatory: boolean;
  outcome: RequirementEvaluationOutcome;
  explanation?: string | null;
  certaintyLevel: EvaluationCertaintyLevel;
  regulatoryVersionId: number;
  regulatorySourceAuthority: string;
  regulatorySourceReference?: string | null;
  regulatoryVerificationStatus: RegulatoryVerificationStatus;
  contributingFacts: FactExplanation[];
  unresolvedConflictIds: number[];
}

/**
 * ============================================================================
 * PATHWAY EXPLANATION (top-level response)
 * ============================================================================
 */
export interface PathwayExplanation {
  pathwayAssessmentId: number;
  pathwayKey: string;
  pathwayName: string;
  overallOutcome: RequirementEvaluationOutcome;
  requirements: RequirementExplanation[];
}
