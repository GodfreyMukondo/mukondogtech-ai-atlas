package com.godfrey.ai_immigration_document_analyzer.agent.dto;

import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AiExplanationStatus;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseReadinessResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.MissingEvidenceItemResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.RequirementEvidenceMatrixRowResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;

import java.util.List;

/**
 * The structured, grounded answer to "why is this whole pathway assessment
 * currently at its overall outcome" (Phase 5.2). Every deterministic field
 * here is copied directly from {@code CaseIntelligenceResponse} - the same
 * already-authorized, already-computed bundle the Case Intelligence view
 * itself displays - never re-derived or guessed by this agent. Only
 * {@link #explanation} and {@link #recommendedNextStep} are the LLM's own
 * prose, layered strictly on top of that already-computed data.
 *
 * {@link #requirementSummaries} preserves full provenance down to the
 * per-requirement level: each row already carries its own contributing
 * facts (with their own evidence/provenance), missing fact keys, conflict
 * ids, and regulatory version/source - nothing here collapses that detail
 * away.
 */
public record ExplainPathwayAssessmentResult(
        Long pathwayAssessmentId,
        String pathwayKey,
        String pathwayName,

        /** The authoritative overall outcome, computed deterministically by {@code PathwayAssessmentService}/{@code LogicEvaluationService} - never decided or overridden by the agent. */
        RequirementEvaluationOutcome overallOutcome,

        /** Multi-dimensional readiness breakdown (requirement coverage, evidence coverage, consistency, overall) - see {@code RequirementReadinessCalculator}. */
        CaseReadinessResponse readiness,

        /** Every requirement in this pathway, with its own outcome, support status, contributing facts, missing keys, conflicts, and regulatory source - the full explainability chain, flattened. */
        List<RequirementEvidenceMatrixRowResponse> requirementSummaries,

        /** Requirements whose evaluation could not proceed for lack of sufficient evidence, prioritized. */
        List<MissingEvidenceItemResponse> missingEvidence,

        /** The Pathway's own pre-existing, non-gating evidence guidance, when recorded. */
        String recommendedEvidenceGuidance,

        /**
         * The LLM's grounded narrative of the whole pathway's status -
         * phrased using {@link #requirementSummaries}/{@link #readiness}/
         * {@link #missingEvidence} as its only grounding context, or a fixed
         * "insufficient evidence" statement when ungrounded (never
         * AI-generated in that case) - see {@link #groundingState}. Null
         * when {@link #aiExplanationStatus} is {@code UNAVAILABLE} or
         * {@code FAILED} - never a fabricated placeholder narrative. This
         * field is the agent's own output; it is never itself authoritative.
         */
        String explanation,

        /**
         * Whether the deterministic assessment above ({@link
         * #overallOutcome}, {@link #requirementSummaries}, {@link
         * #readiness}, {@link #missingEvidence}) is complete and
         * trustworthy - computed entirely from {@code
         * CaseIntelligenceResponse}, independent of whether the LLM ran.
         * See {@code AiExplanationStatus} for the separate, orthogonal
         * question of whether the AI narrative itself was produced.
         */
        AgentGroundingState groundingState,

        /**
         * Whether the LLM actually produced {@link #explanation}/{@link
         * #recommendedNextStep} for this run - NOT whether the pathway
         * assessment above is valid. {@code GENERATED} means real AI prose
         * is present; {@code NOT_ATTEMPTED} means the assessment was
         * ungrounded so the LLM was never asked; {@code UNAVAILABLE}/{@code
         * FAILED} mean the LLM was asked but could not answer (provider
         * outage, quota exhaustion, timeout, or an unusable response) - the
         * deterministic assessment above remains fully valid and complete
         * in every one of these cases.
         */
        AiExplanationStatus aiExplanationStatus,

        /**
         * The LLM's recommended next step, or - when {@link
         * #aiExplanationStatus} is {@code UNAVAILABLE}/{@code FAILED} -
         * the Pathway's own deterministic {@link #recommendedEvidenceGuidance}
         * reused as a safe, non-fabricated fallback (or null when the
         * pathway records none). Never invented text standing in for a
         * missing AI response.
         */
        String recommendedNextStep,

        boolean humanReviewRequired
) {
}
