import type { Pathway, PathwayStatus } from "./pathwayAssessment";
import type { FactCategory, FactProvenanceType, FactValueType } from "./fact";

/**
 * ============================================================================
 * PATHWAY / REQUIREMENT ADMIN TYPES
 * ============================================================================
 *
 * Mirrors the backend's admin authoring DTOs exactly
 * (POST/PATCH /api/admin/pathways, /api/admin/requirements,
 * /api/admin/regulatory-versions). Reuses {@link Pathway}/{@link PathwayStatus}
 * from ./pathwayAssessment rather than redefining the catalogue shape - the
 * admin surface reads/writes the exact same Pathway/Requirement rows the
 * applicant-facing pages already consume.
 * ============================================================================
 */

export type { Pathway, PathwayStatus };

/**
 * Admin-only detail projection (GET /api/admin/pathways/{id}) - the only
 * shape that includes `requirementIds`, derived server-side by parsing the
 * pathway's own `compositionLogic` (never a second, separately-maintained
 * selection list). This is what lets the edit form pre-select a pathway's
 * existing requirements.
 */
export interface PathwayAdminDetail extends Pathway {
  requirementIds: number[];
}

export type RequirementStatus = "DRAFT" | "PUBLISHED" | "DEPRECATED" | "RETRACTED";

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

export type RegulatorySourceType =
  | "AUTHORITATIVE_REGULATORY_SOURCE"
  | "SECONDARY_INFORMATION"
  | "AI_GENERATED_INFORMATION";

export type RegulatoryVerificationStatus =
  | "UNVERIFIED_INGESTION"
  | "HUMAN_VERIFIED"
  | "AUTHORITATIVE_CONFIRMED";

/**
 * ============================================================================
 * REGULATORY VERSION
 * ============================================================================
 */
export interface RegulatoryVersion {
  id: number;
  regulationIdentity: string;
  jurisdiction: string;
  sourceType: RegulatorySourceType;
  sourceAuthority: string;
  sourceReference?: string | null;
  publicationDate?: string | null;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  supersedesVersionId?: number | null;
  supersededByVersionId?: number | null;
  verificationStatus: RegulatoryVerificationStatus;
  changeSummary?: string | null;
}

export interface RegulatoryVersionCreateRequest {
  regulationIdentity: string;
  jurisdiction: string;
  sourceType: RegulatorySourceType;
  sourceAuthority: string;
  sourceReference?: string;
  publicationDate?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  verificationStatus: RegulatoryVerificationStatus;
  changeSummary?: string;
}

/**
 * ============================================================================
 * FACT TYPE (for the requirement-authoring fact-key picker)
 * ============================================================================
 */
export interface FactTypeSummary {
  factKey: string;
  category: FactCategory;
  valueType: FactValueType;
}

/**
 * ============================================================================
 * REQUIREMENT (admin - full catalogue projection, any status)
 * ============================================================================
 */
export interface AdminRequirement {
  id: number;
  requirementKey: string;
  requirementType: RequirementType;
  title: string;
  description?: string | null;
  jurisdiction: string;
  immigrationContext?: string | null;
  regulatoryVersionId: number;
  mandatory: boolean;
  status: RequirementStatus;
}

/**
 * Admin-only detail projection (create/update/get-by-id responses) - the
 * only shape that includes the raw logic JSON and fact bindings, since
 * that detail is unnecessary (and intentionally omitted) from the
 * lightweight {@link AdminRequirement} list projection.
 */
export interface AdminRequirementDetail extends AdminRequirement {
  applicabilityLogicJson?: string | null;
  satisfactionLogicJson: string;
  factBindings: RequirementFactBindingInput[];
}

export interface RequirementFactBindingInput {
  factKey: string;
  requiresVerification: boolean;
  minimumProvenanceType?: FactProvenanceType | null;
}

export interface RequirementCreateRequest {
  requirementKey: string;
  requirementType: RequirementType;
  title: string;
  description?: string;
  jurisdiction: string;
  immigrationContext?: string;
  regulatoryVersionId: number;
  mandatory: boolean;
  applicabilityLogicJson?: string;
  satisfactionLogicJson: string;
  factBindings: RequirementFactBindingInput[];
}

export type RequirementUpdateRequest = Omit<RequirementCreateRequest, "requirementKey">;

export interface RequirementStatusChangeRequest {
  targetStatus: RequirementStatus;
  notes?: string;
}

/**
 * ============================================================================
 * PATHWAY (admin - create/update/status-change)
 * ============================================================================
 */
export interface PathwayCreateRequest {
  pathwayKey: string;
  name: string;
  description?: string;
  jurisdiction: string;
  category: string;
  requirementIds: number[];
  evidenceExpectations?: string;
  validFrom?: string;
  validTo?: string;
}

export type PathwayUpdateRequest = Omit<PathwayCreateRequest, "pathwayKey">;

export interface PathwayStatusChangeRequest {
  targetStatus: PathwayStatus;
  notes?: string;
}
