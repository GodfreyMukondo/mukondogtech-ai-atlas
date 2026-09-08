package com.godfrey.ai_immigration_document_analyzer.fact.controller;

import com.godfrey.ai_immigration_document_analyzer.fact.dto.ConflictResolutionRequest;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactConflictResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactProposalRequest;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactTimelineEntryResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactVerificationRequest;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ============================================================================
 * FACT CONTROLLER
 * ============================================================================
 *
 * Minimum API surface for the Fact foundation - no generic CRUD, no
 * endpoint accepts a raw status/confidence/verification value from a
 * client. Every operation delegates authorization to
 * {@code FactAuthorizationService} through {@code FactService} before any
 * data access; this controller performs no authorization decisions itself.
 *
 * Endpoints:
 *
 *   POST  /api/facts                          propose a fact
 *   GET   /api/facts/{factId}                 retrieve one fact (incl. provenance/confidence)
 *   GET   /api/facts?subjectUserId=...        list a subject's facts
 *   PATCH /api/facts/{factId}/verify          verify a fact (case worker only)
 *   GET   /api/facts/timeline?subjectUserId=  view the transactional timeline
 *   GET   /api/facts/conflicts/{conflictId}   view a conflict
 *   PATCH /api/facts/conflicts/{conflictId}/resolve   resolve a conflict (case worker only)
 * ============================================================================
 */
@RestController
@RequestMapping("/api/facts")
@RequiredArgsConstructor
@Slf4j
@Validated
public class FactController {

    private final FactService factService;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FactResponse> proposeFact(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @Valid @RequestBody FactProposalRequest request
    ) {

        FactResponse response = factService.proposeFact(actor, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/{factId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FactResponse> getFact(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("factId") @Positive Long factId
    ) {

        return ResponseEntity.ok(factService.getFact(actor, factId));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FactResponse>> listFacts(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam("subjectUserId") @Positive Long subjectUserId
    ) {

        return ResponseEntity.ok(factService.listFactsForSubject(actor, subjectUserId));
    }

    @PatchMapping(
            value = "/{factId}/verify",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FactResponse> verifyFact(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("factId") @Positive Long factId,
            @Valid @RequestBody FactVerificationRequest request
    ) {

        return ResponseEntity.ok(factService.verifyFact(actor, factId, request));
    }

    @GetMapping(value = "/timeline", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FactTimelineEntryResponse>> getTimeline(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam("subjectUserId") @Positive Long subjectUserId
    ) {

        return ResponseEntity.ok(factService.getTimeline(actor, subjectUserId));
    }

    @GetMapping(value = "/conflicts/{conflictId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FactConflictResponse> getConflict(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("conflictId") @Positive Long conflictId
    ) {

        return ResponseEntity.ok(factService.getConflict(actor, conflictId));
    }

    @PatchMapping(
            value = "/conflicts/{conflictId}/resolve",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<FactConflictResponse> resolveConflict(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("conflictId") @Positive Long conflictId,
            @Valid @RequestBody ConflictResolutionRequest request
    ) {

        return ResponseEntity.ok(factService.resolveConflict(actor, conflictId, request));
    }
}
