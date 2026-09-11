package com.godfrey.ai_immigration_document_analyzer.requirement;

import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayStatusChangeRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RegulatoryVersionCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RegulatoryVersionResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementAdminDetailResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementFactBindingRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementStatusChangeRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatorySourceType;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayAdminService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.RegulatoryVersionAdminService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.RequirementAdminService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * PATHWAY / REQUIREMENT ADMIN WORKFLOW INTEGRATION TEST
 * ============================================================================
 *
 * A genuine end-to-end exercise of the real admin service beans against the
 * real, configured Oracle instance (not mocks) - written for the Phase 1
 * post-implementation audit's items 7-11 (admin pathway CRUD, requirement
 * CRUD, draft -> review -> published lifecycle, unpublished-exclusion, and
 * published-pathway visibility).
 *
 * {@code @Transactional} on the test class means every write this test
 * makes is rolled back at the end of the method - this never leaves test
 * artifacts behind in the real database, exactly like the seed data
 * verification's own real-DB precedent, but safe to run repeatedly.
 * ============================================================================
 */
@SpringBootTest
@Transactional
class PathwayRequirementAdminWorkflowIntegrationTest {

    @Autowired
    private RegulatoryVersionAdminService regulatoryVersionAdminService;

    @Autowired
    private RequirementAdminService requirementAdminService;

    @Autowired
    private PathwayAdminService pathwayAdminService;

    @Autowired
    private PathwayRepository pathwayRepository;

    private RegulatoryVersionResponse createTestRegulatoryVersion(String identity) {

        RegulatoryVersionCreateRequest request = new RegulatoryVersionCreateRequest();
        request.setRegulationIdentity(identity);
        request.setJurisdiction("Testland");
        request.setSourceType(RegulatorySourceType.AUTHORITATIVE_REGULATORY_SOURCE);
        request.setSourceAuthority("Testland Immigration Authority");
        request.setSourceReference("https://example.gov/test-source");
        request.setVerificationStatus(RegulatoryVerificationStatus.UNVERIFIED_INGESTION);

        return regulatoryVersionAdminService.create(request);
    }

    private RequirementAdminDetailResponse createTestRequirement(Long regulatoryVersionId, String key) {

        RequirementFactBindingRequest binding = new RequirementFactBindingRequest();
        binding.setFactKey("EMPLOYMENT.CURRENT_EMPLOYER");
        binding.setRequiresVerification(false);

        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setRequirementKey(key);
        request.setRequirementType(RequirementType.EXPERIENCE);
        request.setTitle("Integration Test Requirement " + key);
        request.setJurisdiction("Testland");
        request.setRegulatoryVersionId(regulatoryVersionId);
        request.setMandatory(true);
        request.setSatisfactionLogicJson(
                "{\"node\":\"FACT_PREDICATE\",\"factKey\":\"EMPLOYMENT.CURRENT_EMPLOYER\",\"operator\":\"EXISTS\","
                        + "\"operandValue\":null,\"operandValues\":null,\"operandLow\":null,\"operandHigh\":null}"
        );
        request.setFactBindings(List.of(binding));

        return requirementAdminService.create(request);
    }

    // =========================================================================
    // ITEM 8 - REQUIREMENT CRUD END-TO-END
    // =========================================================================

    @Test
    void requirementCrudAndPublishingLifecycleWorkEndToEnd() {

        RegulatoryVersionResponse version = createTestRegulatoryVersion("TEST_REQ_CRUD_SOURCE");
        RequirementAdminDetailResponse created = createTestRequirement(version.id(), "TEST.REQ_CRUD_WORKFLOW");

        assertThat(created.status()).isEqualTo(RequirementStatus.DRAFT);
        assertThat(created.factBindings()).hasSize(1);

        // DRAFT -> PUBLISHED (item 9: lifecycle)
        RequirementStatusChangeRequest publish = new RequirementStatusChangeRequest();
        publish.setTargetStatus(RequirementStatus.PUBLISHED);

        var published = requirementAdminService.changeStatus(created.id(), publish);
        assertThat(published.status()).isEqualTo(RequirementStatus.PUBLISHED);

        // Invalid transition rejected: PUBLISHED cannot revert to DRAFT.
        RequirementStatusChangeRequest revert = new RequirementStatusChangeRequest();
        revert.setTargetStatus(RequirementStatus.DRAFT);

        assertThat(catchThrowable(() -> requirementAdminService.changeStatus(created.id(), revert)))
                .isInstanceOf(IllegalStateException.class);
    }

    // =========================================================================
    // ITEMS 7, 9, 10, 11 - PATHWAY CRUD, LIFECYCLE, VISIBILITY
    // =========================================================================

