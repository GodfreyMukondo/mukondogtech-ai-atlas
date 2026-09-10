package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.DocumentVersion;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItem;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItemStatus;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.DocumentVersionRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.service.EvidenceItemLifecycleService;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactProposalRequest;
import com.godfrey.ai_immigration_document_analyzer.fact.dto.FactResponse;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactConflictRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactEvidenceRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactTimelineEventRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FactService} - the Fact foundation's orchestration
 * entry point.
 *
 * These tests focus on the properties that make the "minimum API surface"
 * design safe: authorization is always checked before any repository
 * write/read, no caller can submit a value shape that does not match its
 * declared type, and reserved (system-only) provenance types can never be
 * injected through the public proposal endpoint.
 */
@ExtendWith(MockitoExtension.class)
class FactServiceTest {

    private static final Long SUBJECT_ID = 10L;
    private static final Long STRANGER_ID = 20L;

    @Mock
    private FactRepository factRepository;

    @Mock
    private FactEvidenceRepository evidenceRepository;

    @Mock
    private FactConflictRepository conflictRepository;

    @Mock
    private FactTimelineEventRepository timelineEventRepository;

    @Mock
    private FactAuthorizationService authorizationService;

    @Mock
    private FactConflictService factConflictService;

    @Mock
    private FactLifecycleService lifecycleService;

    @Mock
    private ConfidenceCalculator confidenceCalculator;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private EvidenceItemRepository evidenceItemRepository;

    @Mock
    private EvidenceItemLifecycleService evidenceItemLifecycleService;

    private FactService factService;

    @BeforeEach
    void setUp() {

        factService = new FactService(
                factRepository, evidenceRepository, conflictRepository, timelineEventRepository,
                authorizationService, factConflictService, lifecycleService, confidenceCalculator,
                documentVersionRepository, evidenceItemRepository, evidenceItemLifecycleService
        );
    }

    private AuthenticatedUser user(Long id, Role... roles) {

        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .toList();

        return new AuthenticatedUser(id, "user" + id + "@example.com", "hash", authorities, true, true, true, true);
    }

    private FactProposalRequest validRequest() {

        FactProposalRequest request = new FactProposalRequest();
        request.setSubjectUserId(SUBJECT_ID);
        request.setCategory(FactCategory.IDENTITY);
        request.setFactKey("IDENTITY.FULL_NAME");
        request.setValueType(FactValueType.STRING);
        request.setStringValue("Jane Doe");
        request.setProvenanceType(FactProvenanceType.USER_INPUT);
        return request;
    }

    // =========================================================================
    // PROPOSE - VALIDATION ORDERING AND SHAPE
    // =========================================================================

