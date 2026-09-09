package com.godfrey.ai_immigration_document_analyzer.caseintelligence.service;

import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseRiskBand;
import com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto.CaseTimelineResponse;
import com.godfrey.ai_immigration_document_analyzer.entity.Document;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.DocumentVersion;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.DocumentVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.DigitalTwinResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.service.DigitalTwinProjectionService;
import com.godfrey.ai_immigration_document_analyzer.fact.service.FactAuthorizationService;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CaseOverviewService} - cross-document contradiction
 * surfacing, immigration timeline gap detection, and evidence/anomaly
 * signal banding (Master Platform Expansion, Phase 2).
 */
@ExtendWith(MockitoExtension.class)
class CaseOverviewServiceTest {

    @Mock
    private DigitalTwinProjectionService digitalTwinProjectionService;

    @Mock
    private FactAuthorizationService factAuthorizationService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private EvidenceItemRepository evidenceItemRepository;

    private CaseOverviewService caseOverviewService;

    private final AuthenticatedUser actor = null; // never dereferenced by mocked collaborators

    @BeforeEach
    void setUp() {
        caseOverviewService = new CaseOverviewService(
                digitalTwinProjectionService, factAuthorizationService,
                documentRepository, documentVersionRepository, evidenceItemRepository
        );
    }

    private FactResponse fact(String factKey, FactCategory category, LocalDateTime from, LocalDateTime to) {
        return new FactResponse(
                1L, 100L, category, factKey, FactValueType.STRING, "value", null, null, null,
                FactStatus.ACCEPTED, FactProvenanceType.USER_INPUT, FactSensitivityTier.T2_STANDARD_PERSONAL,
                0.9, FactConfidenceLevel.HIGH, "explanation", true, LocalDateTime.now(), null,
                from, to, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null, List.of()
        );
    }

    // =========================================================================
    // CONTRADICTIONS
    // =========================================================================

    @Test
    void contradictionsDelegateEntirelyToTheDigitalTwinsOpenConflicts() {

        DigitalTwinResponse twin = new DigitalTwinResponse(100L, LocalDateTime.now(), List.of(), List.of(), "note");
        when(digitalTwinProjectionService.getDigitalTwin(any(), anyLong())).thenReturn(twin);

        var response = caseOverviewService.getContradictions(actor, 100L);

        assertThat(response.openContradictions()).isEmpty();
        assertThat(response.subjectUserId()).isEqualTo(100L);
    }

    // =========================================================================
    // TIMELINE
    // =========================================================================

    @Test
    void onlyTimelineRelevantCategoriesAppearInTheTimeline() {

        FactResponse employment = fact("EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT,
                LocalDateTime.of(2020, 1, 1, 0, 0), null);
        FactResponse finances = fact("FINANCES.SAVINGS_BALANCE", FactCategory.FINANCES,
                LocalDateTime.of(2021, 1, 1, 0, 0), null);

        DigitalTwinResponse twin = new DigitalTwinResponse(
                100L, LocalDateTime.now(), List.of(employment, finances), List.of(), "note"
        );
        when(digitalTwinProjectionService.getDigitalTwin(any(), anyLong())).thenReturn(twin);

        CaseTimelineResponse response = caseOverviewService.getTimeline(actor, 100L);

        assertThat(response.events()).hasSize(1);
        assertThat(response.events().get(0).fact().factKey()).isEqualTo("EMPLOYMENT.CURRENT_EMPLOYER");
    }

    @Test
    void aGapIsDetectedBetweenTwoSuccessiveHistoricalMultiValuedFactsOfTheSameKey() {

        FactResponse firstEmployer = fact("EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT,
                LocalDateTime.of(2018, 1, 1, 0, 0), LocalDateTime.of(2019, 1, 1, 0, 0));
        FactResponse secondEmployer = fact("EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT,
                LocalDateTime.of(2019, 6, 1, 0, 0), null);

        DigitalTwinResponse twin = new DigitalTwinResponse(
                100L, LocalDateTime.now(), List.of(firstEmployer, secondEmployer), List.of(), "note"
        );
        when(digitalTwinProjectionService.getDigitalTwin(any(), anyLong())).thenReturn(twin);

        CaseTimelineResponse response = caseOverviewService.getTimeline(actor, 100L);

        assertThat(response.events()).hasSize(2);
        assertThat(response.events().get(0).gapDaysBeforeThisEntry()).isNull();
        assertThat(response.events().get(1).gapDaysBeforeThisEntry()).isEqualTo(151L); // 2019-01-01 -> 2019-06-01
    }

