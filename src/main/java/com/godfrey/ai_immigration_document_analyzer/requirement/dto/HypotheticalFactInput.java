package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

/**
 * One hypothetical fact override supplied by the caller for a Scenario
 * Simulation (Phase 5.5) - a bare {@code factKey}/{@code value} pair, never
 * arbitrary executable logic or an arbitrary database field. {@code
 * factKey} MUST correspond to a known concept in {@code FactTypeRegistry} -
 * an unknown key is rejected before any simulation logic runs (see {@code
 * ScenarioSimulationService}). {@code value} is a single raw string, parsed
 * into the correct typed field ({@code stringValue}/{@code numberValue}/
 * {@code booleanValue}/{@code dateValue}) according to that registry
 * entry's own declared {@code FactValueType} - the caller never chooses
 * which typed field to populate directly.
 */
@Data
public class HypotheticalFactInput {

    @NotBlank(message = "Fact key is required")
    private String factKey;

    @NotBlank(message = "Hypothetical value is required")
    private String value;
}
