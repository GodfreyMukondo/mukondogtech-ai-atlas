package com.godfrey.ai_immigration_document_analyzer.fact.policy;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;

import java.util.EnumMap;
import java.util.Map;

import static com.godfrey.ai_immigration_document_analyzer.fact.policy.ConflictResolutionClass.*;

/**
 * ============================================================================
 * CONFLICT POLICY REGISTRY
 * ============================================================================
 *
 * Encodes the approved field-level Conflict Auto-Resolution Policy (Table
 * C) as typed reference data. Field-level overrides take precedence over
 * the category default; every category has a documented default so an
 * unmapped-but-known factKey never falls through to an implicit decision.
 *
 * CRITICAL, cross-cutting, enforced by {@code FactConflictService} rather
 * than by this registry alone: no resolution mechanism selected here may
 * ever result in a Fact status resembling "fraud". CONFLICT DETECTED is
 * never FRAUD DETECTED.
 * ============================================================================
 */
public final class ConflictPolicyRegistry {

    /** Winning side must clear this confidence floor for any *_AUTO class to apply. */
    public static final double AUTO_RESOLUTION_CONFIDENCE_FLOOR = 0.6;

    /** AND beat the losing side by at least this much. Both conditions required. */
    public static final double AUTO_RESOLUTION_MIN_CONFIDENCE_GAP = 0.2;

    /** Confidence ceiling forced on both sides of any OPEN conflict, overriding the raw formula. */
    public static final double CONTESTED_CONFIDENCE_CAP = 0.4;

    private static final Map<FactCategory, ConflictResolutionClass> CATEGORY_DEFAULTS = buildCategoryDefaults();

    private static final Map<String, ConflictResolutionClass> FIELD_OVERRIDES = buildFieldOverrides();

    private ConflictPolicyRegistry() {
    }

    public static ConflictResolutionClass resolutionClassFor(FactCategory category, String factKey) {

        String normalizedKey = factKey == null ? null : factKey.trim().toUpperCase();

        ConflictResolutionClass override = FIELD_OVERRIDES.get(normalizedKey);

        if (override != null) {
            return override;
        }

        return CATEGORY_DEFAULTS.getOrDefault(category, ALWAYS_HUMAN_RESOLUTION);
    }

    private static Map<FactCategory, ConflictResolutionClass> buildCategoryDefaults() {

        Map<FactCategory, ConflictResolutionClass> defaults = new EnumMap<>(FactCategory.class);

        defaults.put(FactCategory.IDENTITY, ALWAYS_HUMAN_RESOLUTION);
        defaults.put(FactCategory.NATIONALITY_CITIZENSHIP, ALWAYS_HUMAN_RESOLUTION);
        defaults.put(FactCategory.RESIDENCE, EVIDENCE_WEIGHTED_AUTO_RESOLUTION_ALLOWED);
        defaults.put(FactCategory.TRAVEL_HISTORY, TEMPORAL_AUTO_RESOLUTION_ALLOWED);
        defaults.put(FactCategory.IMMIGRATION_STATUS, ALWAYS_HUMAN_RESOLUTION);
        defaults.put(FactCategory.IMMIGRATION_HISTORY, NEVER_AUTO_RESOLVE);
        defaults.put(FactCategory.EDUCATION, EVIDENCE_WEIGHTED_AUTO_RESOLUTION_ALLOWED);
        defaults.put(FactCategory.EMPLOYMENT, TEMPORAL_AUTO_RESOLUTION_ALLOWED);
        defaults.put(FactCategory.FINANCES, EVIDENCE_WEIGHTED_AUTO_RESOLUTION_ALLOWED);
        defaults.put(FactCategory.LANGUAGE_PROFICIENCY, SAFE_AUTO_RESOLUTION_ALLOWED);
        defaults.put(FactCategory.FAMILY_DEPENDANTS, ALWAYS_HUMAN_RESOLUTION);
        defaults.put(FactCategory.RELATIONSHIPS, EVIDENCE_WEIGHTED_AUTO_RESOLUTION_ALLOWED);
        defaults.put(FactCategory.PROFESSIONAL_CREDENTIALS, EVIDENCE_WEIGHTED_AUTO_RESOLUTION_ALLOWED);
        defaults.put(FactCategory.LEGAL_ADMINISTRATIVE_HISTORY, NEVER_AUTO_RESOLVE);
        defaults.put(FactCategory.HEALTH_IMMIGRATION_RELEVANT, NEVER_AUTO_RESOLVE);
        defaults.put(FactCategory.REGULATORY_CONTEXT, EVIDENCE_WEIGHTED_AUTO_RESOLUTION_ALLOWED);
        defaults.put(FactCategory.CASE_PROCESS_INTERACTION, NOT_APPLICABLE);

        return defaults;
    }

    private static Map<String, ConflictResolutionClass> buildFieldOverrides() {

        Map<String, ConflictResolutionClass> overrides = new java.util.HashMap<>();

        // Explicit per-field overrides where the category default is too coarse.
        overrides.put("IDENTITY.PASSPORT_NUMBER", ALWAYS_HUMAN_RESOLUTION);
        overrides.put("RESIDENCE.CURRENT_ADDRESS", SAFE_AUTO_RESOLUTION_ALLOWED);

        return overrides;
    }
}
