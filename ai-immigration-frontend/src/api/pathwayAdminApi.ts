import API from "./axios";

import type { Pathway } from "../types/pathwayAssessment";

import type {
  AdminRequirement,
  AdminRequirementDetail,
  FactTypeSummary,
  PathwayAdminDetail,
  PathwayCreateRequest,
  PathwayStatusChangeRequest,
  PathwayUpdateRequest,
  RegulatoryVersion,
  RegulatoryVersionCreateRequest,
  RequirementCreateRequest,
  RequirementStatusChangeRequest,
  RequirementUpdateRequest,
} from "../types/pathwayAdmin";

/**
 * ============================================================================
 * PATHWAY / REQUIREMENT ADMIN API CLIENT
 * ============================================================================
 *
 * Centralized HTTP client for the Phase 1 catalogue authoring surface.
 *
 * Authentication:
 * ----------------------------------------------------------------------------
 * Authentication is handled centrally by ./axios. Every endpoint here also
 * requires ROLE_ADMIN server-side (enforced by the existing
 * SecurityConfig `/api/admin/**` rule) - a non-admin caller receives 403
 * regardless of what this client sends.
 *
 * Endpoints:
 * ----------------------------------------------------------------------------
 *
 *   POST  /api/admin/pathways
 *   GET   /api/admin/pathways
 *   GET   /api/admin/pathways/{id}
 *   PATCH /api/admin/pathways/{id}
 *   PATCH /api/admin/pathways/{id}/status
 *
 *   POST  /api/admin/requirements
 *   GET   /api/admin/requirements
 *   GET   /api/admin/requirements/{id}
 *   PATCH /api/admin/requirements/{id}
 *   PATCH /api/admin/requirements/{id}/status
 *
 *   POST  /api/admin/regulatory-versions
 *   GET   /api/admin/regulatory-versions
 *
 *   GET   /api/admin/fact-types
 * ============================================================================
 */

const PATHWAYS_BASE = "/admin/pathways";
const REQUIREMENTS_BASE = "/admin/requirements";
const REGULATORY_VERSIONS_BASE = "/admin/regulatory-versions";
const FACT_TYPES_BASE = "/admin/fact-types";

// =========================================================================
// PATHWAYS
// =========================================================================

export async function listAdminPathwaysApi(): Promise<Pathway[]> {
  const response = await API.get<Pathway[]>(PATHWAYS_BASE);
  return response.data;
}

export async function getAdminPathwayApi(pathwayId: number): Promise<PathwayAdminDetail> {
  const response = await API.get<PathwayAdminDetail>(`${PATHWAYS_BASE}/${pathwayId}`);
  return response.data;
}

export async function createPathwayApi(request: PathwayCreateRequest): Promise<Pathway> {
  const response = await API.post<Pathway>(PATHWAYS_BASE, request);
  return response.data;
}

export async function updatePathwayApi(pathwayId: number, request: PathwayUpdateRequest): Promise<Pathway> {
  const response = await API.patch<Pathway>(`${PATHWAYS_BASE}/${pathwayId}`, request);
  return response.data;
}

export async function changePathwayStatusApi(
  pathwayId: number,
  request: PathwayStatusChangeRequest,
): Promise<Pathway> {
  const response = await API.patch<Pathway>(`${PATHWAYS_BASE}/${pathwayId}/status`, request);
  return response.data;
}

// =========================================================================
// REQUIREMENTS
// =========================================================================

export async function listAdminRequirementsApi(): Promise<AdminRequirement[]> {
  const response = await API.get<AdminRequirement[]>(REQUIREMENTS_BASE);
  return response.data;
}

export async function getAdminRequirementApi(requirementId: number): Promise<AdminRequirementDetail> {
  const response = await API.get<AdminRequirementDetail>(`${REQUIREMENTS_BASE}/${requirementId}`);
  return response.data;
}

export async function createRequirementApi(request: RequirementCreateRequest): Promise<AdminRequirementDetail> {
  const response = await API.post<AdminRequirementDetail>(REQUIREMENTS_BASE, request);
  return response.data;
}

export async function updateRequirementApi(
  requirementId: number,
  request: RequirementUpdateRequest,
): Promise<AdminRequirementDetail> {
  const response = await API.patch<AdminRequirementDetail>(`${REQUIREMENTS_BASE}/${requirementId}`, request);
  return response.data;
}

export async function changeRequirementStatusApi(
  requirementId: number,
  request: RequirementStatusChangeRequest,
): Promise<AdminRequirement> {
  const response = await API.patch<AdminRequirement>(`${REQUIREMENTS_BASE}/${requirementId}/status`, request);
  return response.data;
}

// =========================================================================
// REGULATORY VERSIONS
// =========================================================================

export async function listRegulatoryVersionsApi(): Promise<RegulatoryVersion[]> {
  const response = await API.get<RegulatoryVersion[]>(REGULATORY_VERSIONS_BASE);
  return response.data;
}

export async function createRegulatoryVersionApi(
  request: RegulatoryVersionCreateRequest,
): Promise<RegulatoryVersion> {
  const response = await API.post<RegulatoryVersion>(REGULATORY_VERSIONS_BASE, request);
  return response.data;
}

// =========================================================================
// FACT TYPES (for the requirement fact-key picker)
// =========================================================================

export async function listKnownFactTypesApi(): Promise<FactTypeSummary[]> {
  const response = await API.get<FactTypeSummary[]>(FACT_TYPES_BASE);
  return response.data;
}
