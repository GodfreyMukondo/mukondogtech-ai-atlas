/**
 * ============================================================================
 * PATHWAY / REQUIREMENT ASSESSMENT TYPES
 * ============================================================================
 *
 * Mirrors the backend Requirement/Pathway Phase 1 DTOs exactly:
 *
 * - PathwayResponse
 * - PathwayAssessmentResponse
 * - RequirementEvaluationResponse
 *
 * A Pathway is impersonal regulatory knowledge (never a person's
 * application). A PathwayAssessment/RequirementEvaluation is a
 * personalized, disposable, re-derivable OPINION about one subject as of
 * one assessment date - never a Fact, and never a second copy of Fact data.
 * This file defines shapes only; it holds no state and duplicates nothing.
 *
 * IMPORTANT: the nine-value RequirementEvaluationOutcome union is
 * deliberately NOT collapsible into a boolean. In particular:
 *
 * - INSUFFICIENT_EVIDENCE must never be displayed or treated as
 *   NOT_SATISFIED - it means required evidence is missing or not yet
 *   sufficiently verified, not that the answer is "no".
 * - UNKNOWN must never be displayed or treated as NOT_SATISFIED either -
 *   it means the evaluation itself could not be completed.
 * - CONFLICTED means competing evidence exists - it must never be
 *   presented as, or imply, fraud.
 * ============================================================================
 */

export type PathwayStatus =
  | "DRAFT"
  | "REVIEW"
  | "PUBLISHED"
  | "SUPERSEDED"
  | "ARCHIVED";

export type RequirementEvaluationOutcome =
  | "SATISFIED"
  | "NOT_SATISFIED"
  | "PARTIALLY_SATISFIED"
  | "INSUFFICIENT_EVIDENCE"
  | "UNKNOWN"
  | "CONFLICTED"
  | "NOT_APPLICABLE"
  | "EXPIRED"
  | "PENDING_REVIEW";

export type EvaluationCertaintyLevel =
  | "LOW"
  | "MODERATE"
  | "HIGH"
  | "NOT_APPLICABLE";

/**
 * ============================================================================
 * PATHWAY (catalogue - impersonal)
 * ============================================================================
 */
export interface Pathway {
  id: number;
  pathwayKey: string;
  name: string;
  description?: string | null;
  jurisdiction: string;
  category: string;
  validFrom?: string | null;
  validTo?: string | null;
  status: PathwayStatus;
}

/**
 * ============================================================================
 * REQUIREMENT EVALUATION (personalized)
 * ============================================================================
 *
 * contributingFactIds/unresolvedConflictIds are opaque references only -
 * the frontend never resolves or caches the underlying Fact data itself
 * from this response; that belongs to the future Explainability view.
 */
export interface RequirementEvaluation {
  id: number;
  requirementId: number;
  requirementKey: string;
  requirementTitle: string;
  regulatoryVersionId: number;
  subjectUserId: number;
  assessmentDate: string;
  outcome: RequirementEvaluationOutcome;
  certaintyScore?: number | null;
  certaintyLevel: EvaluationCertaintyLevel;
  explanation?: string | null;
  contributingFactIds: number[];
  unresolvedConflictIds: number[];
  evaluatedAt: string;
}

/**
 * ============================================================================
 * PATHWAY ASSESSMENT (personalized, terminal result)
 * ============================================================================
 */
export interface PathwayAssessment {
  id: number;
  pathwayId: number;
  pathwayKey: string;
  pathwayName: string;
  subjectUserId: number;
  assessmentDate: string;
  outcome: RequirementEvaluationOutcome;
  assessmentConfidenceScore?: number | null;
  assessmentConfidenceLevel: EvaluationCertaintyLevel;
  requirementEvaluations: RequirementEvaluation[];
  computedAt: string;
}
