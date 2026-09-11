package com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto;

import java.util.Map;

/**
 * One renderable node. {@code id} is a stable, type-prefixed string (e.g.
 * {@code "fact:101"}, {@code "document:55"}) so the frontend never needs to
 * disambiguate raw numeric ids across node types. {@code metadata} carries
 * only display-safe, already-authorized fields projected by
 * {@code EvidenceGraphService} - never a raw entity or a field the caller
 * has not already been authorized to see.
 */
public record GraphNodeResponse(
        String id,
        GraphNodeType type,
        String label,
        Map<String, Object> metadata
) {
}
