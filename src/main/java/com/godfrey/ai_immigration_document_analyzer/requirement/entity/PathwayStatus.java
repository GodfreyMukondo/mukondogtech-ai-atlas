package com.godfrey.ai_immigration_document_analyzer.requirement.entity;

/**
 * Lifecycle of a Pathway catalogue entry itself - never a person's
 * application/case status (Phase 1 Pathway Catalogue &amp; Publishing spec).
 *
 * Guarded by {@code PathwayLifecycleService} - no endpoint sets this value
 * directly. {@link #SUPERSEDED} and {@link #ARCHIVED} are deliberately
 * distinct: SUPERSEDED means a newer row sharing this pathway's
 * {@code pathwayKey} has since been published (this row's content is
 * retained, unedited, for historical/reproducibility reasons - see
 * {@code PathwayAssessment.pathwayId}, which still points at the exact row
 * that produced it); ARCHIVED means the pathway itself was retired with no
 * replacement.
 */
public enum PathwayStatus {

    /** Authored, not yet ready for review. */
    DRAFT,

    /** Under administrator review, not yet visible to applicants. */
    REVIEW,

    /** Visible to applicants via GET /api/pathways and assessable. */
    PUBLISHED,

    /** Replaced by a newer PUBLISHED row sharing the same pathwayKey. Terminal. */
    SUPERSEDED,

    /** Retired with no replacement. Terminal. */
    ARCHIVED
}
