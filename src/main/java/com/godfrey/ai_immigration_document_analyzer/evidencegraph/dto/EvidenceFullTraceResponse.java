package com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;

import java.util.List;
import java.util.Map;

/**
 * The full Pathway -&gt; Requirement -&gt; RequirementEvaluation -&gt; Fact -&gt;
 * Evidence -&gt; Document chain for one PathwayAssessment (Evidence
 * Intelligence Graph design, section 7 and 17).
 *
 * Composed BY REFERENCE around the existing, unmodified
 * {@link ExplanationResponse} rather than by extending or duplicating it -
 * {@link #explanation} is exactly what
 * {@code ExplainabilityService.explain(...)} already returns today, so
 * every existing consumer of that response shape keeps working unchanged.
 * {@link #evidenceByFactId} is the one addition: for every Fact id that
 * appears anywhere in {@link #explanation}, the richer evidence-item detail
 * (source data / system interpretation / derived conclusion) this feature
 * adds - empty where no {@code EvidenceItem} exists yet for that Fact.
 */
public record EvidenceFullTraceResponse(
        ExplanationResponse explanation,
        Map<Long, List<EvidenceItemResponse>> evidenceByFactId
) {
}
