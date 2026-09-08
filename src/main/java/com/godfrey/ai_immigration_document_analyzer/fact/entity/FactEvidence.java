package com.godfrey.ai_immigration_document_analyzer.fact.entity;

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
 * The linking record asserting "this source supports this Fact"
 * (Document -&gt; Evidence -&gt; Fact). A relationship/attestation, never a
 * claim itself, and never the Fact's only record of trustworthiness.
 *
 * Deliberately stores only a short snippet, never full document content -
 * data minimization (Fact Model specification, section 10/14).
 */
@Entity
@Table(
        name = "FACT_EVIDENCE",
        indexes = {
                @Index(name = "IDX_FACT_EVIDENCE_FACT", columnList = "FACT_ID"),
                @Index(name = "IDX_FACT_EVIDENCE_DOCUMENT", columnList = "DOCUMENT_ID")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FactEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "FACT_ID", nullable = false)
    private Long factId;

    @Enumerated(EnumType.STRING)
    @Column(name = "SOURCE_TYPE", nullable = false, length = 30)
    private EvidenceSourceType sourceType;

    @Column(name = "DOCUMENT_ID")
    private Long documentId;

    @Column(name = "SOURCE_LOCATOR", length = 255)
    private String sourceLocator;

    /** Short excerpt only - never full document content. */
    @Column(name = "SOURCE_SNIPPET", length = 500)
    private String sourceSnippet;

    @Column(name = "RELIABILITY_NOTE", length = 255)
    private String reliabilityNote;

    @CreationTimestamp
    @Column(name = "CAPTURED_AT", nullable = false, updatable = false)
    private LocalDateTime capturedAt;
}
