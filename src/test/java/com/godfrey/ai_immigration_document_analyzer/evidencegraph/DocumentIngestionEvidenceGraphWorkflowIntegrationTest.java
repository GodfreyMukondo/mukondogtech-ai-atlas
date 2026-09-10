package com.godfrey.ai_immigration_document_analyzer.evidencegraph;

import com.godfrey.ai_immigration_document_analyzer.dto.response.DocumentUploadResponse;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceGraphResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.GraphRelationshipType;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.DocumentVersion;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItem;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItemStatus;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.DocumentVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.service.EvidenceGraphService;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactProposalRequest;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactService;
import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;
import com.godfrey.ai_immigration_document_analyzer.service.DocumentService;
import com.godfrey.ai_immigration_document_analyzer.service.OcrService;
import com.godfrey.ai_immigration_document_analyzer.service.storage.S3FileStorageService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ============================================================================
 * DOCUMENT INGESTION -&gt; EVIDENCE GRAPH WORKFLOW INTEGRATION TEST
 * ============================================================================
 *
 * A genuine end-to-end exercise of the Phase 4 gap closure identified during
 * the Evidence Graph audit: the production {@link DocumentService#uploadDocument}
 * write path now populating {@code DocumentVersion}/{@code EvidenceItem}, and
 * {@link FactService#proposeFact} now completing the optional
 * {@code FactEvidence.evidenceItemId} enrichment and the EvidenceItem's
 * CANDIDATE -&gt; LINKED transition as a side effect of Fact acceptance.
 *
 * Runs against the real, configured Oracle instance - not mocks - mirroring
 * {@code EvidenceGraphPhase3WorkflowIntegrationTest}'s pattern exactly. The
 * ONLY two mocked collaborators are the genuinely external I/O boundaries
 * this application has no control over in a test environment - S3 storage
 * and the OCR engine (Tesseract/PDFBox) - never any graph, fact, or
 * authorization logic. Everything from {@code DocumentService.uploadDocument}
 * onward (DocumentVersion/EvidenceItem creation, FactService, FactAuthorizationService,
 * EvidenceGraphService) is the real, unmodified production wiring.
 *
 * The "document" processed here is synthetic test content, not a real
 * person's immigration document, exactly like every other test in this
 * suite. {@code @Transactional} rolls back every write at the end of the
 * method - no test artifacts are left in the real database.
 * ============================================================================
 */
@SpringBootTest
@Transactional
class DocumentIngestionEvidenceGraphWorkflowIntegrationTest {

    @MockBean
    private S3FileStorageService s3FileStorageService;

    @MockBean
    private OcrService ocrService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private FactService factService;

    @Autowired
    private EvidenceGraphService evidenceGraphService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private EvidenceItemRepository evidenceItemRepository;

    @Autowired
    private FactEvidenceRepository factEvidenceRepository;

    private static final String FACT_KEY = "EMPLOYMENT.CURRENT_EMPLOYER";
    private static final String SYNTHETIC_EXTRACTED_TEXT = "Employed at Acme Corp since 2020.";

    private AuthenticatedUser actorFor(Long userId) {
        return new AuthenticatedUser(
                userId, "document-ingestion-test-" + userId + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())),
                true, true, true, true
        );
    }

    private Long createTestSubject() {
        User saved = userRepository.save(User.builder()
                .fullName("Document Ingestion Test Subject")
                .email("document-ingestion-subject-" + System.nanoTime() + "@example.com")
                .password("irrelevant-hash")
                .role(Role.USER)
                .build());
        return saved.getId();
    }

    private MultipartFile syntheticPdf() {
        return new MockMultipartFile(
                "file",
                "employment-letter.pdf",
                "application/pdf",
                "synthetic test document content - not a real immigration document".getBytes()
        );
    }

    @Test
    void uploadedDocumentFlowsThroughToAnAcceptedFactAndAFullyPopulatedEvidenceGraph() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        // ---- Stub only the external I/O boundary: S3 storage + OCR ----
        when(s3FileStorageService.saveFile(any(), anyLong())).thenReturn("test/evidence-graph-ingestion/" + System.nanoTime());
        when(s3FileStorageService.downloadFile(anyString())).thenReturn("stored bytes".getBytes());
        when(ocrService.extractText(any(), anyString(), anyString())).thenReturn(SYNTHETIC_EXTRACTED_TEXT);

        // =========================================================================
        // STEP 1: REAL DOCUMENT UPLOAD -> DOCUMENT VERSION + EVIDENCE ITEM
        // =========================================================================

        DocumentUploadResponse uploadResponse =
                documentService.uploadDocument(subjectUserId, "EMPLOYMENT_LETTER", syntheticPdf());

        Long documentId = uploadResponse.documentId();
        assertThat(documentId).isNotNull();

        List<DocumentVersion> versions =
                documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId);
        assertThat(versions).hasSize(1);

        DocumentVersion version = versions.get(0);
        assertThat(version.getVersionNumber()).isEqualTo(1);
        assertThat(version.getExtractionMethod()).isEqualTo("PDFBOX_NATIVE_TEXT");

        List<EvidenceItem> items = evidenceItemRepository.findByDocumentVersionId(version.getId());
        assertThat(items).hasSize(1);

        EvidenceItem evidenceItem = items.get(0);
        assertThat(evidenceItem.getStatus()).isEqualTo(EvidenceItemStatus.CANDIDATE);
        assertThat(evidenceItem.getSourceSnippet()).isEqualTo(SYNTHETIC_EXTRACTED_TEXT);

        // =========================================================================
        // STEP 2: REAL FACT PROPOSAL SOURCED FROM THIS DOCUMENT
        // =========================================================================

        FactProposalRequest request = new FactProposalRequest();
        request.setSubjectUserId(subjectUserId);
        request.setCategory(FactCategory.EMPLOYMENT);
        request.setFactKey(FACT_KEY);
        request.setValueType(FactValueType.STRING);
        request.setStringValue("Acme Corp");
        request.setProvenanceType(FactProvenanceType.DOCUMENT_EXTRACTION);
        request.setSourceDocumentId(documentId);
        request.setSourceSnippet(SYNTHETIC_EXTRACTED_TEXT);

        FactResponse factResponse = factService.proposeFact(actor, request);

        assertThat(factResponse.status()).isEqualTo(FactStatus.ACCEPTED);

        // The optional evidenceItemId enrichment must now be populated...
        var savedFactEvidence = factEvidenceRepository.findByFactId(factResponse.id());
        assertThat(savedFactEvidence).hasSize(1);
        assertThat(savedFactEvidence.get(0).getEvidenceItemId()).isEqualTo(evidenceItem.getId());

        // ...and the EvidenceItem must have completed its lifecycle to
        // LINKED, as a side effect of the Fact being accepted.
        EvidenceItem linkedItem = evidenceItemRepository.findById(evidenceItem.getId()).orElseThrow();
        assertThat(linkedItem.getStatus()).isEqualTo(EvidenceItemStatus.LINKED);

        // =========================================================================
        // STEP 3: THE EVIDENCE GRAPH API RETRIEVES THE NEWLY POPULATED GRAPH
        // =========================================================================

        EvidenceGraphResponse documentGraph = evidenceGraphService.graphForDocument(actor, documentId);

        assertThat(documentGraph.nodes()).extracting("id").contains(
                "document:" + documentId,
                "document_version:" + version.getId(),
                "evidence:" + savedFactEvidence.get(0).getId(),
                "fact:" + factResponse.id()
        );

        assertThat(documentGraph.edges())
                .extracting("relationship")
                .contains(
                        GraphRelationshipType.HAS_VERSION,
                        GraphRelationshipType.PRODUCES,
                        GraphRelationshipType.SUPPORTS_FACT
                );

        EvidenceGraphResponse factGraph = evidenceGraphService.graphForFact(actor, factResponse.id());

        assertThat(factGraph.nodes()).extracting("id").contains(
                "fact:" + factResponse.id(),
                "evidence:" + savedFactEvidence.get(0).getId(),
                "document_version:" + version.getId(),
                "document:" + documentId
        );

        // =========================================================================
        // STEP 4: AUTHORIZATION - A STRANGER MAY NOT VIEW THIS GRAPH
        // =========================================================================

        AuthenticatedUser stranger = actorFor(createTestSubject());

        assertThatThrownBy(() -> evidenceGraphService.graphForDocument(stranger, documentId))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> evidenceGraphService.graphForFact(stranger, factResponse.id()))
                .isInstanceOf(AccessDeniedException.class);
    }
}
