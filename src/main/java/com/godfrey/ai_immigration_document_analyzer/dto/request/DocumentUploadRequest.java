package com.godfrey.ai_immigration_document_analyzer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.Serial;
import java.io.Serializable;

/**

 * Request DTO for document uploads.
 * Carries document metadata and file payload.
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

     * User uploading the document.
     */
    @NotNull(message = "User ID is required")
    private Long userId;

    /**

     * Type of document being uploaded.
     * Example: passport, visa, bank_statement
     */
    @NotBlank(message = "Document type is required")
    private String documentType;

    /**

     * Actual uploaded file.
     */
    @NotNull(message = "Document file is required")
    private MultipartFile file;

    /**

     * Optional document description.
     */
    private String description;

    /**

     * Optional country associated with document.
     */
    private String country;

    /**

     * Optional visa type linked to this document.
     */
    private String visaType;
}
