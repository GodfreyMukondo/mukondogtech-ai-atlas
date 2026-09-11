package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * Admin request to transition a Pathway's status, guarded by
 * {@code PathwayLifecycleService}. {@code notes} is optional context for
 * the transition (e.g. why a pathway was archived) - descriptive only, not
 * persisted as a separate audit row in this phase.
 */
@Data
public class PathwayStatusChangeRequest {

    @NotNull(message = "Target status is required")
    private PathwayStatus targetStatus;

    @Size(max = 1000)
    private String notes;
}
