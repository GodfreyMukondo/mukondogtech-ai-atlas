package com.godfrey.ai_immigration_document_analyzer.requirement.logic;

import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LogicEvaluationService} - the closed-grammar
 * three-valued interpreter every Requirement/Pathway evaluation is built
 * on. These tests are the primary evidence base for the resolved Kleene
 * combination rule: a definite NOT_SATISFIED/SATISFIED must never be
 * confused with INSUFFICIENT_EVIDENCE/UNKNOWN/CONFLICTED, and NOT_APPLICABLE
 * must behave as a true identity element for AND/OR.
 */
class LogicEvaluationServiceTest {

    private final LogicEvaluationService evaluator = new LogicEvaluationService();

    private FactResponse fact(
            Long id, String factKey, FactValueType valueType, String stringValue, LocalDateTime dateValue,
            Double numberValue, Boolean booleanValue, boolean verified, FactProvenanceType provenance, double confidence
    ) {
        LocalDateTime now = LocalDateTime.now();

        return new FactResponse(
                id, 10L, FactCategory.IDENTITY, factKey, valueType, stringValue, dateValue, numberValue, booleanValue,
                FactStatus.ACCEPTED, provenance, FactSensitivityTier.T2_STANDARD_PERSONAL, confidence,
                confidence >= 0.8 ? FactConfidenceLevel.HIGH : FactConfidenceLevel.MODERATE, "explanation",
                verified, verified ? now : null, null, null, null, now, now, now, null, List.of()
        );
    }

    private EvaluationContext contextWith(Map<String, List<FactResponse>> facts, Map<String, Long> conflicts) {
        return new EvaluationContext(
                10L, LocalDateTime.now(), new EvaluationFactView(facts, conflicts), Map.of(),
                requirementId -> { throw new UnsupportedOperationException("no REQUIREMENT_REF expected in this test"); }
        );
    }

    // =========================================================================
    // FACT PREDICATE - PRESENCE AND ABSENCE
    // =========================================================================

    @Test
    void absentFactYieldsInsufficientEvidenceNeverNotSatisfied() {

        FactPredicateNode node = new FactPredicateNode("LANGUAGE_PROFICIENCY.TEST_SCORE", PredicateOperator.EXISTS, null, null, null, null);
        EvaluationContext context = contextWith(Map.of(), Map.of());

        NodeResult result = evaluator.evaluate(node, context);

        assertThat(result.value()).isEqualTo(KleeneValue.INDETERMINATE);
        assertThat(result.flavor()).isEqualTo(IndeterminateFlavor.INSUFFICIENT_EVIDENCE);
    }

    @Test
    void conflictedFactYieldsConflictedNeverNotSatisfied() {

        FactPredicateNode node = new FactPredicateNode("EMPLOYMENT.CURRENT_EMPLOYER", PredicateOperator.EXISTS, null, null, null, null);
        EvaluationContext context = contextWith(Map.of(), Map.of("EMPLOYMENT.CURRENT_EMPLOYER", 99L));

        NodeResult result = evaluator.evaluate(node, context);

        assertThat(result.value()).isEqualTo(KleeneValue.INDETERMINATE);
        assertThat(result.flavor()).isEqualTo(IndeterminateFlavor.CONFLICTED);
        assertThat(result.unresolvedConflictIds()).containsExactly(99L);
    }

    @Test
    void notExistsIsTrueWhenFactIsGenuinelyAbsent() {

        FactPredicateNode node = new FactPredicateNode("IMMIGRATION_HISTORY.PRIOR_REFUSAL", PredicateOperator.NOT_EXISTS, null, null, null, null);
        EvaluationContext context = contextWith(Map.of(), Map.of());

        NodeResult result = evaluator.evaluate(node, context);

        assertThat(result.value()).isEqualTo(KleeneValue.TRUE);
    }

    @Test
    void notExistsIsFalseWhenFactIsPresent() {

        FactResponse priorRefusal = fact(1L, "IMMIGRATION_HISTORY.PRIOR_REFUSAL", FactValueType.STRING, "refused 2020", null, null, null, true, FactProvenanceType.HUMAN_VERIFICATION, 0.9);
        FactPredicateNode node = new FactPredicateNode("IMMIGRATION_HISTORY.PRIOR_REFUSAL", PredicateOperator.NOT_EXISTS, null, null, null, null);
        EvaluationContext context = contextWith(Map.of("IMMIGRATION_HISTORY.PRIOR_REFUSAL", List.of(priorRefusal)), Map.of());

        NodeResult result = evaluator.evaluate(node, context);

        assertThat(result.value()).isEqualTo(KleeneValue.FALSE);
    }

