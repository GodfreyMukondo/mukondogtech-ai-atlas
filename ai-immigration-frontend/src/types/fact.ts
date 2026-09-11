/**
 * ============================================================================
 * FACT TYPES (shared vocabulary)
 * ============================================================================
 *
 * The canonical Fact vocabulary shared by every screen that surfaces Fact
 * data - the Digital Twin/Profile view (GET /api/twin/{subjectUserId}) and
 * the Pathway Explainability view (GET /api/pathways/assessments/{id}/
 * explanation). Defined ONCE here so those two features can never drift
 * into two different ideas of what a Fact category/provenance/confidence
 * level is.
 *
 * These are shapes only - this file holds no data and is not itself a
 * store. Every screen that imports these types still reads live from the
 * backend on each request; nothing here caches or persists a Fact.
 * ============================================================================
 */

export type FactCategory =
  | "IDENTITY"
  | "NATIONALITY_CITIZENSHIP"
  | "RESIDENCE"
  | "TRAVEL_HISTORY"
  | "IMMIGRATION_STATUS"
  | "IMMIGRATION_HISTORY"
  | "EDUCATION"
  | "EMPLOYMENT"
  | "FINANCES"
  | "LANGUAGE_PROFICIENCY"
  | "FAMILY_DEPENDANTS"
  | "RELATIONSHIPS"
  | "PROFESSIONAL_CREDENTIALS"
  | "LEGAL_ADMINISTRATIVE_HISTORY"
  | "HEALTH_IMMIGRATION_RELEVANT"
  | "REGULATORY_CONTEXT"
  | "CASE_PROCESS_INTERACTION";

/**
 * How a Fact originated. Kept distinct from confidence/verification -
 * "extracted from a document" and "verified" are two different axes. An
 * AI extraction is never presented as a verified Fact merely because it
 * exists.
 */
export type FactProvenanceType =
  | "USER_INPUT"
  | "DOCUMENT_EXTRACTION"
  | "HUMAN_VERIFICATION"
  | "EXTERNAL_AUTHORITATIVE_SOURCE"
  | "DERIVED_FACT"
  | "SYSTEM_PROCESS"
  | "SIMULATION"
  | "ASSUMPTION";

/** A qualitative summary of confidence - never a stand-in for verification status. */
export type FactConfidenceLevel =
  | "LOW"
  | "MODERATE"
  | "HIGH"
  | "NOT_APPLICABLE";

export type EvidenceSourceType =
  | "DOCUMENT"
  | "USER_STATEMENT"
  | "ADMIN_ATTESTATION"
  | "EXTERNAL_SOURCE"
  | "DERIVATION";

export type FactValueType =
  | "STRING"
  | "DATE"
  | "NUMBER"
  | "BOOLEAN";

/**
 * Fact lifecycle status. VERIFIED is deliberately not a member - it is an
 * overlay (isVerified/verifiedAt/verificationMethod), not a status, so a
 * Fact can be simultaneously ACCEPTED and verified.
 */
export type FactStatus =
  | "PROPOSED"
  | "VALIDATION_FAILED"
  | "SUSPICIOUS"
  | "PENDING_REVIEW"
  | "CONTESTED"
  | "ACCEPTED"
  | "REJECTED"
  | "SUPERSEDED"
  | "RETRACTED";

export type FactSensitivityTier =
  | "T1_ROUTINE"
  | "T2_STANDARD_PERSONAL"
  | "T3_SENSITIVE"
  | "T4_HIGHLY_SENSITIVE";

export type VerificationMethod =
  | "INDEPENDENT_EVIDENCE_MATCH"
  | "HUMAN_REVIEW"
  | "AUTHORITATIVE_CROSS_CHECK";

export type ConflictStatus = "OPEN" | "RESOLVED";

/**
 * How a FactConflict was resolved - never a fraud label, only a resolution
 * mechanism.
 *
 * APPLICANT_CONFIRMATION and HUMAN_DECISION are deliberately distinct and
 * never interchangeable in the UI: HUMAN_DECISION is a case worker's own
 * adjudication (PATCH /conflicts/{id}/resolve); APPLICANT_CONFIRMATION is
 * the subject's own self-reported statement of which value they believe is
 * correct (POST /conflicts/{id}/applicant-confirmation). Neither one sets a
 * Fact's independent verification overlay (isVerified) - that is a fully
 * separate axis, see Fact.isVerified.
 */
export type ConflictResolutionType =
  | "EVIDENCE_WEIGHTED_AUTO"
  | "TEMPORAL_AUTO"
  | "SAFE_AUTO"
  | "HUMAN_DECISION"
  | "APPLICANT_CONFIRMATION";

/**
 * ============================================================================
 * EVIDENCE (Document -> Evidence link for one Fact)
 * ============================================================================
 */
export interface FactEvidence {
  id: number;
  sourceType: EvidenceSourceType;
  documentId?: number | null;
  sourceLocator?: string | null;
  sourceSnippet?: string | null;
  capturedAt: string;
}

/**
 * ============================================================================
 * FACT (full projection, as returned by the Digital Twin / Fact endpoints)
 * ============================================================================
 */
export interface Fact {
  id: number;
  subjectUserId: number;
  category: FactCategory;
  factKey: string;
  valueType: FactValueType;
  stringValue?: string | null;
  dateValue?: string | null;
  numberValue?: number | null;
  booleanValue?: boolean | null;
  status: FactStatus;
  provenanceType: FactProvenanceType;
  sensitivityTier: FactSensitivityTier;
  confidenceScore?: number | null;
  confidenceLevel: FactConfidenceLevel;
  confidenceExplanation?: string | null;
  isVerified: boolean;
  verifiedAt?: string | null;
  verificationMethod?: VerificationMethod | null;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  observedAt: string;
  lastObservedAt: string;
  recordedAt: string;
  supersedesFactId?: number | null;
  evidence: FactEvidence[];
}

/**
 * ============================================================================
 * FACT CONFLICT
 * ============================================================================
 *
 * A detected conflict between two Facts of the same key. This is a
 * CONFLICT DETECTED record only - it is never a fraud determination, and
 * this type carries no field that could be mistaken for one.
 */
export interface FactConflict {
  id: number;
  subjectUserId: number;
  factKey: string;
  factAId: number;
  factBId: number;
  status: ConflictStatus;
  detectedAt: string;
  resolvedAt?: string | null;
  resolutionType?: ConflictResolutionType | null;
  winningFactId?: number | null;
  resolutionNotes?: string | null;
}
