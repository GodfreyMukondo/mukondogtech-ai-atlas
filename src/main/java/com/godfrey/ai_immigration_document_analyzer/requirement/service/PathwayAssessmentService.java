package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayAssessmentResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementEvaluationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessment;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessmentRequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationContext;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationFactView;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.FactEvidenceExpectation;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicEvaluationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.NodeResult;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * PATHWAY ASSESSMENT SERVICE
 * ============================================================================
 *
 * Personalizes one {@code Pathway} against one subject's Facts as of one
 * assessment date (Requirement/Pathway Architecture Specification, sections
 * 7, 8, 9). The Pathway entity itself is never mutated or copied into - this
 * service only reads it and produces a separate, disposable
 * {@code PathwayAssessment}.
 *
 * A Pathway's {@code compositionLogic} is evaluated through the SAME
 * {@code LogicEvaluationService} used for a single Requirement's own logic -
 * composition is just another logic tree, whose leaves happen to be
 * REQUIREMENT_REF nodes. Every referenced Requirement is evaluated through
 * ONE shared {@code RequirementEvaluationService.EvaluationRun}, so a
 * Requirement referenced from two branches of the same Pathway (section 8's
 * reuse guarantee) is computed - and persisted - exactly once.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class PathwayAssessmentService {

    private final PathwayRepository pathwayRepository;
    private final PathwayAssessmentRepository pathwayAssessmentRepository;
    private final PathwayAssessmentRequirementEvaluationRepository pathwayAssessmentEvalRepository;

    private final TemporalFactResolver temporalFactResolver;
    private final LogicEvaluationService logicEvaluationService;
    private final RequirementEvaluationService requirementEvaluationService;
    private final EvaluationCertaintyCalculator certaintyCalculator;
    private final FactAuthorizationService factAuthorizationService;
    private final PathwayOutcomeCalculator pathwayOutcomeCalculator;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // ASSESS (compute a new personalized assessment)
    // =========================================================================

    /** {@code assessmentDate == null} means "now". */
    @Transactional
    public PathwayAssessmentResponse assess(
            AuthenticatedUser actor,
            Long pathwayId,
            Long subjectUserId,
            LocalDateTime assessmentDate
    ) {

        Pathway pathway = pathwayRepository.findById(pathwayId)
                .orElseThrow(() -> new ResourceNotFoundException("Pathway not found."));

        EvaluationFactView factView = temporalFactResolver.resolve(actor, subjectUserId, assessmentDate);
        LocalDateTime resolvedDate = assessmentDate != null ? assessmentDate : LocalDateTime.now();

        RequirementEvaluationService.EvaluationRun run =
                new RequirementEvaluationService.EvaluationRun(actor, subjectUserId, resolvedDate, factView);

        EvaluationContext context = new EvaluationContext(
                subjectUserId,
                resolvedDate,
                factView,
                Map.<String, FactEvidenceExpectation>of(), // evidence bindings are per-Requirement, resolved inside RequirementEvaluationService
                refRequirementId -> requirementEvaluationService.toNodeResult(
                        requirementEvaluationService.evaluateAndPersist(refRequirementId, run)
                )
        );

        LogicNode compositionLogic = deserialize(pathway.getCompositionLogic());
        NodeResult root = logicEvaluationService.evaluate(compositionLogic, context);

        RequirementEvaluationOutcome outcome = pathwayOutcomeCalculator.resolvePathwayOutcome(root, run.computed.values());

        Double confidenceScore = (outcome == RequirementEvaluationOutcome.SATISFIED || outcome == RequirementEvaluationOutcome.NOT_SATISFIED)
                ? certaintyCalculator.forDefiniteOutcome(
                        root.certaintyScore() != null ? root.certaintyScore() : 0.0,
                        RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED // banding only - the per-requirement penalty is already baked into each leaf's own certainty
                ).score()
                : null;

        var confidenceLevel = (confidenceScore != null)
                ? certaintyCalculator.forDefiniteOutcome(confidenceScore, RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED).level()
                : certaintyCalculator.notApplicable().level();

        PathwayAssessment assessment = PathwayAssessment.builder()
                .pathwayId(pathway.getId())
                .subjectUserId(subjectUserId)
                .assessmentDate(resolvedDate)
                .outcome(outcome)
                .assessmentConfidenceScore(confidenceScore)
                .assessmentConfidenceLevel(confidenceLevel)
                .build();

        PathwayAssessment saved = pathwayAssessmentRepository.save(assessment);

        List<RequirementEvaluationResponse> evaluationResponses = run.computed.values().stream()
                .map(evaluation -> {
                    pathwayAssessmentEvalRepository.save(
                            PathwayAssessmentRequirementEvaluation.builder()
                                    .assessmentId(saved.getId())
                                    .evaluationId(evaluation.getId())
                                    .build()
                    );
                    return requirementEvaluationService.toResponse(evaluation);
                })
                .toList();

        return PathwayAssessmentResponse.from(saved, pathway.getPathwayKey(), pathway.getName(), evaluationResponses);
    }

    // =========================================================================
    // RETRIEVAL (already-computed assessment)
    // =========================================================================

    @Transactional(readOnly = true)
    public PathwayAssessmentResponse getAssessment(AuthenticatedUser actor, Long assessmentId) {

        PathwayAssessment assessment = pathwayAssessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pathway assessment not found."));

        factAuthorizationService.assertCanView(actor, assessment.getSubjectUserId(), null, null, "view pathway assessment");

        Pathway pathway = pathwayRepository.findById(assessment.getPathwayId())
                .orElseThrow(() -> new ResourceNotFoundException("Pathway not found."));

        List<RequirementEvaluationResponse> evaluationResponses =
                pathwayAssessmentEvalRepository.findByAssessmentId(assessmentId).stream()
                        .map(link -> requirementEvaluationService.getEvaluationEntityForOwnedAssessment(link.getEvaluationId()))
                        .map(requirementEvaluationService::toResponse)
                        .toList();

        return PathwayAssessmentResponse.from(assessment, pathway.getPathwayKey(), pathway.getName(), evaluationResponses);
    }

    private LogicNode deserialize(String json) {

        try {
            return objectMapper.readValue(json, LogicNode.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored pathway composition logic is not valid JSON.", e);
        }
    }
}
