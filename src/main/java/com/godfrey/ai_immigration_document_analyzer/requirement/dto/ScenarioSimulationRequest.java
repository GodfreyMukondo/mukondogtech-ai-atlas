package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

import java.util.List;

/**
 * Request to run a deterministic what-if scenario against one PUBLISHED
 * pathway (Phase 5.5) - {@code POST /api/pathways/{pathwayId}/scenario-
 * simulations}. Mirrors {@code ExplainPathwayDiscoveryRequest}/{@code
 * ExplainCaseOverviewRequest}'s shape: an explicit {@code subjectUserId},
 * never trusted as an authority on its own - the backend independently
 * re-verifies it via {@code FactAuthorizationService.assertCanView} (through
 * {@code TemporalFactResolver.resolve}) before any data is read, exactly
 * like every other subject-scoped endpoint in this codebase. A case worker
 * with an active assignment may simulate their assigned subject's case,
 * identical to the existing authorization pattern - never a new mechanism.
 *
 * {@code hypotheticalFacts} is a collection because Phase 5.5 explicitly
 * supports multiple simultaneous overrides in one simulation - the
 * resulting AFTER state is always the COMBINED effect of every override
 * together, never attributed to any single one in isolation.
 */
@Data
public class ScenarioSimulationRequest {

    @NotNull(message = "Subject user ID is required")
    @Positive(message = "Subject user ID must be a positive value")
    private Long subjectUserId;

    @NotEmpty(message = "At least one hypothetical fact override is required")
    @Valid
    private List<HypotheticalFactInput> hypotheticalFacts;
}
