package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayAdminDetailResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayStatusChangeRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayUpdateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.AndNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.DerivedPredicateNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.FactPredicateNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.NotNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.OrNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.RequirementRefNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================================
 * PATHWAY ADMIN SERVICE
 * ============================================================================
 *
 * Administrator-only authoring surface for the Pathway catalogue
 * (Phase 1 spec, sections 1 and 3).
 *
 * {@code compositionLogic} is never authored as free-form JSON here - this
 * service builds it deterministically as an AND of the selected
 * {@code requirementIds} (every Requirement referenced here already exists
 * and is validated by id; none is duplicated or embedded), matching the
 * existing {@code LogicNode}/{@code RequirementRefNode} grammar exactly.
 *
 * PUBLISHING: only one row per {@code pathwayKey} is ever PUBLISHED at a
 * time. Publishing a new row for a key that already has a PUBLISHED row
 * automatically transitions the previous one to SUPERSEDED in the same
 * transaction - its content is left completely untouched, so every
 * historical {@code PathwayAssessment.pathwayId} remains reproducible
 * against the exact row that produced it.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class PathwayAdminService {

    private final PathwayRepository pathwayRepository;
    private final RequirementRepository requirementRepository;
    private final PathwayLifecycleService lifecycleService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PathwayResponse create(PathwayCreateRequest request) {

        validateRequirementsExist(request.getRequirementIds());

        Pathway pathway = Pathway.builder()
                .pathwayKey(request.getPathwayKey())
                .name(request.getName())
                .description(request.getDescription())
                .jurisdiction(request.getJurisdiction())
                .category(request.getCategory())
                .compositionLogic(buildCompositionLogic(request.getRequirementIds()))
                .evidenceExpectations(request.getEvidenceExpectations())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .status(PathwayStatus.DRAFT)
                .build();

        return PathwayResponse.from(pathwayRepository.save(pathway));
    }

    @Transactional
    public PathwayResponse update(Long pathwayId, PathwayUpdateRequest request) {

        Pathway pathway = requirePathway(pathwayId);

        if (pathway.getStatus() != PathwayStatus.DRAFT && pathway.getStatus() != PathwayStatus.REVIEW) {

            throw new IllegalStateException(
                    "Only a DRAFT or REVIEW pathway may be edited. Current status: " + pathway.getStatus()
            );
        }

        validateRequirementsExist(request.getRequirementIds());

        pathway.setName(request.getName());
        pathway.setDescription(request.getDescription());
        pathway.setJurisdiction(request.getJurisdiction());
        pathway.setCategory(request.getCategory());
        pathway.setCompositionLogic(buildCompositionLogic(request.getRequirementIds()));
        pathway.setEvidenceExpectations(request.getEvidenceExpectations());
        pathway.setValidFrom(request.getValidFrom());
        pathway.setValidTo(request.getValidTo());

        return PathwayResponse.from(pathwayRepository.save(pathway));
    }

    @Transactional
    public PathwayResponse changeStatus(Long pathwayId, PathwayStatusChangeRequest request) {

        Pathway pathway = requirePathway(pathwayId);

        lifecycleService.assertValidTransition(pathway.getStatus(), request.getTargetStatus());

        if (request.getTargetStatus() == PathwayStatus.PUBLISHED) {
            supersedePreviousPublishedVersion(pathway);
        }

        pathway.setStatus(request.getTargetStatus());

        return PathwayResponse.from(pathwayRepository.save(pathway));
    }

    /**
     * Returns the pathway together with the requirementIds already encoded
     * in its own {@code compositionLogic} - the same JSON {@link #create}/
     * {@link #update} deterministically rebuild on every write, read back
     * out via the same {@code LogicNode} grammar rather than a second,
     * separately-maintained representation. This is what lets the admin
     * edit form pre-select a pathway's existing requirements.
     */
    @Transactional(readOnly = true)
    public PathwayAdminDetailResponse getById(Long pathwayId) {

        Pathway pathway = requirePathway(pathwayId);

        return PathwayAdminDetailResponse.from(pathway, extractRequirementIds(pathway.getCompositionLogic()));
    }

    @Transactional(readOnly = true)
    public List<PathwayResponse> listAll() {

        return pathwayRepository.findAll().stream()
                .map(PathwayResponse::from)
                .toList();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Pathway requirePathway(Long pathwayId) {

        return pathwayRepository.findById(pathwayId)
                .orElseThrow(() -> new ResourceNotFoundException("Pathway not found."));
    }

    private void validateRequirementsExist(List<Long> requirementIds) {

        for (Long requirementId : requirementIds) {

            if (requirementRepository.findById(requirementId).isEmpty()) {

                throw new IllegalArgumentException(
                        "Requirement not found: " + requirementId
                );
            }
        }
    }

    private String buildCompositionLogic(List<Long> requirementIds) {

        LogicNode composition = new AndNode(
                requirementIds.stream()
                        .<LogicNode>map(RequirementRefNode::new)
                        .toList()
        );

        try {

            return objectMapper.writeValueAsString(composition);

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException("Failed to serialize pathway composition logic.", exception);
        }
    }

    /**
     * Walks the existing, closed {@code LogicNode} grammar (the same one
     * {@link #buildCompositionLogic} produces) and collects every
     * referenced requirement id - not just the deterministic flat AND
     * shape this service itself always builds, so a pathway composed by
     * some other means (or a future, richer admin UI) still round-trips
     * correctly. Never a second/duplicated composition representation:
     * this reads the pathway's own persisted {@code compositionLogic},
     * nothing else.
     */
    private List<Long> extractRequirementIds(String compositionLogicJson) {

        try {

            LogicNode root = objectMapper.readValue(compositionLogicJson, LogicNode.class);
            List<Long> requirementIds = new ArrayList<>();
            collectRequirementIds(root, requirementIds);

            return requirementIds;

        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {

            throw new IllegalStateException("Failed to parse pathway composition logic.", exception);
        }
    }

    private void collectRequirementIds(LogicNode node, List<Long> requirementIds) {

        switch (node) {
            case AndNode and -> and.children().forEach(child -> collectRequirementIds(child, requirementIds));
            case OrNode or -> or.children().forEach(child -> collectRequirementIds(child, requirementIds));
            case NotNode not -> collectRequirementIds(not.child(), requirementIds);
            case RequirementRefNode ref -> requirementIds.add(ref.requirementId());
            case FactPredicateNode ignored -> { }
            case DerivedPredicateNode ignored -> { }
        }
    }

    private void supersedePreviousPublishedVersion(Pathway newlyPublished) {

        pathwayRepository.findByPathwayKeyAndStatus(newlyPublished.getPathwayKey(), PathwayStatus.PUBLISHED)
                .filter(previous -> !previous.getId().equals(newlyPublished.getId()))
                .ifPresent(previous -> {

                    lifecycleService.assertValidTransition(previous.getStatus(), PathwayStatus.SUPERSEDED);

                    previous.setStatus(PathwayStatus.SUPERSEDED);
                    pathwayRepository.save(previous);
                });
    }
}
