import type {
  EvaluationCertaintyLevel,
  RequirementEvaluationOutcome,
} from "../types/pathwayAssessment";

/**
 * ============================================================================
 * ASSESSMENT OUTCOME DISPLAY
 * ============================================================================
 *
 * The single place the nine-state RequirementEvaluationOutcome vocabulary is
 * translated into UI copy - so every screen that shows an outcome (request
 * results, and later the explainability view) renders it identically and
 * never collapses it into a pass/fail boolean.
 *
 * Deliberate wording choices:
 *
 * - INSUFFICIENT_EVIDENCE and UNKNOWN never say "not satisfied" or "failed".
 * - CONFLICTED explicitly states this is not a fraud finding.
 * - Certainty/confidence labels never use the word "verified" - confidence
 *   in an evaluation is a distinct concept from Fact verification.
 * ============================================================================
 */

export type OutcomeBadgeVariant =
  | "primary"
  | "success"
  | "warning"
  | "danger"
  | "neutral";

export interface OutcomeDisplay {
  label: string;
  badgeVariant: OutcomeBadgeVariant;
  description: string;
}

const OUTCOME_DISPLAY: Record<RequirementEvaluationOutcome, OutcomeDisplay> = {
  SATISFIED: {
    label: "Satisfied",
    badgeVariant: "success",
    description: "The available evidence meets this requirement.",
  },
  PARTIALLY_SATISFIED: {
    label: "Partially Satisfied",
    badgeVariant: "warning",
    description: "Some, but not all, parts of this are currently satisfied.",
  },
  NOT_SATISFIED: {
    label: "Not Satisfied",
    badgeVariant: "danger",
    description: "The available evidence does not meet this requirement.",
  },
  INSUFFICIENT_EVIDENCE: {
    label: "Insufficient Evidence",
    badgeVariant: "neutral",
    description:
      "Required evidence is missing or not yet sufficiently verified. This is not a negative finding.",
  },
  UNKNOWN: {
    label: "Unknown",
    badgeVariant: "neutral",
    description: "This could not yet be evaluated.",
  },
  CONFLICTED: {
    label: "Conflicting Information",
    badgeVariant: "warning",
    description:
      "Two pieces of information disagree and require review. This is not a finding of fraud.",
  },
  NOT_APPLICABLE: {
    label: "Not Applicable",
    badgeVariant: "neutral",
    description: "This requirement does not apply to you.",
  },
  EXPIRED: {
    label: "Expired",
    badgeVariant: "warning",
    description: "Supporting evidence is no longer within its validity window.",
  },
  PENDING_REVIEW: {
    label: "Pending Review",
    badgeVariant: "neutral",
    description: "This requires human review before a determination can be made.",
  },
};

export function getOutcomeDisplay(
  outcome: RequirementEvaluationOutcome,
): OutcomeDisplay {
  return OUTCOME_DISPLAY[outcome];
}

const CERTAINTY_LABEL: Record<EvaluationCertaintyLevel, string> = {
  HIGH: "High confidence",
  MODERATE: "Moderate confidence",
  LOW: "Low confidence",
  NOT_APPLICABLE: "Confidence not applicable",
};

/**
 * Confidence in an EVALUATION's outcome - never to be confused with, or
 * labeled as, Fact verification status.
 */
export function getCertaintyLabel(
  level: EvaluationCertaintyLevel,
): string {
  return CERTAINTY_LABEL[level];
}
