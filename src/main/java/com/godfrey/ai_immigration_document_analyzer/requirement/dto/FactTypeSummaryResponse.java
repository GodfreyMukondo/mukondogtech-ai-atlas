package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactTypeDefinition;

/**
 * Read-only projection of one {@code FactTypeRegistry} entry, exposed so
 * the Requirement authoring admin UI can populate a real fact-key picker
 * instead of free text prone to typos that would only fail validation
 * server-side at save time.
 */
public record FactTypeSummaryResponse(
        String factKey,
        FactCategory category,
        FactValueType valueType
) {

    public static FactTypeSummaryResponse from(FactTypeDefinition definition) {

        return new FactTypeSummaryResponse(
                definition.factKey(),
                definition.category(),
                definition.valueType()
        );
    }
}
