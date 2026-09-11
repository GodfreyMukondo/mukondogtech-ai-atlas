package com.godfrey.ai_immigration_document_analyzer.requirement.dto;

import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatorySourceType;
import com.godfrey.ai_immigration_document_analyzer.requirement.entity.RegulatoryVerificationStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Admin request to record a new {@code RegulatoryVersion}. Append-only by
 * design - there is no update endpoint: a regulation that changes is a new
 * row, never an in-place edit (see {@code RegulatoryVersion}'s own
 * Javadoc). When a version already exists for the same
 * {@code regulationIdentity}/{@code jurisdiction}, the newly created row's
 * {@code supersedesVersionId} is set automatically and the prior version's
 * {@code supersededByVersionId}/{@code effectiveTo} are updated - see
 * {@code RegulatoryVersionAdminService}.
 */
@Data
public class RegulatoryVersionCreateRequest {

    @NotBlank(message = "Regulation identity is required")
    @Size(max = 150)
    private String regulationIdentity;

    @NotBlank(message = "Jurisdiction is required")
    @Size(max = 100)
    private String jurisdiction;

    @NotNull(message = "Source type is required")
    private RegulatorySourceType sourceType;

    @NotBlank(message = "Source authority is required")
    @Size(max = 255)
    private String sourceAuthority;

    @Size(max = 500)
    private String sourceReference;

    private LocalDateTime publicationDate;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    @NotNull(message = "Verification status is required")
    private RegulatoryVerificationStatus verificationStatus;

    @Size(max = 1000)
    private String changeSummary;
}