    // =========================================================================
    // FACT PREDICATE - EVIDENCE QUALITY GATING
    // =========================================================================

    @Test
    void unverifiedFactFailingEvidenceExpectationIsInsufficientEvidenceNotFalse() {

        FactResponse degree = fact(1L, "EDUCATION.DEGREE_AWARDED", FactValueType.STRING, "Bachelor of Science", null, null, null, false, FactProvenanceType.USER_INPUT, 0.5);

        FactPredicateNode node = new FactPredicateNode(
                "EDUCATION.DEGREE_AWARDED", PredicateOperator.IN, null, List.of("Bachelor's", "Master's"), null, null
        );

        EvaluationContext context = new EvaluationContext(
                10L, LocalDateTime.now(),
                new EvaluationFactView(Map.of("EDUCATION.DEGREE_AWARDED", List.of(degree)), Map.of()),
                Map.of("EDUCATION.DEGREE_AWARDED", new FactEvidenceExpectation(true, null)), // requires verification
                requirementId -> { throw new UnsupportedOperationException(); }
        );

        NodeResult result = evaluator.evaluate(node, context);

        assertThat(result.value()).isEqualTo(KleeneValue.INDETERMINATE);
        assertThat(result.flavor()).isEqualTo(IndeterminateFlavor.INSUFFICIENT_EVIDENCE);
    }

    // =========================================================================
    // SCENARIO A - VERIFIED BACHELOR'S SATISFIES AN EDUCATION REQUIREMENT
    // =========================================================================

    @Test
    void scenarioA_verifiedBachelorsDegreeSatisfiesAnInPredicate() {

        FactResponse degree = fact(1L, "EDUCATION.DEGREE_AWARDED", FactValueType.STRING, "Bachelor of Science", null, null, null, true, FactProvenanceType.HUMAN_VERIFICATION, 0.9);

        FactPredicateNode node = new FactPredicateNode(
                "EDUCATION.DEGREE_AWARDED", PredicateOperator.IN, null, List.of("Bachelor of Science", "Master's"), null, null
        );

        EvaluationContext context = contextWith(Map.of("EDUCATION.DEGREE_AWARDED", List.of(degree)), Map.of());

        NodeResult result = evaluator.evaluate(node, context);

        assertThat(result.value()).isEqualTo(KleeneValue.TRUE);
        assertThat(result.contributingFactIds()).containsExactly(1L);
        assertThat(result.certaintyScore()).isEqualTo(0.9);
    }

    // =========================================================================
    // KLEENE COMBINATION - AND
    // =========================================================================

    @Test
    void andShortCircuitsToFalseEvenNextToAnIndeterminateSibling() {

        FactResponse age = fact(2L, "NUMERIC", FactValueType.NUMBER, null, null, 19.0, null, true, FactProvenanceType.HUMAN_VERIFICATION, 0.9);

        AndNode realAnd = new AndNode(List.of(
                new FactPredicateNode("NUMERIC", PredicateOperator.AT_LEAST, "25", null, null, null), // FALSE (19 < 25)
                new FactPredicateNode("MISSING", PredicateOperator.EXISTS, null, null, null, null)     // INSUFFICIENT_EVIDENCE
        ));

        EvaluationContext context = contextWith(Map.of("NUMERIC", List.of(age)), Map.of());

        NodeResult result = evaluator.evaluate(realAnd, context);

        assertThat(result.value()).isEqualTo(KleeneValue.FALSE);
    }

    @Test
    void andIsIndeterminateWhenNoBranchIsDefinitivelyFalse() {

        AndNode and = new AndNode(List.of(
                new FactPredicateNode("PRESENT", PredicateOperator.EXISTS, null, null, null, null),
                new FactPredicateNode("MISSING", PredicateOperator.EXISTS, null, null, null, null)
        ));

        FactResponse present = fact(1L, "PRESENT", FactValueType.STRING, "x", null, null, null, true, FactProvenanceType.HUMAN_VERIFICATION, 0.9);
        EvaluationContext context = contextWith(Map.of("PRESENT", List.of(present)), Map.of());

        NodeResult result = evaluator.evaluate(and, context);

        assertThat(result.value()).isEqualTo(KleeneValue.INDETERMINATE);
        assertThat(result.flavor()).isEqualTo(IndeterminateFlavor.INSUFFICIENT_EVIDENCE);
    }

