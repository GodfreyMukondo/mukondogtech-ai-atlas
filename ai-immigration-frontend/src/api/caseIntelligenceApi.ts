import API from "./axios";

import type {
  CaseContradictions,
  CaseIntelligence,
  CaseSignals,
  CaseTimeline,
} from "../types/caseIntelligence";

/**
 * ============================================================================
 * CASE INTELLIGENCE API CLIENT
 * ============================================================================
 *
 * Centralized HTTP client for the Phase 2 Case Intelligence / Discovery /
 * Readiness foundation.
 *
 * Authentication is handled centrally by ./axios; authorization is
 * re-enforced server-side against the Fact authorization boundary on every
 * call, exactly like /api/twin and /api/pathways.
 *
 * Endpoints:
 * ----------------------------------------------------------------------------
 *   GET /api/cases/pathway-assessments/{assessmentId}/intelligence
 *   GET /api/cases/{subjectUserId}/contradictions
 *   GET /api/cases/{subjectUserId}/timeline
 *   GET /api/cases/{subjectUserId}/signals
 * ============================================================================
 */

const CASES_API_BASE_PATH = "/cases";

export async function getCaseIntelligenceApi(assessmentId: number): Promise<CaseIntelligence> {

  const response = await API.get<CaseIntelligence>(
    `${CASES_API_BASE_PATH}/pathway-assessments/${assessmentId}/intelligence`,
  );

  return response.data;
}

export async function getCaseContradictionsApi(subjectUserId: number): Promise<CaseContradictions> {

  const response = await API.get<CaseContradictions>(
    `${CASES_API_BASE_PATH}/${subjectUserId}/contradictions`,
  );

  return response.data;
}

export async function getCaseTimelineApi(subjectUserId: number): Promise<CaseTimeline> {

  const response = await API.get<CaseTimeline>(
    `${CASES_API_BASE_PATH}/${subjectUserId}/timeline`,
  );

  return response.data;
}

export async function getCaseSignalsApi(subjectUserId: number): Promise<CaseSignals> {

  const response = await API.get<CaseSignals>(
    `${CASES_API_BASE_PATH}/${subjectUserId}/signals`,
  );

  return response.data;
}
