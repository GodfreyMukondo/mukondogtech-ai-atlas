package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Top-level response of {@code GET /api/pathways/discovery} (Phase 4 -
 * Pathway Discovery &amp; Explainable Ranking). Recomputed fresh on every
 * call from the subject's live Digital Twin and the current PUBLISHED
 * pathway catalogue - never cached, never persisted, never a stored rank.
 */
public record PathwayDiscoveryResponse(
        Long subjectUserId,
        LocalDateTime generatedAt,
        int totalPublishedPathways,
        List<PathwayRankingRow> rankedPathways,
        String disclaimer
) {

    public static final String DISCLAIMER =
            "Pathway rankings reflect how closely your currently available case information aligns with the "
                    + "published requirements represented in MukondoGTech AI. They are not predictions or "
                    + "guarantees of immigration approval, legal advice, or an assessment of your likelihood of "
                    + "success with any government authority.";
}
