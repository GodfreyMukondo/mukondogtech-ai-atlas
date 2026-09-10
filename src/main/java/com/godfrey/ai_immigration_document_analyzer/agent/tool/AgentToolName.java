package com.godfrey.ai_immigration_document_analyzer.agent.tool;

/**
 * The closed, stable set of tools an agent may invoke. An
 * {@link AgentTool}'s name is always one of these - the agent can never
 * invoke something that isn't a registered, typed tool.
 */
public enum AgentToolName {

    /** Requirement outcome, support status, contributing facts/evidence, missing keys, regulatory source. */
    GET_REQUIREMENT_EXPLANATION,

    /** The Evidence Graph (documents/versions/facts/conflicts) behind one requirement evaluation. */
    GET_EVIDENCE_GRAPH_CONTEXT
}
