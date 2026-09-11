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
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.HypotheticalFactInput;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.RequirementDeltaRow;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ScenarioSimulationRequest;
import com.godfrey.ai_immigration_document_analyzer.requirement.dto.ScenarioSimulationResult;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Pathway;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.PathwayStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatorySourceType;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.Requirement;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RequirementEvaluationOutcome;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ScenarioSimulationService} (Phase 5.5 - What-If /
 * Scenario Simulation).
 *
 * {@link RequirementEvaluationService}, {@link LogicEvaluationService}, and
 * {@link PathwayOutcomeCalculator} are wired for REAL - identical to {@code
 * PathwayDiscoveryServiceTest}'s own approach - proving simulation genuinely
 * reuses the same evaluation engine, never a second one. Only the JPA
 * repositories and {@link TemporalFactResolver} are mocked. {@link
 * TemporalFactResolver#overlayHypothetical} is also wired for REAL (not
 * mocked) since it is pure, in-memory, and the exact mechanism under test.
 */
@ExtendWith(MockitoExtension.class)
class ScenarioSimulationServiceTest {

    private static final Long SUBJECT_ID = 42L;
    private static final Long PATHWAY_ID = 10L;
    private static final Long REG_VERSION_ID = 100L;

    @Mock private PathwayRepository pathwayRepository;
    @Mock private RequirementRepository requirementRepository;
    @Mock private RequirementFactBindingRepository factBindingRepository;
    @Mock private RegulatoryVersionRepository regulatoryVersionRepository;
    @Mock private RequirementEvaluationRepository evaluationRepository;
    @Mock private RequirementEvaluationFactRepository evaluationFactRepository;
    @Mock private RequirementEvaluationConflictRepository evaluationConflictRepository;
    @Mock private TemporalFactResolver temporalFactResolver;
    @Mock private FactAuthorizationService factAuthorizationService;

    private TemporalFactResolver realOverlayResolver;
    private ScenarioSimulationService service;

    @BeforeEach
    void setUp() {

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        RequirementEvaluationService requirementEvaluationService = new RequirementEvaluationService(
                requirementRepository, factBindingRepository, regulatoryVersionRepository,
                evaluationRepository, evaluationFactRepository, evaluationConflictRepository,
                temporalFactResolver, new LogicEvaluationService(), new EvaluationCertaintyCalculator(),
                factAuthorizationService, objectMapper
        );

        // overlayHypothetical is pure/in-memory - wired for real via a
        // throwaway instance (its other dependencies are never exercised by
        // that one method) so the test exercises the ACTUAL overlay logic.
        realOverlayResolver = new TemporalFactResolver(null, null, null, null);

        service = new ScenarioSimulationService(
                pathwayRepository, requirementRepository, temporalFactResolver,
                new LogicEvaluationService(), requirementEvaluationService,
                new PathwayOutcomeCalculator(requirementRepository), objectMapper
        );

        lenient().when(regulatoryVersionRepository.findById(REG_VERSION_ID))
                .thenReturn(Optional.of(RegulatoryVersion.builder()
                        .id(REG_VERSION_ID).regulationIdentity("REG").jurisdiction("CA")
                        .sourceType(RegulatorySourceType.AUTHORITATIVE_REGULATORY_SOURCE).sourceAuthority("IRCC")
                        .verificationStatus(RegulatoryVerificationStatus.AUTHORITATIVE_CONFIRMED).build()));
    }

