package com.godfrey.ai_immigration_document_analyzer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.springframework.web.multipart.MultipartFile;

import java.io.Serial;
import java.io.Serializable;

/**
 * ============================================================================
 * DOCUMENT UPLOAD REQUEST
 * ============================================================================
 *
 * Request DTO used when uploading an immigration-related document.
 *
 * Supported files are validated independently by the document upload service.
 * The DTO is responsible for validating request metadata only.
 *
 * IMPORTANT SECURITY NOTE
 * ----------------------------------------------------------------------------
 * The authenticated user's identity should be obtained from Spring Security
 * rather than trusting a userId supplied by the browser.
 *
 * The userId field is retained here for backwards compatibility with existing
 * controller/service code. Production upload logic should compare it against
 * the authenticated principal or preferably ignore the client-supplied value.
 *
 * ============================================================================
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "file")
public class DocumentUploadRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * User associated with the document.
     *
     * Prefer the authenticated principal's ID over this client-supplied value.
     */
    private Long userId;

    /**
     * Logical document category.
     *
     * Examples:
     * - passport
     * - visa
     * - bank_statement
     * - birth_certificate
     * - employment_letter
     */
    @NotBlank(message = "Document type is required")
    @Size(
            max = 100,
            message = "Document type must not exceed 100 characters"
    )
    private String documentType;

    /**
     * Uploaded document payload.
     *
     * The actual file size and content type must also be validated by the
     * document service. Bean validation alone is not sufficient for file
     * security.
     */
    @NotNull(message = "Document file is required")
    private MultipartFile file;

    /**
     * Optional user-provided description.
     */
    @Size(
            max = 1000,
            message = "Description must not exceed 1000 characters"
    )
    private String description;

    /**
     * Optional country associated with the document.
     */
    @Size(
            max = 100,
            message = "Country must not exceed 100 characters"
    )
    private String country;

    /**
     * Optional visa type associated with the document.
     */
    @Size(
            max = 100,
            message = "Visa type must not exceed 100 characters"
    )
    private String visaType;
}

