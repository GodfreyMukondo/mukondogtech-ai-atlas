package com.godfrey.ai_immigration_document_analyzer.fact.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.EvidenceSourceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactEvidence;

import java.time.LocalDateTime;

public record FactEvidenceResponse(
        Long id,
        EvidenceSourceType sourceType,
        Long documentId,
        String sourceLocator,
        String sourceSnippet,
        LocalDateTime capturedAt
) {

    public static FactEvidenceResponse from(FactEvidence evidence) {

        return new FactEvidenceResponse(
                evidence.getId(),
                evidence.getSourceType(),
                evidence.getDocumentId(),
                evidence.getSourceLocator(),
                evidence.getSourceSnippet(),
                evidence.getCapturedAt()
        );
    }
}
