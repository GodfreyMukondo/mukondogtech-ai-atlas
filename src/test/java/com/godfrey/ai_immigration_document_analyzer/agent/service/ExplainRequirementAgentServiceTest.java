package com.godfrey.ai_immigration_document_analyzer.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.AgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainRequirementRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainRequirementResult;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.repository.AgentRunRepository;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.AgentToolName;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.EvidenceGraphContextTool;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.RequirementExplanationTool;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceGraphResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.GraphNodeResponse;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.GraphNodeType;
import com.godfrey.ai_immigration_document_analyzer.exception.AiServiceException;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactEvidenceResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.EvidenceSourceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;
import com.godfrey.ai_immigration_document_analyzer.service.LlmService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
 * Unit tests for {@link ExplainRequirementAgentService} - Phase 5.1's
 * orchestrator. The two tools and the LLM are mocked (the smallest possible
 * boundary); every assertion here is about the orchestrator's OWN logic:
 * grounding-before-LLM, never fabricating ids, recording exactly what
 * happened, and failing safely.
 */
@ExtendWith(MockitoExtension.class)
class ExplainRequirementAgentServiceTest {

    private static final Long SUBJECT_ID = 10L;
    private static final Long ASSESSMENT_ID = 500L;
    private static final Long REQUIREMENT_ID = 12L;
    private static final Long EVALUATION_ID = 4001L;

    @Mock
    private RequirementExplanationTool requirementExplanationTool;

    @Mock
    private EvidenceGraphContextTool evidenceGraphContextTool;

    @Mock
    private LlmService llmService;

    @Mock
    private AgentRunRepository agentRunRepository;

    @Mock
    private FactAuthorizationService factAuthorizationService;

    private ObjectMapper objectMapper;

    private ExplainRequirementAgentService service;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        service = new ExplainRequirementAgentService(
                requirementExplanationTool, evidenceGraphContextTool, llmService,
                agentRunRepository, factAuthorizationService, objectMapper
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

    private ExplainRequirementRequest request() {
        ExplainRequirementRequest request = new ExplainRequirementRequest();
        request.setPathwayAssessmentId(ASSESSMENT_ID);
        request.setRequirementId(REQUIREMENT_ID);
        return request;
    }

    private FactEvidenceResponse evidence(Long id) {
        return new FactEvidenceResponse(id, EvidenceSourceType.DOCUMENT, 55L, "locator", "snippet", LocalDateTime.now());
    }

    private ExplanationResponse.FactExplanation fact(Long factId, List<FactEvidenceResponse> evidence) {
        return new ExplanationResponse.FactExplanation(
                factId, "EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT, "Acme Corp",
                FactProvenanceType.DOCUMENT_EXTRACTION, true, 0.9, FactConfidenceLevel.HIGH, evidence
        );
    }

    private RequirementExplanationTool.Output toolOutput(
            List<ExplanationResponse.FactExplanation> facts,
            List<String> missingKeys,
            List<Long> conflicts,
            boolean mandatory,
            RequirementEvaluationOutcome outcome,
            Long evaluationId
    ) {
        return new RequirementExplanationTool.Output(
                ASSESSMENT_ID, "TEST_PATHWAY", "Test Pathway", SUBJECT_ID,
                REQUIREMENT_ID, "TEST.REQ", "Test Requirement", RequirementType.EXPERIENCE, mandatory,
                evaluationId, outcome, CaseRequirementSupportStatus.SATISFIED, EvaluationCertaintyLevel.HIGH,
                88L, "Testland Authority", RegulatoryVerificationStatus.UNVERIFIED_INGESTION,
                facts, missingKeys, conflicts, "existing explanation", "evidence guidance"
        );
    }

    private EvidenceGraphResponse sampleGraph() {
        GraphNodeResponse node = new GraphNodeResponse("fact:1", GraphNodeType.FACT, "Fact", java.util.Map.of());
        return new EvidenceGraphResponse(List.of(node), List.of());
    }

    // =========================================================================
    // 1. SUCCESSFUL EXPLANATION / STRUCTURED RESULT / PROVENANCE / ACTUAL INVOCATION
    // =========================================================================

    @Test
    void explainRequirementReturnsGroundedCompletedResultAndInvokesBothToolsForReal() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        FactEvidenceResponse ev = evidence(501L);
        RequirementExplanationTool.Output toolOutput = toolOutput(
                List.of(fact(1L, List.of(ev))), List.of(), List.of(), true,
                RequirementEvaluationOutcome.SATISFIED, EVALUATION_ID
        );

        when(requirementExplanationTool.invoke(eq(actor), any())).thenReturn(toolOutput);

        EvidenceGraphResponse graph = sampleGraph();
        when(evidenceGraphContextTool.invoke(eq(actor), any())).thenReturn(graph);

        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"Grounded explanation.\",\"recommendedNextStep\":\"Do nothing further.\"}");

