package com.godfrey.ai_immigration_document_analyzer.agent.tool;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseIntelligenceResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseOverviewSummaryResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseReadinessResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.RequirementEvidenceMatrixRowResponse;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.service.CaseIntelligenceService;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ExplanationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessmentRequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequirementExplanationTool} - the agent's one
 * case-context-gathering tool. Focus: authorization is delegated entirely to
 * {@code CaseIntelligenceService} before any other read, an unknown
 * requirement never leaks a fabricated row, and evaluation-id resolution
 * only ever walks the already-authorized assessment's own links.
 */
@ExtendWith(MockitoExtension.class)
class RequirementExplanationToolTest {

    private static final Long SUBJECT_ID = 10L;
    private static final Long ASSESSMENT_ID = 500L;
    private static final Long REQUIREMENT_ID = 12L;

    @Mock
    private CaseIntelligenceService caseIntelligenceService;

    @Mock
    private PathwayAssessmentRequirementEvaluationRepository assessmentEvalLinkRepository;

    @Mock
    private RequirementEvaluationRepository requirementEvaluationRepository;

    private RequirementExplanationTool tool;

    @BeforeEach
    void setUp() {
        tool = new RequirementExplanationTool(
                caseIntelligenceService, assessmentEvalLinkRepository, requirementEvaluationRepository
        );
    }

