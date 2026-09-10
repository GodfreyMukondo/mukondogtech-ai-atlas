package com.godfrey.ai_immigration_document_analyzer.agent.dto;

import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceGraphResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactEvidenceResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;

import java.util.List;

/**
 * The structured, grounded answer to "why is this requirement currently
 * marked with its status." Every field here is either copied directly from
 * an existing, already-authorized service response (never re-derived or
 * guessed), or - for {@link #explanation}, {@link #recommendedNextStep} -
 * the LLM's prose layered strictly on top of that already-computed data.
 *
 * {@code factsConsidered}/{@code evidenceConsidered}/{@code conflicts} are
 * populated only from the tool responses this run actually received - the
 * LLM never introduces a fact/evidence/conflict id of its own.
 */
public record ExplainRequirementResult(
        Long pathwayAssessmentId,
        Long requirementId,
        String requirementKey,
        String requirementTitle,

        /** The authoritative nine-state outcome (never renamed/collapsed by the agent). */
        RequirementEvaluationOutcome currentStatus,

        /** The Case Intelligence reclassification of {@link #currentStatus} (see CaseRequirementSupportStatus). */
        CaseRequirementSupportStatus supportStatus,

        /**
         * The application's own pre-existing, deterministically-computed
         * explanation for {@link #currentStatus} - produced by
         * {@code RequirementEvaluationService}/{@code LogicEvaluationService}
         * at evaluation time, NOT by this agent. Authoritative application
         * context, never AI-generated - kept strictly separate from
         * {@link #explanation} so a consumer can never mistake one for the
         * other. Null when the evaluation itself recorded none.
         */
        String existingExplanation,

        /**
         * The LLM's grounded explanation - phrased using {@link #existingExplanation}
         * and the facts/evidence/conflicts below as its only grounding
         * context, or a fixed "insufficient evidence" statement (never
         * AI-generated in that case) - see {@link #groundingState}. This
         * field is the agent's own output; it is never itself authoritative.
         */
        String explanation,

        List<ExplanationResponse.FactExplanation> factsConsidered,
        List<FactEvidenceResponse> evidenceConsidered,

        /** Fact keys this requirement is bound to but for which no contributing fact was found. */
        List<String> evidenceGaps,

        /** Unresolved FactConflict ids blocking this requirement - never interpreted as fraud. */
        List<Long> conflicts,

        /** The Evidence Graph (documents/versions/facts/conflicts) behind this requirement's evaluation, when available. */
        EvidenceGraphResponse provenance,

        /**
         * The regulatory version backing this requirement, as actually
         * selected by the application - never invented, never a default.
         * Null only if the requirement genuinely carries none.
         */
        Long regulatoryVersionId,
        String regulatorySourceAuthority,
        RegulatoryVerificationStatus regulatoryVerificationStatus,

        AgentGroundingState groundingState,

        String recommendedNextStep,

        boolean humanReviewRequired
) {
}
