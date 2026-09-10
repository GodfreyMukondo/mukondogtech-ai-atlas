package com.godfrey.ai_immigration_document_analyzer.agent;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.AgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainRequirementRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.repository.AgentRunRepository;
import com.godfrey.ai_immigration_document_analyzer.agent.service.ExplainRequirementAgentService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ============================================================================
 * AGENT EXPLAIN REQUIREMENT WORKFLOW INTEGRATION TEST
 * ============================================================================
 *
 * A genuine end-to-end exercise of the Phase 5.1 EXPLAIN_REQUIREMENT agent
 * against the real, configured Oracle instance - not mocks. Mirrors
 * {@code EvidenceGraphPhase3WorkflowIntegrationTest}'s pattern: build a real
 * published Requirement/Pathway via the existing admin services, record a
 * real Fact/FactEvidence chain, run a real PathwayAssessment, then drive the
 * agent through its real tools ({@code RequirementExplanationTool} ->
 * {@code CaseIntelligenceService}/{@code ExplainabilityService},
 * {@code EvidenceGraphContextTool} -> {@code EvidenceGraphService}) against
 * that real data.
 *
 * The ONLY mocked collaborator is {@link LlmService} - the one genuinely
 * external boundary this test environment has no control over. Every other
 * piece of the agent (tool authorization, requirement/fact/evidence
 * retrieval, grounding decision, AgentRun persistence) is the real,
 * unmodified production wiring, running against real persisted rows.
 *
 * {@code @Transactional} rolls back every write at the end of each method -
 * no test artifacts are left in the real database.
 * ============================================================================
 */
@SpringBootTest
@Transactional
class AgentExplainRequirementWorkflowIntegrationTest {

    @MockBean
    private LlmService llmService;