        AgentRunResponse response = service.explainRequirement(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.toolsInvoked()).containsExactly(
                AgentToolName.GET_REQUIREMENT_EXPLANATION.name(),
                AgentToolName.GET_EVIDENCE_GRAPH_CONTEXT.name()
        );

        ExplainRequirementResult result = response.result();
        assertThat(result).isNotNull();
        assertThat(result.explanation()).isEqualTo("Grounded explanation.");
        assertThat(result.recommendedNextStep()).isEqualTo("Do nothing further.");
        assertThat(result.currentStatus()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(result.factsConsidered()).containsExactly(toolOutput.contributingFacts().get(0));
        assertThat(result.evidenceConsidered()).containsExactly(ev);
        assertThat(result.provenance()).isSameAs(graph);
        assertThat(result.humanReviewRequired()).isFalse();

        // Fix 1: the existing, application-derived explanation is preserved
        // as its own field - never conflated with the LLM's explanation.
        assertThat(result.existingExplanation()).isEqualTo("existing explanation");
        assertThat(result.explanation()).isNotEqualTo(result.existingExplanation());

        // Fix 2: regulatory context is a first-class part of the result.
        assertThat(result.regulatoryVersionId()).isEqualTo(88L);
        assertThat(result.regulatorySourceAuthority()).isEqualTo("Testland Authority");
        assertThat(result.regulatoryVerificationStatus()).isEqualTo(RegulatoryVerificationStatus.UNVERIFIED_INGESTION);

        // Real invocation, not a simulated log entry.
        verify(requirementExplanationTool, times(1)).invoke(eq(actor), any());
        verify(evidenceGraphContextTool, times(1)).invoke(eq(actor), any());
        verify(llmService, times(1)).ask(anyString());
        verify(agentRunRepository, times(1)).save(any(AgentRun.class));
    }

    @Test
    void explainRequirementFeedsTheExistingApplicationExplanationIntoTheLlmPromptAsGroundingContext() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementExplanationTool.Output toolOutput = toolOutput(
                List.of(fact(1L, List.of())), List.of(), List.of(), true,
                RequirementEvaluationOutcome.SATISFIED, EVALUATION_ID
        );

        when(requirementExplanationTool.invoke(eq(actor), any())).thenReturn(toolOutput);
        when(evidenceGraphContextTool.invoke(eq(actor), any())).thenReturn(sampleGraph());

        org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        when(llmService.ask(promptCaptor.capture()))
                .thenReturn("{\"explanation\":\"E\",\"recommendedNextStep\":\"N\"}");

        service.explainRequirement(actor, request());

