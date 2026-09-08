package com.godfrey.ai_immigration_document_analyzer.fact.policy;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;

/**
 * One row of the canonical Fact Type Taxonomy (approved specification,
 * Table A), kept as typed, in-code reference data rather than hardcoded
 * per-field conditionals scattered across services.
 *
 * @param factKey          e.g. "EMPLOYMENT.CURRENT_EMPLOYER"
 * @param category         coarse-grained taxonomy category
 * @param valueType        the strongly typed shape a value of this key must take
 * @param cardinality      single-current / historical-multivalued / permanently-multivalued
 * @param sensitivityTier  default sensitivity tier for this key
 * @param stalenessDays    null = never flagged stale by silence; otherwise days since last observation
 */
public record FactTypeDefinition(
        String factKey,
        FactCategory category,
        FactValueType valueType,
        FactCardinality cardinality,
        FactSensitivityTier sensitivityTier,
        Integer stalenessDays
) {
}
