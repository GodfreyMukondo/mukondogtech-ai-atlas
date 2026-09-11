package com.godfrey.ai_immigration_document_analyzer.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainPathwayAssessmentRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainPathwayAssessmentResult;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.PathwayAssessmentAgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AiExplanationStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.repository.AgentRunRepository;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.AgentToolName;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.PathwayIntelligenceContextTool;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIntelligenceResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIssueSeverity;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseOverviewSummaryResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseReadinessResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRiskBand;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.EvidenceNecessity;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.MissingEvidenceItemResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.RequirementEvidenceMatrixRowResponse;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
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
 * Unit tests for {@link ExplainPathwayAssessmentAgentService} - Phase 5.2's
 * whole-pathway orchestrator. The one tool and the LLM are mocked (the
 * smallest possible boundary); every assertion here is about the
 * orchestrator's OWN logic: grounding-before-LLM (with the pathway-level
 * definition of grounding - see the dedicated section below), never
 * fabricating a requirement/outcome, recording exactly what happened, and
 * failing safely.
 */
@ExtendWith(MockitoExtension.class)
class ExplainPathwayAssessmentAgentServiceTest {

    private static final Long SUBJECT_ID = 10L;
    private static final Long ASSESSMENT_ID = 500L;
    private static final Long REQUIREMENT_ID = 12L;

    @Mock
    private PathwayIntelligenceContextTool pathwayIntelligenceContextTool;

    @Mock
    private LlmService llmService;

    @Mock
    private AgentRunRepository agentRunRepository;

    @Mock
    private FactAuthorizationService factAuthorizationService;

    private ObjectMapper objectMapper;

