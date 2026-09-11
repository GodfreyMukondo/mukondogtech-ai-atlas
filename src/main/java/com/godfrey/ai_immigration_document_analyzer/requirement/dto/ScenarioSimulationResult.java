package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The complete, structured answer to "what would change for this pathway if
 * my case were different in these specific ways" (Phase 5.5). Every field
 * here is either an echo of the caller's own hypothetical input ({@link
 * #hypotheticalInput}) or a value the existing, unmodified evaluation engine
 * produced ({@link #beforePathwayOutcome}/{@link #afterPathwayOutcome}/
 * {@link #requirementDeltas}) - nothing here is AI-generated, and nothing
 * here is ever persisted (see {@code ScenarioSimulationService} - this
 * result exists only for the duration of one HTTP response).
 *
 * {@link #afterPathwayOutcome} always reflects the COMBINED effect of every
 * entry in {@link #hypotheticalInput} together - never attributed to any
 * single override in isolation, since the deterministic engine only ever
 * evaluates the whole overlay at once.
 */
public record ScenarioSimulationResult(
        Long subjectUserId,
        Long pathwayId,
        String pathwayKey,
        String pathwayName,
        LocalDateTime generatedAt,

        /** Exactly what was simulated - every entry carries explicit SIMULATION provenance. */
        List<SimulatedFactEcho> hypotheticalInput,

        /** The pathway's REAL, current deterministic outcome - unaffected by this simulation. */
        RequirementEvaluationOutcome beforePathwayOutcome,

        /** The pathway's deterministic outcome IF every hypothetical override above were true. */
        RequirementEvaluationOutcome afterPathwayOutcome,

        boolean pathwayOutcomeChanged,

        /** Per-requirement BEFORE/AFTER comparison, in requirement-key order. */
        List<RequirementDeltaRow> requirementDeltas,

        /** Fixed, non-AI-generated disclaimer - always rendered verbatim, never paraphrased. */
        String disclaimer
) {

    public static final String DISCLAIMER =
            "This is a hypothetical simulation. Your real case and evidence have not been changed. This is not "
                    + "immigration advice, legal advice, an eligibility guarantee, or a prediction of any "
                    + "government decision.";
}
