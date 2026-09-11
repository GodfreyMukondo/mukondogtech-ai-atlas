package com.godfrey.ai_immigration_document_analyzer.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainPathwayDiscoveryRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainPathwayDiscoveryResult;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.PathwayDiscoveryAgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AiExplanationStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.repository.AgentRunRepository;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.AgentToolName;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.PathwayDiscoveryContextTool;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayDiscoveryResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayRankingRow;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.TopMissingRequirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;
import com.godfrey.ai_immigration_document_analyzer.service.LlmService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExplainPathwayDiscoveryAgentService} - Phase 5.3's
 * upstream, pre-assessment orchestrator. The one tool and the LLM are
 * mocked; every assertion here is about the orchestrator's OWN logic:
 * grounding-before-LLM (poor rankings are still grounded), never
 * fabricating a pathway/score, graceful AI degradation, recording exactly
 * what happened, and failing safely.
 */
@ExtendWith(MockitoExtension.class)
class ExplainPathwayDiscoveryAgentServiceTest {

    private static final Long SUBJECT_ID = 10L;
    private static final Long PATHWAY_ID = 200L;

    @Mock
    private PathwayDiscoveryContextTool pathwayDiscoveryContextTool;

    @Mock
    private LlmService llmService;

    @Mock
    private AgentRunRepository agentRunRepository;

    @Mock
    private FactAuthorizationService factAuthorizationService;

    private ObjectMapper objectMapper;

    private ExplainPathwayDiscoveryAgentService service;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        service = new ExplainPathwayDiscoveryAgentService(
                pathwayDiscoveryContextTool, llmService, agentRunRepository, factAuthorizationService, objectMapper
        );

