package com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto;

import java.util.List;

/**
 * The full graph payload for one traversal - a fixed-shape, read-only
 * projection, never a second persisted copy of the domain. Recomputed on
 * every request, exactly like the Digital Twin and Explainability views it
 * builds on.
 */
public record EvidenceGraphResponse(
        List<GraphNodeResponse> nodes,
        List<GraphEdgeResponse> edges
) {

    public static EvidenceGraphResponse empty() {
        return new EvidenceGraphResponse(List.of(), List.of());
    }
}
