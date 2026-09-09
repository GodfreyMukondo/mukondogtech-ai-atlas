package com.godfrey.ai_immigration_document_analyzer.caseintelligence.service;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseContradictionsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIntelligenceResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIssueSeverity;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRiskBand;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseSignalsResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseTimelineResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.EvidenceNecessity;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessment;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementFactBinding;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementFactBindingRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.service.ExplainabilityService;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CaseIntelligenceService} - the pathway-agnostic
 * Requirement-to-Evidence Matrix, case readiness calculation, missing-
 * evidence prioritization, and Case Overview summary (Master Platform
 * Expansion, Phase 2).
 *
 * {@link ExplainabilityService} and {@link CaseOverviewService} are mocked
 * at their public boundary: these tests verify CaseIntelligenceService's
 * own reshaping/classification logic, never re-test explainability
 * composition or case-wide contradiction/timeline/signal computation
 * (covered separately).
 *
 * Nothing here references any specific pathway or requirement key -
 * proving the service is genuinely pathway-agnostic: every test builds its
 * own arbitrary "REQ_n" requirements and the service classifies them using
 * nothing but the generic RequirementEvaluationOutcome/mandatory vocabulary.
 */
@ExtendWith(MockitoExtension.class)
class CaseIntelligenceServiceTest {

    @Mock
    private ExplainabilityService explainabilityService;

    @Mock
    private RequirementFactBindingRepository requirementFactBindingRepository;

    @Mock
    private PathwayAssessmentRepository pathwayAssessmentRepository;

    @Mock
    private PathwayRepository pathwayRepository;

    @Mock
    private CaseOverviewService caseOverviewService;

    private CaseIntelligenceService caseIntelligenceService;

    private final AuthenticatedUser actor = null; // never dereferenced - every collaborator that would use it is mocked

    private static final Long SUBJECT_ID = 900L;

    @BeforeEach
    void setUp() {

        caseIntelligenceService = new CaseIntelligenceService(
                explainabilityService, requirementFactBindingRepository,
                pathwayAssessmentRepository, pathwayRepository, caseOverviewService
        );

        PathwayAssessment assessment = PathwayAssessment.builder().id(1L).pathwayId(1L).subjectUserId(SUBJECT_ID).build();
        lenient().when(pathwayAssessmentRepository.findById(anyLong())).thenReturn(Optional.of(assessment));
        lenient().when(pathwayRepository.findById(anyLong()))
                .thenReturn(Optional.of(Pathway.builder().id(1L).evidenceExpectations(null).build()));

        lenient().when(caseOverviewService.getContradictions(any(), anyLong())).thenReturn(
                new CaseContradictionsResponse(SUBJECT_ID, List.of(), List.of(), LocalDateTime.now(), "note")
        );
        lenient().when(caseOverviewService.getTimeline(any(), anyLong())).thenReturn(
                new CaseTimelineResponse(SUBJECT_ID, List.of(), LocalDateTime.now(), "note")
        );
        lenient().when(caseOverviewService.getSignals(any(), anyLong())).thenReturn(
                new CaseSignalsResponse(SUBJECT_ID, CaseRiskBand.NORMAL, 0, 0, 0, 0, 0, 0, 0, LocalDateTime.now(), "note")
        );
    }

    private ExplanationResponse.RequirementExplanation requirement(
            long id,
            boolean mandatory,
            RequirementEvaluationOutcome outcome,
            List<ExplanationResponse.FactExplanation> facts
    ) {
        return new ExplanationResponse.RequirementExplanation(
                id, "REQ_" + id, "Requirement " + id, RequirementType.ELIGIBILITY_ATTRIBUTE, mandatory,
                outcome, "explanation", EvaluationCertaintyLevel.HIGH, 1L, "Authority", "https://example.gov",
                RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED, facts, List.of()
        );
    }

    private ExplanationResponse.FactExplanation fact(String factKey) {
        return new ExplanationResponse.FactExplanation(
                1L, factKey, com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory.EMPLOYMENT,
                "value", com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType.USER_INPUT,
                true, 0.9, com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel.HIGH, List.of()
        );
    }

    private void stubExplanation(long assessmentId, ExplanationResponse explanation) {
        when(explainabilityService.explain(any(), org.mockito.ArgumentMatchers.eq(assessmentId))).thenReturn(explanation);
    }

    // =========================================================================
    // SUPPORT STATUS - PATHWAY-AGNOSTIC CLASSIFICATION
    // =========================================================================

