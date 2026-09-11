import type {
  CaseContradictions,
  CaseReadiness,
  CaseRequirementSupportStatus,
  CaseSignals,
  CaseTimeline,
  MissingEvidenceItem,
  RequirementEvidenceMatrixRow,
} from "./caseIntelligence";
import type { RequirementEvaluationOutcome } from "./pathwayAssessment";
import type { FactExplanation } from "./pathwayExplanation";
import type { FactEvidence } from "./fact";
import type { EvidenceGraph } from "./evidenceGraph";
import type { PathwayRankingRow } from "./pathwayDiscovery";

/**
 * ============================================================================
 * AGENT TYPES (Phase 5.1 - EXPLAIN_REQUIREMENT, Phase 5.2 - EXPLAIN_PATHWAY_ASSESSMENT,
 * Phase 5.3 - EXPLAIN_PATHWAY_DISCOVERY)
 * ============================================================================
 *
 * Mirrors the backend's agent.dto package exactly (POST/GET /api/agent/**).
 * Deliberately reuses the existing Requirement/Case-Intelligence/Evidence
 * Graph/Pathway-Discovery vocabulary (CaseRequirementSupportStatus,
 * RequirementEvaluationOutcome, FactExplanation, FactEvidence, EvidenceGraph,
 * CaseReadiness, RequirementEvidenceMatrixRow, MissingEvidenceItem,
 * PathwayRankingRow) rather than redefining any of it - this is a
 * read-projection layer over data those types already model.
 * ============================================================================
 */

export type AgentGoalType =
  | "EXPLAIN_REQUIREMENT"
  | "EXPLAIN_PATHWAY_ASSESSMENT"
  | "EXPLAIN_PATHWAY_DISCOVERY"
  | "EXPLAIN_CASE_OVERVIEW";

export type AgentRunStatus = "COMPLETED" | "INSUFFICIENT_EVIDENCE" | "FAILED";

export type AgentGroundingState = "GROUNDED" | "INSUFFICIENT_EVIDENCE";

/**
 * Whether the LLM's own narrative/recommendedNextStep text was actually
 * produced for a run (Phase 5.2 Production Hardening) - NOT whether the
 * deterministic pathway assessment itself is valid. See
 * `ExplainPathwayAssessmentResult.aiExplanationStatus` below: a run can be
 * fully `GROUNDED` (real deterministic data) while its AI narrative is
 * `UNAVAILABLE`/`FAILED` - the assessment is still returned successfully in
 * that case, only the AI-authored prose is absent.
 *
 * - GENERATED: the LLM produced a usable narrative/next-step pair.
 * - NOT_ATTEMPTED: the assessment was ungrounded, so the LLM was never asked.
 * - UNAVAILABLE: the LLM call itself failed (provider outage, timeout, rate
 *   limiting, exhausted quota/credit balance). A retry may succeed later.
 * - FAILED: the LLM responded but the response could not be used.
 */
export type AiExplanationStatus = "GENERATED" | "NOT_ATTEMPTED" | "UNAVAILABLE" | "FAILED";

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
  /** Null when `aiExplanationStatus` is `UNAVAILABLE`/`FAILED` - never a fabricated placeholder explanation. */
  explanation?: string | null;
  /** Whether the deterministic requirement result above is complete/trustworthy - independent of the AI step. */
  groundingState: AgentGroundingState;
  /** Whether the AI explanation itself was produced - see the type's own doc comment (Phase 5.1 Production Hardening). */
  aiExplanationStatus: AiExplanationStatus;
  factsConsidered: FactExplanation[];
  evidenceConsidered: FactEvidence[];
  evidenceGaps: string[];
  conflicts: number[];
  provenance: EvidenceGraph;
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

/**
 * ============================================================================
 * PHASE 5.2 - EXPLAIN_PATHWAY_ASSESSMENT
 * ============================================================================
 */

export interface ExplainPathwayAssessmentRequest {
  pathwayAssessmentId: number;
}

/**
 * The structured, grounded answer to "why is this whole pathway assessment
 * currently at its overall outcome." Every deterministic field here mirrors
 * the backend's CaseIntelligenceResponse exactly - only `explanation` and
 * `recommendedNextStep` are the LLM's own prose, layered on top.
 */
export interface ExplainPathwayAssessmentResult {
  pathwayAssessmentId: number;
  pathwayKey: string;
  pathwayName: string;
  overallOutcome: RequirementEvaluationOutcome;
  readiness: CaseReadiness;
  requirementSummaries: RequirementEvidenceMatrixRow[];
  missingEvidence: MissingEvidenceItem[];
  recommendedEvidenceGuidance?: string | null;
  /** Null when `aiExplanationStatus` is `UNAVAILABLE`/`FAILED` - never a fabricated placeholder narrative. */
  explanation?: string | null;
  /** Whether the deterministic assessment above is complete/trustworthy - independent of the AI step. */
  groundingState: AgentGroundingState;
  /** Whether the AI narrative itself was produced - see the type's own doc comment. */
  aiExplanationStatus: AiExplanationStatus;
  recommendedNextStep?: string | null;
  humanReviewRequired: boolean;
}

