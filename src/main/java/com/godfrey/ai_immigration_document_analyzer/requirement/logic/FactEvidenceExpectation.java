package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;

/**
 * A Requirement's declared evidence-quality bar for one supported Fact key
 * (mirrors {@code RequirementFactBinding}, decoupled from the JPA entity so
 * the pure logic evaluator never depends on persistence types). A Fact that
 * exists but does not meet this bar yields {@code INSUFFICIENT_EVIDENCE},
 * not {@code NOT_SATISFIED} - evidence existing is not the same as evidence
 * being trustworthy enough.
 */
public record FactEvidenceExpectation(
        boolean requiresVerification,
        FactProvenanceType minimumProvenanceType
) {

    public static FactEvidenceExpectation none() {
        return new FactEvidenceExpectation(false, null);
    }
}
