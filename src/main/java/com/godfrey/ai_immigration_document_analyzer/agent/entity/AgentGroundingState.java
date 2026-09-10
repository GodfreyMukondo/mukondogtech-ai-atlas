package com.godfrey.ai_immigration_document_analyzer.agent.entity;

/**
 * Whether the case data the agent's tools returned was sufficient to ground
 * an explanation. Deliberately checked BEFORE any LLM call is made - see
 * {@code ExplainRequirementAgentService} - rather than left to the model to
 * notice on its own.
 */
public enum AgentGroundingState {

    /** At least one fact, evidence item, or conflict was found to explain the status from. */
    GROUNDED,

    /** Nothing was found to explain - the LLM is never invoked in this case. */
    INSUFFICIENT_EVIDENCE
}
