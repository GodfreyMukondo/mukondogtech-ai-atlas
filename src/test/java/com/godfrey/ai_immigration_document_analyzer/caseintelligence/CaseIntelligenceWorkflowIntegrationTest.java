package com.godfrey.ai_immigration_document_analyzer.caseintelligence;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIntelligenceResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRiskBand;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.service.CaseIntelligenceService;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.service.CaseOverviewService;
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
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayAdminService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.PathwayAssessmentService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.RegulatoryVersionAdminService;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.RequirementAdminService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * CASE INTELLIGENCE WORKFLOW INTEGRATION TEST
 * ============================================================================
 *
 * A genuine end-to-end exercise of the Phase 2 Case Intelligence / Discovery
 * / Readiness foundation against the real, configured Oracle instance - not
 * mocks. Mirrors {@code PathwayRequirementAdminWorkflowIntegrationTest}'s
 * pattern: build a real published Pathway/Requirement, request a real
 * PathwayAssessment through the existing Requirement/Pathway engine, then
 * verify the Phase 2 services correctly reshape - never re-derive - that
 * same evaluation.
 *
 * {@code @Transactional} rolls back every write at the end of each method -
 * no test artifacts are left in the real database.
 * ============================================================================
 */
@SpringBootTest
@Transactional
class CaseIntelligenceWorkflowIntegrationTest {

    @Autowired
    private RegulatoryVersionAdminService regulatoryVersionAdminService;

    @Autowired
    private RequirementAdminService requirementAdminService;

    @Autowired
    private PathwayAdminService pathwayAdminService;

    @Autowired
    private PathwayAssessmentService pathwayAssessmentService;

    @Autowired
    private CaseIntelligenceService caseIntelligenceService;

    @Autowired
    private CaseOverviewService caseOverviewService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FactRepository factRepository;

    private static final String FACT_KEY = "EMPLOYMENT.CURRENT_EMPLOYER";

    private AuthenticatedUser actorFor(Long userId) {
        return new AuthenticatedUser(
                userId, "case-intelligence-test-" + userId + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())),
                true, true, true, true
        );
    }

    private Long createTestSubject() {

        User saved = userRepository.save(User.builder()
                .fullName("Case Intelligence Test Subject")
                .email("case-intelligence-subject-" + System.nanoTime() + "@example.com")
                .password("irrelevant-hash")
                .role(Role.USER)
                .build());

        return saved.getId();
    }

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

    private RequirementAdminDetailResponse createAndPublishTestRequirement(Long regulatoryVersionId, String key) {

        RequirementFactBindingRequest binding = new RequirementFactBindingRequest();
        binding.setFactKey(FACT_KEY);
        binding.setRequiresVerification(false);

        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setRequirementKey(key);
        request.setRequirementType(RequirementType.EXPERIENCE);
        request.setTitle("Case Intelligence Test Requirement " + key);
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

    private PathwayResponse createAndPublishTestPathway(String pathwayKey, Long requirementId) {

        PathwayCreateRequest createPathway = new PathwayCreateRequest();
        createPathway.setPathwayKey(pathwayKey);
        createPathway.setName("Case Intelligence Test Pathway");
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

    // =========================================================================
    // CASE INTELLIGENCE: READINESS + MATRIX + MISSING EVIDENCE
    // =========================================================================

    @Test
    void caseIntelligenceReflectsInsufficientEvidenceThenSatisfiedAfterTheFactIsRecordedEndToEnd() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        RegulatoryVersionResponse version = createTestRegulatoryVersion("TEST_CASE_INTEL_SOURCE_" + System.nanoTime());
        RequirementAdminDetailResponse requirement =
                createAndPublishTestRequirement(version.id(), "TEST.CASE_INTEL_REQ_" + System.nanoTime());
        PathwayResponse pathway = createAndPublishTestPathway("TEST_CASE_INTEL_PATHWAY_" + System.nanoTime(), requirement.id());

        // No Fact recorded yet - the requirement must resolve to
        // INSUFFICIENT_EVIDENCE, never NOT_SATISFIED.
        var firstAssessment = pathwayAssessmentService.assess(actor, pathway.id(), subjectUserId, null);

        CaseIntelligenceResponse beforeEvidence = caseIntelligenceService.getCaseIntelligence(actor, firstAssessment.id());

        assertThat(beforeEvidence.evidenceMatrix()).hasSize(1);
        assertThat(beforeEvidence.evidenceMatrix().get(0).rawOutcome())
                .isEqualTo(RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE);
        assertThat(beforeEvidence.evidenceMatrix().get(0).supportStatus())
                .isEqualTo(com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus.MISSING);
        assertThat(beforeEvidence.readiness().requirementCoveragePercent()).isEqualTo(0.0);

        assertThat(beforeEvidence.missingEvidence()).hasSize(1);
        assertThat(beforeEvidence.missingEvidence().get(0).missingFactKeys()).containsExactly(FACT_KEY);
        assertThat(beforeEvidence.missingEvidence().get(0).necessity())
                .isEqualTo(com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.EvidenceNecessity.REQUIRED);
        assertThat(beforeEvidence.readiness().outstandingIssues()).hasSize(1);
        assertThat(beforeEvidence.overview().missingEvidenceCount()).isEqualTo(1);
        assertThat(beforeEvidence.subjectUserId()).isEqualTo(subjectUserId);

        // Record the Fact the requirement is bound to, then re-assess.
        acceptFact(subjectUserId, FACT_KEY);

        var secondAssessment = pathwayAssessmentService.assess(actor, pathway.id(), subjectUserId, null);

        CaseIntelligenceResponse afterEvidence = caseIntelligenceService.getCaseIntelligence(actor, secondAssessment.id());

        assertThat(afterEvidence.evidenceMatrix().get(0).rawOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(afterEvidence.evidenceMatrix().get(0).supportStatus())
                .isEqualTo(com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus.SATISFIED);
        assertThat(afterEvidence.readiness().requirementCoveragePercent()).isEqualTo(100.0);
        assertThat(afterEvidence.readiness().overallReadinessPercent()).isEqualTo(100.0);
        assertThat(afterEvidence.missingEvidence()).isEmpty();
        assertThat(afterEvidence.readiness().outstandingIssues()).isEmpty();
        assertThat(afterEvidence.overview().overallReadinessPercent()).isEqualTo(100.0);
    }

    // =========================================================================
    // CASE OVERVIEW: TIMELINE + CONTRADICTIONS + SIGNALS
    // =========================================================================

    @Test
    void caseOverviewSurfacesTheRecordedFactOnTheTimelineAndReportsNoSignalsEndToEnd() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        acceptFact(subjectUserId, FACT_KEY);

        var timeline = caseOverviewService.getTimeline(actor, subjectUserId);
        assertThat(timeline.events()).extracting(event -> event.fact().factKey()).contains(FACT_KEY);

        var contradictions = caseOverviewService.getContradictions(actor, subjectUserId);
        assertThat(contradictions.openContradictions()).isEmpty();
        assertThat(contradictions.potentialOverlaps()).isEmpty();

        var signals = caseOverviewService.getSignals(actor, subjectUserId);
        assertThat(signals.riskBand()).isEqualTo(CaseRiskBand.NORMAL);
        assertThat(signals.fraudFlaggedDocumentCount()).isZero();
        assertThat(signals.potentialOverlapContradictionCount()).isZero();
    }
}
