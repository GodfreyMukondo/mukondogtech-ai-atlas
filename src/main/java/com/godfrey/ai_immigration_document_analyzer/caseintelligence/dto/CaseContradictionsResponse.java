package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactConflictResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Case-wide, cross-document contradiction view, combining two distinct
 * sources - both surfaced only as "potential contradictions" requiring
 * verification, NEVER as fraud findings:
 *
 * - {@code openContradictions}: the exact same persisted {@link FactConflictResponse}
 *   rows the Digital Twin's open-conflicts list already returns. This
 *   endpoint performs no detection of its own here - detection already
 *   happened in {@code FactConflictService} at Fact-ingestion time.
 * - {@code potentialOverlaps}: a read-time-only (never persisted) check for
 *   overlapping time windows between two of the subject's own Facts of the
 *   same historical-multi-valued key (e.g. two overlapping employment
 *   records) - a gap in what ingestion-time conflict detection covers,
 *   since it only compares an incoming Fact against the currently
 *   open-ended one, never every pair of already-closed historical Facts.
 */
public record CaseContradictionsResponse(
        Long subjectUserId,
        List<FactConflictResponse> openContradictions,
        List<PotentialOverlapContradiction> potentialOverlaps,
        LocalDateTime generatedAt,
        String note
) {
}
