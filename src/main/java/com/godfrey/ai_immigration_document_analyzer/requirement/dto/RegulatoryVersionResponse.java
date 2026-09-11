package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatorySourceType;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVersion;

import java.time.LocalDateTime;

/** Catalogue projection of a RegulatoryVersion for admin listing/selection when authoring Requirements. */
public record RegulatoryVersionResponse(
        Long id,
        String regulationIdentity,
        String jurisdiction,
        RegulatorySourceType sourceType,
        String sourceAuthority,
        String sourceReference,
        LocalDateTime publicationDate,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        Long supersedesVersionId,
        Long supersededByVersionId,
        RegulatoryVerificationStatus verificationStatus,
        String changeSummary
) {

    public static RegulatoryVersionResponse from(RegulatoryVersion version) {

        return new RegulatoryVersionResponse(
                version.getId(),
                version.getRegulationIdentity(),
                version.getJurisdiction(),
                version.getSourceType(),
                version.getSourceAuthority(),
                version.getSourceReference(),
                version.getPublicationDate(),
                version.getEffectiveFrom(),
                version.getEffectiveTo(),
                version.getSupersedesVersionId(),
                version.getSupersededByVersionId(),
                version.getVerificationStatus(),
                version.getChangeSummary()
        );
    }
}
