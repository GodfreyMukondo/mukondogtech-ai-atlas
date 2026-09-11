import type {
  EvidenceSourceType,
  FactProvenanceType,
  RegulatoryVerificationStatus,
} from "../types/pathwayExplanation";

/**
 * ============================================================================
 * EXPLANATION LABELS
 * ============================================================================
 *
 * Display copy for the Explainability view's provenance/source vocabulary.
 * Kept separate from ./assessmentOutcome, which owns the outcome/certainty
 * vocabulary shared with the results page.
 *
 * Deliberate wording:
 *
 * - Provenance labels describe HOW a Fact originated (self-reported,
 *   extracted, verified, authoritative) - never a confidence/quality score.
 * - "isVerified" is rendered as its own badge, entirely separate from
 *   confidence level - a high-confidence extraction is not a verified Fact.
 * - Regulatory verification status describes trust in the RULE, never in
 *   the applicant's Facts.
 * ============================================================================
 */

const PROVENANCE_LABEL: Record<FactProvenanceType, string> = {
  USER_INPUT: "Self-reported",
  DOCUMENT_EXTRACTION: "Extracted from a document",
  HUMAN_VERIFICATION: "Confirmed by human review",
  EXTERNAL_AUTHORITATIVE_SOURCE: "From an authoritative external source",
  DERIVED_FACT: "Derived from other facts",
  SYSTEM_PROCESS: "System-generated",
  SIMULATION: "Simulated (hypothetical)",
  ASSUMPTION: "Assumed, pending confirmation",
};

export function getProvenanceLabel(provenanceType: FactProvenanceType): string {
  return PROVENANCE_LABEL[provenanceType];
}

const EVIDENCE_SOURCE_LABEL: Record<EvidenceSourceType, string> = {
  DOCUMENT: "Document",
  USER_STATEMENT: "User statement",
  ADMIN_ATTESTATION: "Case worker attestation",
  EXTERNAL_SOURCE: "External source",
  DERIVATION: "Derived",
};

export function getEvidenceSourceLabel(sourceType: EvidenceSourceType): string {
  return EVIDENCE_SOURCE_LABEL[sourceType];
}

const REGULATORY_VERIFICATION_LABEL: Record<RegulatoryVerificationStatus, string> = {
  UNVERIFIED_INGESTION: "Not yet verified against the source",
  HUMAN_VERIFIED: "Verified by a human reviewer",
  AUTHORITATIVE_CONFIRMED: "Confirmed against the official source",
};

export function getRegulatoryVerificationLabel(
  status: RegulatoryVerificationStatus,
): string {
  return REGULATORY_VERIFICATION_LABEL[status];
}