/**
 * The API-facing view of one AgentRun for the EXPLAIN_PATHWAY_ASSESSMENT
 * goal - a deliberate sibling of AgentRunResponse rather than a reuse of it,
 * since that interface's `result` field is typed to ExplainRequirementResult.
 */
export interface PathwayAssessmentAgentRunResponse {
  id: number;
  goal: AgentGoalType;
  status: AgentRunStatus;
  subjectUserId: number;
  pathwayAssessmentId?: number | null;
  planSummary?: string | null;
  toolsInvoked: string[];
  model?: string | null;
  reasoningSummary?: string | null;
  result: ExplainPathwayAssessmentResult | null;
  groundingState?: AgentGroundingState | null;
  humanReviewRequired: boolean;
  errorMessage?: string | null;
  startedAt: string;
  completedAt?: string | null;
}

/**
 * ============================================================================
 * PHASE 5.3 - EXPLAIN_PATHWAY_DISCOVERY
 * ============================================================================
 */

export interface ExplainPathwayDiscoveryRequest {
  subjectUserId: number;
}

/**
 * The structured, grounded answer to "which published pathway should I
 * pursue, and why." Every deterministic field here mirrors the backend's
 * PathwayDiscoveryResponse exactly - only `explanation` and
 * `recommendedNextStep` are the LLM's own prose, layered on top.
 */
export interface ExplainPathwayDiscoveryResult {
  subjectUserId: number;
  generatedAt: string;
  totalPublishedPathways: number;
  rankedPathways: PathwayRankingRow[];
  disclaimer: string;
  /** Null when `aiExplanationStatus` is `UNAVAILABLE`/`FAILED` - never a fabricated placeholder recommendation. */
  explanation?: string | null;
  /** Whether the deterministic ranking above is complete/trustworthy - independent of the AI step. */
  groundingState: AgentGroundingState;
  /** Whether the AI recommendation itself was produced - see the type's own doc comment. */
  aiExplanationStatus: AiExplanationStatus;
  recommendedNextStep?: string | null;
  humanReviewRequired: boolean;
}

/**
 * The API-facing view of one AgentRun for the EXPLAIN_PATHWAY_DISCOVERY
 * goal - a deliberate sibling of AgentRunResponse/PathwayAssessmentAgentRunResponse
 * rather than a reuse of either, since each Phase's `result` field is typed
 * to its own result shape.
 */
export interface PathwayDiscoveryAgentRunResponse {
  id: number;
  goal: AgentGoalType;
  status: AgentRunStatus;
  subjectUserId: number;
  planSummary?: string | null;
  toolsInvoked: string[];
  model?: string | null;
  reasoningSummary?: string | null;
  result: ExplainPathwayDiscoveryResult | null;
  groundingState?: AgentGroundingState | null;
  humanReviewRequired: boolean;
  errorMessage?: string | null;
  startedAt: string;
  completedAt?: string | null;
}

/**
 * ============================================================================
 * PHASE 5.4 - EXPLAIN_CASE_OVERVIEW
 * ============================================================================
 */

export interface ExplainCaseOverviewRequest {
  subjectUserId: number;
}

/**
 * The structured, grounded answer to "what do my case-wide contradictions,
 * timeline, and risk signals mean." Every deterministic field here mirrors
 * the backend's CaseContradictions/CaseTimeline/CaseSignals exactly - the
 * SAME shapes GET /api/cases/{subjectUserId}/{contradictions,timeline,
 * signals} already returns - only `explanation` and `recommendedNextStep`
 * are the LLM's own prose, layered on top.
 */
export interface ExplainCaseOverviewResult {
  subjectUserId: number;
  generatedAt: string;
  contradictions: CaseContradictions;
  timeline: CaseTimeline;
  signals: CaseSignals;
  /** Null when `aiExplanationStatus` is `UNAVAILABLE`/`FAILED` - never a fabricated placeholder explanation. */
  explanation?: string | null;
  /** Whether the subject had any real case history to explain - independent of the AI step. */
  groundingState: AgentGroundingState;
  /** Whether the AI explanation itself was produced - see the type's own doc comment. */
  aiExplanationStatus: AiExplanationStatus;
  recommendedNextStep?: string | null;
  humanReviewRequired: boolean;
}

/**
 * The API-facing view of one AgentRun for the EXPLAIN_CASE_OVERVIEW goal - a
 * deliberate sibling of AgentRunResponse/PathwayAssessmentAgentRunResponse/
 * PathwayDiscoveryAgentRunResponse rather than a reuse of any of them, since
 * each Phase's `result` field is typed to its own result shape.
 */
export interface CaseOverviewAgentRunResponse {
  id: number;
  goal: AgentGoalType;
  status: AgentRunStatus;
  subjectUserId: number;
  planSummary?: string | null;
  toolsInvoked: string[];
  model?: string | null;
  reasoningSummary?: string | null;
  result: ExplainCaseOverviewResult | null;
  groundingState?: AgentGroundingState | null;
  humanReviewRequired: boolean;
  errorMessage?: string | null;
  startedAt: string;
  completedAt?: string | null;
}
