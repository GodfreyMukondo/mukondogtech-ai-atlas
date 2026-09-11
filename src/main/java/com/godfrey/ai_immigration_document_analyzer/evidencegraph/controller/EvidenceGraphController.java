package com.godfrey.ai_immigration_document_analyzer.evidencegraph.controller;

import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceFullTraceResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceGraphResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceItemResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.service.EvidenceGraphService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
 * EVIDENCE GRAPH CONTROLLER
 * ============================================================================
 *
 * Read-only API surface for the Evidence Intelligence Graph. No endpoint
 * here accepts a subjectUserId of any kind from the caller - the backend
 * resolves the subject from the fetched Fact/PathwayAssessment record
 * itself and authorizes against it, exactly like every other endpoint in
 * this codebase (FactController, PathwayController). No new authorization
 * surface is introduced: every method delegates to
 * {@code FactAuthorizationService} via {@code EvidenceGraphService}.
 *
 * This prefix requires no {@code SecurityConfig} change - it falls through
 * to the existing fail-closed {@code .anyRequest().authenticated()} rule,
 * exactly like {@code /api/facts/**} and {@code /api/twin/**} already do.
 *
 * Endpoints:
 *
 *   GET /api/evidence-graph/facts/{factId}
 *                                   node/edge graph for one Fact: its
 *                                   evidence, documents, conflicts, and the
 *                                   requirement evaluations that depend on it
 *   GET /api/evidence-graph/evidence-items/{evidenceItemId}
 *                                   evidence detail panel (source data /
 *                                   system interpretation / derived conclusion)
 *   GET /api/evidence-graph/pathway-assessments/{assessmentId}/full-trace
 *                                   the full Pathway -> ... -> Document chain,
 *                                   composed around the existing
 *                                   ExplainabilityService.explain(...)
 *   GET /api/evidence-graph/documents/{documentId}
 *                                   node/edge graph for one Document: its
 *                                   versions, the evidence/facts it produced,
 *                                   the requirements those facts affect, and
 *                                   any conflicts they carry (backward trace
 *                                   + document impact, Phase 3)
 *   GET /api/evidence-graph/requirement-evaluations/{evaluationId}
 *                                   node/edge graph for one Requirement
 *                                   Evaluation: the Requirement and its
 *                                   RegulatoryVersion, every contributing
 *                                   Fact and its own evidence/document/
 *                                   conflict chain, and any FactConflict
 *                                   this evaluation is recorded as blocked
 *                                   by (requirement traceability, Phase 3)
 * ============================================================================
 */
@RestController
@RequestMapping("/api/evidence-graph")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EvidenceGraphController {

    private final EvidenceGraphService evidenceGraphService;

    @GetMapping(value = "/facts/{factId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvidenceGraphResponse> graphForFact(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("factId") @Positive Long factId
    ) {

        return ResponseEntity.ok(evidenceGraphService.graphForFact(actor, factId));
    }

    @GetMapping(value = "/evidence-items/{evidenceItemId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvidenceItemResponse> getEvidenceItem(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("evidenceItemId") @Positive Long evidenceItemId
    ) {

        return ResponseEntity.ok(evidenceGraphService.getEvidenceItemDetail(actor, evidenceItemId));
    }

    @GetMapping(value = "/pathway-assessments/{assessmentId}/full-trace", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvidenceFullTraceResponse> fullTrace(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("assessmentId") @Positive Long assessmentId
    ) {

        return ResponseEntity.ok(evidenceGraphService.fullTrace(actor, assessmentId));
    }

    @GetMapping(value = "/documents/{documentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvidenceGraphResponse> graphForDocument(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("documentId") @Positive Long documentId
    ) {

        return ResponseEntity.ok(evidenceGraphService.graphForDocument(actor, documentId));
    }

    @GetMapping(value = "/requirement-evaluations/{evaluationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvidenceGraphResponse> graphForRequirementEvaluation(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("evaluationId") @Positive Long evaluationId
    ) {

        return ResponseEntity.ok(evidenceGraphService.graphForRequirementEvaluation(actor, evaluationId));
    }
}
