package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin request to create a new Pathway catalogue entry, starting in
 * {@code DRAFT}. {@link #requirementIds} names existing, already-created
 * Requirements to compose - never a new Requirement definition inline
 * (Requirements are authored separately via {@code RequirementAdminController}
 * and referenced by id, exactly as {@code Pathway.compositionLogic}'s
 * {@code RequirementRefNode} grammar already requires).
 *
 * All listed Requirements are combined with AND (every one must be
 * satisfied) - the simplest, most common composition and the only one this
 * admin surface builds automatically in this phase. A pathway needing OR/NOT
 * composition can still be authored by constructing
 * {@code Pathway.compositionLogic} directly against the existing
 * {@code LogicNode} grammar - not exposed through this form yet.
 */
@Data
public class PathwayCreateRequest {

    @NotBlank(message = "Pathway key is required")
    @Size(max = 150)
    private String pathwayKey;

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
