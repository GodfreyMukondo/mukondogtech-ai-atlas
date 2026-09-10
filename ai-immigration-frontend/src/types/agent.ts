import type { CaseRequirementSupportStatus } from "./caseIntelligence";
import type { RequirementEvaluationOutcome } from "./pathwayAssessment";
import type { FactExplanation } from "./pathwayExplanation";
import type { FactEvidence } from "./fact";
import type { EvidenceGraph } from "./evidenceGraph";

/**
 * ============================================================================
 * AGENT TYPES (Phase 5.1 - EXPLAIN_REQUIREMENT)
 * ============================================================================
 *
 * Mirrors the backend's agent.dto package exactly (POST/GET /api/agent/**).
 * Deliberately reuses the existing Requirement/Case-Intelligence/Evidence
 * Graph vocabulary (CaseRequirementSupportStatus, RequirementEvaluationOutcome,
 * FactExplanation, FactEvidence, EvidenceGraph) rather than redefining any of
 * it - this is a read-projection layer over data those types already model.
 * ============================================================================
 */

export type AgentGoalType = "EXPLAIN_REQUIREMENT";

export type AgentRunStatus = "COMPLETED" | "INSUFFICIENT_EVIDENCE" | "FAILED";

export type AgentGroundingState = "GROUNDED" | "INSUFFICIENT_EVIDENCE";

export interface ExplainRequirementRequest {
  pathwayAssessmentId: number;
  requirementId: number;
}

/**
 * The structured, grounded answer to "why is this requirement currently
 * marked with its status." Never a raw LLM paragraph - every field here
 * corresponds 1:1 to something the backend's tools actually retrieved.
 */
export interface ExplainRequirementResult {
  pathwayAssessmentId: number;
  requirementId: number;
  requirementKey: string;
  requirementTitle: string;
  currentStatus: RequirementEvaluationOutcome;
  supportStatus: CaseRequirementSupportStatus;
  explanation: string;
  factsConsidered: FactExplanation[];
  evidenceConsidered: FactEvidence[];
  evidenceGaps: string[];
  conflicts: number[];
  provenance: EvidenceGraph;
  groundingState: AgentGroundingState;
  recommendedNextStep?: string | null;
  humanReviewRequired: boolean;
}

/**
 * The API-facing view of one AgentRun - the audit record plus its
 * structured result (null when the run failed).
 */
export interface AgentRunResponse {
  id: number;
  goal: AgentGoalType;
  status: AgentRunStatus;
  subjectUserId: number;
  pathwayAssessmentId?: number | null;
  requirementId?: number | null;
  planSummary?: string | null;
  toolsInvoked: string[];
  model?: string | null;
  reasoningSummary?: string | null;
  result: ExplainRequirementResult | null;
  groundingState?: AgentGroundingState | null;
  humanReviewRequired: boolean;
  errorMessage?: string | null;
  startedAt: string;
  completedAt?: string | null;
}
