package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementEvaluationResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatorySourceType;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationConflict;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationFact;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationFactView;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.FactPredicateNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicEvaluationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.PredicateOperator;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.RequirementRefNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RegulatoryVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationFactRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.RequirementFactBindingRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequirementEvaluationService} - the only place a
 * RequirementEvaluation is ever created. Covers Scenarios A/B/C from the
 * approved architecture (satisfied / insufficient evidence / conflicted),
 * the "requirement not published" fallback, circular REQUIREMENT_REF
 * detection, and that authorization is enforced before any evaluation is
 * ever persisted.
 */
@ExtendWith(MockitoExtension.class)
class RequirementEvaluationServiceTest {

    private static final Long SUBJECT_ID = 10L;

    @Mock private RequirementRepository requirementRepository;
    @Mock private RequirementFactBindingRepository factBindingRepository;
    @Mock private RegulatoryVersionRepository regulatoryVersionRepository;
    @Mock private RequirementEvaluationRepository evaluationRepository;
    @Mock private RequirementEvaluationFactRepository evaluationFactRepository;
    @Mock private RequirementEvaluationConflictRepository evaluationConflictRepository;
    @Mock private TemporalFactResolver temporalFactResolver;
    @Mock private FactAuthorizationService factAuthorizationService;

    private RequirementEvaluationService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        service = new RequirementEvaluationService(
                requirementRepository, factBindingRepository, regulatoryVersionRepository,
                evaluationRepository, evaluationFactRepository, evaluationConflictRepository,
                temporalFactResolver, new LogicEvaluationService(), new EvaluationCertaintyCalculator(),
                factAuthorizationService, objectMapper
        );

        org.mockito.Mockito.lenient().when(evaluationRepository.save(any(RequirementEvaluation.class))).thenAnswer(inv -> {
            RequirementEvaluation e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });
        org.mockito.Mockito.lenient().when(evaluationFactRepository.findByEvaluationId(anyLong())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(evaluationConflictRepository.findByEvaluationId(anyLong())).thenReturn(List.of());
    }

