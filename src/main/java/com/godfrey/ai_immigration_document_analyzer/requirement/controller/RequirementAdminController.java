package com.godfrey.ai_immigration_document_analyzer.requirement.controller;

import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementAdminDetailResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementStatusChangeRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementUpdateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.RequirementAdminService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ============================================================================
 * REQUIREMENT ADMIN CONTROLLER
 * ============================================================================
 *
 * Administrator-only Requirement definition authoring (Phase 1 spec,
 * sections 2, 5). Under {@code /api/admin/**} - already {@code ROLE_ADMIN}-
 * gated by the existing {@code SecurityConfig} rule; no security
 * configuration change was needed.
 *
 * Separate from the applicant-facing {@link RequirementController}
 * (catalogue read of one Requirement + personalized evaluation) - neither
 * controller's authorization or behavior is modified by the other.
 *
 * Endpoints:
 *
 *   POST  /api/admin/requirements                create (starts DRAFT)
 *   GET   /api/admin/requirements                list all (any status)
 *   GET   /api/admin/requirements/{id}            one requirement, any status
 *   PATCH /api/admin/requirements/{id}            update (DRAFT only)
 *   PATCH /api/admin/requirements/{id}/status     guarded lifecycle transition
 * ============================================================================
 */
@RestController
@RequestMapping("/api/admin/requirements")
@RequiredArgsConstructor
@Validated
public class RequirementAdminController {

    private final RequirementAdminService requirementAdminService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RequirementAdminDetailResponse> create(@Valid @RequestBody RequirementCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(requirementAdminService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RequirementResponse>> listAll() {

        return ResponseEntity.ok(requirementAdminService.listAll());
    }

    @GetMapping(value = "/{requirementId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RequirementAdminDetailResponse> getById(@PathVariable("requirementId") @Positive Long requirementId) {

        return ResponseEntity.ok(requirementAdminService.getById(requirementId));
    }

    @PatchMapping(value = "/{requirementId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RequirementAdminDetailResponse> update(
            @PathVariable("requirementId") @Positive Long requirementId,
            @Valid @RequestBody RequirementUpdateRequest request
    ) {

        return ResponseEntity.ok(requirementAdminService.update(requirementId, request));
    }

    @PatchMapping(value = "/{requirementId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RequirementResponse> changeStatus(
            @PathVariable("requirementId") @Positive Long requirementId,
            @Valid @RequestBody RequirementStatusChangeRequest request
    ) {

        return ResponseEntity.ok(requirementAdminService.changeStatus(requirementId, request));
    }
}
