package com.godfrey.ai_immigration_document_analyzer.caseintelligence.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CaseTimelineResponse(
        Long subjectUserId,
        List<CaseTimelineEventResponse> events,
        LocalDateTime generatedAt,
        String note
) {
}
