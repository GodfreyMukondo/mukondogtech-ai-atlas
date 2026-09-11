package com.godfrey.ai_immigration_document_analyzer.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * ============================================================================
 * ADMIN CREATE APPLICATION REQUEST
 * ============================================================================
 *
 * Submitted by an administrator to create a new immigration application
 * ("case") on behalf of an existing user, from the admin dashboard's
 * "Create Case" action.
 *
 * Identical to {@link ApplicationSubmitRequest} except that, unlike the
 * applicant-facing endpoint, the target user is not implied by the JWT and
 * must be supplied explicitly here.
 *
 * Supporting documents must already have been uploaded by that user (they
 * can be listed via GET /api/admin/applications/documents/available) - an
 * administrator does not bypass the evidence requirement every other
 * application is held to.
 * ============================================================================
 */
@Data
public class AdminCreateApplicationRequest {

    @NotNull(message = "A target user is required")
    @Positive(message = "User ID must be a positive value")
    private Long userId;

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
     * IDs of documents already uploaded by the target user (see
     * {@link #userId}).
     *
     * At least one supporting document is required.
     */
    @NotNull(message = "Document IDs are required")
    @NotEmpty(message = "At least one supporting document is required")
    private List<Long> documentIds;

    /**
     * Converts this into the same request shape the applicant-facing
     * endpoint uses, so both paths share one implementation in
     * ApplicationService.
     */
    public ApplicationSubmitRequest toApplicationSubmitRequest() {

        ApplicationSubmitRequest request = new ApplicationSubmitRequest();

        request.setFullName(fullName);
        request.setEmail(email);
        request.setPhone(phone);
        request.setDateOfBirth(dateOfBirth);
        request.setCountry(country);
        request.setVisaType(visaType);
        request.setNotes(notes);
        request.setDocumentIds(documentIds);

        return request;
    }
}
