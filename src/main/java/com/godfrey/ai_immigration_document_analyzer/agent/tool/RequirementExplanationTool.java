package com.godfrey.ai_immigration_document_analyzer.agent.tool;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIntelligenceResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.RequirementEvidenceMatrixRowResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.service.CaseIntelligenceService;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessmentRequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ============================================================================
 * REQUIREMENT EXPLANATION TOOL
 * ============================================================================
 *
 * Wraps {@link CaseIntelligenceService#getCaseIntelligence}, the same
 * already-authorized read projection {@code CaseIntelligenceController}
 * exposes - never a second evaluation, never a new authorization decision.
 * Authorization happens entirely inside that call (via
 * {@code ExplainabilityService} -&gt; {@code FactAuthorizationService},
 * checked once against the PathwayAssessment's own subject before any other
 * read); this tool performs no repository read of its own before that
 * succeeds.
 *
 * The one thing this tool adds beyond a straight pass-through is resolving
 * which {@code RequirementEvaluation} id backs the requested requirement -
 * needed by {@link EvidenceGraphContextTool} - by walking the assessment's
 * already-authorized evaluation links (safe: reached only via foreign keys
 * off a record already confirmed to belong to the authorized subject,
 * exactly like {@code EvidenceGraphService}'s own internal traversals).
 * ============================================================================
 */
@Component
@RequiredArgsConstructor
public class RequirementExplanationTool implements AgentTool<RequirementExplanationTool.Input, RequirementExplanationTool.Output> {

    private final CaseIntelligenceService caseIntelligenceService;
    private final PathwayAssessmentRequirementEvaluationRepository assessmentEvalLinkRepository;
    private final RequirementEvaluationRepository requirementEvaluationRepository;

    @Override
    public AgentToolName name() {
        return AgentToolName.GET_REQUIREMENT_EXPLANATION;
    }

    @Override
    public AgentToolAccessLevel accessLevel() {
        return AgentToolAccessLevel.READ_ONLY;
    }

    @Override
    public Output invoke(AuthenticatedUser actor, Input input) {

        // Authorization happens here, first, before any other read below.
        CaseIntelligenceResponse caseIntelligence =
                caseIntelligenceService.getCaseIntelligence(actor, input.pathwayAssessmentId());

        RequirementEvidenceMatrixRowResponse row = caseIntelligence.evidenceMatrix().stream()
                .filter(candidate -> candidate.requirementId().equals(input.requirementId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "This requirement is not part of the specified pathway assessment."
                ));

        Long evaluationId = resolveEvaluationId(input.pathwayAssessmentId(), input.requirementId());

        return new Output(
                caseIntelligence.pathwayAssessmentId(),
                caseIntelligence.pathwayKey(),
                caseIntelligence.pathwayName(),
                caseIntelligence.subjectUserId(),
                row.requirementId(),
                row.requirementKey(),
                row.requirementTitle(),
                row.requirementType(),
                row.mandatory(),
                evaluationId,
                row.rawOutcome(),
                row.supportStatus(),
                row.certaintyLevel(),
                row.regulatoryVersionId(),
                row.regulatorySourceAuthority(),
                row.regulatoryVerificationStatus(),
                row.supportingFacts(),
                row.missingFactKeys(),
                row.conflictingFactConflictIds(),
                row.explanation(),
                caseIntelligence.recommendedEvidenceGuidance()
        );
    }

    /**
     * Resolves the {@code RequirementEvaluation} id for this requirement
     * within this already-authorized assessment - reached only via the
     * assessment's own evaluation links, never a caller-supplied id.
     */
    private Long resolveEvaluationId(Long pathwayAssessmentId, Long requirementId) {

        List<PathwayAssessmentRequirementEvaluation> links =
                assessmentEvalLinkRepository.findByAssessmentId(pathwayAssessmentId);

        for (PathwayAssessmentRequirementEvaluation link : links) {

            RequirementEvaluation evaluation =
                    requirementEvaluationRepository.findById(link.getEvaluationId()).orElse(null);

            if (evaluation != null && requirementId.equals(evaluation.getRequirementId())) {
                return evaluation.getId();
            }
        }

        return null;
    }

    public record Input(
            Long pathwayAssessmentId,
            Long requirementId
    ) {
    }

    public record Output(
            Long pathwayAssessmentId,
            String pathwayKey,
            String pathwayName,
            Long subjectUserId,
            Long requirementId,
            String requirementKey,
            String requirementTitle,
            RequirementType requirementType,
            Boolean mandatory,
            Long evaluationId,
            RequirementEvaluationOutcome outcome,
            CaseRequirementSupportStatus supportStatus,
            EvaluationCertaintyLevel certaintyLevel,
            Long regulatoryVersionId,
            String regulatorySourceAuthority,
            RegulatoryVerificationStatus regulatoryVerificationStatus,
            List<ExplanationResponse.FactExplanation> contributingFacts,
            List<String> missingFactKeys,
            List<Long> unresolvedConflictIds,
            String existingExplanation,
            String pathwayEvidenceExpectations
    ) {

        /** No fact, no evidence, and no conflict to explain the status from. */
        public boolean isGrounded() {
            return (contributingFacts != null && !contributingFacts.isEmpty())
                    || (unresolvedConflictIds != null && !unresolvedConflictIds.isEmpty());
        }
    }
}
