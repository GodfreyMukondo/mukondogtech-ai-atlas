package com.godfrey.ai_immigration_document_analyzer.fact.entity;

/**
 * Who or what performed an action against the Fact foundation. Used on
 * {@link Fact#getCreatedByAccessorType()}, {@link FactTimelineEvent}, and
 * {@link FactAccessAuditLog} so provenance ("how the claim originated") and
 * actor ("who actually did this") are never conflated.
 */
public enum AccessorType {
    SUBJECT,
    CASE_WORKER,
    ADMINISTRATOR,
    SYSTEM,

    /** Reserved for future AI agent access - not used until Stage 9. */
    AGENT
}
