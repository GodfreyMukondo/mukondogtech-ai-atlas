package com.godfrey.ai_immigration_document_analyzer.requirement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * REGULATORY VERSION
 * ============================================================================
 *
 * One point-in-time snapshot of a regulation (Requirement/Pathway
 * Architecture Specification, section 6). A regulation that changes (e.g. a
 * 2026 vs 2027 minimum salary) never rewrites its prior version in place -
 * it produces a NEW RegulatoryVersion row, closes the old one's
 * {@link #effectiveTo}, and links {@link #supersedesVersionId}. Every
 * {@code Requirement} row is pinned to exactly one RegulatoryVersion, so a
 * historical Requirement/RequirementEvaluation is never silently rewritten
 * either - see {@code TemporalFactResolver} for how evaluation reads the
 * correct version for a past assessment date.
 * ============================================================================
 */
@Entity
@Table(
        name = "REGULATORY_VERSIONS",
        indexes = {
                @Index(name = "IDX_REG_VERSION_REGULATION", columnList = "REGULATION_IDENTITY"),
                @Index(name = "IDX_REG_VERSION_JURISDICTION", columnList = "JURISDICTION"),
                @Index(name = "IDX_REG_VERSION_EFFECTIVE", columnList = "EFFECTIVE_FROM, EFFECTIVE_TO")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RegulatoryVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    /** Stable identity of the regulation itself, shared across every version of it. */
    @Column(name = "REGULATION_IDENTITY", nullable = false, length = 150)
    private String regulationIdentity;

    @Column(name = "JURISDICTION", nullable = false, length = 100)
    private String jurisdiction;

    @Enumerated(EnumType.STRING)
    @Column(name = "SOURCE_TYPE", nullable = false, length = 40)
    private RegulatorySourceType sourceType;

    @Column(name = "SOURCE_AUTHORITY", nullable = false, length = 255)
    private String sourceAuthority;

    @Column(name = "SOURCE_REFERENCE", length = 500)
    private String sourceReference;

    @Column(name = "PUBLICATION_DATE")
    private LocalDateTime publicationDate;

    /** When MukondoGTech AI ingested this version - distinct from when the regulator published it. */
    @Column(name = "RETRIEVAL_DATE")
    private LocalDateTime retrievalDate;

    @Column(name = "EFFECTIVE_FROM")
    private LocalDateTime effectiveFrom;

    /** Null = currently in force. */
    @Column(name = "EFFECTIVE_TO")
    private LocalDateTime effectiveTo;

    @Column(name = "SUPERSEDES_VERSION_ID")
    private Long supersedesVersionId;

    @Column(name = "SUPERSEDED_BY_VERSION_ID")
    private Long supersededByVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "VERIFICATION_STATUS", nullable = false, length = 30)
    private RegulatoryVerificationStatus verificationStatus;

    @Column(name = "CHANGE_SUMMARY", length = 1000)
    private String changeSummary;

    @CreationTimestamp
    @Column(name = "RECORDED_AT", nullable = false, updatable = false)
    private LocalDateTime recordedAt;
}
