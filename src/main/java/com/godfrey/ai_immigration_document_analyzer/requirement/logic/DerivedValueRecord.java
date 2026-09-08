package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A transient computation artifact (approved specification, section 4) -
 * e.g. "age = 27, derived from IDENTITY.DATE_OF_BIRTH as of 2026-09-08".
 * Scoped to exactly one {@code RequirementEvaluation}'s explanation. Never
 * persisted as a Fact, never reusable as another Requirement's input except
 * by re-deriving it from the same source Facts.
 */
public record DerivedValueRecord(
        String expression,
        List<Long> inputFactIds,
        String value,
        LocalDateTime computedAt
) {
}
