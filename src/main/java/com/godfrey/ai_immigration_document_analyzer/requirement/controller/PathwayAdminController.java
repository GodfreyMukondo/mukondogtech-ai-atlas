package com.godfrey.ai_immigration_document_analyzer.requirement.controller;

import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayAdminDetailResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayStatusChangeRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayUpdateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayAdminService;

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
 * PATHWAY ADMIN CONTROLLER
 * ============================================================================
 *
 * Administrator-only Pathway catalogue authoring (Phase 1 spec, sections 1,
 * 3, 5). Deliberately under {@code /api/admin/**} - this prefix already
 * requires {@code ROLE_ADMIN} via the existing
 * {@code SecurityConfig.authorizeHttpRequests} rule, so no security
 * configuration change was needed to add this controller.
 *
 * This is a separate concern from the applicant-facing
 * {@link PathwayController} (catalogue reads of PUBLISHED pathways only) -
 * neither controller's authorization or behavior is modified by the other.
 *
 * Endpoints:
 *
 *   POST  /api/admin/pathways                 create (starts DRAFT)
 *   GET   /api/admin/pathways                 list all (any status)
 *   GET   /api/admin/pathways/{id}             one pathway, any status
 *   PATCH /api/admin/pathways/{id}             update (DRAFT/REVIEW only)
 *   PATCH /api/admin/pathways/{id}/status      guarded lifecycle transition
 * ============================================================================
 */
@RestController
@RequestMapping("/api/admin/pathways")
@RequiredArgsConstructor
@Validated
public class PathwayAdminController {

    private final PathwayAdminService pathwayAdminService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathwayResponse> create(@Valid @RequestBody PathwayCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(pathwayAdminService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PathwayResponse>> listAll() {

        return ResponseEntity.ok(pathwayAdminService.listAll());
    }

    @GetMapping(value = "/{pathwayId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathwayAdminDetailResponse> getById(@PathVariable("pathwayId") @Positive Long pathwayId) {

        return ResponseEntity.ok(pathwayAdminService.getById(pathwayId));
    }

    @PatchMapping(value = "/{pathwayId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathwayResponse> update(
            @PathVariable("pathwayId") @Positive Long pathwayId,
            @Valid @RequestBody PathwayUpdateRequest request
    ) {

        return ResponseEntity.ok(pathwayAdminService.update(pathwayId, request));
    }

    @PatchMapping(value = "/{pathwayId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathwayResponse> changeStatus(
            @PathVariable("pathwayId") @Positive Long pathwayId,
            @Valid @RequestBody PathwayStatusChangeRequest request
    ) {

        return ResponseEntity.ok(pathwayAdminService.changeStatus(pathwayId, request));
    }
}
