package com.godfrey.ai_immigration_document_analyzer.agent.dto;

import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AiExplanationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayRankingRow;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The structured, grounded answer to "which published pathway should I
 * pursue, and why" (Phase 5.3). Every deterministic field here is copied
 * directly from {@code PathwayDiscoveryResponse} - the same already-
 * authorized, already-computed, transient ranking {@code
 * PathwayDiscoveryService.discover} produces - never re-derived or guessed
 * by this agent. Only {@link #explanation} and {@link #recommendedNextStep}
 * are the LLM's own prose, layered strictly on top of that already-computed
 * data.
 *
 * {@link #rankedPathways} preserves full provenance down to the per-pathway
 * level: each row already carries its own deterministic explanation,
 * readiness breakdown, and top-missing-requirements list - nothing here
 * collapses that detail away.
 */
public record ExplainPathwayDiscoveryResult(
        Long subjectUserId,
        LocalDateTime generatedAt,
        int totalPublishedPathways,

        /** Every published Pathway's transient alignment result, in rank order - identical to {@code PathwayDiscoveryResponse.rankedPathways}. */
        List<PathwayRankingRow> rankedPathways,

        /** The Discovery feature's own fixed, non-AI-generated disclaimer - carried through verbatim, never paraphrased. */
        String disclaimer,

        /**
         * The LLM's grounded narrative recommending which pathway(s) to
         * pursue and why - phrased using {@link #rankedPathways} as its only
         * grounding context, or a fixed "insufficient evidence" statement
         * when ungrounded (never AI-generated in that case) - see {@link
         * #groundingState}. Null when {@link #aiExplanationStatus} is {@code
         * UNAVAILABLE}/{@code FAILED} - never a fabricated placeholder
         * recommendation. This field is the agent's own output; it is never
         * itself authoritative.
         */
        String explanation,

        /** Whether the deterministic ranking above is complete/trustworthy - independent of whether the LLM ran. */
        AgentGroundingState groundingState,

        /** Whether the LLM actually produced {@link #explanation}/{@link #recommendedNextStep} for this run - see {@code AiExplanationStatus}. */
        AiExplanationStatus aiExplanationStatus,

        String recommendedNextStep,

        boolean humanReviewRequired
) {
}
