package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementEvaluationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationConflict;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationFact;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementFactBinding;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.DerivedValueRecord;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationContext;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationFactView;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.FactEvidenceExpectation;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.IndeterminateFlavor;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.KleeneValue;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicEvaluationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.NodeResult;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RegulatoryVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationFactRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementFactBindingRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * REQUIREMENT EVALUATION SERVICE
 * ============================================================================
 *
 * Orchestrates applying one {@code Requirement} to one subject's Facts as of
 * one assessment date (Requirement/Pathway Architecture Specification,
 * sections 1, 4, 11, 12, 13). This is the ONLY place a
 * {@code RequirementEvaluation} is ever created.
 *
 * Authorization is enforced exactly once per call, transitively, via
 * {@link TemporalFactResolver#resolve} - which either delegates to the
 * already-authorized {@code DigitalTwinProjectionService} (live path) or
 * performs its own explicit {@code FactAuthorizationService} check
 * (historical path). No Fact is ever read here directly.
 *
 * {@code REQUIREMENT_REF} nodes (a Requirement's own COMPOSITE logic, or a
 * Pathway's composition tree) are resolved by recursively evaluating the
 * referenced Requirement through this same service, with cycle detection
 * and memoization scoped to one top-level evaluation call - a shared
 * Requirement referenced twice in one tree is computed once.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class RequirementEvaluationService {

    private final RequirementRepository requirementRepository;
    private final RequirementFactBindingRepository factBindingRepository;
    private final RegulatoryVersionRepository regulatoryVersionRepository;
    private final RequirementEvaluationRepository evaluationRepository;
    private final RequirementEvaluationFactRepository evaluationFactRepository;
    private final RequirementEvaluationConflictRepository evaluationConflictRepository;

    private final TemporalFactResolver temporalFactResolver;
    private final LogicEvaluationService logicEvaluationService;
    private final EvaluationCertaintyCalculator certaintyCalculator;
    private final FactAuthorizationService factAuthorizationService;
    private final ObjectMapper objectMapper;

    /**
     * Internal, per-call bookkeeping - never shared across HTTP requests.
     * Cycle detection and memoization are scoped to one evaluation tree.
     */
    static final class EvaluationRun {

        final AuthenticatedUser actor;
        final Long subjectUserId;
        final LocalDateTime assessmentDate;
        final EvaluationFactView factView;
        /** When false (Pathway Discovery only), {@link #performEvaluation} never writes a row - see {@link #finish}. */
        final boolean persistResults;
        final Set<Long> currentlyEvaluating = new LinkedHashSet<>();
        final Map<Long, RequirementEvaluation> computed = new HashMap<>();
        /** In-memory mirror of {@link #toNodeResult(RequirementEvaluation)}, populated without a DB read either way - the only thing a transient run can use to resolve a REQUIREMENT_REF. */
        final Map<Long, NodeResult> computedNodeResults = new HashMap<>();

        EvaluationRun(AuthenticatedUser actor, Long subjectUserId, LocalDateTime assessmentDate, EvaluationFactView factView) {
            this(actor, subjectUserId, assessmentDate, factView, true);
        }

        EvaluationRun(AuthenticatedUser actor, Long subjectUserId, LocalDateTime assessmentDate, EvaluationFactView factView, boolean persistResults) {
            this.actor = actor;
            this.subjectUserId = subjectUserId;
            this.assessmentDate = assessmentDate;
            this.factView = factView;
            this.persistResults = persistResults;
        }
    }

    // =========================================================================
    // PUBLIC ENTRY POINT
    // =========================================================================

    /** {@code assessmentDate == null} means "now" - see {@link TemporalFactResolver}. */
    @Transactional
    public RequirementEvaluationResponse evaluate(
            AuthenticatedUser actor,
            Long requirementId,
            Long subjectUserId,
            LocalDateTime assessmentDate
    ) {

        EvaluationFactView factView = temporalFactResolver.resolve(actor, subjectUserId, assessmentDate);
        LocalDateTime resolvedDate = assessmentDate != null ? assessmentDate : LocalDateTime.now();

        EvaluationRun run = new EvaluationRun(actor, subjectUserId, resolvedDate, factView);

        RequirementEvaluation evaluation = evaluateAndPersist(requirementId, run);

        return toResponse(evaluation);
    }

    // =========================================================================
    // RECURSIVE EVALUATION (also the REQUIREMENT_REF resolver)
    // =========================================================================

    /**
     * Resolves one Requirement within a shared evaluation run, honoring
     * memoization and cycle detection. Package-visible so
     * {@code PathwayAssessmentService} can share ONE run (and therefore one
     * Fact view, and one memoization cache) across every Requirement a
     * Pathway's composition tree references - a Requirement referenced from
     * two branches of the same Pathway is evaluated once, not twice.
     */
    RequirementEvaluation evaluateAndPersist(Long requirementId, EvaluationRun run) {

        RequirementEvaluation cached = run.computed.get(requirementId);

        if (cached != null) {
            return cached;
        }

        if (!run.currentlyEvaluating.add(requirementId)) {
            throw new IllegalStateException(
                    "Circular requirement dependency detected involving requirement " + requirementId
            );
        }

        try {

            RequirementEvaluation evaluation = performEvaluation(requirementId, run);
            run.computed.put(requirementId, evaluation);
            return evaluation;

        } finally {
            run.currentlyEvaluating.remove(requirementId);
        }
    }

    private RequirementEvaluation performEvaluation(Long requirementId, EvaluationRun run) {

        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement not found."));

        if (requirement.getStatus() != RequirementStatus.PUBLISHED) {

            return finish(
                    requirement,
                    RequirementEvaluationOutcome.UNKNOWN,
                    certaintyCalculator.notApplicable(),
                    "Requirement is not currently published (status=" + requirement.getStatus() + ").",
                    Set.of(),
                    Set.of(),
                    List.of(),
                    run
            );
        }

        RegulatoryVersion regulatoryVersion = regulatoryVersionRepository.findById(requirement.getRegulatoryVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Regulatory version not found."));

        Map<String, FactEvidenceExpectation> expectations = loadEvidenceExpectations(requirementId);

        EvaluationContext context = new EvaluationContext(
                run.subjectUserId,
                run.assessmentDate,
                run.factView,
                expectations,
                // Persisting runs keep the ORIGINAL DB-backed resolution path unchanged; only a
                // transient (Pathway Discovery) run uses the in-memory computedNodeResults cache -
                // see resolveNodeResult. Never a second evaluation algorithm, just a second source
                // for the same NodeResult.
                refRequirementId -> run.persistResults
                        ? toNodeResult(evaluateAndPersist(refRequirementId, run))
                        : resolveNodeResult(refRequirementId, run)
        );

        LogicNode applicabilityLogic = deserialize(requirement.getApplicabilityLogic());

        if (applicabilityLogic != null) {

            NodeResult applicabilityResult = logicEvaluationService.evaluate(applicabilityLogic, context);

            if (applicabilityResult.value() == KleeneValue.FALSE) {

                return finish(
                        requirement,
                        RequirementEvaluationOutcome.NOT_APPLICABLE,
                        certaintyCalculator.notApplicable(),
                        requirement.getTitle() + " - does not apply to this person.",
                        Set.of(),
                        Set.of(),
                        List.of(),
                        run
                );
            }

            // TRUE, INDETERMINATE or NOT_APPLICABLE-from-the-applicability-tree
            // itself all fall through to evaluating satisfactionLogic - an
            // inconclusive applicability check must not silently exclude the
            // requirement from consideration.
        }

        LogicNode satisfactionLogic = deserialize(requirement.getSatisfactionLogic());
        NodeResult result = logicEvaluationService.evaluate(satisfactionLogic, context);

        RequirementEvaluationOutcome outcome = outcomeFromNodeResult(result);

        EvaluationCertaintyCalculator.Result certainty =
                (outcome == RequirementEvaluationOutcome.SATISFIED || outcome == RequirementEvaluationOutcome.NOT_SATISFIED)
                        ? certaintyCalculator.forDefiniteOutcome(
                                result.certaintyScore() != null ? result.certaintyScore() : 0.0,
                                regulatoryVersion.getVerificationStatus()
                        )
                        : certaintyCalculator.notApplicable();

        return finish(
                requirement,
                outcome,
                certainty,
                buildExplanation(requirement, outcome),
                result.contributingFactIds(),
                result.unresolvedConflictIds(),
                result.derivedValues(),
                run
        );
    }

    // =========================================================================
    // PERSISTENCE
    // =========================================================================

    private RequirementEvaluation persist(
            Requirement requirement,
            RequirementEvaluationOutcome outcome,
            EvaluationCertaintyCalculator.Result certainty,
            String explanation,
            Set<Long> contributingFactIds,
            Set<Long> unresolvedConflictIds,
            List<DerivedValueRecord> derivedValues,
            EvaluationRun run
    ) {

        RequirementEvaluation evaluation = RequirementEvaluation.builder()
                .requirementId(requirement.getId())
                .regulatoryVersionId(requirement.getRegulatoryVersionId())
                .subjectUserId(run.subjectUserId)
                .assessmentDate(run.assessmentDate)
                .outcome(outcome)
                .certaintyScore(certainty.score())
                .certaintyLevel(certainty.level())
                .explanation(explanation)
                .derivedValuesJson(serializeDerivedValues(derivedValues))
                .build();

        RequirementEvaluation saved = evaluationRepository.save(evaluation);

        for (Long factId : contributingFactIds) {
            evaluationFactRepository.save(
                    RequirementEvaluationFact.builder().evaluationId(saved.getId()).factId(factId).build()
            );
        }

        for (Long conflictId : unresolvedConflictIds) {
            evaluationConflictRepository.save(
                    RequirementEvaluationConflict.builder().evaluationId(saved.getId()).conflictId(conflictId).build()
            );
        }

        return saved;
    }

    /**
     * The transient counterpart of {@link #persist} - Pathway Discovery only
     * (Phase 4). Builds the exact same {@code RequirementEvaluation} shape
     * with no id and writes nothing: no {@code RequirementEvaluation} row, no
     * {@code RequirementEvaluationFact}/{@code RequirementEvaluationConflict}
     * join rows. This is the ONLY difference from {@link #persist} - the
     * evaluation algebra above ({@code performEvaluation}) is identical
     * either way.
     */
    private RequirementEvaluation buildTransient(
            Requirement requirement,
            RequirementEvaluationOutcome outcome,
            EvaluationCertaintyCalculator.Result certainty,
            String explanation,
            List<DerivedValueRecord> derivedValues,
            EvaluationRun run
    ) {

        return RequirementEvaluation.builder()
                .requirementId(requirement.getId())
                .regulatoryVersionId(requirement.getRegulatoryVersionId())
                .subjectUserId(run.subjectUserId)
                .assessmentDate(run.assessmentDate)
                .outcome(outcome)
                .certaintyScore(certainty.score())
                .certaintyLevel(certainty.level())
                .explanation(explanation)
                .derivedValuesJson(serializeDerivedValues(derivedValues))
                .evaluatedAt(run.assessmentDate)
                .build();
    }

    /**
     * The single point where {@code performEvaluation} routes to a real,
     * persisted row ({@link #persist}) or a disposable, never-saved one
     * ({@link #buildTransient}) - decided once per run via
     * {@link EvaluationRun#persistResults}, never per requirement. Either
     * way, {@code run.computedNodeResults} is populated from the SAME
     * in-memory sets just computed by {@code performEvaluation} (no DB
     * round-trip), so a transient run can resolve a nested REQUIREMENT_REF
     * without ever reading a join table that was never written.
     */
    private RequirementEvaluation finish(
            Requirement requirement,
            RequirementEvaluationOutcome outcome,
            EvaluationCertaintyCalculator.Result certainty,
            String explanation,
            Set<Long> contributingFactIds,
            Set<Long> unresolvedConflictIds,
            List<DerivedValueRecord> derivedValues,
            EvaluationRun run
    ) {

        RequirementEvaluation evaluation = run.persistResults
                ? persist(requirement, outcome, certainty, explanation, contributingFactIds, unresolvedConflictIds, derivedValues, run)
                : buildTransient(requirement, outcome, certainty, explanation, derivedValues, run);

        run.computedNodeResults.put(requirement.getId(), toNodeResult(evaluation, contributingFactIds, unresolvedConflictIds));

        return evaluation;
    }

    /**
     * Resolves one Requirement's {@link NodeResult} within a shared run,
     * for a REQUIREMENT_REF leaf - Pathway Discovery's transient equivalent
     * of {@code toNodeResult(evaluateAndPersist(id, run))}. Correct for a
     * persisting run too (evaluateAndPersist always populates
     * computedNodeResults via {@link #finish}), but only Pathway Discovery
     * calls this directly; the original persisting call sites are untouched.
     */
    NodeResult resolveNodeResult(Long requirementId, EvaluationRun run) {
        evaluateAndPersist(requirementId, run);
        return run.computedNodeResults.get(requirementId);
    }

    // =========================================================================
    // RETRIEVAL (already-computed evaluations)
    // =========================================================================

    @Transactional(readOnly = true)
    public RequirementEvaluationResponse getEvaluation(AuthenticatedUser actor, Long evaluationId) {

        RequirementEvaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement evaluation not found."));

        factAuthorizationService.assertCanView(actor, evaluation.getSubjectUserId(), null, null, "view requirement evaluation");

        return toResponse(evaluation);
    }

    /**
     * Raw entity lookup with NO authorization check of its own - used only
     * by {@code PathwayAssessmentService.getAssessment}, which has already
     * authorized the caller against the owning PathwayAssessment's subject
     * before calling this for each linked evaluation. Never call this from
     * anywhere that has not already established that authorization.
     */
    RequirementEvaluation getEvaluationEntityForOwnedAssessment(Long evaluationId) {
        return evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement evaluation not found."));
    }

    // =========================================================================
    // MAPPING HELPERS
    // =========================================================================

    RequirementEvaluationResponse toResponse(RequirementEvaluation evaluation) {

        Requirement requirement = requirementRepository.findById(evaluation.getRequirementId())
                .orElseThrow(() -> new ResourceNotFoundException("Requirement not found."));

        List<Long> factIds = evaluationFactRepository.findByEvaluationId(evaluation.getId())
                .stream().map(RequirementEvaluationFact::getFactId).toList();

        List<Long> conflictIds = evaluationConflictRepository.findByEvaluationId(evaluation.getId())
                .stream().map(RequirementEvaluationConflict::getConflictId).toList();

        return RequirementEvaluationResponse.from(evaluation, requirement.getRequirementKey(), requirement.getTitle(), factIds, conflictIds);
    }

    NodeResult toNodeResult(RequirementEvaluation evaluation) {

        Set<Long> factIds = new HashSet<>(
                evaluationFactRepository.findByEvaluationId(evaluation.getId())
                        .stream().map(RequirementEvaluationFact::getFactId).toList()
        );

        Set<Long> conflictIds = new HashSet<>(
                evaluationConflictRepository.findByEvaluationId(evaluation.getId())
                        .stream().map(RequirementEvaluationConflict::getConflictId).toList()
        );

        return toNodeResult(evaluation, factIds, conflictIds);
    }

    /**
     * The pure mapping {@link #toNodeResult(RequirementEvaluation)} delegates
     * to after its two DB reads - extracted so {@link #finish} can build the
     * identical {@link NodeResult} from the sets it already holds in memory
     * (real evaluation or transient), with no repository call at all.
     */
    private NodeResult toNodeResult(RequirementEvaluation evaluation, Set<Long> factIds, Set<Long> conflictIds) {

        return switch (evaluation.getOutcome()) {
            case SATISFIED -> NodeResult.trueResult(scoreOf(evaluation), factIds, List.of());
            case NOT_SATISFIED, EXPIRED -> NodeResult.falseResult(scoreOf(evaluation), factIds, List.of());
            case NOT_APPLICABLE -> NodeResult.notApplicable();
            case CONFLICTED -> NodeResult.indeterminate(IndeterminateFlavor.CONFLICTED, factIds, conflictIds);
            case PENDING_REVIEW -> NodeResult.indeterminate(IndeterminateFlavor.PENDING_REVIEW, factIds, conflictIds);
            case INSUFFICIENT_EVIDENCE -> NodeResult.indeterminate(IndeterminateFlavor.INSUFFICIENT_EVIDENCE, factIds, conflictIds);
            case UNKNOWN, PARTIALLY_SATISFIED -> NodeResult.indeterminate(IndeterminateFlavor.UNKNOWN, factIds, conflictIds);
        };
    }

    private double scoreOf(RequirementEvaluation evaluation) {
        return evaluation.getCertaintyScore() != null ? evaluation.getCertaintyScore() : 0.0;
    }

    private RequirementEvaluationOutcome outcomeFromNodeResult(NodeResult result) {
        return com.godfrey.ai_immigration_document_analyzer.requirement.logic.OutcomeMapping.fromNodeResult(result);
    }

    private String buildExplanation(Requirement requirement, RequirementEvaluationOutcome outcome) {

        String reason = switch (outcome) {
            case SATISFIED -> "the available evidence meets this requirement.";
            case NOT_SATISFIED -> "the available evidence does not meet this requirement.";
            case INSUFFICIENT_EVIDENCE -> "required evidence is missing or does not meet the expected quality - not a negative finding.";
            case CONFLICTED -> "conflicting information exists for a Fact this requirement depends on.";
            case UNKNOWN -> "the evaluation could not be completed.";
            case PENDING_REVIEW -> "this requirement requires human review before a determination can stand.";
            case NOT_APPLICABLE -> "this requirement does not apply to this person.";
            case EXPIRED -> "the supporting evidence is no longer within its validity window.";
            case PARTIALLY_SATISFIED -> "some but not all parts of this requirement are currently satisfied.";
        };

        return requirement.getTitle() + " - " + outcome.name() + ": " + reason;
    }

    private Map<String, FactEvidenceExpectation> loadEvidenceExpectations(Long requirementId) {

        Map<String, FactEvidenceExpectation> expectations = new HashMap<>();

        for (RequirementFactBinding binding : factBindingRepository.findByRequirementId(requirementId)) {
            expectations.put(
                    binding.getFactKey(),
                    new FactEvidenceExpectation(
                            Boolean.TRUE.equals(binding.getRequiresVerification()),
                            binding.getMinimumProvenanceType()
                    )
            );
        }

        return expectations;
    }

    private LogicNode deserialize(String json) {

        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, LogicNode.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored requirement logic is not valid JSON.", e);
        }
    }

    private String serializeDerivedValues(List<DerivedValueRecord> derivedValues) {

        if (derivedValues == null || derivedValues.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(derivedValues);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize derived values.", e);
        }
    }
}