    private AuthenticatedUser user(Long id) {
        return new AuthenticatedUser(
                id, "user" + id + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())), true, true, true, true
        );
    }

    private RequirementEvidenceMatrixRowResponse matrixRow(
            List<ExplanationResponse.FactExplanation> facts, List<Long> conflicts
    ) {
        return new RequirementEvidenceMatrixRowResponse(
                REQUIREMENT_ID, "TEST.REQ", "Test Requirement", RequirementType.EXPERIENCE, true,
                CaseRequirementSupportStatus.SATISFIED, RequirementEvaluationOutcome.SATISFIED,
                EvaluationCertaintyLevel.HIGH, 88L, "Testland Authority",
                com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus.UNVERIFIED_INGESTION,
                facts, List.of(), conflicts, "Some explanation"
        );
    }

    private CaseIntelligenceResponse caseIntelligence(List<RequirementEvidenceMatrixRowResponse> matrix) {
        return new CaseIntelligenceResponse(
                ASSESSMENT_ID, SUBJECT_ID, "TEST_PATHWAY", "Test Pathway",
                RequirementEvaluationOutcome.SATISFIED,
                new CaseOverviewSummaryResponse(100.0, 100.0, 100.0, 0, 0, 0, 0,
                        com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRiskBand.NORMAL, "note"),
                new CaseReadinessResponse(100.0, 100.0, 100.0, 100.0, List.of()),
                matrix, List.of(), "Evidence guidance text", LocalDateTime.now()
        );
    }

    // =========================================================================
    // AUTHORIZATION - DELEGATED, NEVER DUPLICATED
    // =========================================================================

    @Test
    void invokePropagatesDenialBeforeResolvingAnyEvaluationLink() {

        AuthenticatedUser stranger = user(999L);

        when(caseIntelligenceService.getCaseIntelligence(stranger, ASSESSMENT_ID))
                .thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> tool.invoke(stranger, new RequirementExplanationTool.Input(ASSESSMENT_ID, REQUIREMENT_ID)))
                .isInstanceOf(AccessDeniedException.class);

        verify(assessmentEvalLinkRepository, never()).findByAssessmentId(anyLong());
        verify(requirementEvaluationRepository, never()).findById(anyLong());
    }

    @Test
    void invokeThrowsResourceNotFoundWhenRequirementIsNotPartOfTheAssessment() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(caseIntelligenceService.getCaseIntelligence(actor, ASSESSMENT_ID))
                .thenReturn(caseIntelligence(List.of())); // empty matrix - requirement not present

        assertThatThrownBy(() -> tool.invoke(actor, new RequirementExplanationTool.Input(ASSESSMENT_ID, REQUIREMENT_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // EVALUATION ID RESOLUTION
    // =========================================================================

    @Test
    void invokeResolvesTheEvaluationIdBackingThisRequirement() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(caseIntelligenceService.getCaseIntelligence(actor, ASSESSMENT_ID))
                .thenReturn(caseIntelligence(List.of(matrixRow(List.of(), List.of()))));

        PathwayAssessmentRequirementEvaluation link = PathwayAssessmentRequirementEvaluation.builder()
                .id(1L).assessmentId(ASSESSMENT_ID).evaluationId(4001L).build();

        when(assessmentEvalLinkRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of(link));

        RequirementEvaluation evaluation = RequirementEvaluation.builder()
                .id(4001L).requirementId(REQUIREMENT_ID).subjectUserId(SUBJECT_ID).build();

        when(requirementEvaluationRepository.findById(4001L)).thenReturn(Optional.of(evaluation));

        RequirementExplanationTool.Output output =
                tool.invoke(actor, new RequirementExplanationTool.Input(ASSESSMENT_ID, REQUIREMENT_ID));

        assertThat(output.evaluationId()).isEqualTo(4001L);
    }

    @Test
    void invokeReturnsNullEvaluationIdWhenNoLinkMatchesThisRequirement() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(caseIntelligenceService.getCaseIntelligence(actor, ASSESSMENT_ID))
                .thenReturn(caseIntelligence(List.of(matrixRow(List.of(), List.of()))));

        PathwayAssessmentRequirementEvaluation link = PathwayAssessmentRequirementEvaluation.builder()
                .id(1L).assessmentId(ASSESSMENT_ID).evaluationId(4001L).build();

        when(assessmentEvalLinkRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of(link));

        RequirementEvaluation otherEvaluation = RequirementEvaluation.builder()
                .id(4001L).requirementId(999L).subjectUserId(SUBJECT_ID).build();

        when(requirementEvaluationRepository.findById(4001L)).thenReturn(Optional.of(otherEvaluation));

        RequirementExplanationTool.Output output =
                tool.invoke(actor, new RequirementExplanationTool.Input(ASSESSMENT_ID, REQUIREMENT_ID));

        assertThat(output.evaluationId()).isNull();
    }

    // =========================================================================
    // GROUNDING
    // =========================================================================

    @Test
    void isGroundedFalseWhenNoFactsAndNoConflicts() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(caseIntelligenceService.getCaseIntelligence(actor, ASSESSMENT_ID))
                .thenReturn(caseIntelligence(List.of(matrixRow(List.of(), List.of()))));
        when(assessmentEvalLinkRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of());

        RequirementExplanationTool.Output output =
                tool.invoke(actor, new RequirementExplanationTool.Input(ASSESSMENT_ID, REQUIREMENT_ID));

        assertThat(output.isGrounded()).isFalse();
    }

    @Test
    void isGroundedTrueWhenUnresolvedConflictsExistEvenWithNoFacts() {

        AuthenticatedUser actor = user(SUBJECT_ID);

        when(caseIntelligenceService.getCaseIntelligence(actor, ASSESSMENT_ID))
                .thenReturn(caseIntelligence(List.of(matrixRow(List.of(), List.of(70L)))));
        when(assessmentEvalLinkRepository.findByAssessmentId(ASSESSMENT_ID)).thenReturn(List.of());

        RequirementExplanationTool.Output output =
                tool.invoke(actor, new RequirementExplanationTool.Input(ASSESSMENT_ID, REQUIREMENT_ID));

        assertThat(output.isGrounded()).isTrue();
    }

    // =========================================================================
    // TOOL METADATA
    // =========================================================================

    @Test
    void toolIsExplicitlyReadOnly() {
        assertThat(tool.accessLevel()).isEqualTo(AgentToolAccessLevel.READ_ONLY);
    }

    @Test
    void toolHasAStableName() {
        assertThat(tool.name()).isEqualTo(AgentToolName.GET_REQUIREMENT_EXPLANATION);
    }
}
