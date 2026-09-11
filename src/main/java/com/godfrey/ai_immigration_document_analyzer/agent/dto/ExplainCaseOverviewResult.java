package com.godfrey.ai_immigration_document_analyzer.agent.dto;

import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AiExplanationStatus;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseContradictionsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseSignalsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseTimelineResponse;

import java.time.LocalDateTime;

/**
 * The structured, grounded answer to "what do my case-wide signals mean"
 * (Phase 5.4) - fact conflicts, timeline gaps/overlaps, and the evidence/
 * anomaly risk band. Every deterministic field here is copied directly from
 * {@link CaseContradictionsResponse}/{@link CaseTimelineResponse}/{@link
 * CaseSignalsResponse} - the SAME already-authorized, already-computed
 * bundle {@code CaseOverviewService}'s three existing methods already
 * produce (identical to what {@code GET /api/cases/{subjectUserId}/{
 * contradictions,timeline,signals}} returns) - never re-derived, re-
 * calculated, or reshaped by this agent. Only {@link #explanation} and
 * {@link #recommendedNextStep} are the LLM's own prose, layered strictly on
 * top of that already-computed data.
 *
 * NO OVERLAP WITH PHASE 5.1/5.2/5.3: this result never touches a
 * Requirement, RequirementEvaluation, PathwayAssessment, or Pathway
 * Discovery ranking - it is exclusively the case-wide, subject-scoped view
 * {@code CaseOverviewService} already exposes, independent of any one
 * pathway or requirement.
 */
public record ExplainCaseOverviewResult(
        Long subjectUserId,
        LocalDateTime generatedAt,

        /** Identical to {@code GET /api/cases/{subjectUserId}/contradictions}. */
        CaseContradictionsResponse contradictions,

        /** Identical to {@code GET /api/cases/{subjectUserId}/timeline}. */
        CaseTimelineResponse timeline,

        /** Identical to {@code GET /api/cases/{subjectUserId}/signals}. */
        CaseSignalsResponse signals,

        /**
         * The LLM's grounded narrative explaining what the contradictions/
         * timeline/signals above mean - phrased using those three
         * deterministic bundles as its only grounding context, or a fixed
         * "insufficient evidence" statement when ungrounded (never
         * AI-generated in that case) - see {@link #groundingState}. Null
         * when {@link #aiExplanationStatus} is {@code UNAVAILABLE}/{@code
         * FAILED} - never a fabricated placeholder explanation. This field
         * is the agent's own output; it is never itself authoritative over
         * any fact, conflict, timeline event, or risk band.
         */
        String explanation,

        /** Whether the case had any real Fact history to report on - independent of whether the LLM ran. */
        AgentGroundingState groundingState,

        /** Whether the LLM actually produced {@link #explanation}/{@link #recommendedNextStep} for this run - see {@code AiExplanationStatus}. */
        AiExplanationStatus aiExplanationStatus,

        String recommendedNextStep,

        boolean humanReviewRequired
) {
}
