package com.godfrey.ai_immigration_document_analyzer.fact.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.VerificationMethod;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class FactVerificationRequest {

    @NotNull(message = "Verification method is required")
    private VerificationMethod method;

    @Size(max = 500)
    private String notes;
}
