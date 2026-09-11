package com.godfrey.ai_immigration_document_analyzer.evidencegraph;

import com.godfrey.ai_immigration_document_analyzer.entity.Document;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceGraphResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.GraphNodeType;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.GraphRelationshipType;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.DocumentVersion;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceDirectness;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItem;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItemStatus;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.DocumentVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.service.EvidenceGraphService;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.EvidenceSourceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactEvidence;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayStatusChangeRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RegulatoryVersionCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RegulatoryVersionResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementAdminDetailResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementFactBindingRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementStatusChangeRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatorySourceType;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayAdminService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayAssessmentService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.RegulatoryVersionAdminService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.RequirementAdminService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * EVIDENCE GRAPH PHASE 3 WORKFLOW INTEGRATION TEST
 * ============================================================================
 *
 * A genuine end-to-end exercise of the Phase 3 Advanced Evidence Graph
 * additions (backward trace from a Document, and requirement traceability
 * from a RequirementEvaluation) against the real, configured Oracle
 * instance - not mocks. Mirrors {@code CaseIntelligenceWorkflowIntegrationTest}'s
 * pattern: build a real published Pathway/Requirement, a real Document ->
 * DocumentVersion -> EvidenceItem -> Fact -> FactEvidence chain, request a
 * real PathwayAssessment, then verify both new traversals produce the
 * expected relationships - and that the pre-existing {@code graphForFact}/
 * {@code fullTrace} traversals still work unchanged (regression).
 *
 * {@code @Transactional} rolls back every write at the end of each method -
 * no test artifacts are left in the real database.
 * ============================================================================
 */
@SpringBootTest
@Transactional
class EvidenceGraphPhase3WorkflowIntegrationTest {

    @Autowired
    private RegulatoryVersionAdminService regulatoryVersionAdminService;

    @Autowired
    private RequirementAdminService requirementAdminService;

    @Autowired
    private PathwayAdminService pathwayAdminService;

    @Autowired
    private PathwayAssessmentService pathwayAssessmentService;

    @Autowired
    private EvidenceGraphService evidenceGraphService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FactRepository factRepository;

    @Autowired
    private FactEvidenceRepository factEvidenceRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private EvidenceItemRepository evidenceItemRepository;

    private static final String FACT_KEY = "EMPLOYMENT.CURRENT_EMPLOYER";