    private ExplainPathwayAssessmentAgentService service;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        service = new ExplainPathwayAssessmentAgentService(
                pathwayIntelligenceContextTool, llmService, agentRunRepository, factAuthorizationService, objectMapper
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

    private ExplainPathwayAssessmentRequest request() {
        ExplainPathwayAssessmentRequest request = new ExplainPathwayAssessmentRequest();
        request.setPathwayAssessmentId(ASSESSMENT_ID);
        return request;
    }

    private RequirementEvidenceMatrixRowResponse row(
            CaseRequirementSupportStatus supportStatus,
            RequirementEvaluationOutcome outcome,
            boolean mandatory,
            List<String> missingKeys,
            List<Long> conflicts
    ) {
        return new RequirementEvidenceMatrixRowResponse(
                REQUIREMENT_ID, "TEST.REQ", "Test Requirement", RequirementType.EXPERIENCE, mandatory,
                supportStatus, outcome, EvaluationCertaintyLevel.HIGH, 88L, "Testland Authority",
                RegulatoryVerificationStatus.UNVERIFIED_INGESTION,
                List.of(), missingKeys, conflicts, "existing per-requirement explanation"
        );
    }

    private CaseIntelligenceResponse caseIntelligence(
            RequirementEvaluationOutcome overallOutcome,
            List<RequirementEvidenceMatrixRowResponse> matrix,
            List<MissingEvidenceItemResponse> missingEvidence
    ) {
        return new CaseIntelligenceResponse(
                ASSESSMENT_ID, SUBJECT_ID, "TEST_PATHWAY", "Test Pathway",
                overallOutcome,
                new CaseOverviewSummaryResponse(80.0, 90.0, 85.0, missingEvidence.size(), 0, 0, 0,
                        CaseRiskBand.NORMAL, "note"),
                new CaseReadinessResponse(80.0, 90.0, 100.0, 85.0, List.of()),
                matrix, missingEvidence, "Evidence guidance text", LocalDateTime.now()
        );
    }

    // =========================================================================
    // 1. SUCCESSFUL NARRATIVE / STRUCTURED RESULT / PROVENANCE / ACTUAL INVOCATION
    // =========================================================================

    @Test
    void explainPathwayAssessmentReturnsGroundedCompletedResultAndInvokesTheToolForReal() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementEvidenceMatrixRowResponse satisfiedRow = row(
                CaseRequirementSupportStatus.SATISFIED, RequirementEvaluationOutcome.SATISFIED, true, List.of(), List.of()
        );

        CaseIntelligenceResponse context = caseIntelligence(
                RequirementEvaluationOutcome.SATISFIED, List.of(satisfiedRow), List.of()
        );

        when(pathwayIntelligenceContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"Grounded pathway narrative.\",\"recommendedNextStep\":\"Do nothing further.\"}");

        PathwayAssessmentAgentRunResponse response = service.explainPathwayAssessment(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.toolsInvoked()).containsExactly(AgentToolName.GET_PATHWAY_INTELLIGENCE_CONTEXT.name());

        ExplainPathwayAssessmentResult result = response.result();
        assertThat(result).isNotNull();
        assertThat(result.explanation()).isEqualTo("Grounded pathway narrative.");
        assertThat(result.recommendedNextStep()).isEqualTo("Do nothing further.");
        assertThat(result.aiExplanationStatus()).isEqualTo(AiExplanationStatus.GENERATED);
        assertThat(result.overallOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(result.requirementSummaries()).containsExactly(satisfiedRow);
        assertThat(result.readiness()).isSameAs(context.readiness());
        assertThat(result.missingEvidence()).isEmpty();
        assertThat(result.humanReviewRequired()).isFalse();

        // Provenance preserved down to the per-requirement level - the whole
        // matrix row (facts, missing keys, conflicts, regulatory source) is
        // carried through untouched, never collapsed away.
        assertThat(result.requirementSummaries().get(0).regulatoryVersionId()).isEqualTo(88L);
        assertThat(result.requirementSummaries().get(0).regulatorySourceAuthority()).isEqualTo("Testland Authority");

        verify(pathwayIntelligenceContextTool, times(1)).invoke(eq(actor), any());
        verify(llmService, times(1)).ask(anyString());
        verify(agentRunRepository, times(1)).save(any(AgentRun.class));
    }

    @Test
    void explainPathwayAssessmentFeedsPerRequirementDataIntoThePromptAsGroundingContext() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementEvidenceMatrixRowResponse missingRow = row(
                CaseRequirementSupportStatus.MISSING, RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE, true,
                List.of("EMPLOYMENT.CURRENT_EMPLOYER"), List.of()
        );

        CaseIntelligenceResponse context = caseIntelligence(
                RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE, List.of(missingRow), List.of()
        );

        when(pathwayIntelligenceContextTool.invoke(eq(actor), any())).thenReturn(context);

        org.mockito.ArgumentCaptor<String> promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        when(llmService.ask(promptCaptor.capture()))
                .thenReturn("{\"explanation\":\"E\",\"recommendedNextStep\":\"N\"}");

        service.explainPathwayAssessment(actor, request());

        assertThat(promptCaptor.getValue())
                .contains("Test Requirement")
                .contains("existing per-requirement explanation")
                .contains("not a fact you may contradict")
                .contains("Never invent a requirement");
    }

    // =========================================================================
    // 2. GROUNDING - MISSING/UNSATISFIED REQUIREMENTS ARE STILL GROUNDED
    // =========================================================================

    @Test
    void explainPathwayAssessmentIsGroundedWhenEveryRequirementIsMissingOrNotSatisfied() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementEvidenceMatrixRowResponse missingRow = row(
                CaseRequirementSupportStatus.MISSING, RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE, true,
                List.of("EMPLOYMENT.CURRENT_EMPLOYER"), List.of()
        );
        RequirementEvidenceMatrixRowResponse notSatisfiedRow = row(
                CaseRequirementSupportStatus.NOT_SATISFIED, RequirementEvaluationOutcome.NOT_SATISFIED, true,
                List.of(), List.of()
        );

