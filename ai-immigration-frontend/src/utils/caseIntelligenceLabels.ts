import type {
  CaseIssueSeverity,
  CaseRequirementSupportStatus,
  CaseRiskBand,
  EvidenceNecessity,
} from "../types/caseIntelligence";

import type { OutcomeBadgeVariant } from "./assessmentOutcome";

/**
 * ============================================================================
 * CASE INTELLIGENCE LABELS
 * ============================================================================
 *
 * Display copy for the Phase 2 Case Intelligence vocabulary - requirement
 * support status, evidence necessity, readiness issue severity, and
 * evidence/anomaly risk banding. Deliberate wording:
 *
 * - Support status describes how well CURRENT evidence supports one
 *   requirement, never a final legal eligibility conclusion.
 * - Severity describes how urgently an outstanding requirement or missing
 *   evidence item needs attention, never a prediction of the final outcome.
 * - A risk band is a non-forensic aggregate indicator only - it is NEVER
 *   presented as a fraud determination or proof of an anomaly.
 * ============================================================================
 */

const SUPPORT_STATUS_LABEL: Record<CaseRequirementSupportStatus, string> = {
  SATISFIED: "Satisfied",
  PARTIALLY_SUPPORTED: "Partially Supported",
  NOT_SATISFIED: "Not Satisfied",
  MISSING: "Evidence Missing",
  CONFLICTING: "Conflicting Evidence",
  NEEDS_VERIFICATION: "Needs Verification",
  NOT_ASSESSABLE: "Not Assessable",
};

const SUPPORT_STATUS_DESCRIPTION: Record<CaseRequirementSupportStatus, string> = {
  SATISFIED: "The available evidence fully meets this requirement.",
  PARTIALLY_SUPPORTED: "Some, but not all, parts of this requirement are currently supported.",
  NOT_SATISFIED: "The available evidence is sufficient and does not meet this requirement.",
  MISSING: "No evidence has been recorded yet for this requirement. This is not a negative finding.",
  CONFLICTING: "Two pieces of information disagree and require review. This is not a finding of fraud.",
  NEEDS_VERIFICATION: "Evidence exists but does not yet meet the expected verification or currency bar.",
  NOT_ASSESSABLE: "This requirement does not apply to you, or could not yet be evaluated.",
};

const SUPPORT_STATUS_BADGE_VARIANT: Record<CaseRequirementSupportStatus, OutcomeBadgeVariant> = {
  SATISFIED: "success",
  PARTIALLY_SUPPORTED: "warning",
  NOT_SATISFIED: "danger",
  MISSING: "neutral",
  CONFLICTING: "warning",
  NEEDS_VERIFICATION: "neutral",
  NOT_ASSESSABLE: "neutral",
};

export function getSupportStatusLabel(status: CaseRequirementSupportStatus): string {
  return SUPPORT_STATUS_LABEL[status];
}

export function getSupportStatusDescription(status: CaseRequirementSupportStatus): string {
  return SUPPORT_STATUS_DESCRIPTION[status];
}

export function getSupportStatusBadgeVariant(status: CaseRequirementSupportStatus): OutcomeBadgeVariant {
  return SUPPORT_STATUS_BADGE_VARIANT[status];
}

const NECESSITY_LABEL: Record<EvidenceNecessity, string> = {
  REQUIRED: "Required for this pathway",
  SUPPORTING: "Supports an optional requirement",
};

export function getNecessityLabel(necessity: EvidenceNecessity): string {
  return NECESSITY_LABEL[necessity];
}

const SEVERITY_LABEL: Record<CaseIssueSeverity, string> = {
  CRITICAL: "Critical",
  HIGH: "High",
  MEDIUM: "Medium",
};

const SEVERITY_BADGE_VARIANT: Record<CaseIssueSeverity, OutcomeBadgeVariant> = {
  CRITICAL: "danger",
  HIGH: "warning",
  MEDIUM: "neutral",
};

export function getSeverityLabel(severity: CaseIssueSeverity): string {
  return SEVERITY_LABEL[severity];
}

export function getSeverityBadgeVariant(severity: CaseIssueSeverity): OutcomeBadgeVariant {
  return SEVERITY_BADGE_VARIANT[severity];
}

const RISK_BAND_LABEL: Record<CaseRiskBand, string> = {
  NORMAL: "Normal",
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  CRITICAL: "Critical",
};

const RISK_BAND_BADGE_VARIANT: Record<CaseRiskBand, OutcomeBadgeVariant> = {
  NORMAL: "success",
  LOW: "neutral",
  MEDIUM: "warning",
  HIGH: "warning",
  CRITICAL: "danger",
};

export function getRiskBandLabel(band: CaseRiskBand): string {
  return RISK_BAND_LABEL[band];
}

export function getRiskBandBadgeVariant(band: CaseRiskBand): OutcomeBadgeVariant {
  return RISK_BAND_BADGE_VARIANT[band];
}
