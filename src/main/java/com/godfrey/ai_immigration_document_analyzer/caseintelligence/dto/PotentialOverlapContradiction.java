package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

import java.time.LocalDateTime;

/**
 * A read-time-only, NEVER-persisted observation that two of the subject's
 * own current Facts of the same HISTORICAL_MULTI_VALUED factKey (e.g. two
 * employment records) claim overlapping time windows.
 *
 * This is deliberately NOT a {@code FactConflict} row: {@code FactConflictService}
 * already detects a conflict at ingestion time when an INCOMING fact
 * overlaps the CURRENTLY open-ended fact of the same key, but it has no
 * mechanism to compare every pair of a subject's already-closed historical
 * facts against each other. This computation fills exactly that gap,
 * without introducing a second persisted conflict-detection system -
 * nothing here is written to the database, and it never upgrades itself
 * into a {@code FactConflict}.
 *
 * Per the approved terminology discipline: this is a "potential
 * contradiction" / "evidence inconsistency" that requires verification -
 * NEVER, under any circumstance, a fraud finding.
 */
public record PotentialOverlapContradiction(
        String factKey,
        Long factAId,
        LocalDateTime factAEffectiveFrom,
        LocalDateTime factAEffectiveTo,
        Long factBId,
        LocalDateTime factBEffectiveFrom,
        LocalDateTime factBEffectiveTo,
        long overlapDays,
        String description
) {
}
