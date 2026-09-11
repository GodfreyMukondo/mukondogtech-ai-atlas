# MukondoGTech AI Immigration Platform — Phase 4 Architecture Assessment

**Document type:** Read-only architecture assessment and Phase 4 planning reference.
**Status:** Documentation only. No source code, configuration, database file, or Git state was modified in producing this document. Nothing was staged or committed.
**Audience:** A developer picking up this codebase cold should be able to read only this file and understand the current architecture and the proposed Phase 4 without needing prior conversation context.

Backend package root: `src/main/java/com/godfrey/ai_immigration_document_analyzer/`
Frontend root: `ai-immigration-frontend/src/`

Committed checkpoints so far:
- `7cd87be6` — `feat(case-intelligence): complete Phase 2 foundation`
- `d44a2f9` — `feat(evidence-graph): complete Phase 3 advanced evidence graph`

---

## 1. Executive Summary

**Current platform maturity:** The backend has a genuinely mature, layered immigration-intelligence core. Four foundational layers are built and tested: (1) a **Fact Foundation** (`fact/`) with typed, provenance-tagged, temporally-scoped facts, conflict detection, confidence scoring, and an IDOR-safe authorization boundary; (2) a **Requirement/Pathway/Regulatory engine** (`requirement/`) with a closed data-driven logic grammar, Kleene three-valued evaluation, and versioned regulatory sourcing; (3) **Case Intelligence** (`caseintelligence/`, Phase 2) which composes the above into readiness scoring, a requirement-evidence matrix, missing-evidence detection, contradiction surfacing, and timeline intelligence; (4) an **Advanced Evidence Graph** (`evidencegraph/`, Phase 3) providing forward/backward traceability from Fact ↔ Evidence ↔ Document ↔ Requirement ↔ RegulatoryVersion, rendered as an interactive graph on the frontend.

**What Phase 2 delivered:** `CaseIntelligenceService`/`CaseOverviewService` — case readiness percentage, the Requirement→Evidence Matrix, missing-evidence detection with necessity/severity, non-forensic contradiction surfacing (reusing `FactConflict`), and timeline intelligence with gap/overlap annotations. 30 files, 3,853 insertions, 0 deletions, zero new database migrations.

**What Phase 3 delivered:** Two new read-time traversal methods on the pre-existing `EvidenceGraphService` (`graphForDocument`, `graphForRequirementEvaluation`), one new `GraphNodeType.CONFLICT` enum value, wiring `RegulatoryVersion` traceability into the graph (the previously-declared-but-unused `VERSIONED_UNDER` edge), and two new "Trace evidence" entry points in the frontend. 13 files, ~1,081 insertions, 0 deletions, zero new database migrations, zero new services/controllers (pure extension).

**What remains:** Every capability built so far activates only *after* a user has already chosen one specific pathway and started supplying facts/documents. There is no answer yet to "which pathway should I even pursue," no hypothetical/what-if evaluation, no proactive regulatory-change or deadline intelligence, no case-aware AI assistant, and no frontend for the already-built case-worker authorization boundary. Additionally, one confirmed pre-existing defect (two frontend pages calling a non-existent `/admin/audit-logs*` endpoint) was found during this audit — not to be fixed under this documentation-only task.

