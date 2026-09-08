package com.godfrey.ai_immigration_document_analyzer.fact.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class ConflictResolutionRequest {

    @NotNull(message = "Winning fact ID is required")
    @Positive(message = "Winning fact ID must be a positive value")
    private Long winningFactId;

    @Size(max = 1000)
    private String notes;
}