    @Test
    void proposeFactRejectsAnUnknownFactKey() {

        FactProposalRequest request = validRequest();
        request.setFactKey("NOT_A_REAL_KEY");

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        assertThatThrownBy(() -> factService.proposeFact(actor, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown fact key");

        verify(factRepository, never()).save(any());
    }

    @Test
    void proposeFactRejectsACategoryThatDoesNotMatchTheRegisteredFactKey() {

        FactProposalRequest request = validRequest();
        request.setCategory(FactCategory.EMPLOYMENT); // IDENTITY.FULL_NAME is registered under IDENTITY

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        assertThatThrownBy(() -> factService.proposeFact(actor, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category does not match");
    }

    @Test
    void proposeFactRejectsAValueTypeThatDoesNotMatchTheRegisteredFactKey() {

        FactProposalRequest request = validRequest();
        request.setValueType(FactValueType.NUMBER);
        request.setStringValue(null);
        request.setNumberValue(42.0);

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        assertThatThrownBy(() -> factService.proposeFact(actor, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Value type does not match");
    }

    @Test
    void proposeFactChecksAuthorizationBeforeTouchingTheRepository() {

        FactProposalRequest request = validRequest();
        AuthenticatedUser stranger = user(STRANGER_ID, Role.USER);

        doThrow(new AccessDeniedException("denied")).when(authorizationService)
                .assertCanCreate(eq(stranger), eq(SUBJECT_ID), any(), anyString());

        assertThatThrownBy(() -> factService.proposeFact(stranger, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(factRepository, never()).save(any());
        verify(evidenceRepository, never()).save(any());
    }

    @Test
    void proposeFactRejectsSystemOnlyProvenanceFromThePublicApi() {

        FactProposalRequest request = validRequest();
        request.setProvenanceType(FactProvenanceType.DERIVED_FACT);

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        assertThatThrownBy(() -> factService.proposeFact(actor, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be submitted directly");

        verify(factRepository, never()).save(any());
    }

    @Test
    void proposeFactRejectsMultiplePopulatedValueFields() {

        FactProposalRequest request = validRequest();
        request.setNumberValue(5.0); // stringValue is also set by validRequest()

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        assertThatThrownBy(() -> factService.proposeFact(actor, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one value field");
    }

    @Test
    void proposeFactRejectsNoPopulatedValueField() {

        FactProposalRequest request = validRequest();
        request.setStringValue(null);

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        assertThatThrownBy(() -> factService.proposeFact(actor, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Exactly one value field");
    }

    @Test
    void proposeFactHappyPathSavesAndDelegatesConflictHandling() {

        FactProposalRequest request = validRequest();
        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        when(confidenceCalculator.compute(any())).thenReturn(
                new ConfidenceCalculator.Result(0.5, com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel.MODERATE, "self-reported")
        );

        when(factRepository.save(any(Fact.class))).thenAnswer(invocation -> {
            Fact f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        Fact accepted = Fact.builder()
                .id(1L).subjectUserId(SUBJECT_ID).category(FactCategory.IDENTITY).factKey("IDENTITY.FULL_NAME")
                .valueType(FactValueType.STRING).stringValue("Jane Doe").status(FactStatus.ACCEPTED)
                .provenanceType(FactProvenanceType.USER_INPUT)
                .confidenceScore(0.5).confidenceLevel(com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel.MODERATE)
                .isVerified(false).observedAt(LocalDateTime.now()).lastObservedAt(LocalDateTime.now())
                .build();

        when(factConflictService.processIncomingFact(any(Fact.class))).thenReturn(accepted);
        when(evidenceRepository.findByFactId(1L)).thenReturn(List.of());

        FactResponse response = factService.proposeFact(actor, request);

        assertThat(response.status()).isEqualTo(FactStatus.ACCEPTED);
        assertThat(response.factKey()).isEqualTo("IDENTITY.FULL_NAME");

        verify(authorizationService).assertCanCreate(eq(actor), eq(SUBJECT_ID), any(), anyString());
        verify(factRepository).save(any(Fact.class));
        verify(factConflictService).processIncomingFact(any(Fact.class));
    }

    // =========================================================================
    // PROPOSE - EVIDENCE INTELLIGENCE GRAPH LINKAGE (Phase 4 gap closure)
    // =========================================================================

    private void stubHappyPathSave(Fact accepted) {

        when(confidenceCalculator.compute(any())).thenReturn(
                new ConfidenceCalculator.Result(0.5, com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel.MODERATE, "document extraction")
        );

        when(factRepository.save(any(Fact.class))).thenAnswer(invocation -> {
            Fact f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        when(factConflictService.processIncomingFact(any(Fact.class))).thenReturn(accepted);
        when(evidenceRepository.findByFactId(1L)).thenReturn(List.of());
    }

    @Test
    void proposeFactLinksAnExistingCandidateEvidenceItemAndTransitionsItToLinkedWhenAccepted() {

        FactProposalRequest request = validRequest();
        request.setProvenanceType(FactProvenanceType.DOCUMENT_EXTRACTION);
        request.setSourceDocumentId(55L);

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        DocumentVersion version = DocumentVersion.builder().id(1L).documentId(55L).versionNumber(1).build();
        EvidenceItem candidate = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).status(EvidenceItemStatus.CANDIDATE).build();

        when(documentVersionRepository.findFirstByDocumentIdOrderByVersionNumberDesc(55L))
                .thenReturn(Optional.of(version));
        when(evidenceItemRepository.findByDocumentVersionId(1L)).thenReturn(List.of(candidate));

        Fact accepted = Fact.builder()
                .id(1L).subjectUserId(SUBJECT_ID).category(FactCategory.IDENTITY).factKey("IDENTITY.FULL_NAME")
                .valueType(FactValueType.STRING).stringValue("Jane Doe").status(FactStatus.ACCEPTED)
                .provenanceType(FactProvenanceType.DOCUMENT_EXTRACTION)
                .confidenceScore(0.5).confidenceLevel(com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel.MODERATE)
                .isVerified(false).observedAt(LocalDateTime.now()).lastObservedAt(LocalDateTime.now())
                .build();

        stubHappyPathSave(accepted);

        factService.proposeFact(actor, request);

        verify(evidenceRepository).save(org.mockito.ArgumentMatchers.argThat(evidence ->
                evidence.getEvidenceItemId() != null && evidence.getEvidenceItemId().equals(900L)
        ));
        verify(evidenceItemLifecycleService).transition(candidate, EvidenceItemStatus.LINKED);
    }

    @Test
    void proposeFactDoesNotLinkAValidationFailedEvidenceItem() {

        FactProposalRequest request = validRequest();
        request.setProvenanceType(FactProvenanceType.DOCUMENT_EXTRACTION);
        request.setSourceDocumentId(55L);

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        DocumentVersion version = DocumentVersion.builder().id(1L).documentId(55L).versionNumber(1).build();
        EvidenceItem validationFailed = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).status(EvidenceItemStatus.VALIDATION_FAILED).build();

        when(documentVersionRepository.findFirstByDocumentIdOrderByVersionNumberDesc(55L))
                .thenReturn(Optional.of(version));
        when(evidenceItemRepository.findByDocumentVersionId(1L)).thenReturn(List.of(validationFailed));

        Fact accepted = Fact.builder()
                .id(1L).subjectUserId(SUBJECT_ID).category(FactCategory.IDENTITY).factKey("IDENTITY.FULL_NAME")
                .valueType(FactValueType.STRING).stringValue("Jane Doe").status(FactStatus.ACCEPTED)
                .provenanceType(FactProvenanceType.DOCUMENT_EXTRACTION)
                .confidenceScore(0.5).confidenceLevel(com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel.MODERATE)
                .isVerified(false).observedAt(LocalDateTime.now()).lastObservedAt(LocalDateTime.now())
                .build();

        stubHappyPathSave(accepted);

        factService.proposeFact(actor, request);

        verify(evidenceRepository).save(org.mockito.ArgumentMatchers.argThat(evidence ->
                evidence.getEvidenceItemId() == null
        ));
        verify(evidenceItemLifecycleService, never()).transition(any(), any());
    }

    @Test
    void proposeFactLeavesACandidateEvidenceItemUnlinkedWhenTheFactIsNotAccepted() {

        FactProposalRequest request = validRequest();
        request.setProvenanceType(FactProvenanceType.DOCUMENT_EXTRACTION);
        request.setSourceDocumentId(55L);

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        DocumentVersion version = DocumentVersion.builder().id(1L).documentId(55L).versionNumber(1).build();
        EvidenceItem candidate = EvidenceItem.builder()
                .id(900L).documentVersionId(1L).status(EvidenceItemStatus.CANDIDATE).build();

        when(documentVersionRepository.findFirstByDocumentIdOrderByVersionNumberDesc(55L))
                .thenReturn(Optional.of(version));
        when(evidenceItemRepository.findByDocumentVersionId(1L)).thenReturn(List.of(candidate));

        Fact contested = Fact.builder()
                .id(1L).subjectUserId(SUBJECT_ID).category(FactCategory.IDENTITY).factKey("IDENTITY.FULL_NAME")
                .valueType(FactValueType.STRING).stringValue("Jane Doe").status(FactStatus.CONTESTED)
                .provenanceType(FactProvenanceType.DOCUMENT_EXTRACTION)
                .confidenceScore(0.5).confidenceLevel(com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel.MODERATE)
                .isVerified(false).observedAt(LocalDateTime.now()).lastObservedAt(LocalDateTime.now())
                .build();

        stubHappyPathSave(contested);

        factService.proposeFact(actor, request);

        verify(evidenceItemLifecycleService, never()).transition(any(), any());
    }

    @Test
    void proposeFactRecordsPlainFactEvidenceWhenNoEvidenceItemExistsYet() {

        FactProposalRequest request = validRequest();
        request.setProvenanceType(FactProvenanceType.DOCUMENT_EXTRACTION);
        request.setSourceDocumentId(55L);

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        when(documentVersionRepository.findFirstByDocumentIdOrderByVersionNumberDesc(55L))
                .thenReturn(Optional.empty());

        Fact accepted = Fact.builder()
                .id(1L).subjectUserId(SUBJECT_ID).category(FactCategory.IDENTITY).factKey("IDENTITY.FULL_NAME")
                .valueType(FactValueType.STRING).stringValue("Jane Doe").status(FactStatus.ACCEPTED)
                .provenanceType(FactProvenanceType.DOCUMENT_EXTRACTION)
                .confidenceScore(0.5).confidenceLevel(com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel.MODERATE)
                .isVerified(false).observedAt(LocalDateTime.now()).lastObservedAt(LocalDateTime.now())
                .build();

        stubHappyPathSave(accepted);

        factService.proposeFact(actor, request);

        verify(evidenceRepository).save(org.mockito.ArgumentMatchers.argThat(evidence ->
                evidence.getEvidenceItemId() == null && evidence.getDocumentId().equals(55L)
        ));
        verify(evidenceItemLifecycleService, never()).transition(any(), any());
    }

    // =========================================================================
    // RETRIEVE - IDOR PROTECTION DELEGATED TO THE AUTHORIZATION BOUNDARY
    // =========================================================================

    @Test
    void getFactThrowsResourceNotFoundWhenTheFactDoesNotExist() {

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        when(factRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factService.getFact(actor, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getFactPropagatesAccessDeniedForAnUnauthorizedActorWithoutLeakingData() {

        Fact fact = Fact.builder().id(1L).subjectUserId(SUBJECT_ID).build();
        AuthenticatedUser stranger = user(STRANGER_ID, Role.USER);

        when(factRepository.findById(1L)).thenReturn(Optional.of(fact));

        doThrow(new AccessDeniedException("denied")).when(authorizationService)
                .assertCanView(eq(stranger), eq(SUBJECT_ID), any(), eq(1L), anyString());

        assertThatThrownBy(() -> factService.getFact(stranger, 1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(evidenceRepository, never()).findByFactId(any());
    }

    @Test
    void listFactsForSubjectChecksAuthorizationBeforeQueryingTheRepository() {

        AuthenticatedUser stranger = user(STRANGER_ID, Role.USER);

        doThrow(new AccessDeniedException("denied")).when(authorizationService)
                .assertCanView(eq(stranger), eq(SUBJECT_ID), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), anyString());

        assertThatThrownBy(() -> factService.listFactsForSubject(stranger, SUBJECT_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(factRepository, never()).findBySubjectUserId(any());
    }

    // =========================================================================
    // VERIFY - DELEGATED TO THE LIFECYCLE SERVICE, NEVER DIRECT
    // =========================================================================

    @Test
    void verifyFactDelegatesToAuthorizationThenLifecycle() {

        Fact fact = Fact.builder()
                .id(1L).subjectUserId(SUBJECT_ID).factKey("IDENTITY.FULL_NAME").status(FactStatus.ACCEPTED)
                .provenanceType(FactProvenanceType.USER_INPUT).isVerified(false)
                .observedAt(LocalDateTime.now()).lastObservedAt(LocalDateTime.now())
                .build();

        AuthenticatedUser caseWorker = user(30L, Role.CASE_WORKER);

        var verificationRequest = new com.godfrey.ai_immigration_document_analyzer.fact.dto.FactVerificationRequest();
        verificationRequest.setMethod(com.godfrey.ai_immigration_document_analyzer.fact.entity.VerificationMethod.HUMAN_REVIEW);
        verificationRequest.setNotes("confirmed");

        when(factRepository.findById(1L)).thenReturn(Optional.of(fact));

        Fact verified = Fact.builder()
                .id(1L).subjectUserId(SUBJECT_ID).factKey("IDENTITY.FULL_NAME").status(FactStatus.ACCEPTED)
                .provenanceType(FactProvenanceType.USER_INPUT).isVerified(true)
                .observedAt(LocalDateTime.now()).lastObservedAt(LocalDateTime.now())
                .build();

        when(lifecycleService.verify(eq(fact), eq(30L), any(), eq("confirmed"))).thenReturn(verified);
        when(evidenceRepository.countByFactId(1L)).thenReturn(0L);
        when(evidenceRepository.findByFactId(1L)).thenReturn(List.of());
        when(confidenceCalculator.compute(any())).thenReturn(
                new ConfidenceCalculator.Result(0.9, com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel.HIGH, "verified")
        );
        when(factRepository.save(any(Fact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FactResponse response = factService.verifyFact(caseWorker, 1L, verificationRequest);

        assertThat(response.isVerified()).isTrue();

        verify(authorizationService).assertCanVerifyOrResolve(eq(caseWorker), eq(SUBJECT_ID), any(), eq(1L), anyString());
        verify(lifecycleService).verify(eq(fact), eq(30L), any(), eq("confirmed"));
    }

    // =========================================================================
    // CONFLICTS
    // =========================================================================

    @Test
    void getConflictThrowsResourceNotFoundWhenMissing() {

        AuthenticatedUser actor = user(SUBJECT_ID, Role.USER);

        when(conflictRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factService.getConflict(actor, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
