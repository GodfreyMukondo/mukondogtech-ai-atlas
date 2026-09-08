package com.godfrey.ai_immigration_document_analyzer.fact.service;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactTimelineEvent;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.TimelineEventType;
import com.godfrey.ai_immigration_document_analyzer.fact.repository.FactTimelineEventRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * FACT TIMELINE SERVICE
 * ============================================================================
 *
 * Appends transactional audit events. This is the one place in the Fact
 * foundation that deliberately does NOT follow this codebase's existing
 * "best-effort side-effect" convention (used correctly for
 * {@code NotificationService}, where losing a notification is low stakes).
 *
 * A dropped timeline entry would silently produce an incomplete audit
 * trail, which undermines the entire auditability promise of the Fact
 * model. Every method here therefore does NOT catch or swallow exceptions:
 * a failure to record propagates and rolls back the surrounding
 * {@code @Transactional} boundary (owned by the caller - typically
 * {@code FactService}/{@code FactConflictService}), so the Fact/Conflict
 * state change and its timeline entry are always committed together or not
 * at all.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
public class FactTimelineService {

    private final FactTimelineEventRepository timelineEventRepository;

    public void record(
            Long subjectUserId,
            TimelineEventType eventType,
            Long factId,
            Long conflictId,
            AccessorType actorType,
            Long actorUserId,
            String previousStatus,
            String newStatus,
            String reason
    ) {

        FactTimelineEvent event = FactTimelineEvent.builder()
                .subjectUserId(subjectUserId)
                .eventType(eventType)
                .factId(factId)
                .conflictId(conflictId)
                .actorType(actorType)
                .actorUserId(actorUserId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(reason)
                .build();

        // Intentionally no try/catch: see class-level note on transactional
        // reliability. Do not wrap this in a best-effort pattern.
        timelineEventRepository.save(event);
    }
}
