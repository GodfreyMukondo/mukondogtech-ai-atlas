package com.godfrey.ai_immigration_document_analyzer.controller;

import com.godfrey.ai_immigration_document_analyzer.service.DocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * ADMIN DOCUMENT CONTROLLER
 * ============================================================================
 *
 * Lets an administrator open the raw file content of a document attached to
 * an applicant's submission (e.g. from the Application Review table).
 *
 * Security model
 * ----------------------------------------------------------------------------
 *
 * Every endpoint under /api/admin/** requires the ADMIN role, enforced by
 * SecurityConfig. Unlike {@code DocumentController}, this deliberately does
 * NOT scope access to a document owner: an administrator reviewing an
 * applicant's evidence is expected to be able to open any document.
 *
 * Endpoints
 * ----------------------------------------------------------------------------
 *
 * GET /api/admin/documents/{documentId}/content
 * ============================================================================
 */
@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminDocumentController {

    private final DocumentService documentService;

    // =========================================================================
    // GET DOCUMENT CONTENT
    // =========================================================================

    /**
     * Streams a document's stored file bytes back to the browser so it can
     * be opened/previewed (PDF, PNG, JPEG) directly.
     */
    @GetMapping(
            value = "/{documentId}/content"
    )
    public ResponseEntity<byte[]> getDocumentContent(

            @PathVariable("documentId")
            Long documentId
    ) {

        validateDocumentId(documentId);

        log.info(
                "Admin document content requested | documentId={}",
                documentId
        );

        final DocumentService.DocumentContent content =
                documentService.getDocumentContentForAdmin(documentId);

        return ResponseEntity
                .ok()
                .contentType(
                        resolveMediaType(content.mimeType())
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\""
                                + sanitizeFileName(content.fileName())
                                + "\""
                )
                .body(content.bytes());
    }

    // =========================================================================
    // MEDIA TYPE RESOLUTION
    // =========================================================================

    private MediaType resolveMediaType(
            String mimeType
    ) {

        if (mimeType == null || mimeType.isBlank()) {

            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {

            return MediaType.parseMediaType(mimeType);

        } catch (RuntimeException ex) {

            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    // =========================================================================
    // DOCUMENT ID VALIDATION
    // =========================================================================

    private void validateDocumentId(
            Long documentId
    ) {

        if (documentId == null || documentId <= 0) {

            throw new IllegalArgumentException(
                    "Document ID must be a positive value."
            );
        }
    }

    // =========================================================================
    // LOGGING/HEADER SAFETY
    // =========================================================================

    /**
     * Prevents header injection and quote-breaking through a stored file
     * name when it is echoed back in Content-Disposition.
     */
    private String sanitizeFileName(
            String fileName
    ) {

        if (fileName == null || fileName.isBlank()) {

            return "document";
        }

        return fileName
                .replace("\r", "")
                .replace("\n", "")
                .replace("\"", "'");
    }
}
