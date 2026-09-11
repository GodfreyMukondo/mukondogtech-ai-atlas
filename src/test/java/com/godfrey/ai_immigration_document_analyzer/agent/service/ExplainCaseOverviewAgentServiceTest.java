package com.godfrey.ai_immigration_document_analyzer.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.CaseOverviewAgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainCaseOverviewRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainCaseOverviewResult;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AiExplanationStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.repository.AgentRunRepository;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.AgentToolName;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.CaseOverviewContextTool;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseContradictionsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRiskBand;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseSignalsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseTimelineEventResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseTimelineResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.PotentialOverlapContradiction;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactConflictResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.ConflictStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
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
 * Unit tests for {@link ExplainCaseOverviewAgentService} - Phase 5.4's
 * case-wide orchestrator. The one tool and the LLM are mocked; every
 * assertion here is about the orchestrator's OWN logic: grounding-before-LLM
 * (a clean case is still grounded), never fabricating a conflict/timeline
 * event/risk signal, graceful AI degradation, recording exactly what
 * happened, and failing safely. Modeled directly on {@code
 * ExplainPathwayDiscoveryAgentServiceTest} (Phase 5.3)'s style.
 */
@ExtendWith(MockitoExtension.class)
class ExplainCaseOverviewAgentServiceTest {

    private static final Long SUBJECT_ID = 10L;

    @Mock
    private CaseOverviewContextTool caseOverviewContextTool;

    @Mock
    private LlmService llmService;

    @Mock
    private AgentRunRepository agentRunRepository;

    @Mock
    private FactAuthorizationService factAuthorizationService;

    private ObjectMapper objectMapper;

    private ExplainCaseOverviewAgentService service;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        service = new ExplainCaseOverviewAgentService(
                caseOverviewContextTool, llmService, agentRunRepository, factAuthorizationService, objectMapper
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

    private ExplainCaseOverviewRequest request() {
        ExplainCaseOverviewRequest request = new ExplainCaseOverviewRequest();
        request.setSubjectUserId(SUBJECT_ID);
        return request;
    }

    private FactResponse fact(String factKey, FactCategory category) {
        return new FactResponse(
                1L, SUBJECT_ID, category, factKey,
                FactValueType.STRING, "value", null, null, null,
                FactStatus.ACCEPTED, FactProvenanceType.USER_INPUT, FactSensitivityTier.T2_STANDARD_PERSONAL,
                0.9, FactConfidenceLevel.HIGH, null, false, null, null,
                LocalDateTime.now().minusYears(1), null, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now(), null, List.of()
        );
    }

    private CaseContradictionsResponse contradictions(List<FactConflictResponse> open, List<PotentialOverlapContradiction> overlaps) {
        return new CaseContradictionsResponse(SUBJECT_ID, open, overlaps, LocalDateTime.now(), "note");
    }

    private CaseTimelineResponse timeline(List<CaseTimelineEventResponse> events) {
        return new CaseTimelineResponse(SUBJECT_ID, events, LocalDateTime.now(), "note");
    }

    private CaseSignalsResponse signals(CaseRiskBand band) {
        return new CaseSignalsResponse(SUBJECT_ID, band, 0, 0, 0, 0, 0, 0, 0, LocalDateTime.now(), "note");
    }

    private CaseOverviewContextTool.Output context(
            CaseContradictionsResponse contradictions,
            CaseTimelineResponse timeline,
            CaseSignalsResponse signals,
            boolean hasAnyCaseHistory
    ) {
        return new CaseOverviewContextTool.Output(SUBJECT_ID, contradictions, timeline, signals, hasAnyCaseHistory);
    }

    private CaseOverviewContextTool.Output cleanGroundedContext() {
        return context(
                contradictions(List.of(), List.of()),
                timeline(List.of()),
                signals(CaseRiskBand.NORMAL),
                true
        );
    }

    // =========================================================================
    // 1/8/13. GROUNDED CASE, LLM AVAILABLE, ACTUAL TOOL INVOCATION
    // =========================================================================

    @Test
    void explainCaseOverviewReturnsGroundedCompletedResultAndInvokesTheToolForReal() {

        AuthenticatedUser actor = user(SUBJECT_ID);
        CaseOverviewContextTool.Output ctx = cleanGroundedContext();

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(ctx);
        when(llmService.ask(anyString())).thenReturn(
                "{\"explanation\":\"Your case shows no open issues.\",\"recommendedNextStep\":\"No action needed.\"}"
        );

        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.toolsInvoked()).containsExactly(AgentToolName.GET_CASE_OVERVIEW_CONTEXT.name());

        ExplainCaseOverviewResult result = response.result();
        assertThat(result).isNotNull();
        assertThat(result.explanation()).isEqualTo("Your case shows no open issues.");
        assertThat(result.recommendedNextStep()).isEqualTo("No action needed.");
        assertThat(result.aiExplanationStatus()).isEqualTo(AiExplanationStatus.GENERATED);
        assertThat(result.contradictions()).isSameAs(ctx.contradictions());
        assertThat(result.timeline()).isSameAs(ctx.timeline());
        assertThat(result.signals()).isSameAs(ctx.signals());
        assertThat(result.humanReviewRequired()).isFalse();

        verify(caseOverviewContextTool, times(1)).invoke(eq(actor), any());
        verify(llmService, times(1)).ask(anyString());
        verify(agentRunRepository, times(1)).save(any(AgentRun.class));
    }

