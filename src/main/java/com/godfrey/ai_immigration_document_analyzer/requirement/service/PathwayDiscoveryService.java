package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.service.RequirementReadinessCalculator;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayDiscoveryResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayRankingRow;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.TopMissingRequirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementFactBinding;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationContext;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationFactView;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.FactEvidenceExpectation;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicEvaluationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.NodeResult;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RegulatoryVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementFactBindingRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ============================================================================
 * PATHWAY DISCOVERY SERVICE
 * ============================================================================
 *
 * Answers "which published Pathways currently align with my case, and why"
 * (Phase 4 - Pathway Discovery &amp; Explainable Ranking) - the upstream
 * question that sits BEFORE a subject ever picks one Pathway to request a
 * formal {@link PathwayAssessmentService#assess assessment} for.
 *
 * READ-ONLY BY DESIGN: unlike {@code PathwayAssessmentService.assess()},
 * nothing here writes a {@code PathwayAssessment} or {@code
 * RequirementEvaluation} row. Every published Pathway is evaluated through
 * the SAME {@link LogicEvaluationService}/{@link RequirementEvaluationService}
 * composition logic {@code PathwayAssessmentService} uses - via a transient
 * {@code RequirementEvaluationService.EvaluationRun} (persistResults=false,
 * see {@code RequirementEvaluationService.finish}) - never a second
 * evaluation engine, never a second readiness formula, never a second
 * SATISFIED/MISSING/CONFLICTING vocabulary (both reused from Case
 * Intelligence's existing {@link RequirementReadinessCalculator} and {@link
 * CaseRequirementSupportStatus}).
 *
 * PERFORMANCE: the subject's Facts are resolved exactly ONCE via {@link
 * TemporalFactResolver#resolve} (which is also where authorization is
 * enforced, exactly once, before any Pathway is read) and the same {@link
 * EvaluationFactView} is passed to every Pathway's evaluation - no Pathway
 * triggers its own Fact/Digital-Twin query. Each referenced Requirement's own
 * row is then a single indexed primary-key read, bounded by how many
 * Requirements one Pathway's composition tree references (typically a
 * handful) - not by the number of Facts or Documents in the case.
 *
 * PATHWAY-AGNOSTIC BY CONSTRUCTION, exactly like {@code CaseIntelligenceService}:
 * nothing below branches on a pathwayKey, requirementKey, or jurisdiction.
 * Every Pathway returned by {@code pathwayRepository.findByStatus(PUBLISHED)}
 * is evaluated identically - a newly published Pathway is discoverable with
 * zero source changes.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class PathwayDiscoveryService {

    /** Small and fixed - Discovery surfaces only the requirements most worth the user's attention, never a full matrix (that stays behind "Assess this pathway" -> Case Intelligence). */
    private static final int MAX_TOP_MISSING_REQUIREMENTS = 5;

    private final PathwayRepository pathwayRepository;
    private final RequirementRepository requirementRepository;
    private final RequirementFactBindingRepository requirementFactBindingRepository;
    private final RegulatoryVersionRepository regulatoryVersionRepository;

    private final TemporalFactResolver temporalFactResolver;
    private final LogicEvaluationService logicEvaluationService;
    private final RequirementEvaluationService requirementEvaluationService;
    private final PathwayOutcomeCalculator pathwayOutcomeCalculator;
    private final RequirementReadinessCalculator requirementReadinessCalculator;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PathwayDiscoveryResponse discover(AuthenticatedUser actor, Long subjectUserId) {

        // The ONE authorization check and the ONE Fact resolution for this
        // entire call - identical boundary and identical resolver
        // PathwayAssessmentService.assess() uses (see TemporalFactResolver).
        EvaluationFactView factView = temporalFactResolver.resolve(actor, subjectUserId, null);
        LocalDateTime resolvedDate = LocalDateTime.now();

        List<Pathway> publishedPathways = pathwayRepository.findByStatus(PathwayStatus.PUBLISHED);

        List<PathwayRankingRow> unranked = publishedPathways.stream()
                .map(pathway -> evaluatePathway(pathway, actor, subjectUserId, resolvedDate, factView))
                .toList();

        List<PathwayRankingRow> ranked = rank(unranked);

        return new PathwayDiscoveryResponse(
                subjectUserId, resolvedDate, publishedPathways.size(), ranked, PathwayDiscoveryResponse.DISCLAIMER
        );
    }

    // =========================================================================
    // ONE PATHWAY - TRANSIENT EVALUATION (no persistence, see EvaluationRun)
    // =========================================================================

    private PathwayRankingRow evaluatePathway(
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

        return toRankingRow(pathway, outcome, run);
    }

    // =========================================================================
    // ONE PATHWAY - AGGREGATION (reuses RequirementReadinessCalculator, never a second formula)
    // =========================================================================

    private record RequirementRow(
            Long requirementId,
            String requirementKey,
            String requirementTitle,
            boolean mandatory,
            CaseRequirementSupportStatus supportStatus,
            List<String> missingFactKeys
    ) {
    }

    private PathwayRankingRow toRankingRow(Pathway pathway, RequirementEvaluationOutcome pathwayOutcome, RequirementEvaluationService.EvaluationRun run) {

        List<RequirementEvaluation> evaluations = List.copyOf(run.computed.values());

        List<RequirementRow> rows = evaluations.stream()
                .map(evaluation -> toRequirementRow(evaluation, run))
                .sorted(Comparator.comparing(RequirementRow::requirementKey))
                .toList();

        List<RequirementReadinessCalculator.MandatoryOutcome> mandatoryOutcomes = evaluations.stream()
                .map(evaluation -> new RequirementReadinessCalculator.MandatoryOutcome(isMandatory(evaluation.getRequirementId()), evaluation.getOutcome()))
                .toList();

        RequirementReadinessCalculator.ReadinessMetrics metrics = requirementReadinessCalculator.computeReadiness(mandatoryOutcomes);

        RegulatoryVerificationStatus certainty = evaluations.stream()
                .map(evaluation -> regulatoryVersionRepository.findById(evaluation.getRegulatoryVersionId()))
                .flatMap(Optional::stream)
                .map(RegulatoryVersion::getVerificationStatus)
                .min(Comparator.comparingInt(Enum::ordinal))
                .orElse(RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED);

        List<TopMissingRequirement> topMissing = rows.stream()
                .filter(row -> row.supportStatus() != CaseRequirementSupportStatus.SATISFIED
                        && row.supportStatus() != CaseRequirementSupportStatus.NOT_ASSESSABLE)
                .sorted(Comparator
                        .comparing((RequirementRow row) -> !row.mandatory())
                        .thenComparing(row -> severityRank(row.supportStatus()))
                        .thenComparing(RequirementRow::requirementKey))
                .limit(MAX_TOP_MISSING_REQUIREMENTS)
                .map(row -> new TopMissingRequirement(
                        row.requirementId(), row.requirementKey(), row.requirementTitle(),
                        row.mandatory(), row.supportStatus(), row.missingFactKeys()
                ))
                .toList();

        return new PathwayRankingRow(
                0, // rank assigned by rank(...) once every Pathway has been scored
                pathway.getId(),
                pathway.getPathwayKey(),
                pathway.getName(),
                pathway.getJurisdiction(),
                pathway.getCategory(),
                pathwayOutcome,
                metrics.overallReadinessPercent(),
                metrics.requirementCoveragePercent(),
                metrics.evidenceCoveragePercent(),
                metrics.consistencyPercent(),
                certainty,
                countByStatus(rows, CaseRequirementSupportStatus.SATISFIED),
                countByStatus(rows, CaseRequirementSupportStatus.PARTIALLY_SUPPORTED),
                countByStatus(rows, CaseRequirementSupportStatus.MISSING),
                countByStatus(rows, CaseRequirementSupportStatus.CONFLICTING),
                countByStatus(rows, CaseRequirementSupportStatus.NEEDS_VERIFICATION),
                topMissing,
                buildExplanation(pathwayOutcome, metrics, rows.size())
        );
    }

    private RequirementRow toRequirementRow(RequirementEvaluation evaluation, RequirementEvaluationService.EvaluationRun run) {

        Requirement requirement = requirementRepository.findById(evaluation.getRequirementId())
                .orElseThrow(() -> new IllegalStateException("Requirement " + evaluation.getRequirementId() + " vanished mid-evaluation."));

        NodeResult nodeResult = run.computedNodeResults.get(evaluation.getRequirementId());
        boolean hasContributingFacts = nodeResult != null && !nodeResult.contributingFactIds().isEmpty();

        CaseRequirementSupportStatus supportStatus =
                requirementReadinessCalculator.deriveSupportStatus(evaluation.getOutcome(), hasContributingFacts);

        return new RequirementRow(
                requirement.getId(),
                requirement.getRequirementKey(),
                requirement.getTitle(),
                Boolean.TRUE.equals(requirement.getMandatory()),
                supportStatus,
                missingFactKeysFor(requirement, run)
        );
    }

    /**
     * A bound Fact key this Requirement declares but for which the already-
     * resolved {@link EvaluationFactView} (not a fresh query) has no accepted
     * Fact at all - a lighter-weight proxy for "missing" than {@code
     * CaseIntelligenceService.missingFactKeysFor}'s per-branch contributing-
     * fact diff, deliberately so: Discovery only ever needs to name what to
     * go add evidence for, never the full per-branch matrix (that stays
     * behind "Assess this pathway").
     */
    private List<String> missingFactKeysFor(Requirement requirement, RequirementEvaluationService.EvaluationRun run) {

        return requirementFactBindingRepository.findByRequirementId(requirement.getId()).stream()
                .map(RequirementFactBinding::getFactKey)
                .filter(factKey -> run.factView.factsFor(factKey).isEmpty())
                .toList();
    }

    private boolean isMandatory(Long requirementId) {
        return requirementRepository.findById(requirementId)
                .map(Requirement::getMandatory)
                .map(Boolean.TRUE::equals)
                .orElse(false);
    }

    private long countByStatus(List<RequirementRow> rows, CaseRequirementSupportStatus status) {
        return rows.stream().filter(row -> row.supportStatus() == status).count();
    }

    /** Lower = more worth the user's attention first, within one mandatory/optional tier. */
    private int severityRank(CaseRequirementSupportStatus status) {
        return switch (status) {
            case MISSING -> 0;
            case CONFLICTING -> 1;
            case NOT_SATISFIED -> 2;
            case NEEDS_VERIFICATION -> 3;
            case PARTIALLY_SUPPORTED -> 4;
            case SATISFIED, NOT_ASSESSABLE -> 5;
        };
    }

    private String buildExplanation(
            RequirementEvaluationOutcome outcome,
            RequirementReadinessCalculator.ReadinessMetrics metrics,
            int requirementCount
    ) {

        if (requirementCount == 0) {
            return "This pathway has no published requirements to evaluate yet.";
        }

        return "Ranked using " + requirementCount + " published requirement" + (requirementCount == 1 ? "" : "s")
                + " - " + Math.round(metrics.requirementCoveragePercent()) + "% of mandatory requirements currently "
                + "satisfied, " + Math.round(metrics.evidenceCoveragePercent()) + "% evidence coverage, "
                + Math.round(metrics.consistencyPercent()) + "% free of open conflicts. Current outcome: "
                + outcome.name() + ".";
    }

    // =========================================================================
    // RANKING - deterministic, documented tie-breaking (never random, never insertion order)
    // =========================================================================

    /**
     * Sort order: overall alignment desc, then requirement coverage desc,
     * then evidence coverage desc, then consistency desc, then regulatory
     * certainty desc (more trusted first), then pathwayKey asc as the final,
     * always-available deterministic tie-breaker. Two calls against the same
     * case state and the same published catalogue always produce the same
     * order.
     */
    private List<PathwayRankingRow> rank(List<PathwayRankingRow> unranked) {

        List<PathwayRankingRow> sorted = unranked.stream()
                .sorted(Comparator
                        .comparingDouble(PathwayRankingRow::overallAlignmentScore).reversed()
                        .thenComparing(Comparator.comparingDouble(PathwayRankingRow::requirementCoveragePercent).reversed())
                        .thenComparing(Comparator.comparingDouble(PathwayRankingRow::evidenceCoveragePercent).reversed())
                        .thenComparing(Comparator.comparingDouble(PathwayRankingRow::consistencyPercent).reversed())
                        .thenComparing(Comparator.<PathwayRankingRow>comparingInt(row -> row.regulatoryCertainty().ordinal()).reversed())
                        .thenComparing(PathwayRankingRow::pathwayKey))
                .toList();

        return java.util.stream.IntStream.range(0, sorted.size())
                .mapToObj(index -> withRank(sorted.get(index), index + 1))
                .toList();
    }

    private PathwayRankingRow withRank(PathwayRankingRow row, int rank) {
        return new PathwayRankingRow(
                rank, row.pathwayId(), row.pathwayKey(), row.name(), row.jurisdiction(), row.category(),
                row.outcome(), row.overallAlignmentScore(), row.requirementCoveragePercent(),
                row.evidenceCoveragePercent(), row.consistencyPercent(), row.regulatoryCertainty(),
                row.supportedRequirementCount(), row.partialRequirementCount(), row.missingRequirementCount(),
                row.conflictingRequirementCount(), row.needsVerificationCount(),
                row.topMissingRequirements(), row.explanation()
        );
    }

    private LogicNode deserialize(String json) {
        try {
            return objectMapper.readValue(json, LogicNode.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored pathway composition logic is not valid JSON.", e);
        }
    }
}
