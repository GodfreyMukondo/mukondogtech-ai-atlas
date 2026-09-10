package com.godfrey.ai_immigration_document_analyzer.agent.tool;

/**
 * A tool's declared access level. Phase 5.1 introduces only READ_ONLY tools -
 * this enum exists so that fact is an explicit, visible declaration every
 * tool must make, not an unstated assumption. A future write-capable tool
 * would need a new value here plus new, deliberate orchestrator-level
 * handling - it could never silently slip in as if it were read-only.
 */
public enum AgentToolAccessLevel {

    READ_ONLY
}
