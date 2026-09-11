package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactTypeDefinition;
import com.godfrey.ai_immigration_document_analyzer.fact.policy.FactTypeRegistry;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.HypotheticalFactInput;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementDeltaRow;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ScenarioSimulationRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ScenarioSimulationResult;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.SimulatedFactEcho;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationContext;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationFactView;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.FactEvidenceExpectation;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicEvaluationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.NodeResult;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * SCENARIO SIMULATION SERVICE
 * ============================================================================
 *
 * Answers "what would change for this pathway if my case were different in
 * these specific ways" (Phase 5.5 - What-If / Scenario Simulation) -
 * entirely deterministic, no LLM dependency of any kind.
 *
 * COMPLETELY SIDE-EFFECT FREE BY CONSTRUCTION: both the BEFORE and AFTER
 * evaluations run through the SAME transient {@code
 * RequirementEvaluationService.EvaluationRun} path Pathway Discovery already
 * uses (Phase 4) - {@code persistResults=false} - so neither pass ever calls
 * {@code RequirementEvaluationRepository.save}, {@code
 * RequirementEvaluationFactRepository.save}, {@code
 * RequirementEvaluationConflictRepository.save}, or {@code
 * PathwayAssessmentRepository.save}. Nothing here writes a {@code Fact} row
 * either - the hypothetical overlay is a purely in-memory {@link
 * EvaluationFactView} built by {@link TemporalFactResolver#overlayHypothetical},
 * discarded when this method returns. Never a second evaluation engine,
 * never a second SATISFIED/NOT_SATISFIED vocabulary, never a new
 * persistence path.
 *
 * AUTHORIZATION: enforced exactly once, transitively, via {@link
 * TemporalFactResolver#resolve} - identical boundary to Pathway Discovery/
 * Assessment. {@code subjectUserId} is never trusted as an authority; it is
 * only ever the id independently re-verified by that boundary.
 *
 * PROVENANCE: every hypothetical value is built with {@link
 * FactProvenanceType#SIMULATION}/{@link FactConfidenceLevel#NOT_APPLICABLE} -
 * the existing, already-reserved provenance mechanism (never a parallel
 * one) - and exists only inside the in-memory overlay this method builds
 * and returns nothing but a delta from. It is never written to {@code
 * FactRepository}, so it can never be read back by {@code
 * DigitalTwinProjectionService}, {@code CaseOverviewService}, or any other
 * real-case query.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class ScenarioSimulationService {

    private final PathwayRepository pathwayRepository;
    private final RequirementRepository requirementRepository;

    private final TemporalFactResolver temporalFactResolver;
    private final LogicEvaluationService logicEvaluationService;
    private final RequirementEvaluationService requirementEvaluationService;
    private final PathwayOutcomeCalculator pathwayOutcomeCalculator;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ScenarioSimulationResult simulate(AuthenticatedUser actor, Long pathwayId, ScenarioSimulationRequest request) {

        Pathway pathway = pathwayRepository.findById(pathwayId)
                .filter(candidate -> candidate.getStatus() == PathwayStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Pathway not found."));

        Long subjectUserId = request.getSubjectUserId();

        // The ONE authorization check and the ONE real-case Fact resolution
        // for this entire call - identical boundary/resolver Pathway
        // Discovery and Pathway Assessment already use.
        EvaluationFactView realView = temporalFactResolver.resolve(actor, subjectUserId, null);
        LocalDateTime resolvedDate = LocalDateTime.now();

        List<FactResponse> hypotheticalFacts = buildHypotheticalFacts(request.getHypotheticalFacts(), subjectUserId, resolvedDate);

        EvaluationFactView hypotheticalView = temporalFactResolver.overlayHypothetical(realView, hypotheticalFacts);

        RunOutcome before = evaluatePathwayTransiently(pathway, actor, subjectUserId, resolvedDate, realView);
        RunOutcome after = evaluatePathwayTransiently(pathway, actor, subjectUserId, resolvedDate, hypotheticalView);

        return buildResult(pathway, subjectUserId, resolvedDate, hypotheticalFacts, before, after);
    }

    // =========================================================================
    // HYPOTHETICAL INPUT VALIDATION / CONSTRUCTION
    // =========================================================================

    /**
     * Validates every override against {@link FactTypeRegistry} (an unknown
     * {@code factKey} is rejected with {@link IllegalArgumentException} -
     * mapped to HTTP 400 by {@code GlobalExceptionHandler}, exactly like
     * every other unknown-key rejection in this codebase), parses {@code
     * value} into the ONE typed field the registry's own {@code
     * FactValueType} calls for, and tags every result with {@code
     * FactProvenanceType.SIMULATION}. Never accepts arbitrary executable
     * logic or an arbitrary database field - only a factKey/value pair
     * checked against the closed, existing taxonomy.
     */
    private List<FactResponse> buildHypotheticalFacts(
            List<HypotheticalFactInput> inputs,
            Long subjectUserId,
            LocalDateTime resolvedDate
    ) {

        Set<String> seenFactKeys = new LinkedHashSet<>();
        List<FactResponse> built = new java.util.ArrayList<>(inputs.size());

        for (HypotheticalFactInput input : inputs) {

            String normalizedKey = input.getFactKey() == null ? null : input.getFactKey().trim().toUpperCase();

            if (!seenFactKeys.add(normalizedKey)) {
                throw new IllegalArgumentException(
                        "Duplicate hypothetical fact key in one simulation request: " + input.getFactKey()
                );
            }

            built.add(buildHypotheticalFact(input, subjectUserId, resolvedDate));
        }

        return built;
    }

    private FactResponse buildHypotheticalFact(HypotheticalFactInput input, Long subjectUserId, LocalDateTime resolvedDate) {

        // Throws IllegalArgumentException for an unrecognized key - the
        // single validation gate for "is this a supported fact concept."
        FactTypeDefinition definition = FactTypeRegistry.get(input.getFactKey());

        String stringValue = null;
        LocalDateTime dateValue = null;
        Double numberValue = null;
        Boolean booleanValue = null;

        switch (definition.valueType()) {

            case STRING -> stringValue = input.getValue();

            case NUMBER -> numberValue = parseNumber(input.getValue(), definition.factKey());

            case BOOLEAN -> booleanValue = parseBoolean(input.getValue(), definition.factKey());

            case DATE -> dateValue = parseDate(input.getValue(), definition.factKey());
        }

        return new FactResponse(
                null,
                subjectUserId,
                definition.category(),
                definition.factKey(),
                definition.valueType(),
                stringValue,
                dateValue,
                numberValue,
                booleanValue,
                FactStatus.ACCEPTED,
                FactProvenanceType.SIMULATION,
                definition.sensitivityTier(),
                null,
                FactConfidenceLevel.NOT_APPLICABLE,
                "Hypothetical value supplied for scenario simulation - not a real, evidenced Fact.",
                false,
                null,
                null,
                resolvedDate,
                null,
                resolvedDate,
                resolvedDate,
                resolvedDate,
                null,
                List.of()
        );
    }

    private Double parseNumber(String rawValue, String factKey) {
        try {
            return Double.valueOf(rawValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Hypothetical value for " + factKey + " must be numeric: " + rawValue
            );
        }
    }

    private Boolean parseBoolean(String rawValue, String factKey) {

        if ("true".equalsIgnoreCase(rawValue)) {
            return Boolean.TRUE;
        }

        if ("false".equalsIgnoreCase(rawValue)) {
            return Boolean.FALSE;
        }

        throw new IllegalArgumentException(
                "Hypothetical value for " + factKey + " must be true or false: " + rawValue
        );
    }

    private LocalDateTime parseDate(String rawValue, String factKey) {
        try {
            return LocalDateTime.parse(rawValue);
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Hypothetical value for " + factKey + " must be an ISO-8601 date-time: " + rawValue
            );
        }
    }

    // =========================================================================
    // ONE PATHWAY - TRANSIENT EVALUATION (persistResults=false, see EvaluationRun)
    // =========================================================================

    private record RunOutcome(RequirementEvaluationOutcome pathwayOutcome, Map<Long, RequirementEvaluation> computed) {
    }

    private RunOutcome evaluatePathwayTransiently(
            Pathway pathway,
            AuthenticatedUser actor,
            Long subjectUserId,
            LocalDateTime resolvedDate,
            EvaluationFactView factView
    ) {

        RequirementEvaluationService.EvaluationRun run =
                new RequirementEvaluationService.EvaluationRun(actor, subjectUserId, resolvedDate, factView, false);

        EvaluationContext context = new EvaluationContext(
                subjectUserId,
                resolvedDate,
                factView,
                Map.<String, FactEvidenceExpectation>of(),
                refRequirementId -> requirementEvaluationService.resolveNodeResult(refRequirementId, run)
        );

        LogicNode compositionLogic = deserialize(pathway.getCompositionLogic());
        NodeResult root = logicEvaluationService.evaluate(compositionLogic, context);

        RequirementEvaluationOutcome outcome = pathwayOutcomeCalculator.resolvePathwayOutcome(root, run.computed.values());

        return new RunOutcome(outcome, Map.copyOf(run.computed));
    }

    // =========================================================================
    // RESULT ASSEMBLY / DELTA
    // =========================================================================

    private ScenarioSimulationResult buildResult(
            Pathway pathway,
            Long subjectUserId,
            LocalDateTime resolvedDate,
            List<FactResponse> hypotheticalFacts,
            RunOutcome before,
            RunOutcome after
    ) {

        List<SimulatedFactEcho> echoes = hypotheticalFacts.stream()
                .map(fact -> new SimulatedFactEcho(fact.factKey(), rawValueOf(fact), FactProvenanceType.SIMULATION))
                .toList();

        List<RequirementDeltaRow> deltas = buildDeltas(before, after);

        return new ScenarioSimulationResult(
                subjectUserId,
                pathway.getId(),
                pathway.getPathwayKey(),
                pathway.getName(),
                resolvedDate,
                echoes,
                before.pathwayOutcome(),
                after.pathwayOutcome(),
                before.pathwayOutcome() != after.pathwayOutcome(),
                deltas,
                ScenarioSimulationResult.DISCLAIMER
        );
    }

    /**
     * Union of every Requirement either run actually reached - a
     * requirement present on only one side (e.g. a hypothetical change
     * altered applicability) is real, meaningful simulation information,
     * never silently dropped or treated as "unchanged".
     */
    private List<RequirementDeltaRow> buildDeltas(RunOutcome before, RunOutcome after) {

        Set<Long> requirementIds = new LinkedHashSet<>();
        requirementIds.addAll(before.computed().keySet());
        requirementIds.addAll(after.computed().keySet());

        Map<Long, RequirementDeltaRow> rows = new LinkedHashMap<>();

        for (Long requirementId : requirementIds) {

            Requirement requirement = requirementRepository.findById(requirementId).orElse(null);

            if (requirement == null) {
                continue;
            }

            RequirementEvaluation beforeEvaluation = before.computed().get(requirementId);
            RequirementEvaluation afterEvaluation = after.computed().get(requirementId);

            RequirementEvaluationOutcome beforeOutcome = beforeEvaluation != null
                    ? beforeEvaluation.getOutcome() : RequirementEvaluationOutcome.NOT_APPLICABLE;

            RequirementEvaluationOutcome afterOutcome = afterEvaluation != null
                    ? afterEvaluation.getOutcome() : RequirementEvaluationOutcome.NOT_APPLICABLE;

            rows.put(requirementId, new RequirementDeltaRow(
                    requirementId,
                    requirement.getRequirementKey(),
                    requirement.getTitle(),
                    Boolean.TRUE.equals(requirement.getMandatory()),
                    beforeEvaluation != null,
                    beforeOutcome,
                    beforeEvaluation != null ? beforeEvaluation.getExplanation() : "Not evaluated in the real (current) case.",
                    afterEvaluation != null,
                    afterOutcome,
                    afterEvaluation != null ? afterEvaluation.getExplanation() : "Not evaluated under the hypothetical scenario.",
                    beforeOutcome != afterOutcome
            ));
        }

        return rows.values().stream()
                .sorted(Comparator.comparing(RequirementDeltaRow::requirementKey))
                .toList();
    }

    private String rawValueOf(FactResponse fact) {

        return switch (fact.valueType()) {
            case STRING -> fact.stringValue();
            case NUMBER -> fact.numberValue() != null ? String.valueOf(fact.numberValue()) : null;
            case BOOLEAN -> fact.booleanValue() != null ? String.valueOf(fact.booleanValue()) : null;
            case DATE -> fact.dateValue() != null ? fact.dateValue().toString() : null;
        };
    }

    private LogicNode deserialize(String json) {
        try {
            return objectMapper.readValue(json, LogicNode.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored pathway composition logic is not valid JSON.", e);
        }
    }
}
