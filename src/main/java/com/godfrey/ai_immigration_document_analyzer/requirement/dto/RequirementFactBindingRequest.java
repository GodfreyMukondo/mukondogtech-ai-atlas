package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * One {@code RequirementFactBinding} to create alongside a Requirement -
 * the requirement's explicit contract with the Fact Foundation (which Fact
 * keys it reads, and the minimum evidence quality expected of each).
 * {@link #factKey} is validated against {@code FactTypeRegistry} before
 * persistence - a binding naming an unknown key is rejected outright,
 * never silently accepted (see {@code RequirementAdminService}).
 */
@Data
public class RequirementFactBindingRequest {

    @NotBlank(message = "Fact key is required")
    @Size(max = 150)
    private String factKey;

    @NotNull(message = "requiresVerification must be specified")
    private Boolean requiresVerification;

    private FactProvenanceType minimumProvenanceType;
}
