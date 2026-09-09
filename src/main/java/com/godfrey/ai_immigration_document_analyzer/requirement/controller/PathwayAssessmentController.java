package com.godfrey.ai_immigration_document_analyzer.requirement.controller;

import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayAssessmentResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayDiscoveryResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.ExplainabilityService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayAssessmentService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayDiscoveryService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * PATHWAY ASSESSMENT CONTROLLER
 * ============================================================================
 *
 * Personalized Pathway evaluation. Every operation here is subject-scoped
 * personal information and delegates authorization entirely to
 * {@code PathwayAssessmentService}/{@code ExplainabilityService} (which
 * reuse the existing Fact authorization boundary) before any data is
 * touched - this controller makes no authorization decisions itself,
 * exactly like {@code FactController}.
 *
 * Endpoints:
 *
 *   GET  /api/pathways/discovery                                   read-only, ranked alignment across every PUBLISHED pathway (Phase 4)
 *   POST /api/pathways/{pathwayId}/assessments                     request a personalized assessment
 *   GET  /api/pathways/assessments/{assessmentId}                  retrieve an assessment
 *   GET  /api/pathways/assessments/{assessmentId}/explanation      full explainability chain
 * ============================================================================
 */
@RestController
@RequestMapping("/api/pathways")
@RequiredArgsConstructor
@Validated
public class PathwayAssessmentController {

    private final PathwayAssessmentService pathwayAssessmentService;
    private final ExplainabilityService explainabilityService;
    private final PathwayDiscoveryService pathwayDiscoveryService;

    /**
     * Read-only, transient ranking of every PUBLISHED pathway against the
     * subject's current case (Phase 4). Never creates a PathwayAssessment or
     * RequirementEvaluation row - see {@link PathwayDiscoveryService}.
     */
    @GetMapping(value = "/discovery", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathwayDiscoveryResponse> discoverPathways(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam("subjectUserId") @Positive Long subjectUserId
    ) {

        return ResponseEntity.ok(pathwayDiscoveryService.discover(actor, subjectUserId));
    }

    @PostMapping(value = "/{pathwayId}/assessments", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathwayAssessmentResponse> requestAssessment(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("pathwayId") @Positive Long pathwayId,
            @RequestParam("subjectUserId") @Positive Long subjectUserId,
            @RequestParam(value = "assessmentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime assessmentDate
    ) {

        PathwayAssessmentResponse response = pathwayAssessmentService.assess(actor, pathwayId, subjectUserId, assessmentDate);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/assessments/{assessmentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathwayAssessmentResponse> getAssessment(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("assessmentId") @Positive Long assessmentId
    ) {

        return ResponseEntity.ok(pathwayAssessmentService.getAssessment(actor, assessmentId));
    }

    @GetMapping(value = "/assessments/{assessmentId}/explanation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ExplanationResponse> explainAssessment(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("assessmentId") @Positive Long assessmentId
    ) {

        return ResponseEntity.ok(explainabilityService.explain(actor, assessmentId));
    }
}
