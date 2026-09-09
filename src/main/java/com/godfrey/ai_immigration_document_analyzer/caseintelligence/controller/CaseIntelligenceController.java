package com.godfrey.ai_immigration_document_analyzer.caseintelligence.controller;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseContradictionsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIntelligenceResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseSignalsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseTimelineResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.service.CaseIntelligenceService;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.service.CaseOverviewService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * CASE INTELLIGENCE CONTROLLER
 * ============================================================================
 *
 * Case Intelligence / Discovery / Readiness foundation (Master Platform
 * Expansion, Phase 2). Every operation is subject-scoped personal
 * information and delegates authorization entirely to the underlying
 * services (which reuse the existing Fact authorization boundary) - this
 * controller makes no authorization decisions itself, exactly like
 * {@code PathwayAssessmentController} and {@code DigitalTwinController}.
 *
 * Endpoints:
 *
 *   GET /api/cases/pathway-assessments/{assessmentId}/intelligence   readiness + evidence matrix + missing evidence
 *   GET /api/cases/{subjectUserId}/contradictions                    open cross-document contradictions
 *   GET /api/cases/{subjectUserId}/timeline                          immigration timeline
 *   GET /api/cases/{subjectUserId}/signals                           evidence/anomaly signal summary
 * ============================================================================
 */
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Validated
public class CaseIntelligenceController {

    private final CaseIntelligenceService caseIntelligenceService;
    private final CaseOverviewService caseOverviewService;

    @GetMapping(value = "/pathway-assessments/{assessmentId}/intelligence", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CaseIntelligenceResponse> getCaseIntelligence(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("assessmentId") @Positive Long assessmentId
    ) {
        return ResponseEntity.ok(caseIntelligenceService.getCaseIntelligence(actor, assessmentId));
    }

    @GetMapping(value = "/{subjectUserId}/contradictions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CaseContradictionsResponse> getContradictions(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("subjectUserId") @Positive Long subjectUserId
    ) {
        return ResponseEntity.ok(caseOverviewService.getContradictions(actor, subjectUserId));
    }

    @GetMapping(value = "/{subjectUserId}/timeline", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CaseTimelineResponse> getTimeline(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("subjectUserId") @Positive Long subjectUserId
    ) {
        return ResponseEntity.ok(caseOverviewService.getTimeline(actor, subjectUserId));
    }

    @GetMapping(value = "/{subjectUserId}/signals", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CaseSignalsResponse> getSignals(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("subjectUserId") @Positive Long subjectUserId
    ) {
        return ResponseEntity.ok(caseOverviewService.getSignals(actor, subjectUserId));
    }
}
