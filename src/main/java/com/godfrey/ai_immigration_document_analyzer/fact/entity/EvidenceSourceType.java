package com.godfrey.ai_immigration_document_analyzer.fact.entity;

/** What kind of source a {@link FactEvidence} record points at. */
public enum EvidenceSourceType {
    DOCUMENT,
    USER_STATEMENT,
    ADMIN_ATTESTATION,
    EXTERNAL_SOURCE,
    DERIVATION
}