    // =========================================================================
    // 2. CASE WITH NO CONFLICTS - STILL FULLY GROUNDED, NOT ONE MORE TO REVIEW
    // =========================================================================

    @Test
    void explainCaseOverviewIsGroundedForACleanCaseWithNoConflictsNoAnomaliesAndNormalRisk() {

        AuthenticatedUser actor = user(SUBJECT_ID);
        CaseOverviewContextTool.Output ctx = cleanGroundedContext();

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(ctx);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"Clean case.\",\"recommendedNextStep\":\"None.\"}");

        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        // A genuinely clean case is real, deterministic signal - never
        // treated as an absence of grounding, and never flagged for review.
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.result().humanReviewRequired()).isFalse();
        verify(llmService, times(1)).ask(anyString());
    }

    // =========================================================================
    // 3/14. CASE WITH OPEN CONTRADICTIONS - HUMAN REVIEW TRIGGERED
    // =========================================================================

    @Test
    void explainCaseOverviewFlagsHumanReviewWhenThereIsAnOpenContradiction() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        FactConflictResponse openConflict = new FactConflictResponse(
                500L, SUBJECT_ID, "EMPLOYMENT.CURRENT_EMPLOYER", 1L, 2L,
                ConflictStatus.OPEN, LocalDateTime.now(), null, null, null, null
        );

        CaseOverviewContextTool.Output ctx = context(
                contradictions(List.of(openConflict), List.of()),
                timeline(List.of()),
                signals(CaseRiskBand.MEDIUM),
                true
        );

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(ctx);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"There is an open contradiction.\",\"recommendedNextStep\":\"Resolve it.\"}");

        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        assertThat(response.result().humanReviewRequired()).isTrue();
        assertThat(response.result().contradictions().openContradictions()).containsExactly(openConflict);
    }

    // =========================================================================
    // 4. TIMELINE ANOMALIES ARE PRESERVED UNCHANGED IN THE RESULT
    // =========================================================================

    @Test
    void explainCaseOverviewPreservesTimelineAnomaliesUnchangedInTheResult() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        CaseTimelineEventResponse overlapEvent = new CaseTimelineEventResponse(
                fact("EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT), null, 45L
        );

        PotentialOverlapContradiction overlap = new PotentialOverlapContradiction(
                "EMPLOYMENT.CURRENT_EMPLOYER", 1L, LocalDateTime.now().minusYears(2), LocalDateTime.now().minusYears(1),
                2L, LocalDateTime.now().minusMonths(18), null, 45L, "overlap description"
        );

        CaseOverviewContextTool.Output ctx = context(
                contradictions(List.of(), List.of(overlap)),
                timeline(List.of(overlapEvent)),
                signals(CaseRiskBand.MEDIUM),
                true
        );

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(ctx);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"There is a timeline overlap.\",\"recommendedNextStep\":\"Review it.\"}");

        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        assertThat(response.result().timeline().events()).containsExactly(overlapEvent);
        assertThat(response.result().contradictions().potentialOverlaps()).containsExactly(overlap);
    }

    // =========================================================================
    // 5/14. ELEVATED RISK SIGNAL - HUMAN REVIEW TRIGGERED
    // =========================================================================

    @Test
    void explainCaseOverviewFlagsHumanReviewForAHighRiskBand() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        CaseOverviewContextTool.Output ctx = context(
                contradictions(List.of(), List.of()), timeline(List.of()), signals(CaseRiskBand.HIGH), true
        );

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(ctx);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"Elevated risk.\",\"recommendedNextStep\":\"Case worker should review.\"}");

        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        assertThat(response.result().humanReviewRequired()).isTrue();
        assertThat(response.result().signals().riskBand()).isEqualTo(CaseRiskBand.HIGH);
    }

    @Test
    void explainCaseOverviewDoesNotFlagHumanReviewForALowRiskBandWithNoContradictions() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        CaseOverviewContextTool.Output ctx = context(
                contradictions(List.of(), List.of()), timeline(List.of()), signals(CaseRiskBand.LOW), true
        );

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(ctx);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"Low risk.\",\"recommendedNextStep\":\"None.\"}");

        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        // Only HIGH/CRITICAL trigger review - LOW does not.
        assertThat(response.result().humanReviewRequired()).isFalse();
    }

    // =========================================================================
    // 6. MIXED SIGNALS - CONTRADICTION + TIMELINE OVERLAP + ELEVATED RISK
    // ALL SURFACE TOGETHER, UNCHANGED
    // =========================================================================

    @Test
    void explainCaseOverviewSurfacesMixedSignalsTogetherWithoutLosingAny() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        FactConflictResponse openConflict = new FactConflictResponse(
                501L, SUBJECT_ID, "EDUCATION.DEGREE_AWARDED", 3L, 4L,
                ConflictStatus.OPEN, LocalDateTime.now(), null, null, null, null
        );
        PotentialOverlapContradiction overlap = new PotentialOverlapContradiction(
                "RESIDENCE.ADDRESS", 5L, LocalDateTime.now().minusYears(1), LocalDateTime.now().minusMonths(6),
                6L, LocalDateTime.now().minusMonths(8), null, 60L, "overlap"
        );
        CaseTimelineEventResponse event = new CaseTimelineEventResponse(
                fact("RESIDENCE.ADDRESS", FactCategory.RESIDENCE), 10L, null
        );

        CaseOverviewContextTool.Output ctx = context(
                contradictions(List.of(openConflict), List.of(overlap)),
                timeline(List.of(event)),
                signals(CaseRiskBand.CRITICAL),
                true
        );

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(ctx);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"Multiple signals present.\",\"recommendedNextStep\":\"Case worker review required.\"}");

        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        ExplainCaseOverviewResult result = response.result();
        assertThat(result.contradictions().openContradictions()).containsExactly(openConflict);
        assertThat(result.contradictions().potentialOverlaps()).containsExactly(overlap);
        assertThat(result.timeline().events()).containsExactly(event);
        assertThat(result.signals().riskBand()).isEqualTo(CaseRiskBand.CRITICAL);
        assertThat(result.humanReviewRequired()).isTrue();
    }

    // =========================================================================
    // 7/13/17. NO FACTS AT ALL - INSUFFICIENT EVIDENCE, EMPTY EDGE CASE
    // =========================================================================

    @Test
    void explainCaseOverviewReturnsInsufficientEvidenceOnlyWhenTheSubjectHasNoCaseHistoryAtAll() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        CaseOverviewContextTool.Output ctx = context(
                contradictions(List.of(), List.of()), timeline(List.of()), signals(CaseRiskBand.NORMAL), false
        );

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(ctx);

        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.INSUFFICIENT_EVIDENCE);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.INSUFFICIENT_EVIDENCE);
        assertThat(response.result().explanation()).contains("Insufficient evidence");
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.NOT_ATTEMPTED);
        assertThat(response.result().humanReviewRequired()).isTrue();

        // Deterministic hallucination defense: the LLM is never called at all
        // when there is genuinely nothing to explain.
        verify(llmService, never()).ask(anyString());
    }

    // =========================================================================
    // 9/10/11. GRACEFUL AI DEGRADATION - the deterministic overview must
    // NEVER disappear merely because the LLM failed.
    // =========================================================================

    @Test
    void explainCaseOverviewDegradesGracefullyWhenOpenAiQuotaIsExhausted() {
        assertGracefulDegradationOnLlmCallFailure(new RuntimeException(
                "429 Too Many Requests - insufficient_quota: credit_balance_exhausted"
        ));
    }

    @Test
    void explainCaseOverviewDegradesGracefullyOnLlmTimeout() {
        assertGracefulDegradationOnLlmCallFailure(new RuntimeException("Read timed out"));
    }

    @Test
    void explainCaseOverviewDegradesGracefullyWhenTheLlmProviderIsUnavailable() {
        assertGracefulDegradationOnLlmCallFailure(new RuntimeException(
                "Connection refused: api.openai.com:443 (internal provider detail)"
        ));
    }

    @Test
    void explainCaseOverviewDegradesGracefullyOnAnUnexpectedLlmException() {
        assertGracefulDegradationOnLlmCallFailure(new IllegalStateException("Failed to generate AI response"));
    }

    private void assertGracefulDegradationOnLlmCallFailure(RuntimeException llmFailure) {

        AuthenticatedUser actor = user(SUBJECT_ID);
        CaseOverviewContextTool.Output ctx = cleanGroundedContext();

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(ctx);
        when(llmService.ask(anyString())).thenThrow(llmFailure);

        // No exception - the deterministic overview succeeds regardless.
        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.errorMessage()).isNull();

        // ---- 11. DETERMINISTIC DATA PRESERVED AFTER LLM FAILURE ----
        ExplainCaseOverviewResult result = response.result();
        assertThat(result).isNotNull();
        assertThat(result.aiExplanationStatus()).isEqualTo(AiExplanationStatus.UNAVAILABLE);
        assertThat(result.explanation()).isNull();
        assertThat(result.recommendedNextStep()).isEqualTo("No action is currently required - your case overview shows no open issues.");

        // ---- 12. AI EXPLANATION NEVER ALTERS DETERMINISTIC STATUS ----
        assertThat(result.contradictions()).isSameAs(ctx.contradictions());
        assertThat(result.timeline()).isSameAs(ctx.timeline());
        assertThat(result.signals()).isSameAs(ctx.signals());
        assertThat(result.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);

        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, times(1)).save(captor.capture());

        AgentRun savedRun = captor.getValue();
        assertThat(savedRun.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(savedRun.getErrorMessage()).isNull();
        assertThat(savedRun.getPathwayAssessmentId()).isNull();
        assertThat(savedRun.getRequirementId()).isNull();

        String leakyDetail = llmFailure.getMessage();
        assertThat(savedRun.getResultJson()).doesNotContain(leakyDetail);
        assertThat(savedRun.getReasoningSummary()).doesNotContain(leakyDetail);
    }

    @Test
    void explainCaseOverviewDegradesGracefullyWhenTheLlmReturnsUnparsableText() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(cleanGroundedContext());
        when(llmService.ask(anyString())).thenReturn("this is not json at all");

        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.FAILED);
        assertThat(response.result().explanation()).isNull();
    }

    @Test
    void explainCaseOverviewDegradesGracefullyWhenTheLlmReturnsAnEmptyResponse() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(cleanGroundedContext());
        when(llmService.ask(anyString())).thenReturn("");

        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.FAILED);
        assertThat(response.result().explanation()).isNull();
    }

    @Test
    void explainCaseOverviewParsesMarkdownFencedJsonFromTheLlm() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(cleanGroundedContext());
        when(llmService.ask(anyString())).thenReturn(
                "```json\n{\"explanation\":\"Fenced explanation.\",\"recommendedNextStep\":\"Fenced next step.\"}\n```"
        );

        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().explanation()).isEqualTo("Fenced explanation.");
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.GENERATED);
    }

    // =========================================================================
    // 15. UNAUTHORIZED CASE ACCESS - NO RUN EVER CREATED
    // =========================================================================

    @Test
    void explainCaseOverviewPropagatesUnauthorizedCaseAccessAndNeverCreatesARun() {

        AuthenticatedUser stranger = user(999L);

        when(caseOverviewContextTool.invoke(eq(stranger), any()))
                .thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> service.explainCaseOverview(stranger, request()))
                .isInstanceOf(AccessDeniedException.class);

        verify(agentRunRepository, never()).save(any());
        verify(llmService, never()).ask(anyString());
    }

    @Test
    void explainCaseOverviewNeverPersistsARawInternalExceptionMessageFromAnUnexpectedFailure() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenThrow(new RuntimeException(
                "ORA-00942: table or view does not exist at "
                        + "com.godfrey.ai_immigration_document_analyzer.internal.SecretRepository.query(SecretRepository.java:42)"
        ));

        assertThatThrownBy(() -> service.explainCaseOverview(actor, request()))
                .isInstanceOf(RuntimeException.class);

        // No AgentRun is ever created for a failure at the authorization/tool
        // boundary - identical to the other three agents' behavior.
        verify(agentRunRepository, never()).save(any());
    }

    // =========================================================================
    // 16. RETRY BEHAVIOUR - a second invocation creates a NEW AgentRun and
    // never mutates or re-derives the deterministic data of the first.
    // =========================================================================

    @Test
    void explainCaseOverviewRetryCreatesANewRunWithoutMutatingDeterministicData() {

        AuthenticatedUser actor = user(SUBJECT_ID);
        CaseOverviewContextTool.Output ctx = cleanGroundedContext();

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(ctx);
        when(llmService.ask(anyString()))
                .thenThrow(new RuntimeException("429 insufficient_quota"))
                .thenReturn("{\"explanation\":\"Recovered.\",\"recommendedNextStep\":\"None.\"}");

        CaseOverviewAgentRunResponse first = service.explainCaseOverview(actor, request());
        CaseOverviewAgentRunResponse second = service.explainCaseOverview(actor, request());

        assertThat(first.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.UNAVAILABLE);
        assertThat(second.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.GENERATED);

        // Both runs saw the exact same, unmutated deterministic data - a
        // retry never recomputes or alters it.
        assertThat(first.result().signals()).isSameAs(ctx.signals());
        assertThat(second.result().signals()).isSameAs(ctx.signals());

        verify(caseOverviewContextTool, times(2)).invoke(eq(actor), any());
        verify(agentRunRepository, times(2)).save(any(AgentRun.class));
    }

    // =========================================================================
    // 18. NO HALLUCINATED DETERMINISTIC SIGNALS - the prompt sent to the LLM
    // carries only the already-computed data, and the parsed LLM output can
    // only ever populate the explanation/next-step fields, never the
    // deterministic ones.
    // =========================================================================

    @Test
    void explainCaseOverviewFeedsOnlyDeterministicDataIntoThePromptAsGroundingContext() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        FactConflictResponse openConflict = new FactConflictResponse(
                502L, SUBJECT_ID, "TRAVEL_HISTORY.LAST_EXIT", 7L, 8L,
                ConflictStatus.OPEN, LocalDateTime.now(), null, null, null, null
        );
        CaseOverviewContextTool.Output ctx = context(
                contradictions(List.of(openConflict), List.of()), timeline(List.of()), signals(CaseRiskBand.MEDIUM), true
        );

        when(caseOverviewContextTool.invoke(eq(actor), any())).thenReturn(ctx);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        when(llmService.ask(promptCaptor.capture()))
                .thenReturn("{\"explanation\":\"E\",\"recommendedNextStep\":\"N\"}");

        CaseOverviewAgentRunResponse response = service.explainCaseOverview(actor, request());

        assertThat(promptCaptor.getValue())
                .contains("TRAVEL_HISTORY.LAST_EXIT")
                .contains("MEDIUM")
                .contains("Never invent a fact, conflict, timeline event")
                .contains("not something you may contradict or override");

        // The LLM output never overwrote the deterministic signals/band.
        assertThat(response.result().signals().riskBand()).isEqualTo(CaseRiskBand.MEDIUM);
        assertThat(response.result().contradictions().openContradictions()).containsExactly(openConflict);
    }

    // =========================================================================
    // AGENT RUN LIFECYCLE / GET RUN
    // =========================================================================

    @Test
    void getRunAuthorizesThroughTheExistingFactAuthorizationBoundary() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        ExplainCaseOverviewResult persistedResult = new ExplainCaseOverviewResult(
                SUBJECT_ID, LocalDateTime.now(),
                contradictions(List.of(), List.of()), timeline(List.of()), signals(CaseRiskBand.NORMAL),
                "An explanation.", AgentGroundingState.GROUNDED, AiExplanationStatus.GENERATED,
                "Next step.", false
        );

        AgentRun run = AgentRun.builder()
                .id(88L).subjectUserId(SUBJECT_ID).requestedByUserId(SUBJECT_ID)
                .status(AgentRunStatus.COMPLETED)
                .resultJson(writeJson(persistedResult))
                .build();

        when(agentRunRepository.findById(88L)).thenReturn(Optional.of(run));

        CaseOverviewAgentRunResponse response = service.getRun(actor, 88L);

        verify(factAuthorizationService).assertCanView(eq(actor), eq(SUBJECT_ID), any(), any(), anyString());
        assertThat(response.result().explanation()).isEqualTo("An explanation.");
    }

    @Test
    void getRunPropagatesDenialForAnUnauthorizedActor() {

        AuthenticatedUser stranger = user(999L);

        AgentRun run = AgentRun.builder().id(88L).subjectUserId(SUBJECT_ID).requestedByUserId(SUBJECT_ID)
                .status(AgentRunStatus.COMPLETED).build();

        when(agentRunRepository.findById(88L)).thenReturn(Optional.of(run));
        doThrow(new AccessDeniedException("denied")).when(factAuthorizationService)
                .assertCanView(eq(stranger), eq(SUBJECT_ID), any(), any(), anyString());

        assertThatThrownBy(() -> service.getRun(stranger, 88L))
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
