import API from "./axios";

import type { Fact, FactConflict } from "../types/fact";

/**
 * ============================================================================
 * FACT API CLIENT
 * ============================================================================
 *
 * Centralized HTTP client for the Fact Foundation's minimum read surface
 * consumed by the frontend.
 *
 * Authentication:
 * ----------------------------------------------------------------------------
 * Authentication is handled centrally by ./axios.
 *
 * Endpoints used here:
 * ----------------------------------------------------------------------------
 *
 *   GET  /api/facts/{factId}
 *   GET  /api/facts/conflicts/{conflictId}
 *   POST /api/facts/conflicts/{conflictId}/applicant-confirmation
 *
 * None of these endpoints takes a subjectUserId of any kind - the backend
 * resolves the subject from the fetched Fact/FactConflict record itself and
 * checks the caller against it. There is no parameter here a caller could
 * tamper with to reach another user's data.
 *
 * NOT included here: PATCH /api/facts/{factId}/verify and
 * PATCH /api/facts/conflicts/{conflictId}/resolve. Both are gated by
 * FactAuthorizationService.assertCanVerifyOrResolve, which deliberately
 * excludes the SUBJECT accessor type - only an assigned CASE_WORKER may
 * call them.
 *
 * confirmFactConflictApi below is a DIFFERENT, subject-only operation
 * gated by the separate FactAuthorizationService.
 * assertCanApplicantConfirmConflict grant - the applicant's own self-report
 * of which value they believe is correct, never verification and never a
 * substitute for case-worker resolution (see the Fact Conflict detail
 * page's documentation for the full reasoning).
 * ============================================================================
 */

const FACT_API_BASE_PATH = "/facts";

/**
 * ============================================================================
 * RETRIEVE ONE FACT
 * ============================================================================
 */
export async function getFactApi(factId: number): Promise<Fact> {

  const response = await API.get<Fact>(
    `${FACT_API_BASE_PATH}/${factId}`,
  );

  return response.data;
}

/**
 * ============================================================================
 * RETRIEVE ONE FACT CONFLICT
 * ============================================================================
 */
export async function getFactConflictApi(conflictId: number): Promise<FactConflict> {

  const response = await API.get<FactConflict>(
    `${FACT_API_BASE_PATH}/conflicts/${conflictId}`,
  );

  return response.data;
}

/**
 * ============================================================================
 * APPLICANT CONFIRMATION OF THEIR OWN OPEN CONFLICT
 * ============================================================================
 *
 * POST /api/facts/conflicts/{conflictId}/applicant-confirmation
 *
 * `winningFactId` must be one of the two facts already present on the
 * fetched FactConflict (factAId/factBId) - this client does not, and must
 * not, accept an arbitrary fact ID or a subjectUserId from the caller. The
 * backend re-derives the subject from the persisted conflict record and
 * authorizes the caller against it (subject only); this request body has
 * no field that could name a different subject to act on.
 *
 * This is self-reported provenance, never independent documentary
 * verification and never interchangeable with the case-worker-only
 * resolveConflict operation - see ConflictResolutionType.APPLICANT_CONFIRMATION.
 * ============================================================================
 */
export interface ConfirmFactConflictPayload {
  winningFactId: number;
  notes?: string;
}

export async function confirmFactConflictApi(
  conflictId: number,
  payload: ConfirmFactConflictPayload,
): Promise<FactConflict> {

  const response = await API.post<FactConflict>(
    `${FACT_API_BASE_PATH}/conflicts/${conflictId}/applicant-confirmation`,
    payload,
  );

  return response.data;
}
