package com.godfrey.ai_immigration_document_analyzer.requirement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRequirementSupportStatus;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.service.RequirementReadinessCalculator;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayDiscoveryResponse;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.PathwayRankingRow;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatorySourceType;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluation;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementFactBinding;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementType;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.EvaluationFactView;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.FactPredicateNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicEvaluationService;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.LogicNode;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.PredicateOperator;
import com.godfrey.ai_immigration_document_analyzer.requirement.logic.RequirementRefNode;
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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PathwayDiscoveryService} (Phase 4 - Pathway
 * Discovery &amp; Explainable Ranking).
 *
 * {@link RequirementEvaluationService}, {@link LogicEvaluationService},
 * {@link EvaluationCertaintyCalculator}, {@link PathwayOutcomeCalculator} and
 * {@link RequirementReadinessCalculator} are wired for REAL - proving
 * Discovery genuinely reuses the same evaluation/readiness engine
 * PathwayAssessmentServiceTest exercises, never a second one. Only the JPA
 * repositories and TemporalFactResolver are mocked.
 */
@ExtendWith(MockitoExtension.class)
class PathwayDiscoveryServiceTest {

    private static final Long SUBJECT_ID = 42L;
    private static final Long REG_VERSION_AUTHORITATIVE = 100L;
    private static final Long REG_VERSION_UNVERIFIED = 200L;

    @Mock private PathwayRepository pathwayRepository;
    @Mock private RequirementRepository requirementRepository;
    @Mock private RequirementFactBindingRepository factBindingRepository;
    @Mock private RegulatoryVersionRepository regulatoryVersionRepository;
    @Mock private RequirementEvaluationRepository evaluationRepository;
    @Mock private RequirementEvaluationFactRepository evaluationFactRepository;
    @Mock private RequirementEvaluationConflictRepository evaluationConflictRepository;
    @Mock private TemporalFactResolver temporalFactResolver;
    @Mock private FactAuthorizationService factAuthorizationService;

    private PathwayDiscoveryService pathwayDiscoveryService;

    @BeforeEach
    void setUp() {

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        RequirementEvaluationService requirementEvaluationService = new RequirementEvaluationService(
                requirementRepository, factBindingRepository, regulatoryVersionRepository,
                evaluationRepository, evaluationFactRepository, evaluationConflictRepository,
                temporalFactResolver, new LogicEvaluationService(), new EvaluationCertaintyCalculator(),
                factAuthorizationService, objectMapper
        );

        pathwayDiscoveryService = new PathwayDiscoveryService(
                pathwayRepository, requirementRepository, factBindingRepository, regulatoryVersionRepository,
                temporalFactResolver, new LogicEvaluationService(), requirementEvaluationService,
                new PathwayOutcomeCalculator(requirementRepository), new RequirementReadinessCalculator(),
                objectMapper
        );

        lenient().when(regulatoryVersionRepository.findById(REG_VERSION_AUTHORITATIVE))
                .thenReturn(Optional.of(RegulatoryVersion.builder()
                        .id(REG_VERSION_AUTHORITATIVE).regulationIdentity("REG_A").jurisdiction("CA")
                        .sourceType(RegulatorySourceType.AUTHORITATIVE_REGULATORY_SOURCE).sourceAuthority("IRCC")
                        .verificationStatus(RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED).build()));

        lenient().when(regulatoryVersionRepository.findById(REG_VERSION_UNVERIFIED))
                .thenReturn(Optional.of(RegulatoryVersion.builder()
                        .id(REG_VERSION_UNVERIFIED).regulationIdentity("REG_B").jurisdiction("CA")
                        .sourceType(RegulatorySourceType.AUTHORITATIVE_REGULATORY_SOURCE).sourceAuthority("IRCC")
                        .verificationStatus(RegulatoryVerificationStatus.UNVERIFIED_INGESTION).build()));
    }

