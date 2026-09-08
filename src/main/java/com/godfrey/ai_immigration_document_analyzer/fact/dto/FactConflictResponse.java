package com.godfrey.ai_immigration_document_analyzer.fact.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.ConflictResolutionType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.ConflictStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConflict;

import java.time.LocalDateTime;

public record FactConflictResponse(
        Long id,
        Long subjectUserId,
        String factKey,
        Long factAId,
        Long factBId,
        ConflictStatus status,
        LocalDateTime detectedAt,
        LocalDateTime resolvedAt,
        ConflictResolutionType resolutionType,
        Long winningFactId,
        String resolutionNotes
) {

    public static FactConflictResponse from(FactConflict conflict) {

        return new FactConflictResponse(
                conflict.getId(),
                conflict.getSubjectUserId(),
                conflict.getFactKey(),
                conflict.getFactAId(),
                conflict.getFactBId(),
                conflict.getStatus(),
                conflict.getDetectedAt(),
                conflict.getResolvedAt(),
                conflict.getResolutionType(),
                conflict.getWinningFactId(),
                conflict.getResolutionNotes()
        );
    }
}
