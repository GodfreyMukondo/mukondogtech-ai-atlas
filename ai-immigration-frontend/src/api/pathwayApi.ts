import API from "./axios";

import type {
  Pathway,
  PathwayAssessment,
} from "../types/pathwayAssessment";

import type { PathwayExplanation } from "../types/pathwayExplanation";
import type { PathwayDiscoveryResponse } from "../types/pathwayDiscovery";

/**
 * ============================================================================
 * PATHWAY / PATHWAY ASSESSMENT API CLIENT
 * ============================================================================
 *
 * Centralized HTTP client for the Requirement/Pathway backend (Phase 1).
 *
 * Authentication:
 * ----------------------------------------------------------------------------
 * Authentication is handled centrally by ./axios.
 *
 * Endpoints:
 * ----------------------------------------------------------------------------
 *
 *   GET  /api/pathways                                  list published pathways (catalogue)
 *   GET  /api/pathways/discovery                        read-only, ranked alignment across every published pathway (Phase 4)
 *   POST /api/pathways/{pathwayId}/assessments           request a personalized assessment
 *   GET  /api/pathways/assessments/{assessmentId}        retrieve a stored assessment
 *   GET  /api/pathways/assessments/{assessmentId}/explanation
 *                                                         full explainability chain
 *
 * Personalization:
 * ----------------------------------------------------------------------------
 * Unlike /api/applications, this backend requires an explicit
 * `subjectUserId` - it does not implicitly resolve "the subject" from the
 * JWT, because a CASE_WORKER may request an assessment for an assigned
 * applicant. Callers on this page always pass the authenticated user's own
 * ID (see RequestPathwayAssessmentPage) - this client does not decide who
 * the subject is.
 * ============================================================================
 */

const PATHWAY_API_BASE_PATH = "/pathways";

/**
 * ============================================================================
 * LIST PUBLISHED PATHWAYS
 * ============================================================================
 */
export async function listPathwaysApi(): Promise<Pathway[]> {

  const response = await API.get<Pathway[]>(
    PATHWAY_API_BASE_PATH,
  );

  return response.data;
}

/**
 * ============================================================================
 * DISCOVER RANKED PATHWAYS (Phase 4 - read-only, transient, never persisted)
 * ============================================================================
 *
 * Unlike requestPathwayAssessmentApi, this never creates a stored
 * PathwayAssessment - it is safe to call every time this page is opened.
 */
export async function discoverPathwaysApi(
  subjectUserId: number,
): Promise<PathwayDiscoveryResponse> {

  const response = await API.get<PathwayDiscoveryResponse>(
    `${PATHWAY_API_BASE_PATH}/discovery`,
    {
      params: {
        subjectUserId,
      },
    },
  );

  return response.data;
}

/**
 * ============================================================================
 * REQUEST A PERSONALIZED ASSESSMENT
 * ============================================================================
 */
export async function requestPathwayAssessmentApi(
  pathwayId: number,
  subjectUserId: number,
): Promise<PathwayAssessment> {

  const response = await API.post<PathwayAssessment>(
    `${PATHWAY_API_BASE_PATH}/${pathwayId}/assessments`,
    null,
    {
      params: {
        subjectUserId,
      },
    },
  );

  return response.data;
}

/**
 * ============================================================================
 * RETRIEVE A STORED ASSESSMENT
 * ============================================================================
 */
export async function getPathwayAssessmentApi(
  assessmentId: number,
): Promise<PathwayAssessment> {

  const response = await API.get<PathwayAssessment>(
    `${PATHWAY_API_BASE_PATH}/assessments/${assessmentId}`,
  );

  return response.data;
}

/**
 * ============================================================================
 * RETRIEVE THE FULL EXPLAINABILITY CHAIN FOR A STORED ASSESSMENT
 * ============================================================================
 *
 * PATHWAY -> REQUIREMENT -> REQUIREMENT EVALUATION -> FACT -> EVIDENCE
 *         -> REGULATORY VERSION
 *
 * Uses the SAME assessmentId as getPathwayAssessmentApi - never a second,
 * separately-issued identifier.
 */
export async function getPathwayExplanationApi(
  assessmentId: number,
): Promise<PathwayExplanation> {

  const response = await API.get<PathwayExplanation>(
    `${PATHWAY_API_BASE_PATH}/assessments/${assessmentId}/explanation`,
  );

  return response.data;
}
