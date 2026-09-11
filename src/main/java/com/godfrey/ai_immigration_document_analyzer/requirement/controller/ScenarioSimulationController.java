package com.godfrey.ai_immigration_document_analyzer.requirement.controller;

import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ScenarioSimulationRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ScenarioSimulationResult;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.ScenarioSimulationService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * SCENARIO SIMULATION CONTROLLER
 * ============================================================================
 *
 * Phase 5.5 - What-If / Scenario Simulation, scoped to ONE published pathway
 * per request. Deliberately a separate controller class rather than an
 * addition to {@code PathwayAssessmentController} - a simulation is neither
 * a read of catalogue data nor a real, persisted assessment, and keeping it
 * separate means neither existing controller's behavior is touched.
 *
 * Every operation here is subject-scoped personal information and delegates
 * authorization entirely to {@code ScenarioSimulationService} (which reuses
 * the existing Fact authorization boundary via {@code TemporalFactResolver})
 * before any data is touched - this controller makes no authorization
 * decision itself, exactly like every other controller in this domain.
 *
 * This prefix requires no {@code SecurityConfig} change - {@code /api/
 * pathways/**} already falls under the existing fail-closed {@code
 * .anyRequest().authenticated()} rule (identical to the sibling {@code
 * PathwayAssessmentController}).
 *
 * NON-PERSISTENT BY DESIGN (Phase 5.5 scope decision): a simulation result
 * is never stored anywhere - there is deliberately no {@code GET .../
 * scenario-simulations/{id}} endpoint. Each call is synchronous:
 * request in, deterministic result out, nothing left behind.
 *
 * Endpoints:
 *
 *   POST /api/pathways/{pathwayId}/scenario-simulations   run a deterministic
 *                                                          what-if simulation
 *                                                          against one PUBLISHED
 *                                                          pathway (no LLM, no
 *                                                          persistence)
 * ============================================================================
 */
@RestController
@RequestMapping("/api/pathways")
@RequiredArgsConstructor
@Validated
public class ScenarioSimulationController {

    private final ScenarioSimulationService scenarioSimulationService;

    @PostMapping(value = "/{pathwayId}/scenario-simulations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScenarioSimulationResult> simulate(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("pathwayId") @Positive Long pathwayId,
            @Valid @RequestBody ScenarioSimulationRequest request
    ) {

        return ResponseEntity.ok(scenarioSimulationService.simulate(actor, pathwayId, request));
    }
}