    @Test
    void backToBackFactsWithNoGapReportNullGap() {

        FactResponse firstEmployer = fact("EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT,
                LocalDateTime.of(2018, 1, 1, 0, 0), LocalDateTime.of(2019, 1, 1, 0, 0));
        FactResponse secondEmployer = fact("EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT,
                LocalDateTime.of(2019, 1, 1, 0, 0), null);

        DigitalTwinResponse twin = new DigitalTwinResponse(
                100L, LocalDateTime.now(), List.of(firstEmployer, secondEmployer), List.of(), "note"
        );
        when(digitalTwinProjectionService.getDigitalTwin(any(), anyLong())).thenReturn(twin);

        CaseTimelineResponse response = caseOverviewService.getTimeline(actor, 100L);

        assertThat(response.events().get(1).gapDaysBeforeThisEntry()).isNull();
    }

    @Test
    void overlappingPeriodsOfTheSameKeyAreFlaggedAsAPotentialOverlapNeverAConflict() {

        FactResponse firstEmployer = fact("EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT,
                LocalDateTime.of(2018, 1, 1, 0, 0), LocalDateTime.of(2019, 6, 1, 0, 0));
        FactResponse secondEmployer = fact("EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT,
                LocalDateTime.of(2019, 1, 1, 0, 0), null);

        DigitalTwinResponse twin = new DigitalTwinResponse(
                100L, LocalDateTime.now(), List.of(firstEmployer, secondEmployer), List.of(), "note"
        );
        when(digitalTwinProjectionService.getDigitalTwin(any(), anyLong())).thenReturn(twin);

        CaseTimelineResponse timeline = caseOverviewService.getTimeline(actor, 100L);

        assertThat(timeline.events().get(1).gapDaysBeforeThisEntry()).isNull();
        assertThat(timeline.events().get(1).overlapDaysWithPreviousEntry()).isEqualTo(151L); // 2019-01-01 -> 2019-06-01

        var contradictions = caseOverviewService.getContradictions(actor, 100L);

        assertThat(contradictions.potentialOverlaps()).hasSize(1);
        assertThat(contradictions.potentialOverlaps().get(0).factKey()).isEqualTo("EMPLOYMENT.CURRENT_EMPLOYER");
        assertThat(contradictions.potentialOverlaps().get(0).description()).contains("potential contradiction");
        assertThat(contradictions.potentialOverlaps().get(0).description()).contains("not a finding of fraud");
    }

    @Test
    void nonHistoricalMultiValuedKeysAreNeverCheckedForOverlap() {

        // IDENTITY.FULL_NAME is SINGLE_CURRENT, not HISTORICAL_MULTI_VALUED -
        // two ACCEPTED facts of that key would only ever occur via a resolved
        // conflict, never a legitimate "successive period" - so overlap
        // detection must not apply to it.
        FactResponse first = fact("IMMIGRATION_HISTORY.PRIOR_VISA", FactCategory.IMMIGRATION_HISTORY,
                LocalDateTime.of(2018, 1, 1, 0, 0), LocalDateTime.of(2019, 1, 1, 0, 0));
        FactResponse second = fact("IMMIGRATION_HISTORY.PRIOR_VISA", FactCategory.IMMIGRATION_HISTORY,
                LocalDateTime.of(2018, 6, 1, 0, 0), null);

        DigitalTwinResponse twin = new DigitalTwinResponse(
                100L, LocalDateTime.now(), List.of(first, second), List.of(), "note"
        );
        when(digitalTwinProjectionService.getDigitalTwin(any(), anyLong())).thenReturn(twin);

        var contradictions = caseOverviewService.getContradictions(actor, 100L);

        // IMMIGRATION_HISTORY.PRIOR_VISA is not a registered FactTypeRegistry key in this
        // test's fixture data, so FactTypeRegistry.get() returns null and isHistoricalMultiValued
        // safely returns false - no overlap is ever reported for an unregistered/non-HMV key.
        assertThat(contradictions.potentialOverlaps()).isEmpty();
    }

