package com.godfrey.ai_immigration_document_analyzer.requirement.controller;

import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactTypeRegistry;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.FactTypeSummaryResponse;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ============================================================================
 * FACT TYPE ADMIN CONTROLLER
 * ============================================================================
 *
 * Read-only listing of the known {@code FactTypeRegistry} vocabulary, for
 * the Requirement authoring admin UI's fact-key picker. Under
 * {@code /api/admin/**} - already {@code ROLE_ADMIN}-gated by the existing
 * {@code SecurityConfig} rule. Exposes nothing beyond what
 * {@code FactTypeRegistry} already declares as public, in-code reference
 * data - no new registry, no duplicated taxonomy.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/admin/fact-types")
public class FactTypeAdminController {

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<FactTypeSummaryResponse>> listKnownFactTypes() {

        List<FactTypeSummaryResponse> types = FactTypeRegistry.allDefinitions().values().stream()
                .map(FactTypeSummaryResponse::from)
                .toList();

        return ResponseEntity.ok(types);
    }
}
