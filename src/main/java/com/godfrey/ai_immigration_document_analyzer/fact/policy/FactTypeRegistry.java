package com.godfrey.ai_immigration_document_analyzer.fact.policy;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory.*;
import static com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier.*;
import static com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType.*;
import static com.godfrey.ai_immigration_document_analyzer.fact.policy.FactCardinality.*;

/**
 * ============================================================================
 * FACT TYPE REGISTRY
 * ============================================================================
 *
 * Static, typed reference data encoding the canonical Fact Type Taxonomy
 * (approved specification, Table A) - a representative, functioning set
 * covering every category, not an exhaustive catalogue. New keys are added
 * here as they're needed; nothing about {@code Fact} itself needs to change
 * to add one.
 *
 * This is the artifact named in the approved implementation roadmap as
 * prerequisite #3 - it exists so validation/sensitivity/cardinality/
 * staleness decisions are looked up from one place, never hardcoded per
 * field inside a service.
 * ============================================================================
 */
public final class FactTypeRegistry {

    private static final Map<String, FactTypeDefinition> DEFINITIONS = buildDefinitions();

    private FactTypeRegistry() {
    }

    public static FactTypeDefinition get(String factKey) {

        FactTypeDefinition definition = DEFINITIONS.get(normalize(factKey));

        if (definition == null) {
            throw new IllegalArgumentException(
                    "Unknown fact key (not present in FactTypeRegistry): " + factKey
            );
        }

        return definition;
    }

    public static boolean isKnown(String factKey) {
        return factKey != null && DEFINITIONS.containsKey(normalize(factKey));
    }

    public static Map<String, FactTypeDefinition> allDefinitions() {
        return Collections.unmodifiableMap(DEFINITIONS);
    }

    private static String normalize(String factKey) {
        return factKey == null ? null : factKey.trim().toUpperCase();
    }

    private static Map<String, FactTypeDefinition> buildDefinitions() {

        Map<String, FactTypeDefinition> definitions = new LinkedHashMap<>();

        register(definitions, "IDENTITY.FULL_NAME", IDENTITY, STRING, SINGLE_CURRENT, T2_STANDARD_PERSONAL, null);
        register(definitions, "IDENTITY.DATE_OF_BIRTH", IDENTITY, DATE, SINGLE_CURRENT, T2_STANDARD_PERSONAL, null);
        register(definitions, "IDENTITY.PASSPORT_NUMBER", IDENTITY, STRING, SINGLE_CURRENT, T3_SENSITIVE, null);

        register(definitions, "NATIONALITY_CITIZENSHIP.NATIONALITY", NATIONALITY_CITIZENSHIP, STRING, HISTORICAL_MULTI_VALUED, T3_SENSITIVE, null);

        register(definitions, "RESIDENCE.CURRENT_COUNTRY", RESIDENCE, STRING, HISTORICAL_MULTI_VALUED, T2_STANDARD_PERSONAL, 365);
        register(definitions, "RESIDENCE.CURRENT_ADDRESS", RESIDENCE, STRING, HISTORICAL_MULTI_VALUED, T2_STANDARD_PERSONAL, 365);

        register(definitions, "TRAVEL_HISTORY.ENTRY_EXIT_RECORD", TRAVEL_HISTORY, STRING, PERMANENTLY_MULTI_VALUED, T2_STANDARD_PERSONAL, null);

        register(definitions, "IMMIGRATION_STATUS.CURRENT_STATUS", IMMIGRATION_STATUS, STRING, SINGLE_CURRENT, T3_SENSITIVE, 180);

        register(definitions, "IMMIGRATION_HISTORY.PRIOR_REFUSAL", IMMIGRATION_HISTORY, STRING, PERMANENTLY_MULTI_VALUED, T3_SENSITIVE, null);

        register(definitions, "EDUCATION.DEGREE_AWARDED", EDUCATION, STRING, PERMANENTLY_MULTI_VALUED, T2_STANDARD_PERSONAL, null);
        register(definitions, "EDUCATION.INSTITUTION", EDUCATION, STRING, PERMANENTLY_MULTI_VALUED, T2_STANDARD_PERSONAL, null);
        register(definitions, "EDUCATION.GRADUATION_DATE", EDUCATION, DATE, PERMANENTLY_MULTI_VALUED, T2_STANDARD_PERSONAL, null);

        register(definitions, "EMPLOYMENT.CURRENT_EMPLOYER", EMPLOYMENT, STRING, HISTORICAL_MULTI_VALUED, T2_STANDARD_PERSONAL, 365);
        register(definitions, "EMPLOYMENT.JOB_TITLE", EMPLOYMENT, STRING, HISTORICAL_MULTI_VALUED, T2_STANDARD_PERSONAL, 365);
        register(definitions, "EMPLOYMENT.SALARY", EMPLOYMENT, NUMBER, HISTORICAL_MULTI_VALUED, T3_SENSITIVE, 180);

        register(definitions, "FINANCES.SAVINGS_BALANCE", FINANCES, NUMBER, HISTORICAL_MULTI_VALUED, T3_SENSITIVE, 90);

        register(definitions, "LANGUAGE_PROFICIENCY.TEST_SCORE", LANGUAGE_PROFICIENCY, STRING, HISTORICAL_MULTI_VALUED, T2_STANDARD_PERSONAL, 730);

        register(definitions, "FAMILY_DEPENDANTS.SPOUSE_NAME", FAMILY_DEPENDANTS, STRING, SINGLE_CURRENT, T3_SENSITIVE, null);
        register(definitions, "FAMILY_DEPENDANTS.DEPENDANT_CHILD", FAMILY_DEPENDANTS, STRING, PERMANENTLY_MULTI_VALUED, T3_SENSITIVE, null);

        register(definitions, "RELATIONSHIPS.SPONSOR", RELATIONSHIPS, STRING, HISTORICAL_MULTI_VALUED, T2_STANDARD_PERSONAL, null);

        register(definitions, "PROFESSIONAL_CREDENTIALS.LICENSE", PROFESSIONAL_CREDENTIALS, STRING, PERMANENTLY_MULTI_VALUED, T2_STANDARD_PERSONAL, 730);

        register(definitions, "LEGAL_ADMINISTRATIVE_HISTORY.DISCLOSURE", LEGAL_ADMINISTRATIVE_HISTORY, STRING, PERMANENTLY_MULTI_VALUED, T4_HIGHLY_SENSITIVE, null);

        register(definitions, "HEALTH_IMMIGRATION_RELEVANT.MEDICAL_EXAM_STATUS", HEALTH_IMMIGRATION_RELEVANT, STRING, HISTORICAL_MULTI_VALUED, T4_HIGHLY_SENSITIVE, 365);

        register(definitions, "REGULATORY_CONTEXT.REQUIREMENT_THRESHOLD", REGULATORY_CONTEXT, STRING, HISTORICAL_MULTI_VALUED, T1_ROUTINE, null);

        register(definitions, "CASE_PROCESS_INTERACTION.RECOMMENDATION_ACKNOWLEDGED", CASE_PROCESS_INTERACTION, BOOLEAN, PERMANENTLY_MULTI_VALUED, T1_ROUTINE, null);

        return definitions;
    }

    private static void register(
            Map<String, FactTypeDefinition> definitions,
            String factKey,
            FactCategory category,
            FactValueType valueType,
            FactCardinality cardinality,
            FactSensitivityTier tier,
            Integer stalenessDays
    ) {

        definitions.put(
                normalize(factKey),
                new FactTypeDefinition(factKey, category, valueType, cardinality, tier, stalenessDays)
        );
    }
}
