import API from "./axios";

import type { EvidenceFullTrace, EvidenceGraph, EvidenceItem } from "../types/evidenceGraph";

/**
 * ============================================================================
 * EVIDENCE GRAPH API CLIENT
 * ============================================================================
 *
 * Centralized HTTP client for the Evidence Intelligence Graph's read-only
 * API surface.
 *
 * Authentication:
 * ----------------------------------------------------------------------------
 * Authentication is handled centrally by ./axios.
 *
 * Endpoints used here:
 * ----------------------------------------------------------------------------
 *
 *   GET /api/evidence-graph/facts/{factId}
 *   GET /api/evidence-graph/evidence-items/{evidenceItemId}
 *   GET /api/evidence-graph/pathway-assessments/{assessmentId}/full-trace
 *   GET /api/evidence-graph/documents/{documentId}
 *   GET /api/evidence-graph/requirement-evaluations/{evaluationId}
 *
 * None of these endpoints takes a subjectUserId of any kind - the backend
 * resolves the subject from the fetched Fact/PathwayAssessment record
 * itself and authorizes against it (self, or an assigned case worker).
 * There is no parameter here a caller could tamper with to reach another
 * subject's evidence graph.
 * ============================================================================
 */

const EVIDENCE_GRAPH_API_BASE_PATH = "/evidence-graph";

/**
 * ============================================================================
 * NODE/EDGE GRAPH FOR ONE FACT
 * ============================================================================
 */
export async function getEvidenceGraphForFactApi(factId: number): Promise<EvidenceGraph> {

  const response = await API.get<EvidenceGraph>(
    `${EVIDENCE_GRAPH_API_BASE_PATH}/facts/${factId}`,
  );

  return response.data;
}

/**
 * ============================================================================
 * EVIDENCE ITEM DETAIL
 * ============================================================================
 */
export async function getEvidenceItemApi(evidenceItemId: number): Promise<EvidenceItem> {

  const response = await API.get<EvidenceItem>(
    `${EVIDENCE_GRAPH_API_BASE_PATH}/evidence-items/${evidenceItemId}`,
  );

  return response.data;
}

/**
 * ============================================================================
 * FULL TRACE FOR ONE PATHWAY ASSESSMENT
 * ============================================================================
 */
export async function getEvidenceFullTraceApi(pathwayAssessmentId: number): Promise<EvidenceFullTrace> {

  const response = await API.get<EvidenceFullTrace>(
    `${EVIDENCE_GRAPH_API_BASE_PATH}/pathway-assessments/${pathwayAssessmentId}/full-trace`,
  );

  return response.data;
}

/**
 * ============================================================================
 * NODE/EDGE GRAPH FOR ONE DOCUMENT (Phase 3 - backward trace + document impact)
 * ============================================================================
 */
export async function getEvidenceGraphForDocumentApi(documentId: number): Promise<EvidenceGraph> {

  const response = await API.get<EvidenceGraph>(
    `${EVIDENCE_GRAPH_API_BASE_PATH}/documents/${documentId}`,
  );

  return response.data;
}

/**
 * ============================================================================
 * NODE/EDGE GRAPH FOR ONE REQUIREMENT EVALUATION (Phase 3 - requirement traceability)
 * ============================================================================
 */
export async function getEvidenceGraphForRequirementEvaluationApi(evaluationId: number): Promise<EvidenceGraph> {

  const response = await API.get<EvidenceGraph>(
    `${EVIDENCE_GRAPH_API_BASE_PATH}/requirement-evaluations/${evaluationId}`,
  );

  return response.data;
}
