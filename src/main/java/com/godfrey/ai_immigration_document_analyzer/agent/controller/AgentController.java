package com.godfrey.ai_immigration_document_analyzer.agent.controller;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.AgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainRequirementRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.service.ExplainRequirementAgentService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * AGENT CONTROLLER
 * ============================================================================
 *
 * Phase 5.1's only API surface: run the EXPLAIN_REQUIREMENT agent, and
 * re-fetch a past run. No endpoint here accepts a subjectUserId of any
 * kind - the backend resolves and authorizes the subject from the
 * PathwayAssessment/AgentRun record itself, exactly like every other
 * controller in this codebase (FactController, EvidenceGraphController).
 *
 * This prefix requires no {@code SecurityConfig} change - it falls through
 * to the existing fail-closed {@code .anyRequest().authenticated()} rule.
 *
 * Endpoints:
 *
 *   POST /api/agent/runs             run the EXPLAIN_REQUIREMENT agent
 *   GET  /api/agent/runs/{id}        re-fetch a past run (self, or any case
 *                                    worker with an active assignment for
 *                                    this subject)
 * ============================================================================
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Validated
public class AgentController {

    private final ExplainRequirementAgentService explainRequirementAgentService;

    @PostMapping(value = "/runs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AgentRunResponse> explainRequirement(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody ExplainRequirementRequest request
    ) {

        AgentRunResponse response = explainRequirementAgentService.explainRequirement(actor, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/runs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AgentRunResponse> getRun(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("id") @Positive Long id
    ) {

        return ResponseEntity.ok(explainRequirementAgentService.getRun(actor, id));
    }
}
