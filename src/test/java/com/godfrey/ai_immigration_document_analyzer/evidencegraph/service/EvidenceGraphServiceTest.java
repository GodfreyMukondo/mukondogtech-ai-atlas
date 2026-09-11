package com.godfrey.ai_immigration_document_analyzer.evidencegraph.service;

import com.godfrey.ai_immigration_document_analyzer.entity.Document;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceGraphResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceItemResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.GraphRelationshipType;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.DocumentVersion;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceDirectness;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItem;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItemStatus;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.DocumentVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.ConflictStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.EvidenceSourceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConflict;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactEvidence;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RegulatoryVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationFactRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationConflict;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationFact;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.ExplainabilityService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EvidenceGraphService} - the Evidence Intelligence
 * Graph's single orchestration entry point.
 *
 * Focus areas: authorization is checked before any repository read beyond
 * the entry record itself (no IDOR path), the per-Fact graph degrades
 * gracefully to FactEvidence-only nodes when no EvidenceItem exists, the
 * full chain is rendered when one does, cross-subject data is never
 * surfaced even if a query technically returns it, and evidence-item
 * detail authorization fails closed when no linked Fact is viewable.
 */
@ExtendWith(MockitoExtension.class)
class EvidenceGraphServiceTest {

    private static final Long SUBJECT_ID = 10L;
    private static final Long STRANGER_ID = 20L;

    @Mock private FactRepository factRepository;
    @Mock private FactEvidenceRepository factEvidenceRepository;
    @Mock private FactConflictRepository factConflictRepository;
    @Mock private EvidenceItemRepository evidenceItemRepository;
    @Mock private DocumentVersionRepository documentVersionRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private RequirementEvaluationFactRepository requirementEvaluationFactRepository;
    @Mock private RequirementEvaluationRepository requirementEvaluationRepository;
    @Mock private RequirementRepository requirementRepository;
    @Mock private PathwayAssessmentRepository pathwayAssessmentRepository;
    @Mock private FactAuthorizationService factAuthorizationService;
    @Mock private ExplainabilityService explainabilityService;
    @Mock private RegulatoryVersionRepository regulatoryVersionRepository;
    @Mock private RequirementEvaluationConflictRepository requirementEvaluationConflictRepository;

    private EvidenceGraphService evidenceGraphService;

    @BeforeEach
    void setUp() {

        evidenceGraphService = new EvidenceGraphService(
                factRepository, factEvidenceRepository, factConflictRepository, evidenceItemRepository,
                documentVersionRepository, documentRepository, requirementEvaluationFactRepository,
                requirementEvaluationRepository, requirementRepository, pathwayAssessmentRepository,
                factAuthorizationService, explainabilityService, regulatoryVersionRepository,
                requirementEvaluationConflictRepository
        );

        org.mockito.Mockito.lenient().when(factConflictRepository.findByFactAIdOrFactBId(anyLong(), anyLong()))
                .thenReturn(List.of());
        org.mockito.Mockito.lenient().when(requirementEvaluationFactRepository.findByFactId(anyLong()))
                .thenReturn(List.of());
        org.mockito.Mockito.lenient().when(factEvidenceRepository.findByFactId(anyLong()))
                .thenReturn(List.of());
    }

    private AuthenticatedUser user(Long id, Role... roles) {

        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .toList();

        return new AuthenticatedUser(id, "user" + id + "@example.com", "hash", authorities, true, true, true, true);
    }