    // =========================================================================
    // SIGNALS
    // =========================================================================

    @Test
    void noSignalsAtAllProducesTheNormalBand() {

        when(documentRepository.findByUserIdOrderByUploadedAtDesc(100L)).thenReturn(List.of());

        DigitalTwinResponse twin = new DigitalTwinResponse(100L, LocalDateTime.now(), List.of(), List.of(), "note");
        when(digitalTwinProjectionService.getDigitalTwin(any(), anyLong())).thenReturn(twin);

        var response = caseOverviewService.getSignals(actor, 100L);

        assertThat(response.riskBand()).isEqualTo(CaseRiskBand.NORMAL);
        assertThat(response.fraudFlaggedDocumentCount()).isZero();
    }

    @Test
    void twoFraudFlaggedDocumentsProduceTheCriticalBand() {

        Document flaggedOne = Document.builder().id(1L).userId(100L).fraudDetected(true).riskLevel("HIGH").build();
        Document flaggedTwo = Document.builder().id(2L).userId(100L).fraudDetected(true).riskLevel("HIGH").build();

        when(documentRepository.findByUserIdOrderByUploadedAtDesc(100L)).thenReturn(List.of(flaggedOne, flaggedTwo));
        lenient().when(documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(any())).thenReturn(List.<DocumentVersion>of());

        DigitalTwinResponse twin = new DigitalTwinResponse(100L, LocalDateTime.now(), List.of(), List.of(), "note");
        when(digitalTwinProjectionService.getDigitalTwin(any(), anyLong())).thenReturn(twin);

        var response = caseOverviewService.getSignals(actor, 100L);

        assertThat(response.riskBand()).isEqualTo(CaseRiskBand.CRITICAL);
        assertThat(response.fraudFlaggedDocumentCount()).isEqualTo(2);
        assertThat(response.note()).contains("never a fraud determination");
    }

    @Test
    void openConflictsAloneProduceOnlyTheMediumBandNeverHigher() {

        when(documentRepository.findByUserIdOrderByUploadedAtDesc(100L)).thenReturn(List.of());

        DigitalTwinResponse twin = new DigitalTwinResponse(
                100L, LocalDateTime.now(), List.of(),
                List.of(com.godfrey.ai_immigration_document_analyzer.fact.dto.FactConflictResponse.from(
                        com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConflict.builder()
                                .id(1L).subjectUserId(100L).factKey("K").factAId(1L).factBId(2L).build()
                )),
                "note"
        );
        when(digitalTwinProjectionService.getDigitalTwin(any(), anyLong())).thenReturn(twin);

        var response = caseOverviewService.getSignals(actor, 100L);

        assertThat(response.riskBand()).isEqualTo(CaseRiskBand.MEDIUM);
        assertThat(response.openContradictionCount()).isEqualTo(1);
    }

    @Test
    void aPotentialOverlapAloneProducesOnlyTheMediumBand() {

        when(documentRepository.findByUserIdOrderByUploadedAtDesc(100L)).thenReturn(List.of());

        FactResponse firstEmployer = fact("EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT,
                LocalDateTime.of(2018, 1, 1, 0, 0), LocalDateTime.of(2019, 6, 1, 0, 0));
        FactResponse secondEmployer = fact("EMPLOYMENT.CURRENT_EMPLOYER", FactCategory.EMPLOYMENT,
                LocalDateTime.of(2019, 1, 1, 0, 0), null);

        DigitalTwinResponse twin = new DigitalTwinResponse(
                100L, LocalDateTime.now(), List.of(firstEmployer, secondEmployer), List.of(), "note"
        );
        when(digitalTwinProjectionService.getDigitalTwin(any(), anyLong())).thenReturn(twin);

        var response = caseOverviewService.getSignals(actor, 100L);

        assertThat(response.riskBand()).isEqualTo(CaseRiskBand.MEDIUM);
        assertThat(response.potentialOverlapContradictionCount()).isEqualTo(1);
    }
}
