package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.CaseAssignmentRepository;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * FACT AUTHORIZATION SERVICE
 * ============================================================================
 *
 * The single retrieval-boundary enforcement point for the Fact foundation.
 * {@code FactService} and {@code DigitalTwinProjectionService} call this
 * BEFORE touching any repository - an unauthorized request never reaches
 * {@code FactRepository}.
 *
 * CRITICAL, deliberate rule (Fact Sensitivity / Access-Control Matrix,
 * approved specification): holding {@code Role.ADMIN} alone grants NO
 * access to Fact data. A System Administrator may administer the platform
 * without automatically being authorized to inspect sensitive case
 * information. Only the subject themselves, or a CASE_WORKER holding an
 * active {@link com.godfrey.ai_immigration_document_analyzer.fact.entity.CaseAssignment}
 * for that subject, may view/create/verify/resolve conflicts for a case.
 *
 * One narrow exception, deliberately kept as its OWN grant rather than
 * folded into the CASE_WORKER-only resolve path: the subject may confirm
 * which side of their OWN open conflict they believe is correct (see
 * {@link #assertCanApplicantConfirmConflict}) - a self-reported provenance
 * statement, never verification, and never interchangeable with staff
 * resolution.
 *
 * Every decision - granted or denied - is recorded via
 * {@link FactAccessAuditService}, satisfying "record security-sensitive
 * Fact access" and providing the evidence base for IDOR testing.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FactAuthorizationService {

    private final CaseAssignmentRepository caseAssignmentRepository;
    private final FactAccessAuditService accessAuditService;

    // =========================================================================
    // VIEW
    // =========================================================================

    public void assertCanView(
            AuthenticatedUser actor,
            Long subjectUserId,
            FactSensitivityTier tier,
            Long factId,
            String purpose
    ) {

        AccessorType accessorType = resolveAccessorType(actor, subjectUserId);

        boolean granted = accessorType == AccessorType.SUBJECT
                || (accessorType == AccessorType.CASE_WORKER && hasActiveAssignment(actor, subjectUserId));

        audit(subjectUserId, factId, actor, accessorType, purpose, tier, granted);

        if (!granted) {

            throw new AccessDeniedException(
                    "You are not authorized to view this information."
            );
        }
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    public void assertCanCreate(
            AuthenticatedUser actor,
            Long subjectUserId,
            FactSensitivityTier tier,
            String purpose
    ) {

        AccessorType accessorType = resolveAccessorType(actor, subjectUserId);

        boolean granted = accessorType == AccessorType.SUBJECT
                || (accessorType == AccessorType.CASE_WORKER && hasActiveAssignment(actor, subjectUserId));

        audit(subjectUserId, null, actor, accessorType, purpose, tier, granted);

        if (!granted) {

            throw new AccessDeniedException(
                    "You are not authorized to record information for this case."
            );
        }
    }

    // =========================================================================
    // VERIFY / RESOLVE CONFLICT
    //
    // Deliberately excludes SUBJECT: no self-verification, and a subject may
    // not adjudicate their own conflicting evidence.
    // =========================================================================

    public void assertCanVerifyOrResolve(
            AuthenticatedUser actor,
            Long subjectUserId,
            FactSensitivityTier tier,
            Long factId,
            String purpose
    ) {

        AccessorType accessorType = resolveAccessorType(actor, subjectUserId);

        boolean granted = accessorType == AccessorType.CASE_WORKER
                && hasActiveAssignment(actor, subjectUserId);

        audit(subjectUserId, factId, actor, accessorType, purpose, tier, granted);

        if (!granted) {

            throw new AccessDeniedException(
                    "Only an authorized case worker assigned to this case may perform this action."
            );
        }
    }

    // =========================================================================
    // APPLICANT CONFLICT CONFIRMATION
    //
    // A DISTINCT grant from assertCanVerifyOrResolve, never a path into it.
    // The applicant may confirm which of two competing values they believe
    // is correct for their OWN case - this is self-reported provenance, not
    // verification, and it is never available to a CASE_WORKER or ADMIN
    // through this method. A case worker/administrator's own resolution
    // authority is entirely unaffected and continues to flow exclusively
    // through assertCanVerifyOrResolve above.
    // =========================================================================

    public void assertCanApplicantConfirmConflict(
            AuthenticatedUser actor,
            Long subjectUserId,
            FactSensitivityTier tier,
            Long factId,
            String purpose
    ) {

        AccessorType accessorType = resolveAccessorType(actor, subjectUserId);

        boolean granted = accessorType == AccessorType.SUBJECT;

        audit(subjectUserId, factId, actor, accessorType, purpose, tier, granted);

        if (!granted) {

            throw new AccessDeniedException(
                    "Only the applicant may confirm their own conflict."
            );
        }
    }

    // =========================================================================
    // ACCESSOR RESOLUTION
    // =========================================================================

    private AccessorType resolveAccessorType(AuthenticatedUser actor, Long subjectUserId) {

        if (actor == null || actor.getUserId() == null) {

            throw new AccessDeniedException(
                    "Authenticated user could not be determined."
            );
        }

        if (actor.getUserId().equals(subjectUserId)) {
            return AccessorType.SUBJECT;
        }

        if (hasAuthority(actor, Role.CASE_WORKER)) {
            return AccessorType.CASE_WORKER;
        }

        if (hasAuthority(actor, Role.ADMIN)) {
            return AccessorType.ADMINISTRATOR;
        }

        return AccessorType.SYSTEM;
    }

    private boolean hasAuthority(AuthenticatedUser actor, Role role) {

        String authority = role.getAuthority();

        for (GrantedAuthority granted : actor.getAuthorities()) {

            if (authority.equals(granted.getAuthority())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasActiveAssignment(AuthenticatedUser actor, Long subjectUserId) {

        return caseAssignmentRepository
                .existsByCaseWorkerUserIdAndSubjectUserIdAndActiveTrue(
                        actor.getUserId(),
                        subjectUserId
                );
    }

    private void audit(
            Long subjectUserId,
            Long factId,
            AuthenticatedUser actor,
            AccessorType accessorType,
            String purpose,
            FactSensitivityTier tier,
            boolean granted
    ) {

        accessAuditService.recordAccess(
                subjectUserId,
                factId,
                actor != null ? actor.getUserId() : null,
                accessorType,
                purpose,
                tier,
                granted,
                granted ? null : "Accessor type " + accessorType + " is not authorized for this subject/tier."
        );

        if (!granted) {

            log.warn(
                    "Fact access denied | subjectUserId={} | accessorType={} | tier={} | purpose={}",
                    subjectUserId,
                    accessorType,
                    tier,
                    purpose
            );
        }
    }
}