**Strongest Phase 4 candidate: Immigration Pathway Discovery & Ranking.** It is the most upstream unanswered question in the entire product funnel — everything else assumes a pathway has already been picked. It can be built almost entirely by orchestrating existing, unmodified services (`PathwayAssessmentService`, `LogicEvaluationService`, `TemporalFactResolver`, `CaseIntelligenceService`'s readiness formula) rather than inventing new evaluation logic, keeping engineering risk low relative to product value. Full justification in §21.

---

## 2. Complete Capability Matrix

| ID | Capability | Status | Existing Implementation | Missing | Phase |
|----|------------|--------|--------------------------|---------|-------|
| A | Immigration Case Intelligence Engine | **COMPLETE** | Backend: `caseintelligence/service/CaseIntelligenceService.java`, `CaseOverviewService.java`. Frontend: `pages/dashboard/CaseIntelligencePage.tsx`, `CaseTimelinePage.tsx`. API: `GET /api/cases/pathway-assessments/{assessmentId}/intelligence`, `GET /api/cases/{subjectUserId}/{contradictions,timeline,signals}`. DB: no dedicated tables — pure read-time composition over `RequirementEvaluation`/`Fact`/`FactConflict`. Tests: covered in Phase 2's 30-file/3,853-line commit (unit + Oracle integration, part of the 239-test full suite). | Multi-pathway comparison (see G) | — (complete) |
| B | Evidence Graph + Trace this Fact | **COMPLETE** | Backend: `evidencegraph/service/EvidenceGraphService.java` (`graphForFact`, `graphForDocument`, `graphForRequirementEvaluation`, `fullTrace`, `getEvidenceItemDetail`), `EvidenceGraphController.java`. Frontend: `pages/dashboard/EvidenceGraphPage.tsx`, `components/evidenceGraph/*` (Canvas/Node/DetailPanel/graphLayout, built on `@xyflow/react`), `components/fact/FactCard.tsx` ("Trace this Fact" entry point, pre-existing, untouched by Phase 3). API: `GET /api/evidence-graph/{facts/{id}, documents/{id}, requirement-evaluations/{id}, evidence-items/{id}, pathway-assessments/{id}/full-trace}`. DB: `evidencegraph/entity/EvidenceItem`, `DocumentVersion` (Flyway V13). Tests: 21/21 in `EvidenceGraphServiceTest` + 1 Oracle integration test (Phase 3). | PAGE/REGION-level provenance (no page/coordinate data exists in the model — deliberately not fabricated) | — (complete) |
| C | Requirement → Evidence Matrix | **COMPLETE** | `CaseIntelligenceService.buildReadiness()` / `toMatrixRow()` → `RequirementEvidenceMatrixRowResponse` (support status, outcome, certainty, regulatory traceability, missing fact keys), surfaced in `CaseIntelligenceResponse.matrix`. Frontend: rendered within `CaseIntelligencePage.tsx`. | — | — (complete) |
| D | Missing Evidence Detection | **COMPLETE** | `CaseIntelligenceService.buildMissingEvidence()` / `toMissingEvidenceItem()` → `MissingEvidenceItemResponse` with `EvidenceNecessity` (REQUIRED/SUPPORTING) and `CaseIssueSeverity`. | Predictive/likelihood-ranked prioritization beyond the deterministic necessity/severity split (see I) | — (complete as detection; I is the gap) |
| E | Cross-Document Contradiction Detection | **COMPLETE (explicitly non-forensic)** | `CaseOverviewService.getContradictions()` — surfaces open `FactConflict` rows (via `DigitalTwinResponse.openConflicts()`) plus a read-time-only period-overlap check (`computeTimeline` → `PotentialOverlapContradiction`) for historical multi-valued facts. Frontend: `pages/dashboard/FactConflictDetailPage.tsx`, surfaced in `CaseIntelligencePage.tsx`. API: `GET /api/cases/{subjectUserId}/contradictions`. | Cross-document textual/semantic contradiction detection beyond structured Fact conflicts | — (complete for structured facts) |
| F | Immigration Timeline Intelligence | **COMPLETE** | `CaseOverviewService.getTimeline()` / `computeTimeline()` → `CaseTimelineEventResponse` list with `gapDaysBeforeThisEntry`/`overlapDaysWithPreviousEntry`, filtered to `TIMELINE_CATEGORIES` (EMPLOYMENT, EDUCATION, RESIDENCE, TRAVEL_HISTORY, IMMIGRATION_STATUS, IMMIGRATION_HISTORY). Frontend: `pages/dashboard/CaseTimelinePage.tsx`. API: `GET /api/cases/{subjectUserId}/timeline`. | — | — (complete) |
| G | Pathway Discovery & Ranking | **FOUNDATION ONLY** | Backend: `requirement/controller/PathwayController.java` (read-only catalogue: `GET /api/pathways`, `GET /api/pathways/{id}`); `Pathway` entity already carries `jurisdiction`/`category` fields usable for filtering. Frontend: `pages/dashboard/RequestPathwayAssessmentPage.tsx` requires the user to already know which pathway to pick. | No endpoint/service evaluates a subject against every published pathway and ranks them; no comparison UI | **RECOMMENDED PHASE 4** |
| H | What-If / Scenario Simulation | **NOT IMPLEMENTED** | None. Grepped `hypothetical\|whatIf\|what-if\|scenario\|simulat` — 5 hits, all unrelated (`ProvenanceTrust`, `ConfidenceCalculator`, `FactConflict`, `FactConfidenceLevel`, `FactProvenanceType`). | A hypothetical-fact override/merge path into `TemporalFactResolver`/`PathwayAssessmentService`; a UI for entering "what if I had X" | Phase 5 (after Discovery) |
| I | Missing Evidence Prediction & Prioritization | **PARTIALLY IMPLEMENTED (prioritization only)** | `MissingEvidenceItemResponse` carries `EvidenceNecessity` + `CaseIssueSeverity` — a deterministic priority (mandatory→CRITICAL, optional→MEDIUM), not predictive. | A predictive/likelihood-ranked model of which missing evidence most affects outcome | Phase 6 |
| J | Regulatory Change Detection / Versioning | **FOUNDATION ONLY (versioning solid, detection absent)** | `requirement/entity/RegulatoryVersion.java` — `RegulatoryVerificationStatus` (UNVERIFIED_INGESTION → HUMAN_VERIFIED → AUTHORITATIVE_CONFIRMED), `effectiveFrom`/`effectiveTo`, `supersedesVersionId`/`supersededByVersionId`. Admin CRUD: `RegulatoryVersionAdminController.java` (`POST/GET /api/admin/regulatory-versions`). Frontend: part of admin Immigration Rules tooling. | No scraper/importer/scheduled job (grepped `scraper\|ingest\|ChangeDetection` — zero hits); purely manual admin entry today | Phase 7 |
| K | Regulatory Impact Alerts | **NOT IMPLEMENTED** | None — no code compares a new `RegulatoryVersion` against affected subjects' evaluations; `Notification` entity has no type for this. | Impact-comparison logic + a `Notification` type + a scheduled trigger | Phase 7 (with J) |
| L | Document Lifecycle Intelligence | **PARTIALLY IMPLEMENTED** | `evidencegraph/entity/DocumentVersion.java` — `documentIssueDate`, `documentExpiryDate`, `supersededByVersionId`, `extractionMethod` (immutable version model, Flyway V13). | Expiry is read-only display only (`EvidenceGraphService.toEvidenceItemResponse`) — never computes an "expiring soon" signal, never surfaced in `CaseSignalsResponse`; legacy `entity/Document.java` has no lifecycle fields at all (only `uploadStatus`, `riskLevel`, `fraudDetected`) | Phase 6 (Deadline Intelligence) |
| M | Document Relationship Intelligence | **COMPLETE (as part of Evidence Graph)** | `EvidenceGraphService.graphForDocument()` — Document → DocumentVersion → EvidenceItem → Fact → Conflict/RequirementEvaluation via `GraphRelationshipType.HAS_VERSION`/`PRODUCES`/`SUPPORTS_FACT`. | No document-to-document relationship type beyond version supersession (e.g. "duplicate-of") | — (complete for the modeled relationships) |
| N | Anomaly Detection | **PARTIALLY IMPLEMENTED (aggregation, not detection)** | `CaseOverviewService.getSignals()` / `computeRiskBand()` rolls up pre-existing `fraud/FraudRuleEngine.java`/`FraudScoringEngine.java` flags (legacy `Document.fraudDetected`/`riskLevel`) plus open Fact conflicts, timeline overlaps, and rejected/validation-failed `EvidenceItem`s into one `CaseRiskBand` (NORMAL/LOW/MEDIUM/HIGH/CRITICAL). API: `GET /api/cases/{subjectUserId}/signals`. Explicitly documented as never a fraud determination. | New anomaly-detection logic (today it's purely a rollup of pre-existing signals) | Not roadmapped — depends on product need |
| O | AI Confidence + Human Verification | **COMPLETE** | `fact/entity/FactConfidenceLevel.java`, `fact/service/ConfidenceCalculator.java`, `Fact.isVerified`/`confidenceScore`, `fact/entity/VerificationMethod.java`, `requirement/service/EvaluationCertaintyCalculator.java` + `EvaluationCertaintyLevel` (requirement-evaluation-level certainty banding tied to `RegulatoryVerificationStatus`). | — | — (complete) |
| P | Case-Aware AI Assistant | **NOT IMPLEMENTED (generic chatbot only)** | `service/ChatService.java` — Session → Conversation History → RAG retrieval over `KnowledgeDocument`/embeddings (`RagRetrievalService`, `EmbeddingService`, `DocumentEmbedding`) → `PromptBuilderService` → `LlmService`. Frontend: `components/chatbot/AIChatWidget.tsx`/`AIChatWidgetContent.tsx`, `pages/ChatPage.tsx`. API: `POST /api/chat`. | Zero access to a subject's `Fact`/`PathwayAssessment`/`CaseIntelligenceResponse`/`Document` — no `subjectUserId` parameter anywhere in `ChatService.ask()`; answers generic knowledge-base questions only | Phase 8 |
| Q | Deadline Intelligence | **NOT IMPLEMENTED** | None. Grepped `deadline\|expiry\|expiresAt\|expiryDate\|reminder` — only unrelated hits (`DocumentVersion.documentExpiryDate` display-only, `PasswordResetToken`/JWT expiry). `RequirementEvaluationOutcome.EXPIRED` enum value exists, implying the concept is modeled but unused proactively. | Date-tracking/reminder job; `Notification` type for expiring documents/visa deadlines/re-verification windows | Phase 9 |
| R | Submission Readiness Auditor | **COMPLETE (as "Case Readiness")** | `CaseIntelligenceService.buildReadiness()` → `CaseReadinessResponse` (requirementCoveragePercent/evidenceCoveragePercent/consistencyPercent/overallReadinessPercent + `outstandingIssues`), rolled up further in `CaseOverviewSummaryResponse`. | — (a separate "readiness auditor" page/endpoint would be duplicative) | — (complete, do not duplicate) |
| S | Professional / Consultant Workspace | **FOUNDATION ONLY — backend only** | `entity/Role.java` (`CASE_WORKER`, distinct from `ADMIN`); `fact/entity/CaseAssignment.java` (caseWorkerUserId/subjectUserId/active); `FactAuthorizationService` branches on `AccessorType.CASE_WORKER` + `hasActiveAssignment()` throughout every `assertCan*` method. | Zero frontend surface — grepped `CaseWorker\|assigned case` across `ai-immigration-frontend/src` with zero matches; no multi-client list view, no case-worker landing page, no Sidebar entry | Phase 10 |
| T | Full Audit Trail | **PARTIALLY IMPLEMENTED — one page orphaned (defect)** | `fact/entity/FactAccessAuditLog.java` + `fact/service/FactAccessAuditService.java` (records every granted/denied Fact access with accessor type, purpose, sensitivity tier); `fraud/audit/FraudAuditLogger.java`/`FraudAuditEntity.java` (fraud-rule trigger logging). | No admin UI for `FactAccessAuditLog` at all; separately, `pages/admin/AuditLogsPage.tsx` and `pages/settings/SecuritySettingsPage.tsx` both call `/admin/audit-logs` and `/admin/audit-logs/stats` — **confirmed zero matching backend endpoint anywhere** (see §16 defect table) | Not roadmapped as a "phase" — a bug fix, tracked separately |
| U | Admin / Regulatory Management | **COMPLETE** | `requirement/controller/PathwayAdminController.java`, `RequirementAdminController.java`, `RegulatoryVersionAdminController.java`, `FactTypeAdminController.java` — full CRUD/lifecycle for Pathway (`PathwayLifecycleService`), Requirement (`RequirementLifecycleService`), append-only RegulatoryVersion. Frontend: `pages/admin/PathwayManagementPage.tsx`, `RequirementManagementPage.tsx`. | — | — (complete) |

---

## 3. Backend Architecture Inventory

### Package layout (verified)

```
analytics/  caseintelligence/  config/  controller/  dto/  entity/  events/
evidencegraph/  exception/  fact/  fraud/  messaging/  repository/  requirement/
security/  service/  specification/  stream/  util/  worker/
```

### 3.1 Fact Foundation (`fact/`)

- **Purpose:** Atomic, provenance-tagged, temporally-scoped Facts as the single source of truth about a subject, with an explicit lifecycle and conflict model.
- **Entry point:** `fact/controller/FactController.java` (`/api/facts`), `fact/controller/DigitalTwinController.java` (`/api/twin/{subjectUserId}`).
- **Data flow:** Document/manual input → `Fact` (typed value, `FactProvenanceType`, `FactStatus` lifecycle) → `FactEvidence` (Fact↔Document/EvidenceItem link) → `FactConflict` (pairwise conflict detection) → `DigitalTwinProjectionService` (live read projection, no stored duplicate).
- **Persistence:** `Fact`, `FactEvidence`, `FactConflict`, `FactAccessAuditLog`, `CaseAssignment` (Flyway V10, fixed in V11 for numeric column precision).
- **Authorization:** `FactAuthorizationService.assertCanView/assertCanCreate/assertCanVerifyOrResolve/assertCanApplicantConfirmConflict` — the single retrieval-boundary enforcement point for the whole Fact/Requirement/CaseIntelligence/EvidenceGraph domain (see §7).
- **Dependencies:** none upstream (foundational layer).
- **Tests:** part of the original 92-test Fact Foundation suite (commit `a7aed561`) plus subsequent additions.
- **Limitations:** Digital Twin is deliberately scoped to the live/current projection only — point-in-time historical reconstruction is handled separately by `TemporalFactResolver` (see 3.3), not by the Digital Twin itself.

### 3.2 Requirement / Pathway / Regulatory (`requirement/`)

- **Purpose:** Versioned, data-driven Requirement/Pathway definitions expressed in a closed logic grammar (AND/OR/NOT/predicates/derived functions — never executable code), evaluated via three-valued (Kleene) logic.
- **Entry point:** `requirement/controller/PathwayController.java` (catalogue), `PathwayAssessmentController.java` (assess/retrieve), `RequirementController.java` (per-requirement evaluation), admin controllers (`PathwayAdminController`, `RequirementAdminController`, `RegulatoryVersionAdminController`, `FactTypeAdminController`).
- **Data flow:** `RegulatoryVersion` (source of truth for a rule, with verification status and effective dates) → `Requirement` (references a `RegulatoryVersion`, composed of a logic tree) → `Pathway` (composes Requirements via the same logic grammar) → `PathwayAssessmentService.assess()` → `RequirementEvaluationService.evaluateAndPersist()` per referenced requirement → `LogicEvaluationService.evaluate()` walks the composition tree → `RequirementEvaluationOutcome` (9-value Kleene outcome).
- **Persistence:** `RegulatoryVersion`, `Requirement`, `Pathway`, `RequirementEvaluation`, `PathwayAssessment`, `PathwayAssessmentRequirementEvaluation`, `RequirementEvaluationFact`, `RequirementEvaluationConflict`, `RequirementFactBinding` (Flyway V12, seed data in V14).
- **Authorization:** reuses `FactAuthorizationService` for subject-scoped reads; admin endpoints gated by `hasRole("ADMIN")` at the `SecurityConfig` layer (`/api/admin/**`).
- **Dependencies:** Fact Foundation (`TemporalFactResolver` resolves Facts as of a point in time), `EvaluationCertaintyCalculator` (confidence banding tied to `RegulatoryVerificationStatus`).
- **Tests:** 36 unit tests covering Kleene combination rules, circular-dependency detection, and requirement-reuse memoization (commit `ba681d0`); full suite validated against live Oracle with Flyway V12.
- **Limitations:** `PathwayAssessmentService.assess()` always persists a full `PathwayAssessment` + N `RequirementEvaluation` rows — there is no non-persisting/transient evaluation mode today (directly relevant to Phase 4, see §9–§10).

### 3.3 Temporal Fact Resolution

- **Purpose:** Reconstruct Fact state as of an arbitrary point in time (e.g. "was this person eligible on date X") without touching the live Digital Twin projection.
- **Entry point:** called internally by `PathwayAssessmentService.assess()` — not exposed as its own controller endpoint.
- **Data flow:** `TemporalFactResolver.resolve(actor, subjectUserId, assessmentDate)` reconstructs an `EvaluationFactView` from `Fact.effectiveFrom`/`effectiveTo` windows via the unmodified `FactRepository`, called **exactly once per `assess()` invocation** and reused across every requirement in that pathway via `RequirementEvaluationService.EvaluationRun`.
- **Persistence:** read-only — reconstructs from existing `Fact` rows, no new persistence.
- **Authorization:** the resolved `actor`/`subjectUserId` pair flows through from `assess()`'s own authorization check — no separate check inside the resolver.
- **Dependencies:** `FactRepository` only.
- **Tests:** covered by the Phase 1/requirement-layer test suites.
- **Limitations:** designed for one subject/one date per call — looping it across multiple pathways in a discovery feature means calling it multiple times for the same subject unless refactored (see §12).

### 3.4 Requirement Evaluation memoization

- `RequirementEvaluationService.EvaluationRun` (a `static final class` holding `final Map<Long, RequirementEvaluation> computed = new HashMap<>()`) ensures a Requirement referenced twice in one Pathway's composition tree is evaluated and persisted only once per `assess()` call (verified at `RequirementEvaluationService.java:92-161`). This memoization is scoped to a single `assess()` call — it does **not** span multiple pathways or multiple discovery iterations.

### 3.5 Explainability

- **Purpose:** Compose a human-readable explanation of *why* an assessment reached its outcome, read-time only, never a second persisted copy.
- **Entry point:** `PathwayAssessmentController.java` → `GET /api/pathways/assessments/{assessmentId}/explanation`.
- **Data flow:** `ExplainabilityService.explain()` walks Pathway → Requirement → RequirementEvaluation → Fact → Evidence → RegulatoryVersion purely by following already-persisted FKs.
- **Persistence:** none — pure read-time composition.
- **Authorization:** reuses the same `FactAuthorizationService.assertCanView` boundary via the `PathwayAssessment.subjectUserId`.
- **Dependencies:** everything in 3.2.
- **Tests:** part of the Requirement/Pathway suite.
- **Limitations:** none identified — this is the canonical explanation composer and is explicitly reused (not duplicated) by both Case Intelligence and Evidence Graph's `fullTrace`.

### 3.6 Case Intelligence (`caseintelligence/`) — Phase 2

- **Purpose:** Aggregate readiness, evidence-matrix, missing-evidence, contradiction, timeline, and risk-signal views for one subject's case.
- **Entry point:** `caseintelligence/controller/CaseIntelligenceController.java` (`/api/cases/**`).
- **Data flow:** `CaseIntelligenceService` composes over `ExplainabilityService.explain()` for the matrix/readiness views; `CaseOverviewService` independently derives contradictions/timeline/signals from `FactConflict`, `Fact`, and legacy fraud signals.
- **Persistence:** none — pure read-time composition (explicit design goal, stated in the class Javadoc as "PATHWAY-AGNOSTIC BY CONSTRUCTION").
- **Authorization:** `FactAuthorizationService.assertCanView` at entry to every method.
- **Dependencies:** `ExplainabilityService`, `Fact`/`FactConflict`, `RequirementEvaluation`.
- **Tests:** part of the 30-file/3,853-line Phase 2 commit; included in the full 239-test suite validated against Oracle.
- **Limitations:** operates on a single already-chosen Pathway/Assessment — it has no multi-pathway view (this is precisely the Phase 4 gap).

### 3.7 Evidence Graph (`evidencegraph/`) — pre-existing, extended in Phase 3

- **Purpose:** Explainability layer over Fact/Evidence/Document/Requirement/RegulatoryVersion relationships, rendered as a node/edge graph.
- **Entry point:** `evidencegraph/controller/EvidenceGraphController.java` (`/api/evidence-graph/**`).
- **Data flow:** `EvidenceGraphService` — authorization checked exactly once at the entry id, every subsequent hop resolved via FK from already-authorized rows (never a second id accepted mid-traversal). `graphForFact` (pre-existing) and Phase 3's `graphForDocument`/`graphForRequirementEvaluation` all reuse the same private helpers (`addEvidenceSubgraph`, `addConflictSubgraph`, `addRequirementEvaluationSubgraph`, now also `addRegulatoryVersionSubgraph`).
- **Persistence:** `EvidenceItem`, `DocumentVersion` (Flyway V13); the graph response itself (`EvidenceGraphResponse`) is recomputed fresh on every call, never cached.
- **Authorization:** `FactAuthorizationService.assertCanView`, using `Document.getUserId()` or `RequirementEvaluation.getSubjectUserId()` as the entry subject depending on which traversal was requested.
- **Dependencies:** Fact Foundation, Requirement/Pathway layer.
- **Tests:** 21/21 `EvidenceGraphServiceTest` + 1 Oracle integration test (Phase 3); full suite 239/239 passing.
- **Limitations:** no `PAGE`/`REGION` node types — `EvidenceItem.sourceSnippet` is text-only, no page/coordinate provenance exists in the model (deliberately not fabricated, per the anti-fabrication mandate).

### 3.8 Fraud subsystem (`fraud/`) — pre-existing, unrelated to Phases 2/3

- **Purpose:** Rule-based, legacy fraud/risk scoring on `Document` uploads.
- **Entry point:** `controller/FraudController.java` (`/api/fraud`).
- **Data flow:** `FraudRuleEngine` / `FraudScoringEngine` set `Document.fraudDetected`/`riskLevel`; `FraudAuditLogger`/`FraudAuditEntity` log rule triggers.
- **Persistence:** fields on `Document`, plus a separate `FraudAuditEntity`.
- **Authorization:** standard authenticated/admin boundary via `SecurityConfig`.
- **Dependencies:** none of the Fact/Requirement/Evidence-Graph architecture — a legacy, parallel signal source that Case Intelligence's `computeRiskBand()` explicitly rolls up as a *non-forensic* input, never re-derives.
- **Limitations:** explicitly documented everywhere it's touched (Evidence Graph's `documentNode`, Case Intelligence's signals) as "never a fraud determination" — a legacy signal, not a new detection capability.

### 3.9 AI / Chat (`service/ChatService.java`, `LlmService.java`, related)

- **Purpose:** Generic RAG-based immigration-knowledge chatbot.
- **Entry point:** `controller/ChatController.java` (`POST /api/chat`).
- **Data flow:** Session → conversation history (`ConversationService`) → `RagRetrievalService` retrieves `KnowledgeDocument`/`DocumentEmbedding` chunks via `EmbeddingService` → `PromptBuilderService` assembles the prompt → `LlmService` calls the model.
- **Persistence:** `ChatLog`, `KnowledgeDocument`, `DocumentEmbedding` (Flyway V9 for chat logs).
- **Authorization:** standard authenticated boundary; no subject-specific case data is ever fetched.
- **Dependencies:** none on Fact/Requirement/Evidence-Graph — this is the confirmed gap for capability P (Case-Aware AI Assistant).
- **Limitations:** `ChatService.ask(question, sessionId)` has no `subjectUserId` parameter anywhere — it cannot answer "what's missing in my case" today.

### 3.10 Document processing (`service/DocumentService.java`, `controller/DocumentController.java`)

- **Purpose:** Upload, store, and process immigration documents; legacy entry point that predates the Fact/Evidence-Graph model.
- **Entry point:** `controller/DocumentController.java` (`/api/documents`): `POST` upload, two `GET`s (list/detail), `DELETE`.
- **Data flow:** upload → `DocumentService` → async processing → `Document.uploadStatus` (free string, no formal state machine) → optionally produces `DocumentVersion`/`EvidenceItem`/`Fact` records in the newer pipeline.
- **Persistence:** `Document` (legacy fields: `uploadStatus`, `riskLevel`, `fraudDetected` — no expiry/lifecycle field at all).
- **Authorization:** ownership check via `Document.getUserId()`, standard authenticated boundary.
- **Limitations:** the real document *lifecycle* model (expiry, supersession, immutable versions) lives one layer up in `evidencegraph/entity/DocumentVersion.java`, not on `Document` itself — a structural gap noted in capability L.

### 3.11 Migrations

See §6 for the full Flyway inventory.

---

## 4. Frontend Architecture Inventory

### 4.1 Navigation (verified from `Sidebar.tsx` / `AppRoutes.tsx`)

**User sidebar groups:**
- Workspace: Dashboard, AI Immigration Assistant
- Case Management: Immigration Profile (→ `DigitalTwinPage.tsx`), My Documents, Applications, **Pathway Assessment** (→ `/dashboard/pathways/assessments/new`), Case Timeline & Signals, Reports & Analytics
- Account: Profile/Settings pages

**Admin sidebar groups:**
- Overview, Operations, Intelligence (AI Model Monitoring, Knowledge Base, Immigration Rules, Pathways, Requirements), Governance (Security Center, System Health, Audit Logs, Global Settings)

**Routed but with NO Sidebar entry (reachable only as drill-downs):** `CaseIntelligencePage.tsx`, `EvidenceGraphPage.tsx`, `PathwayExplanationPage.tsx` (all reached from `PathwayAssessmentResultsPage.tsx`'s "Case Intelligence"/"Why this result?"/"Trace evidence" links).

### 4.2 Full dashboard/admin page inventory

**Dashboard pages** (`pages/dashboard/`): `DashboardPage`, `DocumentsPage`, `ApplicationsPage`, `ReportsPage`, `CaseIntelligencePage`, `CaseTimelinePage`, `PathwayAssessmentResultsPage`, `DigitalTwinPage`, `EvidenceGraphPage`, `PathwayExplanationPage`, `RequestPathwayAssessmentPage`, `FactConflictDetailPage`, `SubmitApplicationPage`, `AIChatPage`.

**Admin pages** (`pages/admin/`): `AdminDashboardPage`, `AnalyticsPage`, `ApplicationReviewPage`, `AuditLogsPage` (**calls a non-existent endpoint — see §16**), `CreateUserPage`, `GlobalSettingsPage`, `ImmigrationRulesPage`, `KnowledgeBasePage`, `SecurityCenterPage`, `SystemHealthPage`, `UserManagementPage`, `AIModelMonitoringPage`, `PathwayManagementPage`, `RequirementManagementPage`, `CreateCasePage`, plus small presentational components (`KPICard`, `SectionCard`, `StatusBadge`).

### 4.3 API clients (`src/api/`)

`adminApi.ts`, `analyticsApi.ts`, `applicationApi.ts`, `authApi.ts`, `axios.ts`, `billingApi.ts`, `caseIntelligenceApi.ts`, `chatApi.ts`, `digitalTwinApi.ts`, `documentApi.ts`, `evidenceGraphApi.ts`, `factApi.ts`, `immigrationRulesApi.ts`, `knowledgeBaseApi.ts`, `notificationApi.ts`, `pathwayAdminApi.ts`, `pathwayApi.ts`, `paymentApi.ts`, `supportApi.ts`, `userApi.ts`. Notably, there is **no `pathwayDiscoveryApi.ts`** — confirming G is genuinely unbuilt on the frontend too.

### 4.4 Findings

- **Implemented pages:** all 14 dashboard pages and all admin pages listed above render real data against real endpoints (verified for the ones touched in Phases 2/3; the rest predate this audit's scope but were not found to be stubs).
- **Partially implemented / calling missing APIs:** `pages/admin/AuditLogsPage.tsx` **and** `pages/settings/SecuritySettingsPage.tsx` both define `AUDIT_LOGS_ENDPOINT = "/admin/audit-logs"` and `AUDIT_STATS_ENDPOINT = "/admin/audit-logs/stats"` and call them — **no matching backend controller mapping exists anywhere in the codebase** (confirmed by grepping every `@RequestMapping`/`@GetMapping` in the backend for `audit-logs`). This is a duplicated defect across two frontend files, not one.
- **Backend APIs with no frontend consumer found:** `evidencegraph`'s `getEvidenceItemApi`/`getEvidenceFullTraceApi` are wired (`evidenceGraphApi.ts`), but no explicit UI entry point calls `fullTrace` today beyond what `EvidenceGraphPage.tsx` might reach indirectly — not confirmed as unused, flagged for awareness only, not a defect.
- **Dead/unreferenced routes:** none found — all routes in `AppRoutes.tsx` correspond to a real Sidebar or drill-down link, except the four "drill-down only, no primary nav" pages noted in §4.1 (which are intentional design, not dead code).
- **Duplicate functionality:** none found — Phase 2/3 explicitly avoided creating a second Evidence Graph or Case Intelligence surface.
- **Placeholder pages:** none identified as stubs in the areas this audit touched.

---

## 5. API Inventory

| Method | Endpoint | Controller | Purpose | Auth | Authorization | Frontend Consumer | Status |
|--------|----------|------------|---------|------|----------------|--------------------|--------|
| GET | `/api/cases/pathway-assessments/{assessmentId}/intelligence` | `CaseIntelligenceController` | Readiness + matrix + missing evidence for one assessment | `@AuthenticationPrincipal AuthenticatedUser` | `FactAuthorizationService.assertCanView` via assessment's subject | `caseIntelligenceApi.ts` | Active |
| GET | `/api/cases/{subjectUserId}/contradictions` | `CaseIntelligenceController` | Open Fact conflicts + timeline overlaps for a subject | Yes | `assertCanView(subjectUserId)` | `caseIntelligenceApi.ts` | Active |
| GET | `/api/cases/{subjectUserId}/timeline` | `CaseIntelligenceController` | Chronological case timeline | Yes | `assertCanView(subjectUserId)` | `caseIntelligenceApi.ts` | Active |
| GET | `/api/cases/{subjectUserId}/signals` | `CaseIntelligenceController` | Risk-band rollup of pre-existing signals | Yes | `assertCanView(subjectUserId)` | `caseIntelligenceApi.ts` | Active |
| GET | `/api/evidence-graph/facts/{factId}` | `EvidenceGraphController` | Forward trace from one Fact | Yes | `assertCanView` via Fact's subject | `evidenceGraphApi.ts` | Active |
| GET | `/api/evidence-graph/evidence-items/{evidenceItemId}` | `EvidenceGraphController` | Evidence item detail (IDOR-safe via per-Fact auth check) | Yes | `assertCanView`, tries each linked Fact's subject | `evidenceGraphApi.ts` | Active |
| GET | `/api/evidence-graph/pathway-assessments/{assessmentId}/full-trace` | `EvidenceGraphController` | Full explainability + evidence trace for an assessment | Yes | `assertCanView` via assessment's subject | `evidenceGraphApi.ts` | Active |
| GET | `/api/evidence-graph/documents/{documentId}` | `EvidenceGraphController` | Backward trace + document impact (Phase 3) | Yes | `assertCanView` via `Document.getUserId()` | `evidenceGraphApi.ts` | Active (Phase 3) |
| GET | `/api/evidence-graph/requirement-evaluations/{evaluationId}` | `EvidenceGraphController` | Requirement traceability (Phase 3) | Yes | `assertCanView` via `RequirementEvaluation.getSubjectUserId()` | `evidenceGraphApi.ts` | Active (Phase 3) |
| GET | `/api/pathways` | `PathwayController` | List published pathway catalogue | Yes | none beyond authentication (catalogue is not subject-scoped) | `pathwayApi.ts` | Active |
| GET | `/api/pathways/{pathwayId}` | `PathwayController` | Get one pathway | Yes | none beyond authentication | `pathwayApi.ts` | Active |
| POST | `/api/pathways/{pathwayId}/assessments` | `PathwayAssessmentController` | Assess one subject against one chosen pathway | Yes | `assertCanCreate`/`assertCanView` via `subjectUserId` in body | `pathwayApi.ts` | Active |
| GET | `/api/pathways/assessments/{assessmentId}` | `PathwayAssessmentController` | Retrieve a computed assessment | Yes | `assertCanView` via assessment's subject | `pathwayApi.ts` | Active |
| GET | `/api/pathways/assessments/{assessmentId}/explanation` | `PathwayAssessmentController` | Explainability composition | Yes | `assertCanView` via assessment's subject | `pathwayApi.ts` | Active |
| GET | `/api/requirements/{requirementId}` | `RequirementController` | Get one requirement definition | Yes | none beyond authentication (definition, not subject data) | (requirement definitions consumed indirectly) | Active |
| POST | `/api/requirements/{requirementId}/evaluations` | `RequirementController` | Evaluate one requirement standalone | Yes | `assertCanCreate`/`assertCanView` | `pathwayApi.ts` (assumed) | Active |
| GET | `/api/requirements/evaluations/{evaluationId}` | `RequirementController` | Retrieve one requirement evaluation | Yes | `assertCanView` via evaluation's subject | `pathwayApi.ts` (assumed) | Active |
| GET | `/api/twin/{subjectUserId}` | `DigitalTwinController` | Live Digital Twin projection | Yes | `assertCanView(subjectUserId)` | `digitalTwinApi.ts` | Active |
| POST | `/api/facts` | `FactController` | Create a Fact | Yes | `assertCanCreate` | `factApi.ts` | Active |
| GET | `/api/facts/{factId}` | `FactController` | Get one Fact | Yes | `assertCanView` | `factApi.ts` | Active |
| GET | `/api/facts` | `FactController` | List Facts for a subject | Yes | `assertCanView` | `factApi.ts` | Active |
| PATCH | `/api/facts/{factId}` (verify/resolve variants) | `FactController` | Verify/update a Fact | Yes | `assertCanVerifyOrResolve` | `factApi.ts` | Active |
| GET | `/api/facts/timeline` | `FactController` | Raw Fact timeline (lower-level than Case Intelligence's timeline) | Yes | `assertCanView` | `factApi.ts` | Active |
| GET | `/api/facts/conflicts/{conflictId}` | `FactController` | Get one Fact conflict | Yes | `assertCanView` | `factApi.ts` | Active |
| PATCH | `/api/facts/conflicts/{conflictId}` (resolve/confirm variants) | `FactController` | Resolve or applicant-confirm a conflict | Yes | `assertCanVerifyOrResolve` / `assertCanApplicantConfirmConflict` | `factApi.ts` | Active |
| GET | `/api/admin/pathways` / `POST` / `GET /{id}` / `PATCH /{id}` / `PATCH /{id}/status` | `PathwayAdminController` | Pathway CRUD + lifecycle | Yes | `hasRole("ADMIN")` (SecurityConfig `/api/admin/**`) | `pathwayAdminApi.ts` | Active |
| GET/POST/PATCH | `/api/admin/requirements/**` | `RequirementAdminController` | Requirement CRUD + lifecycle | Yes | `hasRole("ADMIN")` | admin API client | Active |
| GET/POST | `/api/admin/regulatory-versions`, `/{versionId}` | `RegulatoryVersionAdminController` | Append-only RegulatoryVersion entry | Yes | `hasRole("ADMIN")` | `immigrationRulesApi.ts` (assumed) | Active |
| GET | `/api/admin/fact-types` | `FactTypeAdminController` | List known fact types | Yes | `hasRole("ADMIN")` | admin API client | Active |
| GET | `/api/admin/dashboard` | `AdminController` | Admin dashboard summary | Yes | `hasRole("ADMIN")` | `adminApi.ts` | Active |
| POST | `/api/admin/audit/run` | `AdminController` | Trigger an audit run (fraud/AI audit, not Fact access audit) | Yes | `hasRole("ADMIN")` | `adminApi.ts` | Active |
| GET/PATCH | `/api/notifications/**` | `NotificationController` | List/mark-read notifications | Yes | ownership via subject | `notificationApi.ts` | Active |
| POST | `/api/chat` | `ChatController` | Generic RAG chatbot query | Yes | authenticated only, no subject-case access | `chatApi.ts` | Active |
| POST/GET/DELETE | `/api/documents/**` | `DocumentController` | Upload/list/detail/delete documents | Yes | ownership via `Document.getUserId()` | `documentApi.ts` | Active |
| **GET** | **`/admin/audit-logs`** | **NONE — no matching controller mapping found anywhere in the backend** | (intended: Fact/security audit log listing) | N/A | N/A | `AuditLogsPage.tsx`, `SecuritySettingsPage.tsx` | **BROKEN — confirmed defect, see §16** |
| **GET** | **`/admin/audit-logs/stats`** | **NONE — same as above** | (intended: audit log statistics) | N/A | N/A | `AuditLogsPage.tsx`, `SecuritySettingsPage.tsx` | **BROKEN — confirmed defect, see §16** |
| — | *(no endpoint exists)* | — | Multi-pathway ranked evaluation | — | — | — | **MISSING — this is the Phase 4 gap (capability G)** |

---

## 6. Database / Oracle Architecture

### 6.1 Flyway migration inventory (verified, sorted)

| Version | File | Adds |
|---|---|---|
| V1 | `V1__init_schema.sql` | Base schema (users, core tables) |
| V2 | `V2__add_fraud_tables.sql` | Fraud rule/audit tables |
| V3 | `V3__add_visa_rules.sql` | Legacy visa-rules tables (pre-Requirement-engine) |
| V4 | `V4__add_password_reset_token.sql` | `PasswordResetToken` |
| V5 | `V5__add_notifications.sql` | `Notification` |
| V6 | `V6__add_applications.sql` | `Application` |
| V7 | `V7__add_application_rejection_reason.sql` | Rejection-reason column on `Application` |
| V8 | `V8__add_user_phone_country.sql` | Phone/country columns on `User` |
| V9 | `V9__add_chat_logs.sql` | `ChatLog` |
| V10 | `V10__add_fact_foundation.sql` | `Fact`, `FactEvidence`, `FactConflict`, `CaseAssignment`, `FactAccessAuditLog` |
| V11 | `V11__fix_fact_numeric_column_types.sql` | Fixes `Fact`'s Double columns (Hibernate expected `FLOAT(53)`, migrated columns were plain `NUMBER`) |
| V12 | `V12__add_requirement_pathway_foundation.sql` | `RegulatoryVersion`, `Requirement`, `Pathway`, `RequirementEvaluation`, `PathwayAssessment`, and related join tables |
| V13 | `V13__add_evidence_intelligence_graph.sql` | `EvidenceItem`, `DocumentVersion` |
| V14 | `V14__seed_pathway_catalogue.sql` | Seed data for the pathway catalogue |

**Phases 2 and 3 introduced zero new migrations** — both were pure read-time composition/extension over the V10–V14 schema.

### 6.2 Read-time vs. persistence-requiring capabilities

**Genuinely read-time (recomputed every call, nothing cached or stored):**
- Case Intelligence readiness/matrix/missing-evidence/contradictions/timeline/signals (`CaseIntelligenceService`, `CaseOverviewService`)
- Evidence Graph (`EvidenceGraphService` — every node/edge response is recomputed fresh)
- Explainability (`ExplainabilityService`)
- Digital Twin projection (`DigitalTwinProjectionService`)

**Requires persistence today (and would for any Phase 4 built by reusing it as-is):**
- `PathwayAssessmentService.assess()` — writes `PathwayAssessment` + `PathwayAssessmentRequirementEvaluation` rows
- `RequirementEvaluationService.evaluateAndPersist()` — writes `RequirementEvaluation`, `RequirementEvaluationFact`, `RequirementEvaluationConflict` rows

This split is the central design tension for Phase 4 — see §9 and §10.

---

## 7. Security & Authorization

### 7.1 JWT and filter chain

`security/JwtAuthFilter.java` (444 lines) is a `OncePerRequestFilter` registered via `SecurityConfig.securityFilterChain()`'s `.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`. Authentication is fully stateless (`SessionCreationPolicy.STATELESS`); CSRF, HTTP Basic, form login, and logout are all explicitly disabled since JWT is carried in the `Authorization` header, not a cookie.

### 7.2 Route-level authorization (`SecurityConfig.java`, verified in full)

- **Public (permitAll):** `/api/auth/{login,register,verify-email,forgot-password,reset-password,status}`, `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`, `/error`, `/api/reports/download` (uses its own short-lived signed-token authorization instead, since a plain `window.open` navigation can't carry a bearer header), and CORS preflight (`OPTIONS` on `/**`).
- **Admin-only:** `/api/admin/**` → `hasRole("ADMIN")`.
- **Authenticated, explicit:** `/api/dashboard/**`, `/api/documents/**`, `/api/applications/**`, `/api/users/**`, `/api/profile/**`, `/api/auth/{me,change-password,logout}`.
- **Fail-closed default:** `.anyRequest().authenticated()` — this is what actually covers `/api/cases/**`, `/api/evidence-graph/**`, `/api/pathways/**`, `/api/requirements/**`, `/api/facts/**`, `/api/twin/**`, `/api/notifications/**`, and `/api/chat` (none of these are explicitly listed, they fall through to the catch-all). This is a deliberate, documented pattern (confirmed in the Phase 3 audit: "no SecurityConfig change needed — falls under existing `.anyRequest().authenticated()` fail-closed rule").

### 7.3 Subject-level ownership (below the route level)

Route-level rules only gate *authentication*, not *which subject's data* a request may touch — that check happens one layer down, inside each service:

- **`FactAuthorizationService`** (`fact/service/`) is the single retrieval-boundary enforcement point across the whole Fact/Requirement/CaseIntelligence/EvidenceGraph domain:
  - `assertCanView(actor, subjectUserId, sensitivityTier, factId, purpose)` — granted if `actor` IS the subject (`AccessorType.SUBJECT`), or is a `CASE_WORKER` with an active `CaseAssignment` to that subject.
  - `assertCanCreate` — same grant logic as `assertCanView`.
  - `assertCanVerifyOrResolve` — granted only for `CASE_WORKER` (with active assignment) or `ADMINISTRATOR` — a `SUBJECT` cannot verify their own facts.
  - `assertCanApplicantConfirmConflict` — a **distinct** grant, `SUBJECT`-only, explicitly commented as never a path into `assertCanVerifyOrResolve` (self-reported provenance is kept separate from case-worker/admin verification).
  - Every check is audited via `FactAccessAuditService`/`FactAccessAuditLog` (accessor type, purpose, sensitivity tier, granted/denied).
- **IDOR protection pattern:** the entry id is authorized exactly once, before any other read; every subsequent hop in a traversal (Evidence Graph, Case Intelligence, Explainability) is resolved via FK from an already-authorized row — no method accepts a second subject/entity id mid-traversal from the caller. `EvidenceGraphService.getEvidenceItemDetail` is the special case: since `EvidenceItem` has no `subjectUserId` of its own, it authorizes by trying each linked Fact's subject in turn.
- **Cross-subject defense-in-depth:** every Phase 3 addition (`graphForDocument`, `graphForRequirementEvaluation`) re-checks that each discovered Fact actually belongs to the already-authorized subject before adding it to the response (e.g. `addConflictSideNode` checks `expectedSubjectUserId` on both sides of a conflict) — never trusting that a downstream FK necessarily stays inside the authorized boundary.
- **Admin authorization:** purely route-level (`hasRole("ADMIN")`) — admin endpoints are not subject-scoped, they operate on global definitions (Pathway/Requirement/RegulatoryVersion/FactType).

### 7.4 How future phases must reuse this

Any Phase 4+ feature that reads subject-scoped data **must** call `FactAuthorizationService.assertCanView` (or the appropriate `assertCan*` variant) exactly once at its entry point, using whichever field on the entry entity identifies the owning subject (`Document.getUserId()`, `RequirementEvaluation.getSubjectUserId()`, `PathwayAssessment.subjectUserId`, or a directly-supplied `subjectUserId` for a genuinely new query like Pathway Discovery) — never inventing a parallel authorization mechanism, and never accepting a second subject id mid-traversal from the caller.

---

## 8. Provenance & Explainability

### 8.1 The full provenance chain (as it actually exists today)

```
RegulatorySource (sourceAuthority / sourceReference fields on RegulatoryVersion — no separate entity)
        │
        ▼
RegulatoryVersion  (RegulatoryVerificationStatus: UNVERIFIED_INGESTION → HUMAN_VERIFIED → AUTHORITATIVE_CONFIRMED;
                     effectiveFrom / effectiveTo; supersedesVersionId / supersededByVersionId)
        │
        ▼
Requirement  (references exactly one RegulatoryVersion; composed via the closed logic grammar)
        │
        ▼
Pathway  (composes Requirements via the same logic grammar — never duplicates a Requirement's definition)
        │
        ▼
PathwayAssessment / RequirementEvaluation  (personalized, disposable — reference Facts by id only, never copy values)
        │
        ▼
Fact  (typed value, FactProvenanceType, FactStatus lifecycle, FactConfidenceLevel)
        │
        ▼
FactEvidence  (the one Fact↔Document/EvidenceItem linkage — EvidenceItem is an enrichment of this, never a second mechanism)
        │
        ▼
EvidenceItem  (sourceType, status, directness, extractionConfidence, sourceSnippet)
        │
        ▼
DocumentVersion → Document
```

Note: there is no distinct `RegulatorySource` entity — `sourceAuthority`/`sourceReference` are fields directly on `RegulatoryVersion`. This is a deliberate simplification, not a gap — a source has never needed independent identity or CRUD separate from the version that cites it.

### 8.2 Explainability composition

`ExplainabilityService.explain()` is the canonical, single explanation composer — it walks the chain above purely by following already-persisted foreign keys, entirely at read time, and is reused (not duplicated) by:
- `PathwayAssessmentController`'s `GET .../explanation` endpoint directly
- `CaseIntelligenceService`'s matrix/readiness building
- `EvidenceGraphService.fullTrace()`, which composes `ExplainabilityService.explain()` with an evidence-item lookup per contributing fact

### 8.3 Evidence Graph as the explainability *layer*

The Evidence Graph is explicitly documented (and verified in Phase 3's own class Javadocs) as a graph-shaped *rendering* of this same provenance chain, not a second source of truth — every node/edge in `EvidenceGraphResponse` corresponds to a real row or FK relationship already described above. `GraphNodeType` (DOCUMENT, DOCUMENT_VERSION, EVIDENCE, FACT, REQUIREMENT, REQUIREMENT_EVALUATION, PATHWAY, PATHWAY_ASSESSMENT, REGULATORY_VERSION, CONFLICT) and `GraphRelationshipType` (HAS_VERSION, PRODUCES, SUPPORTS_FACT, CONFLICTS_WITH, DERIVED_FROM, DEPENDS_ON_FACT, EVALUATED_UNDER, BLOCKED_BY_CONFLICT, CONTAINS_EVALUATION, VERSIONED_UNDER, COMPOSED_OF) are the vocabulary for this rendering.

### 8.4 "Trace this Fact" and confidence/verification states

`components/fact/FactCard.tsx`'s pre-existing "Trace this Fact" link is the frontend entry point into `graphForFact`, untouched by Phase 3. Confidence/verification states threaded through this entire chain: `FactConfidenceLevel` (Fact-level), `EvaluationCertaintyLevel` (RequirementEvaluation-level, discounted when the underlying `RegulatoryVersion.verificationStatus` is `UNVERIFIED_INGESTION`), and `PathwayAssessment.assessmentConfidenceLevel` (Pathway-level, banded via the same `EvaluationCertaintyCalculator`). At no point is `UNVERIFIED_INGESTION` silently upgraded — the discount is applied every time the chain is walked, not baked into a stored value.

---

## 9. Pathway Discovery — Detailed Assessment

### 9.1 What already exists

- `PathwayController.getPublishedPathways()` — `GET /api/pathways` lists the read-only catalogue, filtered to `PathwayStatus.PUBLISHED` (confirmed by grepping `PathwayRepository.findByStatus`).
- `Pathway` entity already carries `jurisdiction` and `category` fields — ready to use for filtering/grouping in a discovery UI with zero schema change.
- `PathwayAssessmentService.assess(actor, pathwayId, subjectUserId, assessmentDate)` is a complete, correct, single-pathway evaluation pipeline.
- `CaseIntelligenceService`'s readiness formula (`requirementCoveragePercent`/`evidenceCoveragePercent`/`consistencyPercent` → `overallReadinessPercent`) is a proven, comparable scalar metric already computed per-assessment.

### 9.2 What is missing

- No service iterates `PathwayRepository.findByStatus(PUBLISHED)` and evaluates the current subject against every one of them.
- No response shape exists for "ranked list of pathways with a comparable score."
- No frontend page or Sidebar entry for this exists (`RequestPathwayAssessmentPage.tsx` requires a pre-selected `pathwayId`).

### 9.3 How the pieces actually work together today

- **Published-pathway identification:** `PathwayRepository.findByStatus(PathwayStatus.PUBLISHED)` — this is the same query Discovery must reuse, never a hardcoded list.
- **Assessment:** `PathwayAssessmentService.assess()` resolves the subject's facts once via `TemporalFactResolver`, evaluates the pathway's composition-logic tree via `LogicEvaluationService`, and persists a `PathwayAssessment` + N `RequirementEvaluation` rows.
- **Readiness calculation:** computed independently, after the fact, by `CaseIntelligenceService` reading back the persisted `RequirementEvaluation` rows for a given assessment — it is **not** computed inside `assess()` itself.
- **Requirement evaluations:** each is memoized per-`assess()`-call (a Requirement referenced twice in one Pathway's tree is evaluated once), but there is **no** cross-pathway memoization — two different Pathways sharing an identical Requirement will each trigger their own evaluation and their own persisted row.
- **Evidence's effect on assessment:** flows in via `RequirementEvaluationFact`/`FactEvidence` — a requirement's outcome depends on which Facts (and their confidence) are available at evaluation time, all resolved through the one `EvaluationFactView` built by `TemporalFactResolver`.
- **Conflicts' effect on assessment:** an open `FactConflict` referenced by a contributing Fact can push a `RequirementEvaluationOutcome` to `CONFLICTED`, tracked via `RequirementEvaluationConflict`.
- **Regulatory verification's effect on assessment:** `EvaluationCertaintyCalculator` discounts confidence when the Requirement's `RegulatoryVersion.verificationStatus` is `UNVERIFIED_INGESTION`, independent of whether the underlying Facts themselves are strong.

### 9.4 Can Discovery be read-only?

**Not without new engineering.** `assess()` is `@Transactional` (writable) and unconditionally calls `pathwayAssessmentRepository.save()` plus a `.save()` per requirement evaluation — there is no parameter or overload that skips persistence. A genuinely read-only Discovery pass requires either (a) a new non-persisting evaluation method that shares the same `LogicEvaluationService`/`TemporalFactResolver`/`RequirementEvaluationService` logic but stops short of `.save()`, or (b) accepting that every discovery call is a real, audit-trailed assessment (see §10, Option A vs. B).

### 9.5 Performance implications

Looping `assess()` (or an equivalent) once per published Pathway means: (a) `TemporalFactResolver.resolve()` runs once per Pathway instead of once per subject — the same Fact data is reconstructed redundantly N times; (b) each Pathway's full set of Requirements is evaluated independently even where Pathways share identical Requirements; (c) if persisting, N full `PathwayAssessment` rows (plus their child `RequirementEvaluation` rows) are written per single "show me my options" page load. See §12 for concrete recommendations.

### 9.6 Authorization requirements

Discovery must authorize once at entry — `FactAuthorizationService.assertCanView(actor, subjectUserId, ...)` — exactly like every existing subject-scoped read, before resolving any Fact or iterating any Pathway. No new authorization mechanism is needed or appropriate.

---

## 10. Phase 4 Architecture Options

### Option A — Persist every discovery assessment (reuse `assess()` verbatim)

- **Architecture:** Discovery service loops published Pathways and calls the existing `PathwayAssessmentService.assess()` unmodified for each one, then reads back each resulting `PathwayAssessment`'s readiness via `CaseIntelligenceService`.
- **Advantages:** Zero new evaluation logic — lowest engineering risk and fastest to ship; every discovery result is automatically a fully explainable, persisted, auditable `PathwayAssessment` a user can revisit later; reuses 100% of existing tested code paths.
- **Disadvantages:** Every "just browsing" page view writes N `PathwayAssessment` + M `RequirementEvaluation` rows to Oracle; redundant `TemporalFactResolver` calls (once per Pathway); no natural way to say "this was a discovery preview, not a real assessment" without a schema change or a naming convention.
- **Database impact:** none (no new schema), but real, unbounded row growth proportional to (subjects × pathways × discovery-views).
- **Performance:** worst of the three options — full write transaction per Pathway.
- **Audit implications:** every discovery view becomes a real, permanent record — arguably a feature (full history of "what you were shown"), arguably noise.
- **Complexity:** lowest.
- **Freshness:** always current (recomputed on every call).
- **Risks:** database bloat at scale; a user re-opening Discovery repeatedly multiplies rows.

### Option B — Read-only / transient evaluation (new non-persisting path)

- **Architecture:** A new method (e.g. on a new `PathwayDiscoveryService`) that resolves the subject's `EvaluationFactView` **once**, then runs the same `LogicEvaluationService`/composition-logic walk per Pathway but calls a non-persisting variant of requirement evaluation (or discards the built entities without `.save()`), returning only the DTOs.
- **Advantages:** Zero database writes; one `TemporalFactResolver.resolve()` call total regardless of Pathway count; no row growth; genuinely "free to browse."
- **Disadvantages:** Requires either a refactor of `RequirementEvaluationService`/`LogicEvaluationService` to accept a non-persisting mode, or a parallel (but logic-sharing) code path — more engineering than Option A; a discovery result cannot later be "reopened" as a permanent assessment without the user explicitly choosing one pathway and re-running the real `assess()`.
- **Database impact:** none.
- **Performance:** best of the three — one fact-resolution pass, no write transactions.
- **Audit implications:** discovery views are not tracked at all (arguably correct — browsing isn't a decision).
- **Complexity:** moderate — the non-persisting mode must be threaded carefully through `RequirementEvaluationService.EvaluationRun` without breaking its existing memoization contract for the real `assess()` path.
- **Risks:** a bug that accidentally persists partial state (e.g. a `.save()` left in a shared code path) would be a correctness/security issue, not just noise — needs careful test coverage.

### Option C — Hybrid

- **Architecture:** Discovery itself is read-only (Option B), but selecting one ranked result from the discovery list to "commit to" triggers a single real `assess()` call (Option A) for that one Pathway only — turning a preview into a real, auditable assessment exactly once, on explicit user action.
- **Advantages:** Combines Option B's cheap browsing with Option A's proper audit trail at the moment a user actually decides to pursue a pathway; matches the natural product flow (browse → pick → commit).
- **Disadvantages:** Two distinct code paths (transient evaluation + persisted evaluation) must be kept logically consistent — a discovery preview's outcome must match what `assess()` produces when the user commits, or trust in the ranking erodes.
- **Database impact:** minimal — one `PathwayAssessment` per genuine user decision, not per page view.
- **Performance:** good — the expensive read-only pass happens once per browse, the expensive write pass happens only once per actual decision.
- **Audit implications:** clean — the audit trail records decisions, not window-shopping.
- **Complexity:** highest of the three, but each piece (Option B's transient path, Option A's existing `assess()`) is independently simple.
- **Risks:** the two paths drifting out of sync over time if not tested against each other explicitly.

### Recommendation

**Option C (Hybrid)** is the correct target architecture — it matches the real user journey (§14) and keeps the database clean without sacrificing auditability. **Option B alone** is an acceptable, lower-effort first cut if engineering time is constrained, since it can be upgraded to Option C later (the "commit to a pathway" step already exists as the current `RequestPathwayAssessmentPage.tsx` flow calling `assess()`). **Option A should not be the final target** — it is only acceptable as a genuinely temporary shortcut, explicitly flagged as technical debt if chosen.

---

## 11. Ranking Architecture

Discovery must **not** invent a second readiness algorithm — it must rank Pathways using the exact same signals `CaseIntelligenceService` already computes, so "readiness" means one thing everywhere in the product.

| Canonical signal | Canonical source (do not reimplement) |
|---|---|
| Requirement support / outcome per requirement | `RequirementEvaluationService` → `RequirementEvaluationOutcome` (9-value Kleene outcome) |
| Mandatory-requirement weighting | `Requirement.mandatory` flag, read exactly as `PathwayAssessmentService.resolvePathwayOutcome()` and `CaseIntelligenceService` already read it |
| Evidence coverage | `CaseIntelligenceService.buildReadiness()` → `evidenceCoveragePercent` |
| Missing evidence | `CaseIntelligenceService.buildMissingEvidence()` → `MissingEvidenceItemResponse` (necessity/severity) |
| Conflicts | `FactConflict` via `CaseOverviewService.getContradictions()` / `RequirementEvaluationConflict` |
| Verification state | `RegulatoryVerificationStatus` via `EvaluationCertaintyCalculator` |
| Overall readiness (the ranking scalar) | `CaseIntelligenceService.buildReadiness()` → `overallReadinessPercent` |
| Risk | `CaseOverviewService.computeRiskBand()` → `CaseRiskBand` |

**Ranking logic itself (new, minimal):** sort published Pathways by `overallReadinessPercent` descending, using `RequirementEvaluationOutcome` as a tiebreak (e.g. `SATISFIED`/`PARTIALLY_SATISFIED` pathways above `INSUFFICIENT_EVIDENCE`/`UNKNOWN` ones at equal readiness). No new scoring formula, no new weighting scheme — Discovery's only new logic is the orchestration loop and the sort, not the scoring itself.

---

## 12. Performance Analysis

| Concern | Current state | Risk for Discovery | Recommendation |
|---|---|---|---|
| N+1 queries | `RequirementEvaluationService` resolves bindings/evidence per-requirement inside `evaluateAndPersist` (verified: `factBindingRepository.findByRequirementId(requirementId)` called per requirement) | Looped once per Pathway × per Requirement, this multiplies with Pathway count | Batch-load `RequirementFactBinding`s for all requirements referenced by all candidate pathways up front, keyed by requirement id, before the ranking loop |
| Repeated `TemporalFactResolver` calls | One call per `assess()` invocation today (by design, for a single pathway) | A naive Discovery loop calling `assess()` per Pathway repeats this N times for the *same* subject and *same* date | Resolve `EvaluationFactView` **once** per Discovery request and pass it into a refactored evaluation path (this is the strongest single performance lever available) |
| Repeated fact/evidence loading | Same root cause as above | Same | Same fix — one resolved `EvaluationFactView`, shared across all pathways in one discovery call |
| Pathway count | Currently small (seeded catalogue via V14); no pagination on `GET /api/pathways` today | A growing catalogue makes an unbounded "evaluate every published pathway" loop increasingly expensive per request | Cap the evaluated set (e.g. filter by `jurisdiction`/`category` before ranking, or paginate ranking results) rather than always evaluating the full catalogue |
| Oracle load | Each `assess()` call today is a full write transaction (2+ inserts minimum) | Option A's per-view write load could become a real Oracle load concern at scale | Prefer Option B/C (§10) — reads dominate, writes happen only on explicit commit |
| Persistence overhead | `PathwayAssessment` + `PathwayAssessmentRequirementEvaluation` + `RequirementEvaluation` + `RequirementEvaluationFact`/`Conflict` rows per assessment | Multiplies by Pathway count under Option A | Same as above |
| Concurrency | No caching or locking observed on any read-time service today (by design — always fresh) | A discovery request under load is CPU/DB-bound per user, not shared across users (no cross-user caching applies — each ranking is subject-specific) | No cross-request caching is appropriate (results are subject-specific and must reflect the latest Facts); focus on per-request efficiency, not caching |
| API calls | Today: 1 call per pathway assessment (`POST .../assessments`) | A naive frontend calling one assessment endpoint per pathway in a loop would multiply round-trips | Expose Discovery as a single endpoint (`GET /api/pathways/discovery`) that does the fan-out server-side, not client-side |
| Frontend loading | `EvidenceGraphPage.tsx`/`CaseIntelligencePage.tsx` already show loading/error/empty states as the established pattern | A ranked list with N pathways evaluated server-side could have a longer single-request latency | Show a loading skeleton for the whole ranked list (single request), not per-row spinners |
| Bounded evaluation | Not currently a concern (single-pathway `assess()` calls are inherently bounded) | An unbounded "rank against every published pathway" call needs an explicit cap | Recommend a hard cap (e.g. evaluate at most ~50 published pathways per discovery call) with a documented rationale, revisited if the catalogue grows past that |

**Production-safe limits recommended:** resolve facts once per discovery request; cap evaluated-pathway count; avoid per-pathway write transactions unless Option A is deliberately chosen; paginate the `GET /api/pathways` catalogue itself if it grows large, independent of Discovery.

---

## 13. Frontend Product Architecture (description only — nothing created)

- **Proposed route:** `/dashboard/pathways/discovery`
- **Proposed page:** `pages/dashboard/PathwayDiscoveryPage.tsx` — mirrors the existing loading/error/empty-state pattern from `EvidenceGraphPage.tsx`/`CaseIntelligencePage.tsx`.
- **Proposed components:** a ranked list/card grid (reusing `Badge`, `Button`, `EmptyState`, `Loader`, `ErrorAlert` from `components/common/`), one card per ranked Pathway showing readiness percentage, outcome badge, jurisdiction/category tags, and a "Why this ranking?" expandable section.
- **Proposed API service:** new `pathwayDiscoveryApi.ts` alongside the existing `pathwayApi.ts`, calling the new `GET /api/pathways/discovery` endpoint.
- **Proposed TypeScript types:** new `types/pathwayDiscovery.ts` mirroring the new `PathwayDiscoveryResponse`/`PathwayRankingRow` DTOs, following the exact pattern of `types/evidenceGraph.ts` mirroring its backend DTOs.
- **Proposed navigation entry:** a new Sidebar item positioned **above** the existing "Pathway Assessment" item in the Case Management group (e.g. "Discover Pathways" or "Find My Pathway"), since Discovery is logically upstream of choosing one pathway to assess.
- **Handoff to Pathway Assessment results:** selecting a ranked pathway either (a) under Option C, triggers a real `assess()` call and navigates to the existing `PathwayAssessmentResultsPage.tsx`, or (b) under Option B alone, deep-links to `RequestPathwayAssessmentPage.tsx` with the pathway pre-selected.
- **Handoff to Case Intelligence:** unchanged — reached from `PathwayAssessmentResultsPage.tsx`'s existing "Case Intelligence" link once a real assessment exists.
- **Handoff to Evidence Graph:** unchanged — reached from the existing "Trace evidence"/"Why this result?" links once a real assessment exists.

---

## 14. User Experience

**Intended journey:**

```
Applicant
  ↓
Pathway Discovery  (new — "here are pathways ranked by your current readiness")
  ↓
Ranked Pathways  (list, each showing a readiness percentage and outcome badge)
  ↓
"Why this ranking?"  (expandable — which requirements are met/missing/uncertain for THIS pathway)
  ↓
Pathway Details  (commit to one — triggers or reuses a real assessment)
  ↓
Existing Assessment  (PathwayAssessmentResultsPage.tsx, unchanged)
  ↓
Case Intelligence  (CaseIntelligencePage.tsx, unchanged)
  ↓
Requirement → Evidence  (the existing matrix, unchanged)
  ↓
Evidence Graph  (EvidenceGraphPage.tsx, unchanged)
```

**How the UI must communicate uncertainty without overclaiming:**

- Readiness is presented as a **percentage of requirements/evidence covered**, never as "your chance of approval" or any probability-of-outcome language — matching the existing non-guarantee disclaimer pattern already used in `CaseOverviewSummaryResponse`.
- Evidence coverage and missing evidence use the existing necessity/severity vocabulary (REQUIRED/SUPPORTING, CRITICAL/MEDIUM) — never a new "urgency" scale invented for Discovery alone.
- Conflicts surface using the existing non-forensic language ("potential conflict," "requires verification") — never "fraud" or "invalid."
- Regulatory verification state is shown per pathway exactly as `RegulatoryVerificationStatus` already models it (e.g. a badge distinguishing `AUTHORITATIVE_CONFIRMED` from `UNVERIFIED_INGESTION`), never silently hidden or upgraded.
- Every ranked outcome uses the existing `RequirementEvaluationOutcome` vocabulary (SATISFIED/PARTIALLY_SATISFIED/INSUFFICIENT_EVIDENCE/etc.) — never a simplified "good/bad" binary that discards the Kleene three-valued nuance.

---

## 15. What MUST NOT Be Rebuilt

| Existing system | Why rebuilding it would be harmful |
|---|---|
| `PathwayAssessmentService.assess()` (composition-logic evaluation) | Already correctly implements the approved logic-grammar/Kleene evaluation with tested edge cases (circular-dependency detection, requirement-reuse memoization); a second implementation would inevitably drift from it and produce inconsistent outcomes between Discovery and a real assessment |
| `LogicEvaluationService` (Kleene 3-valued engine) | The core correctness guarantee of the entire Requirement/Pathway system — reimplementing it for Discovery risks subtly different three-valued-logic semantics between "browsing" and "assessing," undermining trust in the ranking |
| `TemporalFactResolver` | Already correctly handles point-in-time Fact reconstruction from `effectiveFrom`/`effectiveTo` windows; a second resolver risks divergent temporal semantics |
| `RequirementEvaluationService` (incl. its memoization) | Encodes the exact "requirement referenced twice = evaluated once" contract already tested; duplicating it for Discovery would need to re-solve the same memoization problem, likely worse |
| `CaseIntelligenceService` (readiness formula) | Is the platform's one definition of "readiness" — a second, Discovery-specific readiness formula would let two screens disagree about how ready a user actually is, which is a trust-destroying inconsistency |
| `ExplainabilityService` | Already the canonical "why" composer reused by three different surfaces (assessment explanation, Case Intelligence, Evidence Graph full-trace) — Discovery's "Why this ranking?" should call into the same service, not write new explanation text |
| Evidence Graph (`EvidenceGraphService`/Controller) | A complete, tested traceability layer; Discovery does not need its own evidence visualization — it hands off to this once a pathway is chosen |
| `FactAuthorizationService` | The single IDOR-safe authorization boundary for this entire domain; any new service inventing its own ownership check reintroduces exactly the class of bug this service exists to prevent |
| Pathway / Requirement / RegulatoryVersion data model | Already correctly data-driven (no hardcoded FSWP/country-specific logic anywhere) — Discovery must query these models generically (`findByStatus(PUBLISHED)`), never hardcode a pathway list |

---

## 16. Existing Defects

| Defect | Location | Impact | Phase 4 relevance | Fix now? |
|---|---|---|---|---|
| Frontend calls a backend endpoint that does not exist | `ai-immigration-frontend/src/pages/admin/AuditLogsPage.tsx` (`AUDIT_LOGS_ENDPOINT = "/admin/audit-logs"`, `AUDIT_STATS_ENDPOINT = "/admin/audit-logs/stats"`) — confirmed via exhaustive grep of every `@RequestMapping`/`@GetMapping` in the backend for `audit-logs`: zero matches | The Admin Audit Logs page is fully non-functional — every request 404s | None — unrelated to Pathway Discovery | **NO — out of scope for this documentation task** |
| Same missing endpoint, second consumer | `ai-immigration-frontend/src/pages/settings/SecuritySettingsPage.tsx` (identical `AUDIT_LOGS_ENDPOINT`/`AUDIT_STATS_ENDPOINT` constants and calls) | Security Settings' audit section is also fully non-functional | None | **NO — out of scope for this documentation task** |
| Real audit data exists with no admin-facing API | `fact/entity/FactAccessAuditLog.java` + `fact/service/FactAccessAuditService.java` record every Fact access grant/denial, but no controller exposes them | The two broken pages above have nothing to fall back to even if pointed at the right base path — the intended data source has no REST surface yet | None | **NO — out of scope for this documentation task** |

---

## 17. Pre-Existing Working Tree (read-only Git inspection)

`git status --short` at the time of this assessment: **290 entries total** — `167` unstaged-modified (` M`), `7` unstaged-deleted (` D`), `116` untracked (`??`). `git diff --cached --stat` is **empty** — nothing is currently staged.

**Composition:**
- **Unstaged-modified (167 files):** the large pre-existing baseline spanning nearly every frontend page/component/api-client and numerous backend controllers/services/entities/tests — established in earlier turns of this engagement as intentional in-progress work that predates and is independent of Phases 2/3, and must remain untouched.
- **Unstaged-deleted (7 files):** the known "leading-space directory typo" duplicates — e.g. `" legal/CookieConsentBanner.tsx"`, `" types/analytics.ts"`, `" types/auth.ts"`, `" types/chat.ts"`, `" types/document.ts"`, `" types/payment.ts"` (a literal leading space in the path segment) — each has a correctly-pathed untracked (`??`) counterpart already present (e.g. `ai-immigration-frontend/src/legal/CookieConsentBanner.tsx`, `ai-immigration-frontend/src/types/analytics.ts`). This is pre-existing, unrelated to Phases 2/3/4, and must not be resolved as a side effect of any future phase's work.
- **Untracked (116 files):** the substantial pre-existing feature surface that was never committed — including the entire `evidencegraph/` and `caseintelligence/` backend packages except the files Phases 2/3 explicitly modified, all `requirement/admin` controllers/services/DTOs, `Application`/`Notification`/`ChatLog`/`PasswordResetToken` subsystems, Flyway migrations V4–V9/V13/V14, several frontend `types/*.ts`/`api/*.ts`/`utils/*.ts` files, the admin pathway/requirement management pages, and scratch artifacts (`cp.txt`, `docs/`, `phase2_approutes.patch`, `phase2_sidebar.patch`, `sidebar_full.diff`) that must never be staged.
- **The one file already fully resolved by this engagement:** `ai-immigration-frontend/src/features/documents/components/DocumentCard.tsx` — previously a mixed file (pre-existing dark-theme/status-enum work + a genuine Phase 3 addition); the Phase 3 portion (28 insertions, isolated via a hand-built index-only patch) was committed in `d44a2f9`, and the file now shows as plain ` M` in the working tree — its **remaining unstaged diff is entirely pre-existing** dark-theme/status-enum work and must not be touched or re-staged as part of Phase 4.

**Files that must remain protected (never staged/committed as a side effect of Phase 4 work):** `cp.txt`, `docs/`, `phase2_approutes.patch`, `phase2_sidebar.patch`, `sidebar_full.diff`, the leading-space duplicate paths, and every one of the 167 unstaged-modified pre-existing files not directly touched by a deliberate, explicitly-scoped Phase 4 change.

No Git command that could alter this state (`add`, `commit`, `reset`, `checkout`, `stash`, `clean`, `revert`) was run in producing this report.

---

## 18. Future Phase Roadmap

1. **Phase 4 — Pathway Discovery & Explainable Ranking** (this document's recommendation). Depends only on already-completed Phases 1–3. No dependency on any later phase.
2. **What-If / Scenario Simulation.** Naturally follows Discovery: once a user can see *which* pathway to target, the next question is "what would change if I had X." Depends on Phase 4's evaluation orchestration (a transient/non-persisting evaluation mode, if built for Discovery under Option B/C, is exactly the mechanism What-If needs for hypothetical-fact injection) — building What-If first without Discovery would duplicate that transient-evaluation groundwork.
3. **Missing Evidence Prediction & Prioritization.** Deepens the existing deterministic necessity/severity model (capability I) into something predictive. Benefits from Discovery/What-If existing first, since "which evidence matters most" is more actionable once a user has a specific target pathway rather than an unranked guess.
4. **Regulatory Change Detection & Impact Intelligence** (capabilities J+K together). Independent of Discovery/What-If technically, but higher product value once more users have a specific pathway+assessment on file to be "impacted" by a regulatory change — sequencing after Discovery increases the population this feature can usefully notify.
5. **Case-Aware AI Assistant.** Requires giving `ChatService` access to a subject's Fact/Assessment/Case Intelligence data — most valuable once Discovery/Case Intelligence together give the assistant something substantive to reason about ("why am I not ready for X").
6. **Deadline Intelligence.** Builds on Document Lifecycle's already-captured (but unused) `documentExpiryDate` and the modeled-but-unscheduled `RequirementEvaluationOutcome.EXPIRED` — logically follows once there are real assessments (from Discovery) and real documents with lifecycle data to track deadlines against.
7. **Professional / Consultant Workspace.** The backend authorization boundary (`CASE_WORKER`/`CaseAssignment`) already exists independently of all of the above — this can in principle be built at any point, but is sequenced last here because it's a distinct user-role investment (multi-client UI) rather than a deepening of the applicant-facing intelligence stack; prioritize earlier only if case-worker/consultant users become a business priority sooner.

---

## 19. Phase 4 Scope

### IN SCOPE

- A new read method that evaluates the current subject against every `PUBLISHED` Pathway and returns a ranked list.
- A new DTO (`PathwayDiscoveryResponse` / `PathwayRankingRow`) reusing existing enums/fields (`RequirementEvaluationOutcome`, `overallReadinessPercent`, `Pathway.jurisdiction`/`category`).
- A new controller endpoint (`GET /api/pathways/discovery`).
- Authorization via the existing `FactAuthorizationService.assertCanView`, checked once at entry.
- A decision (per §10) on Option A/B/C for whether/how discovery evaluation persists.
- A performance approach that resolves the subject's facts once per discovery request, not once per pathway (per §12).
- A new frontend page, API client, TypeScript types, and Sidebar nav entry (per §13), reusing existing common components.
- A "Why this ranking?" explanation reusing `ExplainabilityService`, not new explanation text.
- Non-guarantee UX language consistent with the rest of the platform (per §14).
- Unit tests for the ranking orchestration, an Oracle integration test for the end-to-end discovery flow, and regression tests confirming existing single-pathway assessment, Case Intelligence, and Evidence Graph are unaffected.

### OUT OF SCOPE

- What-If / Scenario Simulation (Phase 5).
- Missing Evidence Prediction beyond the existing deterministic necessity/severity model (Phase 6).
- Regulatory Change Detection / Impact Alerts (Phase 7).
- Case-Aware AI Assistant changes to `ChatService` (Phase 8).
- Deadline Intelligence / document expiry alerting (Phase 9).
- Professional/Consultant Workspace frontend (Phase 10).
- Fixing the `AuditLogsPage`/`SecuritySettingsPage` → `/admin/audit-logs*` defect (§16) — tracked separately, not part of Phase 4.
- Any change to `PathwayAssessmentService.assess()`'s existing single-pathway behavior, `LogicEvaluationService`, `TemporalFactResolver`, `RequirementEvaluationService`'s persistence contract, `CaseIntelligenceService`'s readiness formula, `ExplainabilityService`, or `FactAuthorizationService` beyond calling them as they exist today (per §15).
- Any new database migration, unless Option A/C's persistence strategy is chosen and even then only if a genuinely new column/table is required (current schema likely suffices either way).
- Resolving the pre-existing working-tree baseline (§17) — none of those 290 entries are to be staged, reverted, or otherwise altered by Phase 4 work.

---

## 20. Phase 4 Definition of Done

- **Backend:** a single new orchestration service and DTO set exist; zero modifications to `PathwayAssessmentService`, `LogicEvaluationService`, `TemporalFactResolver`, `RequirementEvaluationService`, `CaseIntelligenceService`, or `ExplainabilityService`'s existing method signatures/behavior (extension only, if any, mirroring the Phase 3 precedent).
- **Frontend:** one new page, one new API client, one new types file, one new Sidebar entry — no duplicate Evidence Graph/Case Intelligence surface created.
- **Authorization:** the new endpoint calls `FactAuthorizationService.assertCanView` exactly once at entry; a cross-subject test confirms a stranger cannot request another subject's discovery ranking.
- **Performance:** `TemporalFactResolver.resolve()` is called at most once per discovery request regardless of published-pathway count (verified by test or code inspection); an explicit, documented cap exists on the number of pathways evaluated per call.
- **Explainability:** the "Why this ranking?" view is backed by `ExplainabilityService`, not new bespoke explanation text.
- **Provenance:** every ranked pathway's regulatory verification state is visible and never silently upgraded from `UNVERIFIED_INGESTION`.
- **Tests:** new unit tests for the ranking orchestration and sort order; a new Oracle integration test exercising the full discovery flow against a real published-pathway catalogue; the full existing suite (239 tests as of the Phase 3 checkpoint, plus whatever Phase 4 adds) continues to pass with zero regressions in Case Intelligence, Evidence Graph, Pathway Assessment, or Digital Twin.
- **Oracle compatibility:** no Postgres-specific SQL; no new migration unless a genuine new column/table is required and justified in the PR description.
- **UX:** loading/empty/error states match the established pattern (`Loader`/`EmptyState`/`ErrorAlert`); readiness/evidence/conflict/verification language matches §14's non-guarantee vocabulary exactly.
- **Non-guarantee language:** no UI copy anywhere states or implies a probability of visa/application approval.
- **Data-driven pathway architecture:** the evaluated pathway set comes only from `PathwayRepository.findByStatus(PUBLISHED)` — a code review confirms no hardcoded pathway key, country, or visa-category logic was introduced anywhere in the new code.

---

## 21. Final Recommendation

## RECOMMENDED PHASE 4
**Immigration Pathway Discovery & Explainable Ranking.**

## WHY
Every capability delivered by Phases 1–3 (Fact Foundation, Requirement/Pathway/Regulatory engine, Case Intelligence, Advanced Evidence Graph) operates strictly *after* a user has already chosen one specific pathway to be assessed against. There is currently no answer anywhere in the product to the single most upstream question a prospective applicant actually has: "which pathway even fits me?" `RequestPathwayAssessmentPage.tsx` requires a pre-selected `pathwayId`, and `PathwayController` exposes only a flat, unranked catalogue. Closing this gap converts the platform from "explain the pathway you already guessed" into "tell me which pathway fits me" — a materially larger acquisition and retention lever than any deeper investment in a case a user has already committed to, and the one gap that gates whether users ever reach the very mature Case Intelligence/Evidence Graph experience at all.

## EXISTING SYSTEMS TO REUSE
`PathwayAssessmentService.assess()`'s composition-logic evaluation core, `TemporalFactResolver.resolve()`, `LogicEvaluationService`'s Kleene three-valued engine, `RequirementEvaluationService.EvaluationRun`'s memoization contract, `CaseIntelligenceService`'s `overallReadinessPercent` as the single ranking metric, `ExplainabilityService` for "Why this ranking?", `Pathway.jurisdiction`/`category` for filtering, `FactAuthorizationService.assertCanView` for authorization, and the existing `PathwayAssessmentResultsPage.tsx`/`CaseIntelligencePage.tsx`/`EvidenceGraphPage.tsx` as the unmodified downstream handoff destinations.

## NEW BACKEND WORK
One orchestration service (e.g. `requirement/service/PathwayDiscoveryService`) that resolves a subject's `EvaluationFactView` once, loops `PathwayRepository.findByStatus(PUBLISHED)` (capped, per §12), evaluates each via the existing composition-logic path (in a non-persisting mode per Option B/C, §10), and ranks by `overallReadinessPercent`. One new DTO pair (`PathwayDiscoveryResponse`/`PathwayRankingRow`). One new controller endpoint (`GET /api/pathways/discovery`).

## NEW FRONTEND WORK
One page (`PathwayDiscoveryPage.tsx`), one API client (`pathwayDiscoveryApi.ts`), one types file (`types/pathwayDiscovery.ts`), one Sidebar nav entry positioned above "Pathway Assessment," reusing existing common components (`Badge`, `Button`, `Loader`, `EmptyState`, `ErrorAlert`) and existing downstream pages for handoff.

## DATABASE STRATEGY
No new migration required under any of the three options in §10. Recommended: Option C (hybrid) — the discovery pass itself writes nothing; committing to one ranked pathway triggers the existing, unmodified `assess()` write path exactly as it already works today.

## SECURITY STRATEGY
Reuse `FactAuthorizationService.assertCanView(actor, subjectUserId, ...)` exactly once at the new endpoint's entry — no new authorization mechanism, no endpoint accepting a subject id that bypasses this check, matching the IDOR-safe pattern documented and tested throughout Phases 1–3.

## PERFORMANCE STRATEGY
Resolve `TemporalFactResolver`'s `EvaluationFactView` exactly once per discovery request (not once per pathway); cap the number of pathways evaluated per call; avoid per-pathway write transactions (Option B/C); batch-load `RequirementFactBinding`s across all candidate pathways up front rather than per-requirement-per-pathway.

## TEST STRATEGY
Unit tests for the ranking orchestration and sort/tiebreak logic; an Oracle integration test exercising a realistic multi-pathway discovery flow end to end; explicit cross-subject authorization tests; regression tests confirming Case Intelligence, Evidence Graph, Pathway Assessment, and Digital Twin are unaffected (the full existing suite, 239 tests as of the Phase 3 checkpoint, must continue to pass).

## RISKS
Persistence cost if Option A is chosen instead of B/C (§10); redundant fact-resolution if the orchestration loop isn't refactored to resolve once (§12); a single scalar readiness score across pathways with very different mandatory/optional requirement mixes can mislead without the existing non-guarantee disclaimer language (§14); the transient (non-persisting) evaluation path, if built, must be carefully isolated from the real `assess()` path's `.save()` calls to avoid an accidental partial-persistence bug.

## OUT OF SCOPE
What-If/Scenario Simulation, Missing Evidence Prediction, Regulatory Change Detection/Impact Alerts, Case-Aware AI Assistant, Deadline Intelligence, Professional/Consultant Workspace, the `AuditLogsPage`/`SecuritySettingsPage` missing-endpoint defect, any modification to the nine systems listed in §15, and any action on the pre-existing 290-entry working tree described in §17.

## DEFINITION OF DONE
As specified in full in §20.

---

**STOP HERE — WAIT FOR HUMAN REVIEW**