    @Test
    void readinessIsFullWhenEveryRequirementIsSatisfied() {

        ExplanationResponse explanation = new ExplanationResponse(
                10L, "ANY_PATHWAY_KEY", "Any Pathway", RequirementEvaluationOutcome.SATISFIED,
                List.of(
                        requirement(1L, true, RequirementEvaluationOutcome.SATISFIED, List.of(fact("A"))),
                        requirement(2L, false, RequirementEvaluationOutcome.SATISFIED, List.of(fact("B")))
                )
        );
        stubExplanation(10L, explanation);

        CaseIntelligenceResponse response = caseIntelligenceService.getCaseIntelligence(actor, 10L);

        assertThat(response.readiness().requirementCoveragePercent()).isEqualTo(100.0);
        assertThat(response.readiness().evidenceCoveragePercent()).isEqualTo(100.0);
        assertThat(response.readiness().consistencyPercent()).isEqualTo(100.0);
        assertThat(response.readiness().overallReadinessPercent()).isEqualTo(100.0);
        assertThat(response.readiness().outstandingIssues()).isEmpty();
        assertThat(response.evidenceMatrix()).hasSize(2);
        assertThat(response.evidenceMatrix()).extracting(row -> row.supportStatus())
                .containsOnly(CaseRequirementSupportStatus.SATISFIED);
        assertThat(response.subjectUserId()).isEqualTo(SUBJECT_ID);
        assertThat(response.overview().overallReadinessPercent()).isEqualTo(100.0);
    }

    @Test
    void aMandatoryNotSatisfiedRequirementIsCriticalAndKeptDistinctFromMissingEvidence() {

        ExplanationResponse explanation = new ExplanationResponse(
                11L, "ANY_PATHWAY_KEY", "Any Pathway", RequirementEvaluationOutcome.NOT_SATISFIED,
                List.of(
                        requirement(1L, true, RequirementEvaluationOutcome.NOT_SATISFIED, List.of(fact("A"))),
                        requirement(2L, true, RequirementEvaluationOutcome.SATISFIED, List.of(fact("B")))
                )
        );
        stubExplanation(11L, explanation);

        CaseIntelligenceResponse response = caseIntelligenceService.getCaseIntelligence(actor, 11L);

        assertThat(response.readiness().requirementCoveragePercent()).isEqualTo(50.0);

        var notSatisfiedRow = response.evidenceMatrix().stream()
                .filter(r -> r.requirementId() == 1L).findFirst().orElseThrow();
        assertThat(notSatisfiedRow.supportStatus()).isEqualTo(CaseRequirementSupportStatus.NOT_SATISFIED);

        assertThat(response.readiness().outstandingIssues()).hasSize(1);
        assertThat(response.readiness().outstandingIssues().get(0).severity()).isEqualTo(CaseIssueSeverity.CRITICAL);

        // NOT_SATISFIED (evidence WAS sufficient and definitively failed) must never appear
        // in the missing-evidence list, which is reserved for MISSING (no evidence recorded at all).
        assertThat(response.missingEvidence()).isEmpty();
    }

    @Test
    void insufficientEvidenceWithNoContributingFactsIsClassifiedMissingNeverNotSatisfied() {

        ExplanationResponse explanation = new ExplanationResponse(
                12L, "ANY_PATHWAY_KEY", "Any Pathway", RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE,
                List.of(requirement(1L, true, RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE, List.of()))
        );
        stubExplanation(12L, explanation);

        when(requirementFactBindingRepository.findByRequirementId(1L)).thenReturn(List.of(
                RequirementFactBinding.builder().requirementId(1L).factKey("EMPLOYMENT.CURRENT_EMPLOYER").build()
        ));

        CaseIntelligenceResponse response = caseIntelligenceService.getCaseIntelligence(actor, 12L);

        assertThat(response.evidenceMatrix().get(0).supportStatus()).isEqualTo(CaseRequirementSupportStatus.MISSING);
        assertThat(response.readiness().outstandingIssues().get(0).severity()).isEqualTo(CaseIssueSeverity.HIGH);
        assertThat(response.readiness().evidenceCoveragePercent()).isEqualTo(0.0);

        assertThat(response.missingEvidence()).hasSize(1);
        assertThat(response.missingEvidence().get(0).necessity()).isEqualTo(EvidenceNecessity.REQUIRED);
        assertThat(response.missingEvidence().get(0).missingFactKeys()).containsExactly("EMPLOYMENT.CURRENT_EMPLOYER");
    }

    @Test
    void insufficientEvidenceWithAnExistingButUnqualifyingFactIsClassifiedNeedsVerificationNotMissing() {

        // The fact key already appears among contributingFacts (LogicEvaluationService
        // includes idsOf(facts) even when the fact fails the verification/provenance bar) -
        // this means a Fact WAS recorded, it just does not yet meet the declared quality bar.
        ExplanationResponse explanation = new ExplanationResponse(
                13L, "ANY_PATHWAY_KEY", "Any Pathway", RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE,
                List.of(requirement(1L, true, RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE,
                        List.of(fact("LANGUAGE_PROFICIENCY.TEST_SCORE"))))
        );
        stubExplanation(13L, explanation);

        CaseIntelligenceResponse response = caseIntelligenceService.getCaseIntelligence(actor, 13L);

        assertThat(response.evidenceMatrix().get(0).supportStatus())
                .isEqualTo(CaseRequirementSupportStatus.NEEDS_VERIFICATION);
        assertThat(response.missingEvidence()).isEmpty(); // NEEDS_VERIFICATION is never listed as MISSING
    }