    @Test
    void andOfAllTrueIsTrueWithMinimumCertainty() {

        FactResponse strong = fact(1L, "A", FactValueType.STRING, "x", null, null, null, true, FactProvenanceType.EXTERNAL_AUTHORITATIVE_SOURCE, 0.9);
        FactResponse weak = fact(2L, "B", FactValueType.STRING, "y", null, null, null, true, FactProvenanceType.USER_INPUT, 0.5);

        AndNode and = new AndNode(List.of(
                new FactPredicateNode("A", PredicateOperator.EXISTS, null, null, null, null),
                new FactPredicateNode("B", PredicateOperator.EXISTS, null, null, null, null)
        ));

        EvaluationContext context = contextWith(Map.of("A", List.of(strong), "B", List.of(weak)), Map.of());

        NodeResult result = evaluator.evaluate(and, context);

        assertThat(result.value()).isEqualTo(KleeneValue.TRUE);
        assertThat(result.certaintyScore()).isEqualTo(0.5); // weakest link
    }

    // =========================================================================
    // KLEENE COMBINATION - OR
    // =========================================================================

    @Test
    void orIsTrueIfAnyBranchIsTrueRegardlessOfSiblingUncertainty() {

        FactResponse present = fact(1L, "A", FactValueType.STRING, "x", null, null, null, true, FactProvenanceType.HUMAN_VERIFICATION, 0.85);

        OrNode or = new OrNode(List.of(
                new FactPredicateNode("A", PredicateOperator.EXISTS, null, null, null, null),
                new FactPredicateNode("MISSING", PredicateOperator.EXISTS, null, null, null, null)
        ));

        EvaluationContext context = contextWith(Map.of("A", List.of(present)), Map.of());

        NodeResult result = evaluator.evaluate(or, context);

        assertThat(result.value()).isEqualTo(KleeneValue.TRUE);
        assertThat(result.certaintyScore()).isEqualTo(0.85);
    }

    @Test
    void orOfAllFalseIsFalse() {

        FactResponse low = fact(1L, "AGE", FactValueType.NUMBER, null, null, 19.0, null, true, FactProvenanceType.HUMAN_VERIFICATION, 0.9);

        OrNode or = new OrNode(List.of(
                new FactPredicateNode("AGE", PredicateOperator.AT_LEAST, "25", null, null, null),
                new FactPredicateNode("AGE", PredicateOperator.AT_LEAST, "30", null, null, null)
        ));

        EvaluationContext context = contextWith(Map.of("AGE", List.of(low)), Map.of());

        NodeResult result = evaluator.evaluate(or, context);

        assertThat(result.value()).isEqualTo(KleeneValue.FALSE);
    }

    // =========================================================================
    // NOT
    // =========================================================================

    @Test
    void notFlipsTrueAndFalseButPassesThroughIndeterminateAndNotApplicable() {

        FactResponse present = fact(1L, "A", FactValueType.STRING, "x", null, null, null, true, FactProvenanceType.HUMAN_VERIFICATION, 0.9);

        NodeResult trueChild = evaluator.evaluate(
                new FactPredicateNode("A", PredicateOperator.EXISTS, null, null, null, null),
                contextWith(Map.of("A", List.of(present)), Map.of())
        );

        assertThat(trueChild.value()).isEqualTo(KleeneValue.TRUE);

        NodeResult negated = evaluator.evaluate(new NotNode(new FactPredicateNode("A", PredicateOperator.EXISTS, null, null, null, null)),
                contextWith(Map.of("A", List.of(present)), Map.of()));

        assertThat(negated.value()).isEqualTo(KleeneValue.FALSE);

        NodeResult negatedAbsent = evaluator.evaluate(
                new NotNode(new FactPredicateNode("MISSING", PredicateOperator.EXISTS, null, null, null, null)),
                contextWith(Map.of(), Map.of())
        );

        assertThat(negatedAbsent.value()).isEqualTo(KleeneValue.INDETERMINATE);
        assertThat(negatedAbsent.flavor()).isEqualTo(IndeterminateFlavor.INSUFFICIENT_EVIDENCE);
    }

