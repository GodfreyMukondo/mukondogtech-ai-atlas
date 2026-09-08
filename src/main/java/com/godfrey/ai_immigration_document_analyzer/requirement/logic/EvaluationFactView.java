package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;

import java.util.List;
import java.util.Map;

/**
 * The Fact data an evaluation reads, already resolved for one specific
 * assessment date - either the live, authorized Digital Twin projection
 * (assessment date = now) or a point-in-time reconstruction (assessment
 * date in the past). {@code LogicEvaluationService} never knows or cares
 * which source produced this view - both are pre-filtered to Facts that
 * were {@code ACCEPTED} as of the assessment date.
 *
 * A Fact key present in {@link #openConflictIdByFactKey} means that key was
 * {@code CONTESTED} (not accepted) as of the assessment date - the
 * evaluator must never fall back to guessing a value for it.
 */
public record EvaluationFactView(
        Map<String, List<FactResponse>> acceptedFactsByKey,
        Map<String, Long> openConflictIdByFactKey
) {

    public static EvaluationFactView empty() {
        return new EvaluationFactView(Map.of(), Map.of());
    }

    public List<FactResponse> factsFor(String factKey) {
        return acceptedFactsByKey().getOrDefault(factKey, List.of());
    }

    public boolean isConflicted(String factKey) {
        return openConflictIdByFactKey().containsKey(factKey);
    }
}
