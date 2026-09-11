package com.godfrey.ai_immigration_document_analyzer.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainPathwayAssessmentRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainPathwayAssessmentResult;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.PathwayAssessmentAgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGoalType;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AiExplanationStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.repository.AgentRunRepository;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.AgentToolName;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.PathwayIntelligenceContextTool;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIntelligenceResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.MissingEvidenceItemResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.RequirementEvidenceMatrixRowResponse;
import com.godfrey.ai_immigration_document_analyzer.exception.AiServiceException;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
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
import java.util.List;

/**
 * ============================================================================
 * EXPLAIN PATHWAY ASSESSMENT AGENT SERVICE
 * ============================================================================
 *
 * Phase 5.2's orchestrator - the whole-pathway counterpart to {@code
 * ExplainRequirementAgentService}. Executes a fixed, auditable plan (see
 * {@link #PLAN_SUMMARY}) over the single registered read-only tool, grounds
 * (or honestly declines to ground) a pathway-level narrative, optionally
 * asks the existing {@link LlmService} to phrase it, and persists exactly
 * one {@link AgentRun} per invocation.
 *
 * ARCHITECTURAL INVARIANTS (identical to Phase 5.1's, restated for this goal)
 * ----------------------------------------------------------------------------
 * - The agent never touches a repository directly - only the typed {@code
 *   PathwayIntelligenceContextTool}, which itself only wraps the already-
 *   authorized, already-deterministic {@code CaseIntelligenceService}.
 * - Authorization happens exactly once, inside the tool call, before this
 *   service reads or writes anything else.
 * - The LLM is never asked to decide the pathway's outcome or any
 *   requirement's outcome - only to narrate an outcome this service already
 *   read from existing, authoritative, deterministic services. It is never
 *   invoked at all when nothing was found to explain (see {@link #isGrounded}).
 * - GROUNDING NOTE: a pathway containing requirements with MISSING or
 *   NOT_SATISFIED support status is still GROUNDED - that is real,
 *   deterministically-computed signal about the case, not an absence of
 *   data. Ungrounded means the assessment produced no requirement
 *   evaluations at all (an empty {@code evidenceMatrix}), never merely an
 *   unfavorable one. See {@link #isGrounded}.
 * - {@code requirementSummaries}/{@code missingEvidence}/{@code readiness}
 *   in the result are populated only from the tool's output - the LLM can
 *   never introduce a requirement, fact, evidence, or conflict id that
 *   wasn't already there, and can never change any outcome/support status.
 * - No hidden chain-of-thought is ever persisted - only the fixed plan
 *   summary, the tool name actually invoked, and a concise reasoning
 *   summary.
 * - GRACEFUL AI DEGRADATION (Phase 5.2 Production Hardening): the LLM is a
 *   presentation-layer enhancement on top of an already-complete
 *   deterministic result, never a precondition for returning one. Any
 *   failure while calling the LLM or interpreting its response (provider
 *   outage, timeout, rate limiting, exhausted quota/credit balance, an
 *   empty or unparsable reply) is caught narrowly around the LLM step ONLY
 *   and downgrades {@link AiExplanationStatus} to {@code UNAVAILABLE}/
 *   {@code FAILED} - it never aborts the request, never discards the
 *   already-loaded {@code CaseIntelligenceResponse}, and never fabricates a
 *   narrative to fill the gap. The run is still recorded as {@code
 *   COMPLETED} because the authoritative assessment itself succeeded; only
 *   {@link ExplainPathwayAssessmentResult#aiExplanationStatus()} reflects
 *   the degraded AI step. See {@link #attemptLlmNarrative}.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExplainPathwayAssessmentAgentService {

    private static final String PLAN_SUMMARY = """
            1. Load the whole pathway assessment's Case Intelligence bundle (overall outcome, readiness, requirement-to-evidence matrix, missing evidence, regulatory sources) via GET_PATHWAY_INTELLIGENCE_CONTEXT.
            2. Check whether the assessment produced any requirement evaluations to ground a narrative in.
            3. If grounded, ask the language model to narrate the pathway's overall status using only the data already gathered.
            4. Assemble the structured result and record this run.""";

    private static final String INSUFFICIENT_EVIDENCE_EXPLANATION =
            "Insufficient evidence to determine. This pathway assessment did not produce any requirement "
                    + "evaluations, so no pathway-level explanation can be grounded in real case data.";

    private static final String INSUFFICIENT_EVIDENCE_NEXT_STEP =
            "Re-run this pathway assessment, or verify the pathway's requirement composition.";

    /**
     * The only message ever persisted/returned for a failure this service
     * did not itself construct with a known-safe message - matches {@code
     * GlobalExceptionHandler}'s own convention of never surfacing a raw
     * exception message, SQL error, stack trace, or internal class/path
     * detail to a caller. The real exception is always logged server-side
     * in full alongside this.
     */
    private static final String GENERIC_FAILURE_MESSAGE =
            "An unexpected error occurred while generating this explanation. Please try again later.";

    private final PathwayIntelligenceContextTool pathwayIntelligenceContextTool;
    private final LlmService llmService;
    private final AgentRunRepository agentRunRepository;
    private final FactAuthorizationService factAuthorizationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PathwayAssessmentAgentRunResponse explainPathwayAssessment(
            AuthenticatedUser actor,
            ExplainPathwayAssessmentRequest request
    ) {

        List<AgentToolName> toolsInvoked = new ArrayList<>();

        // ---- STEP 1: GET_PATHWAY_INTELLIGENCE_CONTEXT ---------------------
        // Authorization happens here, first. A denied/not-found request
        // propagates immediately - no AgentRun is created for it.
        CaseIntelligenceResponse context = pathwayIntelligenceContextTool.invoke(
                actor,
                new PathwayIntelligenceContextTool.Input(request.getPathwayAssessmentId())
        );
        toolsInvoked.add(AgentToolName.GET_PATHWAY_INTELLIGENCE_CONTEXT);

        AgentRun run = AgentRun.builder()
                .goal(AgentGoalType.EXPLAIN_PATHWAY_ASSESSMENT)
                .subjectUserId(context.subjectUserId())
                .requestedByUserId(actor.getUserId())
                .pathwayAssessmentId(request.getPathwayAssessmentId())
                .planSummary(PLAN_SUMMARY)
                .status(AgentRunStatus.FAILED) // overwritten below on every path; never left stale
                .build();

        try {

            // ---- STEP 2: GROUNDING CHECK ------------------------------------
            boolean grounded = isGrounded(context);

            ExplainPathwayAssessmentResult result;
            String reasoningSummary;
            String model = null;

            if (!grounded) {

                // ---- STEP 3 (skipped): NEVER invoke the LLM with nothing to explain ----
                result = buildResult(
                        context,
                        AgentGroundingState.INSUFFICIENT_EVIDENCE,
                        AiExplanationStatus.NOT_ATTEMPTED,
                        INSUFFICIENT_EVIDENCE_EXPLANATION,
                        INSUFFICIENT_EVIDENCE_NEXT_STEP
                );

                reasoningSummary = "No requirement evaluations were found for this pathway assessment - "
                        + "narrative withheld rather than fabricated.";

                run.setStatus(AgentRunStatus.INSUFFICIENT_EVIDENCE);

            } else {

                // ---- STEP 3: LLM NARRATES THE ALREADY-COMPUTED STATUS ----------
                // Never lets an LLM failure abort this request - see
                // attemptLlmNarrative()'s own javadoc and the class-level
                // "GRACEFUL AI DEGRADATION" note above. model stays null:
                // LlmService.ask() intentionally exposes only the final text
                // to callers - see ExplainRequirementAgentService for the
                // full rationale.
                LlmAttemptOutcome aiOutcome = attemptLlmNarrative(context);

                result = buildResult(
                        context,
                        AgentGroundingState.GROUNDED,
                        aiOutcome.status(),
                        aiOutcome.explanation(),
                        aiOutcome.recommendedNextStep()
                );

                reasoningSummary = aiOutcome.reasoningSummary();

                // Always COMPLETED here: the authoritative deterministic
                // assessment succeeded regardless of whether the optional AI
                // narrative did - see aiOutcome.status() for that separate
                // outcome.
                run.setStatus(AgentRunStatus.COMPLETED);
            }

            // ---- STEP 4: RECORD THE RUN -------------------------------------
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

            return PathwayAssessmentAgentRunResponse.from(saved, result);

        } catch (RuntimeException exception) {

            run.setToolsInvoked(String.join(",", toolsInvoked.stream().map(Enum::name).toList()));
            run.setStatus(AgentRunStatus.FAILED);
            run.setErrorMessage(safeErrorMessage(exception));
            run.setCompletedAt(LocalDateTime.now());

            agentRunRepository.save(run);

            log.error(
                    "Agent run failed | subjectUserId={} | pathwayAssessmentId={}",
                    context.subjectUserId(), request.getPathwayAssessmentId(), exception
            );

            if (exception instanceof AiServiceException aiServiceException) {
                throw aiServiceException;
            }

            throw new AiServiceException("The explain-pathway-assessment agent could not complete this run.", exception);
        }
    }

    @Transactional(readOnly = true)
    public PathwayAssessmentAgentRunResponse getRun(AuthenticatedUser actor, Long runId) {

        AgentRun run = agentRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent run not found."));

        // Same boundary every other case-scoped retrieval in this codebase
        // uses - self, or a case worker with an active CaseAssignment for
        // this subject. Never a second, looser authorization path.
        factAuthorizationService.assertCanView(actor, run.getSubjectUserId(), null, null, "view agent run");

        ExplainPathwayAssessmentResult result = deserialize(run.getResultJson());

        return PathwayAssessmentAgentRunResponse.from(run, result);
    }

    // =========================================================================
    // GROUNDING
    // =========================================================================

    /**
     * Grounded iff the assessment produced at least one requirement
     * evaluation - NOT whether those evaluations are favorable. A pathway
     * whose every requirement sits on MISSING/NOT_SATISFIED is still fully
     * grounded: that is real, deterministically-computed signal the LLM may
     * honestly narrate. Only a genuinely empty matrix (no requirements were
     * ever evaluated for this assessment) withholds the LLM call.
     */
    private boolean isGrounded(CaseIntelligenceResponse context) {
        return context.evidenceMatrix() != null && !context.evidenceMatrix().isEmpty();
    }

    // =========================================================================
    // LLM
    // =========================================================================

    /**
     * Thrown when the call to the LLM provider itself did not complete -
     * network/connection failure, timeout, rate limiting (HTTP 429), or an
     * exhausted quota/credit balance. Caught ONLY by {@link
     * #attemptLlmNarrative} and converted to {@link
     * AiExplanationStatus#UNAVAILABLE} - it never propagates out of this
     * service and never aborts the request.
     */
    private static final class LlmUnavailableException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        LlmUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Thrown when the LLM call completed but its response could not be
     * used - empty text, or a payload that did not parse into the expected
     * explanation/recommendedNextStep shape. Caught ONLY by {@link
     * #attemptLlmNarrative} and converted to {@link
     * AiExplanationStatus#FAILED} - it never propagates out of this service
     * and never aborts the request.
     */
    private static final class LlmResponseUnusableException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        LlmResponseUnusableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record LlmNarrativeSection(String explanation, String recommendedNextStep) {
    }

    private record LlmAttemptOutcome(
            AiExplanationStatus status,
            String explanation,
            String recommendedNextStep,
            String reasoningSummary
    ) {
    }

    /**
     * Attempts the optional AI narration step and NEVER lets it fail the
     * request - narrow, intentional handling of exactly the two failure
     * modes {@link #narrateWithLlm} can raise (see their own javadoc),
     * never a blind {@code catch (Exception)}. Every failure is logged in
     * full server-side (never returned to the caller) before this method
     * returns a deterministic-safe fallback: no fabricated narrative, and -
     * per Phase 5.2 Production Hardening section 6 - the Pathway's own
     * pre-existing {@code recommendedEvidenceGuidance} reused as the only
     * permitted non-AI fallback next step (or null when the pathway records
     * none).
     */
    private LlmAttemptOutcome attemptLlmNarrative(CaseIntelligenceResponse context) {

        try {

            LlmNarrativeSection section = narrateWithLlm(context);

            return new LlmAttemptOutcome(
                    AiExplanationStatus.GENERATED,
                    section.explanation(),
                    section.recommendedNextStep(),
                    "Narrated using " + context.evidenceMatrix().size() + " requirement evaluation(s) "
                            + "and " + context.missingEvidence().size() + " missing-evidence item(s), grounded in the "
                            + "existing Case Intelligence data."
            );

        } catch (LlmUnavailableException exception) {

            log.warn(
                    "AI explanation unavailable for pathway assessment {} - deterministic assessment is still "
                            + "returned successfully | reason={}",
                    context.pathwayAssessmentId(), exception.getMessage(), exception
            );

            return new LlmAttemptOutcome(
                    AiExplanationStatus.UNAVAILABLE,
                    null,
                    context.recommendedEvidenceGuidance(),
                    "The deterministic pathway assessment completed successfully. The AI narrative could not be "
                            + "generated because the AI provider was unavailable."
            );

        } catch (LlmResponseUnusableException exception) {

            log.warn(
                    "AI explanation response unusable for pathway assessment {} - deterministic assessment is "
                            + "still returned successfully | reason={}",
                    context.pathwayAssessmentId(), exception.getMessage(), exception
            );

            return new LlmAttemptOutcome(
                    AiExplanationStatus.FAILED,
                    null,
                    context.recommendedEvidenceGuidance(),
                    "The deterministic pathway assessment completed successfully. The AI narrative could not be "
                            + "generated because the AI provider's response could not be used."
            );
        }
    }

    private LlmNarrativeSection narrateWithLlm(CaseIntelligenceResponse context) {

        String prompt = buildPrompt(context);

        String rawResponse;

        try {

            rawResponse = llmService.ask(prompt);

        } catch (Exception exception) {

            throw new LlmUnavailableException("The AI provider call failed.", exception);
        }

        if (!StringUtils.hasText(rawResponse)) {

            throw new LlmResponseUnusableException("The AI provider returned an empty response.", null);
        }

        LlmJsonPayload payload = parseJsonPayload(rawResponse);

        return new LlmNarrativeSection(payload.explanation(), payload.recommendedNextStep());
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

            throw new LlmResponseUnusableException(
                    "The AI provider response could not be interpreted.",
                    exception
            );
        }
    }

    private String buildPrompt(CaseIntelligenceResponse context) {

        String requirementRowsBlock = context.evidenceMatrix().stream()
                .map(this::describeRow)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("None.");

        String missingEvidenceBlock = context.missingEvidence().isEmpty()
                ? "None."
                : context.missingEvidence().stream()
                        .map(item -> "- %s (%s): %s".formatted(item.requirementTitle(), item.necessity(), item.reason()))
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("None.");

        return """
                You are an AI assistant inside MukondoGTech AI's immigration case platform, explaining why one \
                whole pathway assessment currently has its overall outcome.

                RULES:
                - Use ONLY the case data provided below. Never invent a requirement, fact, document, date, or \
                regulation not listed here.
                - Never claim a different overall outcome, or a different outcome for any individual requirement, \
                than what is stated below - narrate the already-computed result, do not decide it.
                - Each requirement's own EXISTING EXPLANATION (when present) is authoritative, already computed by \
                the rules engine - not written by you, and not a fact you may contradict. Treat all of it as \
                grounding context: summarize and clarify, but never override, replace, or produce a narrative that \
                disagrees with it.
                - If a requirement has MISSING or NOT_SATISFIED status, say so plainly - that is real information \
                about the case, not something to soften or omit.
                - If the data below is thin, say so plainly rather than filling gaps with assumptions.
                - Do not claim to be a lawyer or immigration officer, and do not guarantee any outcome.
                - Respond with ONLY a single JSON object, no markdown fences, no extra commentary, in exactly this shape:
                {"explanation": "...", "recommendedNextStep": "..."}

                PATHWAY: %s (%s)
                OVERALL OUTCOME: %s

                READINESS: requirement coverage %s%%, evidence coverage %s%%, consistency %s%%, overall %s%%

                REQUIREMENTS:
                %s

                MISSING EVIDENCE:
                %s

                PATHWAY EVIDENCE GUIDANCE: %s
                """.formatted(
                context.pathwayName(), context.pathwayKey(),
                context.overallOutcome(),
                context.readiness().requirementCoveragePercent(),
                context.readiness().evidenceCoveragePercent(),
                context.readiness().consistencyPercent(),
                context.readiness().overallReadinessPercent(),
                requirementRowsBlock,
                missingEvidenceBlock,
                context.recommendedEvidenceGuidance() != null ? context.recommendedEvidenceGuidance() : "None provided."
        );
    }

    private String describeRow(RequirementEvidenceMatrixRowResponse row) {

        return "- %s (%s): mandatory=%s, outcome=%s, supportStatus=%s, certainty=%s, regulatorySource=%s, missingFactKeys=%s, unresolvedConflicts=%d, existingExplanation=%s".formatted(
                row.requirementTitle(), row.requirementKey(),
                Boolean.TRUE.equals(row.mandatory()),
                row.rawOutcome(), row.supportStatus(), row.certaintyLevel(),
                row.regulatorySourceAuthority() != null ? row.regulatorySourceAuthority() : "Not recorded",
                row.missingFactKeys().isEmpty() ? "None" : String.join(", ", row.missingFactKeys()),
                row.conflictingFactConflictIds().size(),
                StringUtils.hasText(row.explanation()) ? row.explanation() : "None recorded."
        );
    }

    // =========================================================================
    // RESULT ASSEMBLY
    // =========================================================================

    private ExplainPathwayAssessmentResult buildResult(
            CaseIntelligenceResponse context,
            AgentGroundingState groundingState,
            AiExplanationStatus aiExplanationStatus,
            String explanation,
            String recommendedNextStep
    ) {

        boolean humanReviewRequired = determineHumanReviewRequired(context, groundingState);

        return new ExplainPathwayAssessmentResult(
                context.pathwayAssessmentId(),
                context.pathwayKey(),
                context.pathwayName(),
                context.overallOutcome(),
                context.readiness(),
                context.evidenceMatrix(),
                context.missingEvidence(),
                context.recommendedEvidenceGuidance(),
                explanation,
                groundingState,
                aiExplanationStatus,
                recommendedNextStep,
                humanReviewRequired
        );
    }

    private boolean determineHumanReviewRequired(CaseIntelligenceResponse context, AgentGroundingState groundingState) {

        if (groundingState == AgentGroundingState.INSUFFICIENT_EVIDENCE) {
            return true;
        }

        if (context.overallOutcome() == RequirementEvaluationOutcome.CONFLICTED
                || context.overallOutcome() == RequirementEvaluationOutcome.PENDING_REVIEW) {
            return true;
        }

        return context.evidenceMatrix().stream()
                .anyMatch(row -> !row.conflictingFactConflictIds().isEmpty());
    }

    // =========================================================================
    // SERIALIZATION
    // =========================================================================

    private String serialize(ExplainPathwayAssessmentResult result) {

        try {

            return objectMapper.writeValueAsString(result);

        } catch (Exception exception) {

            throw new IllegalStateException("Failed to serialize the agent run result.", exception);
        }
    }

    private ExplainPathwayAssessmentResult deserialize(String resultJson) {

        if (!StringUtils.hasText(resultJson)) {
            return null;
        }

        try {

            return objectMapper.readValue(resultJson, ExplainPathwayAssessmentResult.class);

        } catch (Exception exception) {

            log.error("Failed to deserialize a persisted agent run result.", exception);
            return null;
        }
    }

    /**
     * Never persists a raw internal exception message, SQL error, stack
     * trace, filesystem path, provider error, or internal class name - only
     * ever one of this service's own pre-written, already-safe {@link
     * AiServiceException} messages, or a fixed generic statement for
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
