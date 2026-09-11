package com.godfrey.ai_immigration_document_analyzer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * ============================================================================
 * APPLICATION SUBMIT REQUEST
 * ============================================================================
 *
 * Submitted by an authenticated applicant to create a new immigration
 * application.
 *
 * Supporting documents must already have been uploaded via
 * POST /api/documents/upload (which returns each document's ID); this
 * request references those documents by ID rather than accepting file
 * uploads directly, so document processing (OCR, fraud analysis, S3
 * storage) is handled once by the existing document pipeline.
 *
 * IMPORTANT:
 *
 * userId is intentionally NOT part of this request. The backend resolves
 * the authenticated user from the JWT.
 * ============================================================================
 */
@Data
public class ApplicationSubmitRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Country is required")
    @Size(max = 150, message = "Country must not exceed 150 characters")
    private String country;

    @NotBlank(message = "Visa type is required")
    @Size(max = 150, message = "Visa type must not exceed 150 characters")
    private String visaType;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;

    /**
     * IDs of documents previously uploaded by this same authenticated user
     * via POST /api/documents/upload.
     *
     * At least one supporting document is required.
     */
    @NotNull(message = "Document IDs are required")
    @NotEmpty(message = "At least one supporting document is required")
    private List<Long> documentIds;
}
