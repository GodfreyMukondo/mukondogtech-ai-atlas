package com.godfrey.ai_immigration_document_analyzer.agent.entity;

/**
 * The set of goals the agent can be asked to pursue. Phase 5.1 introduces
 * exactly one - deliberately not a free-text goal, so the orchestrator can
 * never be asked to do something no plan exists for.
 */
public enum AgentGoalType {

    /** "Why is this pathway requirement currently marked with its status?" */
    EXPLAIN_REQUIREMENT
}
