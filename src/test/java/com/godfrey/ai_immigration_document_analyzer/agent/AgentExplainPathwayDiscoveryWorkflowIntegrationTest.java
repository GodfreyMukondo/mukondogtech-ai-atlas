package com.godfrey.ai_immigration_document_analyzer.agent;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainPathwayDiscoveryRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AiExplanationStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.PathwayDiscoveryAgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.repository.AgentRunRepository;
import com.godfrey.ai_immigration_document_analyzer.agent.service.ExplainPathwayDiscoveryAgentService;
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
 * AGENT EXPLAIN PATHWAY DISCOVERY WORKFLOW INTEGRATION TEST
 * ============================================================================
 *
 * A genuine end-to-end exercise of the Phase 5.3 EXPLAIN_PATHWAY_DISCOVERY
 * agent against the real, configured Oracle instance - not mocks. Mirrors
 * {@code AgentExplainPathwayAssessmentWorkflowIntegrationTest}'s pattern:
 * build a real published Requirement/Pathway via the existing admin
 * services, record a real Fact/no-Fact case, then drive the agent through
 * its real tool ({@code PathwayDiscoveryContextTool} -&gt; {@code
 * PathwayDiscoveryService}) against that real, transient ranking.
 *
 * The ONLY mocked collaborator is {@link LlmService}.
 *
 * {@code @Transactional} rolls back every write at the end of each method -
 * no test artifacts are left in the real database.
 * ============================================================================
 */
@SpringBootTest
@Transactional
class AgentExplainPathwayDiscoveryWorkflowIntegrationTest {

    @MockBean
    private LlmService llmService;

    @Autowired
    private ExplainPathwayDiscoveryAgentService explainPathwayDiscoveryAgentService;

    @Autowired
    private RegulatoryVersionAdminService regulatoryVersionAdminService;

    @Autowired
    private RequirementAdminService requirementAdminService;

    @Autowired
    private PathwayAdminService pathwayAdminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FactRepository factRepository;

    @Autowired
    private AgentRunRepository agentRunRepository;

    private static final String FACT_KEY = "EMPLOYMENT.CURRENT_EMPLOYER";

