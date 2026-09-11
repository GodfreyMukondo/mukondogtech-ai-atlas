package com.godfrey.ai_immigration_document_analyzer.agent;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainPathwayAssessmentRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.PathwayAssessmentAgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AiExplanationStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.repository.AgentRunRepository;
import com.godfrey.ai_immigration_document_analyzer.agent.service.ExplainPathwayAssessmentAgentService;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.EvidenceSourceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactEvidence;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
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
import com.godfrey.ai_immigration_document_analyzer.service.LlmService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ============================================================================
 * AGENT EXPLAIN PATHWAY ASSESSMENT WORKFLOW INTEGRATION TEST
 * ============================================================================
 *
 * A genuine end-to-end exercise of the Phase 5.2 EXPLAIN_PATHWAY_ASSESSMENT
 * agent against the real, configured Oracle instance - not mocks. Mirrors
 * {@code AgentExplainRequirementWorkflowIntegrationTest}'s pattern: build a
 * real published Requirement/Pathway via the existing admin services, run a
 * real PathwayAssessment, then drive the agent through its real tool
 * ({@code PathwayIntelligenceContextTool} -&gt; {@code CaseIntelligenceService})
 * against that real data.
 *
 * The ONLY mocked collaborator is {@link LlmService}.
 *
 * {@code @Transactional} rolls back every write at the end of each method -
 * no test artifacts are left in the real database.
 * ============================================================================
 */
@SpringBootTest
@Transactional
class AgentExplainPathwayAssessmentWorkflowIntegrationTest {

    @MockBean
    private LlmService llmService;

    @Autowired
    private ExplainPathwayAssessmentAgentService explainPathwayAssessmentAgentService;

    @Autowired
    private RegulatoryVersionAdminService regulatoryVersionAdminService;

    @Autowired
    private RequirementAdminService requirementAdminService;

    @Autowired
    private PathwayAdminService pathwayAdminService;

    @Autowired
    private PathwayAssessmentService pathwayAssessmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FactRepository factRepository;

    @Autowired
    private FactEvidenceRepository factEvidenceRepository;

    @Autowired
    private AgentRunRepository agentRunRepository;

    private static final String FACT_KEY = "EMPLOYMENT.CURRENT_EMPLOYER";

