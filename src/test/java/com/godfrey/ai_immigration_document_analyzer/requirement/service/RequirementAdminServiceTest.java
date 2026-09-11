package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementAdminDetailResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementFactBindingRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementStatusChangeRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementUpdateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RegulatoryVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementFactBindingRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequirementAdminService} - Requirement definition
 * authoring (Phase 1 spec, section 2).
 */
@ExtendWith(MockitoExtension.class)
class RequirementAdminServiceTest {

    @Mock
    private RequirementRepository requirementRepository;

    @Mock
    private RequirementFactBindingRepository factBindingRepository;

    @Mock
    private RegulatoryVersionRepository regulatoryVersionRepository;

    private RequirementAdminService requirementAdminService;

    @BeforeEach
    void setUp() {

        requirementAdminService = new RequirementAdminService(
                requirementRepository, factBindingRepository, regulatoryVersionRepository,
                new RequirementLifecycleService(), new ObjectMapper()
        );

        org.mockito.Mockito.lenient().when(requirementRepository.save(any(Requirement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        org.mockito.Mockito.lenient().when(factBindingRepository.save(any(
                com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementFactBinding.class
        ))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private RequirementFactBindingRequest binding(String factKey) {

        RequirementFactBindingRequest binding = new RequirementFactBindingRequest();
        binding.setFactKey(factKey);
        binding.setRequiresVerification(false);
        return binding;
    }

    private RequirementCreateRequest validCreateRequest() {

        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setRequirementKey("TEST.REQUIREMENT");
        request.setRequirementType(RequirementType.EXPERIENCE);
        request.setTitle("Test Requirement");
        request.setJurisdiction("Testland");
        request.setRegulatoryVersionId(1L);
        request.setMandatory(true);
        request.setSatisfactionLogicJson(
                "{\"node\":\"FACT_PREDICATE\",\"factKey\":\"EMPLOYMENT.CURRENT_EMPLOYER\",\"operator\":\"EXISTS\","
                        + "\"operandValue\":null,\"operandValues\":null,\"operandLow\":null,\"operandHigh\":null}"
        );
        request.setFactBindings(List.of(binding("EMPLOYMENT.CURRENT_EMPLOYER")));
        return request;
    }

    // =========================================================================
    // CREATE - VALIDATION GATES
    // =========================================================================

    @Test
    void createRejectsAnUnknownRegulatoryVersion() {

        when(regulatoryVersionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requirementAdminService.create(validCreateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(requirementRepository, never()).save(any());
    }

    @Test
    void createRejectsAnUnknownFactKey() {

        when(regulatoryVersionRepository.findById(1L)).thenReturn(Optional.of(RegulatoryVersion.builder().id(1L).build()));

        RequirementCreateRequest request = validCreateRequest();
        request.setFactBindings(List.of(binding("NOT_A_REAL_KEY")));

        assertThatThrownBy(() -> requirementAdminService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NOT_A_REAL_KEY");

        verify(requirementRepository, never()).save(any());
    }

    @Test
    void createRejectsMalformedSatisfactionLogic() {

        when(regulatoryVersionRepository.findById(1L)).thenReturn(Optional.of(RegulatoryVersion.builder().id(1L).build()));

        RequirementCreateRequest request = validCreateRequest();
        request.setSatisfactionLogicJson("{ not valid json");

        assertThatThrownBy(() -> requirementAdminService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("satisfactionLogicJson");

        verify(requirementRepository, never()).save(any());
    }

    @Test
    void createSucceedsAndPersistsFactBindingsStartingDraft() {

        when(regulatoryVersionRepository.findById(1L)).thenReturn(Optional.of(RegulatoryVersion.builder().id(1L).build()));

        RequirementAdminDetailResponse response = requirementAdminService.create(validCreateRequest());

        assertThat(response.status()).isEqualTo(RequirementStatus.DRAFT);

        verify(factBindingRepository).save(org.mockito.ArgumentMatchers.argThat(b ->
                b.getFactKey().equals("EMPLOYMENT.CURRENT_EMPLOYER") && !b.getRequiresVerification()
        ));
    }

    // =========================================================================
    // UPDATE - ONLY DRAFT
    // =========================================================================

    @Test
    void updateRejectsAPublishedRequirement() {

        Requirement published = Requirement.builder().id(1L).status(RequirementStatus.PUBLISHED).build();

        when(requirementRepository.findById(1L)).thenReturn(Optional.of(published));

        RequirementUpdateRequest update = new RequirementUpdateRequest();
        update.setRequirementType(RequirementType.EXPERIENCE);
        update.setTitle("Changed");
        update.setJurisdiction("Testland");
        update.setRegulatoryVersionId(1L);
        update.setMandatory(true);
        update.setSatisfactionLogicJson(validCreateRequest().getSatisfactionLogicJson());
        update.setFactBindings(List.of(binding("EMPLOYMENT.CURRENT_EMPLOYER")));

        assertThatThrownBy(() -> requirementAdminService.update(1L, update))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateReplacesFactBindingsWhollyForADraftRequirement() {

        Requirement draft = Requirement.builder().id(1L).status(RequirementStatus.DRAFT).build();

        when(requirementRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(regulatoryVersionRepository.findById(1L)).thenReturn(Optional.of(RegulatoryVersion.builder().id(1L).build()));

        RequirementUpdateRequest update = new RequirementUpdateRequest();
        update.setRequirementType(RequirementType.EXPERIENCE);
        update.setTitle("Changed Title");
        update.setJurisdiction("Testland");
        update.setRegulatoryVersionId(1L);
        update.setMandatory(true);
        update.setSatisfactionLogicJson(validCreateRequest().getSatisfactionLogicJson());
        update.setFactBindings(List.of(binding("LANGUAGE_PROFICIENCY.TEST_SCORE")));

        RequirementAdminDetailResponse response = requirementAdminService.update(1L, update);

        assertThat(response.title()).isEqualTo("Changed Title");

        verify(factBindingRepository).deleteByRequirementId(1L);
        verify(factBindingRepository).save(org.mockito.ArgumentMatchers.argThat(b ->
                b.getFactKey().equals("LANGUAGE_PROFICIENCY.TEST_SCORE")
        ));
    }

    // =========================================================================
    // STATUS TRANSITIONS
    // =========================================================================

    @Test
    void changeStatusDraftToPublishedIsAllowed() {

        Requirement draft = Requirement.builder().id(1L).status(RequirementStatus.DRAFT).build();

        when(requirementRepository.findById(1L)).thenReturn(Optional.of(draft));

        RequirementStatusChangeRequest request = new RequirementStatusChangeRequest();
        request.setTargetStatus(RequirementStatus.PUBLISHED);

        RequirementResponse response = requirementAdminService.changeStatus(1L, request);

        assertThat(response.status()).isEqualTo(RequirementStatus.PUBLISHED);
    }

    @Test
    void changeStatusPublishedBackToDraftIsRejected() {

        Requirement published = Requirement.builder().id(1L).status(RequirementStatus.PUBLISHED).build();

        when(requirementRepository.findById(1L)).thenReturn(Optional.of(published));

        RequirementStatusChangeRequest request = new RequirementStatusChangeRequest();
        request.setTargetStatus(RequirementStatus.DRAFT);

        assertThatThrownBy(() -> requirementAdminService.changeStatus(1L, request))
                .isInstanceOf(IllegalStateException.class);
    }
}
