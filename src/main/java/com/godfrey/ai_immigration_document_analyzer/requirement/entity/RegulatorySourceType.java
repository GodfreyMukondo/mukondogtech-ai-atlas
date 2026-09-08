package com.godfrey.ai_immigration_document_analyzer.requirement.entity;

/**
 * Where a {@link RegulatoryVersion}'s content came from (approved
 * specification, section 14). {@link #AI_GENERATED_INFORMATION} can never
 * promote itself to {@link RegulatoryVerificationStatus#AUTHORITATIVE_CONFIRMED}
 * merely by existing - only an explicit human/authoritative confirmation
 * (recorded the same way {@code Fact.verificationMethod} records one) can
 * raise a version's trust tier.
 */
public enum RegulatorySourceType {

    /** Primary legal/regulatory text or an official government publication. */
    AUTHORITATIVE_REGULATORY_SOURCE,

    /** Reputable summaries, legal-practice guidance, historical archives. */
    SECONDARY_INFORMATION,

    /** Synthesized by an AI process (summarization, extraction, translation). Always starts UNVERIFIED_INGESTION. */
    AI_GENERATED_INFORMATION
}
