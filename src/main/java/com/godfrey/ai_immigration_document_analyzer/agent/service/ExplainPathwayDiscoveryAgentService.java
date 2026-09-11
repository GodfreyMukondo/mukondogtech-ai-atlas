package com.godfrey.ai_immigration_document_analyzer.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainPathwayDiscoveryRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainPathwayDiscoveryResult;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.PathwayDiscoveryAgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGoalType;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGroundingState;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRun;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentRunStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AiExplanationStatus;
import com.godfrey.ai_immigration_document_analyzer.agent.repository.AgentRunRepository;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.AgentToolName;
import com.godfrey.ai_immigration_document_analyzer.agent.tool.PathwayDiscoveryContextTool;
import com.godfrey.ai_immigration_document_analyzer.exception.AiServiceException;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayDiscoveryResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayRankingRow;
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
 * EXPLAIN PATHWAY DISCOVERY AGENT SERVICE
 * ============================================================================
 *
 * Phase 5.3's orchestrator - the upstream, pre-assessment counterpart to
 * {@code ExplainPathwayAssessmentAgentService}: "which published pathway
 * should I pursue, and why" rather than "why does my already-chosen pathway
 * have this outcome." Executes a fixed, auditable plan (see {@link
 * #PLAN_SUMMARY}) over the single registered read-only tool, grounds (or
 * honestly declines to ground) a recommendation, optionally asks the
 * existing {@link LlmService} to phrase it, and persists exactly one {@link
 * AgentRun} per invocation.
 *
 * ARCHITECTURAL INVARIANTS (identical to Phase 5.1/5.2's, restated for this goal)
 * ----------------------------------------------------------------------------
 * - The agent never touches a repository directly - only the typed {@code
 *   PathwayDiscoveryContextTool}, which itself only wraps the already-
 *   authorized, already-deterministic {@code PathwayDiscoveryService}.
 * - Authorization happens exactly once, inside the tool call, before this
 *   service reads or writes anything else. {@code subjectUserId} is never
 *   trusted as an authority - it is only ever the id independently
 *   re-verified by that boundary.
 * - The LLM is never asked to decide which pathway is best or to compute any
 *   score/outcome - only to narrate a ranking this service already read from
 *   an existing, authoritative, deterministic service. It is never invoked
 *   at all when there is nothing to explain (see {@link #isGrounded}).
 * - GROUNDING NOTE: a ranking where every pathway scores poorly is still
 *   GROUNDED - that is real, deterministically-computed signal about the
 *   case, not an absence of data. Ungrounded means there were no PUBLISHED
 *   pathways to rank at all (an empty {@code rankedPathways}), never merely
 *   an unfavorable ranking. See {@link #isGrounded}.
 * - {@code rankedPathways} in the result is populated only from the tool's
 *   output - the LLM can never introduce a pathway, score, or requirement
 *   that wasn't already there, and can never change any ranking/outcome.
 * - No hidden chain-of-thought is ever persisted - only the fixed plan
 *   summary, the tool name actually invoked, and a concise reasoning
 *   summary.
 * - GRACEFUL AI DEGRADATION (built in from the start, per Phase 5.1/5.2
 *   Production Hardening): any failure while calling the LLM or interpreting
 *   its response is caught narrowly around the LLM step ONLY and downgrades
 *   {@link AiExplanationStatus} to {@code UNAVAILABLE}/{@code FAILED} - it
 *   never aborts the request, never discards the already-loaded {@code
 *   PathwayDiscoveryResponse}, and never fabricates a recommendation to fill
 *   the gap. The run is still recorded {@code COMPLETED} because the
 *   authoritative ranking itself succeeded. See {@link #attemptLlmRecommendation}.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExplainPathwayDiscoveryAgentService {

    private static final String PLAN_SUMMARY = """
            1. Load the subject's current Pathway Discovery ranking (every PUBLISHED pathway's transient alignment) via GET_PATHWAY_DISCOVERY_CONTEXT.
            2. Check whether any published pathway was actually ranked to ground a recommendation in.
            3. If grounded, ask the language model to recommend which pathway(s) to pursue using only the data already gathered.
            4. Assemble the structured result and record this run.""";

    private static final String INSUFFICIENT_EVIDENCE_EXPLANATION =
            "Insufficient evidence to determine. There are currently no published pathways to rank, so no "
                    + "recommendation can be grounded in real data.";

    private static final String INSUFFICIENT_EVIDENCE_NEXT_STEP =
            "Check back once immigration pathways have been published, or contact support.";

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

    private final PathwayDiscoveryContextTool pathwayDiscoveryContextTool;
    private final LlmService llmService;
    private final AgentRunRepository agentRunRepository;
    private final FactAuthorizationService factAuthorizationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public PathwayDiscoveryAgentRunResponse explainPathwayDiscovery(
            AuthenticatedUser actor,
            ExplainPathwayDiscoveryRequest request
    ) {

        List<AgentToolName> toolsInvoked = new ArrayList<>();

        // ---- STEP 1: GET_PATHWAY_DISCOVERY_CONTEXT -------------------------
        // Authorization happens here, first. A denied request propagates
        // immediately - no AgentRun is created for it.
        PathwayDiscoveryResponse context = pathwayDiscoveryContextTool.invoke(
                actor,
                new PathwayDiscoveryContextTool.Input(request.getSubjectUserId())
        );
        toolsInvoked.add(AgentToolName.GET_PATHWAY_DISCOVERY_CONTEXT);

        AgentRun run = AgentRun.builder()
                .goal(AgentGoalType.EXPLAIN_PATHWAY_DISCOVERY)
                .subjectUserId(context.subjectUserId())
                .requestedByUserId(actor.getUserId())
                .planSummary(PLAN_SUMMARY)
                .status(AgentRunStatus.FAILED) // overwritten below on every path; never left stale
                .build();

        try {

            // ---- STEP 2: GROUNDING CHECK ------------------------------------
            boolean grounded = isGrounded(context);

            ExplainPathwayDiscoveryResult result;
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

                reasoningSummary = "No published pathways were found to rank - recommendation withheld rather "
                        + "than fabricated.";

                run.setStatus(AgentRunStatus.INSUFFICIENT_EVIDENCE);

            } else {

                // ---- STEP 3: LLM RECOMMENDS FROM THE ALREADY-COMPUTED RANKING ----
                // Never lets an LLM failure abort this request - see
                // attemptLlmRecommendation()'s own javadoc. model stays null:
                // LlmService.ask() intentionally exposes only the final text
                // to callers - see ExplainRequirementAgentService for the
                // full rationale.
                LlmAttemptOutcome aiOutcome = attemptLlmRecommendation(context);

                result = buildResult(
                        context,
                        AgentGroundingState.GROUNDED,
                        aiOutcome.status(),
                        aiOutcome.explanation(),
                        aiOutcome.recommendedNextStep()
                );

                reasoningSummary = aiOutcome.reasoningSummary();

                // Always COMPLETED here: the authoritative deterministic
                // ranking succeeded regardless of whether the optional AI
                // recommendation did - see aiOutcome.status() for that
                // separate outcome.
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

            return PathwayDiscoveryAgentRunResponse.from(saved, result);

        } catch (RuntimeException exception) {

            run.setToolsInvoked(String.join(",", toolsInvoked.stream().map(Enum::name).toList()));
            run.setStatus(AgentRunStatus.FAILED);
            run.setErrorMessage(safeErrorMessage(exception));
            run.setCompletedAt(LocalDateTime.now());

            agentRunRepository.save(run);

            log.error(
                    "Agent run failed | subjectUserId={}",
                    context.subjectUserId(), exception
            );

            if (exception instanceof AiServiceException aiServiceException) {
                throw aiServiceException;
            }

            throw new AiServiceException("The explain-pathway-discovery agent could not complete this run.", exception);
        }
    }

    @Transactional(readOnly = true)
    public PathwayDiscoveryAgentRunResponse getRun(AuthenticatedUser actor, Long runId) {

        AgentRun run = agentRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent run not found."));

        // Same boundary every other case-scoped retrieval in this codebase
        // uses - self, or a case worker with an active CaseAssignment for
        // this subject. Never a second, looser authorization path.
        factAuthorizationService.assertCanView(actor, run.getSubjectUserId(), null, null, "view agent run");

        ExplainPathwayDiscoveryResult result = deserialize(run.getResultJson());

        return PathwayDiscoveryAgentRunResponse.from(run, result);
    }

    // =========================================================================
    // GROUNDING
    // =========================================================================

    /**
     * Grounded iff at least one published pathway was ranked - NOT whether
     * that ranking is favorable. A ranking where every pathway scores poorly
     * is still fully grounded: that is real, deterministically-computed
     * signal the LLM may honestly narrate. Only a genuinely empty ranking
     * (no published pathways exist) withholds the LLM call.
     */
    private boolean isGrounded(PathwayDiscoveryResponse context) {
        return context.rankedPathways() != null && !context.rankedPathways().isEmpty();
    }

    // =========================================================================
    // LLM
    // =========================================================================

    /**
     * Thrown when the call to the LLM provider itself did not complete -
     * network/connection failure, timeout, rate limiting (HTTP 429), or an
     * exhausted quota/credit balance. Caught ONLY by {@link
     * #attemptLlmRecommendation} and converted to {@link
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
     * #attemptLlmRecommendation} and converted to {@link
     * AiExplanationStatus#FAILED} - it never propagates out of this service
     * and never aborts the request.
     */
    private static final class LlmResponseUnusableException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        LlmResponseUnusableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record LlmRecommendationSection(String explanation, String recommendedNextStep) {
    }

    private record LlmAttemptOutcome(
            AiExplanationStatus status,
            String explanation,
            String recommendedNextStep,
            String reasoningSummary
    ) {
    }

    /**
     * Attempts the optional AI recommendation step and NEVER lets it fail
     * the request - narrow, intentional handling of exactly the two failure
     * modes {@link #recommendWithLlm} can raise, never a blind {@code catch
     * (Exception)}. Every failure is logged in full server-side (never
     * returned to the caller) before this method returns a deterministic-
     * safe fallback: no fabricated recommendation, and the ranking's own
     * fixed {@code DISCLAIMER}-adjacent guidance text as the only permitted
     * non-AI fallback next step.
     */
    private LlmAttemptOutcome attemptLlmRecommendation(PathwayDiscoveryResponse context) {

        try {

            LlmRecommendationSection section = recommendWithLlm(context);

            return new LlmAttemptOutcome(
                    AiExplanationStatus.GENERATED,
                    section.explanation(),
                    section.recommendedNextStep(),
                    "Recommended from " + context.rankedPathways().size() + " ranked published pathway(s), "
                            + "grounded in the existing Pathway Discovery data."
            );

        } catch (LlmUnavailableException exception) {

            log.warn(
                    "AI recommendation unavailable for pathway discovery (subject {}) - deterministic ranking is "
                            + "still returned successfully | reason={}",
                    context.subjectUserId(), exception.getMessage(), exception
            );

            return new LlmAttemptOutcome(
                    AiExplanationStatus.UNAVAILABLE,
                    null,
                    fallbackNextStep(context),
                    "The deterministic pathway ranking completed successfully. The AI recommendation could not "
                            + "be generated because the AI provider was unavailable."
            );

        } catch (LlmResponseUnusableException exception) {

            log.warn(
                    "AI recommendation response unusable for pathway discovery (subject {}) - deterministic "
                            + "ranking is still returned successfully | reason={}",
                    context.subjectUserId(), exception.getMessage(), exception
            );

            return new LlmAttemptOutcome(
                    AiExplanationStatus.FAILED,
                    null,
                    fallbackNextStep(context),
                    "The deterministic pathway ranking completed successfully. The AI recommendation could not "
                            + "be generated because the AI provider's response could not be used."
            );
        }
    }

    /** The top-ranked pathway's own deterministic explanation, reused verbatim as a safe, non-fabricated fallback next step. */
    private String fallbackNextStep(PathwayDiscoveryResponse context) {

        return context.rankedPathways().stream()
                .filter(row -> row.rank() == 1)
                .findFirst()
                .map(PathwayRankingRow::explanation)
                .filter(StringUtils::hasText)
                .orElse("Review your top-ranked pathway's requirements and provide any missing evidence.");
    }

    private LlmRecommendationSection recommendWithLlm(PathwayDiscoveryResponse context) {

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

        return new LlmRecommendationSection(payload.explanation(), payload.recommendedNextStep());
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

    private String buildPrompt(PathwayDiscoveryResponse context) {

        String rankedBlock = context.rankedPathways().stream()
                .map(this::describeRow)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("None.");

        return """
                You are an AI assistant inside MukondoGTech AI's immigration case platform, helping a user \
                understand which published immigration pathway currently aligns best with their case.

                RULES:
                - Use ONLY the ranked pathway data provided below. Never invent a pathway, requirement, score, \
                document, date, or regulation not listed here.
                - Never claim a different rank, alignment score, or outcome for any pathway than what is stated \
                below - narrate the already-computed ranking, do not decide it.
                - Each pathway's own EXPLANATION (when present) is authoritative, already computed by the rules \
                engine - not written by you, and not a fact you may contradict. Treat it as grounding context: \
                summarize and clarify, but never override, replace, or produce a narrative that disagrees with it.
                - Never state or imply a probability, likelihood, or guarantee of immigration approval - the \
                alignment scores below measure evidence coverage only, exactly as the DISCLAIMER states.
                - If a pathway has missing or conflicting requirements, say so plainly - that is real information, \
                not something to soften or omit.
                - If the data below is thin, say so plainly rather than filling gaps with assumptions.
                - Do not claim to be a lawyer or immigration officer, and do not guarantee any outcome.
                - Respond with ONLY a single JSON object, no markdown fences, no extra commentary, in exactly this shape:
                {"explanation": "...", "recommendedNextStep": "..."}

                DISCLAIMER (authoritative, must not be contradicted): %s

                TOTAL PUBLISHED PATHWAYS: %s

                RANKED PATHWAYS (rank 1 = strongest current alignment):
                %s
                """.formatted(
                context.disclaimer(),
                context.totalPublishedPathways(),
                rankedBlock
        );
    }

    private String describeRow(PathwayRankingRow row) {

        return "- Rank %d: %s (%s), jurisdiction=%s, category=%s: outcome=%s, overallAlignment=%.1f%%, requirementCoverage=%.1f%%, evidenceCoverage=%.1f%%, consistency=%.1f%%, regulatoryCertainty=%s, supported=%d, partial=%d, missing=%d, conflicting=%d, needsVerification=%d, explanation=%s".formatted(
                row.rank(), row.name(), row.pathwayKey(), row.jurisdiction(), row.category(),
                row.outcome(), row.overallAlignmentScore(), row.requirementCoveragePercent(),
                row.evidenceCoveragePercent(), row.consistencyPercent(), row.regulatoryCertainty(),
                row.supportedRequirementCount(), row.partialRequirementCount(), row.missingRequirementCount(),
                row.conflictingRequirementCount(), row.needsVerificationCount(),
                StringUtils.hasText(row.explanation()) ? row.explanation() : "None recorded."
        );
    }

    // =========================================================================
    // RESULT ASSEMBLY
    // =========================================================================

    private ExplainPathwayDiscoveryResult buildResult(
            PathwayDiscoveryResponse context,
            AgentGroundingState groundingState,
            AiExplanationStatus aiExplanationStatus,
            String explanation,
            String recommendedNextStep
    ) {

        boolean humanReviewRequired = determineHumanReviewRequired(context, groundingState);

        return new ExplainPathwayDiscoveryResult(
                context.subjectUserId(),
                context.generatedAt(),
                context.totalPublishedPathways(),
                context.rankedPathways(),
                context.disclaimer(),
                explanation,
                groundingState,
                aiExplanationStatus,
                recommendedNextStep,
                humanReviewRequired
        );
    }

    private boolean determineHumanReviewRequired(PathwayDiscoveryResponse context, AgentGroundingState groundingState) {

        if (groundingState == AgentGroundingState.INSUFFICIENT_EVIDENCE) {
            return true;
        }

        return context.rankedPathways().stream()
                .filter(row -> row.rank() == 1)
                .anyMatch(row -> row.conflictingRequirementCount() > 0 || row.needsVerificationCount() > 0);
    }

    // =========================================================================
    // SERIALIZATION
    // =========================================================================

    private String serialize(ExplainPathwayDiscoveryResult result) {

        try {

            return objectMapper.writeValueAsString(result);

        } catch (Exception exception) {

            throw new IllegalStateException("Failed to serialize the agent run result.", exception);
        }
    }

    private ExplainPathwayDiscoveryResult deserialize(String resultJson) {

        if (!StringUtils.hasText(resultJson)) {
            return null;
        }

        try {

            return objectMapper.readValue(resultJson, ExplainPathwayDiscoveryResult.class);

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
