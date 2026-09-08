package com.godfrey.ai_immigration_document_analyzer.requirement.controller;

import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;

import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ============================================================================
 * PATHWAY CONTROLLER
 * ============================================================================
 *
 * Catalogue reads only - a Pathway is impersonal regulatory knowledge (never
 * a person's application), so these endpoints need only authentication, no
 * case-level authorization. Personalized evaluation lives on
 * {@link PathwayAssessmentController}.
 *
 * Endpoints:
 *
 *   GET /api/pathways              list published pathways
 *   GET /api/pathways/{pathwayId}  one pathway
 * ============================================================================
 */
@RestController
@RequestMapping("/api/pathways")
@RequiredArgsConstructor
@Validated
public class PathwayController {

    private final PathwayRepository pathwayRepository;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PathwayResponse>> listPublishedPathways() {

        List<PathwayResponse> pathways = pathwayRepository.findByStatus(PathwayStatus.PUBLISHED)
                .stream()
                .map(PathwayResponse::from)
                .toList();

        return ResponseEntity.ok(pathways);
    }

    @GetMapping(value = "/{pathwayId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PathwayResponse> getPathway(@PathVariable("pathwayId") @Positive Long pathwayId) {

        return pathwayRepository.findById(pathwayId)
                .map(PathwayResponse::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Pathway not found."));
    }
}
