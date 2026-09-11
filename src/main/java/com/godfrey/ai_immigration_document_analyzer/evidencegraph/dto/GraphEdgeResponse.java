package com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto;

import java.util.Map;

/**
 * One renderable, directed edge between two {@link GraphNodeResponse} ids.
 */
public record GraphEdgeResponse(
        String id,
        String sourceNodeId,
        String targetNodeId,
        GraphRelationshipType relationship,
        Map<String, Object> metadata
) {
}
