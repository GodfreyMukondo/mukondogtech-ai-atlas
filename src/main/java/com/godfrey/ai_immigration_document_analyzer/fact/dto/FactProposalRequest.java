package com.godfrey.ai_immigration_document_analyzer.fact.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Request to propose a new Fact. userId is never trusted for the ACTING
 * caller (resolved from the JWT as everywhere else in this codebase) - only
 * {@code subjectUserId} (who the fact is about) is supplied here, and it is
 * validated against the caller's authorization before anything is written.
 */
@Data
public class FactProposalRequest {

    @NotNull(message = "Subject user ID is required")
    @Positive(message = "Subject user ID must be a positive value")
    private Long subjectUserId;

    @NotNull(message = "Category is required")
    private FactCategory category;

    @NotBlank(message = "Fact key is required")
    @Size(max = 150)
    private String factKey;

    @NotNull(message = "Value type is required")
    private FactValueType valueType;

    @Size(max = 2000)
    private String stringValue;

    private LocalDateTime dateValue;

    private Double numberValue;

    private Boolean booleanValue;

    @NotNull(message = "Provenance type is required")
    private FactProvenanceType provenanceType;

    private Long sourceDocumentId;

    @Size(max = 255)
    private String sourceLocator;

    @Size(max = 500)
    private String sourceSnippet;

    @Size(max = 500)
    private String sourceDescription;

    private LocalDateTime effectiveFrom;

    private LocalDateTime observedAt;
}