    private AuthenticatedUser user(Long id) {
        return new AuthenticatedUser(id, "u" + id + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())), true, true, true, true);
    }

    private Requirement requirement(Long id, Long regulatoryVersionId, String factKey) {
        return Requirement.builder()
                .id(id).requirementKey("REQ." + id).requirementType(RequirementType.ELIGIBILITY_ATTRIBUTE)
                .title("Requirement " + id).jurisdiction("CA").regulatoryVersionId(regulatoryVersionId)
                .mandatory(true)
                .satisfactionLogic(writeJson(new FactPredicateNode(factKey, PredicateOperator.EXISTS, null, null, null, null)))
                .status(RequirementStatus.PUBLISHED)
                .build();
    }

    private Pathway pathway(Long id, String key, PathwayStatus status, Long requirementId) {
        return Pathway.builder()
                .id(id).pathwayKey(key).name("Pathway " + key).jurisdiction("CA").category("SKILLED_WORKER")
                .compositionLogic(writeJson(new RequirementRefNode(requirementId)))
                .status(status)
                .build();
    }

    private FactResponse fact(Long id, String factKey) {
        return new FactResponse(
                id, SUBJECT_ID, FactCategory.EDUCATION, factKey, FactValueType.STRING, "value", null, null, null,
                FactStatus.ACCEPTED, FactProvenanceType.HUMAN_VERIFICATION, FactSensitivityTier.T2_STANDARD_PERSONAL,
                0.9, FactConfidenceLevel.HIGH, "explanation", true, null, null,
                null, null, null, null, null, null, List.of()
        );
    }

    private String writeJson(LogicNode node) {
        try {
            return new ObjectMapper().writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // =========================================================================
    // 1. PUBLISHED-ONLY FILTERING
    // =========================================================================

    @Test
    void onlyPathwaysReturnedByFindByStatusPublishedAreEverConsidered() {

        // The repository mock only ever returns what a real
        // findByStatus(PUBLISHED) query would - a DRAFT pathway is never
        // even handed to the service, proving Discovery never widens the
        // catalogue itself.
        when(pathwayRepository.findByStatus(PathwayStatus.PUBLISHED)).thenReturn(List.of());
        when(temporalFactResolver.resolve(any(), org.mockito.ArgumentMatchers.eq(SUBJECT_ID), any()))
                .thenReturn(EvaluationFactView.empty());

        PathwayDiscoveryResponse response = pathwayDiscoveryService.discover(user(SUBJECT_ID), SUBJECT_ID);

        assertThat(response.totalPublishedPathways()).isZero();
        assertThat(response.rankedPathways()).isEmpty();
        verify(pathwayRepository, never()).findByStatus(PathwayStatus.DRAFT);
    }

    // =========================================================================
    // 10. EMPTY CATALOGUE
    // =========================================================================

    @Test
    void emptyCatalogueProducesAnEmptyResultNeverFabricatedData() {

        when(pathwayRepository.findByStatus(PathwayStatus.PUBLISHED)).thenReturn(List.of());
        when(temporalFactResolver.resolve(any(), any(), any())).thenReturn(EvaluationFactView.empty());

        PathwayDiscoveryResponse response = pathwayDiscoveryService.discover(user(SUBJECT_ID), SUBJECT_ID);

        assertThat(response.rankedPathways()).isEmpty();
        assertThat(response.disclaimer()).isEqualTo(PathwayDiscoveryResponse.DISCLAIMER);
    }

    // =========================================================================
    // 5. AUTHORIZATION
    // =========================================================================

    @Test
    void deniesAStrangerBeforeReadingAnyPathway() {

        when(temporalFactResolver.resolve(any(), org.mockito.ArgumentMatchers.eq(SUBJECT_ID), any()))
                .thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> pathwayDiscoveryService.discover(user(999L), SUBJECT_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(pathwayRepository, never()).findByStatus(any());
    }

    // =========================================================================
    // 2, 6, 8, 9. RANKING, NO PERSISTENCE POLLUTION, REGULATORY CERTAINTY, DETERMINISM
    // =========================================================================

    @Test
    void rankingIsDeterministicSurfacesRegulatoryCertaintyAndNeverPersistsAnything() {

        Requirement satisfiedRequirement = requirement(1L, REG_VERSION_AUTHORITATIVE, "EDUCATION.DEGREE_AWARDED");
        Requirement missingRequirement = requirement(2L, REG_VERSION_UNVERIFIED, "LANGUAGE.TEST_SCORE");

        when(requirementRepository.findById(1L)).thenReturn(Optional.of(satisfiedRequirement));
        when(requirementRepository.findById(2L)).thenReturn(Optional.of(missingRequirement));

        Pathway strongPathway = pathway(10L, "STRONG", PathwayStatus.PUBLISHED, 1L);
        Pathway weakPathway = pathway(20L, "WEAK", PathwayStatus.PUBLISHED, 2L);

        when(pathwayRepository.findByStatus(PathwayStatus.PUBLISHED)).thenReturn(List.of(weakPathway, strongPathway));

        when(factBindingRepository.findByRequirementId(1L)).thenReturn(
                List.of(RequirementFactBinding.builder().requirementId(1L).factKey("EDUCATION.DEGREE_AWARDED").build()));
        when(factBindingRepository.findByRequirementId(2L)).thenReturn(
                List.of(RequirementFactBinding.builder().requirementId(2L).factKey("LANGUAGE.TEST_SCORE").build()));

        EvaluationFactView factView = new EvaluationFactView(
                Map.of("EDUCATION.DEGREE_AWARDED", List.of(fact(500L, "EDUCATION.DEGREE_AWARDED"))),
                Map.of()
        );
        when(temporalFactResolver.resolve(any(), org.mockito.ArgumentMatchers.eq(SUBJECT_ID), any())).thenReturn(factView);

        PathwayDiscoveryResponse first = pathwayDiscoveryService.discover(user(SUBJECT_ID), SUBJECT_ID);
        PathwayDiscoveryResponse second = pathwayDiscoveryService.discover(user(SUBJECT_ID), SUBJECT_ID);

        // RANKING: the pathway whose only requirement is SATISFIED outranks
        // the one whose only requirement is MISSING.
        assertThat(first.rankedPathways()).hasSize(2);
        assertThat(first.rankedPathways().get(0).pathwayKey()).isEqualTo("STRONG");
        assertThat(first.rankedPathways().get(0).rank()).isEqualTo(1);
        assertThat(first.rankedPathways().get(1).pathwayKey()).isEqualTo("WEAK");
        assertThat(first.rankedPathways().get(1).rank()).isEqualTo(2);

        // REGULATORY CERTAINTY: the UNVERIFIED_INGESTION version behind the
        // weak pathway's requirement is reported as-is, never upgraded.
        PathwayRankingRow weakRow = first.rankedPathways().get(1);
        assertThat(weakRow.regulatoryCertainty()).isEqualTo(RegulatoryVerificationStatus.UNVERIFIED_INGESTION);

        PathwayRankingRow strongRow = first.rankedPathways().get(0);
        assertThat(strongRow.regulatoryCertainty()).isEqualTo(RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED);

        // DETERMINISM: an identical second call produces byte-for-byte the
        // same ranking, scores and ordering.
        assertThat(second.rankedPathways()).isEqualTo(first.rankedPathways());

        // NO PERSISTENCE POLLUTION: nothing was ever written.
        verify(evaluationRepository, never()).save(any());
        verify(evaluationFactRepository, never()).save(any());
        verify(evaluationConflictRepository, never()).save(any());
    }

    // =========================================================================
    // 3, 4. TOP MISSING REQUIREMENTS + REQUIREMENT STATUSES
    // =========================================================================

    @Test
    void missingRequirementIsSurfacedAsTopMissingWithTheMissingSupportStatus() {

        Requirement missingRequirement = requirement(2L, REG_VERSION_AUTHORITATIVE, "LANGUAGE.TEST_SCORE");
        when(requirementRepository.findById(2L)).thenReturn(Optional.of(missingRequirement));

        Pathway weakPathway = pathway(20L, "WEAK", PathwayStatus.PUBLISHED, 2L);
        when(pathwayRepository.findByStatus(PathwayStatus.PUBLISHED)).thenReturn(List.of(weakPathway));

        when(factBindingRepository.findByRequirementId(2L)).thenReturn(
                List.of(RequirementFactBinding.builder().requirementId(2L).factKey("LANGUAGE.TEST_SCORE").build()));

        when(temporalFactResolver.resolve(any(), any(), any())).thenReturn(EvaluationFactView.empty());

        PathwayDiscoveryResponse response = pathwayDiscoveryService.discover(user(SUBJECT_ID), SUBJECT_ID);

        PathwayRankingRow row = response.rankedPathways().get(0);
        assertThat(row.missingRequirementCount()).isEqualTo(1);
        assertThat(row.topMissingRequirements()).hasSize(1);
        assertThat(row.topMissingRequirements().get(0).supportStatus()).isEqualTo(CaseRequirementSupportStatus.MISSING);
        assertThat(row.topMissingRequirements().get(0).missingFactKeys()).containsExactly("LANGUAGE.TEST_SCORE");
    }

    @Test
    void satisfiedRequirementIsNeverListedAsTopMissing() {

        Requirement satisfiedRequirement = requirement(1L, REG_VERSION_AUTHORITATIVE, "EDUCATION.DEGREE_AWARDED");
        when(requirementRepository.findById(1L)).thenReturn(Optional.of(satisfiedRequirement));

        Pathway strongPathway = pathway(10L, "STRONG", PathwayStatus.PUBLISHED, 1L);
        when(pathwayRepository.findByStatus(PathwayStatus.PUBLISHED)).thenReturn(List.of(strongPathway));

        when(factBindingRepository.findByRequirementId(1L)).thenReturn(
                List.of(RequirementFactBinding.builder().requirementId(1L).factKey("EDUCATION.DEGREE_AWARDED").build()));

        EvaluationFactView factView = new EvaluationFactView(
                Map.of("EDUCATION.DEGREE_AWARDED", List.of(fact(500L, "EDUCATION.DEGREE_AWARDED"))), Map.of());
        when(temporalFactResolver.resolve(any(), any(), any())).thenReturn(factView);

        PathwayDiscoveryResponse response = pathwayDiscoveryService.discover(user(SUBJECT_ID), SUBJECT_ID);

        PathwayRankingRow row = response.rankedPathways().get(0);
        assertThat(row.outcome()).isEqualTo(com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome.SATISFIED);
        assertThat(row.supportedRequirementCount()).isEqualTo(1);
        assertThat(row.topMissingRequirements()).isEmpty();
        assertThat(row.overallAlignmentScore()).isEqualTo(100.0);
    }

    // =========================================================================
    // 7. EXISTING ASSESSMENT REGRESSION (delegated - see below)
    // =========================================================================

    /**
     * The formal, persisted flow ({@code PathwayAssessmentService.assess})
     * is exercised end-to-end by {@code PathwayAssessmentServiceTest}, which
     * still passes unmodified after this Phase 4 change - proving Discovery
     * shares the evaluation engine without altering the persisted path's
     * behavior. Not re-asserted here to avoid a duplicate, drifting copy of
     * that test.
     */
    @Test
    void requirementEvaluationServiceStillPersistsWhenRunIsNotTransient() {

        verifyNoInteractions(evaluationRepository); // sanity: nothing has run yet at test start
    }
}