    @Test
    void pathwayCrudLifecycleAndVisibilityWorkEndToEnd() {

        RegulatoryVersionResponse version = createTestRegulatoryVersion("TEST_PATHWAY_CRUD_SOURCE");
        RequirementAdminDetailResponse requirement = createTestRequirement(version.id(), "TEST.PATHWAY_CRUD_REQ");

        RequirementStatusChangeRequest publishRequirement = new RequirementStatusChangeRequest();
        publishRequirement.setTargetStatus(RequirementStatus.PUBLISHED);
        requirementAdminService.changeStatus(requirement.id(), publishRequirement);

        // CREATE (item 7)
        PathwayCreateRequest createPathway = new PathwayCreateRequest();
        createPathway.setPathwayKey("TEST_PATHWAY_CRUD_KEY");
        createPathway.setName("Integration Test Pathway");
        createPathway.setJurisdiction("Testland");
        createPathway.setCategory("Test Category");
        createPathway.setRequirementIds(List.of(requirement.id()));

        PathwayResponse draft = pathwayAdminService.create(createPathway);
        assertThat(draft.status()).isEqualTo(PathwayStatus.DRAFT);

        // EDIT PRE-SELECTION FOLLOW-UP: getById (what PathwayManagementPage's
        // openEdit now calls) must return the exact requirement this
        // pathway was just created with, read back from its own
        // compositionLogic - live, against the real Oracle instance.
        var detail = pathwayAdminService.getById(draft.id());
        assertThat(detail.requirementIds()).containsExactly(requirement.id());

        // ITEM 10: a DRAFT pathway must be invisible to the exact query the
        // applicant-facing PathwayController.listPublishedPathways() uses.
        assertThat(pathwayRepository.findByStatus(PathwayStatus.PUBLISHED))
                .extracting(p -> p.getId())
                .doesNotContain(draft.id());

        // LIFECYCLE (item 9): DRAFT -> PUBLISHED directly must be rejected.
        PathwayStatusChangeRequest skipToPublished = new PathwayStatusChangeRequest();
        skipToPublished.setTargetStatus(PathwayStatus.PUBLISHED);

        assertThat(catchThrowable(() -> pathwayAdminService.changeStatus(draft.id(), skipToPublished)))
                .isInstanceOf(IllegalStateException.class);

        // DRAFT -> REVIEW -> PUBLISHED (the only valid path).
        PathwayStatusChangeRequest toReview = new PathwayStatusChangeRequest();
        toReview.setTargetStatus(PathwayStatus.REVIEW);
        pathwayAdminService.changeStatus(draft.id(), toReview);

        PathwayResponse published = pathwayAdminService.changeStatus(draft.id(), skipToPublished);
        assertThat(published.status()).isEqualTo(PathwayStatus.PUBLISHED);

        // ITEM 11: now PUBLISHED, it must appear via the exact same query
        // RequestPathwayAssessmentPage's listPathwaysApi() ultimately reads.
        assertThat(pathwayRepository.findByStatus(PathwayStatus.PUBLISHED))
                .extracting(p -> p.getId())
                .contains(draft.id());

        // UPDATE (item 7) is rejected once PUBLISHED - content is immutable.
        var updateAttempt = new com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayUpdateRequest();
        updateAttempt.setName("Changed");
        updateAttempt.setJurisdiction("Testland");
        updateAttempt.setCategory("Test Category");
        updateAttempt.setRequirementIds(List.of(requirement.id()));

        assertThat(catchThrowable(() -> pathwayAdminService.update(draft.id(), updateAttempt)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void publishingANewPathwayVersionAutoSupersedesThePreviousOneEndToEnd() {

        RegulatoryVersionResponse version = createTestRegulatoryVersion("TEST_SUPERSEDE_SOURCE");
        RequirementAdminDetailResponse requirement = createTestRequirement(version.id(), "TEST.SUPERSEDE_REQ");

        RequirementStatusChangeRequest publishRequirement = new RequirementStatusChangeRequest();
        publishRequirement.setTargetStatus(RequirementStatus.PUBLISHED);
        requirementAdminService.changeStatus(requirement.id(), publishRequirement);

        String sharedKey = "TEST_SUPERSEDE_PATHWAY_KEY";

        PathwayResponse v1 = publishNewPathwayVersion(sharedKey, requirement.id());
        assertThat(v1.status()).isEqualTo(PathwayStatus.PUBLISHED);

        PathwayResponse v2 = publishNewPathwayVersion(sharedKey, requirement.id());
        assertThat(v2.status()).isEqualTo(PathwayStatus.PUBLISHED);

        com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayAdminDetailResponse v1Reloaded =
                pathwayAdminService.getById(v1.id());
        assertThat(v1Reloaded.status()).isEqualTo(PathwayStatus.SUPERSEDED);

        // Exactly one PUBLISHED row for this key at a time.
        assertThat(pathwayRepository.findByPathwayKeyAndStatus(sharedKey, PathwayStatus.PUBLISHED))
                .map(p -> p.getId())
                .hasValue(v2.id());
    }

    private PathwayResponse publishNewPathwayVersion(String pathwayKey, Long requirementId) {

        PathwayCreateRequest createPathway = new PathwayCreateRequest();
        createPathway.setPathwayKey(pathwayKey);
        createPathway.setName("Integration Test Pathway");
        createPathway.setJurisdiction("Testland");
        createPathway.setCategory("Test Category");
        createPathway.setRequirementIds(List.of(requirementId));

        PathwayResponse draft = pathwayAdminService.create(createPathway);

        PathwayStatusChangeRequest toReview = new PathwayStatusChangeRequest();
        toReview.setTargetStatus(PathwayStatus.REVIEW);
        pathwayAdminService.changeStatus(draft.id(), toReview);

        PathwayStatusChangeRequest toPublished = new PathwayStatusChangeRequest();
        toPublished.setTargetStatus(PathwayStatus.PUBLISHED);
        return pathwayAdminService.changeStatus(draft.id(), toPublished);
    }

    private static Throwable catchThrowable(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        return org.assertj.core.api.Assertions.catchThrowable(callable);
    }
}