    @Test
    void aConflictedRequirementIsClassifiedConflictingNeverLabeledFraud() {

        ExplanationResponse explanation = new ExplanationResponse(
                14L, "ANY_PATHWAY_KEY", "Any Pathway", RequirementEvaluationOutcome.CONFLICTED,
                List.of(requirement(1L, false, RequirementEvaluationOutcome.CONFLICTED, List.of()))
        );
        stubExplanation(14L, explanation);

        CaseIntelligenceResponse response = caseIntelligenceService.getCaseIntelligence(actor, 14L);

        assertThat(response.evidenceMatrix().get(0).supportStatus()).isEqualTo(CaseRequirementSupportStatus.CONFLICTING);
        assertThat(response.readiness().consistencyPercent()).isEqualTo(0.0);
        assertThat(response.readiness().requirementCoveragePercent()).isEqualTo(100.0); // no mandatory requirements
    }

    @Test
    void expiredAndPendingReviewAreClassifiedNeedsVerification() {

        ExplanationResponse explanation = new ExplanationResponse(
                15L, "ANY_PATHWAY_KEY", "Any Pathway", RequirementEvaluationOutcome.EXPIRED,
                List.of(
                        requirement(1L, true, RequirementEvaluationOutcome.EXPIRED, List.of(fact("A"))),
                        requirement(2L, true, RequirementEvaluationOutcome.PENDING_REVIEW, List.of(fact("B")))
                )
        );
        stubExplanation(15L, explanation);

        CaseIntelligenceResponse response = caseIntelligenceService.getCaseIntelligence(actor, 15L);

        assertThat(response.evidenceMatrix()).extracting(row -> row.supportStatus())
                .containsOnly(CaseRequirementSupportStatus.NEEDS_VERIFICATION);
    }

    @Test
    void notApplicableAndUnknownAreClassifiedNotAssessable() {

        ExplanationResponse explanation = new ExplanationResponse(
                16L, "ANY_PATHWAY_KEY", "Any Pathway", RequirementEvaluationOutcome.NOT_APPLICABLE,
                List.of(
                        requirement(1L, true, RequirementEvaluationOutcome.NOT_APPLICABLE, List.of()),
                        requirement(2L, true, RequirementEvaluationOutcome.UNKNOWN, List.of())
                )
        );
        stubExplanation(16L, explanation);

        CaseIntelligenceResponse response = caseIntelligenceService.getCaseIntelligence(actor, 16L);

        assertThat(response.evidenceMatrix()).extracting(row -> row.supportStatus())
                .containsOnly(CaseRequirementSupportStatus.NOT_ASSESSABLE);
        assertThat(response.readiness().outstandingIssues()).isEmpty();
    }

    @Test
    void partiallySatisfiedIsClassifiedPartiallySupported() {

        ExplanationResponse explanation = new ExplanationResponse(
                17L, "ANY_PATHWAY_KEY", "Any Pathway", RequirementEvaluationOutcome.PARTIALLY_SATISFIED,
                List.of(requirement(1L, true, RequirementEvaluationOutcome.PARTIALLY_SATISFIED, List.of(fact("A"))))
        );
        stubExplanation(17L, explanation);

        CaseIntelligenceResponse response = caseIntelligenceService.getCaseIntelligence(actor, 17L);

        assertThat(response.evidenceMatrix().get(0).supportStatus())
                .isEqualTo(CaseRequirementSupportStatus.PARTIALLY_SUPPORTED);
        assertThat(response.readiness().outstandingIssues().get(0).severity()).isEqualTo(CaseIssueSeverity.MEDIUM);
    }

    @Test
    void emptyRequirementListProducesFullReadinessAndNoMatrixRows() {

        ExplanationResponse explanation = new ExplanationResponse(
                18L, "ANY_PATHWAY_KEY", "Any Pathway", RequirementEvaluationOutcome.NOT_APPLICABLE, List.of()
        );
        stubExplanation(18L, explanation);

        CaseIntelligenceResponse response = caseIntelligenceService.getCaseIntelligence(actor, 18L);

        assertThat(response.readiness().overallReadinessPercent()).isEqualTo(100.0);
        assertThat(response.evidenceMatrix()).isEmpty();
        assertThat(response.missingEvidence()).isEmpty();
    }

    @Test
    void recommendedEvidenceGuidanceSurfacesThePathwaysOwnNonGatingEvidenceExpectations() {

        when(pathwayRepository.findById(anyLong())).thenReturn(Optional.of(
                Pathway.builder().id(1L).evidenceExpectations("Bring a passport-style photo, if available.").build()
        ));

        ExplanationResponse explanation = new ExplanationResponse(
                19L, "ANY_PATHWAY_KEY", "Any Pathway", RequirementEvaluationOutcome.SATISFIED, List.of()
        );
        stubExplanation(19L, explanation);

        CaseIntelligenceResponse response = caseIntelligenceService.getCaseIntelligence(actor, 19L);

        assertThat(response.recommendedEvidenceGuidance()).isEqualTo("Bring a passport-style photo, if available.");
    }
}
