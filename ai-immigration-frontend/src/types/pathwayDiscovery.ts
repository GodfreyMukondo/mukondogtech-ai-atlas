import type { RequirementEvaluationOutcome } from "./pathwayAssessment";
import type { RegulatoryVerificationStatus } from "./pathwayExplanation";
import type { CaseRequirementSupportStatus } from "./caseIntelligence";

/**
 * ============================================================================
 * PATHWAY DISCOVERY TYPES
 * ============================================================================
 *
 * Mirrors the backend Phase 4 DTOs exactly:
 *
 * - PathwayDiscoveryResponse
 * - PathwayRankingRow
 * - TopMissingRequirement
 *
 * A ranked row is a TRANSIENT, read-only alignment result - it is never a
 * PathwayAssessment, never persisted, and never re-fetchable by id. Reuses
 * the same RequirementEvaluationOutcome, RegulatoryVerificationStatus and
 * CaseRequirementSupportStatus vocabularies already defined elsewhere in
 * this app - this file defines shapes only, it invents no new vocabulary.
 * ============================================================================
 */

export interface TopMissingRequirement {
  requirementId: number;
  requirementKey: string;
  requirementTitle: string;
  mandatory: boolean;
  supportStatus: CaseRequirementSupportStatus;
  missingFactKeys: string[];
}

export interface PathwayRankingRow {
  rank: number;
  pathwayId: number;
  pathwayKey: string;
  name: string;
  jurisdiction: string;
  category: string;

  outcome: RequirementEvaluationOutcome;

  overallAlignmentScore: number;
  requirementCoveragePercent: number;
  evidenceCoveragePercent: number;
  consistencyPercent: number;

  regulatoryCertainty: RegulatoryVerificationStatus;

  supportedRequirementCount: number;
  partialRequirementCount: number;
  missingRequirementCount: number;
  conflictingRequirementCount: number;
  needsVerificationCount: number;

  topMissingRequirements: TopMissingRequirement[];

  explanation?: string | null;
}

export interface PathwayDiscoveryResponse {
  subjectUserId: number;
  generatedAt: string;
  totalPublishedPathways: number;
  rankedPathways: PathwayRankingRow[];
  disclaimer: string;
}
