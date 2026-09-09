import type {
  EvaluationCertaintyLevel,
  RequirementEvaluationOutcome,
} from "./pathwayAssessment";

import type {
  FactExplanation,
  RegulatoryVerificationStatus,
  RequirementType,
} from "./pathwayExplanation";

import type { Fact, FactConflict } from "./fact";

/**
 * ============================================================================
 * CASE INTELLIGENCE TYPES
 * ============================================================================
 *
 * Mirrors the backend's Phase 2 Case Intelligence DTOs exactly
 * (GET /api/cases/**). Deliberately reuses the Requirement/Pathway outcome
 * vocabulary from ./pathwayAssessment, the explainability Fact shape from
 * ./pathwayExplanation, and the Digital Twin's own Fact/FactConflict types
 * from ./fact - never a second, drifting copy of any of them.
 *
 * IMPORTANT: none of these figures are, or imply, a probability of visa
 * approval, and no field here is or implies a fraud determination.
 * ============================================================================
 */

export type CaseIssueSeverity = "CRITICAL" | "HIGH" | "MEDIUM";

export type CaseRiskBand = "NORMAL" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

/**
 * A Case-Intelligence-specific reclassification of a Requirement's raw
 * nine-state {@link RequirementEvaluationOutcome} - never a rename of it.
 * See the backend's {@code CaseRequirementSupportStatus} Javadoc for the
 * exact mapping and for why NOT_SATISFIED was added beyond the six
 * originally-specified values (a definitive, evidenced failure must never
 * be blurred into "MISSING evidence").
 */
export type CaseRequirementSupportStatus =
  | "SATISFIED"
  | "PARTIALLY_SUPPORTED"
  | "NOT_SATISFIED"
  | "MISSING"
  | "CONFLICTING"
  | "NEEDS_VERIFICATION"
  | "NOT_ASSESSABLE";

/**
 * Whether a missing piece of evidence is bound to a MANDATORY requirement
 * (REQUIRED) or a non-mandatory one (SUPPORTING). The third,
 * non-item-specific "merely recommended" tier is
 * {@link CaseIntelligence.recommendedEvidenceGuidance} - the Pathway's own
 * pre-existing, non-gating evidenceExpectations text.
 */
export type EvidenceNecessity = "REQUIRED" | "SUPPORTING";

/**
 * ============================================================================
 * REQUIREMENT-TO-EVIDENCE MATRIX
 * ============================================================================
 */
export interface RequirementEvidenceMatrixRow {
  requirementId: number;
  requirementKey: string;
  requirementTitle: string;
  requirementType: RequirementType;
  mandatory: boolean;
  supportStatus: CaseRequirementSupportStatus;
  rawOutcome: RequirementEvaluationOutcome;
  certaintyLevel: EvaluationCertaintyLevel;
  regulatoryVersionId: number;
  regulatorySourceAuthority: string;
  regulatoryVerificationStatus: RegulatoryVerificationStatus;
  supportingFacts: FactExplanation[];
  missingFactKeys: string[];
  conflictingFactConflictIds: number[];
  explanation?: string | null;
}

/**
 * ============================================================================
 * CASE READINESS
 * ============================================================================
 */
export interface OutstandingIssue {
  requirementId: number;
  requirementKey: string;
  requirementTitle: string;
  mandatory: boolean;
  outcome: RequirementEvaluationOutcome;
  severity: CaseIssueSeverity;
  reason?: string | null;
}

export interface CaseReadiness {
  requirementCoveragePercent: number;
  evidenceCoveragePercent: number;
  consistencyPercent: number;
  overallReadinessPercent: number;
  outstandingIssues: OutstandingIssue[];
}

/**
 * ============================================================================
 * CASE OVERVIEW SUMMARY
 * ============================================================================
 */
export interface CaseOverviewSummary {
  requirementCoveragePercent: number;
  evidenceCoveragePercent: number;
  overallReadinessPercent: number;
  missingEvidenceCount: number;
  verificationNeededCount: number;
  contradictionCount: number;
  timelineIssueCount: number;
  riskBand: CaseRiskBand;
  note: string;
}

/**
 * ============================================================================
 * MISSING EVIDENCE
 * ============================================================================
 */
export interface MissingEvidenceItem {
  requirementId: number;
  requirementKey: string;
  requirementTitle: string;
  mandatory: boolean;
  necessity: EvidenceNecessity;
  priority: CaseIssueSeverity;
  missingFactKeys: string[];
  reason?: string | null;
}

/**
 * ============================================================================
 * CASE INTELLIGENCE BUNDLE (pathway-assessment-scoped)
 * ============================================================================
 */
export interface CaseIntelligence {
  pathwayAssessmentId: number;
  subjectUserId: number;
  pathwayKey: string;
  pathwayName: string;
  overallOutcome: RequirementEvaluationOutcome;
  overview: CaseOverviewSummary;
  readiness: CaseReadiness;
  evidenceMatrix: RequirementEvidenceMatrixRow[];
  missingEvidence: MissingEvidenceItem[];
  recommendedEvidenceGuidance?: string | null;
  generatedAt: string;
}

/**
 * ============================================================================
 * CASE-WIDE (subjectUserId-scoped)
 * ============================================================================
 */

/**
 * A read-time-only (never persisted) observation that two of the subject's
 * own Facts of the same historical-multi-valued key claim overlapping time
 * windows - a "potential contradiction" requiring verification, never a
 * fraud finding.
 */
export interface PotentialOverlapContradiction {
  factKey: string;
  factAId: number;
  factAEffectiveFrom?: string | null;
  factAEffectiveTo?: string | null;
  factBId: number;
  factBEffectiveFrom?: string | null;
  factBEffectiveTo?: string | null;
  overlapDays: number;
  description: string;
}

export interface CaseContradictions {
  subjectUserId: number;
  openContradictions: FactConflict[];
  potentialOverlaps: PotentialOverlapContradiction[];
  generatedAt: string;
  note: string;
}

export interface CaseTimelineEvent {
  fact: Fact;
  gapDaysBeforeThisEntry?: number | null;
  overlapDaysWithPreviousEntry?: number | null;
}

export interface CaseTimeline {
  subjectUserId: number;
  events: CaseTimelineEvent[];
  generatedAt: string;
  note: string;
}

export interface CaseSignals {
  subjectUserId: number;
  riskBand: CaseRiskBand;
  fraudFlaggedDocumentCount: number;
  highRiskDocumentCount: number;
  mediumRiskDocumentCount: number;
  openContradictionCount: number;
  potentialOverlapContradictionCount: number;
  rejectedEvidenceItemCount: number;
  validationFailedEvidenceItemCount: number;
  generatedAt: string;
  note: string;
}
