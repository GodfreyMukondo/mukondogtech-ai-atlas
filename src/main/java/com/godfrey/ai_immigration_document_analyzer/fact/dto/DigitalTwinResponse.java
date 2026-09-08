package com.godfrey.ai_immigration_document_analyzer.fact.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The Digital Twin read projection: "what does MukondoGTech AI currently
 * know about this case." Computed fresh from source tables on every
 * request (see {@code DigitalTwinProjectionService}) - this response is
 * never itself persisted as a second source of truth.
 */
public record DigitalTwinResponse(
        Long subjectUserId,
        LocalDateTime generatedAt,
        List<FactResponse> currentFacts,
        List<FactConflictResponse> openConflicts,
        String note
) {
}
