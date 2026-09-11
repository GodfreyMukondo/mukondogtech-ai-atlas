package com.godfrey.ai_immigration_document_analyzer.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.godfrey.ai_immigration_document_analyzer.agent.dto.CaseOverviewAgentRunResponse;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainCaseOverviewRequest;
import com.godfrey.ai_immigration_document_analyzer.agent.dto.ExplainCaseOverviewResult;
import com.godfrey.ai_immigration_document_analyzer.agent.entity.AgentGoalType;
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
import com.godfrey.ai_immigration_document_analyzer.exception.AiServiceException;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactConflictResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
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
 * EXPLAIN CASE OVERVIEW AGENT SERVICE
 * ============================================================================
 *
 * Phase 5.4's orchestrator - the case-wide counterpart to {@code
 * ExplainRequirementAgentService}/{@code ExplainPathwayAssessmentAgentService}/
 * {@code ExplainPathwayDiscoveryAgentService}: "what do my case-wide
 * contradictions, timeline, and risk signals mean" rather than anything
 * about one requirement, one pathway assessment, or the pathway ranking.
 * Executes a fixed, auditable plan (see {@link #PLAN_SUMMARY}) over the
 * single registered read-only tool, grounds (or honestly declines to
 * ground) an explanation, optionally asks the existing {@link LlmService} to
 * phrase it, and persists exactly one {@link AgentRun} per invocation.
 *
 * NO OVERLAP WITH PHASE 5.1/5.2/5.3: this goal never reads a Requirement,
 * RequirementEvaluation, PathwayAssessment, or Pathway Discovery ranking -
 * it is exclusively {@code CaseOverviewService}'s case-wide, subject-scoped
 * view (contradictions/timeline/signals), independent of any one pathway or
 * requirement. {@code pathwayAssessmentId}/{@code requirementId} on the
 * persisted {@link AgentRun} are always {@code null} for this goal.
 *
 * ARCHITECTURAL INVARIANTS (identical to Phase 5.1/5.2/5.3's, restated for this goal)
 * ----------------------------------------------------------------------------
 * - The agent never touches a repository directly - only the typed {@code
 *   CaseOverviewContextTool}, which itself only wraps the already-
 *   authorized, already-deterministic {@code CaseOverviewService}.
 * - Authorization happens exactly once, inside the tool call, before this
 *   service reads or writes anything else. {@code subjectUserId} is never
 *   trusted as an authority - it is only ever the id independently
 *   re-verified by that boundary.
 * - The LLM is never asked to decide whether a contradiction exists, compute
 *   a gap/overlap, or determine a risk band - only to narrate a bundle this
 *   service already read from an existing, authoritative, deterministic
 *   service. It is never invoked at all when there is nothing to explain
 *   (see {@link #isGrounded}).
 * - GROUNDING NOTE: a case with zero open contradictions, zero timeline
 *   anomalies, and a NORMAL risk band is still GROUNDED - that is real,
 *   deterministically-computed signal about the case (a genuinely clean
 *   case), never an absence of data. Ungrounded means the subject has no
 *   Fact history at all to report on. See {@link #isGrounded}.
 * - Every deterministic field in the result is populated only from the
 *   tool's output - the LLM can never introduce a conflict, timeline event,
 *   risk signal, or fact that wasn't already there, and can never change any
 *   risk band, conflict status, or classification.
 * - No hidden chain-of-thought is ever persisted - only the fixed plan
 *   summary, the tool name actually invoked, and a concise reasoning
 *   summary.
 * - GRACEFUL AI DEGRADATION (identical contract to Phase 5.1-5.3): any
 *   failure while calling the LLM or interpreting its response is caught
 *   narrowly around the LLM step ONLY and downgrades {@link
 *   AiExplanationStatus} to {@code UNAVAILABLE}/{@code FAILED} - it never
 *   aborts the request, never discards the already-loaded case overview, and
 *   never fabricates an explanation to fill the gap. The run is still
 *   recorded {@code COMPLETED} because the authoritative overview itself
 *   succeeded. See {@link #attemptLlmExplanation}.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExplainCaseOverviewAgentService {

    private static final String PLAN_SUMMARY = """
            1. Load the subject's current case-wide contradictions, timeline, and signals bundle via GET_CASE_OVERVIEW_CONTEXT.
            2. Check whether the subject has any Fact history at all to ground an explanation in.
            3. If grounded, ask the language model to explain what the already-computed signals mean using only the data already gathered.
            4. Assemble the structured result and record this run.""";

    private static final String INSUFFICIENT_EVIDENCE_EXPLANATION =
            "Insufficient evidence to determine. There is currently no case history recorded for this subject, so "
                    + "no case overview can be grounded in real data.";

    private static final String INSUFFICIENT_EVIDENCE_NEXT_STEP =
            "Provide your case information (employment, education, residence, travel, or immigration history) to "
                    + "build a case overview.";

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

    private final CaseOverviewContextTool caseOverviewContextTool;
    private final LlmService llmService;
    private final AgentRunRepository agentRunRepository;
    private final FactAuthorizationService factAuthorizationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public CaseOverviewAgentRunResponse explainCaseOverview(
            AuthenticatedUser actor,
            ExplainCaseOverviewRequest request
    ) {

        List<AgentToolName> toolsInvoked = new ArrayList<>();

        // ---- STEP 1: GET_CASE_OVERVIEW_CONTEXT -----------------------------
        // Authorization happens here, first. A denied request propagates
        // immediately - no AgentRun is created for it.
        CaseOverviewContextTool.Output context = caseOverviewContextTool.invoke(
                actor,
                new CaseOverviewContextTool.Input(request.getSubjectUserId())
        );
        toolsInvoked.add(AgentToolName.GET_CASE_OVERVIEW_CONTEXT);

        AgentRun run = AgentRun.builder()
                .goal(AgentGoalType.EXPLAIN_CASE_OVERVIEW)
                .subjectUserId(context.subjectUserId())
                .requestedByUserId(actor.getUserId())
                .planSummary(PLAN_SUMMARY)
                .status(AgentRunStatus.FAILED) // overwritten below on every path; never left stale
                .build();

        try {

            // ---- STEP 2: GROUNDING CHECK ------------------------------------
            boolean grounded = isGrounded(context);

            ExplainCaseOverviewResult result;
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

                reasoningSummary = "No case history was found for this subject - explanation withheld rather "
                        + "than fabricated.";

                run.setStatus(AgentRunStatus.INSUFFICIENT_EVIDENCE);

            } else {

                // ---- STEP 3: LLM NARRATES THE ALREADY-COMPUTED OVERVIEW ----
                // Never lets an LLM failure abort this request - see
                // attemptLlmExplanation()'s own javadoc. model stays null:
                // LlmService.ask() intentionally exposes only the final text
                // to callers - see ExplainRequirementAgentService for the
                // full rationale.
                LlmAttemptOutcome aiOutcome = attemptLlmExplanation(context);

                result = buildResult(
                        context,
                        AgentGroundingState.GROUNDED,
                        aiOutcome.status(),
                        aiOutcome.explanation(),
                        aiOutcome.recommendedNextStep()
                );

                reasoningSummary = aiOutcome.reasoningSummary();

                // Always COMPLETED here: the authoritative deterministic
                // overview succeeded regardless of whether the optional AI
                // narration did - see aiOutcome.status() for that separate
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

            return CaseOverviewAgentRunResponse.from(saved, result);

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

            throw new AiServiceException("The explain-case-overview agent could not complete this run.", exception);
        }
    }

    @Transactional(readOnly = true)
    public CaseOverviewAgentRunResponse getRun(AuthenticatedUser actor, Long runId) {

        AgentRun run = agentRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent run not found."));

        // Same boundary every other case-scoped retrieval in this codebase
        // uses - self, or a case worker with an active CaseAssignment for
        // this subject. Never a second, looser authorization path.
        factAuthorizationService.assertCanView(actor, run.getSubjectUserId(), null, null, "view agent run");

        ExplainCaseOverviewResult result = deserialize(run.getResultJson());

        return CaseOverviewAgentRunResponse.from(run, result);
    }

    // =========================================================================
    // GROUNDING
    // =========================================================================

    /**
     * Grounded iff the subject has ANY current Fact at all - NOT whether
     * that history is favorable. A case with zero open contradictions, zero
     * timeline anomalies, and a NORMAL risk band is still fully grounded:
     * that is a real, deterministically-computed result about the case
     * (genuinely clean), not an absence of data. Only a subject with no Fact
     * history whatsoever withholds the LLM call - see {@code
     * CaseOverviewContextTool.Output#hasAnyCaseHistory}.
     */
    private boolean isGrounded(CaseOverviewContextTool.Output context) {
        return context.hasAnyCaseHistory();
    }

    // =========================================================================
    // HUMAN REVIEW
    // =========================================================================

    /**
     * Grounded purely in already-computed {@code CaseOverviewService}
     * signals and its own documented policy ("an elevated band means a case
     * worker should look closer") - never a new judgment invented here:
     * an open (unresolved) contradiction, or a HIGH/CRITICAL risk band.
     */
    private boolean determineHumanReviewRequired(CaseOverviewContextTool.Output context, AgentGroundingState groundingState) {

        if (groundingState == AgentGroundingState.INSUFFICIENT_EVIDENCE) {
            return true;
        }

        boolean hasOpenContradiction = !context.contradictions().openContradictions().isEmpty();
        CaseRiskBand riskBand = context.signals().riskBand();
        boolean elevatedRisk = riskBand == CaseRiskBand.HIGH || riskBand == CaseRiskBand.CRITICAL;

        return hasOpenContradiction || elevatedRisk;
    }

    // =========================================================================
    // LLM
    // =========================================================================

    /**
     * Thrown when the call to the LLM provider itself did not complete -
     * network/connection failure, timeout, rate limiting (HTTP 429), or an
     * exhausted quota/credit balance. Caught ONLY by {@link
     * #attemptLlmExplanation} and converted to {@link
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
     * #attemptLlmExplanation} and converted to {@link
     * AiExplanationStatus#FAILED} - it never propagates out of this service
     * and never aborts the request.
     */
    private static final class LlmResponseUnusableException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        LlmResponseUnusableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record LlmExplanationSection(String explanation, String recommendedNextStep) {
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
     * modes {@link #explainWithLlm} can raise, never a blind {@code catch
     * (Exception)}. Every failure is logged in full server-side (never
     * returned to the caller) before this method returns a deterministic-
     * safe fallback: no fabricated explanation, and a fixed, non-AI fallback
     * next step.
     */
    private LlmAttemptOutcome attemptLlmExplanation(CaseOverviewContextTool.Output context) {

        try {

            LlmExplanationSection section = explainWithLlm(context);

            return new LlmAttemptOutcome(
                    AiExplanationStatus.GENERATED,
                    section.explanation(),
                    section.recommendedNextStep(),
                    "Explained from the case's existing contradictions/timeline/signals bundle, grounded in the "
                            + "already-computed Case Overview data."
            );

        } catch (LlmUnavailableException exception) {

            log.warn(
                    "AI explanation unavailable for case overview (subject {}) - deterministic overview is "
                            + "still returned successfully | reason={}",
                    context.subjectUserId(), exception.getMessage(), exception
            );

            return new LlmAttemptOutcome(
                    AiExplanationStatus.UNAVAILABLE,
                    null,
                    fallbackNextStep(context),
                    "The deterministic case overview completed successfully. The AI explanation could not be "
                            + "generated because the AI provider was unavailable."
            );

        } catch (LlmResponseUnusableException exception) {

            log.warn(
                    "AI explanation response unusable for case overview (subject {}) - deterministic overview is "
                            + "still returned successfully | reason={}",
                    context.subjectUserId(), exception.getMessage(), exception
            );

            return new LlmAttemptOutcome(
                    AiExplanationStatus.FAILED,
                    null,
                    fallbackNextStep(context),
                    "The deterministic case overview completed successfully. The AI explanation could not be "
                            + "generated because the AI provider's response could not be used."
            );
        }
    }

    /** A safe, non-fabricated fallback next step derived purely from already-computed signals. */
    private String fallbackNextStep(CaseOverviewContextTool.Output context) {

        if (!context.contradictions().openContradictions().isEmpty()) {
            return "Review and resolve your open contradictions on the Immigration Profile page.";
        }

        CaseRiskBand riskBand = context.signals().riskBand();

        if (riskBand == CaseRiskBand.HIGH || riskBand == CaseRiskBand.CRITICAL) {
            return "A case worker should review your case signals.";
        }

        return "No action is currently required - your case overview shows no open issues.";
    }

    private LlmExplanationSection explainWithLlm(CaseOverviewContextTool.Output context) {

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

            throw new LlmResponseUnusableException(
                    "The AI provider response could not be interpreted.",
                    exception
            );
        }
    }

    private String buildPrompt(CaseOverviewContextTool.Output context) {

        CaseContradictionsResponse contradictions = context.contradictions();
        CaseTimelineResponse timeline = context.timeline();
        CaseSignalsResponse signals = context.signals();

        String contradictionsBlock = contradictions.openContradictions().isEmpty()
                ? "None."
                : contradictions.openContradictions().stream()
                        .map(this::describeContradiction)
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("None.");

        String overlapsBlock = contradictions.potentialOverlaps().isEmpty()
                ? "None."
                : contradictions.potentialOverlaps().stream()
                        .map(this::describeOverlap)
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("None.");

        String timelineBlock = timeline.events().isEmpty()
                ? "None."
                : timeline.events().stream()
                        .map(this::describeTimelineEvent)
                        .reduce((a, b) -> a + "\n" + b)
                        .orElse("None.");

        return """
                You are an AI assistant inside MukondoGTech AI's immigration case platform, helping a user \
                understand what their case-wide contradictions, timeline, and risk signals mean.

                RULES:
                - Use ONLY the deterministic data provided below. Never invent a fact, conflict, timeline event, \
                document, date, or risk signal not listed here.
                - Never state or imply a different risk band, conflict status, or classification than what is \
                stated below - narrate the already-computed result, do not decide it.
                - The RISK BAND, CONTRADICTIONS, and TIMELINE data below are authoritative, already computed by \
                deterministic case logic - not written by you, and not something you may contradict or override.
                - Never make an eligibility or immigration-status determination of any kind - this data is about \
                case consistency and evidence signals only, never a prediction of any government decision.
                - Never claim that a fraud determination has been made - an elevated risk band or open \
                contradiction is a non-forensic signal only, requiring verification, never proof of anything.
                - Clearly distinguish what IS known (the data below) from what is missing or uncertain - never \
                claim missing evidence exists when none is listed.
                - If there are no contradictions, no timeline anomalies, and a NORMAL risk band, say so plainly - \
                a clean case overview is real, positive information, not something to embellish.
                - Recommend human review only when the data below actually supports it (an open contradiction, or \
                an elevated risk band) - never suggest review without a reason grounded in this data.
                - Do not claim to be a lawyer or immigration officer, and do not guarantee any outcome.
                - Respond with ONLY a single JSON object, no markdown fences, no extra commentary, in exactly this shape:
                {"explanation": "...", "recommendedNextStep": "..."}

                RISK BAND: %s (fraudFlaggedDocs=%d, highRiskDocs=%d, mediumRiskDocs=%d, openContradictions=%d, \
                potentialOverlaps=%d, rejectedEvidence=%d, validationFailedEvidence=%d)

                OPEN CONTRADICTIONS:
                %s

                POTENTIAL TIMELINE OVERLAPS:
                %s

                TIMELINE (%d entries, chronological):
                %s
                """.formatted(
                signals.riskBand(),
                signals.fraudFlaggedDocumentCount(), signals.highRiskDocumentCount(), signals.mediumRiskDocumentCount(),
                signals.openContradictionCount(), signals.potentialOverlapContradictionCount(),
                signals.rejectedEvidenceItemCount(), signals.validationFailedEvidenceItemCount(),
                contradictionsBlock,
                overlapsBlock,
                timeline.events().size(),
                timelineBlock
        );
    }

    private String describeContradiction(FactConflictResponse conflict) {
        return "- factKey=%s, status=%s, detectedAt=%s".formatted(
                conflict.factKey(), conflict.status(), conflict.detectedAt()
        );
    }

    private String describeOverlap(PotentialOverlapContradiction overlap) {
        return "- factKey=%s, overlapDays=%d, description=%s".formatted(
                overlap.factKey(), overlap.overlapDays(), overlap.description()
        );
    }

    private String describeTimelineEvent(CaseTimelineEventResponse event) {
        return "- factKey=%s, category=%s, effectiveFrom=%s, effectiveTo=%s, gapDaysBefore=%s, overlapDaysWithPrevious=%s".formatted(
                event.fact().factKey(), event.fact().category(),
                event.fact().effectiveFrom(), event.fact().effectiveTo(),
                event.gapDaysBeforeThisEntry(), event.overlapDaysWithPreviousEntry()
        );
    }

    // =========================================================================
    // RESULT ASSEMBLY
    // =========================================================================

    private ExplainCaseOverviewResult buildResult(
            CaseOverviewContextTool.Output context,
            AgentGroundingState groundingState,
            AiExplanationStatus aiExplanationStatus,
            String explanation,
            String recommendedNextStep
    ) {

        boolean humanReviewRequired = determineHumanReviewRequired(context, groundingState);

        return new ExplainCaseOverviewResult(
                context.subjectUserId(),
                LocalDateTime.now(),
                context.contradictions(),
                context.timeline(),
                context.signals(),
                explanation,
                groundingState,
                aiExplanationStatus,
                recommendedNextStep,
                humanReviewRequired
        );
    }

    // =========================================================================
    // SERIALIZATION
    // =========================================================================

    private String serialize(ExplainCaseOverviewResult result) {

        try {

            return objectMapper.writeValueAsString(result);

        } catch (Exception exception) {

            throw new IllegalStateException("Failed to serialize the agent run result.", exception);
        }
    }

    private ExplainCaseOverviewResult deserialize(String resultJson) {

        if (!StringUtils.hasText(resultJson)) {
            return null;
        }

        try {

            return objectMapper.readValue(resultJson, ExplainCaseOverviewResult.class);

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
