package com.godfrey.ai_immigration_document_analyzer.agent.controller;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.AgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.CaseOverviewAgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainCaseOverviewRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainPathwayAssessmentRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainPathwayDiscoveryRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainRequirementRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.PathwayAssessmentAgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.PathwayDiscoveryAgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.service.ExplainCaseOverviewAgentService;
import com.godfrey.ai_immigration_document_analyzer.agent.service.ExplainPathwayAssessmentAgentService;
import com.godfrey.ai_immigration_document_analyzer.agent.service.ExplainPathwayDiscoveryAgentService;
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
 *   POST /api/agent/runs                  run the EXPLAIN_REQUIREMENT agent
 *   GET  /api/agent/runs/{id}             re-fetch a past run (self, or any case
 *                                         worker with an active assignment for
 *                                         this subject)
 *   POST /api/agent/pathway-runs          run the EXPLAIN_PATHWAY_ASSESSMENT agent (Phase 5.2)
 *   GET  /api/agent/pathway-runs/{id}     re-fetch a past pathway-assessment run (same
 *                                         authorization boundary as above)
 *   POST /api/agent/discovery-runs        run the EXPLAIN_PATHWAY_DISCOVERY agent (Phase 5.3)
 *   GET  /api/agent/discovery-runs/{id}   re-fetch a past discovery run (same
 *                                         authorization boundary as above)
 *   POST /api/agent/case-overview-runs        run the EXPLAIN_CASE_OVERVIEW agent (Phase 5.4)
 *   GET  /api/agent/case-overview-runs/{id}   re-fetch a past case-overview run (same
 *                                             authorization boundary as above)
 *
 * Each goal's endpoint pair is deliberately separate rather than a change to
 * an earlier pair - every {@code *AgentRunResponse.result} is hard-typed to
 * its own Phase's result shape and is already relied upon by that Phase's
 * tests and frontend, so none of them are touched by a later Phase.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Validated
public class AgentController {

    private final ExplainRequirementAgentService explainRequirementAgentService;
    private final ExplainPathwayAssessmentAgentService explainPathwayAssessmentAgentService;
    private final ExplainPathwayDiscoveryAgentService explainPathwayDiscoveryAgentService;
    private final ExplainCaseOverviewAgentService explainCaseOverviewAgentService;

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

    @PostMapping(value = "/pathway-runs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathwayAssessmentAgentRunResponse> explainPathwayAssessment(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody ExplainPathwayAssessmentRequest request
    ) {

        PathwayAssessmentAgentRunResponse response = explainPathwayAssessmentAgentService.explainPathwayAssessment(actor, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/pathway-runs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathwayAssessmentAgentRunResponse> getPathwayAssessmentRun(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("id") @Positive Long id
    ) {

        return ResponseEntity.ok(explainPathwayAssessmentAgentService.getRun(actor, id));
    }

    @PostMapping(value = "/discovery-runs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathwayDiscoveryAgentRunResponse> explainPathwayDiscovery(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody ExplainPathwayDiscoveryRequest request
    ) {

        PathwayDiscoveryAgentRunResponse response = explainPathwayDiscoveryAgentService.explainPathwayDiscovery(actor, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/discovery-runs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathwayDiscoveryAgentRunResponse> getPathwayDiscoveryRun(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("id") @Positive Long id
    ) {

        return ResponseEntity.ok(explainPathwayDiscoveryAgentService.getRun(actor, id));
    }

    @PostMapping(value = "/case-overview-runs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CaseOverviewAgentRunResponse> explainCaseOverview(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody ExplainCaseOverviewRequest request
    ) {

        CaseOverviewAgentRunResponse response = explainCaseOverviewAgentService.explainCaseOverview(actor, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/case-overview-runs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CaseOverviewAgentRunResponse> getCaseOverviewRun(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("id") @Positive Long id
    ) {

        return ResponseEntity.ok(explainCaseOverviewAgentService.getRun(actor, id));
    }
}