    private Fact fact(Long id, Long subjectUserId) {

        return Fact.builder()
                .id(id)
                .subjectUserId(subjectUserId)
                .category(FactCategory.NATIONALITY_CITIZENSHIP)
                .factKey("NATIONALITY_CITIZENSHIP.CURRENT_NATIONALITY")
                .valueType(FactValueType.STRING)
                .stringValue("Zimbabwe")
                .status(FactStatus.ACCEPTED)
                .provenanceType(com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType.DOCUMENT_EXTRACTION)
                .confidenceScore(0.6)
                .confidenceLevel(com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel.MODERATE)
                .isVerified(false)
                .observedAt(LocalDateTime.now())
                .lastObservedAt(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // AUTHORIZATION / IDOR
    // =========================================================================

    @Test
    void graphForFactThrowsResourceNotFoundWhenMissing() {

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        when(factRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evidenceGraphService.graphForFact(actor, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void graphForFactDeniesAStrangerBeforeReadingEvidenceOrConflicts() {

        Fact fact = fact(1L, SUBJECT_ID);
        AuthenticatedUser stranger = user(STRANGER_ID, Role.USER);

        when(factRepository.findById(1L)).thenReturn(Optional.of(fact));

        doThrow(new AccessDeniedException("denied")).when(factAuthorizationService)
                .assertCanView(eq(stranger), eq(SUBJECT_ID), any(), eq(1L), org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(() -> evidenceGraphService.graphForFact(stranger, 1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(factEvidenceRepository, never()).findByFactId(any());
        verify(factConflictRepository, never()).findByFactAIdOrFactBId(any(), any());
    }

    // =========================================================================
    // GRACEFUL DEGRADATION - NO EVIDENCEITEM EXISTS YET
    // =========================================================================

    @Test
    void graphForFactRendersDocumentDirectlyWhenNoEvidenceItemExists() {

        Fact fact = fact(1L, SUBJECT_ID);
        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        FactEvidence legacyEvidence = FactEvidence.builder()
                .id(500L).factId(1L).sourceType(EvidenceSourceType.DOCUMENT).documentId(55L)
                .evidenceItemId(null) // pre-existing row, no EvidenceItem enrichment
                .capturedAt(LocalDateTime.now())
                .build();

        Document document = Document.builder().id(55L).userId(SUBJECT_ID).fileName("passport.pdf")
                .documentType("PASSPORT").filePath("x").fileSize(1L).uploadedAt(LocalDateTime.now()).build();

        when(factRepository.findById(1L)).thenReturn(Optional.of(fact));
        when(factEvidenceRepository.findByFactId(1L)).thenReturn(List.of(legacyEvidence));
        when(documentRepository.findById(55L)).thenReturn(Optional.of(document));

        EvidenceGraphResponse response = evidenceGraphService.graphForFact(subject, 1L);

        assertThat(response.nodes()).extracting("id").contains("fact:1", "evidence:500", "document:55");
        assertThat(response.edges())
                .anySatisfy(e -> {
                    assertThat(e.relationship()).isEqualTo(GraphRelationshipType.SUPPORTS_FACT);
                    assertThat(e.sourceNodeId()).isEqualTo("evidence:500");
                    assertThat(e.targetNodeId()).isEqualTo("fact:1");
                })
                .anySatisfy(e -> {
                    assertThat(e.relationship()).isEqualTo(GraphRelationshipType.PRODUCES);
                    assertThat(e.sourceNodeId()).isEqualTo("document:55");
                    assertThat(e.targetNodeId()).isEqualTo("evidence:500");
                });

        // No DocumentVersion/EvidenceItem lookup should occur when the
        // legacy row carries no evidenceItemId.
        verify(evidenceItemRepository, never()).findById(any());
    }

    // =========================================================================
    // FULL CHAIN - EVIDENCEITEM + DOCUMENTVERSION PRESENT
    // =========================================================================

    @Test
    void graphForFactRendersFullChainWhenEvidenceItemExists() {

        Fact fact = fact(1L, SUBJECT_ID);
        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        FactEvidence enrichedEvidence = FactEvidence.builder()
                .id(501L).factId(1L).sourceType(EvidenceSourceType.DOCUMENT).documentId(55L)
                .evidenceItemId(900L)
                .capturedAt(LocalDateTime.now())
                .build();

        EvidenceItem evidenceItem = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).sourceType(EvidenceSourceType.DOCUMENT)
                .status(EvidenceItemStatus.LINKED).directness(EvidenceDirectness.DIRECT)
                .extractionConfidence(0.88).extractedAt(LocalDateTime.now())
                .build();

        DocumentVersion version = DocumentVersion.builder()
                .id(1L).documentId(55L).versionNumber(1).extractionMethod("OCR_V2")
                .createdAt(LocalDateTime.now())
                .build();

        Document document = Document.builder().id(55L).userId(SUBJECT_ID).fileName("passport.pdf")
                .documentType("PASSPORT").filePath("x").fileSize(1L).uploadedAt(LocalDateTime.now()).build();

        when(factRepository.findById(1L)).thenReturn(Optional.of(fact));
        when(factEvidenceRepository.findByFactId(1L)).thenReturn(List.of(enrichedEvidence));
        when(evidenceItemRepository.findById(900L)).thenReturn(Optional.of(evidenceItem));
        when(documentVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(documentRepository.findById(55L)).thenReturn(Optional.of(document));

        EvidenceGraphResponse response = evidenceGraphService.graphForFact(subject, 1L);

        assertThat(response.nodes()).extracting("id")
                .contains("fact:1", "evidence:501", "document_version:1", "document:55");

        assertThat(response.edges())
                .anySatisfy(e -> assertThat(e.relationship()).isEqualTo(GraphRelationshipType.HAS_VERSION))
                .anySatisfy(e -> assertThat(e.relationship()).isEqualTo(GraphRelationshipType.PRODUCES))
                .anySatisfy(e -> assertThat(e.relationship()).isEqualTo(GraphRelationshipType.SUPPORTS_FACT));
    }

    // =========================================================================
    // CONFLICT CLUSTER
    // =========================================================================

    @Test
    void graphForFactIncludesConflictingFactAsSeparateNodeNeverDeleted() {

        Fact fact = fact(1L, SUBJECT_ID);
        Fact otherFact = fact(2L, SUBJECT_ID);
        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        FactConflict conflict = FactConflict.builder()
                .id(70L).subjectUserId(SUBJECT_ID).factKey(fact.getFactKey())
                .factAId(1L).factBId(2L).status(ConflictStatus.OPEN)
                .build();

        when(factRepository.findById(1L)).thenReturn(Optional.of(fact));
        when(factRepository.findById(2L)).thenReturn(Optional.of(otherFact));
        when(factConflictRepository.findByFactAIdOrFactBId(1L, 1L)).thenReturn(List.of(conflict));

        EvidenceGraphResponse response = evidenceGraphService.graphForFact(subject, 1L);

        assertThat(response.nodes()).extracting("id").contains("fact:1", "fact:2");
        assertThat(response.edges()).anySatisfy(e -> {
            assertThat(e.relationship()).isEqualTo(GraphRelationshipType.CONFLICTS_WITH);
            assertThat(e.metadata()).containsEntry("status", ConflictStatus.OPEN);
        });
    }

    @Test
    void graphForFactNeverSurfacesAConflictingFactBelongingToAnotherSubject() {

        Fact fact = fact(1L, SUBJECT_ID);
        Fact crossSubjectFact = fact(2L, STRANGER_ID); // defense-in-depth scenario
        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        FactConflict conflict = FactConflict.builder()
                .id(70L).subjectUserId(SUBJECT_ID).factKey(fact.getFactKey())
                .factAId(1L).factBId(2L).status(ConflictStatus.OPEN)
                .build();

        when(factRepository.findById(1L)).thenReturn(Optional.of(fact));
        when(factRepository.findById(2L)).thenReturn(Optional.of(crossSubjectFact));
        when(factConflictRepository.findByFactAIdOrFactBId(1L, 1L)).thenReturn(List.of(conflict));

        EvidenceGraphResponse response = evidenceGraphService.graphForFact(subject, 1L);

        assertThat(response.nodes()).extracting("id").doesNotContain("fact:2");
    }

    // =========================================================================
    // REQUIREMENT EVALUATION UPWARD HOP
    // =========================================================================

    @Test
    void graphForFactIncludesDependingRequirementEvaluation() {

        Fact fact = fact(1L, SUBJECT_ID);
        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        RequirementEvaluationFact link = RequirementEvaluationFact.builder()
                .id(1L).evaluationId(4001L).factId(1L).build();

        RequirementEvaluation evaluation = RequirementEvaluation.builder()
                .id(4001L).requirementId(12L).regulatoryVersionId(88L).subjectUserId(SUBJECT_ID)
                .assessmentDate(LocalDateTime.now())
                .outcome(com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome.SATISFIED)
                .certaintyLevel(com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel.MODERATE)
                .build();

        when(factRepository.findById(1L)).thenReturn(Optional.of(fact));
        when(requirementEvaluationFactRepository.findByFactId(1L)).thenReturn(List.of(link));
        when(requirementEvaluationRepository.findById(4001L)).thenReturn(Optional.of(evaluation));
        when(requirementRepository.findById(12L)).thenReturn(Optional.empty());

        EvidenceGraphResponse response = evidenceGraphService.graphForFact(subject, 1L);

        assertThat(response.nodes()).extracting("id").contains("requirement_evaluation:4001");
        assertThat(response.edges()).anySatisfy(e ->
                assertThat(e.relationship()).isEqualTo(GraphRelationshipType.DEPENDS_ON_FACT)
        );
    }

    @Test
    void graphForFactIgnoresAnEvaluationBelongingToAnotherSubject() {

        Fact fact = fact(1L, SUBJECT_ID);
        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        RequirementEvaluationFact link = RequirementEvaluationFact.builder()
                .id(1L).evaluationId(4001L).factId(1L).build();

        RequirementEvaluation crossSubjectEvaluation = RequirementEvaluation.builder()
                .id(4001L).requirementId(12L).regulatoryVersionId(88L).subjectUserId(STRANGER_ID)
                .assessmentDate(LocalDateTime.now())
                .outcome(com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome.SATISFIED)
                .certaintyLevel(com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel.MODERATE)
                .build();

        when(factRepository.findById(1L)).thenReturn(Optional.of(fact));
        when(requirementEvaluationFactRepository.findByFactId(1L)).thenReturn(List.of(link));
        when(requirementEvaluationRepository.findById(4001L)).thenReturn(Optional.of(crossSubjectEvaluation));

        EvidenceGraphResponse response = evidenceGraphService.graphForFact(subject, 1L);

        assertThat(response.nodes()).extracting("id").doesNotContain("requirement_evaluation:4001");
    }

    // =========================================================================
    // EVIDENCE ITEM DETAIL - AUTHORIZATION FAILS CLOSED
    // =========================================================================

    @Test
    void getEvidenceItemDetailThrowsAccessDeniedWhenNoLinkedFactIsViewable() {

        AuthenticatedUser stranger = user(STRANGER_ID, Role.USER);

        EvidenceItem item = EvidenceItem.builder()
                .id(900L).sourceType(EvidenceSourceType.DOCUMENT).status(EvidenceItemStatus.LINKED)
                .extractedAt(LocalDateTime.now())
                .build();

        FactEvidence link = FactEvidence.builder()
                .id(501L).factId(1L).sourceType(EvidenceSourceType.DOCUMENT)
                .evidenceItemId(900L).capturedAt(LocalDateTime.now())
                .build();

        Fact owningFact = fact(1L, SUBJECT_ID);

        when(evidenceItemRepository.findById(900L)).thenReturn(Optional.of(item));
        when(factEvidenceRepository.findByEvidenceItemId(900L)).thenReturn(List.of(link));
        when(factRepository.findById(1L)).thenReturn(Optional.of(owningFact));

        doThrow(new AccessDeniedException("denied")).when(factAuthorizationService)
                .assertCanView(eq(stranger), eq(SUBJECT_ID), any(), eq(1L), org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(() -> evidenceGraphService.getEvidenceItemDetail(stranger, 900L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getEvidenceItemDetailSeparatesSourceDataFromDerivedConclusion() {

        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        EvidenceItem item = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).sourceType(EvidenceSourceType.DOCUMENT)
                .status(EvidenceItemStatus.LINKED).directness(EvidenceDirectness.DIRECT)
                .extractionConfidence(0.88).sourceSnippet("Nationality: Zimbabwe")
                .extractedAt(LocalDateTime.now())
                .build();

        FactEvidence link = FactEvidence.builder()
                .id(501L).factId(1L).sourceType(EvidenceSourceType.DOCUMENT)
                .evidenceItemId(900L).capturedAt(LocalDateTime.now())
                .build();

        Fact owningFact = fact(1L, SUBJECT_ID);

        DocumentVersion version = DocumentVersion.builder()
                .id(1L).documentId(55L).versionNumber(1).extractionMethod("OCR_V2")
                .createdAt(LocalDateTime.now())
                .build();

        Document document = Document.builder().id(55L).userId(SUBJECT_ID).fileName("passport.pdf")
                .documentType("PASSPORT").filePath("x").fileSize(1L).uploadedAt(LocalDateTime.now()).build();

        when(evidenceItemRepository.findById(900L)).thenReturn(Optional.of(item));
        when(factEvidenceRepository.findByEvidenceItemId(900L)).thenReturn(List.of(link));
        when(factRepository.findById(1L)).thenReturn(Optional.of(owningFact));
        when(documentVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        when(documentRepository.findById(55L)).thenReturn(Optional.of(document));

        EvidenceItemResponse response = evidenceGraphService.getEvidenceItemDetail(subject, 900L);

        assertThat(response.sourceData().documentFileName()).isEqualTo("passport.pdf");
        assertThat(response.sourceData().sourceSnippet()).isEqualTo("Nationality: Zimbabwe");
        assertThat(response.systemInterpretation().extractionMethod()).isEqualTo("OCR_V2");
        assertThat(response.systemInterpretation().extractionConfidence()).isEqualTo(0.88);
        assertThat(response.derivedConclusion().factId()).isEqualTo(1L);
        assertThat(response.derivedConclusion().isVerified()).isFalse();
    }

    // =========================================================================
    // PER-DOCUMENT GRAPH (Phase 3) - BACKWARD TRACE + DOCUMENT IMPACT
    // =========================================================================

    @Test
    void graphForDocumentThrowsResourceNotFoundWhenMissing() {

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evidenceGraphService.graphForDocument(actor, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void graphForDocumentDeniesAStrangerBeforeReadingVersions() {

        Document document = Document.builder().id(55L).userId(SUBJECT_ID).fileName("passport.pdf")
                .documentType("PASSPORT").filePath("x").fileSize(1L).uploadedAt(LocalDateTime.now()).build();

        AuthenticatedUser stranger = user(STRANGER_ID, Role.USER);

        when(documentRepository.findById(55L)).thenReturn(Optional.of(document));

        doThrow(new AccessDeniedException("denied")).when(factAuthorizationService)
                .assertCanView(eq(stranger), eq(SUBJECT_ID), any(), eq(null), org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(() -> evidenceGraphService.graphForDocument(stranger, 55L))
                .isInstanceOf(AccessDeniedException.class);

        verify(documentVersionRepository, never()).findByDocumentIdOrderByVersionNumberDesc(any());
    }

    @Test
    void graphForDocumentBuildsTheFullBackwardChainToFact() {

        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        Document document = Document.builder().id(55L).userId(SUBJECT_ID).fileName("payslip.pdf")
                .documentType("PAYSLIP").filePath("x").fileSize(1L).uploadedAt(LocalDateTime.now())
                .fraudDetected(false).riskLevel("LOW").build();

        DocumentVersion version = DocumentVersion.builder()
                .id(1L).documentId(55L).versionNumber(1).extractionMethod("OCR_V2")
                .createdAt(LocalDateTime.now()).build();

        EvidenceItem evidenceItem = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).sourceType(EvidenceSourceType.DOCUMENT)
                .status(EvidenceItemStatus.LINKED).extractedAt(LocalDateTime.now()).build();

        FactEvidence factEvidence = FactEvidence.builder()
                .id(501L).factId(1L).sourceType(EvidenceSourceType.DOCUMENT)
                .evidenceItemId(900L).capturedAt(LocalDateTime.now()).build();

        Fact fact = fact(1L, SUBJECT_ID);

        when(documentRepository.findById(55L)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(55L)).thenReturn(List.of(version));
        when(evidenceItemRepository.findByDocumentVersionId(1L)).thenReturn(List.of(evidenceItem));
        when(factEvidenceRepository.findByEvidenceItemId(900L)).thenReturn(List.of(factEvidence));
        when(factRepository.findById(1L)).thenReturn(Optional.of(fact));

        EvidenceGraphResponse response = evidenceGraphService.graphForDocument(subject, 55L);

        assertThat(response.nodes()).extracting("id")
                .contains("document:55", "document_version:1", "evidence:501", "fact:1");

        assertThat(response.edges())
                .anySatisfy(e -> assertThat(e.relationship()).isEqualTo(GraphRelationshipType.HAS_VERSION))
                .anySatisfy(e -> assertThat(e.relationship()).isEqualTo(GraphRelationshipType.PRODUCES))
                .anySatisfy(e -> assertThat(e.relationship()).isEqualTo(GraphRelationshipType.SUPPORTS_FACT));
    }

    @Test
    void graphForDocumentNeverSurfacesAFactBelongingToAnotherSubject() {

        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        Document document = Document.builder().id(55L).userId(SUBJECT_ID).fileName("payslip.pdf")
                .documentType("PAYSLIP").filePath("x").fileSize(1L).uploadedAt(LocalDateTime.now()).build();

        DocumentVersion version = DocumentVersion.builder()
                .id(1L).documentId(55L).versionNumber(1).createdAt(LocalDateTime.now()).build();

        EvidenceItem evidenceItem = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).sourceType(EvidenceSourceType.DOCUMENT)
                .status(EvidenceItemStatus.LINKED).extractedAt(LocalDateTime.now()).build();

        FactEvidence factEvidence = FactEvidence.builder()
                .id(501L).factId(2L).sourceType(EvidenceSourceType.DOCUMENT)
                .evidenceItemId(900L).capturedAt(LocalDateTime.now()).build();

        Fact crossSubjectFact = fact(2L, STRANGER_ID);

        when(documentRepository.findById(55L)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(55L)).thenReturn(List.of(version));
        when(evidenceItemRepository.findByDocumentVersionId(1L)).thenReturn(List.of(evidenceItem));
        when(factEvidenceRepository.findByEvidenceItemId(900L)).thenReturn(List.of(factEvidence));
        when(factRepository.findById(2L)).thenReturn(Optional.of(crossSubjectFact));

        EvidenceGraphResponse response = evidenceGraphService.graphForDocument(subject, 55L);

        assertThat(response.nodes()).extracting("id").doesNotContain("fact:2");
    }

    @Test
    void graphForDocumentWithNoVersionsProducesJustTheDocumentNode() {

        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        Document document = Document.builder().id(55L).userId(SUBJECT_ID).fileName("empty.pdf")
                .documentType("OTHER").filePath("x").fileSize(1L).uploadedAt(LocalDateTime.now()).build();

        when(documentRepository.findById(55L)).thenReturn(Optional.of(document));
        when(documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(55L)).thenReturn(List.of());

        EvidenceGraphResponse response = evidenceGraphService.graphForDocument(subject, 55L);

        assertThat(response.nodes()).extracting("id").containsExactly("document:55");
        assertThat(response.edges()).isEmpty();
    }

    // =========================================================================
    // PER-REQUIREMENT-EVALUATION GRAPH (Phase 3) - REQUIREMENT TRACEABILITY
    // =========================================================================

    private RequirementEvaluation evaluation(Long id, Long requirementId, Long subjectUserId) {
        return RequirementEvaluation.builder()
                .id(id).requirementId(requirementId).regulatoryVersionId(88L).subjectUserId(subjectUserId)
                .assessmentDate(LocalDateTime.now())
                .outcome(com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome.SATISFIED)
                .certaintyLevel(com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel.HIGH)
                .build();
    }

    @Test
    void graphForRequirementEvaluationThrowsResourceNotFoundWhenMissing() {

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        when(requirementEvaluationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evidenceGraphService.graphForRequirementEvaluation(actor, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void graphForRequirementEvaluationDeniesAStrangerBeforeReadingFacts() {

        RequirementEvaluation eval = evaluation(4001L, 12L, SUBJECT_ID);
        AuthenticatedUser stranger = user(STRANGER_ID, Role.USER);

        when(requirementEvaluationRepository.findById(4001L)).thenReturn(Optional.of(eval));

        doThrow(new AccessDeniedException("denied")).when(factAuthorizationService)
                .assertCanView(eq(stranger), eq(SUBJECT_ID), any(), eq(null), org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(() -> evidenceGraphService.graphForRequirementEvaluation(stranger, 4001L))
                .isInstanceOf(AccessDeniedException.class);

        verify(requirementEvaluationFactRepository, never()).findByEvaluationId(any());
    }

    @Test
    void graphForRequirementEvaluationIncludesRequirementAndRegulatoryVersion() {

        RequirementEvaluation eval = evaluation(4001L, 12L, SUBJECT_ID);
        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        Requirement requirement = Requirement.builder()
                .id(12L).requirementKey("TEST.REQ").title("Test Requirement")
                .requirementType(com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType.EXPERIENCE)
                .mandatory(true).regulatoryVersionId(88L).build();

        RegulatoryVersion regulatoryVersion = RegulatoryVersion.builder()
                .id(88L).regulationIdentity("TEST_SOURCE").jurisdiction("Testland")
                .sourceAuthority("Testland Authority")
                .verificationStatus(com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus.UNVERIFIED_INGESTION)
                .build();

        when(requirementEvaluationRepository.findById(4001L)).thenReturn(Optional.of(eval));
        when(requirementRepository.findById(12L)).thenReturn(Optional.of(requirement));
        when(regulatoryVersionRepository.findById(88L)).thenReturn(Optional.of(regulatoryVersion));
        when(requirementEvaluationFactRepository.findByEvaluationId(4001L)).thenReturn(List.of());
        when(requirementEvaluationConflictRepository.findByEvaluationId(4001L)).thenReturn(List.of());

        EvidenceGraphResponse response = evidenceGraphService.graphForRequirementEvaluation(subject, 4001L);

        assertThat(response.nodes()).extracting("id")
                .contains("requirement_evaluation:4001", "requirement:12", "regulatory_version:88");

        assertThat(response.edges())
                .anySatisfy(e -> assertThat(e.relationship()).isEqualTo(GraphRelationshipType.EVALUATED_UNDER))
                .anySatisfy(e -> assertThat(e.relationship()).isEqualTo(GraphRelationshipType.VERSIONED_UNDER));
    }

    @Test
    void graphForRequirementEvaluationIncludesContributingFactsViaDependsOnFact() {

        RequirementEvaluation eval = evaluation(4001L, 12L, SUBJECT_ID);
        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        RequirementEvaluationFact link = RequirementEvaluationFact.builder()
                .id(1L).evaluationId(4001L).factId(1L).build();

        Fact fact = fact(1L, SUBJECT_ID);

        when(requirementEvaluationRepository.findById(4001L)).thenReturn(Optional.of(eval));
        when(requirementRepository.findById(12L)).thenReturn(Optional.empty());
        when(requirementEvaluationFactRepository.findByEvaluationId(4001L)).thenReturn(List.of(link));
        when(factRepository.findById(1L)).thenReturn(Optional.of(fact));
        when(requirementEvaluationConflictRepository.findByEvaluationId(4001L)).thenReturn(List.of());

        EvidenceGraphResponse response = evidenceGraphService.graphForRequirementEvaluation(subject, 4001L);

        assertThat(response.nodes()).extracting("id").contains("fact:1");
        assertThat(response.edges()).anySatisfy(e ->
                assertThat(e.relationship()).isEqualTo(GraphRelationshipType.DEPENDS_ON_FACT)
        );
    }

    @Test
    void graphForRequirementEvaluationRendersAConflictAsItsOwnNodeNeverAsFraud() {

        RequirementEvaluation eval = evaluation(4001L, 12L, SUBJECT_ID);
        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        RequirementEvaluationConflict evalConflictLink = RequirementEvaluationConflict.builder()
                .id(1L).evaluationId(4001L).conflictId(70L).build();

        FactConflict conflict = FactConflict.builder()
                .id(70L).subjectUserId(SUBJECT_ID).factKey("K")
                .factAId(1L).factBId(2L).status(ConflictStatus.OPEN).build();

        Fact factA = fact(1L, SUBJECT_ID);
        Fact factB = fact(2L, SUBJECT_ID);

        when(requirementEvaluationRepository.findById(4001L)).thenReturn(Optional.of(eval));
        when(requirementRepository.findById(12L)).thenReturn(Optional.empty());
        when(requirementEvaluationFactRepository.findByEvaluationId(4001L)).thenReturn(List.of());
        when(requirementEvaluationConflictRepository.findByEvaluationId(4001L)).thenReturn(List.of(evalConflictLink));
        when(factConflictRepository.findById(70L)).thenReturn(Optional.of(conflict));
        when(factRepository.findById(1L)).thenReturn(Optional.of(factA));
        when(factRepository.findById(2L)).thenReturn(Optional.of(factB));

        EvidenceGraphResponse response = evidenceGraphService.graphForRequirementEvaluation(subject, 4001L);

        assertThat(response.nodes()).extracting("id").contains("conflict:70", "fact:1", "fact:2");
        assertThat(response.edges())
                .anySatisfy(e -> assertThat(e.relationship()).isEqualTo(GraphRelationshipType.BLOCKED_BY_CONFLICT))
                .filteredOn(e -> e.relationship() == GraphRelationshipType.CONFLICTS_WITH)
                .hasSize(2);
    }

    @Test
    void graphForRequirementEvaluationNeverSurfacesAConflictFactBelongingToAnotherSubject() {

        RequirementEvaluation eval = evaluation(4001L, 12L, SUBJECT_ID);
        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        RequirementEvaluationConflict evalConflictLink = RequirementEvaluationConflict.builder()
                .id(1L).evaluationId(4001L).conflictId(70L).build();

        FactConflict conflict = FactConflict.builder()
                .id(70L).subjectUserId(SUBJECT_ID).factKey("K")
                .factAId(1L).factBId(2L).status(ConflictStatus.OPEN).build();

        Fact factA = fact(1L, SUBJECT_ID);
        Fact crossSubjectFactB = fact(2L, STRANGER_ID);

        when(requirementEvaluationRepository.findById(4001L)).thenReturn(Optional.of(eval));
        when(requirementRepository.findById(12L)).thenReturn(Optional.empty());
        when(requirementEvaluationFactRepository.findByEvaluationId(4001L)).thenReturn(List.of());
        when(requirementEvaluationConflictRepository.findByEvaluationId(4001L)).thenReturn(List.of(evalConflictLink));
        when(factConflictRepository.findById(70L)).thenReturn(Optional.of(conflict));
        when(factRepository.findById(1L)).thenReturn(Optional.of(factA));
        when(factRepository.findById(2L)).thenReturn(Optional.of(crossSubjectFactB));

        EvidenceGraphResponse response = evidenceGraphService.graphForRequirementEvaluation(subject, 4001L);

        assertThat(response.nodes()).extracting("id").contains("conflict:70", "fact:1");
        assertThat(response.nodes()).extracting("id").doesNotContain("fact:2");
    }
}
