package com.godfrey.ai_immigration_document_analyzer.requirement;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayDiscoveryResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayStatusChangeRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RegulatoryVersionCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RegulatoryVersionResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementAdminDetailResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementCreateRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementFactBindingRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementStatusChangeRequest;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatorySourceType;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayAdminService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayAssessmentService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayDiscoveryService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.RegulatoryVersionAdminService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.RequirementAdminService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ============================================================================
 * PATHWAY DISCOVERY WORKFLOW INTEGRATION TEST
 * ============================================================================
 *
 * A genuine end-to-end exercise of Phase 4 - Pathway Discovery &amp;
 * Explainable Ranking - against the real, configured Oracle instance, not
 * mocks. Mirrors {@code CaseIntelligenceWorkflowIntegrationTest}'s pattern:
 * build a real published Pathway/Requirement via the existing admin
 * services, then verify {@code PathwayDiscoveryService} ranks it correctly
 * WITHOUT writing any PathwayAssessment/RequirementEvaluation row - the one
 * property no mocked unit test can fully prove.
 *
 * {@code @Transactional} rolls back every write at the end of each method -
 * no test artifacts are left in the real database.
 * ============================================================================
 */
@SpringBootTest
@Transactional
class PathwayDiscoveryWorkflowIntegrationTest {

    @Autowired
    private RegulatoryVersionAdminService regulatoryVersionAdminService;

    @Autowired
    private RequirementAdminService requirementAdminService;

    @Autowired
    private PathwayAdminService pathwayAdminService;

    @Autowired
    private PathwayAssessmentService pathwayAssessmentService;

    @Autowired
    private PathwayDiscoveryService pathwayDiscoveryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FactRepository factRepository;

    @Autowired
    private PathwayAssessmentRepository pathwayAssessmentRepository;

    @Autowired
    private RequirementEvaluationRepository requirementEvaluationRepository;

    private static final String FACT_KEY = "EMPLOYMENT.CURRENT_EMPLOYER";

