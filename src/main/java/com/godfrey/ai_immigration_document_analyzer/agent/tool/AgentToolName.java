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
    GET_EVIDENCE_GRAPH_CONTEXT,

    /** The full Case Intelligence bundle (readiness, requirement-to-evidence matrix, missing evidence) for one whole pathway assessment (Phase 5.2). */
    GET_PATHWAY_INTELLIGENCE_CONTEXT,

    /** The ranked, transient Pathway Discovery alignment across every PUBLISHED pathway (Phase 5.3). */
    GET_PATHWAY_DISCOVERY_CONTEXT,

    /** The case-wide contradictions/timeline/signals bundle for one subject (Phase 5.4). */
    GET_CASE_OVERVIEW_CONTEXT
}
