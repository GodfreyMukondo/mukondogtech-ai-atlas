package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.dto.response.DocumentResponse;
import com.godfrey.ai_immigration_document_analyzer.entity.Document;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.DocumentVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.service.EvidenceItemLifecycleService;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.service.storage.S3FileStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentService}, focused on the two properties
 * that matter most for a multi-tenant document store:
 *
 * 1. A user can never read or delete another user's document by guessing
 *    or iterating document IDs (IDOR protection).
 * 2. Disallowed file types are rejected before anything is written to
 *    storage.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceOwnershipTest {

    private static final Long OWNER_ID = 1L;
    private static final Long ATTACKER_ID = 999L;
    private static final Long DOCUMENT_ID = 5L;

    @Mock
    private S3FileStorageService s3FileStorageService;

    @Mock
    private OcrService ocrService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private LlmService llmService;

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
                documentVersionRepository,
                evidenceItemRepository,
                evidenceItemLifecycleService
        );
    }

    private Document ownedDocument() {

        LocalDateTime now = LocalDateTime.now();

        return Document.builder()
                .id(DOCUMENT_ID)
                .userId(OWNER_ID)
                .documentType("PASSPORT")
                .fileName("passport.pdf")
                .filePath("s3-object-key")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .extractedText("sample text")
                .summary("sample summary")
                .fraudDetected(false)
                .riskLevel("LOW")
                .uploadStatus("COMPLETED")
                .uploadedAt(now)
                .updatedAt(now)
                .build();
    }

    // =========================================================================
    // OWNERSHIP - READ
    // =========================================================================

    @Test
    void getDocumentById_returnsDocument_whenOwnedByCaller() {

        when(documentRepository.findByIdAndUserId(DOCUMENT_ID, OWNER_ID))
                .thenReturn(Optional.of(ownedDocument()));

        DocumentResponse response =
                documentService.getDocumentById(DOCUMENT_ID, OWNER_ID);

        assertThat(response.id()).isEqualTo(DOCUMENT_ID);
        assertThat(response.fileName()).isEqualTo("passport.pdf");
    }

    @Test
    void getDocumentById_throwsNotFound_whenDocumentBelongsToAnotherUser() {

        // The attacker guesses a valid document ID that belongs to someone
        // else. The repository call is scoped to (documentId, callerId), so
        // it correctly returns empty rather than leaking the document.
        when(documentRepository.findByIdAndUserId(DOCUMENT_ID, ATTACKER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                documentService.getDocumentById(DOCUMENT_ID, ATTACKER_ID)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // OWNERSHIP - DELETE
    // =========================================================================

    @Test
    void deleteDocument_deletesStorageThenRecord_whenOwnedByCaller() {

        Document document = ownedDocument();

        when(documentRepository.findByIdAndUserId(DOCUMENT_ID, OWNER_ID))
                .thenReturn(Optional.of(document));

        documentService.deleteDocument(DOCUMENT_ID, OWNER_ID);

        verify(s3FileStorageService).deleteFile("s3-object-key");
        verify(documentRepository).delete(document);
    }

    @Test
    void deleteDocument_throwsNotFound_andNeverTouchesStorage_whenNotOwnedByCaller() {

        when(documentRepository.findByIdAndUserId(DOCUMENT_ID, ATTACKER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                documentService.deleteDocument(DOCUMENT_ID, ATTACKER_ID)
        ).isInstanceOf(ResourceNotFoundException.class);

        // Nothing should be deleted anywhere as a side effect of a failed
        // ownership check.
        verify(s3FileStorageService, never()).deleteFile(any());
        verify(documentRepository, never()).delete(any());
    }

    // =========================================================================
    // INPUT VALIDATION
    // =========================================================================

    @Test
    void getDocumentsByUserId_rejectsInvalidUserId_withoutTouchingRepository() {

        assertThatThrownBy(() -> documentService.getDocumentsByUserId(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> documentService.getDocumentsByUserId(0L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(documentRepository, never()).findByUserIdOrderByUploadedAtDesc(anyLong());
    }

    // =========================================================================
    // UPLOAD - FILE TYPE VALIDATION
    // =========================================================================

    @Test
    void uploadDocument_rejectsDisallowedFileType_beforeTouchingStorage() {

        MultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/octet-stream",
                "not a real document".getBytes()
        );

        assertThatThrownBy(() ->
                documentService.uploadDocument(OWNER_ID, "PASSPORT", file)
        ).isInstanceOf(IllegalArgumentException.class);

        verify(s3FileStorageService, never()).saveFile(any(), anyLong());
    }

    @Test
    void uploadDocument_rejectsExtensionMimeMismatch_beforeTouchingStorage() {

        // A .pdf filename with an image content-type: exactly the kind of
        // mismatch a spoofed upload would rely on.
        MultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "image/png",
                "not really a pdf".getBytes()
        );

        assertThatThrownBy(() ->
                documentService.uploadDocument(OWNER_ID, "PASSPORT", file)
        ).isInstanceOf(IllegalArgumentException.class);

        verify(s3FileStorageService, never()).saveFile(any(), anyLong());
    }

    @Test
    void uploadDocument_rejectsEmptyFile() {

        MultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThatThrownBy(() ->
                documentService.uploadDocument(OWNER_ID, "PASSPORT", file)
        ).isInstanceOf(IllegalArgumentException.class);

        verify(s3FileStorageService, never()).saveFile(any(), anyLong());
    }
}