    @Autowired
    private ExplainRequirementAgentService explainRequirementAgentService;

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
                userId, "agent-workflow-test-" + userId + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())),
                true, true, true, true
        );
    }

    private Long createTestSubject() {
        User saved = userRepository.save(User.builder()
                .fullName("Agent Workflow Test Subject")
                .email("agent-workflow-subject-" + System.nanoTime() + "@example.com")
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
        request.setTitle("Agent Workflow Test Requirement " + key);
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
        createPathway.setName("Agent Workflow Test Pathway");
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
    // GROUNDED, SUCCESSFUL EXPLANATION AGAINST REAL DATA
    // =========================================================================

    @Test
    void explainRequirementProducesAGroundedAuditedResultForARealPersistedRequirement() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        RegulatoryVersionResponse version =
                createTestRegulatoryVersion("TEST_AGENT_WORKFLOW_SOURCE_" + System.nanoTime());
        RequirementAdminDetailResponse requirement =
                createAndPublishTestRequirement(version.id(), "TEST.AGENT_WORKFLOW_REQ_" + System.nanoTime());
        PathwayResponse pathway =
                createAndPublishTestPathway("TEST_AGENT_WORKFLOW_PATHWAY_" + System.nanoTime(), requirement.id());

        acceptFact(subjectUserId);

        var assessment = pathwayAssessmentService.assess(actor, pathway.id(), subjectUserId, null);
        assertThat(assessment.requirementEvaluations()).hasSize(1);

        when(llmService.ask(anyString())).thenReturn(
                "{\"explanation\":\"This requirement is satisfied by a recorded employment fact.\","
                        + "\"recommendedNextStep\":\"No further action needed.\"}"
        );

        ExplainRequirementRequest request = new ExplainRequirementRequest();
        request.setPathwayAssessmentId(assessment.id());
        request.setRequirementId(requirement.id());

        AgentRunResponse response = explainRequirementAgentService.explainRequirement(actor, request);

        // ---- Structured, grounded result over REAL persisted data ----
        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.result().currentStatus()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(response.result().factsConsidered()).extracting("factKey").contains(FACT_KEY);
        assertThat(response.result().explanation()).isEqualTo("This requirement is satisfied by a recorded employment fact.");
        assertThat(response.result().provenance()).isNotNull();
        assertThat(response.result().provenance().nodes()).isNotEmpty();
        assertThat(response.toolsInvoked()).contains("GET_REQUIREMENT_EXPLANATION", "GET_EVIDENCE_GRAPH_CONTEXT");

        // Fix 1: the application's own existing explanation is preserved,
        // distinct from the (mocked) LLM's explanation, against real data.
        assertThat(response.result().existingExplanation()).isNotNull();
        assertThat(response.result().existingExplanation()).isNotEqualTo(response.result().explanation());

        // Fix 2: regulatory context reflects the REAL regulatory version
        // actually selected by the application for this requirement.
        assertThat(response.result().regulatoryVersionId()).isEqualTo(version.id());
        assertThat(response.result().regulatorySourceAuthority()).isEqualTo("Testland Immigration Authority");
        assertThat(response.result().regulatoryVerificationStatus())
                .isEqualTo(com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus.UNVERIFIED_INGESTION);

        // ---- The run was genuinely persisted (real row, real database) ----
        AgentRun persisted = agentRunRepository.findById(response.id()).orElseThrow();
        assertThat(persisted.getSubjectUserId()).isEqualTo(subjectUserId);
        assertThat(persisted.getRequestedByUserId()).isEqualTo(subjectUserId);
        assertThat(persisted.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(persisted.getResultJson()).isNotBlank();
        assertThat(persisted.getPlanSummary()).isNotBlank();
        assertThat(persisted.getCompletedAt()).isNotNull();

        // ---- The audit run can be re-fetched without re-executing the agent ----
        AgentRunResponse refetched = explainRequirementAgentService.getRun(actor, response.id());
        assertThat(refetched.result().explanation()).isEqualTo(response.result().explanation());

        // ---- A stranger may not view this run or re-run it for this subject's requirement ----
        AuthenticatedUser stranger = actorFor(createTestSubject());

        assertThatThrownBy(() -> explainRequirementAgentService.getRun(stranger, response.id()))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> explainRequirementAgentService.explainRequirement(stranger, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    // =========================================================================
    // INSUFFICIENT EVIDENCE AGAINST A REAL REQUIREMENT WITH NO FACTS
    // =========================================================================

    @Test
    void explainRequirementReportsInsufficientEvidenceForARealRequirementWithNoFactsAndNeverCallsTheLlm() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        RegulatoryVersionResponse version =
                createTestRegulatoryVersion("TEST_AGENT_WORKFLOW_EMPTY_SOURCE_" + System.nanoTime());
        RequirementAdminDetailResponse requirement =
                createAndPublishTestRequirement(version.id(), "TEST.AGENT_WORKFLOW_EMPTY_REQ_" + System.nanoTime());
        PathwayResponse pathway =
                createAndPublishTestPathway("TEST_AGENT_WORKFLOW_EMPTY_PATHWAY_" + System.nanoTime(), requirement.id());

        // No Fact recorded - the requirement must resolve to INSUFFICIENT_EVIDENCE.
        var assessment = pathwayAssessmentService.assess(actor, pathway.id(), subjectUserId, null);
        assertThat(assessment.requirementEvaluations().get(0).outcome())
                .isEqualTo(RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE);

        ExplainRequirementRequest request = new ExplainRequirementRequest();
        request.setPathwayAssessmentId(assessment.id());
        request.setRequirementId(requirement.id());

        AgentRunResponse response = explainRequirementAgentService.explainRequirement(actor, request);

        assertThat(response.status()).isEqualTo(AgentRunStatus.INSUFFICIENT_EVIDENCE);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.INSUFFICIENT_EVIDENCE);
        assertThat(response.result().explanation()).contains("Insufficient evidence");
        assertThat(response.result().humanReviewRequired()).isTrue();

        verify(llmService, never()).ask(anyString());

        AgentRun persisted = agentRunRepository.findById(response.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(AgentRunStatus.INSUFFICIENT_EVIDENCE);
        assertThat(persisted.getModel()).isNull();
    }
}