    private AuthenticatedUser actorFor(Long userId) {
        return new AuthenticatedUser(
                userId, "evidence-graph-phase3-test-" + userId + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())),
                true, true, true, true
        );
    }

    private Long createTestSubject() {
        User saved = userRepository.save(User.builder()
                .fullName("Evidence Graph Phase 3 Test Subject")
                .email("evidence-graph-phase3-subject-" + System.nanoTime() + "@example.com")
                .password("irrelevant-hash")
                .role(Role.USER)
                .build());
        return saved.getId();
    }

    private RegulatoryVersionResponse createTestRegulatoryVersion(String identity) {
        RegulatoryVersionCreateRequest request = new RegulatoryVersionCreateRequest();
        request.setRegulationIdentity(identity);
        request.setJurisdiction("Testland");
        request.setSourceType(RegulatorySourceType.AUTHORITATIVE_REGULATORY_SOURCE);
        request.setSourceAuthority("Testland Immigration Authority");
        request.setSourceReference("https://example.gov/test-source");
        request.setVerificationStatus(RegulatoryVerificationStatus.UNVERIFIED_INGESTION);
        return regulatoryVersionAdminService.create(request);
    }

    private RequirementAdminDetailResponse createAndPublishTestRequirement(Long regulatoryVersionId, String key) {
        RequirementFactBindingRequest binding = new RequirementFactBindingRequest();
        binding.setFactKey(FACT_KEY);
        binding.setRequiresVerification(false);

        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setRequirementKey(key);
        request.setRequirementType(RequirementType.EXPERIENCE);
        request.setTitle("Evidence Graph Phase 3 Test Requirement " + key);
        request.setJurisdiction("Testland");
        request.setRegulatoryVersionId(regulatoryVersionId);
        request.setMandatory(true);
        request.setSatisfactionLogicJson(
                "{\"node\":\"FACT_PREDICATE\",\"factKey\":\"" + FACT_KEY + "\",\"operator\":\"EXISTS\","
                        + "\"operandValue\":null,\"operandValues\":null,\"operandLow\":null,\"operandHigh\":null}"
        );
        request.setFactBindings(List.of(binding));

        RequirementAdminDetailResponse created = requirementAdminService.create(request);

        RequirementStatusChangeRequest publish = new RequirementStatusChangeRequest();
        publish.setTargetStatus(RequirementStatus.PUBLISHED);
        requirementAdminService.changeStatus(created.id(), publish);

        return created;
    }

    private PathwayResponse createAndPublishTestPathway(String pathwayKey, Long requirementId) {
        PathwayCreateRequest createPathway = new PathwayCreateRequest();
        createPathway.setPathwayKey(pathwayKey);
        createPathway.setName("Evidence Graph Phase 3 Test Pathway");
        createPathway.setJurisdiction("Testland");
        createPathway.setCategory("Test Category");
        createPathway.setRequirementIds(List.of(requirementId));

        PathwayResponse draft = pathwayAdminService.create(createPathway);

        PathwayStatusChangeRequest toReview = new PathwayStatusChangeRequest();
        toReview.setTargetStatus(PathwayStatus.REVIEW);
        pathwayAdminService.changeStatus(draft.id(), toReview);

        PathwayStatusChangeRequest toPublished = new PathwayStatusChangeRequest();
        toPublished.setTargetStatus(PathwayStatus.PUBLISHED);
        return pathwayAdminService.changeStatus(draft.id(), toPublished);
    }

    @Test
    void documentAndRequirementEvaluationGraphsTraceTheFullChainEndToEndAgainstOracle() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        // ---- Build a real, published Pathway/Requirement (reuses Phase 1 admin services) ----
        RegulatoryVersionResponse version =
                createTestRegulatoryVersion("TEST_EVIDENCE_GRAPH_P3_SOURCE_" + System.nanoTime());
        RequirementAdminDetailResponse requirement =
                createAndPublishTestRequirement(version.id(), "TEST.EVIDENCE_GRAPH_P3_REQ_" + System.nanoTime());
        PathwayResponse pathway =
                createAndPublishTestPathway("TEST_EVIDENCE_GRAPH_P3_PATHWAY_" + System.nanoTime(), requirement.id());

        // ---- Build a real Document -> DocumentVersion -> EvidenceItem chain ----
        Document document = documentRepository.save(Document.builder()
                .userId(subjectUserId)
                .documentType("EMPLOYMENT_LETTER")
                .fileName("employment-letter.pdf")
                .filePath("test/evidence-graph-p3/" + System.nanoTime() + ".pdf")
                .fileSize(1024L)
                .build());

        DocumentVersion documentVersion = documentVersionRepository.save(DocumentVersion.builder()
                .documentId(document.getId())
                .versionNumber(1)
                .extractionMethod("OCR_V2")
                .build());

        EvidenceItem evidenceItem = evidenceItemRepository.save(EvidenceItem.builder()
                .documentVersionId(documentVersion.getId())
                .sourceType(EvidenceSourceType.DOCUMENT)
                .status(EvidenceItemStatus.LINKED)
                .directness(EvidenceDirectness.DIRECT)
                .extractionConfidence(0.9)
                .sourceSnippet("Employed at Acme Corp since 2020.")
                .build());

        // ---- Build a real Fact + FactEvidence linking it to the document chain ----
        Fact fact = factRepository.save(Fact.builder()
                .subjectUserId(subjectUserId)
                .category(FactCategory.EMPLOYMENT)
                .factKey(FACT_KEY)
                .valueType(FactValueType.STRING)
                .stringValue("Acme Corp")
                .status(FactStatus.ACCEPTED)
                .provenanceType(FactProvenanceType.DOCUMENT_EXTRACTION)
                .createdByAccessorType(AccessorType.SYSTEM)
                .sensitivityTier(FactSensitivityTier.T2_STANDARD_PERSONAL)
                .confidenceScore(0.9)
                .confidenceLevel(FactConfidenceLevel.HIGH)
                .effectiveFrom(LocalDateTime.now().minusYears(1))
                .observedAt(LocalDateTime.now())
                .lastObservedAt(LocalDateTime.now())
                .build());

        factEvidenceRepository.save(FactEvidence.builder()
                .factId(fact.getId())
                .sourceType(EvidenceSourceType.DOCUMENT)
                .documentId(document.getId())
                .evidenceItemId(evidenceItem.getId())
                .sourceSnippet("Employed at Acme Corp since 2020.")
                .build());

        // ---- Produce a real PathwayAssessment / RequirementEvaluation ----
        var assessment = pathwayAssessmentService.assess(actor, pathway.id(), subjectUserId, null);
        assertThat(assessment.requirementEvaluations()).hasSize(1);
        Long evaluationId = assessment.requirementEvaluations().get(0).id();

        // =========================================================================
        // NEW (Phase 3): BACKWARD TRACE FROM THE DOCUMENT
        // =========================================================================

        EvidenceGraphResponse documentGraph = evidenceGraphService.graphForDocument(actor, document.getId());

        assertThat(documentGraph.nodes()).extracting("id").contains(
                "document:" + document.getId(),
                "document_version:" + documentVersion.getId(),
                "fact:" + fact.getId(),
                "requirement_evaluation:" + evaluationId,
                "requirement:" + requirement.id(),
                "regulatory_version:" + version.id()
        );

        assertThat(documentGraph.edges())
                .extracting("relationship")
                .contains(
                        GraphRelationshipType.HAS_VERSION,
                        GraphRelationshipType.PRODUCES,
                        GraphRelationshipType.SUPPORTS_FACT,
                        GraphRelationshipType.DEPENDS_ON_FACT,
                        GraphRelationshipType.EVALUATED_UNDER,
                        GraphRelationshipType.VERSIONED_UNDER
                );

        // Legacy, non-forensic risk signal surfaced - never a fraud label.
        var documentNode = documentGraph.nodes().stream()
                .filter(n -> n.type() == GraphNodeType.DOCUMENT)
                .findFirst().orElseThrow();
        assertThat(documentNode.metadata()).containsKeys("fraudDetected", "riskLevel");

        // =========================================================================
        // NEW (Phase 3): REQUIREMENT TRACEABILITY FROM THE EVALUATION
        // =========================================================================

        EvidenceGraphResponse evaluationGraph = evidenceGraphService.graphForRequirementEvaluation(actor, evaluationId);

        assertThat(evaluationGraph.nodes()).extracting("id").contains(
                "requirement_evaluation:" + evaluationId,
                "requirement:" + requirement.id(),
                "regulatory_version:" + version.id(),
                "fact:" + fact.getId(),
                "evidence:%s".formatted(
                        factEvidenceRepository.findByFactId(fact.getId()).get(0).getId()
                ),
                "document:" + document.getId()
        );

        assertThat(evaluationGraph.edges())
                .extracting("relationship")
                .contains(
                        GraphRelationshipType.EVALUATED_UNDER,
                        GraphRelationshipType.VERSIONED_UNDER,
                        GraphRelationshipType.DEPENDS_ON_FACT,
                        GraphRelationshipType.SUPPORTS_FACT
                );

        // =========================================================================
        // REGRESSION: PRE-EXISTING graphForFact / fullTrace STILL WORK UNCHANGED
        // =========================================================================

        EvidenceGraphResponse factGraph = evidenceGraphService.graphForFact(actor, fact.getId());
        assertThat(factGraph.nodes()).extracting("id").contains(
                "fact:" + fact.getId(), "document:" + document.getId(), "requirement:" + requirement.id()
        );

        var fullTrace = evidenceGraphService.fullTrace(actor, assessment.id());
        assertThat(fullTrace.explanation().requirements()).hasSize(1);
        assertThat(fullTrace.evidenceByFactId()).containsKey(fact.getId());
        assertThat(fullTrace.evidenceByFactId().get(fact.getId())).isNotEmpty();
    }
}
