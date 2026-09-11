package com.godfrey.ai_immigration_document_analyzer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * ============================================================================
 * REJECT APPLICATION REQUEST
 * ============================================================================
 *
 * Submitted by an administrator to reject a pending immigration application.
 *
 * The reason is required: it is emailed to the applicant and stored on the
 * application so both the applicant and other administrators can see why it
 * was rejected.
 * ============================================================================
 */
@Data
public class RejectApplicationRequest {

    @NotBlank(message = "A rejection reason is required")
    @Size(min = 5, max = 1000, message = "Rejection reason must be between 5 and 1000 characters")
    private String reason;
}
