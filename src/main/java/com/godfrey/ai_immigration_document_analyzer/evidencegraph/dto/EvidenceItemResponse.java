package com.godfrey.ai_immigration_document_analyzer.evidencegraph.dto;

import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceDirectness;
import com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity.EvidenceItemStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.EvidenceSourceType;

import java.time.LocalDateTime;

/**
 * The evidence detail-panel projection. Deliberately structured as three
 * separate sub-records so the frontend can never accidentally render an
 * AI's interpretation as if it were the original source, or a derived
 * Fact as if it were independently verified (Evidence Intelligence Graph
 * design, section 8; production spec, section 16):
 *
 *   SOURCE DATA           - what the document itself says (§ sourceData)
 *   SYSTEM INTERPRETATION - how/when the system read it (§ systemInterpretation)
 *   DERIVED CONCLUSION    - the Fact this evidence produced, and its own,
 *                           entirely separate confidence/verification state
 *                           (§ derivedConclusion)
 */
public record EvidenceItemResponse(
        Long id,
        EvidenceSourceType sourceType,
        EvidenceItemStatus status,
        EvidenceDirectness directness,
        String rejectionReason,
        SourceData sourceData,
        SystemInterpretation systemInterpretation,
        DerivedConclusion derivedConclusion
) {

    public record SourceData(
            Long documentId,
            String documentFileName,
            Long documentVersionId,
            Integer documentVersionNumber,
            String sourceSnippet,
            LocalDateTime documentIssueDate,
            LocalDateTime documentExpiryDate
    ) {
    }

    public record SystemInterpretation(
            String extractionMethod,
            Double extractionConfidence,
            LocalDateTime extractedAt
    ) {
    }

    public record DerivedConclusion(
            Long factId,
            String factKey,
            String valueSummary,
            Boolean isVerified,
            Double factConfidenceScore
    ) {
    }
}
