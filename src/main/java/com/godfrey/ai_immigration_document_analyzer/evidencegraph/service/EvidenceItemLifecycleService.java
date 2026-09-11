package com.godfrey.ai_immigration_document_analyzer.evidencegraph.service;

import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItem;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItemStatus;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.repository.EvidenceItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * EVIDENCE ITEM LIFECYCLE SERVICE
 * ============================================================================
 *
 * Enforces the EvidenceItem lifecycle state machine (Evidence Intelligence
 * Graph design, section 12) - the only place an EvidenceItem's
 * {@code status} is ever changed, exactly mirroring how
 * {@code FactLifecycleService} is the only place a Fact's status changes.
 *
 * No public API accepts a raw status value from a caller. In particular,
 * {@code CANDIDATE -> LINKED} is only ever reached as a side effect of the
 * Fact it supports being accepted (never a directly callable transition),
 * and {@code -> REJECTED} is restricted to a human, case-worker/
 * administrator-authorized caller - never the applicant, mirroring
 * {@code FactAuthorizationService.assertCanVerifyOrResolve}'s exclusion of
 * self-adjudication. The caller (an orchestrating service) is responsible
 * for enforcing that authorization boundary before invoking
 * {@link #reject(EvidenceItem, String)} - this class enforces the state
 * machine, not who may drive it, exactly like {@code FactLifecycleService}.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceItemLifecycleService {

    private final EvidenceItemRepository evidenceItemRepository;

    private static final Map<EvidenceItemStatus, Set<EvidenceItemStatus>> ALLOWED_TRANSITIONS = buildTransitions();

    // =========================================================================
    // TRANSITION GUARD
    // =========================================================================

    public void assertValidTransition(EvidenceItemStatus from, EvidenceItemStatus to) {

        Set<EvidenceItemStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(EvidenceItemStatus.class));

        if (!allowed.contains(to)) {

            throw new IllegalStateException(
                    "Invalid EvidenceItem status transition: " + from + " -> " + to
            );
        }
    }

    // =========================================================================
    // GUARDED TRANSITIONS
    // =========================================================================

    public EvidenceItem transition(EvidenceItem item, EvidenceItemStatus newStatus) {

        assertValidTransition(item.getStatus(), newStatus);

        EvidenceItemStatus previous = item.getStatus();
        item.setStatus(newStatus);

        EvidenceItem saved = evidenceItemRepository.save(item);

        log.info(
                "EvidenceItem transitioned | evidenceItemId={} | {} -> {}",
                item.getId(), previous, newStatus
        );

        return saved;
    }

    /**
     * Case worker/administrator only - the caller must have already
     * enforced that authorization boundary. Never reachable by the
     * applicant.
     */
    public EvidenceItem reject(EvidenceItem item, String reason) {

        EvidenceItem rejected = transition(item, EvidenceItemStatus.REJECTED);
        rejected.setRejectionReason(reason);

        return evidenceItemRepository.save(rejected);
    }

    // =========================================================================
    // STATE MACHINE DEFINITION
    // =========================================================================

    private static Map<EvidenceItemStatus, Set<EvidenceItemStatus>> buildTransitions() {

        Map<EvidenceItemStatus, Set<EvidenceItemStatus>> transitions = new EnumMap<>(EvidenceItemStatus.class);

        transitions.put(EvidenceItemStatus.DISCOVERED, EnumSet.of(
                EvidenceItemStatus.EXTRACTED
        ));

        transitions.put(EvidenceItemStatus.EXTRACTED, EnumSet.of(
                EvidenceItemStatus.VALIDATION_FAILED,
                EvidenceItemStatus.CANDIDATE
        ));

        transitions.put(EvidenceItemStatus.VALIDATION_FAILED, EnumSet.noneOf(EvidenceItemStatus.class));

        transitions.put(EvidenceItemStatus.CANDIDATE, EnumSet.of(
                EvidenceItemStatus.LINKED,
                EvidenceItemStatus.REJECTED
        ));

        transitions.put(EvidenceItemStatus.LINKED, EnumSet.of(
                EvidenceItemStatus.SUPERSEDED,
                EvidenceItemStatus.REJECTED
        ));

        transitions.put(EvidenceItemStatus.SUPERSEDED, EnumSet.noneOf(EvidenceItemStatus.class));

        transitions.put(EvidenceItemStatus.REJECTED, EnumSet.noneOf(EvidenceItemStatus.class));

        return transitions;
    }
}