    private AuthenticatedUser actorFor(Long userId) {
        return new AuthenticatedUser(
                userId, "pathway-agent-workflow-test-" + userId + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())),
                true, true, true, true
        );
    }

    private Long createTestSubject() {
        User saved = userRepository.save(User.builder()
                .fullName("Pathway Agent Workflow Test Subject")
                .email("pathway-agent-workflow-subject-" + System.nanoTime() + "@example.com")
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
        request.setTitle("Pathway Agent Workflow Test Requirement " + key);
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
        createPathway.setName("Pathway Agent Workflow Test Pathway");
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

    private void acceptFact(Long subjectUserId) {
        Fact fact = factRepository.save(Fact.builder()
                .subjectUserId(subjectUserId)
                .category(FactCategory.EMPLOYMENT)
                .factKey(FACT_KEY)
                .valueType(FactValueType.STRING)
                .stringValue("Acme Corp")
                .status(FactStatus.ACCEPTED)
                .provenanceType(FactProvenanceType.DOCUMENT_EXTRACTION)
                .createdByAccessorType(AccessorType.SYSTEM)
                .sensitivityTier(FactSensitivityTier.T2_STANDARD_PERSONAL)
                .confidenceScore(0.9)
                .confidenceLevel(FactConfidenceLevel.HIGH)
                .effectiveFrom(LocalDateTime.now().minusYears(1))
                .observedAt(LocalDateTime.now())
                .lastObservedAt(LocalDateTime.now())
                .build());

        factEvidenceRepository.save(FactEvidence.builder()
                .factId(fact.getId())
                .sourceType(EvidenceSourceType.DOCUMENT)
                .sourceSnippet("Employed at Acme Corp since 2020.")
                .build());
    }

    // =========================================================================
    // GROUNDED, SUCCESSFUL PATHWAY NARRATIVE AGAINST REAL DATA
    // =========================================================================

    @Test
    void explainPathwayAssessmentProducesAGroundedAuditedResultForARealPersistedAssessment() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        RegulatoryVersionResponse version =
                createTestRegulatoryVersion("TEST_PATHWAY_AGENT_WORKFLOW_SOURCE_" + System.nanoTime());
        RequirementAdminDetailResponse requirement =
                createAndPublishTestRequirement(version.id(), "TEST.PATHWAY_AGENT_WORKFLOW_REQ_" + System.nanoTime());
        PathwayResponse pathway =
                createAndPublishTestPathway("TEST_PATHWAY_AGENT_WORKFLOW_PATHWAY_" + System.nanoTime(), requirement.id());

        acceptFact(subjectUserId);

        var assessment = pathwayAssessmentService.assess(actor, pathway.id(), subjectUserId, null);
        assertThat(assessment.requirementEvaluations()).hasSize(1);
        assertThat(assessment.outcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);

        when(llmService.ask(anyString())).thenReturn(
                "{\"explanation\":\"This pathway is satisfied - its one requirement is backed by a recorded employment fact.\","
                        + "\"recommendedNextStep\":\"No further action needed.\"}"
        );

        ExplainPathwayAssessmentRequest request = new ExplainPathwayAssessmentRequest();
        request.setPathwayAssessmentId(assessment.id());

        PathwayAssessmentAgentRunResponse response = explainPathwayAssessmentAgentService.explainPathwayAssessment(actor, request);

        // ---- Structured, grounded result over REAL persisted data ----
        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.result().overallOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(response.result().requirementSummaries()).hasSize(1);
        assertThat(response.result().requirementSummaries().get(0).supportStatus())
                .isEqualTo(CaseRequirementSupportStatus.SATISFIED);
        assertThat(response.result().explanation())
                .isEqualTo("This pathway is satisfied - its one requirement is backed by a recorded employment fact.");
        assertThat(response.toolsInvoked()).containsExactly("GET_PATHWAY_INTELLIGENCE_CONTEXT");

        // Provenance preserved down to the requirement/regulatory-version level.
        assertThat(response.result().requirementSummaries().get(0).regulatoryVersionId()).isEqualTo(version.id());
        assertThat(response.result().requirementSummaries().get(0).regulatorySourceAuthority())
                .isEqualTo("Testland Immigration Authority");

        // ---- The run was genuinely persisted (real row, real database) ----
        AgentRun persisted = agentRunRepository.findById(response.id()).orElseThrow();
        assertThat(persisted.getSubjectUserId()).isEqualTo(subjectUserId);
        assertThat(persisted.getRequestedByUserId()).isEqualTo(subjectUserId);
        assertThat(persisted.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(persisted.getRequirementId()).isNull(); // pathway-level run, not tied to one requirement
        assertThat(persisted.getResultJson()).isNotBlank();
        assertThat(persisted.getCompletedAt()).isNotNull();

        // ---- The audit run can be re-fetched without re-executing the agent ----
        PathwayAssessmentAgentRunResponse refetched = explainPathwayAssessmentAgentService.getRun(actor, response.id());
        assertThat(refetched.result().explanation()).isEqualTo(response.result().explanation());

        // ---- A stranger may not view this run or re-run it for this subject's pathway ----
        AuthenticatedUser stranger = actorFor(createTestSubject());

        assertThatThrownBy(() -> explainPathwayAssessmentAgentService.getRun(stranger, response.id()))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> explainPathwayAssessmentAgentService.explainPathwayAssessment(stranger, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    // =========================================================================
    // A REQUIREMENT-LEVEL INSUFFICIENT-EVIDENCE OUTCOME IS STILL A GROUNDED
    // PATHWAY-LEVEL NARRATIVE - never confused with "nothing to explain"
    // =========================================================================

    @Test
    void explainPathwayAssessmentIsStillGroundedWhenItsOnlyRequirementHasInsufficientEvidence() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        RegulatoryVersionResponse version =
                createTestRegulatoryVersion("TEST_PATHWAY_AGENT_WORKFLOW_EMPTY_SOURCE_" + System.nanoTime());
        RequirementAdminDetailResponse requirement =
                createAndPublishTestRequirement(version.id(), "TEST.PATHWAY_AGENT_WORKFLOW_EMPTY_REQ_" + System.nanoTime());
        PathwayResponse pathway =
                createAndPublishTestPathway("TEST_PATHWAY_AGENT_WORKFLOW_EMPTY_PATHWAY_" + System.nanoTime(), requirement.id());

        // No Fact recorded - the one requirement resolves to INSUFFICIENT_EVIDENCE,
        // but the pathway assessment ITSELF still produced a real evaluation row.
        var assessment = pathwayAssessmentService.assess(actor, pathway.id(), subjectUserId, null);
        assertThat(assessment.requirementEvaluations().get(0).outcome())
                .isEqualTo(RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE);

        when(llmService.ask(anyString())).thenReturn(
                "{\"explanation\":\"This pathway currently has one requirement with insufficient evidence.\","
                        + "\"recommendedNextStep\":\"Provide evidence of current employment.\"}"
        );

        ExplainPathwayAssessmentRequest request = new ExplainPathwayAssessmentRequest();
        request.setPathwayAssessmentId(assessment.id());

        PathwayAssessmentAgentRunResponse response = explainPathwayAssessmentAgentService.explainPathwayAssessment(actor, request);

        // The whole-pathway agent must treat this as GROUNDED - the matrix has
        // a real row - and MUST invoke the LLM to narrate it honestly, never
        // silently withholding an explanation just because one requirement is
        // unmet.
        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.result().requirementSummaries()).hasSize(1);
        assertThat(response.result().requirementSummaries().get(0).supportStatus())
                .isEqualTo(CaseRequirementSupportStatus.MISSING);
        assertThat(response.result().missingEvidence()).hasSize(1);
        assertThat(response.result().explanation())
                .isEqualTo("This pathway currently has one requirement with insufficient evidence.");

        verify(llmService, times(1)).ask(anyString());

        AgentRun persisted = agentRunRepository.findById(response.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(persisted.getGroundingState()).isEqualTo(AgentGroundingState.GROUNDED);
    }

    // =========================================================================
    // GRACEFUL AI DEGRADATION AGAINST REAL DATA - Phase 5.2 Production
    // Hardening, Test 8: the complete deterministic pathway assessment and
    // AgentRun round trip must succeed even when the AI provider fails
    // (mocked here exactly like OpenAI returning HTTP 429/insufficient_quota
    // would) - no live OpenAI credentials are required for this test.
    // =========================================================================

    @Test
    void explainPathwayAssessmentReturnsTheFullDeterministicAssessmentWhenTheLlmIsUnavailable() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        RegulatoryVersionResponse version =
                createTestRegulatoryVersion("TEST_PATHWAY_AGENT_WORKFLOW_DEGRADED_SOURCE_" + System.nanoTime());
        RequirementAdminDetailResponse requirement =
                createAndPublishTestRequirement(version.id(), "TEST.PATHWAY_AGENT_WORKFLOW_DEGRADED_REQ_" + System.nanoTime());
        PathwayResponse pathway =
                createAndPublishTestPathway("TEST_PATHWAY_AGENT_WORKFLOW_DEGRADED_PATHWAY_" + System.nanoTime(), requirement.id());

        acceptFact(subjectUserId);

        var assessment = pathwayAssessmentService.assess(actor, pathway.id(), subjectUserId, null);
        assertThat(assessment.outcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);

        // Simulates OpenAI returning HTTP 429 / insufficient_quota /
        // credit_balance_exhausted - LlmService wraps any such provider
        // failure into a generic RuntimeException (see LlmService.ask()).
        when(llmService.ask(anyString())).thenThrow(new RuntimeException(
                "429 Too Many Requests - insufficient_quota: credit_balance_exhausted"
        ));

        ExplainPathwayAssessmentRequest request = new ExplainPathwayAssessmentRequest();
        request.setPathwayAssessmentId(assessment.id());

        PathwayAssessmentAgentRunResponse response = explainPathwayAssessmentAgentService.explainPathwayAssessment(actor, request);

        // ---- The run still completes successfully - never a 5xx/exception ----
        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.errorMessage()).isNull();

        // ---- The full deterministic assessment is present and unaltered ----
        assertThat(response.result().overallOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(response.result().groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.result().requirementSummaries()).hasSize(1);
        assertThat(response.result().requirementSummaries().get(0).supportStatus())
                .isEqualTo(CaseRequirementSupportStatus.SATISFIED);
        assertThat(response.result().requirementSummaries().get(0).regulatoryVersionId()).isEqualTo(version.id());

        // ---- The AI narrative is honestly absent - never fabricated ----
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.UNAVAILABLE);
        assertThat(response.result().explanation()).isNull();

        // ---- The AgentRun round trip persisted correctly ----
        AgentRun persisted = agentRunRepository.findById(response.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(persisted.getSubjectUserId()).isEqualTo(subjectUserId);
        assertThat(persisted.getErrorMessage()).isNull();
        assertThat(persisted.getResultJson()).doesNotContain("insufficient_quota");

        // ---- Re-fetching the run returns the same graceful-degradation result ----
        PathwayAssessmentAgentRunResponse refetched = explainPathwayAssessmentAgentService.getRun(actor, response.id());
        assertThat(refetched.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.UNAVAILABLE);
        assertThat(refetched.result().overallOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
    }
}
