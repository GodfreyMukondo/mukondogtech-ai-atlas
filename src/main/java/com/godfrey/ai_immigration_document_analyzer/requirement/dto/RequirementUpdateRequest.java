package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.util.List;

/**
 * Admin request to update a Requirement's content. Only permitted while
 * {@code DRAFT} - {@code requirementKey} is never editable; a
 * {@code PUBLISHED} requirement is never edited in place (a rule change is
 * a new row against a new {@code RegulatoryVersion}, exactly as
 * {@code Requirement}'s own Javadoc documents).
 */
@Data
public class RequirementUpdateRequest {

    @NotNull(message = "Requirement type is required")
    private RequirementType requirementType;

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotBlank(message = "Jurisdiction is required")
    @Size(max = 100)
    private String jurisdiction;

    @Size(max = 100)
    private String immigrationContext;

    @NotNull(message = "Regulatory version ID is required")
    @Positive(message = "Regulatory version ID must be a positive value")
    private Long regulatoryVersionId;

    @NotNull(message = "mandatory must be specified")
    private Boolean mandatory;

    private String applicabilityLogicJson;

    @NotBlank(message = "Satisfaction logic is required")
    private String satisfactionLogicJson;

    @NotEmpty(message = "At least one fact binding is required")
    @Valid
    private List<RequirementFactBindingRequest> factBindings;
}
