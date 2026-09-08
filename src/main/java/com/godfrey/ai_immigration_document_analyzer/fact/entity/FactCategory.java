package com.godfrey.ai_immigration_document_analyzer.fact.entity;

/**
 * Canonical taxonomy of Fact categories for MukondoGTech AI (Fact Model
 * Architecture Specification, Table A).
 *
 * A category is the coarse-grained classification that drives default
 * sensitivity, cardinality, and staleness behaviour for every Fact of that
 * kind. The fine-grained predicate itself (e.g. "current employer" vs
 * "job title") is carried separately on {@link Fact#getFactKey()} and
 * validated against {@code FactTypeRegistry}.
 *
 * Deliberately excludes a "DOCUMENTS" category (a Document is a source, not
 * a Fact) and a "derived/assessment" category (an Assessment is a distinct
 * object type, never a Fact) - see the approved Fact Model specification,
 * section 0/1.
 */
public enum FactCategory {

    IDENTITY,
    NATIONALITY_CITIZENSHIP,
    RESIDENCE,
    TRAVEL_HISTORY,
    IMMIGRATION_STATUS,
    IMMIGRATION_HISTORY,
    EDUCATION,
    EMPLOYMENT,
    FINANCES,
    LANGUAGE_PROFICIENCY,
    FAMILY_DEPENDANTS,
    RELATIONSHIPS,
    PROFESSIONAL_CREDENTIALS,
    LEGAL_ADMINISTRATIVE_HISTORY,
    HEALTH_IMMIGRATION_RELEVANT,

    /**
     * Subject is a Requirement/Pathway/jurisdiction, not a person. Uses the
     * same Fact primitive but is never personal data.
     */
    REGULATORY_CONTEXT,

    /**
     * Narrow category for atomic facts about the case's own process (e.g.
     * "user acknowledged recommendation #12"). Never a home for Assessments
     * or Recommendations themselves.
     */
    CASE_PROCESS_INTERACTION
}
