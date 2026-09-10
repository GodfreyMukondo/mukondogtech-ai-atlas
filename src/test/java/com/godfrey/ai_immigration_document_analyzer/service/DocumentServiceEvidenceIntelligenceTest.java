package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.entity.Document;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.DocumentVersion;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItem;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItemStatus;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.DocumentVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.service.EvidenceItemLifecycleService;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.service.storage.S3FileStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Evidence Intelligence Graph write path added to
 * {@link DocumentService#uploadDocument} - the Phase 4 gap identified during
 * the Evidence Graph audit (no production write path ever populated
 * {@code DocumentVersion}/{@code EvidenceItem}).
 *
 * Focus areas: a successfully OCR'd document produces a DocumentVersion and
 * a CANDIDATE EvidenceItem; a document with no readable text produces a
 * VALIDATION_FAILED EvidenceItem and no source snippet; retrying against a
 * document id that already has a version never creates a duplicate; and a
 * failure in this optional enrichment never fails the upload itself
 * (mirrors this class's existing OCR/AI failure-isolation tests).
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceEvidenceIntelligenceTest {

    private static final Long OWNER_ID = 1L;

    @Mock
    private S3FileStorageService s3FileStorageService;

    @Mock
    private OcrService ocrService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private LlmService llmService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private EvidenceItemRepository evidenceItemRepository;

    @Mock
    private EvidenceItemLifecycleService evidenceItemLifecycleService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {

        documentService = new DocumentService(
                s3FileStorageService,
                ocrService,
                documentRepository,
                llmService,
                notificationService,
                documentVersionRepository,
                evidenceItemRepository,
                evidenceItemLifecycleService
        );
    }

    private MultipartFile validPdfFile() {

        return new MockMultipartFile(
                "file",
                "employment-letter.pdf",
                "application/pdf",
                "synthetic test document bytes".getBytes()
        );
    }

    private void stubStorageAndDocumentPersistence() {

        when(s3FileStorageService.saveFile(any(), anyLong())).thenReturn("object-key-123");
        when(s3FileStorageService.downloadFile("object-key-123")).thenReturn("file bytes".getBytes());

        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            document.setId(55L);
            return document;
        });
    }

    // =========================================================================
    // NEW DOCUMENT - SUCCESSFUL EXTRACTION
    // =========================================================================

    @Test
    void uploadDocumentCreatesADocumentVersionAndACandidateEvidenceItemWhenTextIsExtracted() {

        stubStorageAndDocumentPersistence();

        when(ocrService.extractText(any(), any(), any()))
                .thenReturn("Employed at Acme Corp since 2020.");

        when(documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(55L))
                .thenReturn(List.of());

        DocumentVersion savedVersion = DocumentVersion.builder().id(1L).documentId(55L).versionNumber(1).build();
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);

        EvidenceItem discovered = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).status(EvidenceItemStatus.DISCOVERED).build();
        when(evidenceItemRepository.save(any(EvidenceItem.class))).thenReturn(discovered);

        EvidenceItem extracted = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).status(EvidenceItemStatus.EXTRACTED).build();
        EvidenceItem candidate = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).status(EvidenceItemStatus.CANDIDATE)
                .sourceSnippet("Employed at Acme Corp since 2020.").build();

        when(evidenceItemLifecycleService.transition(discovered, EvidenceItemStatus.EXTRACTED))
                .thenReturn(extracted);
        when(evidenceItemLifecycleService.transition(eq(extracted), eq(EvidenceItemStatus.CANDIDATE)))
                .thenReturn(candidate);

        documentService.uploadDocument(OWNER_ID, "EMPLOYMENT_LETTER", validPdfFile());

        ArgumentCaptor<DocumentVersion> versionCaptor = ArgumentCaptor.forClass(DocumentVersion.class);
        verify(documentVersionRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getDocumentId()).isEqualTo(55L);
        assertThat(versionCaptor.getValue().getVersionNumber()).isEqualTo(1);
        assertThat(versionCaptor.getValue().getExtractionMethod()).isEqualTo("PDFBOX_NATIVE_TEXT");

        verify(evidenceItemLifecycleService).transition(discovered, EvidenceItemStatus.EXTRACTED);
        verify(evidenceItemLifecycleService).transition(extracted, EvidenceItemStatus.CANDIDATE);

        // The candidate transition must already carry the snippet - set on
        // the in-memory item before the transition (and therefore its save)
        // is invoked, not afterwards.
        ArgumentCaptor<EvidenceItem> transitionedCaptor = ArgumentCaptor.forClass(EvidenceItem.class);
        verify(evidenceItemLifecycleService).transition(transitionedCaptor.capture(), eq(EvidenceItemStatus.CANDIDATE));
        assertThat(transitionedCaptor.getValue().getSourceSnippet()).isEqualTo("Employed at Acme Corp since 2020.");
    }

    // =========================================================================
    // FAILED / EMPTY EXTRACTION
    // =========================================================================

    @Test
    void uploadDocumentRecordsAValidationFailedEvidenceItemWhenNoTextIsExtracted() {

        stubStorageAndDocumentPersistence();

        when(ocrService.extractText(any(), any(), any())).thenReturn("");

        when(documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(55L))
                .thenReturn(List.of());

        DocumentVersion savedVersion = DocumentVersion.builder().id(1L).documentId(55L).versionNumber(1).build();
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenReturn(savedVersion);

        EvidenceItem discovered = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).status(EvidenceItemStatus.DISCOVERED).build();
        when(evidenceItemRepository.save(any(EvidenceItem.class))).thenReturn(discovered);

        EvidenceItem extracted = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).status(EvidenceItemStatus.EXTRACTED).build();
        when(evidenceItemLifecycleService.transition(discovered, EvidenceItemStatus.EXTRACTED))
                .thenReturn(extracted);

        EvidenceItem validationFailed = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).status(EvidenceItemStatus.VALIDATION_FAILED).build();
        when(evidenceItemLifecycleService.transition(extracted, EvidenceItemStatus.VALIDATION_FAILED))
                .thenReturn(validationFailed);

        documentService.uploadDocument(OWNER_ID, "EMPLOYMENT_LETTER", validPdfFile());

        verify(evidenceItemLifecycleService).transition(extracted, EvidenceItemStatus.VALIDATION_FAILED);
        verify(evidenceItemLifecycleService, never()).transition(any(), eq(EvidenceItemStatus.CANDIDATE));

        // The item handed to the failing transition must never have had a
        // snippet set on it - no readable text means no excerpt to store.
        assertThat(extracted.getSourceSnippet()).isNull();
    }

    // =========================================================================
    // RETRY / IDEMPOTENCY
    // =========================================================================

    @Test
    void uploadDocumentNeverCreatesASecondDocumentVersionWhenOneAlreadyExistsForThisDocumentId() {

        stubStorageAndDocumentPersistence();

        when(ocrService.extractText(any(), any(), any())).thenReturn("Some extracted text.");

        DocumentVersion existingVersion = DocumentVersion.builder().id(1L).documentId(55L).versionNumber(1).build();
        when(documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(55L))
                .thenReturn(List.of(existingVersion));

        documentService.uploadDocument(OWNER_ID, "EMPLOYMENT_LETTER", validPdfFile());

        verify(documentVersionRepository, never()).save(any());
        verify(evidenceItemRepository, never()).save(any());
        verify(evidenceItemLifecycleService, never()).transition(any(), any());
    }

    // =========================================================================
    // FAILURE ISOLATION - MIRRORS THIS CLASS'S EXISTING OCR/AI DEGRADATION
    // =========================================================================

    @Test
    void uploadDocumentSucceedsEvenWhenEvidenceGraphPersistenceFails() {

        stubStorageAndDocumentPersistence();

        when(ocrService.extractText(any(), any(), any())).thenReturn("Some extracted text.");

        when(documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(55L))
                .thenThrow(new RuntimeException("simulated database failure"));

        var response = documentService.uploadDocument(OWNER_ID, "EMPLOYMENT_LETTER", validPdfFile());

        assertThat(response.documentId()).isEqualTo(55L);
        verify(documentRepository).save(any(Document.class));
        verify(notificationService).notify(eq(OWNER_ID), eq("DOCUMENT_PROCESSED"), any(), any(), any());
    }
}
