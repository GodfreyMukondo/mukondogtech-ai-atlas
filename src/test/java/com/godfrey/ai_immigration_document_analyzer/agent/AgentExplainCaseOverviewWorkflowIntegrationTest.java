package com.godfrey.ai_immigration_document_analyzer.agent;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.CaseOverviewAgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainCaseOverviewRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AiExplanationStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.repository.AgentRunRepository;
import com.godfrey.ai_immigration_document_analyzer.agent.service.ExplainCaseOverviewAgentService;
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
 * AGENT EXPLAIN CASE OVERVIEW WORKFLOW INTEGRATION TEST
 * ============================================================================
 *
 * A genuine end-to-end exercise of the Phase 5.4 EXPLAIN_CASE_OVERVIEW agent
 * against the real, configured Oracle instance - not mocks. Mirrors {@code
 * AgentExplainPathwayDiscoveryWorkflowIntegrationTest}'s pattern: create a
 * real test subject, record real Facts (including a deliberate conflict),
 * then drive the agent through its real tool ({@code CaseOverviewContextTool}
 * -&gt; {@code CaseOverviewService}) against that real, transient overview.
 *
 * The ONLY mocked collaborator is {@link LlmService} - this test does not
 * depend on any paid external AI credits.
 *
 * {@code @Transactional} rolls back every write at the end of each method -
 * no test artifacts are left in the real database.
 * ============================================================================
 */
@SpringBootTest
@Transactional
class AgentExplainCaseOverviewWorkflowIntegrationTest {

    @MockBean
    private LlmService llmService;

    @Autowired
    private ExplainCaseOverviewAgentService explainCaseOverviewAgentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FactRepository factRepository;

    @Autowired
    private AgentRunRepository agentRunRepository;

