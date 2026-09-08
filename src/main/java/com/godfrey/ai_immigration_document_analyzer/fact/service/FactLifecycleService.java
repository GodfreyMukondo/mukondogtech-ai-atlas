package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.TimelineEventType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.VerificationMethod;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================================
 * FACT LIFECYCLE SERVICE
 * ============================================================================
 *
 * Enforces the approved Fact lifecycle state machine (Table F). This is the
 * only place a Fact's {@code status} is ever changed.
 *
 * No public API accepts a raw status value from a caller: there is no
 * "set status = ACCEPTED" endpoint. ACCEPTED is only ever reached as the
 * internal result of {@code FactService.proposeFact(...)} completing its
 * conflict check, and VERIFIED is only ever reached via
 * {@link #verify(Fact, Long, VerificationMethod, String)}, which itself is
 * only reachable through {@code FactService.verifyFact(...)} after
 * {@code FactAuthorizationService.assertCanVerifyOrResolve(...)} has
 * already run. No arbitrary caller can move a Fact into either state
 * directly.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class FactLifecycleService {

    private final FactRepository factRepository;
    private final FactTimelineService timelineService;

    private static final Map<FactStatus, Set<FactStatus>> ALLOWED_TRANSITIONS = buildTransitions();

    // =========================================================================
    // TRANSITION GUARD
    // =========================================================================

    public void assertValidTransition(FactStatus from, FactStatus to) {

        Set<FactStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(FactStatus.class));

        if (!allowed.contains(to)) {

            throw new IllegalStateException(
                    "Invalid Fact status transition: " + from + " -> " + to
            );
        }
    }

    // =========================================================================
    // GUARDED TRANSITIONS
    // =========================================================================

    public Fact transition(
            Fact fact,
            FactStatus newStatus,
            AccessorType actorType,
            Long actorUserId,
            TimelineEventType eventType,
            String reason
    ) {

        assertValidTransition(fact.getStatus(), newStatus);

        String previousStatus = fact.getStatus().name();

        fact.setStatus(newStatus);

        if (newStatus == FactStatus.REJECTED) {
            fact.setRejectionReason(reason);
        }

        if (newStatus == FactStatus.RETRACTED) {
            fact.setRetractionReason(reason);
        }

        Fact saved = factRepository.save(fact);

        timelineService.record(
                fact.getSubjectUserId(),
                eventType,
                fact.getId(),
                null,
                actorType,
                actorUserId,
                previousStatus,
                newStatus.name(),
                reason
        );

        return saved;
    }

    /**
     * Closes an open-ended {@code effectiveTo} on a superseded Fact and
     * links it to its successor. This is the one documented exception to
     * "a Fact is never mutated after creation" - it records when something
     * stopped being true, not a change to what was claimed.
     */
    public Fact supersede(Fact oldFact, Fact newFact, AccessorType actorType, Long actorUserId) {

        assertValidTransition(oldFact.getStatus(), FactStatus.SUPERSEDED);

        oldFact.setEffectiveTo(newFact.getEffectiveFrom() != null ? newFact.getEffectiveFrom() : LocalDateTime.now());
        oldFact.setStatus(FactStatus.SUPERSEDED);

        Fact savedOld = factRepository.save(oldFact);

        timelineService.record(
                oldFact.getSubjectUserId(),
                TimelineEventType.FACT_SUPERSEDED,
                oldFact.getId(),
                null,
                actorType,
                actorUserId,
                FactStatus.ACCEPTED.name(),
                FactStatus.SUPERSEDED.name(),
                "Superseded by fact " + newFact.getId()
        );

        return savedOld;
    }

    /**
     * Verification is an overlay, never a status - see class-level note.
     * Cannot verify a Fact that is not currently ACCEPTED.
     */
    public Fact verify(
            Fact fact,
            Long verifiedByUserId,
            VerificationMethod method,
            String notes
    ) {

        if (fact.getStatus() != FactStatus.ACCEPTED) {

            throw new IllegalStateException(
                    "Only an ACCEPTED fact may be verified. Current status: " + fact.getStatus()
            );
        }

        fact.setIsVerified(true);
        fact.setVerifiedAt(LocalDateTime.now());
        fact.setVerifiedByUserId(verifiedByUserId);
        fact.setVerificationMethod(method);
        fact.setVerificationNotes(notes);

        Fact saved = factRepository.save(fact);

        timelineService.record(
                fact.getSubjectUserId(),
                TimelineEventType.FACT_VERIFIED,
                fact.getId(),
                null,
                com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType.CASE_WORKER,
                verifiedByUserId,
                FactStatus.ACCEPTED.name(),
                FactStatus.ACCEPTED.name(),
                notes
        );

        return saved;
    }

    // =========================================================================
    // STATE MACHINE DEFINITION
    // =========================================================================

    private static Map<FactStatus, Set<FactStatus>> buildTransitions() {

        Map<FactStatus, Set<FactStatus>> transitions = new EnumMap<>(FactStatus.class);

        transitions.put(FactStatus.PROPOSED, EnumSet.of(
                FactStatus.VALIDATION_FAILED,
                FactStatus.SUSPICIOUS,
                FactStatus.ACCEPTED,
                FactStatus.CONTESTED,
                FactStatus.REJECTED
        ));

        transitions.put(FactStatus.VALIDATION_FAILED, EnumSet.noneOf(FactStatus.class));

        transitions.put(FactStatus.SUSPICIOUS, EnumSet.of(FactStatus.PENDING_REVIEW));

        transitions.put(FactStatus.PENDING_REVIEW, EnumSet.of(
                FactStatus.PROPOSED,
                FactStatus.REJECTED
        ));

        transitions.put(FactStatus.CONTESTED, EnumSet.of(
                FactStatus.ACCEPTED,
                FactStatus.REJECTED
        ));

        transitions.put(FactStatus.ACCEPTED, EnumSet.of(
                FactStatus.SUPERSEDED,
                FactStatus.RETRACTED,
                FactStatus.CONTESTED
        ));

        transitions.put(FactStatus.REJECTED, EnumSet.noneOf(FactStatus.class));

        transitions.put(FactStatus.SUPERSEDED, EnumSet.noneOf(FactStatus.class));

        transitions.put(FactStatus.RETRACTED, EnumSet.noneOf(FactStatus.class));

        return transitions;
    }
}