    // =========================================================================
    // NOT_APPLICABLE - IDENTITY ELEMENT FOR AND/OR
    // =========================================================================

    @Test
    void notApplicableChildIsExcludedFromAndAggregation() {

        FactResponse present = fact(1L, "A", FactValueType.STRING, "x", null, null, null, true, FactProvenanceType.HUMAN_VERIFICATION, 0.9);

        AndNode and = new AndNode(List.of(
                new FactPredicateNode("A", PredicateOperator.EXISTS, null, null, null, null),
                new RequirementRefNode(999L) // resolver below returns NOT_APPLICABLE
        ));

        EvaluationContext context = new EvaluationContext(
                10L, LocalDateTime.now(),
                new EvaluationFactView(Map.of("A", List.of(present)), Map.of()),
                Map.of(),
                requirementId -> NodeResult.notApplicable()
        );

        NodeResult result = evaluator.evaluate(and, context);

        assertThat(result.value()).isEqualTo(KleeneValue.TRUE); // the NOT_APPLICABLE sibling is excluded, not a vacuous failure
    }

    @Test
    void andOfAllNotApplicableIsItselfNotApplicable() {

        AndNode and = new AndNode(List.of(new RequirementRefNode(1L), new RequirementRefNode(2L)));

        EvaluationContext context = new EvaluationContext(
                10L, LocalDateTime.now(), EvaluationFactView.empty(), Map.of(),
                requirementId -> NodeResult.notApplicable()
        );

        NodeResult result = evaluator.evaluate(and, context);

        assertThat(result.value()).isEqualTo(KleeneValue.NOT_APPLICABLE);
    }

    // =========================================================================
    // DERIVED PREDICATE - AGE_AT
    // =========================================================================

    @Test
    void ageAtDerivesAgeFromDateOfBirthWithoutMutatingIt() {

        LocalDateTime dob = LocalDateTime.of(1999, 3, 14, 0, 0);
        FactResponse dobFact = fact(1L, "IDENTITY.DATE_OF_BIRTH", FactValueType.DATE, null, dob, null, null, true, FactProvenanceType.HUMAN_VERIFICATION, 0.9);

        DerivedPredicateNode node = new DerivedPredicateNode(
                DerivedFunction.AGE_AT, List.of("IDENTITY.DATE_OF_BIRTH"), PredicateOperator.AT_LEAST, "25", null, null
        );

        EvaluationContext context = new EvaluationContext(
                10L, LocalDateTime.of(2026, 9, 8, 0, 0),
                new EvaluationFactView(Map.of("IDENTITY.DATE_OF_BIRTH", List.of(dobFact)), Map.of()),
                Map.of(),
                requirementId -> { throw new UnsupportedOperationException(); }
        );

        NodeResult result = evaluator.evaluate(node, context);

        assertThat(result.value()).isEqualTo(KleeneValue.TRUE); // age 27 >= 25
        assertThat(result.derivedValues()).hasSize(1);
        assertThat(result.derivedValues().get(0).value()).isEqualTo("27");
        assertThat(result.derivedValues().get(0).inputFactIds()).containsExactly(1L);

        // The Fact itself is never touched - dobFact.dateValue() is still the raw date of birth.
        assertThat(dobFact.dateValue()).isEqualTo(dob);
    }

    @Test
    void ageAtBelowThresholdIsFalse() {

        LocalDateTime dob = LocalDateTime.of(2010, 1, 1, 0, 0);
        FactResponse dobFact = fact(1L, "IDENTITY.DATE_OF_BIRTH", FactValueType.DATE, null, dob, null, null, true, FactProvenanceType.HUMAN_VERIFICATION, 0.9);

        DerivedPredicateNode node = new DerivedPredicateNode(
                DerivedFunction.AGE_AT, List.of("IDENTITY.DATE_OF_BIRTH"), PredicateOperator.AT_LEAST, "25", null, null
        );

        EvaluationContext context = new EvaluationContext(
                10L, LocalDateTime.of(2026, 9, 8, 0, 0),
                new EvaluationFactView(Map.of("IDENTITY.DATE_OF_BIRTH", List.of(dobFact)), Map.of()),
                Map.of(),
                requirementId -> { throw new UnsupportedOperationException(); }
        );

        NodeResult result = evaluator.evaluate(node, context);

        assertThat(result.value()).isEqualTo(KleeneValue.FALSE);
    }
}
