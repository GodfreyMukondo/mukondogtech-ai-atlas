package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;

/**
 * One chronological entry in the immigration timeline - the underlying
 * {@link FactResponse} unchanged (never a second, re-typed copy of Fact
 * data), plus two purely mathematical, mutually-exclusive observations
 * computed by comparing this Fact's window against the previous same-key
 * Fact's window:
 *
 * - {@code gapDaysBeforeThisEntry}: a plain day count when this Fact's
 *   {@code effectiveFrom} starts strictly after the previous Fact of the
 *   same key ended.
 * - {@code overlapDaysWithPreviousEntry}: a plain day count when this
 *   Fact's window instead starts BEFORE the previous one ended - i.e. the
 *   two claim overlapping time (see {@link PotentialOverlapContradiction}).
 *
 * Neither figure is ever labeled an "unexplained absence", "impossible
 * chronology", or anything else carrying immigration-law or fraud
 * significance. What (if anything) either observation means is left
 * entirely to human review.
 */
public record CaseTimelineEventResponse(
        FactResponse fact,
        Long gapDaysBeforeThisEntry,
        Long overlapDaysWithPreviousEntry
) {
}
