# Evidence Intelligence Graph

Status: **implemented (backend + frontend), read-only**. Builds on the Fact Foundation, Requirement/Pathway engine, and Document pipeline without duplicating any of them.

## 1. Purpose

The Evidence Intelligence Graph is a traceability and explainability layer that answers "why does MukondoGTech AI believe this?" by making the chain

```
Document -> DocumentVersion -> EvidenceItem -> Fact -> RequirementEvaluation -> Requirement
```

traversable in both directions, alongside the existing conflict (`FactConflict`) and regulatory (`RegulatoryVersion`) relationships. It is **not** a second source of truth: every read recomputes from the existing Fact/Document/Requirement tables plus two new additive tables, exactly like the existing Digital Twin and Explainability views.

## 2. Ontology

| Concept | Backing |
|---|---|
| Document, Fact, FactConflict, Requirement, RequirementEvaluation, Pathway, PathwayAssessment, RegulatoryVersion | **Reused, unmodified** existing entities |
| DocumentVersion | **New** (`DOCUMENT_VERSIONS`) - one immutable, point-in-time state of a Document's content/extraction |
| EvidenceItem | **New** (`EVIDENCE_ITEMS`) - evidence as a first-class, lifecycle-bearing object, optionally enriching `FactEvidence` via a nullable `evidence_item_id` |
| EvidenceClaim, VerificationEvent, ApplicantConfirmation, CaseWorkerResolution | **Deliberately not created** - each is already fully representable by an existing discriminator (`FactStatus.PROPOSED`, `TimelineEventType.FACT_VERIFIED`, `ConflictResolutionType`) |
| RegulatorySource | **Deliberately not created** - source identity remains inline on `RegulatoryVersion`, as it already was |

## 3. Relationships

`HAS_VERSION` (Document→DocumentVersion), `PRODUCES` (DocumentVersion→EvidenceItem, or Document→Evidence directly when no EvidenceItem exists yet), `SUPPORTS_FACT` (Evidence↔Fact via `FactEvidence`), `CONFLICTS_WITH` (Fact↔Fact via `FactConflict`), `DEPENDS_ON_FACT` (RequirementEvaluation→Fact via `RequirementEvaluationFact`), `EVALUATED_UNDER` (RequirementEvaluation→Requirement). No `CONTRADICTS_FACT` edge exists directly from evidence to a Fact it did not produce - contradiction is always mediated through two Facts already in a `FactConflict`.

## 4. Provenance

Every node in the graph carries the existing provenance/confidence/verification fields it already had (`Fact.provenanceType`, `.confidenceScore`, `.isVerified`; `EvidenceItem.extractionConfidence`, `.directness`). Nothing is re-derived or aliased.

## 5. Evidence strength

Kept as separate, never-merged concepts:

- **Confidence** — `Fact.confidenceScore/Level` (`ConfidenceCalculator`)
- **Verification** — `Fact.isVerified` overlay, set only by `FactLifecycleService.verify()`
- **Evidence strength** — `EvidenceItem.extractionConfidence`/`directness`, a vector, never collapsed into one score
- **Eligibility** — `RequirementEvaluation.outcome`, produced only by `LogicEvaluationService`

`Document.fraudDetected`/`riskLevel` (legacy, explicitly non-forensic per its own code comments) are **never** surfaced by this feature as proof of anything - they remain that subsystem's own signal, untouched.

## 6. Conflict handling

Rendered via the existing `FactConflict` record - both competing Facts remain visible as separate graph nodes, connected by a `CONFLICTS_WITH` edge carrying `status`/`resolutionType` metadata. Nothing is hidden or auto-resolved by this feature.

## 7. Temporal validity

`DocumentVersion` carries the document's own issue/expiry dates, independent of the Fact's `effectiveFrom/To` window it happens to support - see `EvidenceItemResponse.SourceData`.

## 8. Evidence lifecycle

`DISCOVERED → EXTRACTED → {VALIDATION_FAILED | CANDIDATE} → {LINKED | REJECTED} → {SUPERSEDED | REJECTED}`, enforced by `EvidenceItemLifecycleService`'s guarded transition map (mirrors `FactLifecycleService` exactly). `REJECTED` is restricted, by convention, to a case worker/administrator caller - never the applicant.

## 9. API

All endpoints are `GET`-only and require no `SecurityConfig` change (fall through to the existing fail-closed `.anyRequest().authenticated()` rule):

- `GET /api/evidence-graph/facts/{factId}` — node/edge graph for one Fact (evidence, documents, conflicts, one upward hop into requirement evaluations)
- `GET /api/evidence-graph/evidence-items/{evidenceItemId}` — detail panel (source data / system interpretation / derived conclusion, kept separate)
- `GET /api/evidence-graph/pathway-assessments/{assessmentId}/full-trace` — the full chain, composed around the existing `ExplainabilityService.explain(...)`

Authorization is checked once, at the entry id the caller supplies, via the existing `FactAuthorizationService` — no new authorization surface.

## 10. Database

Migration `V13__add_evidence_intelligence_graph.sql`: two new tables (`document_versions`, `evidence_items`), one additive nullable column (`fact_evidence.evidence_item_id`), and one closed indexing gap (`fact_conflicts.fact_key`). No existing table's columns are altered or removed. Validated by a full Spring context load against a live Oracle instance.

## 11. Frontend

`/dashboard/evidence-graph?fact={id}`, reached via a "Trace this Fact" link on `FactCard` (Digital Twin, Fact Conflict pages) and on each contributing Fact in the Pathway Explanation page. Rendered with `@xyflow/react` (React Flow) - pan/zoom/minimap/fit-to-view, node-type filtering, label search, and a detail panel for evidence nodes.

## 12. Limitations / deliberately out of scope

- **The Document extraction pipeline is not wired to automatically create `EvidenceItem`/`DocumentVersion` rows.** No caller of `FactService.proposeFact` exists anywhere in the codebase today - this is a pre-existing gap, not introduced or closed by this feature. The graph works immediately with real data by degrading gracefully to `FactEvidence`-only nodes when no `EvidenceItem` exists.
- No write endpoints were added for evidence lifecycle transitions (extraction, rejection) - only the guarded service-layer state machine exists; wiring a case-worker-facing UI/endpoint for `reject(...)` is a natural next increment.
- N-way conflict clustering (grouping every conflict touching one `factKey` into a single view) is not implemented as a separate endpoint in this pass - the per-Fact graph already surfaces every conflict that Fact participates in.
- No dedicated graph database was introduced - the traversal is a fixed-depth relational query set, matching this graph's actual shape and scale.

## 13. Future extension points

This graph is the foundational read layer for planned future work (none implemented here): Regulatory Impact Engine (query "which Requirements cite this RegulatoryVersion"), Risk Intelligence (a strength/corroboration vector to reason over, kept separate from verification), a Future Simulator (already excluded via `SIMULATION` provenance), Proactive Intelligence/Next-Best-Action (the missing-evidence classification this graph exposes), and a controlled multi-agent architecture (agents may populate `EvidenceItem` candidates; they never write Fact status, verification, or conflict resolution directly).
