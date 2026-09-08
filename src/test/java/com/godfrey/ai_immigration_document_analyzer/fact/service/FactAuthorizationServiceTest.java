package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.CaseAssignmentRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FactAuthorizationService} - the single retrieval
 * boundary for the Fact foundation.
 *
 * These tests are the primary evidence base for the approved access-control
 * matrix: a plain user can never reach another subject's facts, an ADMIN
 * holds no case-data access by itself, and only a CASE_WORKER with an
 * active {@code CaseAssignment} may act on a subject's case. Every decision
 * (granted or denied) must also be recorded through
 * {@link FactAccessAuditService}.
 */
@ExtendWith(MockitoExtension.class)
class FactAuthorizationServiceTest {

    private static final Long SUBJECT_ID = 100L;
    private static final Long STRANGER_ID = 200L;
    private static final Long CASE_WORKER_ID = 300L;
    private static final Long ADMIN_ID = 400L;

    @Mock
    private CaseAssignmentRepository caseAssignmentRepository;

    @Mock
    private FactAccessAuditService accessAuditService;

    private FactAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new FactAuthorizationService(caseAssignmentRepository, accessAuditService);
    }

    private AuthenticatedUser user(Long id, Role... roles) {

        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .toList();

        return new AuthenticatedUser(id, "user" + id + "@example.com", "hash", authorities, true, true, true, true);
    }

    // =========================================================================
    // VIEW - IDOR PROTECTION
    // =========================================================================

    @Test
    void subjectCanViewTheirOwnFact() {

        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        authorizationService.assertCanView(subject, SUBJECT_ID, FactSensitivityTier.T2_STANDARD_PERSONAL, 1L, "view fact");

        verify(accessAuditService).recordAccess(
                eq(SUBJECT_ID), eq(1L), eq(SUBJECT_ID), any(), eq("view fact"),
                eq(FactSensitivityTier.T2_STANDARD_PERSONAL), eq(true), isNull()
        );
    }

    @Test
    void anotherPlainUserCannotViewSomeoneElsesFact() {

        AuthenticatedUser stranger = user(STRANGER_ID, Role.USER);

        assertThatThrownBy(() ->
                authorizationService.assertCanView(stranger, SUBJECT_ID, FactSensitivityTier.T2_STANDARD_PERSONAL, 1L, "view fact")
        ).isInstanceOf(AccessDeniedException.class);

        verify(accessAuditService).recordAccess(
                eq(SUBJECT_ID), eq(1L), eq(STRANGER_ID), any(), eq("view fact"),
                eq(FactSensitivityTier.T2_STANDARD_PERSONAL), eq(false), anyString()
        );
    }

    @Test
    void caseWorkerWithoutActiveAssignmentCannotView() {

        AuthenticatedUser caseWorker = user(CASE_WORKER_ID, Role.CASE_WORKER);

        when(caseAssignmentRepository.existsByCaseWorkerUserIdAndSubjectUserIdAndActiveTrue(CASE_WORKER_ID, SUBJECT_ID))
                .thenReturn(false);

        assertThatThrownBy(() ->
                authorizationService.assertCanView(caseWorker, SUBJECT_ID, null, 1L, "view fact")
        ).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void caseWorkerWithActiveAssignmentCanView() {

        AuthenticatedUser caseWorker = user(CASE_WORKER_ID, Role.CASE_WORKER);

        when(caseAssignmentRepository.existsByCaseWorkerUserIdAndSubjectUserIdAndActiveTrue(CASE_WORKER_ID, SUBJECT_ID))
                .thenReturn(true);

        authorizationService.assertCanView(caseWorker, SUBJECT_ID, null, 1L, "view fact");

        verify(accessAuditService).recordAccess(
                eq(SUBJECT_ID), eq(1L), eq(CASE_WORKER_ID), any(), eq("view fact"), isNull(), eq(true), isNull()
        );
    }

    @Test
    void adminRoleAloneGrantsNoFactAccess() {

        AuthenticatedUser admin = user(ADMIN_ID, Role.ADMIN);

        assertThatThrownBy(() ->
                authorizationService.assertCanView(admin, SUBJECT_ID, null, 1L, "view fact")
        ).isInstanceOf(AccessDeniedException.class);

        // An ADMIN never even reaches the CaseAssignment check - platform
        // administration and case-data access are separate grants.
        verify(caseAssignmentRepository, never())
                .existsByCaseWorkerUserIdAndSubjectUserIdAndActiveTrue(anyLong(), anyLong());
    }

    @Test
    void adminHoldingAlsoCaseWorkerRoleStillNeedsAnActiveAssignment() {

        AuthenticatedUser adminAndCaseWorker = user(ADMIN_ID, Role.ADMIN, Role.CASE_WORKER);

        when(caseAssignmentRepository.existsByCaseWorkerUserIdAndSubjectUserIdAndActiveTrue(ADMIN_ID, SUBJECT_ID))
                .thenReturn(false);

        assertThatThrownBy(() ->
                authorizationService.assertCanView(adminAndCaseWorker, SUBJECT_ID, null, 1L, "view fact")
        ).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void nullActorIsDenied() {

        assertThatThrownBy(() ->
                authorizationService.assertCanView(null, SUBJECT_ID, null, 1L, "view fact")
        ).isInstanceOf(AccessDeniedException.class);
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    @Test
    void subjectCanCreateFactsForThemselves() {

        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        authorizationService.assertCanCreate(subject, SUBJECT_ID, FactSensitivityTier.T1_ROUTINE, "propose fact");

        verify(accessAuditService, times(1)).recordAccess(
                eq(SUBJECT_ID), isNull(), eq(SUBJECT_ID), any(), eq("propose fact"),
                eq(FactSensitivityTier.T1_ROUTINE), eq(true), isNull()
        );
    }

    @Test
    void strangerCannotCreateFactsForAnotherSubject() {

        AuthenticatedUser stranger = user(STRANGER_ID, Role.USER);

        assertThatThrownBy(() ->
                authorizationService.assertCanCreate(stranger, SUBJECT_ID, FactSensitivityTier.T1_ROUTINE, "propose fact")
        ).isInstanceOf(AccessDeniedException.class);
    }

    // =========================================================================
    // VERIFY / RESOLVE - NO SELF-VERIFICATION
    // =========================================================================

    @Test
    void subjectCannotVerifyTheirOwnFact() {

        AuthenticatedUser subject = user(SUBJECT_ID, Role.USER);

        assertThatThrownBy(() ->
                authorizationService.assertCanVerifyOrResolve(subject, SUBJECT_ID, null, 1L, "verify fact")
        ).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assignedCaseWorkerCanVerify() {

        AuthenticatedUser caseWorker = user(CASE_WORKER_ID, Role.CASE_WORKER);

        when(caseAssignmentRepository.existsByCaseWorkerUserIdAndSubjectUserIdAndActiveTrue(CASE_WORKER_ID, SUBJECT_ID))
                .thenReturn(true);

        authorizationService.assertCanVerifyOrResolve(caseWorker, SUBJECT_ID, null, 1L, "verify fact");
    }

    @Test
    void unassignedCaseWorkerCannotVerify() {

        AuthenticatedUser caseWorker = user(CASE_WORKER_ID, Role.CASE_WORKER);

        when(caseAssignmentRepository.existsByCaseWorkerUserIdAndSubjectUserIdAndActiveTrue(CASE_WORKER_ID, SUBJECT_ID))
                .thenReturn(false);

        assertThatThrownBy(() ->
                authorizationService.assertCanVerifyOrResolve(caseWorker, SUBJECT_ID, null, 1L, "verify fact")
        ).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminCannotVerifyEvenWithoutAssignmentPath() {

        AuthenticatedUser admin = user(ADMIN_ID, Role.ADMIN);

        assertThatThrownBy(() ->
                authorizationService.assertCanVerifyOrResolve(admin, SUBJECT_ID, null, 1L, "verify fact")
        ).isInstanceOf(AccessDeniedException.class);

        verify(caseAssignmentRepository, never())
                .existsByCaseWorkerUserIdAndSubjectUserIdAndActiveTrue(anyLong(), anyLong());
    }
}
