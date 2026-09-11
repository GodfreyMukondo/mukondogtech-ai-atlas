import type {
  ConflictResolutionType,
  Fact,
  FactCategory,
  FactConfidenceLevel,
} from "../types/fact";

/**
 * ============================================================================
 * FACT LABELS
 * ============================================================================
 *
 * Display helpers for raw Fact data (the Digital Twin view). Deliberately
 * NOT shared with ./assessmentOutcome's getCertaintyLabel: Fact confidence
 * and Requirement Evaluation certainty are two distinct concepts in this
 * system's architecture (a Fact's confidence never becomes, or is copied
 * into, an evaluation's certainty), so they get separate label functions
 * even though today's wording happens to read the same.
 * ============================================================================
 */

const CATEGORY_LABEL: Record<FactCategory, string> = {
  IDENTITY: "Identity",
  NATIONALITY_CITIZENSHIP: "Nationality & Citizenship",
  RESIDENCE: "Residence",
  TRAVEL_HISTORY: "Travel History",
  IMMIGRATION_STATUS: "Immigration Status",
  IMMIGRATION_HISTORY: "Immigration History",
  EDUCATION: "Education",
  EMPLOYMENT: "Employment",
  FINANCES: "Finances",
  LANGUAGE_PROFICIENCY: "Language Proficiency",
  FAMILY_DEPENDANTS: "Family & Dependants",
  RELATIONSHIPS: "Relationships",
  PROFESSIONAL_CREDENTIALS: "Professional Credentials",
  LEGAL_ADMINISTRATIVE_HISTORY: "Legal & Administrative History",
  HEALTH_IMMIGRATION_RELEVANT: "Health (Immigration-Relevant)",
  REGULATORY_CONTEXT: "Regulatory Context",
  CASE_PROCESS_INTERACTION: "Case Activity",
};

export function getCategoryLabel(category: FactCategory): string {
  return CATEGORY_LABEL[category] ?? category;
}

/**
 * "EMPLOYMENT.CURRENT_EMPLOYER" -> "Current Employer".
 *
 * The Fact key taxonomy is an open, backend-owned registry - this derives
 * a readable label generically rather than hardcoding every possible key.
 */
export function formatFactKeyLabel(factKey: string): string {
  const segment = factKey.includes(".") ? factKey.split(".").slice(1).join(".") : factKey;

  return segment
    .split("_")
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(" ");
}

/**
 * Renders a Fact's single populated value according to its valueType -
 * never guesses across the other, unpopulated value fields.
 */
export function formatFactValue(fact: Fact): string {

  switch (fact.valueType) {

    case "STRING":
      return fact.stringValue ?? "—";

    case "DATE": {
      if (!fact.dateValue) {
        return "—";
      }
      const date = new Date(fact.dateValue);
      return Number.isNaN(date.getTime())
        ? fact.dateValue
        : new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(date);
    }

    case "NUMBER":
      return fact.numberValue !== null && fact.numberValue !== undefined
        ? new Intl.NumberFormat().format(fact.numberValue)
        : "—";

    case "BOOLEAN":
      if (fact.booleanValue === null || fact.booleanValue === undefined) {
        return "—";
      }
      return fact.booleanValue ? "Yes" : "No";

    default:
      return "—";
  }
}

const FACT_CONFIDENCE_LABEL: Record<FactConfidenceLevel, string> = {
  HIGH: "High confidence",
  MODERATE: "Moderate confidence",
  LOW: "Low confidence",
  NOT_APPLICABLE: "Confidence not applicable",
};

/** Confidence in a FACT's accuracy - never to be read as, or substituted for, verification status. */
export function getFactConfidenceLabel(level: FactConfidenceLevel): string {
  return FACT_CONFIDENCE_LABEL[level];
}

const CONFLICT_RESOLUTION_LABEL: Record<ConflictResolutionType, string> = {
  EVIDENCE_WEIGHTED_AUTO: "Automatically resolved based on the strength of the evidence",
  TEMPORAL_AUTO: "Automatically resolved as a routine update over time",
  SAFE_AUTO: "Automatically resolved using the most recently recorded update",
  HUMAN_DECISION: "Resolved by an authorized case worker",
  APPLICANT_CONFIRMATION: "Confirmed by you - a self-reported statement, not independent verification",
};

/** How a FactConflict was resolved - never a fraud label, only a resolution mechanism. */
export function getConflictResolutionLabel(type: ConflictResolutionType): string {
  return CONFLICT_RESOLUTION_LABEL[type];
}
