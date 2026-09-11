import API from "./axios";

import type {
  AgentRunResponse,
  CaseOverviewAgentRunResponse,
  ExplainCaseOverviewRequest,
  ExplainPathwayAssessmentRequest,
  ExplainPathwayDiscoveryRequest,
  ExplainRequirementRequest,
  PathwayAssessmentAgentRunResponse,
  PathwayDiscoveryAgentRunResponse,
} from "../types/agent";

/**
 * ============================================================================
 * AGENT API CLIENT (Phase 5.1 - EXPLAIN_REQUIREMENT, Phase 5.2 - EXPLAIN_PATHWAY_ASSESSMENT,
 * Phase 5.3 - EXPLAIN_PATHWAY_DISCOVERY)
 * ============================================================================
 *
 * Centralized HTTP client for the agent's API surface.
 * Authentication is handled centrally by ./axios.
 *
 * Endpoints used here:
 * ----------------------------------------------------------------------------
 *
 *   POST /api/agent/runs                  run the EXPLAIN_REQUIREMENT agent
 *   GET  /api/agent/runs/{id}             re-fetch a past run
 *   POST /api/agent/pathway-runs          run the EXPLAIN_PATHWAY_ASSESSMENT agent (Phase 5.2)
 *   GET  /api/agent/pathway-runs/{id}     re-fetch a past pathway-assessment run
 *   POST /api/agent/discovery-runs        run the EXPLAIN_PATHWAY_DISCOVERY agent (Phase 5.3)
 *   GET  /api/agent/discovery-runs/{id}   re-fetch a past discovery run
 *   POST /api/agent/case-overview-runs        run the EXPLAIN_CASE_OVERVIEW agent (Phase 5.4)
 *   GET  /api/agent/case-overview-runs/{id}   re-fetch a past case-overview run
 *
 * Every request/response pair below travels through the centrally-configured
 * `API` (axios) client, which attaches the authenticated user's JWT - the
 * backend always derives and re-verifies the authorized subject itself
 * (never trusting a client-supplied id as an authority), exactly like every
 * other API client in this app.
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

/**
 * ============================================================================
 * RUN THE EXPLAIN_PATHWAY_ASSESSMENT AGENT (Phase 5.2)
 * ============================================================================
 */
export async function explainPathwayAssessmentApi(
  request: ExplainPathwayAssessmentRequest,
): Promise<PathwayAssessmentAgentRunResponse> {

  const response = await API.post<PathwayAssessmentAgentRunResponse>(
    `${AGENT_API_BASE_PATH}/pathway-runs`,
    request,
  );

  return response.data;
}

/**
 * ============================================================================
 * RE-FETCH A PAST PATHWAY-ASSESSMENT AGENT RUN (Phase 5.2)
 * ============================================================================
 */
export async function getPathwayAssessmentAgentRunApi(runId: number): Promise<PathwayAssessmentAgentRunResponse> {

  const response = await API.get<PathwayAssessmentAgentRunResponse>(
    `${AGENT_API_BASE_PATH}/pathway-runs/${runId}`,
  );

  return response.data;
}

/**
 * ============================================================================
 * RUN THE EXPLAIN_PATHWAY_DISCOVERY AGENT (Phase 5.3)
 * ============================================================================
 */
export async function explainPathwayDiscoveryApi(
  request: ExplainPathwayDiscoveryRequest,
): Promise<PathwayDiscoveryAgentRunResponse> {

  const response = await API.post<PathwayDiscoveryAgentRunResponse>(
    `${AGENT_API_BASE_PATH}/discovery-runs`,
    request,
  );

  return response.data;
}

/**
 * ============================================================================
 * RE-FETCH A PAST PATHWAY-DISCOVERY AGENT RUN (Phase 5.3)
 * ============================================================================
 */
export async function getPathwayDiscoveryAgentRunApi(runId: number): Promise<PathwayDiscoveryAgentRunResponse> {

  const response = await API.get<PathwayDiscoveryAgentRunResponse>(
    `${AGENT_API_BASE_PATH}/discovery-runs/${runId}`,
  );

  return response.data;
}

/**
 * ============================================================================
 * RUN THE EXPLAIN_CASE_OVERVIEW AGENT (Phase 5.4)
 * ============================================================================
 */
export async function explainCaseOverviewApi(
  request: ExplainCaseOverviewRequest,
): Promise<CaseOverviewAgentRunResponse> {

  const response = await API.post<CaseOverviewAgentRunResponse>(
    `${AGENT_API_BASE_PATH}/case-overview-runs`,
    request,
  );

  return response.data;
}

/**
 * ============================================================================
 * RE-FETCH A PAST CASE-OVERVIEW AGENT RUN (Phase 5.4)
 * ============================================================================
 */
export async function getCaseOverviewAgentRunApi(runId: number): Promise<CaseOverviewAgentRunResponse> {

  const response = await API.get<CaseOverviewAgentRunResponse>(
    `${AGENT_API_BASE_PATH}/case-overview-runs/${runId}`,
  );

  return response.data;
}
