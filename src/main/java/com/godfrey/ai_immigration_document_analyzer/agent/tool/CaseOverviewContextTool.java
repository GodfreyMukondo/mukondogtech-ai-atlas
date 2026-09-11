package com.godfrey.ai_immigration_document_analyzer.agent.tool;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseContradictionsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseSignalsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseTimelineResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.service.CaseOverviewService;
import com.godfrey.ai_immigration_document_analyzer.fact.service.DigitalTwinProjectionService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * CASE OVERVIEW CONTEXT TOOL
 * ============================================================================
 *
 * Wraps {@link CaseOverviewService#getContradictions}/{@link
 * CaseOverviewService#getTimeline}/{@link CaseOverviewService#getSignals} -
 * the SAME three already-authorized, already-deterministic bundles {@code
 * CaseIntelligenceController} exposes at {@code GET /api/cases/{
 * subjectUserId}/{contradictions,timeline,signals}} (Phase 5.4). Deliberately
 * ONE combined context operation (per the agent's own single tool-call
 * design) rather than three separate LLM-facing tools, so the orchestrator
 * makes exactly one tool call and the LLM narrates one coherent bundle - the
 * response is a thin composition record, never a second, reshaped
 * representation of any of the three DTOs it carries verbatim.
 *
 * Never a second evaluation, never a second contradiction/timeline/risk
 * calculation of its own - this tool performs no repository read and no
 * interpretation of its own beyond the one additional grounding signal
 * below.
 *
 * Authorization happens entirely inside each of the three {@code
 * CaseOverviewService} calls (via {@code DigitalTwinProjectionService
 * .getDigitalTwin} -&gt; {@code FactAuthorizationService.assertCanView}, or
 * directly for {@code getSignals}); this tool performs no repository read of
 * its own and makes no additional authorization decision. {@code
 * subjectUserId} arriving here is never trusted as an authority - it is only
 * ever used as the id independently re-verified by that boundary, exactly
 * like the existing {@code GET /api/cases/{subjectUserId}/**} endpoints.
 *
 * {@link Output#hasAnyCaseHistory} exists SOLELY so the agent service can
 * distinguish "a real case with zero conflicts/zero timeline issues/normal
 * risk" (still grounded - see the field's own Javadoc) from "no Fact
 * evidence exists for this subject at all" (ungrounded) - it is read
 * directly from the same already-authorized {@link DigitalTwinProjectionService}
 * every {@code CaseOverviewService} method already depends on, never a new
 * calculation of its own.
 * ============================================================================
 */
@Component
@RequiredArgsConstructor
public class CaseOverviewContextTool
        implements AgentTool<CaseOverviewContextTool.Input, CaseOverviewContextTool.Output> {

    private final CaseOverviewService caseOverviewService;
    private final DigitalTwinProjectionService digitalTwinProjectionService;

    @Override
    public AgentToolName name() {
        return AgentToolName.GET_CASE_OVERVIEW_CONTEXT;
    }

    @Override
    public AgentToolAccessLevel accessLevel() {
        return AgentToolAccessLevel.READ_ONLY;
    }

    @Override
    public Output invoke(AuthenticatedUser actor, Input input) {

        Long subjectUserId = input.subjectUserId();

        CaseContradictionsResponse contradictions = caseOverviewService.getContradictions(actor, subjectUserId);
        CaseTimelineResponse timeline = caseOverviewService.getTimeline(actor, subjectUserId);
        CaseSignalsResponse signals = caseOverviewService.getSignals(actor, subjectUserId);

        boolean hasAnyCaseHistory = !digitalTwinProjectionService.getDigitalTwin(actor, subjectUserId)
                .currentFacts()
                .isEmpty();

        return new Output(subjectUserId, contradictions, timeline, signals, hasAnyCaseHistory);
    }

    public record Input(
            Long subjectUserId
    ) {
    }

    /**
     * A thin composition of three already-existing, already-authorized
     * response shapes - never a second representation of anything inside
     * them.
     *
     * {@code hasAnyCaseHistory}: whether the subject has ANY current Fact at
     * all, regardless of category or whether it triggered any
     * conflict/timeline/signal. This is the sole grounding signal - "no
     * conflicts", "no timeline anomalies", and "NORMAL risk band" are all
     * still fully grounded, real, deterministically-computed results, never
     * treated as an absence of evidence.
     */
    public record Output(
            Long subjectUserId,
            CaseContradictionsResponse contradictions,
            CaseTimelineResponse timeline,
            CaseSignalsResponse signals,
            boolean hasAnyCaseHistory
    ) {
    }
}