        assertThat(promptCaptor.getValue())
                .contains("EXISTING APPLICATION EXPLANATION")
                .contains("existing explanation")
                .contains("not a fact you may contradict");
    }

    @Test
    void explainRequirementNeverLetsTheLlmIntroduceAFactOrEvidenceIdOfItsOwn() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        FactEvidenceResponse ev = evidence(501L);
        RequirementExplanationTool.Output toolOutput = toolOutput(
                List.of(fact(1L, List.of(ev))), List.of(), List.of(), true,
                RequirementEvaluationOutcome.SATISFIED, EVALUATION_ID
        );

        when(requirementExplanationTool.invoke(eq(actor), any())).thenReturn(toolOutput);
        when(evidenceGraphContextTool.invoke(eq(actor), any())).thenReturn(sampleGraph());

        // The LLM's prose mentions a fact/evidence that was never in the tool output.
        when(llmService.ask(anyString())).thenReturn(
                "{\"explanation\":\"Also see FAKE.FACT and evidence #999, which strongly support this.\","
                        + "\"recommendedNextStep\":\"None.\"}"
        );

        AgentRunResponse response = service.explainRequirement(actor, request());

        // The structured, code-populated fields are unaffected by anything the LLM's text claims.
        assertThat(response.result().factsConsidered()).extracting(ExplanationResponse.FactExplanation::factId)
                .containsExactly(1L);
        assertThat(response.result().evidenceConsidered()).extracting(FactEvidenceResponse::id)
                .containsExactly(501L);
    }

    // =========================================================================
    // 2. MISSING / INSUFFICIENT EVIDENCE - HALLUCINATION RESISTANCE
    // =========================================================================

    @Test
    void explainRequirementReturnsInsufficientEvidenceWithoutEverInvokingTheLlm() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementExplanationTool.Output toolOutput = toolOutput(
                List.of(), List.of("EMPLOYMENT.CURRENT_EMPLOYER"), List.of(), true,
                RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE, null
        );

        when(requirementExplanationTool.invoke(eq(actor), any())).thenReturn(toolOutput);

        AgentRunResponse response = service.explainRequirement(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.INSUFFICIENT_EVIDENCE);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.INSUFFICIENT_EVIDENCE);
        assertThat(response.result().explanation()).contains("Insufficient evidence");
        assertThat(response.result().recommendedNextStep()).contains("EMPLOYMENT.CURRENT_EMPLOYER");
        assertThat(response.result().humanReviewRequired()).isTrue(); // mandatory + no evidence

        // Deterministic hallucination defense: the LLM is never called at all.
        verify(llmService, never()).ask(anyString());
        // No evaluation was resolved, so the graph tool is never called either.
        verify(evidenceGraphContextTool, never()).invoke(any(), any());
    }

    // =========================================================================
    // 3. CONFLICTING FACTS
    // =========================================================================

    @Test
    void explainRequirementFlagsHumanReviewAndStillExplainsWhenConflictsExist() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementExplanationTool.Output toolOutput = toolOutput(
                List.of(), List.of(), List.of(70L), true,
                RequirementEvaluationOutcome.CONFLICTED, EVALUATION_ID
        );

        when(requirementExplanationTool.invoke(eq(actor), any())).thenReturn(toolOutput);
        when(evidenceGraphContextTool.invoke(eq(actor), any())).thenReturn(sampleGraph());
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"Conflicting facts exist.\",\"recommendedNextStep\":\"Resolve the conflict.\"}");

        AgentRunResponse response = service.explainRequirement(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().conflicts()).containsExactly(70L);
        assertThat(response.result().humanReviewRequired()).isTrue();
    }

    // =========================================================================
    // 5/6. UNAUTHORIZED CASE / REQUIREMENT - NO RUN EVER CREATED
    // =========================================================================

    @Test
    void explainRequirementPropagatesUnauthorizedCaseAccessAndNeverCreatesARun() {

        AuthenticatedUser stranger = user(999L);

        when(requirementExplanationTool.invoke(eq(stranger), any()))
                .thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> service.explainRequirement(stranger, request()))
                .isInstanceOf(AccessDeniedException.class);

        verify(agentRunRepository, never()).save(any());
        verify(evidenceGraphContextTool, never()).invoke(any(), any());
        verify(llmService, never()).ask(anyString());
    }

    @Test
    void explainRequirementPropagatesUnauthorizedRequirementAccessAndNeverCreatesARun() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(requirementExplanationTool.invoke(eq(actor), any()))
                .thenThrow(new ResourceNotFoundException("This requirement is not part of the specified pathway assessment."));

        assertThatThrownBy(() -> service.explainRequirement(actor, request()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(agentRunRepository, never()).save(any());
    }

    // =========================================================================
    // 9/10. TOOL FAILURE / LLM FAILURE - FAIL SAFELY, RECORD THE RUN
    // =========================================================================

    @Test
    void explainRequirementRecordsAFailedRunWhenTheEvidenceGraphToolFails() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementExplanationTool.Output toolOutput = toolOutput(
                List.of(fact(1L, List.of())), List.of(), List.of(), true,
                RequirementEvaluationOutcome.SATISFIED, EVALUATION_ID
        );

        when(requirementExplanationTool.invoke(eq(actor), any())).thenReturn(toolOutput);
        when(evidenceGraphContextTool.invoke(eq(actor), any()))
                .thenThrow(new RuntimeException("evidence graph unavailable"));

        assertThatThrownBy(() -> service.explainRequirement(actor, request()))
                .isInstanceOf(AiServiceException.class);

        verify(llmService, never()).ask(anyString());

        org.mockito.ArgumentCaptor<AgentRun> captor = org.mockito.ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, times(1)).save(captor.capture());

        AgentRun savedRun = captor.getValue();
        assertThat(savedRun.getStatus()).isEqualTo(AgentRunStatus.FAILED);
        assertThat(savedRun.getSubjectUserId()).isEqualTo(SUBJECT_ID);

        // Fix 3: the raw internal exception message is never persisted -
        // only a fixed, generic, safe statement. The real detail is only
        // ever logged server-side (verified separately, below).
        assertThat(savedRun.getErrorMessage())
                .doesNotContain("evidence graph unavailable")
                .isEqualTo("An unexpected error occurred while generating this explanation. Please try again later.");
    }

    @Test
    void explainRequirementNeverPersistsARawInternalExceptionMessage() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementExplanationTool.Output toolOutput = toolOutput(
                List.of(fact(1L, List.of())), List.of(), List.of(), true,
                RequirementEvaluationOutcome.SATISFIED, EVALUATION_ID
        );

        when(requirementExplanationTool.invoke(eq(actor), any())).thenReturn(toolOutput);

        // Simulates a leaky, unexpected internal failure - the kind of raw
        // detail that must never reach a persisted/returned field.
        when(evidenceGraphContextTool.invoke(eq(actor), any())).thenThrow(
                new RuntimeException(
                        "ORA-00942: table or view does not exist at "
                                + "com.godfrey.ai_immigration_document_analyzer.internal.SecretRepository.query(SecretRepository.java:42)"
                )
        );

        assertThatThrownBy(() -> service.explainRequirement(actor, request()))
                .isInstanceOf(AiServiceException.class);

        org.mockito.ArgumentCaptor<AgentRun> captor = org.mockito.ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, times(1)).save(captor.capture());

        String persistedMessage = captor.getValue().getErrorMessage();

        assertThat(persistedMessage)
                .doesNotContain("ORA-00942")
                .doesNotContain("SecretRepository")
                .doesNotContain("com.godfrey")
                .isEqualTo("An unexpected error occurred while generating this explanation. Please try again later.");
    }

    @Test
    void explainRequirementPreservesItsOwnKnownSafeMessageWhenTheLlmIsUnavailable() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementExplanationTool.Output toolOutput = toolOutput(
                List.of(fact(1L, List.of())), List.of(), List.of(), true,
                RequirementEvaluationOutcome.SATISFIED, EVALUATION_ID
        );

        when(requirementExplanationTool.invoke(eq(actor), any())).thenReturn(toolOutput);
        when(evidenceGraphContextTool.invoke(eq(actor), any())).thenReturn(sampleGraph());
        when(llmService.ask(anyString())).thenThrow(new RuntimeException(
                "Connection refused: api.openai.com:443 (internal provider detail)"
        ));

        assertThatThrownBy(() -> service.explainRequirement(actor, request()))
                .isInstanceOf(AiServiceException.class);

        org.mockito.ArgumentCaptor<AgentRun> captor = org.mockito.ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, times(1)).save(captor.capture());

        // The service's OWN pre-written, already-safe message is kept
        // (never the raw provider error it wraps).
        assertThat(captor.getValue().getErrorMessage())
                .isEqualTo("The AI explanation service is currently unavailable.")
                .doesNotContain("api.openai.com")
                .doesNotContain("Connection refused");
    }

    @Test
    void explainRequirementRecordsAFailedRunWhenTheLlmFails() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementExplanationTool.Output toolOutput = toolOutput(
                List.of(fact(1L, List.of())), List.of(), List.of(), true,
                RequirementEvaluationOutcome.SATISFIED, EVALUATION_ID
        );

        when(requirementExplanationTool.invoke(eq(actor), any())).thenReturn(toolOutput);
        when(evidenceGraphContextTool.invoke(eq(actor), any())).thenReturn(sampleGraph());
        when(llmService.ask(anyString())).thenThrow(new RuntimeException("model unavailable"));

        assertThatThrownBy(() -> service.explainRequirement(actor, request()))
                .isInstanceOf(AiServiceException.class);

        org.mockito.ArgumentCaptor<AgentRun> captor = org.mockito.ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AgentRunStatus.FAILED);
    }

    @Test
    void explainRequirementFailsSafelyWhenTheLlmReturnsUnparsableText() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementExplanationTool.Output toolOutput = toolOutput(
                List.of(fact(1L, List.of())), List.of(), List.of(), true,
                RequirementEvaluationOutcome.SATISFIED, EVALUATION_ID
        );

        when(requirementExplanationTool.invoke(eq(actor), any())).thenReturn(toolOutput);
        when(evidenceGraphContextTool.invoke(eq(actor), any())).thenReturn(sampleGraph());
        when(llmService.ask(anyString())).thenReturn("this is not json at all");

        assertThatThrownBy(() -> service.explainRequirement(actor, request()))
                .isInstanceOf(AiServiceException.class);

        org.mockito.ArgumentCaptor<AgentRun> captor = org.mockito.ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AgentRunStatus.FAILED);
    }

    @Test
    void explainRequirementParsesMarkdownFencedJsonFromTheLlm() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementExplanationTool.Output toolOutput = toolOutput(
                List.of(fact(1L, List.of())), List.of(), List.of(), true,
                RequirementEvaluationOutcome.SATISFIED, EVALUATION_ID
        );

        when(requirementExplanationTool.invoke(eq(actor), any())).thenReturn(toolOutput);
        when(evidenceGraphContextTool.invoke(eq(actor), any())).thenReturn(sampleGraph());
        when(llmService.ask(anyString())).thenReturn(
                "```json\n{\"explanation\":\"Fenced explanation.\",\"recommendedNextStep\":\"Fenced next step.\"}\n```"
        );

        AgentRunResponse response = service.explainRequirement(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().explanation()).isEqualTo("Fenced explanation.");
    }

    // =========================================================================
    // AGENT RUN LIFECYCLE / GET RUN
    // =========================================================================

    @Test
    void getRunAuthorizesThroughTheExistingFactAuthorizationBoundary() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        ExplainRequirementResult persistedResult = new ExplainRequirementResult(
                ASSESSMENT_ID, REQUIREMENT_ID, "TEST.REQ", "Test Requirement",
                RequirementEvaluationOutcome.SATISFIED, CaseRequirementSupportStatus.SATISFIED,
                "The existing application explanation.", "An explanation.", List.of(), List.of(), List.of(), List.of(),
                sampleGraph(), 88L, "Testland Authority", RegulatoryVerificationStatus.UNVERIFIED_INGESTION,
                AgentGroundingState.GROUNDED, "Next step.", false
        );

        AgentRun run = AgentRun.builder()
                .id(77L).subjectUserId(SUBJECT_ID).requestedByUserId(SUBJECT_ID)
                .status(AgentRunStatus.COMPLETED)
                .resultJson(writeJson(persistedResult))
                .build();

        when(agentRunRepository.findById(77L)).thenReturn(Optional.of(run));

        AgentRunResponse response = service.getRun(actor, 77L);

        verify(factAuthorizationService).assertCanView(eq(actor), eq(SUBJECT_ID), any(), any(), anyString());
        assertThat(response.result().explanation()).isEqualTo("An explanation.");
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
