package com.godfrey.ai_immigration_document_analyzer.caseintelligence.service;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIntelligenceResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIssueSeverity;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseOverviewSummaryResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseReadinessResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.EvidenceNecessity;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.MissingEvidenceItemResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.OutstandingIssueResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.RequirementEvidenceMatrixRowResponse;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessment;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementFactBinding;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementFactBindingRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.ExplainabilityService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * CASE INTELLIGENCE SERVICE
 * ============================================================================
 *
 * Answers "how ready is this case for this Pathway, and what's standing in
 * the way" (Master Platform Expansion, Case Intelligence Engine /
 * Requirement-to-Evidence Matrix / Case Readiness / Missing Evidence
 * sections) by reshaping the SAME explainability chain
 * {@link ExplainabilityService#explain} already assembles - it is the only
 * place a PathwayAssessment's RequirementEvaluations are ever read.
 *
 * PATHWAY-AGNOSTIC BY CONSTRUCTION: nothing in this class branches on a
 * pathwayKey, requirementKey, or jurisdiction. Every calculation below
 * operates purely on the generic {@code RequirementEvaluationOutcome}
 * vocabulary, the generic {@code RequirementFactBinding} data, and the
 * generic {@code mandatory} flag - the exact same code path evaluates the
 * seeded Canada/UK pathways and any future pathway an administrator
 * publishes, with zero source changes required.
 *
 * Nothing here re-evaluates a Requirement, re-authorizes the caller (that
 * happens once, inside {@code explain}), or persists anything: readiness,
 * the evidence matrix, and the missing-evidence list are all purely
 * computed, read-time projections, recomputed fresh on every call - exactly
 * like the Digital Twin and Explainability views they sit alongside.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class CaseIntelligenceService {

    private final ExplainabilityService explainabilityService;
    private final RequirementFactBindingRepository requirementFactBindingRepository;
    private final PathwayAssessmentRepository pathwayAssessmentRepository;
    private final PathwayRepository pathwayRepository;
    private final CaseOverviewService caseOverviewService;

    @Transactional(readOnly = true)
    public CaseIntelligenceResponse getCaseIntelligence(AuthenticatedUser actor, Long pathwayAssessmentId) {

        ExplanationResponse explanation = explainabilityService.explain(actor, pathwayAssessmentId);

        List<RequirementEvidenceMatrixRowResponse> matrix = explanation.requirements().stream()
                .map(this::toMatrixRow)
                .toList();

        CaseReadinessResponse readiness = buildReadiness(explanation, matrix);
        List<MissingEvidenceItemResponse> missingEvidence = buildMissingEvidence(explanation, matrix);

        PathwayAssessment assessment = pathwayAssessmentRepository.findById(pathwayAssessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pathway assessment not found."));

        Long subjectUserId = assessment.getSubjectUserId();

        String recommendedEvidenceGuidance = pathwayRepository.findById(assessment.getPathwayId())
                .map(com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway::getEvidenceExpectations)
                .filter(text -> text != null && !text.isBlank())
                .orElse(null);

        CaseOverviewSummaryResponse overview = buildOverview(actor, subjectUserId, readiness, matrix, missingEvidence);

        return new CaseIntelligenceResponse(
                explanation.pathwayAssessmentId(),
                subjectUserId,
                explanation.pathwayKey(),
                explanation.pathwayName(),
                explanation.overallOutcome(),
                overview,
                readiness,
                matrix,
                missingEvidence,
                recommendedEvidenceGuidance,
                LocalDateTime.now()
        );
    }

    // =========================================================================
    // SUPPORT STATUS (generic - see CaseRequirementSupportStatus)
    // =========================================================================

    private CaseRequirementSupportStatus deriveSupportStatus(ExplanationResponse.RequirementExplanation requirement) {

        return switch (requirement.outcome()) {
            case SATISFIED -> CaseRequirementSupportStatus.SATISFIED;
            case PARTIALLY_SATISFIED -> CaseRequirementSupportStatus.PARTIALLY_SUPPORTED;
            case NOT_SATISFIED -> CaseRequirementSupportStatus.NOT_SATISFIED;
            case CONFLICTED -> CaseRequirementSupportStatus.CONFLICTING;
            case EXPIRED, PENDING_REVIEW -> CaseRequirementSupportStatus.NEEDS_VERIFICATION;
            case UNKNOWN, NOT_APPLICABLE -> CaseRequirementSupportStatus.NOT_ASSESSABLE;
            case INSUFFICIENT_EVIDENCE -> requirement.contributingFacts().isEmpty()
                    ? CaseRequirementSupportStatus.MISSING
                    : CaseRequirementSupportStatus.NEEDS_VERIFICATION;
        };
    }

    private RequirementEvidenceMatrixRowResponse toMatrixRow(ExplanationResponse.RequirementExplanation requirement) {

        return new RequirementEvidenceMatrixRowResponse(
                requirement.requirementId(),
                requirement.requirementKey(),
                requirement.requirementTitle(),
                requirement.requirementType(),
                requirement.mandatory(),
                deriveSupportStatus(requirement),
                requirement.outcome(),
                requirement.certaintyLevel(),
                requirement.regulatoryVersionId(),
                requirement.regulatorySourceAuthority(),
                requirement.regulatoryVerificationStatus(),
                requirement.contributingFacts(),
                missingFactKeysFor(requirement),
                requirement.unresolvedConflictIds(),
                requirement.explanation()
        );
    }

    /** Declared bindings for this requirement that do not appear among its own contributing facts. */
    private List<String> missingFactKeysFor(ExplanationResponse.RequirementExplanation requirement) {

        Set<String> contributingFactKeys = requirement.contributingFacts().stream()
                .map(ExplanationResponse.FactExplanation::factKey)
                .collect(Collectors.toSet());

        return requirementFactBindingRepository.findByRequirementId(requirement.requirementId()).stream()
                .map(RequirementFactBinding::getFactKey)
                .filter(factKey -> !contributingFactKeys.contains(factKey))
                .toList();
    }

    // =========================================================================
    // READINESS
    // =========================================================================

    private CaseReadinessResponse buildReadiness(
            ExplanationResponse explanation,
            List<RequirementEvidenceMatrixRowResponse> matrix
    ) {

        List<ExplanationResponse.RequirementExplanation> requirements = explanation.requirements();

        if (requirements.isEmpty()) {
            return new CaseReadinessResponse(100.0, 100.0, 100.0, 100.0, List.of());
        }

        List<ExplanationResponse.RequirementExplanation> mandatory = requirements.stream()
                .filter(r -> Boolean.TRUE.equals(r.mandatory()))
                .toList();

        double requirementCoverage = mandatory.isEmpty()
                ? 100.0
                : percentage(mandatory, r -> r.outcome() == RequirementEvaluationOutcome.SATISFIED);

        double evidenceCoverage =
                percentage(requirements, r -> r.outcome() != RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE);

        double consistency =
                percentage(requirements, r -> r.outcome() != RequirementEvaluationOutcome.CONFLICTED);

        double overall = round((requirementCoverage + evidenceCoverage + consistency) / 3.0);

        List<OutstandingIssueResponse> outstandingIssues = matrix.stream()
                .map(this::toOutstandingIssue)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new CaseReadinessResponse(
                round(requirementCoverage), round(evidenceCoverage), round(consistency), overall, outstandingIssues
        );
    }

    private OutstandingIssueResponse toOutstandingIssue(RequirementEvidenceMatrixRowResponse row) {

        boolean mandatory = Boolean.TRUE.equals(row.mandatory());

        CaseIssueSeverity severity = switch (row.supportStatus()) {
            case NOT_SATISFIED -> mandatory ? CaseIssueSeverity.CRITICAL : CaseIssueSeverity.MEDIUM;
            case MISSING, CONFLICTING -> CaseIssueSeverity.HIGH;
            case NEEDS_VERIFICATION -> mandatory ? CaseIssueSeverity.HIGH : CaseIssueSeverity.MEDIUM;
            case PARTIALLY_SUPPORTED -> CaseIssueSeverity.MEDIUM;
            case SATISFIED, NOT_ASSESSABLE -> null; // nothing outstanding
        };

        if (severity == null) {
            return null;
        }

        return new OutstandingIssueResponse(
                row.requirementId(),
                row.requirementKey(),
                row.requirementTitle(),
                row.mandatory(),
                row.rawOutcome(),
                severity,
                row.explanation()
        );
    }

    private double percentage(
            List<ExplanationResponse.RequirementExplanation> requirements,
            java.util.function.Predicate<ExplanationResponse.RequirementExplanation> predicate
    ) {
        long matching = requirements.stream().filter(predicate).count();
        return (matching * 100.0) / requirements.size();
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    // =========================================================================
    // MISSING EVIDENCE
    // =========================================================================

    private List<MissingEvidenceItemResponse> buildMissingEvidence(
            ExplanationResponse explanation,
            List<RequirementEvidenceMatrixRowResponse> matrix
    ) {

        return matrix.stream()
                .filter(row -> row.supportStatus() == CaseRequirementSupportStatus.MISSING)
                .map(this::toMissingEvidenceItem)
                .toList();
    }

    private MissingEvidenceItemResponse toMissingEvidenceItem(RequirementEvidenceMatrixRowResponse row) {

        boolean mandatory = Boolean.TRUE.equals(row.mandatory());
        EvidenceNecessity necessity = mandatory ? EvidenceNecessity.REQUIRED : EvidenceNecessity.SUPPORTING;

        String reason = "Needed to evaluate \"" + row.requirementTitle() + "\" ("
                + (mandatory ? "a mandatory" : "an optional") + " requirement of this pathway, sourced from "
                + row.regulatorySourceAuthority() + "). " + (row.explanation() != null ? row.explanation() : "");

        return new MissingEvidenceItemResponse(
                row.requirementId(),
                row.requirementKey(),
                row.requirementTitle(),
                row.mandatory(),
                necessity,
                mandatory ? CaseIssueSeverity.CRITICAL : CaseIssueSeverity.MEDIUM,
                row.missingFactKeys(),
                reason.trim()
        );
    }

    // =========================================================================
    // CASE OVERVIEW SUMMARY
    // =========================================================================

    private CaseOverviewSummaryResponse buildOverview(
            AuthenticatedUser actor,
            Long subjectUserId,
            CaseReadinessResponse readiness,
            List<RequirementEvidenceMatrixRowResponse> matrix,
            List<MissingEvidenceItemResponse> missingEvidence
    ) {

        long verificationNeededCount = matrix.stream()
                .filter(row -> row.supportStatus() == CaseRequirementSupportStatus.NEEDS_VERIFICATION)
                .count();

        var contradictions = caseOverviewService.getContradictions(actor, subjectUserId);
        long contradictionCount = contradictions.openContradictions().size() + contradictions.potentialOverlaps().size();

        var timeline = caseOverviewService.getTimeline(actor, subjectUserId);
        long timelineIssueCount = timeline.events().stream()
                .filter(event -> event.gapDaysBeforeThisEntry() != null || event.overlapDaysWithPreviousEntry() != null)
                .count();

        var signals = caseOverviewService.getSignals(actor, subjectUserId);

        return new CaseOverviewSummaryResponse(
                readiness.requirementCoveragePercent(),
                readiness.evidenceCoveragePercent(),
                readiness.overallReadinessPercent(),
                missingEvidence.size(),
                verificationNeededCount,
                contradictionCount,
                timelineIssueCount,
                signals.riskBand(),
                "Every figure above is an internal MukondoGTech AI heuristic computed from your current case "
                        + "information - never a government eligibility score, and never a guarantee of any "
                        + "immigration outcome."
        );
    }
}
