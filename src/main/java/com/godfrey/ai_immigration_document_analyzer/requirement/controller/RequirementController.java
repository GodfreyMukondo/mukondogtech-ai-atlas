package com.godfrey.ai_immigration_document_analyzer.requirement.controller;

import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementEvaluationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.RequirementEvaluationService;
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
 * REQUIREMENT CONTROLLER
 * ============================================================================
 *
 * Minimum API surface for the Requirement layer. Authoring (create/update/
 * publish) lives separately on {@code RequirementAdminController}
 * ({@code /api/admin/requirements}, ROLE_ADMIN only) - this controller
 * remains read/evaluate only. Catalogue reads (GET /{id}) return
 * impersonal, reusable regulatory knowledge and need only authentication.
 * Evaluation reads/writes are personal and delegate their authorization
 * entirely to {@code RequirementEvaluationService}, which never touches a
 * Fact without going through the existing Fact authorization boundary -
 * this controller makes no authorization decisions itself.
 *
 * Endpoints:
 *
 *   GET  /api/requirements/{requirementId}                         catalogue read
 *   POST /api/requirements/{requirementId}/evaluations             evaluate for a subject
 *   GET  /api/requirements/evaluations/{evaluationId}               retrieve an evaluation
 * ============================================================================
 */
@RestController
@RequestMapping("/api/requirements")
@RequiredArgsConstructor
@Validated
public class RequirementController {

    private final RequirementRepository requirementRepository;
    private final RequirementEvaluationService requirementEvaluationService;

    @GetMapping(value = "/{requirementId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RequirementResponse> getRequirement(@PathVariable("requirementId") @Positive Long requirementId) {

        return requirementRepository.findById(requirementId)
                .map(RequirementResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement not found."));
    }

    @PostMapping(value = "/{requirementId}/evaluations", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RequirementEvaluationResponse> evaluate(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("requirementId") @Positive Long requirementId,
            @RequestParam("subjectUserId") @Positive Long subjectUserId,
            @RequestParam(value = "assessmentDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime assessmentDate
    ) {

        RequirementEvaluationResponse response = requirementEvaluationService.evaluate(actor, requirementId, subjectUserId, assessmentDate);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/evaluations/{evaluationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RequirementEvaluationResponse> getEvaluation(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("evaluationId") @Positive Long evaluationId
    ) {

        return ResponseEntity.ok(requirementEvaluationService.getEvaluation(actor, evaluationId));
    }
}
