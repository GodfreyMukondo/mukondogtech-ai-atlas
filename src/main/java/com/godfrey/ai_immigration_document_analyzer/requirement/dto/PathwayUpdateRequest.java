package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin request to update a Pathway's content. Only permitted while the
 * pathway is {@code DRAFT} or {@code REVIEW} - {@code pathwayKey} is never
 * editable (it is the row's stable identity across versions); a
 * {@code PUBLISHED} pathway is never edited in place (see
 * {@code PathwayLifecycleService}/{@code PathwayAdminService}).
 */
@Data
public class PathwayUpdateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotBlank(message = "Jurisdiction is required")
    @Size(max = 100)
    private String jurisdiction;

    @NotBlank(message = "Category is required")
    @Size(max = 100)
    private String category;

    @NotEmpty(message = "At least one requirement must be selected")
    private List<Long> requirementIds;

    private String evidenceExpectations;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;
}
