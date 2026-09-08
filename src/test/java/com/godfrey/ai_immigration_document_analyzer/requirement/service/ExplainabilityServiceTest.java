package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.EvidenceSourceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactEvidence;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessment;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessmentRequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatorySourceType;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationFact;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RegulatoryVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationFactRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ExplainabilityService} - Scenario F ("why do you
 * think I qualify"): the full PATHWAY -> REQUIREMENT -> REQUIREMENT
 * EVALUATION -> FACT -> EVIDENCE -> REGULATORY SOURCE chain, and that it is
 * gated by the same authorization boundary as everything else touching a
 * subject's data (Scenario H).
 */
@ExtendWith(MockitoExtension.class)
class ExplainabilityServiceTest {

    private static final Long SUBJECT_ID = 10L;

    @Mock private PathwayAssessmentRepository pathwayAssessmentRepository;
    @Mock private PathwayRepository pathwayRepository;
    @Mock private PathwayAssessmentRequirementEvaluationRepository assessmentEvalLinkRepository;
    @Mock private RequirementEvaluationRepository requirementEvaluationRepository;
    @Mock private RequirementEvaluationFactRepository evaluationFactRepository;
    @Mock private RequirementEvaluationConflictRepository evaluationConflictRepository;
    @Mock private RequirementRepository requirementRepository;
    @Mock private RegulatoryVersionRepository regulatoryVersionRepository;
    @Mock private FactRepository factRepository;
    @Mock private FactEvidenceRepository factEvidenceRepository;
    @Mock private FactAuthorizationService factAuthorizationService;

    private ExplainabilityService explainabilityService;

    @BeforeEach
    void setUp() {
        explainabilityService = new ExplainabilityService(
                pathwayAssessmentRepository, pathwayRepository, assessmentEvalLinkRepository,
                requirementEvaluationRepository, evaluationFactRepository, evaluationConflictRepository,
                requirementRepository, regulatoryVersionRepository, factRepository, factEvidenceRepository,
                factAuthorizationService
        );
    }

    private AuthenticatedUser user(Long id, Role... roles) {
        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority())).toList();
        return new AuthenticatedUser(id, "u" + id + "@example.com", "hash", authorities, true, true, true, true);
    }

    @Test
    void scenarioF_producesTheFullExplanationChainFromPathwayDownToEvidence() {

        PathwayAssessment assessment = PathwayAssessment.builder()
                .id(1L).pathwayId(1L).subjectUserId(SUBJECT_ID).assessmentDate(LocalDateTime.now())
                .outcome(RequirementEvaluationOutcome.SATISFIED).assessmentConfidenceLevel(EvaluationCertaintyLevel.HIGH)
                .build();

        Pathway pathway = Pathway.builder()
                .id(1L).pathwayKey("TEST.PATHWAY").name("Test Pathway").jurisdiction("CA").category("SKILLED_WORKER")
                .compositionLogic("{}").status(PathwayStatus.PUBLISHED).build();

        RequirementEvaluation evaluation = RequirementEvaluation.builder()
                .id(20L).requirementId(2L).regulatoryVersionId(100L).subjectUserId(SUBJECT_ID)
                .assessmentDate(LocalDateTime.now()).outcome(RequirementEvaluationOutcome.SATISFIED)
                .certaintyScore(0.9).certaintyLevel(EvaluationCertaintyLevel.HIGH)
                .explanation("Education requirement - SATISFIED: the available evidence meets this requirement.")
                .build();

        Requirement requirement = Requirement.builder()
                .id(2L).requirementKey("EDUCATION.BACHELORS").requirementType(RequirementType.CREDENTIAL)
                .title("Bachelor's degree or higher").jurisdiction("CA").regulatoryVersionId(100L)
                .mandatory(true).satisfactionLogic("{}").status(RequirementStatus.PUBLISHED).build();

        RegulatoryVersion version = RegulatoryVersion.builder()
                .id(100L).regulationIdentity("EDU_REQ").jurisdiction("CA")
                .sourceType(RegulatorySourceType.AUTHORITATIVE_REGULATORY_SOURCE).sourceAuthority("IRCC")
                .sourceReference("IRPA s.75").verificationStatus(RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED)
                .build();

        Fact degreeFact = Fact.builder()
                .id(30L).subjectUserId(SUBJECT_ID).category(FactCategory.EDUCATION).factKey("EDUCATION.DEGREE_AWARDED")
                .valueType(FactValueType.STRING).stringValue("Bachelor of Science").status(FactStatus.ACCEPTED)
                .provenanceType(FactProvenanceType.HUMAN_VERIFICATION).isVerified(true)
                .confidenceScore(0.9).confidenceLevel(FactConfidenceLevel.HIGH)
                .observedAt(LocalDateTime.now()).lastObservedAt(LocalDateTime.now()).build();

        FactEvidence evidence = FactEvidence.builder()
                .id(40L).factId(30L).sourceType(EvidenceSourceType.DOCUMENT).documentId(50L)
                .sourceLocator("page 1").sourceSnippet("Bachelor of Science, State University")
                .capturedAt(LocalDateTime.now()).build();

        when(pathwayAssessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(pathwayRepository.findById(1L)).thenReturn(Optional.of(pathway));
        when(assessmentEvalLinkRepository.findByAssessmentId(1L)).thenReturn(List.of(
                PathwayAssessmentRequirementEvaluation.builder().id(1L).assessmentId(1L).evaluationId(20L).build()
        ));
        when(requirementEvaluationRepository.findById(20L)).thenReturn(Optional.of(evaluation));
        when(requirementRepository.findById(2L)).thenReturn(Optional.of(requirement));
        when(regulatoryVersionRepository.findById(100L)).thenReturn(Optional.of(version));
        when(evaluationFactRepository.findByEvaluationId(20L)).thenReturn(List.of(
                RequirementEvaluationFact.builder().id(1L).evaluationId(20L).factId(30L).build()
        ));
        when(evaluationConflictRepository.findByEvaluationId(20L)).thenReturn(List.of());
        when(factRepository.findById(30L)).thenReturn(Optional.of(degreeFact));
        when(factEvidenceRepository.findByFactId(30L)).thenReturn(List.of(evidence));

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        ExplanationResponse response = explainabilityService.explain(actor, 1L);

        assertThat(response.pathwayKey()).isEqualTo("TEST.PATHWAY");
        assertThat(response.overallOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(response.requirements()).hasSize(1);

        ExplanationResponse.RequirementExplanation reqExplanation = response.requirements().get(0);
        assertThat(reqExplanation.requirementKey()).isEqualTo("EDUCATION.BACHELORS");
        assertThat(reqExplanation.outcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(reqExplanation.regulatorySourceAuthority()).isEqualTo("IRCC");
        assertThat(reqExplanation.regulatorySourceReference()).isEqualTo("IRPA s.75");
        assertThat(reqExplanation.contributingFacts()).hasSize(1);

        ExplanationResponse.FactExplanation factExplanation = reqExplanation.contributingFacts().get(0);
        assertThat(factExplanation.factId()).isEqualTo(30L);
        assertThat(factExplanation.valueSummary()).isEqualTo("Bachelor of Science");
        assertThat(factExplanation.isVerified()).isTrue();
        assertThat(factExplanation.evidence()).hasSize(1);
        assertThat(factExplanation.evidence().get(0).documentId()).isEqualTo(50L);
        assertThat(factExplanation.evidence().get(0).sourceSnippet()).isEqualTo("Bachelor of Science, State University");
    }

    // =========================================================================
    // SCENARIO H - UNAUTHORIZED ACCESS IS DENIED BEFORE ANY DETAIL IS READ
    // =========================================================================

    @Test
    void unauthorizedCaseWorkerCannotExplainAnotherSubjectsAssessment() {

        PathwayAssessment assessment = PathwayAssessment.builder()
                .id(1L).pathwayId(1L).subjectUserId(SUBJECT_ID).assessmentDate(LocalDateTime.now())
                .outcome(RequirementEvaluationOutcome.SATISFIED).assessmentConfidenceLevel(EvaluationCertaintyLevel.HIGH)
                .build();

        when(pathwayAssessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));

        AuthenticatedUser unassignedCaseWorker = user(200L, Role.CASE_WORKER);

        doThrow(new AccessDeniedException("denied")).when(factAuthorizationService)
                .assertCanView(eq(unassignedCaseWorker), eq(SUBJECT_ID), isNull(), isNull(), anyString());

        assertThatThrownBy(() -> explainabilityService.explain(unassignedCaseWorker, 1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(pathwayRepository, never()).findById(anyLongMatcher());
        verify(assessmentEvalLinkRepository, never()).findByAssessmentId(anyLongMatcher());
    }

    @Test
    void explainThrowsResourceNotFoundWhenAssessmentMissing() {

        when(pathwayAssessmentRepository.findById(999L)).thenReturn(Optional.empty());

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        assertThatThrownBy(() -> explainabilityService.explain(actor, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Long anyLongMatcher() {
        return org.mockito.ArgumentMatchers.anyLong();
    }
}
