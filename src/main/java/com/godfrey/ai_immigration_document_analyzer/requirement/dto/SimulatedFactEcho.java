package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;

/**
 * Echoes back one hypothetical override actually used to build the AFTER
 * state, so the response is never ambiguous about what was simulated.
 * {@code provenance} is always {@link FactProvenanceType#SIMULATION} -
 * carried explicitly on every row so a client can never mistake this for a
 * real accepted Fact even if it renders the two lists side by side.
 */
public record SimulatedFactEcho(
        String factKey,
        String simulatedValue,
        FactProvenanceType provenance
) {
}