    private AuthenticatedUser actorFor(Long userId) {
        return new AuthenticatedUser(
                userId, "case-overview-agent-workflow-test-" + userId + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())),
                true, true, true, true
        );
    }

    private Long createTestSubject() {
        User saved = userRepository.save(User.builder()
                .fullName("Case Overview Agent Workflow Test Subject")
                .email("case-overview-agent-workflow-subject-" + System.nanoTime() + "@example.com")
                .password("irrelevant-hash")
                .role(Role.USER)
                .build());
        return saved.getId();
    }

    private void recordFact(Long subjectUserId, String factKey, FactCategory category, String value) {
        factRepository.save(Fact.builder()
                .subjectUserId(subjectUserId)
                .category(category)
                .factKey(factKey)
                .valueType(FactValueType.STRING)
                .stringValue(value)
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
    // GROUNDED, SUCCESSFUL EXPLANATION AGAINST REAL DATA
    // =========================================================================

    @Test
    void explainCaseOverviewProducesAGroundedAuditedResultForARealSubjectWithCaseHistory() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        recordFact(subjectUserId, "EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT, "Acme Corp");

        when(llmService.ask(anyString())).thenReturn(
                "{\"explanation\":\"Your case currently shows no open issues.\","
                        + "\"recommendedNextStep\":\"No action is required.\"}"
        );

        ExplainCaseOverviewRequest request = new ExplainCaseOverviewRequest();
        request.setSubjectUserId(subjectUserId);

        CaseOverviewAgentRunResponse response = explainCaseOverviewAgentService.explainCaseOverview(actor, request);

        // ---- Structured, grounded result over REAL persisted data ----
        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.result().explanation()).isEqualTo("Your case currently shows no open issues.");
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.GENERATED);
        assertThat(response.toolsInvoked()).contains("GET_CASE_OVERVIEW_CONTEXT");
        assertThat(response.result().signals().subjectUserId()).isEqualTo(subjectUserId);

        // ---- The run was genuinely persisted (real row, real database) ----
        AgentRun persisted = agentRunRepository.findById(response.id()).orElseThrow();
        assertThat(persisted.getSubjectUserId()).isEqualTo(subjectUserId);
        assertThat(persisted.getRequestedByUserId()).isEqualTo(subjectUserId);
        assertThat(persisted.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(persisted.getPathwayAssessmentId()).isNull(); // case-wide goal, never tied to one assessment
        assertThat(persisted.getRequirementId()).isNull();
        assertThat(persisted.getResultJson()).isNotBlank();
        assertThat(persisted.getCompletedAt()).isNotNull();

        // ---- The audit run can be re-fetched without re-executing the agent ----
        CaseOverviewAgentRunResponse refetched = explainCaseOverviewAgentService.getRun(actor, response.id());
        assertThat(refetched.result().explanation()).isEqualTo(response.result().explanation());

        // ---- A stranger may not view this run or re-run it for this subject ----
        AuthenticatedUser stranger = actorFor(createTestSubject());

        assertThatThrownBy(() -> explainCaseOverviewAgentService.getRun(stranger, response.id()))
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() -> explainCaseOverviewAgentService.explainCaseOverview(stranger, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    // =========================================================================
    // NO CASE HISTORY AT ALL - GENUINELY UNGROUNDED, NEVER FABRICATED
    // =========================================================================

    @Test
    void explainCaseOverviewIsInsufficientEvidenceWhenNoFactHasBeenRecordedYet() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        ExplainCaseOverviewRequest request = new ExplainCaseOverviewRequest();
        request.setSubjectUserId(subjectUserId);

        CaseOverviewAgentRunResponse response = explainCaseOverviewAgentService.explainCaseOverview(actor, request);

        assertThat(response.status()).isEqualTo(AgentRunStatus.INSUFFICIENT_EVIDENCE);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.INSUFFICIENT_EVIDENCE);
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.NOT_ATTEMPTED);
        assertThat(response.result().humanReviewRequired()).isTrue();

        verify(llmService, never()).ask(anyString());
    }

    // =========================================================================
    // GRACEFUL AI DEGRADATION AGAINST REAL DATA - the complete deterministic
    // overview and AgentRun round trip must succeed even when the AI provider
    // fails (mocked here exactly like OpenAI returning HTTP 429 would) - no
    // live OpenAI credentials are required for this test.
    // =========================================================================

    @Test
    void explainCaseOverviewReturnsTheFullDeterministicOverviewWhenTheLlmIsUnavailable() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser actor = actorFor(subjectUserId);

        recordFact(subjectUserId, "EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT, "Acme Corp");

        // Simulates OpenAI returning HTTP 429 / insufficient_quota /
        // credit_balance_exhausted.
        when(llmService.ask(anyString())).thenThrow(new RuntimeException(
                "429 Too Many Requests - insufficient_quota: credit_balance_exhausted"
        ));

        ExplainCaseOverviewRequest request = new ExplainCaseOverviewRequest();
        request.setSubjectUserId(subjectUserId);

        CaseOverviewAgentRunResponse response = explainCaseOverviewAgentService.explainCaseOverview(actor, request);

        // ---- The run still completes successfully - never a 5xx/exception ----
        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.errorMessage()).isNull();

        // ---- The full deterministic overview is present and unaltered ----
        assertThat(response.result().groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.result().signals().subjectUserId()).isEqualTo(subjectUserId);

        // ---- The AI explanation is honestly absent - never fabricated ----
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.UNAVAILABLE);
        assertThat(response.result().explanation()).isNull();

        // ---- The AgentRun round trip persisted correctly, no leaked provider detail ----
        AgentRun persisted = agentRunRepository.findById(response.id()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(persisted.getSubjectUserId()).isEqualTo(subjectUserId);
        assertThat(persisted.getErrorMessage()).isNull();
        assertThat(persisted.getResultJson()).doesNotContain("insufficient_quota");

        // ---- Re-fetching the run returns the same graceful-degradation result ----
        CaseOverviewAgentRunResponse refetched = explainCaseOverviewAgentService.getRun(actor, response.id());
        assertThat(refetched.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.UNAVAILABLE);

        // ---- Retrying does not mutate this run or corrupt the deterministic data ----
        // NOTE: doReturn(...).when(...) is required here rather than
        // when(...).thenReturn(...) - the mock is currently stubbed to
        // THROW, and when(mock.method()) would invoke that throwing
        // stub immediately while evaluating the argument, before the new
        // stub could even be attached.
        org.mockito.Mockito.doReturn("{\"explanation\":\"Recovered.\",\"recommendedNextStep\":\"None.\"}")
                .when(llmService).ask(anyString());

        CaseOverviewAgentRunResponse retried = explainCaseOverviewAgentService.explainCaseOverview(actor, request);

        assertThat(retried.id()).isNotEqualTo(response.id());
        assertThat(retried.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.GENERATED);

        AgentRun originalStillIntact = agentRunRepository.findById(response.id()).orElseThrow();
        assertThat(originalStillIntact.getResultJson()).isEqualTo(persisted.getResultJson());
    }

    // =========================================================================
    // UNAUTHORIZED ACCESS - PROPAGATED BEFORE ANY RUN IS CREATED
    // =========================================================================

    @Test
    void explainCaseOverviewDeniesAStrangerBeforeCreatingAnyRun() {

        Long subjectUserId = createTestSubject();
        AuthenticatedUser stranger = actorFor(createTestSubject());

        ExplainCaseOverviewRequest request = new ExplainCaseOverviewRequest();
        request.setSubjectUserId(subjectUserId);

        assertThatThrownBy(() -> explainCaseOverviewAgentService.explainCaseOverview(stranger, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(llmService, never()).ask(anyString());
    }
}