    private AuthenticatedUser actorFor(Long userId) {
        return new AuthenticatedUser(
                userId, "pathway-discovery-test-" + userId + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())),
                true, true, true, true
        );
    }

    private Long createTestSubject() {

        User saved = userRepository.save(User.builder()
                .fullName("Pathway Discovery Test Subject")
                .email("pathway-discovery-subject-" + System.nanoTime() + "@example.com")
                .password("irrelevant-hash")
                .role(Role.USER)
                .build());

        return saved.getId();
    }

    private RegulatoryVersionResponse createTestRegulatoryVersion(String identity, RegulatoryVerificationStatus status) {

        RegulatoryVersionCreateRequest request = new RegulatoryVersionCreateRequest();
        request.setRegulationIdentity(identity);
        request.setJurisdiction("Testland");
        request.setSourceType(RegulatorySourceType.AUTHORITATIVE_REGULATORY_SOURCE);
        request.setSourceAuthority("Testland Immigration Authority");
        request.setSourceReference("https://example.gov/test-source");
        request.setVerificationStatus(status);

        return regulatoryVersionAdminService.create(request);
    }

    private RequirementAdminDetailResponse createAndPublishTestRequirement(Long regulatoryVersionId, String key) {

        RequirementFactBindingRequest binding = new RequirementFactBindingRequest();
        binding.setFactKey(FACT_KEY);
        binding.setRequiresVerification(false);

        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setRequirementKey(key);
        request.setRequirementType(RequirementType.EXPERIENCE);
        request.setTitle("Pathway Discovery Test Requirement " + key);
        request.setJurisdiction("Testland");
        request.setRegulatoryVersionId(regulatoryVersionId);
        request.setMandatory(true);
        request.setSatisfactionLogicJson(
                "{\"node\":\"FACT_PREDICATE\",\"factKey\":\"" + FACT_KEY + "\",\"operator\":\"EXISTS\","
                        + "\"operandValue\":null,\"operandValues\":null,\"operandLow\":null,\"operandHigh\":null}"
        );
        request.setFactBindings(List.of(binding));

        RequirementAdminDetailResponse created = requirementAdminService.create(request);

        RequirementStatusChangeRequest publish = new RequirementStatusChangeRequest();
        publish.setTargetStatus(RequirementStatus.PUBLISHED);
        requirementAdminService.changeStatus(created.id(), publish);

        return created;
    }

    private PathwayResponse createPathway(String pathwayKey, Long requirementId, boolean publish) {

        PathwayCreateRequest createPathway = new PathwayCreateRequest();
        createPathway.setPathwayKey(pathwayKey);
        createPathway.setName("Pathway Discovery Test Pathway " + pathwayKey);
        createPathway.setJurisdiction("Testland");
        createPathway.setCategory("Test Category");
        createPathway.setRequirementIds(List.of(requirementId));

        PathwayResponse draft = pathwayAdminService.create(createPathway);

        if (!publish) {
            return draft;
        }

        PathwayStatusChangeRequest toReview = new PathwayStatusChangeRequest();
        toReview.setTargetStatus(PathwayStatus.REVIEW);
        pathwayAdminService.changeStatus(draft.id(), toReview);

        PathwayStatusChangeRequest toPublished = new PathwayStatusChangeRequest();
        toPublished.setTargetStatus(PathwayStatus.PUBLISHED);
        return pathwayAdminService.changeStatus(draft.id(), toPublished);
    }

    private void acceptFact(Long subjectUserId, String factKey) {

        factRepository.save(Fact.builder()
                .subjectUserId(subjectUserId)
                .category(FactCategory.EMPLOYMENT)
                .factKey(factKey)
                .valueType(FactValueType.STRING)
                .stringValue("Acme Corp")
                .status(FactStatus.ACCEPTED)
                .provenanceType(FactProvenanceType.USER_INPUT)
                .createdByAccessorType(AccessorType.SUBJECT)
                .createdByUserId(subjectUserId)
                .sensitivityTier(FactSensitivityTier.T2_STANDARD_PERSONAL)
                .confidenceScore(0.9)
                .confidenceLevel(FactConfidenceLevel.HIGH)
                .effectiveFrom(LocalDateTime.now().minusYears(1))
                .observedAt(LocalDateTime.now())
                .lastObservedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void discoveryRanksThePublishedPathwayCorrectlyBeforeAndAfterEvidenceWithoutEverPersistingAnAssessment() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        RegulatoryVersionResponse version =
                createTestRegulatoryVersion("TEST_DISCOVERY_SOURCE_" + System.nanoTime(), RegulatoryVerificationStatus.UNVERIFIED_INGESTION);
        RequirementAdminDetailResponse requirement =
                createAndPublishTestRequirement(version.id(), "TEST.DISCOVERY_REQ_" + System.nanoTime());

        PathwayResponse publishedPathway =
                createPathway("TEST_DISCOVERY_PUBLISHED_" + System.nanoTime(), requirement.id(), true);
        PathwayResponse draftPathway =
                createPathway("TEST_DISCOVERY_DRAFT_" + System.nanoTime(), requirement.id(), false);

        long assessmentCountBefore = pathwayAssessmentRepository.count();
        long evaluationCountBefore = requirementEvaluationRepository.count();

        // ---------------------------------------------------------------
        // BEFORE EVIDENCE: no Fact recorded yet.
        // ---------------------------------------------------------------
        PathwayDiscoveryResponse before = pathwayDiscoveryService.discover(actor, subjectUserId);

        assertThat(before.subjectUserId()).isEqualTo(subjectUserId);
        assertThat(before.rankedPathways())
                .extracting(row -> row.pathwayKey())
                .contains(publishedPathway.pathwayKey())
                .doesNotContain(draftPathway.pathwayKey()); // DRAFT pathway never discoverable

        var beforeRow = before.rankedPathways().stream()
                .filter(row -> row.pathwayKey().equals(publishedPathway.pathwayKey()))
                .findFirst().orElseThrow();

        assertThat(beforeRow.outcome()).isEqualTo(RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE);
        assertThat(beforeRow.missingRequirementCount()).isEqualTo(1);
        assertThat(beforeRow.topMissingRequirements()).hasSize(1);
        assertThat(beforeRow.topMissingRequirements().get(0).supportStatus()).isEqualTo(CaseRequirementSupportStatus.MISSING);
        assertThat(beforeRow.topMissingRequirements().get(0).missingFactKeys()).containsExactly(FACT_KEY);
        assertThat(beforeRow.regulatoryCertainty()).isEqualTo(RegulatoryVerificationStatus.UNVERIFIED_INGESTION);

        // ---------------------------------------------------------------
        // NO PERSISTENCE POLLUTION: calling Discovery wrote nothing.
        // ---------------------------------------------------------------
        assertThat(pathwayAssessmentRepository.count()).isEqualTo(assessmentCountBefore);
        assertThat(requirementEvaluationRepository.count()).isEqualTo(evaluationCountBefore);

        // ---------------------------------------------------------------
        // AFTER EVIDENCE: record the Fact, discover again.
        // ---------------------------------------------------------------
        acceptFact(subjectUserId, FACT_KEY);

        PathwayDiscoveryResponse after = pathwayDiscoveryService.discover(actor, subjectUserId);

        var afterRow = after.rankedPathways().stream()
                .filter(row -> row.pathwayKey().equals(publishedPathway.pathwayKey()))
                .findFirst().orElseThrow();

        assertThat(afterRow.outcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(afterRow.overallAlignmentScore()).isEqualTo(100.0);
        assertThat(afterRow.topMissingRequirements()).isEmpty();

        // Still zero persisted rows from either Discovery call.
        assertThat(pathwayAssessmentRepository.count()).isEqualTo(assessmentCountBefore);
        assertThat(requirementEvaluationRepository.count()).isEqualTo(evaluationCountBefore);

        // ---------------------------------------------------------------
        // REGRESSION: the EXISTING formal assessment flow still persists
        // exactly one PathwayAssessment when explicitly requested.
        // ---------------------------------------------------------------
        var formalAssessment = pathwayAssessmentService.assess(actor, publishedPathway.id(), subjectUserId, null);

        assertThat(formalAssessment.outcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(pathwayAssessmentRepository.count()).isEqualTo(assessmentCountBefore + 1);
        assertThat(requirementEvaluationRepository.count()).isEqualTo(evaluationCountBefore + 1);
    }

    @Test
    void discoveryDeniesAStrangerBeforeReadingAnyPathway() {

        Long subjectUserId = createTestSubject();
        Long strangerUserId = createTestSubject(); // a real, unrelated, unassigned user

        AuthenticatedUser stranger = actorFor(strangerUserId);

        assertThatThrownBy(() -> pathwayDiscoveryService.discover(stranger, subjectUserId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
