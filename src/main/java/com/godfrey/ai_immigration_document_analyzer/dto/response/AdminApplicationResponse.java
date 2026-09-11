package com.godfrey.ai_immigration_document_analyzer.dto.response;

import com.godfrey.ai_immigration_document_analyzer.entity.Application;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ============================================================================
 * ADMIN APPLICATION RESPONSE DTO
 * ============================================================================
 *
 * Shape returned by GET /api/admin/applications, matching the frontend's
 * ApplicationReviewPage.tsx Application contract exactly (id as a string,
 * applicantName rather than fullName).
 *
 * aiConfidence defaults to 0 when no automated assessment has been
 * recorded yet; there is currently no AI eligibility-scoring pipeline for
 * applications (unlike document OCR/fraud analysis), so this reflects
 * "not yet assessed" rather than a fabricated score.
 * ============================================================================
 */
public record AdminApplicationResponse(

        String id,

        String applicantName,

        String email,

        String phone,

        LocalDate dateOfBirth,

        String country,

        String visaType,

        String notes,

        LocalDateTime submittedAt,

        String status,

        String riskLevel,

        double aiConfidence,

        int documentCount,

        String rejectionReason

) {

    public static AdminApplicationResponse from(
            Application application,
            int documentCount
    ) {

        if (application == null) {
            throw new IllegalArgumentException(
                    "Application cannot be null."
            );
        }

        return new AdminApplicationResponse(
                String.valueOf(application.getId()),
                application.getFullName(),
                application.getEmail(),
                application.getPhone(),
                application.getDateOfBirth(),
                application.getCountry(),
                application.getVisaType(),
                application.getNotes(),
                application.getSubmittedAt(),
                application.getStatus(),
                application.getRiskLevel(),
                application.getAiConfidence() == null
                        ? 0.0
                        : application.getAiConfidence(),
                documentCount,
                application.getRejectionReason()
        );
    }
}
