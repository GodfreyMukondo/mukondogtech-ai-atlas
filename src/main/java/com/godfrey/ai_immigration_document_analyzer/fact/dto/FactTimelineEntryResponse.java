package com.godfrey.ai_immigration_document_analyzer.fact.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.AccessorType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactTimelineEvent;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.TimelineEventType;

import java.time.LocalDateTime;

public record FactTimelineEntryResponse(
        Long id,
        TimelineEventType eventType,
        Long factId,
        Long conflictId,
        AccessorType actorType,
        Long actorUserId,
        String previousStatus,
        String newStatus,
        String reason,
        LocalDateTime occurredAt
) {

    public static FactTimelineEntryResponse from(FactTimelineEvent event) {

        return new FactTimelineEntryResponse(
                event.getId(),
                event.getEventType(),
                event.getFactId(),
                event.getConflictId(),
                event.getActorType(),
                event.getActorUserId(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getReason(),
                event.getOccurredAt()
        );
    }
}
