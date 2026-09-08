package com.godfrey.ai_immigration_document_analyzer.fact.controller;

import com.godfrey.ai_immigration_document_analyzer.fact.dto.DigitalTwinResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.service.DigitalTwinProjectionService;
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
 * Exposes the Digital Twin read projection: "what does MukondoGTech AI
 * currently know about this case" in one call, so a future consumer never
 * needs to inspect FACTS/FACT_EVIDENCE/FACT_CONFLICTS directly.
 *
 * Endpoint: GET /api/twin/{subjectUserId}
 */
@RestController
@RequestMapping("/api/twin")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DigitalTwinController {

    private final DigitalTwinProjectionService digitalTwinProjectionService;

    @GetMapping(value = "/{subjectUserId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DigitalTwinResponse> getDigitalTwin(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("subjectUserId") @Positive Long subjectUserId
    ) {

        return ResponseEntity.ok(digitalTwinProjectionService.getDigitalTwin(actor, subjectUserId));
    }
}
