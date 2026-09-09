package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayAssessmentResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayAssessment;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatorySourceType;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.AndNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationFactView;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.FactPredicateNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicEvaluationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.OrNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.PredicateOperator;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.RequirementRefNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayAssessmentRequirementEvaluationRepository;
import com.godfrey.ai_immigration_document_analyzer.requirement.repository.PathwayRepository;
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

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PathwayAssessmentService} - Scenario E from the
 * approved architecture: Bachelor's AND Language AND (Employment OR
 * Alternative qualification). Also verifies that a Requirement referenced
 * from the composition tree is evaluated (and persisted) exactly once per
 * assessment, per the shared-EvaluationRun reuse guarantee.
 */
@ExtendWith(MockitoExtension.class)
class PathwayAssessmentServiceTest {

    private static final Long SUBJECT_ID = 10L;
    private static final Long REG_VERSION_ID = 100L;

    @Mock private PathwayRepository pathwayRepository;
    @Mock private RequirementRepository requirementRepository;
    @Mock private RequirementFactBindingRepository factBindingRepository;
    @Mock private RegulatoryVersionRepository regulatoryVersionRepository;
    @Mock private RequirementEvaluationRepository evaluationRepository;
    @Mock private RequirementEvaluationFactRepository evaluationFactRepository;
    @Mock private RequirementEvaluationConflictRepository evaluationConflictRepository;
    @Mock private PathwayAssessmentRepository pathwayAssessmentRepository;
    @Mock private PathwayAssessmentRequirementEvaluationRepository pathwayAssessmentEvalRepository;
    @Mock private TemporalFactResolver temporalFactResolver;
    @Mock private FactAuthorizationService factAuthorizationService;

    private PathwayAssessmentService pathwayAssessmentService;
    private ObjectMapper objectMapper;
    private final AtomicLong evaluationIdSeq = new AtomicLong(0);

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        RequirementEvaluationService requirementEvaluationService = new RequirementEvaluationService(
                requirementRepository, factBindingRepository, regulatoryVersionRepository,
                evaluationRepository, evaluationFactRepository, evaluationConflictRepository,
                temporalFactResolver, new LogicEvaluationService(), new EvaluationCertaintyCalculator(),
                factAuthorizationService, objectMapper
        );

        pathwayAssessmentService = new PathwayAssessmentService(
                pathwayRepository, pathwayAssessmentRepository, pathwayAssessmentEvalRepository,
                temporalFactResolver, new LogicEvaluationService(), requirementEvaluationService,
                new EvaluationCertaintyCalculator(), factAuthorizationService,
                new PathwayOutcomeCalculator(requirementRepository), objectMapper
        );