    private AuthenticatedUser actorFor(Long userId) {
        return new AuthenticatedUser(
                userId, "discovery-agent-workflow-test-" + userId + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())),
                true, true, true, true
        );
    }

    private Long createTestSubject() {
        User saved = userRepository.save(User.builder()
                .fullName("Discovery Agent Workflow Test Subject")
                .email("discovery-agent-workflow-subject-" + System.nanoTime() + "@example.com")
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
        request.setTitle("Discovery Agent Workflow Test Requirement " + key);
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
        createPathway.setName("Discovery Agent Workflow Test Pathway");
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
        factRepository.save(Fact.builder()
                .subjectUserId(subjectUserId)
                .category(FactCategory.EMPLOYMENT)
                .factKey(FACT_KEY)
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
    // GROUNDED, SUCCESSFUL RECOMMENDATION AGAINST REAL DATA
    // =========================================================================

    @Test
    void explainPathwayDiscoveryProducesAGroundedAuditedResultForARealPublishedPathway() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        RegulatoryVersionResponse version =
                createTestRegulatoryVersion("TEST_DISCOVERY_AGENT_SOURCE_" + System.nanoTime());
        RequirementAdminDetailResponse requirement =
                createAndPublishTestRequirement(version.id(), "TEST.DISCOVERY_AGENT_REQ_" + System.nanoTime());
        PathwayResponse pathway =
                createAndPublishTestPathway("TEST_DISCOVERY_AGENT_PATHWAY_" + System.nanoTime(), requirement.id());

        acceptFact(subjectUserId);

        when(llmService.ask(anyString())).thenReturn(
                "{\"explanation\":\"This pathway is your strongest current option.\","
                        + "\"recommendedNextStep\":\"Proceed to a formal assessment.\"}"
        );

        ExplainPathwayDiscoveryRequest request = new ExplainPathwayDiscoveryRequest();
        request.setSubjectUserId(subjectUserId);

        PathwayDiscoveryAgentRunResponse response = explainPathwayDiscoveryAgentService.explainPathwayDiscovery(actor, request);

        // ---- Structured, grounded result over REAL persisted data ----
        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.result().rankedPathways())
                .extracting(row -> row.pathwayKey())
                .contains(pathway.pathwayKey());
        assertThat(response.result().explanation())
                .isEqualTo("This pathway is your strongest current option.");
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.GENERATED);
        assertThat(response.toolsInvoked()).contains("GET_PATHWAY_DISCOVERY_CONTEXT");

        var row = response.result().rankedPathways().stream()
                .filter(r -> r.pathwayKey().equals(pathway.pathwayKey()))
                .findFirst().orElseThrow();
        assertThat(row.outcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(row.regulatoryCertainty()).isEqualTo(RegulatoryVerificationStatus.UNVERIFIED_INGESTION);

        // ---- The run was genuinely persisted (real row, real database) ----
        AgentRun persisted = agentRunRepository.findById(response.id()).orElseThrow();
        assertThat(persisted.getSubjectUserId()).isEqualTo(subjectUserId);
        assertThat(persisted.getRequestedByUserId()).isEqualTo(subjectUserId);
        assertThat(persisted.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(persisted.getPathwayAssessmentId()).isNull(); // pre-assessment goal, never tied to one assessment
        assertThat(persisted.getRequirementId()).isNull();
        assertThat(persisted.getResultJson()).isNotBlank();
        assertThat(persisted.getCompletedAt()).isNotNull();

        // ---- The audit run can be re-fetched without re-executing the agent ----
        PathwayDiscoveryAgentRunResponse refetched = explainPathwayDiscoveryAgentService.getRun(actor, response.id());
        assertThat(refetched.result().explanation()).isEqualTo(response.result().explanation());

        // ---- A stranger may not view this run or re-run it for this subject ----
        AuthenticatedUser stranger = actorFor(createTestSubject());

        assertThatThrownBy(() -> explainPathwayDiscoveryAgentService.getRun(stranger, response.id()))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> explainPathwayDiscoveryAgentService.explainPathwayDiscovery(stranger, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    // =========================================================================
    // A REQUIREMENT-LEVEL INSUFFICIENT-EVIDENCE OUTCOME IS STILL A GROUNDED
    // DISCOVERY RECOMMENDATION - never confused with "nothing to explain"
    // =========================================================================

    @Test
    void explainPathwayDiscoveryIsStillGroundedWhenNoEvidenceHasBeenRecordedYet() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        RegulatoryVersionResponse version =
                createTestRegulatoryVersion("TEST_DISCOVERY_AGENT_EMPTY_SOURCE_" + System.nanoTime());
        RequirementAdminDetailResponse requirement =
                createAndPublishTestRequirement(version.id(), "TEST.DISCOVERY_AGENT_EMPTY_REQ_" + System.nanoTime());
        PathwayResponse pathway =
                createAndPublishTestPathway("TEST_DISCOVERY_AGENT_EMPTY_PATHWAY_" + System.nanoTime(), requirement.id());

        // No Fact recorded - the pathway ranks with INSUFFICIENT_EVIDENCE,
        // but Discovery ITSELF still produced a real ranked row.
        when(llmService.ask(anyString())).thenReturn(
                "{\"explanation\":\"No pathway is fully ready yet - evidence is still needed.\","
                        + "\"recommendedNextStep\":\"Provide evidence of current employment.\"}"
        );

        ExplainPathwayDiscoveryRequest request = new ExplainPathwayDiscoveryRequest();
        request.setSubjectUserId(subjectUserId);

        PathwayDiscoveryAgentRunResponse response = explainPathwayDiscoveryAgentService.explainPathwayDiscovery(actor, request);

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);

        var row = response.result().rankedPathways().stream()
                .filter(r -> r.pathwayKey().equals(pathway.pathwayKey()))
                .findFirst().orElseThrow();
        assertThat(row.outcome()).isEqualTo(RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE);

        verify(llmService, times(1)).ask(anyString());
    }

    // =========================================================================
    // GRACEFUL AI DEGRADATION AGAINST REAL DATA - the complete deterministic
    // ranking and AgentRun round trip must succeed even when the AI provider
    // fails (mocked here exactly like OpenAI returning HTTP 429 would) - no
    // live OpenAI credentials are required for this test.
    // =========================================================================

    @Test
    void explainPathwayDiscoveryReturnsTheFullDeterministicRankingWhenTheLlmIsUnavailable() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        RegulatoryVersionResponse version =
                createTestRegulatoryVersion("TEST_DISCOVERY_AGENT_DEGRADED_SOURCE_" + System.nanoTime());
        RequirementAdminDetailResponse requirement =
                createAndPublishTestRequirement(version.id(), "TEST.DISCOVERY_AGENT_DEGRADED_REQ_" + System.nanoTime());
        PathwayResponse pathway =
                createAndPublishTestPathway("TEST_DISCOVERY_AGENT_DEGRADED_PATHWAY_" + System.nanoTime(), requirement.id());

        acceptFact(subjectUserId);

        // Simulates OpenAI returning HTTP 429 / insufficient_quota /
        // credit_balance_exhausted.
        when(llmService.ask(anyString())).thenThrow(new RuntimeException(
                "429 Too Many Requests - insufficient_quota: credit_balance_exhausted"
        ));

        ExplainPathwayDiscoveryRequest request = new ExplainPathwayDiscoveryRequest();
        request.setSubjectUserId(subjectUserId);

        PathwayDiscoveryAgentRunResponse response = explainPathwayDiscoveryAgentService.explainPathwayDiscovery(actor, request);

        // ---- The run still completes successfully - never a 5xx/exception ----
        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.errorMessage()).isNull();

        // ---- The full deterministic ranking is present and unaltered ----
        assertThat(response.result().groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        var row = response.result().rankedPathways().stream()
                .filter(r -> r.pathwayKey().equals(pathway.pathwayKey()))
                .findFirst().orElseThrow();
        assertThat(row.outcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(row.regulatoryCertainty()).isEqualTo(RegulatoryVerificationStatus.UNVERIFIED_INGESTION);

        // ---- The AI recommendation is honestly absent - never fabricated ----
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.UNAVAILABLE);
        assertThat(response.result().explanation()).isNull();

        // ---- The AgentRun round trip persisted correctly ----
        AgentRun persisted = agentRunRepository.findById(response.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(persisted.getSubjectUserId()).isEqualTo(subjectUserId);
        assertThat(persisted.getErrorMessage()).isNull();
        assertThat(persisted.getResultJson()).doesNotContain("insufficient_quota");

        // ---- Re-fetching the run returns the same graceful-degradation result ----
        PathwayDiscoveryAgentRunResponse refetched = explainPathwayDiscoveryAgentService.getRun(actor, response.id());
        assertThat(refetched.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.UNAVAILABLE);
    }

    // =========================================================================
    // UNAUTHORIZED ACCESS - PROPAGATED BEFORE ANY RUN IS CREATED
    // =========================================================================

    @Test
    void explainPathwayDiscoveryDeniesAStrangerBeforeCreatingAnyRun() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser stranger = actorFor(createTestSubject());

        ExplainPathwayDiscoveryRequest request = new ExplainPathwayDiscoveryRequest();
        request.setSubjectUserId(subjectUserId);

        assertThatThrownBy(() -> explainPathwayDiscoveryAgentService.explainPathwayDiscovery(stranger, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(llmService, never()).ask(anyString());
    }
}
