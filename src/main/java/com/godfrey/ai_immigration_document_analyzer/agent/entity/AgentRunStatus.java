package com.godfrey.ai_immigration_document_analyzer.agent.entity;

/**
 * Terminal states of one {@link AgentRun}.
 */
public enum AgentRunStatus {

    /** A grounded explanation was produced and persisted. */
    COMPLETED,

    /** The gathered case data did not ground an explanation - reported honestly, never hallucinated past. */
    INSUFFICIENT_EVIDENCE,

    /** A tool or the LLM call failed - never masked as a result. */
    FAILED
}