        org.mockito.Mockito.lenient().when(agentRunRepository.save(any(AgentRun.class))).thenAnswer(invocation -> {
            AgentRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(1L);
            }
            return run;
        });
    }

    private AuthenticatedUser user(Long id) {
        return new AuthenticatedUser(
                id, "user" + id + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())), true, true, true, true
        );
    }

    private ExplainPathwayDiscoveryRequest request() {
        ExplainPathwayDiscoveryRequest request = new ExplainPathwayDiscoveryRequest();
        request.setSubjectUserId(SUBJECT_ID);
        return request;
    }

    private PathwayRankingRow row(
            int rank, RequirementEvaluationOutcome outcome, long conflicting, long needsVerification
    ) {
        return new PathwayRankingRow(
                rank, PATHWAY_ID, "TEST_PATHWAY", "Test Pathway", "Testland", "Work",
                outcome, 80.0, 80.0, 90.0, 100.0,
                RegulatoryVerificationStatus.UNVERIFIED_INGESTION,
                3, 1, 1, conflicting, needsVerification,
                List.of(new TopMissingRequirement(
                        900L, "TEST.REQ", "Test Requirement", true,
                        CaseRequirementSupportStatus.MISSING, List.of("EMPLOYMENT.CURRENT_EMPLOYER")
                )),
                "Ranked using 3 published requirements - 80% mandatory satisfied."
        );
    }

    private PathwayDiscoveryResponse discovery(List<PathwayRankingRow> ranked) {
        return new PathwayDiscoveryResponse(
                SUBJECT_ID, LocalDateTime.now(), ranked.size(), ranked,
                "Pathway rankings reflect evidence alignment only - never a guarantee of approval."
        );
    }

    // =========================================================================
    // 1. SUCCESSFUL RECOMMENDATION / STRUCTURED RESULT / ACTUAL INVOCATION
    // =========================================================================

    @Test
    void explainPathwayDiscoveryReturnsGroundedCompletedResultAndInvokesTheToolForReal() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        PathwayRankingRow topRow = row(1, RequirementEvaluationOutcome.SATISFIED, 0, 0);
        PathwayDiscoveryResponse context = discovery(List.of(topRow));

        when(pathwayDiscoveryContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString())).thenReturn(
                "{\"explanation\":\"Pathway 1 is your strongest current option.\",\"recommendedNextStep\":\"Proceed with assessment.\"}"
        );

        PathwayDiscoveryAgentRunResponse response = service.explainPathwayDiscovery(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.toolsInvoked()).containsExactly(AgentToolName.GET_PATHWAY_DISCOVERY_CONTEXT.name());

        ExplainPathwayDiscoveryResult result = response.result();
        assertThat(result).isNotNull();
        assertThat(result.explanation()).isEqualTo("Pathway 1 is your strongest current option.");
        assertThat(result.recommendedNextStep()).isEqualTo("Proceed with assessment.");
        assertThat(result.aiExplanationStatus()).isEqualTo(AiExplanationStatus.GENERATED);
        assertThat(result.rankedPathways()).containsExactly(topRow);
        assertThat(result.disclaimer()).isEqualTo(context.disclaimer());
        assertThat(result.humanReviewRequired()).isFalse();

        verify(pathwayDiscoveryContextTool, times(1)).invoke(eq(actor), any());
        verify(llmService, times(1)).ask(anyString());
        verify(agentRunRepository, times(1)).save(any(AgentRun.class));
    }

    @Test
    void explainPathwayDiscoveryFeedsRankedDataAndDisclaimerIntoThePromptAsGroundingContext() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        PathwayDiscoveryResponse context = discovery(List.of(row(1, RequirementEvaluationOutcome.SATISFIED, 0, 0)));

        when(pathwayDiscoveryContextTool.invoke(eq(actor), any())).thenReturn(context);

        org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        when(llmService.ask(promptCaptor.capture()))
                .thenReturn("{\"explanation\":\"E\",\"recommendedNextStep\":\"N\"}");

        service.explainPathwayDiscovery(actor, request());

        assertThat(promptCaptor.getValue())
                .contains("Test Pathway")
                .contains("Ranked using 3 published requirements")
                .contains("not a fact you may contradict")
                .contains("Never invent a pathway")
                .contains(context.disclaimer());
    }

    // =========================================================================
    // 2. GROUNDING - A POOR RANKING IS STILL GROUNDED
    // =========================================================================

    @Test
    void explainPathwayDiscoveryIsGroundedEvenWhenEveryPathwayHasMissingOrConflictingRequirements() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        PathwayRankingRow poorRow = row(1, RequirementEvaluationOutcome.NOT_SATISFIED, 1, 0);
        PathwayDiscoveryResponse context = discovery(List.of(poorRow));

        when(pathwayDiscoveryContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"No pathway is fully ready yet.\",\"recommendedNextStep\":\"Resolve the conflict first.\"}");

        PathwayDiscoveryAgentRunResponse response = service.explainPathwayDiscovery(actor, request());

        // A ranked list where the top pathway has an unresolved conflict is
        // real, deterministically-computed signal - NOT an absence of
        // grounding. The LLM must be invoked, never skipped.
        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.result().humanReviewRequired()).isTrue(); // top pathway has a conflict
        verify(llmService, times(1)).ask(anyString());
    }

    @Test
    void explainPathwayDiscoveryReturnsInsufficientEvidenceOnlyWhenThereAreNoPublishedPathways() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        PathwayDiscoveryResponse context = discovery(List.of());

        when(pathwayDiscoveryContextTool.invoke(eq(actor), any())).thenReturn(context);

        PathwayDiscoveryAgentRunResponse response = service.explainPathwayDiscovery(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.INSUFFICIENT_EVIDENCE);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.INSUFFICIENT_EVIDENCE);
        assertThat(response.result().explanation()).contains("Insufficient evidence");
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.NOT_ATTEMPTED);
        assertThat(response.result().humanReviewRequired()).isTrue();

        // Deterministic hallucination defense: the LLM is never called at all
        // when there is genuinely nothing to recommend.
        verify(llmService, never()).ask(anyString());
    }

    // =========================================================================
    // 3. HUMAN REVIEW REQUIRED
    // =========================================================================

    @Test
    void explainPathwayDiscoveryFlagsHumanReviewWhenTheTopPathwayNeedsVerification() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        PathwayRankingRow topRow = row(1, RequirementEvaluationOutcome.PENDING_REVIEW, 0, 1);
        PathwayDiscoveryResponse context = discovery(List.of(topRow));

        when(pathwayDiscoveryContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"Under review.\",\"recommendedNextStep\":\"Wait for verification.\"}");

        PathwayDiscoveryAgentRunResponse response = service.explainPathwayDiscovery(actor, request());

        assertThat(response.result().humanReviewRequired()).isTrue();
    }

    @Test
    void explainPathwayDiscoveryDoesNotFlagHumanReviewWhenOnlyALowerRankedPathwayHasConflicts() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        PathwayRankingRow topRow = row(1, RequirementEvaluationOutcome.SATISFIED, 0, 0);
        PathwayRankingRow secondRow = row(2, RequirementEvaluationOutcome.CONFLICTED, 1, 0);
        PathwayDiscoveryResponse context = discovery(List.of(topRow, secondRow));

        when(pathwayDiscoveryContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"Pathway 1 is clearly strongest.\",\"recommendedNextStep\":\"Proceed.\"}");

        PathwayDiscoveryAgentRunResponse response = service.explainPathwayDiscovery(actor, request());

        // Only the TOP-ranked pathway's own conflicts/needs-verification
        // count toward human review - a lower-ranked pathway's issues don't
        // block the top recommendation.
        assertThat(response.result().humanReviewRequired()).isFalse();
    }

    // =========================================================================
    // 4. UNAUTHORIZED CASE ACCESS - NO RUN EVER CREATED
    // =========================================================================

    @Test
    void explainPathwayDiscoveryPropagatesUnauthorizedCaseAccessAndNeverCreatesARun() {

        AuthenticatedUser stranger = user(999L);

        when(pathwayDiscoveryContextTool.invoke(eq(stranger), any()))
                .thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> service.explainPathwayDiscovery(stranger, request()))
                .isInstanceOf(AccessDeniedException.class);

        verify(agentRunRepository, never()).save(any());
        verify(llmService, never()).ask(anyString());
    }

    // =========================================================================
    // 5. GRACEFUL AI DEGRADATION - the deterministic ranking must NEVER
    // disappear merely because the LLM failed.
    // =========================================================================

    @Test
    void explainPathwayDiscoveryDegradesGracefullyWhenOpenAiQuotaIsExhausted() {
        assertGracefulDegradationOnLlmCallFailure(new RuntimeException(
                "429 Too Many Requests - insufficient_quota: credit_balance_exhausted"
        ));
    }

    @Test
    void explainPathwayDiscoveryDegradesGracefullyOnLlmTimeout() {
        assertGracefulDegradationOnLlmCallFailure(new RuntimeException("Read timed out"));
    }

    @Test
    void explainPathwayDiscoveryDegradesGracefullyWhenTheLlmProviderIsUnavailable() {
        assertGracefulDegradationOnLlmCallFailure(new RuntimeException(
                "Connection refused: api.openai.com:443 (internal provider detail)"
        ));
    }

    @Test
    void explainPathwayDiscoveryDegradesGracefullyOnAnUnexpectedLlmException() {
        assertGracefulDegradationOnLlmCallFailure(new IllegalStateException("Failed to generate AI response"));
    }

    private void assertGracefulDegradationOnLlmCallFailure(RuntimeException llmFailure) {

        AuthenticatedUser actor = user(SUBJECT_ID);

        PathwayRankingRow topRow = row(1, RequirementEvaluationOutcome.SATISFIED, 0, 0);
        PathwayDiscoveryResponse context = discovery(List.of(topRow));

        when(pathwayDiscoveryContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString())).thenThrow(llmFailure);

        // No exception - the deterministic ranking succeeds regardless.
        PathwayDiscoveryAgentRunResponse response = service.explainPathwayDiscovery(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.errorMessage()).isNull();

        ExplainPathwayDiscoveryResult result = response.result();
        assertThat(result).isNotNull();
        assertThat(result.aiExplanationStatus()).isEqualTo(AiExplanationStatus.UNAVAILABLE);
        assertThat(result.explanation()).isNull();
        // The top pathway's own deterministic explanation is the only
        // permitted fallback next step - never a fabricated one.
        assertThat(result.recommendedNextStep()).isEqualTo(topRow.explanation());

        // The authoritative deterministic ranking is completely intact.
        assertThat(result.rankedPathways()).containsExactly(topRow);
        assertThat(result.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);

        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, times(1)).save(captor.capture());

        AgentRun savedRun = captor.getValue();
        assertThat(savedRun.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(savedRun.getErrorMessage()).isNull();

        String leakyDetail = llmFailure.getMessage();
        assertThat(savedRun.getResultJson()).doesNotContain(leakyDetail);
        assertThat(savedRun.getReasoningSummary()).doesNotContain(leakyDetail);
    }

    @Test
    void explainPathwayDiscoveryDegradesGracefullyWhenTheLlmReturnsUnparsableText() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        PathwayDiscoveryResponse context = discovery(List.of(row(1, RequirementEvaluationOutcome.SATISFIED, 0, 0)));

        when(pathwayDiscoveryContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString())).thenReturn("this is not json at all");

        PathwayDiscoveryAgentRunResponse response = service.explainPathwayDiscovery(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.FAILED);
        assertThat(response.result().explanation()).isNull();
    }

    @Test
    void explainPathwayDiscoveryDegradesGracefullyWhenTheLlmReturnsAnEmptyResponse() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        PathwayDiscoveryResponse context = discovery(List.of(row(1, RequirementEvaluationOutcome.SATISFIED, 0, 0)));

        when(pathwayDiscoveryContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString())).thenReturn("");

        PathwayDiscoveryAgentRunResponse response = service.explainPathwayDiscovery(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.FAILED);
        assertThat(response.result().explanation()).isNull();
    }

    @Test
    void explainPathwayDiscoveryParsesMarkdownFencedJsonFromTheLlm() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        PathwayDiscoveryResponse context = discovery(List.of(row(1, RequirementEvaluationOutcome.SATISFIED, 0, 0)));

        when(pathwayDiscoveryContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString())).thenReturn(
                "```json\n{\"explanation\":\"Fenced recommendation.\",\"recommendedNextStep\":\"Fenced next step.\"}\n```"
        );

        PathwayDiscoveryAgentRunResponse response = service.explainPathwayDiscovery(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().explanation()).isEqualTo("Fenced recommendation.");
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.GENERATED);
    }

    @Test
    void explainPathwayDiscoveryNeverPersistsARawInternalExceptionMessageFromAnUnexpectedFailure() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(pathwayDiscoveryContextTool.invoke(eq(actor), any())).thenThrow(new RuntimeException(
                "ORA-00942: table or view does not exist at "
                        + "com.godfrey.ai_immigration_document_analyzer.internal.SecretRepository.query(SecretRepository.java:42)"
        ));

        assertThatThrownBy(() -> service.explainPathwayDiscovery(actor, request()))
                .isInstanceOf(RuntimeException.class);

        // No AgentRun is ever created for a failure at the authorization/tool
        // boundary - identical to the other two agents' behavior.
        verify(agentRunRepository, never()).save(any());
    }

    // =========================================================================
    // AGENT RUN LIFECYCLE / GET RUN
    // =========================================================================

    @Test
    void getRunAuthorizesThroughTheExistingFactAuthorizationBoundary() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        ExplainPathwayDiscoveryResult persistedResult = new ExplainPathwayDiscoveryResult(
                SUBJECT_ID, LocalDateTime.now(), 1, List.of(row(1, RequirementEvaluationOutcome.SATISFIED, 0, 0)),
                "disclaimer", "A recommendation.", AgentGroundingState.GROUNDED, AiExplanationStatus.GENERATED,
                "Next step.", false
        );

        AgentRun run = AgentRun.builder()
                .id(77L).subjectUserId(SUBJECT_ID).requestedByUserId(SUBJECT_ID)
                .status(AgentRunStatus.COMPLETED)
                .resultJson(writeJson(persistedResult))
                .build();

        when(agentRunRepository.findById(77L)).thenReturn(Optional.of(run));

        PathwayDiscoveryAgentRunResponse response = service.getRun(actor, 77L);

        verify(factAuthorizationService).assertCanView(eq(actor), eq(SUBJECT_ID), any(), any(), anyString());
        assertThat(response.result().explanation()).isEqualTo("A recommendation.");
    }

    @Test
    void getRunPropagatesDenialForAnUnauthorizedActor() {

        AuthenticatedUser stranger = user(999L);

        AgentRun run = AgentRun.builder().id(77L).subjectUserId(SUBJECT_ID).requestedByUserId(SUBJECT_ID)
                .status(AgentRunStatus.COMPLETED).build();

        when(agentRunRepository.findById(77L)).thenReturn(Optional.of(run));
        doThrow(new AccessDeniedException("denied")).when(factAuthorizationService)
                .assertCanView(eq(stranger), eq(SUBJECT_ID), any(), any(), anyString());

        assertThatThrownBy(() -> service.getRun(stranger, 77L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getRunThrowsResourceNotFoundWhenTheRunDoesNotExist() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(agentRunRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRun(actor, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
