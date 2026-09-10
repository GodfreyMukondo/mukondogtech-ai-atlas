package com.godfrey.ai_immigration_document_analyzer.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainRequirementRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainRequirementResult;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.AgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGoalType;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.repository.AgentRunRepository;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.AgentToolName;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.EvidenceGraphContextTool;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.RequirementExplanationTool;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto.EvidenceGraphResponse;
import com.godfrey.ai_immigration_document_analyzer.exception.AiServiceException;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactEvidenceResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;
import com.godfrey.ai_immigration_document_analyzer.service.LlmService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * EXPLAIN REQUIREMENT AGENT SERVICE
 * ============================================================================
 *
 * Phase 5.1's single orchestrator. Executes a fixed, auditable plan (see
 * {@link #PLAN_SUMMARY}) over the two registered read-only tools, grounds
 * (or honestly declines to ground) an explanation, optionally asks the
 * existing {@link LlmService} to phrase it, and persists exactly one
 * {@link AgentRun} per invocation.
 *
 * ARCHITECTURAL INVARIANTS
 * ----------------------------------------------------------------------------
 * - The agent never touches a repository directly - only the two typed
 *   {@code AgentTool}s, which themselves only wrap already-authorized
 *   services ({@code CaseIntelligenceService}, {@code EvidenceGraphService}).
 * - Authorization happens exactly once, inside the first tool call, before
 *   this service reads or writes anything else - identical to every other
 *   authorization-first service in this codebase.
 * - The LLM is never asked to decide the requirement's outcome - only to
 *   explain an outcome this service already computed via existing,
 *   authoritative services. It is never invoked at all when nothing was
 *   found to explain (see {@link #isGrounded}).
 * - {@code factsConsidered}/{@code evidenceConsidered}/{@code conflicts} in
 *   the result are populated only from tool output - the LLM can never
 *   introduce a fact/evidence/conflict id that wasn't already there.
 * - No hidden chain-of-thought is ever persisted - only the fixed plan
 *   summary, the tool names actually invoked, and a concise reasoning
 *   summary.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExplainRequirementAgentService {

    private static final String PLAN_SUMMARY = """
            1. Load the requirement's current status and supporting case data (facts, evidence, missing evidence, conflicts, regulatory source) via GET_REQUIREMENT_EXPLANATION.
            2. Load the Evidence Graph behind this requirement's evaluation via GET_EVIDENCE_GRAPH_CONTEXT, when an evaluation exists.
            3. Check whether the gathered data is sufficient to ground an explanation.
            4. If grounded, ask the language model to explain the status using only the data already gathered.
            5. Assemble the structured result and record this run.""";

    private static final String INSUFFICIENT_EVIDENCE_EXPLANATION =
            "Insufficient evidence to determine. No facts, evidence, or conflicts have been recorded yet "
                    + "for this requirement, so no explanation can be grounded in real case data.";

    /**
     * The only message ever persisted/returned for a failure this service
     * did not itself construct with a known-safe message (see
     * {@link #safeErrorMessage}) - matches {@code GlobalExceptionHandler}'s
     * own convention of never surfacing a raw exception message, SQL error,
     * stack trace, or internal class/path detail to a caller. The real
     * exception is always logged server-side in full alongside this.
     */
    private static final String GENERIC_FAILURE_MESSAGE =
            "An unexpected error occurred while generating this explanation. Please try again later.";

    private final RequirementExplanationTool requirementExplanationTool;
    private final EvidenceGraphContextTool evidenceGraphContextTool;
    private final LlmService llmService;
    private final AgentRunRepository agentRunRepository;
    private final FactAuthorizationService factAuthorizationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AgentRunResponse explainRequirement(AuthenticatedUser actor, ExplainRequirementRequest request) {

        List<AgentToolName> toolsInvoked = new ArrayList<>();

        // ---- STEP 1: GET_REQUIREMENT_EXPLANATION -------------------------
        // Authorization happens here, first. A denied/not-found request
        // propagates immediately - no AgentRun is created for it, exactly
        // like every other endpoint in this codebase records no
        // domain-specific audit row of its own for a 403/404 (the existing
        // FactAccessAuditLog already captures the denial itself).
        RequirementExplanationTool.Output requirementContext = requirementExplanationTool.invoke(
                actor,
                new RequirementExplanationTool.Input(request.getPathwayAssessmentId(), request.getRequirementId())
        );
        toolsInvoked.add(AgentToolName.GET_REQUIREMENT_EXPLANATION);

        AgentRun run = AgentRun.builder()
                .goal(AgentGoalType.EXPLAIN_REQUIREMENT)
                .subjectUserId(requirementContext.subjectUserId())
                .requestedByUserId(actor.getUserId())
                .pathwayAssessmentId(request.getPathwayAssessmentId())
                .requirementId(request.getRequirementId())
                .planSummary(PLAN_SUMMARY)
                .status(AgentRunStatus.FAILED) // overwritten below on every path; never left stale
                .build();

        try {

            // ---- STEP 2: GET_EVIDENCE_GRAPH_CONTEXT -----------------------
            EvidenceGraphResponse provenance = EvidenceGraphResponse.empty();

            if (requirementContext.evaluationId() != null) {

                provenance = evidenceGraphContextTool.invoke(
                        actor,
                        new EvidenceGraphContextTool.Input(requirementContext.evaluationId())
                );

                toolsInvoked.add(AgentToolName.GET_EVIDENCE_GRAPH_CONTEXT);
            }

            // ---- STEP 3: GROUNDING CHECK -----------------------------------
            boolean grounded = isGrounded(requirementContext);

            ExplainRequirementResult result;
            String reasoningSummary;
            String model = null;

            if (!grounded) {

                // ---- STEP 4 (skipped): NEVER invoke the LLM with nothing to explain ----
                result = buildResult(
                        requirementContext,
                        provenance,
                        AgentGroundingState.INSUFFICIENT_EVIDENCE,
                        INSUFFICIENT_EVIDENCE_EXPLANATION,
                        buildFallbackNextStep(requirementContext)
                );

                reasoningSummary = "No contributing facts or unresolved conflicts were found for this "
                        + "requirement - explanation withheld rather than fabricated.";

                run.setStatus(AgentRunStatus.INSUFFICIENT_EVIDENCE);

            } else {

                // ---- STEP 4: LLM EXPLAINS THE ALREADY-COMPUTED STATUS ----------
                // model stays null: LlmService.ask() intentionally exposes only
                // the final text to callers - the real provider-reported model
                // name is visible solely to AIModelMonitoringService, which
                // LlmService already records every call against. Recording a
                // guessed model name here would be exactly the kind of
                // fabricated detail this run must never contain.
                LlmExplanationSection llmSection = explainWithLlm(requirementContext);

                result = buildResult(
                        requirementContext,
                        provenance,
                        AgentGroundingState.GROUNDED,
                        llmSection.explanation(),
                        llmSection.recommendedNextStep()
                );

                reasoningSummary = "Explained using " + requirementContext.contributingFacts().size()
                        + " contributing fact(s) and " + requirementContext.unresolvedConflictIds().size()
                        + " unresolved conflict(s), grounded in the existing Requirement/Evidence Graph data.";

                run.setStatus(AgentRunStatus.COMPLETED);
            }

            // ---- STEP 5: RECORD THE RUN -------------------------------------
            run.setToolsInvoked(String.join(",", toolsInvoked.stream().map(Enum::name).toList()));
            run.setModel(model);
            run.setReasoningSummary(reasoningSummary);
            run.setGroundingState(result.groundingState());
            run.setHumanReviewRequired(result.humanReviewRequired());
            run.setResultJson(serialize(result));
            run.setCompletedAt(LocalDateTime.now());

            AgentRun saved = agentRunRepository.save(run);

            log.info(
                    "Agent run completed | runId={} | goal={} | status={} | tools={}",
                    saved.getId(), saved.getGoal(), saved.getStatus(), run.getToolsInvoked()
            );

            return AgentRunResponse.from(saved, result);

        } catch (RuntimeException exception) {

            run.setToolsInvoked(String.join(",", toolsInvoked.stream().map(Enum::name).toList()));
            run.setStatus(AgentRunStatus.FAILED);
            run.setErrorMessage(safeErrorMessage(exception));
            run.setCompletedAt(LocalDateTime.now());

            agentRunRepository.save(run);

            log.error(
                    "Agent run failed | subjectUserId={} | requirementId={}",
                    requirementContext.subjectUserId(), request.getRequirementId(), exception
            );

            if (exception instanceof AiServiceException aiServiceException) {
                throw aiServiceException;
            }

            throw new AiServiceException("The explain-requirement agent could not complete this run.", exception);
        }
    }

    @Transactional(readOnly = true)
    public AgentRunResponse getRun(AuthenticatedUser actor, Long runId) {

        AgentRun run = agentRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent run not found."));

        // Same boundary every other case-scoped retrieval in this codebase
        // uses - self, or a case worker with an active CaseAssignment for
        // this subject. Never a second, looser authorization path.
        factAuthorizationService.assertCanView(actor, run.getSubjectUserId(), null, null, "view agent run");

        ExplainRequirementResult result = deserialize(run.getResultJson());

        return AgentRunResponse.from(run, result);
    }

    // =========================================================================
    // GROUNDING
    // =========================================================================

    private boolean isGrounded(RequirementExplanationTool.Output data) {
        return data.isGrounded();
    }

    private String buildFallbackNextStep(RequirementExplanationTool.Output data) {

        if (data.missingFactKeys() != null && !data.missingFactKeys().isEmpty()) {

            return "Provide or upload evidence establishing: " + String.join(", ", data.missingFactKeys()) + ".";
        }

        return "Review this requirement's evidence expectations and provide supporting documentation.";
    }

    // =========================================================================
    // LLM
    // =========================================================================

    private record LlmExplanationSection(String explanation, String recommendedNextStep) {
    }

    private LlmExplanationSection explainWithLlm(RequirementExplanationTool.Output data) {

        String prompt = buildPrompt(data);

        String rawResponse;

        try {

            rawResponse = llmService.ask(prompt);

        } catch (Exception exception) {

            throw new AiServiceException("The AI explanation service is currently unavailable.", exception);
        }

        if (!StringUtils.hasText(rawResponse)) {

            throw new AiServiceException("The AI explanation service returned no response.");
        }

        LlmJsonPayload payload = parseJsonPayload(rawResponse);

        return new LlmExplanationSection(payload.explanation(), payload.recommendedNextStep());
    }

    private record LlmJsonPayload(String explanation, String recommendedNextStep) {
    }

    private LlmJsonPayload parseJsonPayload(String rawResponse) {

        String cleaned = rawResponse.trim();

        if (cleaned.startsWith("```")) {

            int firstNewline = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");

            if (firstNewline >= 0 && lastFence > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastFence).trim();
            }
        }

        try {

            return objectMapper.readValue(cleaned, LlmJsonPayload.class);

        } catch (Exception exception) {

            throw new AiServiceException(
                    "The AI explanation service returned a response that could not be interpreted.",
                    exception
            );
        }
    }

    private String buildPrompt(RequirementExplanationTool.Output data) {

        String factsBlock = data.contributingFacts().isEmpty()
                ? "None recorded."
                : data.contributingFacts().stream()
                        .map(fact -> "- %s: %s (provenance=%s, verified=%s, confidence=%s)".formatted(
                                fact.factKey(), fact.valueSummary(), fact.provenanceType(),
                                fact.isVerified(), fact.confidenceLevel()
                        ))
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("None recorded.");

        String missingKeysBlock = data.missingFactKeys().isEmpty()
                ? "None."
                : String.join(", ", data.missingFactKeys());

        String conflictsBlock = data.unresolvedConflictIds().isEmpty()
                ? "None."
                : data.unresolvedConflictIds().size() + " unresolved conflict(s) exist for facts this requirement depends on.";

        String existingExplanationBlock = StringUtils.hasText(data.existingExplanation())
                ? data.existingExplanation()
                : "None recorded.";

        return """
                You are an AI assistant inside MukondoGTech AI's immigration case platform, explaining why one \
                pathway requirement currently has its status.

                RULES:
                - Use ONLY the case data provided below. Never invent a fact, document, date, or regulation not listed here.
                - Never claim the requirement is satisfied or not satisfied beyond what CURRENT STATUS already states - \
                explain that status, do not decide it.
                - EXISTING APPLICATION EXPLANATION below is the application's own, already-authoritative explanation for \
                CURRENT STATUS, computed by its rules engine - not written by you, and not a fact you may contradict. \
                Treat it as grounding context: clarify, expand, and phrase it for a reader, but do not override, replace, \
                or produce an explanation that disagrees with it. If it is thin or absent, say so rather than inventing \
                a replacement.
                - If the data below is thin, say so plainly rather than filling gaps with assumptions.
                - Do not claim to be a lawyer or immigration officer, and do not guarantee any outcome.
                - Respond with ONLY a single JSON object, no markdown fences, no extra commentary, in exactly this shape:
                {"explanation": "...", "recommendedNextStep": "..."}

                REQUIREMENT: %s (%s)
                MANDATORY: %s
                CURRENT STATUS: %s (case support status: %s)
                CERTAINTY: %s
                REGULATORY SOURCE: %s

                EXISTING APPLICATION EXPLANATION (authoritative, not AI-generated):
                %s

                CONTRIBUTING FACTS:
                %s

                MISSING FACT KEYS (bound to this requirement, not yet recorded):
                %s

                UNRESOLVED CONFLICTS: %s

                PATHWAY EVIDENCE GUIDANCE: %s
                """.formatted(
                data.requirementTitle(), data.requirementKey(),
                Boolean.TRUE.equals(data.mandatory()),
                data.outcome(), data.supportStatus(),
                data.certaintyLevel(),
                data.regulatorySourceAuthority() != null ? data.regulatorySourceAuthority() : "Not recorded",
                existingExplanationBlock,
                factsBlock,
                missingKeysBlock,
                conflictsBlock,
                data.pathwayEvidenceExpectations() != null ? data.pathwayEvidenceExpectations() : "None provided."
        );
    }

    // =========================================================================
    // RESULT ASSEMBLY
    // =========================================================================

    private ExplainRequirementResult buildResult(
            RequirementExplanationTool.Output data,
            EvidenceGraphResponse provenance,
            AgentGroundingState groundingState,
            String explanation,
            String recommendedNextStep
    ) {

        List<FactEvidenceResponse> evidenceConsidered = flattenEvidence(data.contributingFacts());

        boolean humanReviewRequired = determineHumanReviewRequired(data, groundingState);

        return new ExplainRequirementResult(
                data.pathwayAssessmentId(),
                data.requirementId(),
                data.requirementKey(),
                data.requirementTitle(),
                data.outcome(),
                data.supportStatus(),
                data.existingExplanation(),
                explanation,
                data.contributingFacts(),
                evidenceConsidered,
                data.missingFactKeys(),
                data.unresolvedConflictIds(),
                provenance,
                data.regulatoryVersionId(),
                data.regulatorySourceAuthority(),
                data.regulatoryVerificationStatus(),
                groundingState,
                recommendedNextStep,
                humanReviewRequired
        );
    }

    private List<FactEvidenceResponse> flattenEvidence(List<ExplanationResponse.FactExplanation> contributingFacts) {

        Map<Long, FactEvidenceResponse> byId = new LinkedHashMap<>();

        for (ExplanationResponse.FactExplanation fact : contributingFacts) {

            for (FactEvidenceResponse evidence : fact.evidence()) {
                byId.putIfAbsent(evidence.id(), evidence);
            }
        }

        return List.copyOf(byId.values());
    }

    private boolean determineHumanReviewRequired(RequirementExplanationTool.Output data, AgentGroundingState groundingState) {

        if (!data.unresolvedConflictIds().isEmpty()) {
            return true;
        }

        if (data.outcome() == RequirementEvaluationOutcome.CONFLICTED
                || data.outcome() == RequirementEvaluationOutcome.PENDING_REVIEW) {
            return true;
        }

        return groundingState == AgentGroundingState.INSUFFICIENT_EVIDENCE && Boolean.TRUE.equals(data.mandatory());
    }

    // =========================================================================
    // SERIALIZATION
    // =========================================================================

    private String serialize(ExplainRequirementResult result) {

        try {

            return objectMapper.writeValueAsString(result);

        } catch (Exception exception) {

            throw new IllegalStateException("Failed to serialize the agent run result.", exception);
        }
    }

    private ExplainRequirementResult deserialize(String resultJson) {

        if (!StringUtils.hasText(resultJson)) {
            return null;
        }

        try {

            return objectMapper.readValue(resultJson, ExplainRequirementResult.class);

        } catch (Exception exception) {

            log.error("Failed to deserialize a persisted agent run result.", exception);
            return null;
        }
    }

    /**
     * Never persists a raw internal exception message, SQL error, stack
     * trace, filesystem path, provider error, or internal class name -
     * only ever one of this service's own pre-written, already-safe
     * {@link AiServiceException} messages, or a fixed generic statement for
     * anything else. The real exception is always logged in full,
     * server-side, immediately after this is called - this method only
     * controls what gets PERSISTED/returned.
     */
    private String safeErrorMessage(RuntimeException exception) {

        if (exception instanceof AiServiceException) {
            return exception.getMessage();
        }

        return GENERIC_FAILURE_MESSAGE;
    }
}
