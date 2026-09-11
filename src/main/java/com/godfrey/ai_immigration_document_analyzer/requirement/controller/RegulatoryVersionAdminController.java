package com.godfrey.ai_immigration_document_analyzer.requirement.controller;

import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RegulatoryVersionCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RegulatoryVersionResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.RegulatoryVersionAdminService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ============================================================================
 * REGULATORY VERSION ADMIN CONTROLLER
 * ============================================================================
 *
 * Administrator-only. Deliberately append-only - no update/delete endpoint,
 * matching {@code RegulatoryVersion}'s own immutable-by-design entity
 * contract (a regulation that changes is a new row, never an in-place
 * edit). Under {@code /api/admin/**} - already {@code ROLE_ADMIN}-gated by
 * the existing {@code SecurityConfig} rule.
 *
 * Endpoints:
 *
 *   POST /api/admin/regulatory-versions            record a new version
 *   GET  /api/admin/regulatory-versions             list all
 *   GET  /api/admin/regulatory-versions/{id}         one version
 * ============================================================================
 */
@RestController
@RequestMapping("/api/admin/regulatory-versions")
@RequiredArgsConstructor
@Validated
public class RegulatoryVersionAdminController {

    private final RegulatoryVersionAdminService regulatoryVersionAdminService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RegulatoryVersionResponse> create(@Valid @RequestBody RegulatoryVersionCreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(regulatoryVersionAdminService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RegulatoryVersionResponse>> listAll() {

        return ResponseEntity.ok(regulatoryVersionAdminService.listAll());
    }

    @GetMapping(value = "/{versionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RegulatoryVersionResponse> getById(@PathVariable("versionId") @Positive Long versionId) {

        return ResponseEntity.ok(regulatoryVersionAdminService.getById(versionId));
    }
}
