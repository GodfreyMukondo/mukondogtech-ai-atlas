package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactEvidenceResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessment;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationConflict;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RegulatoryVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationFactRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ============================================================================
 * EXPLAINABILITY SERVICE
 * ============================================================================
 *
 * Answers "why does MukondoGTech AI think I may qualify for this pathway"
 * (Requirement/Pathway Architecture Specification, section 10) by walking
 * the reference graph that already exists - PathwayAssessment ->
 * RequirementEvaluation -> Fact -> FactEvidence -> RegulatoryVersion - and
 * composing it into one read-only response. Nothing here is persisted; the
 * explanation is recomputed from source records every time, exactly like
 * the Digital Twin projection it ultimately reads through.
 *
 * Authorization is checked exactly once, against the owning
 * PathwayAssessment's subject, before any Requirement/Fact detail is read -
 * the same boundary every other Fact/Requirement retrieval in this system
 * uses.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class ExplainabilityService {

    private final PathwayAssessmentRepository pathwayAssessmentRepository;
    private final PathwayRepository pathwayRepository;
    private final PathwayAssessmentRequirementEvaluationRepository assessmentEvalLinkRepository;
    private final RequirementEvaluationRepository requirementEvaluationRepository;
    private final RequirementEvaluationFactRepository evaluationFactRepository;
    private final RequirementEvaluationConflictRepository evaluationConflictRepository;
    private final RequirementRepository requirementRepository;
    private final RegulatoryVersionRepository regulatoryVersionRepository;
    private final FactRepository factRepository;
    private final FactEvidenceRepository factEvidenceRepository;
    private final FactAuthorizationService factAuthorizationService;

    @Transactional(readOnly = true)
    public ExplanationResponse explain(AuthenticatedUser actor, Long pathwayAssessmentId) {

        PathwayAssessment assessment = pathwayAssessmentRepository.findById(pathwayAssessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pathway assessment not found."));

        factAuthorizationService.assertCanView(actor, assessment.getSubjectUserId(), null, null, "explain pathway assessment");

        Pathway pathway = pathwayRepository.findById(assessment.getPathwayId())
                .orElseThrow(() -> new ResourceNotFoundException("Pathway not found."));

        List<ExplanationResponse.RequirementExplanation> requirementExplanations =
                assessmentEvalLinkRepository.findByAssessmentId(pathwayAssessmentId).stream()
                        .map(link -> explainRequirementEvaluation(link.getEvaluationId()))
                        .toList();

        return new ExplanationResponse(
                assessment.getId(),
                pathway.getPathwayKey(),
                pathway.getName(),
                assessment.getOutcome(),
                requirementExplanations
        );
    }

    private ExplanationResponse.RequirementExplanation explainRequirementEvaluation(Long evaluationId) {

        RequirementEvaluation evaluation = requirementEvaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new ResourceNotFoundException("Requirement evaluation not found."));

        Requirement requirement = requirementRepository.findById(evaluation.getRequirementId())
                .orElseThrow(() -> new ResourceNotFoundException("Requirement not found."));

        RegulatoryVersion version = regulatoryVersionRepository.findById(evaluation.getRegulatoryVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Regulatory version not found."));

        List<ExplanationResponse.FactExplanation> facts = evaluationFactRepository.findByEvaluationId(evaluationId).stream()
                .map(link -> explainFact(link.getFactId()))
                .toList();

        List<Long> unresolvedConflictIds = evaluationConflictRepository.findByEvaluationId(evaluationId).stream()
                .map(RequirementEvaluationConflict::getConflictId)
                .toList();

        return new ExplanationResponse.RequirementExplanation(
                requirement.getId(),
                requirement.getRequirementKey(),
                requirement.getTitle(),
                requirement.getRequirementType(),
                requirement.getMandatory(),
                evaluation.getOutcome(),
                evaluation.getExplanation(),
                evaluation.getCertaintyLevel(),
                version.getId(),
                version.getSourceAuthority(),
                version.getSourceReference(),
                version.getVerificationStatus(),
                facts,
                unresolvedConflictIds
        );
    }

    private ExplanationResponse.FactExplanation explainFact(Long factId) {

        Fact fact = factRepository.findById(factId)
                .orElseThrow(() -> new ResourceNotFoundException("Fact not found."));

        List<FactEvidenceResponse> evidence = factEvidenceRepository.findByFactId(factId).stream()
                .map(FactEvidenceResponse::from)
                .toList();

        return new ExplanationResponse.FactExplanation(
                fact.getId(),
                fact.getFactKey(),
                fact.getCategory(),
                valueSummary(fact),
                fact.getProvenanceType(),
                fact.getIsVerified(),
                fact.getConfidenceScore(),
                fact.getConfidenceLevel(),
                evidence
        );
    }

    private String valueSummary(Fact fact) {

        return switch (fact.getValueType()) {
            case STRING -> fact.getStringValue();
            case DATE -> fact.getDateValue() != null ? fact.getDateValue().toLocalDate().toString() : null;
            case NUMBER -> fact.getNumberValue() != null ? String.valueOf(fact.getNumberValue()) : null;
            case BOOLEAN -> fact.getBooleanValue() != null ? String.valueOf(fact.getBooleanValue()) : null;
        };
    }
}
