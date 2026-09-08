package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;

import java.util.EnumMap;
import java.util.Map;

/**
 * A trust ranking over {@code FactProvenanceType}, used only to check
 * whether a Fact meets a Requirement's declared
 * {@link FactEvidenceExpectation#minimumProvenanceType()} - deliberately NOT
 * a replacement for {@code ConfidenceCalculator}'s scoring, just an ordering
 * over the same provenance vocabulary for one narrow comparison.
 */
public final class ProvenanceTrust {

    private static final Map<FactProvenanceType, Integer> RANK = buildRank();

    private ProvenanceTrust() {
    }

    /** True if {@code actual} meets or exceeds {@code minimum} on the trust ranking. {@code minimum == null} always passes. */
    public static boolean meetsMinimum(FactProvenanceType actual, FactProvenanceType minimum) {

        if (minimum == null) {
            return true;
        }

        if (actual == null) {
            return false;
        }

        return RANK.getOrDefault(actual, 0) >= RANK.getOrDefault(minimum, 0);
    }

    private static Map<FactProvenanceType, Integer> buildRank() {

        Map<FactProvenanceType, Integer> rank = new EnumMap<>(FactProvenanceType.class);

        rank.put(FactProvenanceType.ASSUMPTION, 0);
        rank.put(FactProvenanceType.USER_INPUT, 1);
        rank.put(FactProvenanceType.DERIVED_FACT, 1);
        rank.put(FactProvenanceType.SYSTEM_PROCESS, 2);
        rank.put(FactProvenanceType.DOCUMENT_EXTRACTION, 3);
        rank.put(FactProvenanceType.HUMAN_VERIFICATION, 4);
        rank.put(FactProvenanceType.EXTERNAL_AUTHORITATIVE_SOURCE, 5);
        // SIMULATION intentionally excluded - a simulated Fact must never
        // reach real (non-simulation) Requirement Evaluation in the first
        // place, so it has no meaningful rank here.

        return rank;
    }
}
