package com.godfrey.ai_immigration_document_analyzer.agent.entity;

/**
 * ============================================================================
 * AI EXPLANATION STATUS
 * ============================================================================
 *
 * Whether the LLM's own narrative/recommended-next-step text was actually
 * produced for a run - NOT whether the underlying pathway assessment itself
 * is trustworthy or complete. Deliberately a separate axis from {@link
 * AgentGroundingState} (Phase 5.2 Production Hardening):
 *
 * - {@link AgentGroundingState} answers "did the DETERMINISTIC evaluation
 *   produce enough data to explain" - computed entirely from {@code
 *   CaseIntelligenceResponse}, never from the LLM.
 * - {@link AiExplanationStatus} answers "did the OPTIONAL AI narration step
 *   succeed" - the LLM call is a presentation-layer enhancement on top of an
 *   already-complete deterministic result, never a precondition for it.
 *
 * A run can therefore be {@code GROUNDED} (the assessment has real,
 * deterministically-computed data) while its {@code aiExplanationStatus} is
 * {@code UNAVAILABLE} or {@code FAILED} (the AI provider could not be
 * reached, or returned something unusable) - the deterministic result is
 * still returned successfully in that case; only the AI-authored prose is
 * absent. See {@code ExplainPathwayAssessmentAgentService}.
 * ============================================================================
 */
public enum AiExplanationStatus {

    /** The LLM was invoked and produced a usable narrative/next-step pair. */
    GENERATED,

    /**
     * The LLM was never invoked because the assessment itself was
     * ungrounded (see {@link AgentGroundingState#INSUFFICIENT_EVIDENCE}) -
     * there was nothing for it to narrate, so it was deliberately skipped
     * rather than asked to explain an empty result.
     */
    NOT_ATTEMPTED,

    /**
     * The LLM was invoked but the call itself failed - provider outage,
     * network/connection failure, timeout, rate limiting, or an exhausted
     * quota/credit balance. A retry may succeed once the provider recovers.
     */
    UNAVAILABLE,

    /**
     * The LLM was invoked and responded, but the response could not be
     * used - empty text, or a payload that could not be parsed into the
     * expected explanation/recommendedNextStep shape.
     */
    FAILED
}
