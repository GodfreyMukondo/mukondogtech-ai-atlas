package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/** Admin request to transition a Requirement's status, guarded by {@code RequirementLifecycleService}. */
@Data
public class RequirementStatusChangeRequest {

    @NotNull(message = "Target status is required")
    private RequirementStatus targetStatus;

    @Size(max = 1000)
    private String notes;
}
