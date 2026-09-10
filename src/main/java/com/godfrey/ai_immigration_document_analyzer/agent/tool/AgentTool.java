package com.godfrey.ai_immigration_document_analyzer.agent.tool;

import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

/**
 * A single, explicitly typed, authorized capability the agent may invoke.
 *
 * This is the ONLY surface through which the agent ever touches case data -
 * never a repository, never raw SQL, never an arbitrary Java method, never
 * an HTTP call. Every implementation:
 *
 *   1. has a stable {@link #name()}
 *   2. declares a typed input and output ({@code I}, {@code O})
 *   3. declares its {@link #accessLevel()} explicitly
 *   4. enforces authorization by delegating to an existing, already-tested
 *      authorization boundary (FactAuthorizationService, transitively via
 *      the existing service it wraps) - never a second, parallel check
 *   5. returns only data the caller is already authorized to see
 *
 * Implementations wrap an existing, already-authorized service - they never
 * introduce a new authorization decision of their own.
 */
public interface AgentTool<I, O> {

    AgentToolName name();

    AgentToolAccessLevel accessLevel();

    O invoke(AuthenticatedUser actor, I input);
}