        lenient().when(evaluationRepository.save(any(RequirementEvaluation.class))).thenAnswer(inv -> {
            RequirementEvaluation e = inv.getArgument(0);
            e.setId(evaluationIdSeq.incrementAndGet());
            return e;
        });
        lenient().when(evaluationFactRepository.findByEvaluationId(anyLong())).thenReturn(List.of());
        lenient().when(evaluationConflictRepository.findByEvaluationId(anyLong())).thenReturn(List.of());
        lenient().when(pathwayAssessmentRepository.save(any(PathwayAssessment.class))).thenAnswer(inv -> {
            PathwayAssessment a = inv.getArgument(0);
            a.setId(500L);
            return a;
        });
        lenient().when(regulatoryVersionRepository.findById(REG_VERSION_ID))
                .thenReturn(Optional.of(RegulatoryVersion.builder()
                        .id(REG_VERSION_ID).regulationIdentity("TEST").jurisdiction("CA")
                        .sourceType(RegulatorySourceType.AUTHORITATIVE_REGULATORY_SOURCE).sourceAuthority("IRCC")
                        .verificationStatus(RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED).build()));
        lenient().when(factBindingRepository.findByRequirementId(anyLong())).thenReturn(List.of());
    }

    private AuthenticatedUser user(Long id, Role... roles) {
        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority())).toList();
        return new AuthenticatedUser(id, "u" + id + "@example.com", "hash", authorities, true, true, true, true);
    }

    private Requirement requirement(Long id, LogicNode satisfactionLogic) {
        return Requirement.builder()
                .id(id).requirementKey("REQ." + id).requirementType(RequirementType.ELIGIBILITY_ATTRIBUTE)
                .title("Requirement " + id).jurisdiction("CA").regulatoryVersionId(REG_VERSION_ID)
                .mandatory(true).satisfactionLogic(writeJson(satisfactionLogic)).status(RequirementStatus.PUBLISHED)
                .build();
    }

    private FactResponse fact(Long id, String factKey, boolean verified, double confidence) {
        LocalDateTime now = LocalDateTime.now();
        return new FactResponse(
                id, SUBJECT_ID, FactCategory.EDUCATION, factKey, FactValueType.STRING, "value", null, null, null,
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
    // SCENARIO E - BACHELOR'S AND LANGUAGE AND (EMPLOYMENT OR ALT)
    // =========================================================================

    @Test
    void scenarioE_satisfiedViaTheAlternativeQualificationBranch() {

        Requirement bachelors = requirement(1L, new FactPredicateNode("EDUCATION.DEGREE_AWARDED", PredicateOperator.EXISTS, null, null, null, null));
        Requirement language = requirement(2L, new FactPredicateNode("LANGUAGE_PROFICIENCY.TEST_SCORE", PredicateOperator.EXISTS, null, null, null, null));
        Requirement employment = requirement(3L, new FactPredicateNode("EMPLOYMENT.CURRENT_EMPLOYER", PredicateOperator.EXISTS, null, null, null, null));
        Requirement altQualification = requirement(4L, new FactPredicateNode("PROFESSIONAL_CREDENTIALS.LICENSE", PredicateOperator.EXISTS, null, null, null, null));

        when(requirementRepository.findById(1L)).thenReturn(Optional.of(bachelors));
        when(requirementRepository.findById(2L)).thenReturn(Optional.of(language));
        when(requirementRepository.findById(3L)).thenReturn(Optional.of(employment));
        when(requirementRepository.findById(4L)).thenReturn(Optional.of(altQualification));

        Pathway pathway = Pathway.builder()
                .id(1L).pathwayKey("TEST.PATHWAY").name("Test Pathway").jurisdiction("CA").category("SKILLED_WORKER")
                .compositionLogic(writeJson(new AndNode(List.of(
                        new RequirementRefNode(1L),
                        new RequirementRefNode(2L),
                        new OrNode(List.of(new RequirementRefNode(3L), new RequirementRefNode(4L)))
                ))))
                .status(PathwayStatus.PUBLISHED)
                .build();

        when(pathwayRepository.findById(1L)).thenReturn(Optional.of(pathway));

        FactResponse degree = fact(10L, "EDUCATION.DEGREE_AWARDED", true, 0.9);
        FactResponse language1 = fact(11L, "LANGUAGE_PROFICIENCY.TEST_SCORE", true, 0.8);
        FactResponse license = fact(12L, "PROFESSIONAL_CREDENTIALS.LICENSE", true, 0.85);
        // No EMPLOYMENT.CURRENT_EMPLOYER fact - absent, but the OR branch still succeeds via altQualification.

        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(new EvaluationFactView(
                Map.of(
                        "EDUCATION.DEGREE_AWARDED", List.of(degree),
                        "LANGUAGE_PROFICIENCY.TEST_SCORE", List.of(language1),
                        "PROFESSIONAL_CREDENTIALS.LICENSE", List.of(license)
                ),
                Map.of()
        ));

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        PathwayAssessmentResponse response = pathwayAssessmentService.assess(actor, 1L, SUBJECT_ID, null);

        assertThat(response.outcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(response.requirementEvaluations()).hasSize(4); // all four referenced requirements evaluated once each

        long satisfiedCount = response.requirementEvaluations().stream()
                .filter(r -> r.outcome() == RequirementEvaluationOutcome.SATISFIED).count();
        assertThat(satisfiedCount).isEqualTo(3); // bachelors, language, altQualification (employment is INSUFFICIENT_EVIDENCE but irrelevant to the OR's success)
    }

    @Test
    void scenarioE_insufficientEvidenceWhenNeitherOrBranchCanBeConfirmed() {

        Requirement bachelors = requirement(1L, new FactPredicateNode("EDUCATION.DEGREE_AWARDED", PredicateOperator.EXISTS, null, null, null, null));
        Requirement language = requirement(2L, new FactPredicateNode("LANGUAGE_PROFICIENCY.TEST_SCORE", PredicateOperator.EXISTS, null, null, null, null));
        Requirement employment = requirement(3L, new FactPredicateNode("EMPLOYMENT.CURRENT_EMPLOYER", PredicateOperator.EXISTS, null, null, null, null));
        Requirement altQualification = requirement(4L, new FactPredicateNode("PROFESSIONAL_CREDENTIALS.LICENSE", PredicateOperator.EXISTS, null, null, null, null));

        when(requirementRepository.findById(1L)).thenReturn(Optional.of(bachelors));
        when(requirementRepository.findById(2L)).thenReturn(Optional.of(language));
        when(requirementRepository.findById(3L)).thenReturn(Optional.of(employment));
        when(requirementRepository.findById(4L)).thenReturn(Optional.of(altQualification));

        Pathway pathway = Pathway.builder()
                .id(1L).pathwayKey("TEST.PATHWAY").name("Test Pathway").jurisdiction("CA").category("SKILLED_WORKER")
                .compositionLogic(writeJson(new AndNode(List.of(
                        new RequirementRefNode(1L),
                        new RequirementRefNode(2L),
                        new OrNode(List.of(new RequirementRefNode(3L), new RequirementRefNode(4L)))
                ))))
                .status(PathwayStatus.PUBLISHED)
                .build();

        when(pathwayRepository.findById(1L)).thenReturn(Optional.of(pathway));

        FactResponse degree = fact(10L, "EDUCATION.DEGREE_AWARDED", true, 0.9);
        FactResponse language1 = fact(11L, "LANGUAGE_PROFICIENCY.TEST_SCORE", true, 0.8);
        // Neither employment nor altQualification facts exist.

        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(new EvaluationFactView(
                Map.of(
                        "EDUCATION.DEGREE_AWARDED", List.of(degree),
                        "LANGUAGE_PROFICIENCY.TEST_SCORE", List.of(language1)
                ),
                Map.of()
        ));

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        PathwayAssessmentResponse response = pathwayAssessmentService.assess(actor, 1L, SUBJECT_ID, null);

        // The AND as a whole is indeterminate (blocked by the OR branch), but
        // two mandatory requirements (bachelors, language) ARE satisfied -
        // this is exactly the PARTIALLY_SATISFIED presentation case.
        assertThat(response.outcome()).isEqualTo(RequirementEvaluationOutcome.PARTIALLY_SATISFIED);
    }

    // =========================================================================
    // SCENARIO H - IDOR PROTECTION ON ASSESSMENT RETRIEVAL
    // =========================================================================

    @Test
    void getAssessmentDeniesAnUnauthorizedActor() {

        PathwayAssessment stored = PathwayAssessment.builder()
                .id(1L).pathwayId(1L).subjectUserId(SUBJECT_ID).assessmentDate(LocalDateTime.now())
                .outcome(RequirementEvaluationOutcome.SATISFIED)
                .assessmentConfidenceLevel(com.godfrey.ai_immigration_document_analyzer.requirement.entity.EvaluationCertaintyLevel.HIGH)
                .build();

        when(pathwayAssessmentRepository.findById(1L)).thenReturn(Optional.of(stored));

        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException("denied"))
                .when(factAuthorizationService).assertCanView(
                        org.mockito.ArgumentMatchers.any(), eq(SUBJECT_ID),
                        org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.anyString()
                );

        AuthenticatedUser stranger = user(999L, Role.USER);

        assertThat(
                org.junit.jupiter.api.Assertions.assertThrows(
                        org.springframework.security.access.AccessDeniedException.class,
                        () -> pathwayAssessmentService.getAssessment(stranger, 1L)
                )
        ).isNotNull();

        org.mockito.Mockito.verify(pathwayRepository, org.mockito.Mockito.never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void getAssessmentThrowsResourceNotFoundWhenMissing() {

        when(pathwayAssessmentRepository.findById(999L)).thenReturn(Optional.empty());

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException.class,
                () -> pathwayAssessmentService.getAssessment(actor, 999L)
        );
    }
}
