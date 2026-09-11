package com.godfrey.ai_immigration_document_analyzer.evidencegraph.service;

import com.godfrey.ai_immigration_document_analyzer.entity.Document;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceFullTraceResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceGraphResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceItemResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.GraphEdgeResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.GraphNodeResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.GraphNodeType;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.GraphRelationshipType;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.DocumentVersion;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItem;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.DocumentVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConflict;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactEvidence;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessment;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationConflict;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationFact;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RegulatoryVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationFactRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.ExplainabilityService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * EVIDENCE GRAPH SERVICE
 * ============================================================================
 *
 * The Evidence Intelligence Graph's single orchestration entry point - a
 * read-only, recomputed-on-every-call composition over rows that already
 * exist (Fact, FactEvidence, FactConflict, RequirementEvaluation,
 * Requirement) plus the two additive entities this feature introduces
 * (EvidenceItem, DocumentVersion). This is a query pattern, never a second
 * source of truth: nothing here is persisted, and every traversal
 * degrades gracefully to {@code FactEvidence}-only nodes when no richer
 * {@code EvidenceItem} exists yet for a given Fact.
 *
 * AUTHORIZATION: every public method checks
 * {@code FactAuthorizationService.assertCanView} exactly once, at the
 * traversal's entry point (the id the caller actually supplied), before any
 * repository read - identical to {@code ExplainabilityService} and
 * {@code TemporalFactResolver}. Every subsequent hop is resolved by
 * following foreign keys from records already known to belong to that
 * authorized subject; no method here accepts a second id from the caller
 * mid-traversal (see the design document, section 20, for the full IDOR
 * analysis this implements).
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class EvidenceGraphService {

    private final FactRepository factRepository;
    private final FactEvidenceRepository factEvidenceRepository;
    private final FactConflictRepository factConflictRepository;
    private final EvidenceItemRepository evidenceItemRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentRepository documentRepository;
    private final RequirementEvaluationFactRepository requirementEvaluationFactRepository;
    private final RequirementEvaluationRepository requirementEvaluationRepository;
    private final RequirementRepository requirementRepository;
    private final PathwayAssessmentRepository pathwayAssessmentRepository;
    private final FactAuthorizationService factAuthorizationService;
    private final ExplainabilityService explainabilityService;
    private final RegulatoryVersionRepository regulatoryVersionRepository;
    private final RequirementEvaluationConflictRepository requirementEvaluationConflictRepository;

    // =========================================================================
    // PER-FACT GRAPH: DOCUMENT <-> EVIDENCE <-> FACT <-> CONFLICT, ONE HOP UP
    // =========================================================================

    @Transactional(readOnly = true)
    public EvidenceGraphResponse graphForFact(AuthenticatedUser actor, Long factId) {

        Fact fact = factRepository.findById(factId)
                .orElseThrow(() -> new ResourceNotFoundException("Fact not found."));

        factAuthorizationService.assertCanView(
                actor, fact.getSubjectUserId(), fact.getSensitivityTier(), fact.getId(), "view evidence graph"
        );

        Map<String, GraphNodeResponse> nodesById = new LinkedHashMap<>();
        List<GraphEdgeResponse> edges = new ArrayList<>();

        String factNodeId = addNode(nodesById, factNode(fact));

        addEvidenceSubgraph(fact, factNodeId, nodesById, edges);
        addConflictSubgraph(fact, factNodeId, nodesById, edges);
        addRequirementEvaluationSubgraph(fact, factNodeId, nodesById, edges);

        return new EvidenceGraphResponse(List.copyOf(nodesById.values()), edges);
    }

    // =========================================================================
    // PER-DOCUMENT GRAPH: BACKWARD TRACE + DOCUMENT IMPACT
    //
    // Document -> DocumentVersion -> EvidenceItem -> Fact (via the reverse
    // FactEvidence.evidenceItemId lookup already used by
    // getEvidenceItemDetail), then the SAME conflict/requirement-evaluation
    // subgraphs graphForFact already builds - reused, not duplicated, for
    // every Fact this document is found to support.
    // =========================================================================

    @Transactional(readOnly = true)
    public EvidenceGraphResponse graphForDocument(AuthenticatedUser actor, Long documentId) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found."));

        // Document already carries its own owning subject directly - the
        // same ownership boundary DocumentRepository.findByIdAndUserId
        // enforces elsewhere in this codebase, checked here through the
        // one shared authorization service rather than a parallel rule.
        factAuthorizationService.assertCanView(actor, document.getUserId(), null, null, "view evidence graph");

        Map<String, GraphNodeResponse> nodesById = new LinkedHashMap<>();
        List<GraphEdgeResponse> edges = new ArrayList<>();

        String documentNodeId = addNode(nodesById, documentNode(document));

        for (DocumentVersion version : documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)) {

            String versionNodeId = addNode(nodesById, documentVersionNode(version));
            edges.add(edge(GraphRelationshipType.HAS_VERSION, documentNodeId, versionNodeId, Map.of()));

            for (EvidenceItem evidenceItem : evidenceItemRepository.findByDocumentVersionId(version.getId())) {

                for (FactEvidence factEvidence : factEvidenceRepository.findByEvidenceItemId(evidenceItem.getId())) {

                    Fact fact = factRepository.findById(factEvidence.getFactId()).orElse(null);

                    // Defense in depth: never surface a Fact belonging to a
                    // different subject even if a row technically links here.
                    if (fact == null || !fact.getSubjectUserId().equals(document.getUserId())) {
                        continue;
                    }

                    String evidenceNodeId = addNode(nodesById, evidenceNode(factEvidence));
                    edges.add(edge(GraphRelationshipType.PRODUCES, versionNodeId, evidenceNodeId, Map.of()));

                    String factNodeId = addNode(nodesById, factNode(fact));
                    edges.add(edge(GraphRelationshipType.SUPPORTS_FACT, evidenceNodeId, factNodeId, Map.of()));

                    // Reuse, never duplicate: the same conflict and
                    // requirement-evaluation subgraphs graphForFact builds.
                    addConflictSubgraph(fact, factNodeId, nodesById, edges);
                    addRequirementEvaluationSubgraph(fact, factNodeId, nodesById, edges);
                }
            }
        }

        return new EvidenceGraphResponse(List.copyOf(nodesById.values()), edges);
    }

    // =========================================================================
    // PER-REQUIREMENT-EVALUATION GRAPH: REQUIREMENT TRACEABILITY
    //
    // Evaluation -> Requirement -> RegulatoryVersion, Evaluation -> Facts
    // (-> their own evidence/document/conflict subgraphs, reused from
    // graphForFact), and Evaluation -> Conflict for every FactConflict this
    // evaluation is recorded as blocked by (RequirementEvaluationConflict) -
    // the already-declared BLOCKED_BY_CONFLICT edge type, wired up here.
    // =========================================================================

    @Transactional(readOnly = true)
    public EvidenceGraphResponse graphForRequirementEvaluation(AuthenticatedUser actor, Long evaluationId) {

        RequirementEvaluation evaluation = requirementEvaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement evaluation not found."));

        factAuthorizationService.assertCanView(
                actor, evaluation.getSubjectUserId(), null, null, "view requirement evaluation graph"
        );

        Map<String, GraphNodeResponse> nodesById = new LinkedHashMap<>();
        List<GraphEdgeResponse> edges = new ArrayList<>();

        String evaluationNodeId = addNode(nodesById, evaluationNode(evaluation));

        requirementRepository.findById(evaluation.getRequirementId()).ifPresent(requirement -> {
            String requirementNodeId = addNode(nodesById, requirementNode(requirement));
            edges.add(edge(GraphRelationshipType.EVALUATED_UNDER, evaluationNodeId, requirementNodeId, Map.of()));
            addRegulatoryVersionSubgraph(requirement, requirementNodeId, nodesById, edges);
        });

        for (RequirementEvaluationFact link : requirementEvaluationFactRepository.findByEvaluationId(evaluationId)) {

            Fact fact = factRepository.findById(link.getFactId()).orElse(null);

            if (fact == null || !fact.getSubjectUserId().equals(evaluation.getSubjectUserId())) {
                continue;
            }

            String factNodeId = addNode(nodesById, factNode(fact));
            edges.add(edge(GraphRelationshipType.DEPENDS_ON_FACT, evaluationNodeId, factNodeId, Map.of()));

            // Reuse, never duplicate: the same evidence/document chain and
            // fact-to-fact conflicts graphForFact already builds.
            addEvidenceSubgraph(fact, factNodeId, nodesById, edges);
            addConflictSubgraph(fact, factNodeId, nodesById, edges);
        }

        for (RequirementEvaluationConflict link : requirementEvaluationConflictRepository.findByEvaluationId(evaluationId)) {

            FactConflict conflict = factConflictRepository.findById(link.getConflictId()).orElse(null);

            if (conflict == null || !conflict.getSubjectUserId().equals(evaluation.getSubjectUserId())) {
                continue;
            }

            String conflictNodeId = addNode(nodesById, conflictNode(conflict));
            edges.add(edge(GraphRelationshipType.BLOCKED_BY_CONFLICT, evaluationNodeId, conflictNodeId, Map.of()));

            addConflictSideNode(conflict.getFactAId(), conflictNodeId, evaluation.getSubjectUserId(), nodesById, edges);
            addConflictSideNode(conflict.getFactBId(), conflictNodeId, evaluation.getSubjectUserId(), nodesById, edges);
        }

        return new EvidenceGraphResponse(List.copyOf(nodesById.values()), edges);
    }

    /** One side (Fact A or Fact B) of a {@link FactConflict}, linked to its CONFLICT node - never fabricated if the Fact cannot be authorized. */
    private void addConflictSideNode(
            Long factId,
            String conflictNodeId,
            Long expectedSubjectUserId,
            Map<String, GraphNodeResponse> nodesById,
            List<GraphEdgeResponse> edges
    ) {

        Fact fact = factRepository.findById(factId).orElse(null);

        if (fact == null || !fact.getSubjectUserId().equals(expectedSubjectUserId)) {
            return;
        }

        String factNodeId = addNode(nodesById, factNode(fact));
        edges.add(edge(GraphRelationshipType.CONFLICTS_WITH, factNodeId, conflictNodeId, Map.of()));
    }

    private void addEvidenceSubgraph(
            Fact fact,
            String factNodeId,
            Map<String, GraphNodeResponse> nodesById,
            List<GraphEdgeResponse> edges
    ) {

        for (FactEvidence factEvidence : factEvidenceRepository.findByFactId(fact.getId())) {

            String evidenceNodeId = addNode(nodesById, evidenceNode(factEvidence));
            edges.add(edge(GraphRelationshipType.SUPPORTS_FACT, evidenceNodeId, factNodeId, Map.of()));

            EvidenceItem evidenceItem = factEvidence.getEvidenceItemId() != null
                    ? evidenceItemRepository.findById(factEvidence.getEvidenceItemId()).orElse(null)
                    : null;

            if (evidenceItem != null && evidenceItem.getDocumentVersionId() != null) {

                documentVersionRepository.findById(evidenceItem.getDocumentVersionId()).ifPresent(version -> {

                    String versionNodeId = addNode(nodesById, documentVersionNode(version));
                    edges.add(edge(GraphRelationshipType.PRODUCES, versionNodeId, evidenceNodeId, Map.of()));

                    documentRepository.findById(version.getDocumentId()).ifPresent(document -> {
                        String documentNodeId = addNode(nodesById, documentNode(document));
                        edges.add(edge(GraphRelationshipType.HAS_VERSION, documentNodeId, versionNodeId, Map.of()));
                    });
                });

            } else if (factEvidence.getDocumentId() != null) {

                // Graceful degradation: no EvidenceItem/DocumentVersion exists
                // yet for this (pre-existing) FactEvidence row - render the
                // Document directly, exactly as much of the chain as
                // actually exists, never a fabricated intermediate node.
                documentRepository.findById(factEvidence.getDocumentId()).ifPresent(document -> {
                    String documentNodeId = addNode(nodesById, documentNode(document));
                    edges.add(edge(GraphRelationshipType.PRODUCES, documentNodeId, evidenceNodeId, Map.of()));
                });
            }
        }
    }

    private void addConflictSubgraph(
            Fact fact,
            String factNodeId,
            Map<String, GraphNodeResponse> nodesById,
            List<GraphEdgeResponse> edges
    ) {

        for (FactConflict conflict : factConflictRepository.findByFactAIdOrFactBId(fact.getId(), fact.getId())) {

            Long otherFactId = conflict.getFactAId().equals(fact.getId()) ? conflict.getFactBId() : conflict.getFactAId();

            factRepository.findById(otherFactId).ifPresent(otherFact -> {

                // Defense in depth: an otherwise-unreachable cross-subject
                // conflict should never surface another subject's Fact.
                if (!otherFact.getSubjectUserId().equals(fact.getSubjectUserId())) {
                    return;
                }

                String otherNodeId = addNode(nodesById, factNode(otherFact));

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("conflictId", conflict.getId());
                metadata.put("status", conflict.getStatus());
                metadata.put("resolutionType", conflict.getResolutionType());

                edges.add(edge(GraphRelationshipType.CONFLICTS_WITH, factNodeId, otherNodeId, metadata));
            });
        }
    }

    private void addRequirementEvaluationSubgraph(
            Fact fact,
            String factNodeId,
            Map<String, GraphNodeResponse> nodesById,
            List<GraphEdgeResponse> edges
    ) {

        for (RequirementEvaluationFact link : requirementEvaluationFactRepository.findByFactId(fact.getId())) {

            RequirementEvaluation evaluation = requirementEvaluationRepository.findById(link.getEvaluationId()).orElse(null);

            if (evaluation == null || !evaluation.getSubjectUserId().equals(fact.getSubjectUserId())) {
                continue;
            }

            String evaluationNodeId = addNode(nodesById, evaluationNode(evaluation));
            edges.add(edge(GraphRelationshipType.DEPENDS_ON_FACT, evaluationNodeId, factNodeId, Map.of()));

            requirementRepository.findById(evaluation.getRequirementId()).ifPresent(requirement -> {
                String requirementNodeId = addNode(nodesById, requirementNode(requirement));
                edges.add(edge(GraphRelationshipType.EVALUATED_UNDER, evaluationNodeId, requirementNodeId, Map.of()));
                addRegulatoryVersionSubgraph(requirement, requirementNodeId, nodesById, edges);
            });
        }
    }

    /** Requirement -> RegulatoryVersion (regulatory traceability) - the already-declared VERSIONED_UNDER edge, wired up. */
    private void addRegulatoryVersionSubgraph(
            Requirement requirement,
            String requirementNodeId,
            Map<String, GraphNodeResponse> nodesById,
            List<GraphEdgeResponse> edges
    ) {

        regulatoryVersionRepository.findById(requirement.getRegulatoryVersionId()).ifPresent(version -> {
            String versionNodeId = addNode(nodesById, regulatoryVersionNode(version));
            edges.add(edge(GraphRelationshipType.VERSIONED_UNDER, requirementNodeId, versionNodeId, Map.of()));
        });
    }

    // =========================================================================
    // EVIDENCE ITEM DETAIL PANEL
    // =========================================================================

    @Transactional(readOnly = true)
    public EvidenceItemResponse getEvidenceItemDetail(AuthenticatedUser actor, Long evidenceItemId) {

        EvidenceItem item = evidenceItemRepository.findById(evidenceItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence item not found."));

        // An EvidenceItem has no subjectUserId of its own - authorize
        // against every Fact it is actually linked to via FactEvidence, and
        // fail closed if it is linked to none the caller may view.
        List<FactEvidence> links = factEvidenceRepository.findByEvidenceItemId(evidenceItemId);

        Fact authorizedFact = null;

        for (FactEvidence link : links) {

            Fact candidate = factRepository.findById(link.getFactId()).orElse(null);

            if (candidate == null) {
                continue;
            }

            try {
                factAuthorizationService.assertCanView(
                        actor, candidate.getSubjectUserId(), candidate.getSensitivityTier(), candidate.getId(), "view evidence item"
                );
                authorizedFact = candidate;
                break;

            } catch (org.springframework.security.access.AccessDeniedException ignored) {
                // Try the next linked Fact - this caller may still be
                // authorized to view a different Fact this evidence supports.
            }
        }

        if (authorizedFact == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to view this evidence item."
            );
        }

        DocumentVersion version = item.getDocumentVersionId() != null
                ? documentVersionRepository.findById(item.getDocumentVersionId()).orElse(null)
                : null;

        Document document = version != null
                ? documentRepository.findById(version.getDocumentId()).orElse(null)
                : null;

        return toEvidenceItemResponse(item, version, document, authorizedFact);
    }

    // =========================================================================
    // FULL TRACE: PATHWAY -> ... -> DOCUMENT, COMPOSED AROUND EXPLAINABILITY
    // =========================================================================

    @Transactional(readOnly = true)
    public EvidenceFullTraceResponse fullTrace(AuthenticatedUser actor, Long pathwayAssessmentId) {

        PathwayAssessment assessment = pathwayAssessmentRepository.findById(pathwayAssessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pathway assessment not found."));

        factAuthorizationService.assertCanView(actor, assessment.getSubjectUserId(), null, null, "view evidence trace");

        // ExplainabilityService re-checks authorization itself (harmless,
        // same boundary) - never bypassed or duplicated with looser rules.
        ExplanationResponse explanation = explainabilityService.explain(actor, pathwayAssessmentId);

        Map<Long, List<EvidenceItemResponse>> evidenceByFactId = new LinkedHashMap<>();

        for (ExplanationResponse.RequirementExplanation requirementExplanation : explanation.requirements()) {

            for (ExplanationResponse.FactExplanation factExplanation : requirementExplanation.contributingFacts()) {

                List<EvidenceItemResponse> evidenceResponses = new ArrayList<>();

                for (var factEvidenceResponse : factExplanation.evidence()) {

                    Long evidenceItemId = factEvidenceRepository.findById(factEvidenceResponse.id())
                            .map(FactEvidence::getEvidenceItemId)
                            .orElse(null);

                    if (evidenceItemId == null) {
                        continue;
                    }

                    EvidenceItem item = evidenceItemRepository.findById(evidenceItemId).orElse(null);

                    if (item == null) {
                        continue;
                    }

                    DocumentVersion version = item.getDocumentVersionId() != null
                            ? documentVersionRepository.findById(item.getDocumentVersionId()).orElse(null)
                            : null;

                    Document document = version != null
                            ? documentRepository.findById(version.getDocumentId()).orElse(null)
                            : null;

                    Fact fact = factRepository.findById(factExplanation.factId()).orElse(null);

                    evidenceResponses.add(toEvidenceItemResponse(item, version, document, fact));
                }

                evidenceByFactId.put(factExplanation.factId(), evidenceResponses);
            }
        }

        return new EvidenceFullTraceResponse(explanation, evidenceByFactId);
    }

    // =========================================================================
    // NODE / EDGE BUILDERS
    // =========================================================================

    private String addNode(Map<String, GraphNodeResponse> nodesById, GraphNodeResponse node) {
        nodesById.putIfAbsent(node.id(), node);
        return node.id();
    }

    private GraphEdgeResponse edge(
            GraphRelationshipType relationship,
            String sourceNodeId,
            String targetNodeId,
            Map<String, Object> metadata
    ) {
        return new GraphEdgeResponse(
                sourceNodeId + ":" + relationship.name() + ":" + targetNodeId,
                sourceNodeId,
                targetNodeId,
                relationship,
                metadata
        );
    }

    private GraphNodeResponse factNode(Fact fact) {

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("factKey", fact.getFactKey());
        metadata.put("category", fact.getCategory());
        metadata.put("status", fact.getStatus());
        metadata.put("provenanceType", fact.getProvenanceType());
        metadata.put("confidenceLevel", fact.getConfidenceLevel());
        metadata.put("confidenceScore", fact.getConfidenceScore());
        metadata.put("isVerified", fact.getIsVerified());
        metadata.put("valueSummary", valueSummary(fact));

        return new GraphNodeResponse(
                nodeId(GraphNodeType.FACT, fact.getId()),
                GraphNodeType.FACT,
                fact.getFactKey(),
                metadata
        );
    }

    private GraphNodeResponse evidenceNode(FactEvidence factEvidence) {

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sourceType", factEvidence.getSourceType());
        metadata.put("evidenceItemId", factEvidence.getEvidenceItemId());
        metadata.put("capturedAt", factEvidence.getCapturedAt());

        return new GraphNodeResponse(
                nodeId(GraphNodeType.EVIDENCE, factEvidence.getId()),
                GraphNodeType.EVIDENCE,
                "Evidence #" + factEvidence.getId(),
                metadata
        );
    }

    private GraphNodeResponse documentVersionNode(DocumentVersion version) {

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("versionNumber", version.getVersionNumber());
        metadata.put("extractionMethod", version.getExtractionMethod());
        metadata.put("createdAt", version.getCreatedAt());

        return new GraphNodeResponse(
                nodeId(GraphNodeType.DOCUMENT_VERSION, version.getId()),
                GraphNodeType.DOCUMENT_VERSION,
                "Version " + version.getVersionNumber(),
                metadata
        );
    }

    private GraphNodeResponse documentNode(Document document) {

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("fileName", document.getFileName());
        metadata.put("documentType", document.getDocumentType());
        metadata.put("uploadedAt", document.getUploadedAt());

        // Legacy, explicitly non-forensic risk indicator (see Document
        // Javadoc) - surfaced here only as a signal for a case worker to
        // look closer, never rendered or interpreted as fraud proof.
        metadata.put("fraudDetected", document.getFraudDetected());
        metadata.put("riskLevel", document.getRiskLevel());

        return new GraphNodeResponse(
                nodeId(GraphNodeType.DOCUMENT, document.getId()),
                GraphNodeType.DOCUMENT,
                document.getFileName(),
                metadata
        );
    }

    private GraphNodeResponse regulatoryVersionNode(RegulatoryVersion version) {

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("regulationIdentity", version.getRegulationIdentity());
        metadata.put("jurisdiction", version.getJurisdiction());
        metadata.put("sourceAuthority", version.getSourceAuthority());
        metadata.put("sourceReference", version.getSourceReference());
        metadata.put("verificationStatus", version.getVerificationStatus());
        metadata.put("effectiveFrom", version.getEffectiveFrom());
        metadata.put("effectiveTo", version.getEffectiveTo());

        return new GraphNodeResponse(
                nodeId(GraphNodeType.REGULATORY_VERSION, version.getId()),
                GraphNodeType.REGULATORY_VERSION,
                version.getRegulationIdentity(),
                metadata
        );
    }

    /** CONFLICT DETECTED only, never a fraud determination - see {@link GraphNodeType#CONFLICT}. */
    private GraphNodeResponse conflictNode(FactConflict conflict) {

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("factKey", conflict.getFactKey());
        metadata.put("status", conflict.getStatus());
        metadata.put("resolutionType", conflict.getResolutionType());
        metadata.put("detectedAt", conflict.getDetectedAt());
        metadata.put("resolvedAt", conflict.getResolvedAt());

        return new GraphNodeResponse(
                nodeId(GraphNodeType.CONFLICT, conflict.getId()),
                GraphNodeType.CONFLICT,
                "Conflicting information",
                metadata
        );
    }

    private GraphNodeResponse evaluationNode(RequirementEvaluation evaluation) {

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("outcome", evaluation.getOutcome());
        metadata.put("certaintyLevel", evaluation.getCertaintyLevel());
        metadata.put("assessmentDate", evaluation.getAssessmentDate());

        return new GraphNodeResponse(
                nodeId(GraphNodeType.REQUIREMENT_EVALUATION, evaluation.getId()),
                GraphNodeType.REQUIREMENT_EVALUATION,
                "Evaluation #" + evaluation.getId(),
                metadata
        );
    }

    private GraphNodeResponse requirementNode(Requirement requirement) {

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requirementKey", requirement.getRequirementKey());
        metadata.put("requirementType", requirement.getRequirementType());
        metadata.put("mandatory", requirement.getMandatory());

        return new GraphNodeResponse(
                nodeId(GraphNodeType.REQUIREMENT, requirement.getId()),
                GraphNodeType.REQUIREMENT,
                requirement.getTitle(),
                metadata
        );
    }

    private String nodeId(GraphNodeType type, Long id) {
        return type.name().toLowerCase() + ":" + id;
    }

    private String valueSummary(Fact fact) {

        return switch (fact.getValueType()) {
            case STRING -> fact.getStringValue();
            case DATE -> fact.getDateValue() != null ? fact.getDateValue().toLocalDate().toString() : null;
            case NUMBER -> fact.getNumberValue() != null ? String.valueOf(fact.getNumberValue()) : null;
            case BOOLEAN -> fact.getBooleanValue() != null ? String.valueOf(fact.getBooleanValue()) : null;
        };
    }

    private EvidenceItemResponse toEvidenceItemResponse(
            EvidenceItem item,
            DocumentVersion version,
            Document document,
            Fact fact
    ) {

        EvidenceItemResponse.SourceData sourceData = new EvidenceItemResponse.SourceData(
                document != null ? document.getId() : null,
                document != null ? document.getFileName() : null,
                version != null ? version.getId() : null,
                version != null ? version.getVersionNumber() : null,
                item.getSourceSnippet(),
                version != null ? version.getDocumentIssueDate() : null,
                version != null ? version.getDocumentExpiryDate() : null
        );

        EvidenceItemResponse.SystemInterpretation systemInterpretation = new EvidenceItemResponse.SystemInterpretation(
                version != null ? version.getExtractionMethod() : null,
                item.getExtractionConfidence(),
                item.getExtractedAt()
        );

        EvidenceItemResponse.DerivedConclusion derivedConclusion = fact != null
                ? new EvidenceItemResponse.DerivedConclusion(
                        fact.getId(),
                        fact.getFactKey(),
                        valueSummary(fact),
                        fact.getIsVerified(),
                        fact.getConfidenceScore()
                )
                : null;

        return new EvidenceItemResponse(
                item.getId(),
                item.getSourceType(),
                item.getStatus(),
                item.getDirectness(),
                item.getRejectionReason(),
                sourceData,
                systemInterpretation,
                derivedConclusion
        );
    }
}