        MissingEvidenceItemResponse missingEvidenceItem = new MissingEvidenceItemResponse(
                REQUIREMENT_ID, "TEST.REQ", "Test Requirement", true, EvidenceNecessity.REQUIRED,
                CaseIssueSeverity.CRITICAL, List.of("EMPLOYMENT.CURRENT_EMPLOYER"), "reason"
        );

        CaseIntelligenceResponse context = caseIntelligence(
                RequirementEvaluationOutcome.NOT_SATISFIED,
                List.of(missingRow, notSatisfiedRow),
                List.of(missingEvidenceItem)
        );

        when(pathwayIntelligenceContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"Two requirements are unmet.\",\"recommendedNextStep\":\"Provide missing evidence.\"}");

        PathwayAssessmentAgentRunResponse response = service.explainPathwayAssessment(actor, request());

        // A pathway full of MISSING/NOT_SATISFIED requirements is real,
        // deterministically-computed signal - NOT an absence of grounding.
        // The LLM must be invoked, never skipped.
        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.GENERATED);
        assertThat(response.result().missingEvidence()).containsExactly(missingEvidenceItem);
        // Deterministic missing-evidence/support-status data remains fully
        // visible regardless of the AI narrative - never collapsed away.
        assertThat(response.result().requirementSummaries())
                .extracting(RequirementEvidenceMatrixRowResponse::supportStatus)
                .containsExactlyInAnyOrder(CaseRequirementSupportStatus.MISSING, CaseRequirementSupportStatus.NOT_SATISFIED);
        verify(llmService, times(1)).ask(anyString());
    }

    /**
     * Test 5 (missing evidence, LLM unavailable branch): the deterministic
     * missing-evidence signal must remain fully visible even when the LLM
     * cannot be reached - graceful degradation never hides deterministic
     * data, it only withholds the AI's own prose.
     */
    @Test
    void explainPathwayAssessmentKeepsMissingEvidenceVisibleWhenTheLlmIsUnavailable() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementEvidenceMatrixRowResponse missingRow = row(
                CaseRequirementSupportStatus.MISSING, RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE, true,
                List.of("EMPLOYMENT.CURRENT_EMPLOYER"), List.of()
        );

        MissingEvidenceItemResponse missingEvidenceItem = new MissingEvidenceItemResponse(
                REQUIREMENT_ID, "TEST.REQ", "Test Requirement", true, EvidenceNecessity.REQUIRED,
                CaseIssueSeverity.CRITICAL, List.of("EMPLOYMENT.CURRENT_EMPLOYER"), "reason"
        );

        CaseIntelligenceResponse context = caseIntelligence(
                RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE, List.of(missingRow), List.of(missingEvidenceItem)
        );

        when(pathwayIntelligenceContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString())).thenThrow(new RuntimeException("provider unavailable"));

        PathwayAssessmentAgentRunResponse response = service.explainPathwayAssessment(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.UNAVAILABLE);
        assertThat(response.result().missingEvidence()).containsExactly(missingEvidenceItem);
        assertThat(response.result().requirementSummaries().get(0).missingFactKeys())
                .containsExactly("EMPLOYMENT.CURRENT_EMPLOYER");
    }

    @Test
    void explainPathwayAssessmentReturnsInsufficientEvidenceOnlyWhenTheMatrixIsGenuinelyEmpty() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        CaseIntelligenceResponse context = caseIntelligence(
                RequirementEvaluationOutcome.UNKNOWN, List.of(), List.of()
        );

        when(pathwayIntelligenceContextTool.invoke(eq(actor), any())).thenReturn(context);

        PathwayAssessmentAgentRunResponse response = service.explainPathwayAssessment(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.INSUFFICIENT_EVIDENCE);
        assertThat(response.groundingState()).isEqualTo(AgentGroundingState.INSUFFICIENT_EVIDENCE);
        assertThat(response.result().explanation()).contains("Insufficient evidence");
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.NOT_ATTEMPTED);
        assertThat(response.result().humanReviewRequired()).isTrue();

        // Deterministic hallucination defense: the LLM is never called at all
        // when there is genuinely nothing to narrate.
        verify(llmService, never()).ask(anyString());
    }

    // =========================================================================
    // 3. CONFLICTS / HUMAN REVIEW REQUIRED
    // =========================================================================

    @Test
    void explainPathwayAssessmentFlagsHumanReviewWhenAnyRequirementHasUnresolvedConflicts() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementEvidenceMatrixRowResponse conflictedRow = row(
                CaseRequirementSupportStatus.CONFLICTING, RequirementEvaluationOutcome.CONFLICTED, true,
                List.of(), List.of(70L)
        );

        CaseIntelligenceResponse context = caseIntelligence(
                RequirementEvaluationOutcome.CONFLICTED, List.of(conflictedRow), List.of()
        );

        when(pathwayIntelligenceContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"Conflicting facts exist.\",\"recommendedNextStep\":\"Resolve the conflict.\"}");

        PathwayAssessmentAgentRunResponse response = service.explainPathwayAssessment(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().humanReviewRequired()).isTrue();
    }

    @Test
    void explainPathwayAssessmentFlagsHumanReviewWhenOverallOutcomeIsPendingReview() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementEvidenceMatrixRowResponse pendingRow = row(
                CaseRequirementSupportStatus.NEEDS_VERIFICATION, RequirementEvaluationOutcome.PENDING_REVIEW, true,
                List.of(), List.of()
        );

        CaseIntelligenceResponse context = caseIntelligence(
                RequirementEvaluationOutcome.PENDING_REVIEW, List.of(pendingRow), List.of()
        );

        when(pathwayIntelligenceContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString()))
                .thenReturn("{\"explanation\":\"Under review.\",\"recommendedNextStep\":\"Wait for verification.\"}");

        PathwayAssessmentAgentRunResponse response = service.explainPathwayAssessment(actor, request());

        assertThat(response.result().humanReviewRequired()).isTrue();
    }

    // =========================================================================
    // 4. UNAUTHORIZED CASE ACCESS - NO RUN EVER CREATED
    // =========================================================================

    @Test
    void explainPathwayAssessmentPropagatesUnauthorizedCaseAccessAndNeverCreatesARun() {

        AuthenticatedUser stranger = user(999L);

        when(pathwayIntelligenceContextTool.invoke(eq(stranger), any()))
                .thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> service.explainPathwayAssessment(stranger, request()))
                .isInstanceOf(AccessDeniedException.class);

        verify(agentRunRepository, never()).save(any());
        verify(llmService, never()).ask(anyString());
    }

    // =========================================================================
    // 5. GRACEFUL AI DEGRADATION - the deterministic assessment must NEVER
    // disappear merely because the LLM failed (Phase 5.2 Production
    // Hardening). Every scenario below expects the run to COMPLETE
    // successfully with the full deterministic result, never an exception.
    // =========================================================================

    /** Test 2 (spec): OpenAI quota exhausted. */
    @Test
    void explainPathwayAssessmentDegradesGracefullyWhenOpenAiQuotaIsExhausted() {
        assertGracefulDegradationOnLlmCallFailure(new RuntimeException(
                "429 Too Many Requests - insufficient_quota: You exceeded your current quota, "
                        + "credit_balance_exhausted"
        ));
    }

    /** Test 3 (spec): LLM provider timeout. */
    @Test
    void explainPathwayAssessmentDegradesGracefullyOnLlmTimeout() {
        assertGracefulDegradationOnLlmCallFailure(new RuntimeException("Read timed out"));
    }

    /** Test 4 (spec): LLM provider unavailable / connection failure. */
    @Test
    void explainPathwayAssessmentDegradesGracefullyWhenTheLlmProviderIsUnavailable() {
        assertGracefulDegradationOnLlmCallFailure(new RuntimeException(
                "Connection refused: api.openai.com:443 (internal provider detail)"
        ));
    }

    @Test
    void explainPathwayAssessmentDegradesGracefullyOnAnUnexpectedLlmException() {
        assertGracefulDegradationOnLlmCallFailure(new IllegalStateException(
                "Failed to generate AI response"
        ));
    }

    /**
     * Every one of the four scenarios above must produce the exact same
     * shape of graceful degradation: HTTP-successful, COMPLETED run,
     * deterministic assessment fully present, no fabricated narrative, and
     * no leaked internal/provider detail anywhere in what gets persisted or
     * returned.
     */
    private void assertGracefulDegradationOnLlmCallFailure(RuntimeException llmFailure) {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementEvidenceMatrixRowResponse satisfiedRow = row(
                CaseRequirementSupportStatus.SATISFIED, RequirementEvaluationOutcome.SATISFIED, true, List.of(), List.of()
        );

        CaseIntelligenceResponse context = caseIntelligence(
                RequirementEvaluationOutcome.SATISFIED, List.of(satisfiedRow), List.of()
        );

        when(pathwayIntelligenceContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString())).thenThrow(llmFailure);

        // No exception - the deterministic assessment succeeds regardless.
        PathwayAssessmentAgentRunResponse response = service.explainPathwayAssessment(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.errorMessage()).isNull();

        ExplainPathwayAssessmentResult result = response.result();
        assertThat(result).isNotNull();
        assertThat(result.aiExplanationStatus()).isEqualTo(AiExplanationStatus.UNAVAILABLE);
        assertThat(result.explanation()).isNull();
        // The deterministic Pathway evidence guidance is the only permitted
        // fallback next step - never a fabricated one.
        assertThat(result.recommendedNextStep()).isEqualTo(context.recommendedEvidenceGuidance());

        // The authoritative deterministic assessment is completely intact.
        assertThat(result.overallOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(result.groundingState()).isEqualTo(AgentGroundingState.GROUNDED);
        assertThat(result.requirementSummaries()).containsExactly(satisfiedRow);
        assertThat(result.readiness()).isSameAs(context.readiness());

        ArgumentCaptor<AgentRun> captor = ArgumentCaptor.forClass(AgentRun.class);
        verify(agentRunRepository, times(1)).save(captor.capture());

        AgentRun savedRun = captor.getValue();
        assertThat(savedRun.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(savedRun.getErrorMessage()).isNull();
        assertThat(savedRun.getGroundingState()).isEqualTo(AgentGroundingState.GROUNDED);

        // No trace of the raw provider/internal exception message anywhere
        // that gets persisted or returned.
        String leakyDetail = llmFailure.getMessage();
        assertThat(savedRun.getResultJson()).doesNotContain(leakyDetail);
        assertThat(savedRun.getReasoningSummary()).doesNotContain(leakyDetail);
    }

    @Test
    void explainPathwayAssessmentDegradesGracefullyWhenTheLlmReturnsUnparsableText() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementEvidenceMatrixRowResponse satisfiedRow = row(
                CaseRequirementSupportStatus.SATISFIED, RequirementEvaluationOutcome.SATISFIED, true, List.of(), List.of()
        );

        CaseIntelligenceResponse context = caseIntelligence(
                RequirementEvaluationOutcome.SATISFIED, List.of(satisfiedRow), List.of()
        );

        when(pathwayIntelligenceContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString())).thenReturn("this is not json at all");

        PathwayAssessmentAgentRunResponse response = service.explainPathwayAssessment(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.FAILED);
        assertThat(response.result().explanation()).isNull();
        // The deterministic assessment is still fully present.
        assertThat(response.result().overallOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
    }

    @Test
    void explainPathwayAssessmentDegradesGracefullyWhenTheLlmReturnsAnEmptyResponse() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementEvidenceMatrixRowResponse satisfiedRow = row(
                CaseRequirementSupportStatus.SATISFIED, RequirementEvaluationOutcome.SATISFIED, true, List.of(), List.of()
        );

        CaseIntelligenceResponse context = caseIntelligence(
                RequirementEvaluationOutcome.SATISFIED, List.of(satisfiedRow), List.of()
        );

        when(pathwayIntelligenceContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString())).thenReturn("");

        PathwayAssessmentAgentRunResponse response = service.explainPathwayAssessment(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.FAILED);
        assertThat(response.result().explanation()).isNull();
    }

    @Test
    void explainPathwayAssessmentNeverPersistsARawInternalExceptionMessageFromAnUnexpectedFailure() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        // Simulates a leaky, unexpected internal failure surfacing from the
        // tool call itself (before the LLM boundary's own safe-wrapping) -
        // the kind of raw detail that must never reach a persisted field.
        when(pathwayIntelligenceContextTool.invoke(eq(actor), any())).thenThrow(new RuntimeException(
                "ORA-00942: table or view does not exist at "
                        + "com.godfrey.ai_immigration_document_analyzer.internal.SecretRepository.query(SecretRepository.java:42)"
        ));

        assertThatThrownBy(() -> service.explainPathwayAssessment(actor, request()))
                .isInstanceOf(RuntimeException.class);

        // No AgentRun is ever created for a failure at the authorization/tool
        // boundary - identical to the single-requirement agent's behavior.
        verify(agentRunRepository, never()).save(any());
    }

    @Test
    void explainPathwayAssessmentParsesMarkdownFencedJsonFromTheLlm() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementEvidenceMatrixRowResponse satisfiedRow = row(
                CaseRequirementSupportStatus.SATISFIED, RequirementEvaluationOutcome.SATISFIED, true, List.of(), List.of()
        );

        CaseIntelligenceResponse context = caseIntelligence(
                RequirementEvaluationOutcome.SATISFIED, List.of(satisfiedRow), List.of()
        );

        when(pathwayIntelligenceContextTool.invoke(eq(actor), any())).thenReturn(context);
        when(llmService.ask(anyString())).thenReturn(
                "```json\n{\"explanation\":\"Fenced narrative.\",\"recommendedNextStep\":\"Fenced next step.\"}\n```"
        );

        PathwayAssessmentAgentRunResponse response = service.explainPathwayAssessment(actor, request());

        assertThat(response.status()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(response.result().explanation()).isEqualTo("Fenced narrative.");
        assertThat(response.result().aiExplanationStatus()).isEqualTo(AiExplanationStatus.GENERATED);
    }

    // =========================================================================
    // AGENT RUN LIFECYCLE / GET RUN
    // =========================================================================

    @Test
    void getRunAuthorizesThroughTheExistingFactAuthorizationBoundary() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        RequirementEvidenceMatrixRowResponse satisfiedRow = row(
                CaseRequirementSupportStatus.SATISFIED, RequirementEvaluationOutcome.SATISFIED, true, List.of(), List.of()
        );

        ExplainPathwayAssessmentResult persistedResult = new ExplainPathwayAssessmentResult(
                ASSESSMENT_ID, "TEST_PATHWAY", "Test Pathway", RequirementEvaluationOutcome.SATISFIED,
                new CaseReadinessResponse(100.0, 100.0, 100.0, 100.0, List.of()),
                List.of(satisfiedRow), List.of(), "guidance",
                "A narrative.", AgentGroundingState.GROUNDED, AiExplanationStatus.GENERATED, "Next step.", false
        );

        AgentRun run = AgentRun.builder()
                .id(77L).subjectUserId(SUBJECT_ID).requestedByUserId(SUBJECT_ID)
                .status(AgentRunStatus.COMPLETED)
                .resultJson(writeJson(persistedResult))
                .build();

        when(agentRunRepository.findById(77L)).thenReturn(Optional.of(run));

        PathwayAssessmentAgentRunResponse response = service.getRun(actor, 77L);

        verify(factAuthorizationService).assertCanView(eq(actor), eq(SUBJECT_ID), any(), any(), anyString());
        assertThat(response.result().explanation()).isEqualTo("A narrative.");
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