    private AuthenticatedUser user(Long id, Role... roles) {
        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority())).toList();
        return new AuthenticatedUser(id, "u" + id + "@example.com", "hash", authorities, true, true, true, true);
    }

    private Requirement requirement(Long id, RequirementStatus status, LogicNode satisfactionLogic) {
        return requirement(id, status, satisfactionLogic, 100L);
    }

    private Requirement requirement(Long id, RequirementStatus status, LogicNode satisfactionLogic, Long regulatoryVersionId) {
        return Requirement.builder()
                .id(id)
                .requirementKey("TEST.REQUIREMENT." + id)
                .requirementType(RequirementType.ELIGIBILITY_ATTRIBUTE)
                .title("Test requirement " + id)
                .jurisdiction("CA")
                .regulatoryVersionId(regulatoryVersionId)
                .mandatory(true)
                .satisfactionLogic(writeJson(satisfactionLogic))
                .status(status)
                .build();
    }

    private RegulatoryVersion regulatoryVersion(Long id, RegulatoryVerificationStatus verificationStatus) {
        return RegulatoryVersion.builder()
                .id(id)
                .regulationIdentity("TEST_REG")
                .jurisdiction("CA")
                .sourceType(RegulatorySourceType.AUTHORITATIVE_REGULATORY_SOURCE)
                .sourceAuthority("IRCC")
                .verificationStatus(verificationStatus)
                .build();
    }

    private FactResponse fact(Long id, String factKey, String stringValue, boolean verified, double confidence) {
        LocalDateTime now = LocalDateTime.now();
        return new FactResponse(
                id, SUBJECT_ID, FactCategory.EDUCATION, factKey, FactValueType.STRING, stringValue, null, null, null,
                FactStatus.ACCEPTED, FactProvenanceType.HUMAN_VERIFICATION, FactSensitivityTier.T2_STANDARD_PERSONAL,
                confidence, FactConfidenceLevel.HIGH, "explanation", verified, verified ? now : null, null,
                null, null, now, now, now, null, List.of()
        );
    }

    private String writeJson(LogicNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================================
    // SCENARIO A - SATISFIED
    // =========================================================================

    @Test
    void scenarioA_satisfiedFromAVerifiedFact() {

        LogicNode logic = new FactPredicateNode("EDUCATION.DEGREE_AWARDED", PredicateOperator.EXISTS, null, null, null, null);
        Requirement requirement = requirement(1L, RequirementStatus.PUBLISHED, logic);

        when(requirementRepository.findById(1L)).thenReturn(Optional.of(requirement));
        when(regulatoryVersionRepository.findById(100L)).thenReturn(Optional.of(regulatoryVersion(100L, RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED)));
        when(factBindingRepository.findByRequirementId(1L)).thenReturn(List.of());

        FactResponse degree = fact(5L, "EDUCATION.DEGREE_AWARDED", "Bachelor of Science", true, 0.9);
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any()))
                .thenReturn(new EvaluationFactView(java.util.Map.of("EDUCATION.DEGREE_AWARDED", List.of(degree)), java.util.Map.of()));

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        RequirementEvaluationResponse response = service.evaluate(actor, 1L, SUBJECT_ID, null);

        assertThat(response.outcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(response.certaintyLevel()).isEqualTo(EvaluationCertaintyLevel.HIGH);

        verify(evaluationFactRepository).save(org.mockito.ArgumentMatchers.argThat(
                (RequirementEvaluationFact f) -> f.getFactId().equals(5L)
        ));
    }

    // =========================================================================
    // SCENARIO B - INSUFFICIENT EVIDENCE, NEVER NOT_SATISFIED
    // =========================================================================

    @Test
    void scenarioB_noLanguageFactYieldsInsufficientEvidenceNotNotSatisfied() {

        LogicNode logic = new FactPredicateNode("LANGUAGE_PROFICIENCY.TEST_SCORE", PredicateOperator.EXISTS, null, null, null, null);
        Requirement requirement = requirement(2L, RequirementStatus.PUBLISHED, logic);

        when(requirementRepository.findById(2L)).thenReturn(Optional.of(requirement));
        when(regulatoryVersionRepository.findById(100L)).thenReturn(Optional.of(regulatoryVersion(100L, RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED)));
        when(factBindingRepository.findByRequirementId(2L)).thenReturn(List.of());
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(EvaluationFactView.empty());

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        RequirementEvaluationResponse response = service.evaluate(actor, 2L, SUBJECT_ID, null);

        assertThat(response.outcome()).isEqualTo(RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE);
        assertThat(response.outcome()).isNotEqualTo(RequirementEvaluationOutcome.NOT_SATISFIED);
        assertThat(response.certaintyLevel()).isEqualTo(EvaluationCertaintyLevel.NOT_APPLICABLE);
    }

    // =========================================================================
    // SCENARIO C - CONFLICTED
    // =========================================================================

    @Test
    void scenarioC_contestedEmploymentFactYieldsConflictedOutcome() {

        LogicNode logic = new FactPredicateNode("EMPLOYMENT.CURRENT_EMPLOYER", PredicateOperator.EXISTS, null, null, null, null);
        Requirement requirement = requirement(3L, RequirementStatus.PUBLISHED, logic);

        when(requirementRepository.findById(3L)).thenReturn(Optional.of(requirement));
        when(regulatoryVersionRepository.findById(100L)).thenReturn(Optional.of(regulatoryVersion(100L, RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED)));
        when(factBindingRepository.findByRequirementId(3L)).thenReturn(List.of());
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any()))
                .thenReturn(new EvaluationFactView(java.util.Map.of(), java.util.Map.of("EMPLOYMENT.CURRENT_EMPLOYER", 77L)));

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        RequirementEvaluationResponse response = service.evaluate(actor, 3L, SUBJECT_ID, null);

        assertThat(response.outcome()).isEqualTo(RequirementEvaluationOutcome.CONFLICTED);
        assertThat(response.unresolvedConflictIds()).isEmpty(); // stubbed join repo returns empty; verifies the save call instead

        verify(evaluationConflictRepository).save(org.mockito.ArgumentMatchers.argThat(
                (RequirementEvaluationConflict c) -> c.getConflictId().equals(77L)
        ));
    }

    // =========================================================================
    // NOT PUBLISHED -> UNKNOWN, LOGIC NEVER EVALUATED
    // =========================================================================

    @Test
    void draftRequirementYieldsUnknownWithoutTouchingFacts() {

        LogicNode logic = new FactPredicateNode("ANY", PredicateOperator.EXISTS, null, null, null, null);
        Requirement requirement = requirement(4L, RequirementStatus.DRAFT, logic);

        when(requirementRepository.findById(4L)).thenReturn(Optional.of(requirement));
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(EvaluationFactView.empty());

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        RequirementEvaluationResponse response = service.evaluate(actor, 4L, SUBJECT_ID, null);

        assertThat(response.outcome()).isEqualTo(RequirementEvaluationOutcome.UNKNOWN);
        verify(regulatoryVersionRepository, never()).findById(any());
    }

    // =========================================================================
    // CIRCULAR REQUIREMENT_REF DETECTION
    // =========================================================================

    @Test
    void circularRequirementReferenceIsDetectedAndRejected() {

        Requirement a = requirement(10L, RequirementStatus.PUBLISHED, new RequirementRefNode(20L));
        Requirement b = requirement(20L, RequirementStatus.PUBLISHED, new RequirementRefNode(10L));

        when(requirementRepository.findById(10L)).thenReturn(Optional.of(a));
        when(requirementRepository.findById(20L)).thenReturn(Optional.of(b));
        when(regulatoryVersionRepository.findById(100L)).thenReturn(Optional.of(regulatoryVersion(100L, RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED)));
        when(factBindingRepository.findByRequirementId(anyLong())).thenReturn(List.of());
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(EvaluationFactView.empty());

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        assertThatThrownBy(() -> service.evaluate(actor, 10L, SUBJECT_ID, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Circular requirement dependency");
    }

    // =========================================================================
    // AUTHORIZATION IS ENFORCED BEFORE ANYTHING IS PERSISTED
    // =========================================================================

    @Test
    void unauthorizedActorNeverProducesAnEvaluationRow() {

        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any()))
                .thenThrow(new AccessDeniedException("denied"));

        AuthenticatedUser stranger = user(999L, Role.USER);

        assertThatThrownBy(() -> service.evaluate(stranger, 1L, SUBJECT_ID, null))
                .isInstanceOf(AccessDeniedException.class);

        verify(evaluationRepository, never()).save(any());
        verify(requirementRepository, never()).findById(any());
    }

    // =========================================================================
    // RETRIEVAL - IDOR PROTECTION
    // =========================================================================

    @Test
    void getEvaluationDeniesAnUnauthorizedActor() {

        RequirementEvaluation stored = RequirementEvaluation.builder()
                .id(1L).requirementId(1L).regulatoryVersionId(100L).subjectUserId(SUBJECT_ID)
                .assessmentDate(LocalDateTime.now()).outcome(RequirementEvaluationOutcome.SATISFIED)
                .certaintyLevel(EvaluationCertaintyLevel.HIGH).build();

        when(evaluationRepository.findById(1L)).thenReturn(Optional.of(stored));

        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(factAuthorizationService).assertCanView(any(), eq(SUBJECT_ID), any(), any(), org.mockito.ArgumentMatchers.anyString());

        AuthenticatedUser stranger = user(999L, Role.USER);

        assertThatThrownBy(() -> service.getEvaluation(stranger, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getEvaluationThrowsResourceNotFoundWhenMissing() {

        when(evaluationRepository.findById(999L)).thenReturn(Optional.empty());

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        assertThatThrownBy(() -> service.getEvaluation(actor, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
