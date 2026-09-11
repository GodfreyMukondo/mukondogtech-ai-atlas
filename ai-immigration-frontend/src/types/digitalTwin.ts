import type { Fact, FactConflict } from "./fact";

/**
 * ============================================================================
 * DIGITAL TWIN TYPES
 * ============================================================================
 *
 * Mirrors the backend's DigitalTwinResponse exactly
 * (GET /api/twin/{subjectUserId}):
 *
 *   subjectUserId, generatedAt, currentFacts[], openConflicts[], note
 *
 * The Digital Twin is a PURE LIVE READ PROJECTION on the backend - it is
 * recomputed from FACTS/FACT_CONFLICTS on every request and is never
 * itself persisted there. This type must not become the seed of a second,
 * frontend-side source of truth either: nothing built on this type may
 * cache, diff, or persist Fact data beyond the lifetime of one rendered
 * view - every screen re-fetches from the backend.
 *
 * Reuses Fact/FactConflict from ./fact rather than redefining them - the
 * same canonical Fact vocabulary the Explainability view uses.
 * ============================================================================
 */
export interface DigitalTwin {
  subjectUserId: number;
  generatedAt: string;
  currentFacts: Fact[];
  openConflicts: FactConflict[];
  note: string;
}
