package com.godfrey.ai_immigration_document_analyzer.controller;

import com.godfrey.ai_immigration_document_analyzer.dto.response.DocumentResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.DocumentUploadResponse;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;
import com.godfrey.ai_immigration_document_analyzer.service.DocumentService;

import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ============================================================================
 * DOCUMENT CONTROLLER
 * ============================================================================
 *
 * REST API for authenticated immigration document operations.
 *
 * Security model
 * ----------------------------------------------------------------------------
 *
 * - Authentication is handled by Spring Security.
 * - The authenticated database user is obtained exclusively from
 *   {@link AuthenticatedUser}.
 * - The frontend must NEVER provide userId.
 * - Every document operation is scoped to the authenticated user.
 * - Document ownership must be enforced by the service/repository layer.
 *
 * Endpoints
 * ----------------------------------------------------------------------------
 *
 * POST   /api/documents/upload
 * GET    /api/documents
 * GET    /api/documents/{documentId}
 * DELETE /api/documents/{documentId}
 *
 * Upload contract
 * ----------------------------------------------------------------------------
 *
 * POST /api/documents/upload
 *
 * Content-Type:
 *
 *     multipart/form-data
 *
 * Parts:
 *
 *     documentType = PASSPORT
 *     file         = <binary file>
 *
 * IMPORTANT
 * ----------------------------------------------------------------------------
 *
 * The upload endpoint intentionally does NOT consume application/json.
 *
 * A document upload contains binary file data and therefore must be submitted
 * using multipart/form-data.
 *
 * ============================================================================
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DocumentController {

    private final DocumentService documentService;

    // =========================================================================
    // UPLOAD DOCUMENT
    // =========================================================================

    /**
     * Uploads a document for the currently authenticated user.
     *
     * Authentication:
     *
     *     AuthenticatedUser
     *
     * Request:
     *
     *     multipart/form-data
     *
     * Required parts:
     *
     *     documentType
     *     file
     *
     * The user ID is deliberately NOT accepted from the request.
     *
     * @param authenticatedUser authenticated application user
     * @param documentType      immigration document type
     * @param file              uploaded document
     *
     * @return created document upload response
     */
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DocumentUploadResponse> uploadDocument(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @RequestParam("documentType")
            @NotBlank(message = "Document type is required.")
            String documentType,

            @RequestParam("file")
            MultipartFile file
    ) {

        final Long userId =
                requireAuthenticatedUserId(authenticatedUser);

        final String normalizedDocumentType =
                normalizeDocumentType(documentType);

        validateUploadedFile(file);

        /*
         * Do not log the actual file contents.
         *
         * The filename and size are useful for operational diagnostics,
         * while the binary contents must never be written to application logs.
         */
        log.info(
                "Document upload started | userId={} | documentType={} | fileName={} | size={} | contentType={}",
                userId,
                normalizedDocumentType,
                sanitizeFileName(file.getOriginalFilename()),
                file.getSize(),
                file.getContentType()
        );

        final DocumentUploadResponse response =
                documentService.uploadDocument(
                        userId,
                        normalizedDocumentType,
                        file
                );

        log.info(
                "Document upload completed | userId={} | documentType={} | fileName={}",
                userId,
                normalizedDocumentType,
                sanitizeFileName(file.getOriginalFilename())
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================================
    // GET CURRENT USER DOCUMENTS
    // =========================================================================

    /**
     * Returns all documents belonging to the authenticated user.
     *
     * No userId is accepted from the client.
     *
     * @param authenticatedUser authenticated application user
     *
     * @return authenticated user's documents
     */
    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<DocumentResponse>> getDocuments(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
    ) {

        final Long userId =
                requireAuthenticatedUserId(authenticatedUser);

        log.debug(
                "Fetching documents | userId={}",
                userId
        );

        final List<DocumentResponse> documents =
                documentService.getDocumentsByUserId(userId);

        return ResponseEntity.ok(documents);
    }

    // =========================================================================
    // GET SINGLE DOCUMENT
    // =========================================================================

    /**
     * Returns a single document belonging to the authenticated user.
     *
     * IMPORTANT:
     *
     * The service layer must enforce ownership using:
     *
     *     documentId
     *     authenticated userId
     *
     * This prevents an authenticated user from accessing another user's
     * document simply by changing the document ID.
     *
     * @param authenticatedUser authenticated application user
     * @param documentId        document identifier
     *
     * @return requested document
     */
    @GetMapping(
            value = "/{documentId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DocumentResponse> getDocument(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable("documentId")
            Long documentId
    ) {

        final Long userId =
                requireAuthenticatedUserId(authenticatedUser);

        validateDocumentId(documentId);

        log.debug(
                "Fetching document | documentId={} | userId={}",
                documentId,
                userId
        );

        final DocumentResponse response =
                documentService.getDocumentById(
                        documentId,
                        userId
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // DELETE DOCUMENT
    // =========================================================================

    /**
     * Deletes a document belonging to the authenticated user.
     *
     * Ownership enforcement must occur inside the service/repository layer.
     *
     * @param authenticatedUser authenticated application user
     * @param documentId        document identifier
     *
     * @return HTTP 204 when deletion succeeds
     */
    @DeleteMapping(
            value = "/{documentId}"
    )
    public ResponseEntity<Void> deleteDocument(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable("documentId")
            Long documentId
    ) {

        final Long userId =
                requireAuthenticatedUserId(authenticatedUser);

        validateDocumentId(documentId);

        log.info(
                "Deleting document | documentId={} | userId={}",
                documentId,
                userId
        );

        documentService.deleteDocument(
                documentId,
                userId
        );

        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // AUTHENTICATED USER VALIDATION
    // =========================================================================

    /**
     * Resolves the authenticated application's database user ID.
     *
     * The controller never accepts the user ID from the frontend.
     *
     * @param authenticatedUser Spring Security principal
     *
     * @return authenticated database user ID
     */
    private Long requireAuthenticatedUserId(
            AuthenticatedUser authenticatedUser
    ) {

        if (authenticatedUser == null) {

            log.warn(
                    "Document endpoint accessed without authenticated principal."
            );

            throw new IllegalStateException(
                    "Authenticated user could not be determined."
            );
        }

        final Long userId =
                authenticatedUser.getUserId();

        if (userId == null || userId <= 0) {

            log.error(
                    "Authenticated principal contains invalid database user ID."
            );

            throw new IllegalStateException(
                    "Authenticated user could not be determined."
            );
        }

        return userId;
    }

    // =========================================================================
    // DOCUMENT ID VALIDATION
    // =========================================================================

    /**
     * Validates document identifiers before passing them to the service layer.
     */
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
    // DOCUMENT TYPE NORMALIZATION
    // =========================================================================

    /**
     * Normalizes the document type before passing it to the service layer.
     *
     * Example:
     *
     *     " passport " -> "PASSPORT"
     */
    private String normalizeDocumentType(
            String documentType
    ) {

        if (documentType == null) {

            throw new IllegalArgumentException(
                    "Document type is required."
            );
        }

        final String normalized =
                documentType.trim().toUpperCase();

        if (normalized.isBlank()) {

            throw new IllegalArgumentException(
                    "Document type is required."
            );
        }

        return normalized;
    }

    // =========================================================================
    // FILE VALIDATION
    // =========================================================================

    /**
     * Performs basic request-level validation.
     *
     * More advanced validation such as:
     *
     * - permitted extensions
     * - MIME type verification
     * - maximum file size
     * - PDF/image validation
     * - malware scanning
     * - content inspection
     *
     * should remain in the service/security layer.
     */
    private void validateUploadedFile(
            MultipartFile file
    ) {

        if (file == null) {

            throw new IllegalArgumentException(
                    "A document file is required."
            );
        }

        if (file.isEmpty()) {

            throw new IllegalArgumentException(
                    "The uploaded document is empty."
            );
        }

        if (file.getSize() <= 0) {

            throw new IllegalArgumentException(
                    "The uploaded document contains no data."
            );
        }
    }

    // =========================================================================
    // LOGGING SAFETY
    // =========================================================================

    /**
     * Prevents line breaks and excessive filename content from entering
     * application logs.
     *
     * This is a small defensive measure against log injection.
     */
    private String sanitizeFileName(
            String originalFileName
    ) {

        if (originalFileName == null ||
                originalFileName.isBlank()) {

            return "unknown";
        }

        return originalFileName
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "")
                .trim();
    }
}
