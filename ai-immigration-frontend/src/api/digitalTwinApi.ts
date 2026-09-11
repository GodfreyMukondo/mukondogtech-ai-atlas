import API from "./axios";

import type { DigitalTwin } from "../types/digitalTwin";

/**
 * ============================================================================
 * DIGITAL TWIN API CLIENT
 * ============================================================================
 *
 * Centralized HTTP client for the Digital Twin read projection.
 *
 * Authentication:
 * ----------------------------------------------------------------------------
 * Authentication is handled centrally by ./axios.
 *
 * Endpoint:
 * ----------------------------------------------------------------------------
 *
 *   GET /api/twin/{subjectUserId}
 *
 * Personalization:
 * ----------------------------------------------------------------------------
 * Like /api/pathways/{id}/assessments, this backend requires an explicit
 * `subjectUserId` path segment rather than implicitly resolving "the
 * subject" from the JWT - the backend independently re-verifies the caller
 * is authorized to view that subject (self, or an assigned CASE_WORKER)
 * before returning anything; a stranger's ID here is rejected server-side
 * regardless of what the frontend sends. Callers on this page always pass
 * the authenticated user's own ID (see DigitalTwinPage) - this client does
 * not decide who the subject is.
 *
 * This is a READ projection only - there is no corresponding write/mutate
 * endpoint, and none is added here.
 * ============================================================================
 */

const DIGITAL_TWIN_API_BASE_PATH = "/twin";

/**
 * ============================================================================
 * RETRIEVE THE DIGITAL TWIN FOR ONE SUBJECT
 * ============================================================================
 */
export async function getDigitalTwinApi(
  subjectUserId: number,
): Promise<DigitalTwin> {

  const response = await API.get<DigitalTwin>(
    `${DIGITAL_TWIN_API_BASE_PATH}/${subjectUserId}`,
  );

  return response.data;
}
