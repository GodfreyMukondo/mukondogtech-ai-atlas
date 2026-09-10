import API from "./axios";

import type { AgentRunResponse, ExplainRequirementRequest } from "../types/agent";

/**
 * ============================================================================
 * AGENT API CLIENT (Phase 5.1 - EXPLAIN_REQUIREMENT)
 * ============================================================================
 *
 * Centralized HTTP client for the Phase 5.1 agent's API surface.
 * Authentication is handled centrally by ./axios.
 *
 * Endpoints used here:
 * ----------------------------------------------------------------------------
 *
 *   POST /api/agent/runs          run the EXPLAIN_REQUIREMENT agent
 *   GET  /api/agent/runs/{id}     re-fetch a past run
 *
 * Neither endpoint takes a subjectUserId of any kind - the backend resolves
 * the subject from the PathwayAssessment/AgentRun record itself and
 * authorizes against it, exactly like every other API client in this app.
 * ============================================================================
 */

const AGENT_API_BASE_PATH = "/agent";

/**
 * ============================================================================
 * RUN THE EXPLAIN_REQUIREMENT AGENT
 * ============================================================================
 */
export async function explainRequirementApi(
  request: ExplainRequirementRequest,
): Promise<AgentRunResponse> {

  const response = await API.post<AgentRunResponse>(
    `${AGENT_API_BASE_PATH}/runs`,
    request,
  );

  return response.data;
}

/**
 * ============================================================================
 * RE-FETCH A PAST AGENT RUN
 * ============================================================================
 */
export async function getAgentRunApi(runId: number): Promise<AgentRunResponse> {

  const response = await API.get<AgentRunResponse>(
    `${AGENT_API_BASE_PATH}/runs/${runId}`,
  );

  return response.data;
}
