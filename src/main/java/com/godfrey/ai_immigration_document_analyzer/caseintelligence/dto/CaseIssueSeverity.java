package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

/**
 * How urgently a case-readiness issue or missing-evidence item needs
 * attention. Deliberately a small, transparent, count/rule-based banding -
 * never a machine-learned probability, and never a statement that the
 * underlying requirement will or will not ultimately be approved.
 */
public enum CaseIssueSeverity {

    /** A mandatory requirement is definitively NOT_SATISFIED. */
    CRITICAL,

    /** Evidence is missing/unverified, conflicted, or expired for a requirement. */
    HIGH,

    /** A non-mandatory requirement is unresolved, or human review is pending. */
    MEDIUM
}
