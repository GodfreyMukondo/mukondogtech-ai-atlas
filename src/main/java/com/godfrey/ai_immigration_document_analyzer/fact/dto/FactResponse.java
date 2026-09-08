package com.godfrey.ai_immigration_document_analyzer.fact.dto;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.Fact;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactCategory;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactConfidenceLevel;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactSensitivityTier;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactStatus;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactValueType;
import com.godfrey.ai_immigration_document_analyzer.fact.entity.VerificationMethod;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Safe, fully-typed projection of a Fact - never a raw entity leaked
 * through the API. Confidence, verification, status and provenance are
 * kept as clearly separate fields, never collapsed into one summary value.
 */
public record FactResponse(
        Long id,
        Long subjectUserId,
        FactCategory category,
        String factKey,
        FactValueType valueType,
        String stringValue,
        LocalDateTime dateValue,
        Double numberValue,
        Boolean booleanValue,
        FactStatus status,
        FactProvenanceType provenanceType,
        FactSensitivityTier sensitivityTier,
        Double confidenceScore,
        FactConfidenceLevel confidenceLevel,
        String confidenceExplanation,
        Boolean isVerified,
        LocalDateTime verifiedAt,
        VerificationMethod verificationMethod,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        LocalDateTime observedAt,
        LocalDateTime lastObservedAt,
        LocalDateTime recordedAt,
        Long supersedesFactId,
        List<FactEvidenceResponse> evidence
) {

    public static FactResponse from(Fact fact, List<FactEvidenceResponse> evidence) {

        return new FactResponse(
                fact.getId(),
                fact.getSubjectUserId(),
                fact.getCategory(),
                fact.getFactKey(),
                fact.getValueType(),
                fact.getStringValue(),
                fact.getDateValue(),
                fact.getNumberValue(),
                fact.getBooleanValue(),
                fact.getStatus(),
                fact.getProvenanceType(),
                fact.getSensitivityTier(),
                fact.getConfidenceScore(),
                fact.getConfidenceLevel(),
                fact.getConfidenceExplanation(),
                fact.getIsVerified(),
                fact.getVerifiedAt(),
                fact.getVerificationMethod(),
                fact.getEffectiveFrom(),
                fact.getEffectiveTo(),
                fact.getObservedAt(),
                fact.getLastObservedAt(),
                fact.getRecordedAt(),
                fact.getSupersedesFactId(),
                evidence
        );
    }
}
