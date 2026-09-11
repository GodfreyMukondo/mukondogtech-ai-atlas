package com.godfrey.ai_immigration_document_analyzer.agent.entity;

/**
 * The set of goals the agent can be asked to pursue. Phase 5.1 introduces
 * exactly one - deliberately not a free-text goal, so the orchestrator can
 * never be asked to do something no plan exists for.
 */
public enum AgentGoalType {

    /** "Why is this pathway requirement currently marked with its status?" */
    EXPLAIN_REQUIREMENT,

    /** "Why is this whole pathway assessment currently at its overall outcome?" (Phase 5.2) */
    EXPLAIN_PATHWAY_ASSESSMENT,

    /** "Which published pathway should I pursue, and why?" (Phase 5.3) */
    EXPLAIN_PATHWAY_DISCOVERY,

    /** "What do my case-wide contradictions, timeline, and risk signals mean?" (Phase 5.4) */
    EXPLAIN_CASE_OVERVIEW
}
