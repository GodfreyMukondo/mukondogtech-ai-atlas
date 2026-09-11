package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactTypeRegistry;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementAdminDetailResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementFactBindingRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementStatusChangeRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementUpdateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementFactBinding;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RegulatoryVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementFactBindingRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ============================================================================
 * REQUIREMENT ADMIN SERVICE
 * ============================================================================
 *
 * Administrator-only authoring surface for Requirement definitions
 * (Phase 1 spec, section 2). Every write here goes through the same
 * discipline the rest of the Requirement layer already established:
 *
 *   - {@code satisfactionLogicJson}/{@code applicabilityLogicJson} must
 *     parse into the existing, closed {@code LogicNode} grammar - never
 *     accepted as opaque text, never executable.
 *   - Every {@code factKey} named in a binding must already exist in
 *     {@code FactTypeRegistry} - a Requirement can never declare a
 *     dependency on a Fact key the system doesn't otherwise know about.
 *   - {@code regulatoryVersionId} must reference a real, already-recorded
 *     {@code RegulatoryVersion} - a Requirement can never exist without a
 *     traceable regulatory source.
 *   - Once {@code PUBLISHED}, a Requirement's content is immutable; a rule
 *     change is a new row against a new RegulatoryVersion, never an edit
 *     of a row evaluation may already have relied on.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class RequirementAdminService {

    private final RequirementRepository requirementRepository;
    private final RequirementFactBindingRepository factBindingRepository;
    private final RegulatoryVersionRepository regulatoryVersionRepository;
    private final RequirementLifecycleService lifecycleService;
    private final ObjectMapper objectMapper;

    @Transactional
    public RequirementAdminDetailResponse create(RequirementCreateRequest request) {

        regulatoryVersionRepository.findById(request.getRegulatoryVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Regulatory version not found."));

        validateLogic(request.getSatisfactionLogicJson(), "satisfactionLogicJson");
        validateLogic(request.getApplicabilityLogicJson(), "applicabilityLogicJson");
        validateFactBindings(request.getFactBindings());

        Requirement requirement = Requirement.builder()
                .requirementKey(request.getRequirementKey())
                .requirementType(request.getRequirementType())
                .title(request.getTitle())
                .description(request.getDescription())
                .jurisdiction(request.getJurisdiction())
                .immigrationContext(request.getImmigrationContext())
                .regulatoryVersionId(request.getRegulatoryVersionId())
                .mandatory(request.getMandatory())
                .applicabilityLogic(request.getApplicabilityLogicJson())
                .satisfactionLogic(request.getSatisfactionLogicJson())
                .status(RequirementStatus.DRAFT)
                .build();

        Requirement saved = requirementRepository.save(requirement);

        List<RequirementFactBinding> bindings = saveFactBindings(saved.getId(), request.getFactBindings());

        return RequirementAdminDetailResponse.from(saved, bindings);
    }

    @Transactional
    public RequirementAdminDetailResponse update(Long requirementId, RequirementUpdateRequest request) {

        Requirement requirement = requireRequirement(requirementId);

        if (requirement.getStatus() != RequirementStatus.DRAFT) {

            throw new IllegalStateException(
                    "Only a DRAFT requirement may be edited. Current status: " + requirement.getStatus()
            );
        }

        regulatoryVersionRepository.findById(request.getRegulatoryVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Regulatory version not found."));

        validateLogic(request.getSatisfactionLogicJson(), "satisfactionLogicJson");
        validateLogic(request.getApplicabilityLogicJson(), "applicabilityLogicJson");
        validateFactBindings(request.getFactBindings());

        requirement.setRequirementType(request.getRequirementType());
        requirement.setTitle(request.getTitle());
        requirement.setDescription(request.getDescription());
        requirement.setJurisdiction(request.getJurisdiction());
        requirement.setImmigrationContext(request.getImmigrationContext());
        requirement.setRegulatoryVersionId(request.getRegulatoryVersionId());
        requirement.setMandatory(request.getMandatory());
        requirement.setApplicabilityLogic(request.getApplicabilityLogicJson());
        requirement.setSatisfactionLogic(request.getSatisfactionLogicJson());

        Requirement saved = requirementRepository.save(requirement);

        factBindingRepository.deleteByRequirementId(requirementId);
        List<RequirementFactBinding> bindings = saveFactBindings(requirementId, request.getFactBindings());

        return RequirementAdminDetailResponse.from(saved, bindings);
    }

    @Transactional
    public RequirementResponse changeStatus(Long requirementId, RequirementStatusChangeRequest request) {

        Requirement requirement = requireRequirement(requirementId);

        lifecycleService.assertValidTransition(requirement.getStatus(), request.getTargetStatus());

        requirement.setStatus(request.getTargetStatus());

        return RequirementResponse.from(requirementRepository.save(requirement));
    }

    @Transactional(readOnly = true)
    public RequirementAdminDetailResponse getById(Long requirementId) {

        Requirement requirement = requireRequirement(requirementId);
        List<RequirementFactBinding> bindings = factBindingRepository.findByRequirementId(requirementId);

        return RequirementAdminDetailResponse.from(requirement, bindings);
    }

    @Transactional(readOnly = true)
    public List<RequirementResponse> listAll() {

        return requirementRepository.findAll().stream()
                .map(RequirementResponse::from)
                .toList();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Requirement requireRequirement(Long requirementId) {

        return requirementRepository.findById(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement not found."));
    }

    private void validateLogic(String json, String fieldName) {

        if (json == null || json.isBlank()) {
            return;
        }

        try {

            objectMapper.readValue(json, LogicNode.class);

        } catch (JsonProcessingException exception) {

            throw new IllegalArgumentException(
                    fieldName + " is not a valid LogicNode expression: " + exception.getOriginalMessage()
            );
        }
    }

    private void validateFactBindings(List<RequirementFactBindingRequest> bindings) {

        for (RequirementFactBindingRequest binding : bindings) {

            if (!FactTypeRegistry.isKnown(binding.getFactKey())) {

                throw new IllegalArgumentException(
                        "Unknown fact key (not present in FactTypeRegistry): " + binding.getFactKey()
                );
            }
        }
    }

    private List<RequirementFactBinding> saveFactBindings(Long requirementId, List<RequirementFactBindingRequest> bindings) {

        return bindings.stream()
                .map(binding -> factBindingRepository.save(
                        RequirementFactBinding.builder()
                                .requirementId(requirementId)
                                .factKey(binding.getFactKey())
                                .requiresVerification(binding.getRequiresVerification())
                                .minimumProvenanceType(binding.getMinimumProvenanceType())
                                .build()
                ))
                .toList();
    }
}
