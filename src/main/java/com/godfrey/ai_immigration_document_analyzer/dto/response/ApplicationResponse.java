package com.godfrey.ai_immigration_document_analyzer.dto.response;

import com.godfrey.ai_immigration_document_analyzer.entity.Application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================================
 * APPLICATION RESPONSE DTO
 * ============================================================================
 *
 * Applicant-facing representation of a submitted application, returned by:
 *
 *     POST /api/applications
 *     GET  /api/applications
 *     GET  /api/applications/{id}
 * ============================================================================
 */
public record ApplicationResponse(

        Long id,

        String fullName,

        String email,

        String phone,

        LocalDate dateOfBirth,

        String country,

        String visaType,

        String notes,

        String status,

        String riskLevel,

        Double aiConfidence,

        String rejectionReason,

        LocalDateTime submittedAt,

        LocalDateTime updatedAt,

        int documentCount,

        List<ApplicationDocumentSummary> documents

) {

    /**
     * Builds a response including the full attached-document list (used by
     * the submit and single-application-detail endpoints).
     */
    public static ApplicationResponse from(
            Application application,
            List<ApplicationDocumentSummary> documents
    ) {

        return from(
                application,
                documents == null ? 0 : documents.size(),
                documents
        );
    }

    /**
     * Builds a response with only a document count, without fetching every
     * attached document (used by the application list endpoint).
     */
    public static ApplicationResponse from(
            Application application,
            int documentCount
    ) {

        return from(
                application,
                documentCount,
                null
        );
    }

    private static ApplicationResponse from(
            Application application,
            int documentCount,
            List<ApplicationDocumentSummary> documents
    ) {

        if (application == null) {
            throw new IllegalArgumentException(
                    "Application cannot be null."
            );
        }

        return new ApplicationResponse(
                application.getId(),
                application.getFullName(),
                application.getEmail(),
                application.getPhone(),
                application.getDateOfBirth(),
                application.getCountry(),
                application.getVisaType(),
                application.getNotes(),
                application.getStatus(),
                application.getRiskLevel(),
                application.getAiConfidence(),
                application.getRejectionReason(),
                application.getSubmittedAt(),
                application.getUpdatedAt(),
                documentCount,
                documents
        );
    }
}
