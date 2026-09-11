package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayStatusChangeRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayUpdateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;
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
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PathwayAdminService} - Pathway catalogue authoring
 * (Phase 1 spec, sections 1, 3).
 *
 * A real {@link PathwayLifecycleService} and {@link ObjectMapper} are used
 * (both cheap, dependency-light collaborators, matching the convention
 * already established for {@code FactConflictServiceTest}) so these tests
 * exercise the genuine transition guard and JSON composition, while the
 * repository boundary stays mocked.
 */
@ExtendWith(MockitoExtension.class)
class PathwayAdminServiceTest {

    @Mock
    private PathwayRepository pathwayRepository;

    @Mock
    private RequirementRepository requirementRepository;

    private PathwayAdminService pathwayAdminService;

    @BeforeEach
    void setUp() {

        pathwayAdminService = new PathwayAdminService(
                pathwayRepository, requirementRepository, new PathwayLifecycleService(), new ObjectMapper()
        );

        org.mockito.Mockito.lenient().when(pathwayRepository.save(any(Pathway.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private PathwayCreateRequest createRequest(List<Long> requirementIds) {

        PathwayCreateRequest request = new PathwayCreateRequest();
        request.setPathwayKey("TEST_PATHWAY");
        request.setName("Test Pathway");
        request.setJurisdiction("Testland");
        request.setCategory("Skilled Worker");
        request.setRequirementIds(requirementIds);
        return request;
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    @Test
    void createBuildsAndCompositionLogicFromRequirementIdsAndStartsDraft() {

        when(requirementRepository.findById(1L)).thenReturn(Optional.of(Requirement.builder().id(1L).build()));
        when(requirementRepository.findById(2L)).thenReturn(Optional.of(Requirement.builder().id(2L).build()));

        PathwayResponse response = pathwayAdminService.create(createRequest(List.of(1L, 2L)));

        assertThat(response.status()).isEqualTo(PathwayStatus.DRAFT);

        org.mockito.ArgumentCaptor<Pathway> captor = org.mockito.ArgumentCaptor.forClass(Pathway.class);
        org.mockito.Mockito.verify(pathwayRepository).save(captor.capture());

        assertThat(captor.getValue().getCompositionLogic())
                .contains("\"node\":\"AND\"")
                .contains("\"requirementId\":1")
                .contains("\"requirementId\":2");
    }

    @Test
    void createRejectsAnUnknownRequirementId() {

        when(requirementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pathwayAdminService.create(createRequest(List.of(999L))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("999");

        org.mockito.Mockito.verify(pathwayRepository, org.mockito.Mockito.never()).save(any());
    }

    // =========================================================================
    // GET BY ID - REQUIREMENT PRE-SELECTION (edit form support)
    //
    // requirementIds is never a second, separately-maintained field on
    // Pathway - it is always derived by parsing the pathway's own
    // compositionLogic, the exact JSON create()/update() themselves
    // deterministically produce.
    // =========================================================================

    @Test
    void getByIdExtractsRequirementIdsFromAnAndOfRequirementRefs() {

        Pathway pathway = Pathway.builder()
                .id(1L).pathwayKey("TEST_PATHWAY").status(PathwayStatus.DRAFT)
                .compositionLogic(
                        "{\"node\":\"AND\",\"children\":["
                                + "{\"node\":\"REQUIREMENT_REF\",\"requirementId\":5},"
                                + "{\"node\":\"REQUIREMENT_REF\",\"requirementId\":9}"
                                + "]}"
                )
                .build();

        when(pathwayRepository.findById(1L)).thenReturn(Optional.of(pathway));

        var detail = pathwayAdminService.getById(1L);

        assertThat(detail.requirementIds()).containsExactly(5L, 9L);
    }

    @Test
    void getByIdRoundTripsExactlyWhatCreateItselfProduces() {

        when(requirementRepository.findById(3L)).thenReturn(Optional.of(Requirement.builder().id(3L).build()));
        when(requirementRepository.findById(7L)).thenReturn(Optional.of(Requirement.builder().id(7L).build()));

        PathwayResponse created = pathwayAdminService.create(createRequest(List.of(3L, 7L)));

        org.mockito.ArgumentCaptor<Pathway> captor = org.mockito.ArgumentCaptor.forClass(Pathway.class);
        org.mockito.Mockito.verify(pathwayRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

        Pathway savedPathway = captor.getAllValues().get(0);
        savedPathway.setId(created.id());

        when(pathwayRepository.findById(created.id())).thenReturn(Optional.of(savedPathway));

        var detail = pathwayAdminService.getById(created.id());

        assertThat(detail.requirementIds()).containsExactly(3L, 7L);
    }

    @Test
    void getByIdReturnsAnEmptyListForACompositionWithNoRequirementReferences() {

        Pathway pathway = Pathway.builder()
                .id(1L).pathwayKey("TEST_PATHWAY").status(PathwayStatus.DRAFT)
                .compositionLogic(
                        "{\"node\":\"FACT_PREDICATE\",\"factKey\":\"EMPLOYMENT.CURRENT_EMPLOYER\","
                                + "\"operator\":\"EXISTS\",\"operandValue\":null,\"operandValues\":null,"
                                + "\"operandLow\":null,\"operandHigh\":null}"
                )
                .build();

        when(pathwayRepository.findById(1L)).thenReturn(Optional.of(pathway));

        var detail = pathwayAdminService.getById(1L);

        assertThat(detail.requirementIds()).isEmpty();
    }

    // =========================================================================
    // UPDATE - ONLY DRAFT/REVIEW
    // =========================================================================

    @Test
    void updateRejectsAPublishedPathway() {

        Pathway published = Pathway.builder().id(1L).pathwayKey("TEST_PATHWAY").status(PathwayStatus.PUBLISHED).build();

        when(pathwayRepository.findById(1L)).thenReturn(Optional.of(published));

        PathwayUpdateRequest update = new PathwayUpdateRequest();
        update.setName("Changed");
        update.setJurisdiction("Testland");
        update.setCategory("Skilled Worker");
        update.setRequirementIds(List.of(1L));

        assertThatThrownBy(() -> pathwayAdminService.update(1L, update))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateSucceedsForADraftPathway() {

        Pathway draft = Pathway.builder().id(1L).pathwayKey("TEST_PATHWAY").status(PathwayStatus.DRAFT).build();

        when(pathwayRepository.findById(1L)).thenReturn(Optional.of(draft));
        when(requirementRepository.findById(5L)).thenReturn(Optional.of(Requirement.builder().id(5L).build()));

        PathwayUpdateRequest update = new PathwayUpdateRequest();
        update.setName("Updated Name");
        update.setJurisdiction("Testland");
        update.setCategory("Skilled Worker");
        update.setRequirementIds(List.of(5L));

        PathwayResponse response = pathwayAdminService.update(1L, update);

        assertThat(response.name()).isEqualTo("Updated Name");
    }

    // =========================================================================
    // STATUS TRANSITIONS - PREVENT INVALID PUBLICATION STATES
    // =========================================================================

    @Test
    void changeStatusRejectsDraftDirectlyToPublished() {

        Pathway draft = Pathway.builder().id(1L).pathwayKey("TEST_PATHWAY").status(PathwayStatus.DRAFT).build();

        when(pathwayRepository.findById(1L)).thenReturn(Optional.of(draft));

        PathwayStatusChangeRequest request = new PathwayStatusChangeRequest();
        request.setTargetStatus(PathwayStatus.PUBLISHED);

        assertThatThrownBy(() -> pathwayAdminService.changeStatus(1L, request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void publishingANewVersionAutoSupersedesThePreviousPublishedVersion() {

        Pathway newVersion = Pathway.builder().id(2L).pathwayKey("TEST_PATHWAY").status(PathwayStatus.REVIEW).build();
        Pathway previousPublished = Pathway.builder().id(1L).pathwayKey("TEST_PATHWAY").status(PathwayStatus.PUBLISHED).build();

        when(pathwayRepository.findById(2L)).thenReturn(Optional.of(newVersion));
        when(pathwayRepository.findByPathwayKeyAndStatus("TEST_PATHWAY", PathwayStatus.PUBLISHED))
                .thenReturn(Optional.of(previousPublished));

        PathwayStatusChangeRequest request = new PathwayStatusChangeRequest();
        request.setTargetStatus(PathwayStatus.PUBLISHED);

        PathwayResponse response = pathwayAdminService.changeStatus(2L, request);

        assertThat(response.status()).isEqualTo(PathwayStatus.PUBLISHED);
        assertThat(previousPublished.getStatus()).isEqualTo(PathwayStatus.SUPERSEDED);

        org.mockito.Mockito.verify(pathwayRepository).save(previousPublished);
    }

    @Test
    void publishingWithNoPreviousPublishedVersionDoesNotAttemptToSupersedeAnything() {

        Pathway firstVersion = Pathway.builder().id(1L).pathwayKey("TEST_PATHWAY").status(PathwayStatus.REVIEW).build();

        when(pathwayRepository.findById(1L)).thenReturn(Optional.of(firstVersion));
        when(pathwayRepository.findByPathwayKeyAndStatus("TEST_PATHWAY", PathwayStatus.PUBLISHED))
                .thenReturn(Optional.empty());

        PathwayStatusChangeRequest request = new PathwayStatusChangeRequest();
        request.setTargetStatus(PathwayStatus.PUBLISHED);

        PathwayResponse response = pathwayAdminService.changeStatus(1L, request);

        assertThat(response.status()).isEqualTo(PathwayStatus.PUBLISHED);
    }
}