    private AuthenticatedUser user(Long id) {
        return new AuthenticatedUser(id, "u" + id + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority(Role.USER.getAuthority())), true, true, true, true);
    }

    private Requirement requirement(Long id, LogicNode satisfactionLogic) {
        return Requirement.builder()
                .id(id).requirementKey("REQ." + id).requirementType(RequirementType.ELIGIBILITY_ATTRIBUTE)
                .title("Requirement " + id).jurisdiction("CA").regulatoryVersionId(REG_VERSION_ID)
                .mandatory(true)
                .satisfactionLogic(writeJson(satisfactionLogic))
                .status(RequirementStatus.PUBLISHED)
                .build();
    }

    private Pathway pathway(Long requirementId) {
        return Pathway.builder()
                .id(PATHWAY_ID).pathwayKey("TEST_PATHWAY").name("Test Pathway").jurisdiction("CA").category("SKILLED_WORKER")
                .compositionLogic(writeJson(new RequirementRefNode(requirementId)))
                .status(PathwayStatus.PUBLISHED)
                .build();
    }

    private FactResponse realFact(String factKey, FactCategory category, String stringValue, Double numberValue) {
        return new FactResponse(
                500L, SUBJECT_ID, category, factKey, numberValue != null ? FactValueType.NUMBER : FactValueType.STRING,
                stringValue, null, numberValue, null,
                FactStatus.ACCEPTED, FactProvenanceType.USER_INPUT, FactSensitivityTier.T2_STANDARD_PERSONAL,
                0.9, FactConfidenceLevel.HIGH, "explanation", false, null, null,
                null, null, null, null, null, null, List.of()
        );
    }

    private ScenarioSimulationRequest request(HypotheticalFactInput... overrides) {
        ScenarioSimulationRequest request = new ScenarioSimulationRequest();
        request.setSubjectUserId(SUBJECT_ID);
        request.setHypotheticalFacts(List.of(overrides));
        return request;
    }

    private HypotheticalFactInput override(String factKey, String value) {
        HypotheticalFactInput input = new HypotheticalFactInput();
        input.setFactKey(factKey);
        input.setValue(value);
        return input;
    }

    private String writeJson(LogicNode node) {
        try {
            return new ObjectMapper().writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void stubPathwayAndRequirement(Requirement req) {
        when(pathwayRepository.findById(PATHWAY_ID)).thenReturn(Optional.of(pathway(req.getId())));
        when(requirementRepository.findById(req.getId())).thenReturn(Optional.of(req));
        when(factBindingRepository.findByRequirementId(req.getId())).thenReturn(List.of());
    }

    // =========================================================================
    // 1. NO HYPOTHETICAL OVERRIDES - BEFORE == AFTER, nothing changed
    // =========================================================================

    @Test
    void noHypotheticalOverridesProducesAnIdenticalBeforeAndAfter() {

        Requirement req = requirement(1L, new FactPredicateNode("EDUCATION.DEGREE_AWARDED", PredicateOperator.EXISTS, null, null, null, null));
        stubPathwayAndRequirement(req);

        EvaluationFactView real = new EvaluationFactView(
                Map.of("EDUCATION.DEGREE_AWARDED", List.of(realFact("EDUCATION.DEGREE_AWARDED", FactCategory.EDUCATION, "MSc", null))),
                Map.of()
        );
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(real);
        when(temporalFactResolver.overlayHypothetical(any(), any())).thenAnswer(invocation ->
                realOverlayResolver.overlayHypothetical(invocation.getArgument(0), invocation.getArgument(1)));

        ScenarioSimulationRequest emptyOverrides = new ScenarioSimulationRequest();
        emptyOverrides.setSubjectUserId(SUBJECT_ID);
        emptyOverrides.setHypotheticalFacts(List.of());

        ScenarioSimulationResult result = service.simulate(user(SUBJECT_ID), PATHWAY_ID, emptyOverrides);

        assertThat(result.beforePathwayOutcome()).isEqualTo(result.afterPathwayOutcome());
        assertThat(result.pathwayOutcomeChanged()).isFalse();
        assertThat(result.hypotheticalInput()).isEmpty();
        assertThat(result.requirementDeltas()).allMatch(row -> !row.changed());
    }

    // =========================================================================
    // 2/5. ONE HYPOTHETICAL OVERRIDE - requirement becomes SATISFIED
    // =========================================================================

    @Test
    void oneHypotheticalOverrideCanFlipARequirementFromNotSatisfiedToSatisfied() {

        Requirement req = requirement(1L, new FactPredicateNode("EMPLOYMENT.SALARY", PredicateOperator.AT_LEAST, "50000", null, null, null));
        stubPathwayAndRequirement(req);

        EvaluationFactView real = new EvaluationFactView(
                Map.of("EMPLOYMENT.SALARY", List.of(realFact("EMPLOYMENT.SALARY", FactCategory.EMPLOYMENT, null, 20000.0))),
                Map.of()
        );
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(real);
        when(temporalFactResolver.overlayHypothetical(any(), any())).thenAnswer(invocation ->
                realOverlayResolver.overlayHypothetical(invocation.getArgument(0), invocation.getArgument(1)));

        ScenarioSimulationResult result = service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(override("EMPLOYMENT.SALARY", "60000")));

        RequirementDeltaRow row = result.requirementDeltas().get(0);
        assertThat(row.beforeOutcome()).isEqualTo(RequirementEvaluationOutcome.NOT_SATISFIED);
        assertThat(row.afterOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(row.changed()).isTrue();
        assertThat(result.pathwayOutcomeChanged()).isTrue();
        assertThat(result.hypotheticalInput()).hasSize(1);
        assertThat(result.hypotheticalInput().get(0).provenance()).isEqualTo(FactProvenanceType.SIMULATION);
    }

    // =========================================================================
    // 6. A HYPOTHETICAL OVERRIDE CAN ALSO MAKE A REQUIREMENT WORSE
    // =========================================================================

    @Test
    void oneHypotheticalOverrideCanFlipARequirementFromSatisfiedToNotSatisfied() {

        Requirement req = requirement(1L, new FactPredicateNode("EMPLOYMENT.SALARY", PredicateOperator.AT_LEAST, "50000", null, null, null));
        stubPathwayAndRequirement(req);

        EvaluationFactView real = new EvaluationFactView(
                Map.of("EMPLOYMENT.SALARY", List.of(realFact("EMPLOYMENT.SALARY", FactCategory.EMPLOYMENT, null, 80000.0))),
                Map.of()
        );
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(real);
        when(temporalFactResolver.overlayHypothetical(any(), any())).thenAnswer(invocation ->
                realOverlayResolver.overlayHypothetical(invocation.getArgument(0), invocation.getArgument(1)));

        ScenarioSimulationResult result = service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(override("EMPLOYMENT.SALARY", "10000")));

        RequirementDeltaRow row = result.requirementDeltas().get(0);
        assertThat(row.beforeOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(row.afterOutcome()).isEqualTo(RequirementEvaluationOutcome.NOT_SATISFIED);
        assertThat(row.changed()).isTrue();
    }

    // =========================================================================
    // 3. MULTIPLE HYPOTHETICAL OVERRIDES - the AFTER state is the COMBINATION
    // =========================================================================

    @Test
    void multipleHypotheticalOverridesProduceOneCombinedAfterState() {

        Requirement req = requirement(1L, new FactPredicateNode("EMPLOYMENT.SALARY", PredicateOperator.AT_LEAST, "50000", null, null, null));
        stubPathwayAndRequirement(req);

        EvaluationFactView real = new EvaluationFactView(Map.of(), Map.of());
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(real);
        when(temporalFactResolver.overlayHypothetical(any(), any())).thenAnswer(invocation ->
                realOverlayResolver.overlayHypothetical(invocation.getArgument(0), invocation.getArgument(1)));

        ScenarioSimulationResult result = service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(
                override("EMPLOYMENT.SALARY", "60000"),
                override("EDUCATION.DEGREE_AWARDED", "MSc")
        ));

        assertThat(result.hypotheticalInput()).hasSize(2);
        assertThat(result.requirementDeltas().get(0).afterOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
    }

    // =========================================================================
    // 4. UNCHANGED REQUIREMENT - override targets an unrelated factKey
    // =========================================================================

    @Test
    void anOverrideForAnUnrelatedFactKeyLeavesTheRequirementUnchanged() {

        Requirement req = requirement(1L, new FactPredicateNode("EDUCATION.DEGREE_AWARDED", PredicateOperator.EXISTS, null, null, null, null));
        stubPathwayAndRequirement(req);

        EvaluationFactView real = new EvaluationFactView(
                Map.of("EDUCATION.DEGREE_AWARDED", List.of(realFact("EDUCATION.DEGREE_AWARDED", FactCategory.EDUCATION, "MSc", null))),
                Map.of()
        );
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(real);
        when(temporalFactResolver.overlayHypothetical(any(), any())).thenAnswer(invocation ->
                realOverlayResolver.overlayHypothetical(invocation.getArgument(0), invocation.getArgument(1)));

        ScenarioSimulationResult result = service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(override("EMPLOYMENT.SALARY", "60000")));

        RequirementDeltaRow row = result.requirementDeltas().get(0);
        assertThat(row.beforeOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(row.afterOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(row.changed()).isFalse();
    }

    // =========================================================================
    // 7. MISSING EVIDENCE - no fact at all for the required key, either side
    // =========================================================================

    @Test
    void missingEvidenceProducesInsufficientEvidenceOnBothSidesWhenUnrelated() {

        Requirement req = requirement(1L, new FactPredicateNode("EDUCATION.DEGREE_AWARDED", PredicateOperator.EXISTS, null, null, null, null));
        stubPathwayAndRequirement(req);

        EvaluationFactView real = new EvaluationFactView(Map.of(), Map.of());
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(real);
        when(temporalFactResolver.overlayHypothetical(any(), any())).thenAnswer(invocation ->
                realOverlayResolver.overlayHypothetical(invocation.getArgument(0), invocation.getArgument(1)));

        ScenarioSimulationResult result = service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(override("EMPLOYMENT.SALARY", "60000")));

        RequirementDeltaRow row = result.requirementDeltas().get(0);
        assertThat(row.beforeOutcome()).isEqualTo(RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE);
        assertThat(row.afterOutcome()).isEqualTo(RequirementEvaluationOutcome.INSUFFICIENT_EVIDENCE);
        assertThat(row.changed()).isFalse();
    }

    // =========================================================================
    // 8. CONFLICTING EVIDENCE - a hypothetical override for a CONFLICTED key
    // supersedes the real conflict FOR SIMULATION PURPOSES ONLY
    // =========================================================================

    @Test
    void aHypotheticalOverrideForAConflictedFactKeySupersedesTheConflictInTheSimulationOnly() {

        Requirement req = requirement(1L, new FactPredicateNode("EDUCATION.DEGREE_AWARDED", PredicateOperator.EXISTS, null, null, null, null));
        stubPathwayAndRequirement(req);

        EvaluationFactView real = new EvaluationFactView(Map.of(), Map.of("EDUCATION.DEGREE_AWARDED", 999L));
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(real);
        when(temporalFactResolver.overlayHypothetical(any(), any())).thenAnswer(invocation ->
                realOverlayResolver.overlayHypothetical(invocation.getArgument(0), invocation.getArgument(1)));

        ScenarioSimulationResult result = service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(override("EDUCATION.DEGREE_AWARDED", "MSc")));

        RequirementDeltaRow row = result.requirementDeltas().get(0);
        assertThat(row.beforeOutcome()).isEqualTo(RequirementEvaluationOutcome.CONFLICTED);
        assertThat(row.afterOutcome()).isEqualTo(RequirementEvaluationOutcome.SATISFIED);
        assertThat(row.changed()).isTrue();

        // The REAL conflict map handed to the resolver is never mutated -
        // overlayHypothetical returns a distinct object (see next test too).
    }

    // =========================================================================
    // 9. A HISTORICAL_MULTI_VALUED KEY'S REAL FACTS ARE FULLY REPLACED, NOT
    // APPENDED TO, BY THE SINGLE HYPOTHETICAL VALUE
    // =========================================================================

    @Test
    void overlayReplacesMultipleRealFactsForOneKeyWithExactlyOneHypotheticalValue() {

        EvaluationFactView real = new EvaluationFactView(
                Map.of("EMPLOYMENT.SALARY", List.of(
                        realFact("EMPLOYMENT.SALARY", FactCategory.EMPLOYMENT, null, 10000.0),
                        realFact("EMPLOYMENT.SALARY", FactCategory.EMPLOYMENT, null, 20000.0)
                )),
                Map.of()
        );

        FactResponse hypothetical = new FactResponse(
                null, SUBJECT_ID, FactCategory.EMPLOYMENT, "EMPLOYMENT.SALARY", FactValueType.NUMBER,
                null, null, 99999.0, null, FactStatus.ACCEPTED, FactProvenanceType.SIMULATION,
                FactSensitivityTier.T3_SENSITIVE, null, FactConfidenceLevel.NOT_APPLICABLE, "hypothetical",
                false, null, null, null, null, null, null, null, null, List.of()
        );

        EvaluationFactView overlaid = realOverlayResolver.overlayHypothetical(real, List.of(hypothetical));

        assertThat(overlaid.factsFor("EMPLOYMENT.SALARY")).hasSize(1);
        assertThat(overlaid.factsFor("EMPLOYMENT.SALARY").get(0).numberValue()).isEqualTo(99999.0);
        // The REAL view passed in is untouched (records are immutable, and
        // overlayHypothetical never mutates the maps it was given).
        assertThat(real.factsFor("EMPLOYMENT.SALARY")).hasSize(2);
    }

    // =========================================================================
    // 11. HUMAN-REVIEW STATE - PENDING_REVIEW flows through as-is, never
    // invented or suppressed
    // =========================================================================

    @Test
    void pendingReviewOutcomeIsReportedAsIsNeverSuppressedOrReinterpreted() {

        // A requirement whose applicability itself is INDETERMINATE (no
        // applicability logic set, satisfaction depends on a conflicted
        // key) naturally surfaces CONFLICTED/PENDING-style outcomes -
        // reusing the existing outcome vocabulary rather than inventing a
        // dedicated PENDING_REVIEW test fixture at the interpreter level,
        // which is already covered by LogicEvaluationServiceTest.
        Requirement req = requirement(1L, new FactPredicateNode("EDUCATION.DEGREE_AWARDED", PredicateOperator.EXISTS, null, null, null, null));
        stubPathwayAndRequirement(req);

        EvaluationFactView real = new EvaluationFactView(Map.of(), Map.of("EDUCATION.DEGREE_AWARDED", 999L));
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(real);
        when(temporalFactResolver.overlayHypothetical(any(), any())).thenAnswer(invocation ->
                realOverlayResolver.overlayHypothetical(invocation.getArgument(0), invocation.getArgument(1)));

        // Override an UNRELATED key - the real conflict on
        // EDUCATION.DEGREE_AWARDED remains untouched in the AFTER view too.
        ScenarioSimulationResult result = service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(override("EMPLOYMENT.SALARY", "1")));

        RequirementDeltaRow row = result.requirementDeltas().get(0);
        assertThat(row.beforeOutcome()).isEqualTo(RequirementEvaluationOutcome.CONFLICTED);
        assertThat(row.afterOutcome()).isEqualTo(RequirementEvaluationOutcome.CONFLICTED);
        assertThat(row.changed()).isFalse();
    }

    // =========================================================================
    // 12/13. PATHWAY-LEVEL OUTCOME CHANGE / NO CHANGE
    // =========================================================================

    @Test
    void pathwayOutcomeUnchangedWhenNoRequirementChanges() {

        Requirement req = requirement(1L, new FactPredicateNode("EDUCATION.DEGREE_AWARDED", PredicateOperator.EXISTS, null, null, null, null));
        stubPathwayAndRequirement(req);

        EvaluationFactView real = new EvaluationFactView(
                Map.of("EDUCATION.DEGREE_AWARDED", List.of(realFact("EDUCATION.DEGREE_AWARDED", FactCategory.EDUCATION, "MSc", null))),
                Map.of()
        );
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(real);
        when(temporalFactResolver.overlayHypothetical(any(), any())).thenAnswer(invocation ->
                realOverlayResolver.overlayHypothetical(invocation.getArgument(0), invocation.getArgument(1)));

        ScenarioSimulationResult result = service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(override("EMPLOYMENT.SALARY", "1")));

        assertThat(result.pathwayOutcomeChanged()).isFalse();
        assertThat(result.beforePathwayOutcome()).isEqualTo(result.afterPathwayOutcome());
    }

    // =========================================================================
    // 14. INVALID HYPOTHETICAL FACT - unknown factKey
    // =========================================================================

    @Test
    void unknownFactKeyIsRejectedBeforeAnyEvaluationRuns() {

        when(pathwayRepository.findById(PATHWAY_ID)).thenReturn(Optional.of(pathway(1L)));
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(EvaluationFactView.empty());

        assertThatThrownBy(() -> service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(override("NOT.A.REAL.FACT.KEY", "x"))))
                .isInstanceOf(IllegalArgumentException.class);

        verify(evaluationRepository, never()).save(any());
    }

    // =========================================================================
    // 15. INVALID HYPOTHETICAL VALUE - wrong type for the registered key
    // =========================================================================

    @Test
    void nonNumericValueForANumericFactKeyIsRejected() {

        when(pathwayRepository.findById(PATHWAY_ID)).thenReturn(Optional.of(pathway(1L)));
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(EvaluationFactView.empty());

        assertThatThrownBy(() -> service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(override("EMPLOYMENT.SALARY", "not-a-number"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateFactKeyInOneRequestIsRejected() {

        when(pathwayRepository.findById(PATHWAY_ID)).thenReturn(Optional.of(pathway(1L)));
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(EvaluationFactView.empty());

        assertThatThrownBy(() -> service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(
                override("EMPLOYMENT.SALARY", "1"), override("employment.salary", "2")
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // 16. UNAUTHORIZED SUBJECT - propagated before any pathway evaluation
    // =========================================================================

    @Test
    void deniesAStrangerBeforeEvaluatingAnything() {

        when(pathwayRepository.findById(PATHWAY_ID)).thenReturn(Optional.of(pathway(1L)));
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenThrow(new AccessDeniedException("denied"));

        assertThatThrownBy(() -> service.simulate(user(999L), PATHWAY_ID, request(override("EMPLOYMENT.SALARY", "1"))))
                .isInstanceOf(AccessDeniedException.class);

        verify(evaluationRepository, never()).save(any());
    }

    @Test
    void nonPublishedPathwayIsTreatedAsNotFound() {

        Pathway draft = Pathway.builder()
                .id(PATHWAY_ID).pathwayKey("DRAFT").name("Draft").jurisdiction("CA").category("X")
                .compositionLogic(writeJson(new RequirementRefNode(1L)))
                .status(PathwayStatus.DRAFT)
                .build();

        when(pathwayRepository.findById(PATHWAY_ID)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(override("EMPLOYMENT.SALARY", "1"))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(temporalFactResolver, never()).resolve(any(), any(), any());
    }

    // =========================================================================
    // 17. SIMULATION PROVENANCE
    // =========================================================================

    @Test
    void everySimulatedFactEchoCarriesExplicitSimulationProvenance() {

        Requirement req = requirement(1L, new FactPredicateNode("EDUCATION.DEGREE_AWARDED", PredicateOperator.EXISTS, null, null, null, null));
        stubPathwayAndRequirement(req);

        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(EvaluationFactView.empty());
        when(temporalFactResolver.overlayHypothetical(any(), any())).thenAnswer(invocation ->
                realOverlayResolver.overlayHypothetical(invocation.getArgument(0), invocation.getArgument(1)));

        ScenarioSimulationResult result = service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(override("EDUCATION.DEGREE_AWARDED", "MSc")));

        assertThat(result.hypotheticalInput())
                .isNotEmpty()
                .allMatch(echo -> echo.provenance() == FactProvenanceType.SIMULATION);

        assertThat(result.disclaimer()).isEqualTo(ScenarioSimulationResult.DISCLAIMER);
    }

    // =========================================================================
    // 18/ZERO-WRITE. THE SIMULATION NEVER WRITES ANYTHING - RELEASE-BLOCKING
    // =========================================================================

    @Test
    void simulationNeverWritesToAnyAuthoritativePersistenceMethod() {

        Requirement req = requirement(1L, new FactPredicateNode("EMPLOYMENT.SALARY", PredicateOperator.AT_LEAST, "50000", null, null, null));
        stubPathwayAndRequirement(req);

        EvaluationFactView real = new EvaluationFactView(
                Map.of("EMPLOYMENT.SALARY", List.of(realFact("EMPLOYMENT.SALARY", FactCategory.EMPLOYMENT, null, 20000.0))),
                Map.of()
        );
        when(temporalFactResolver.resolve(any(), eq(SUBJECT_ID), any())).thenReturn(real);
        when(temporalFactResolver.overlayHypothetical(any(), any())).thenAnswer(invocation ->
                realOverlayResolver.overlayHypothetical(invocation.getArgument(0), invocation.getArgument(1)));

        service.simulate(user(SUBJECT_ID), PATHWAY_ID, request(override("EMPLOYMENT.SALARY", "60000")));

        // The three persistence methods RequirementEvaluationService.persist()
        // calls for a REAL (persistResults=true) run - none may EVER be
        // invoked by a simulation, on either the BEFORE or the AFTER pass.
        verify(evaluationRepository, never()).save(any());
        verify(evaluationFactRepository, never()).save(any());
        verify(evaluationConflictRepository, never()).save(any());
    }
}
