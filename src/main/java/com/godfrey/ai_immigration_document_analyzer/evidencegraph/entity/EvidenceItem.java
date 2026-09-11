package com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.EvidenceSourceType;

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
 * EVIDENCE ITEM
 * ============================================================================
 *
 * One piece of evidentiary material as a first-class, lifecycle-bearing
 * object (Evidence Intelligence Graph design, section 5) - independent of
 * whether it has (yet) produced an accepted Fact. {@code FactEvidence}
 * remains the Document&lt;-&gt;Fact linkage row this system already had; a row
 * here is an optional ENRICHMENT of it (see {@code FactEvidence.evidenceItemId}),
 * never a replacement, and never a second Fact-linkage mechanism.
 *
 * CRITICAL, non-negotiable invariants carried over from the Fact
 * Foundation:
 *
 *   - Nothing here ever sets or implies {@code Fact.isVerified}. That
 *     overlay is set exclusively by
 *     {@code FactLifecycleService.verify(...)}, a human, case-worker-
 *     authorized action. An EvidenceItem's own {@link #extractionConfidence}
 *     is a completely separate axis from Fact-level confidence and from
 *     verification.
 *   - Nothing here ever records a "fraud" determination.
 *     {@code Document.fraudDetected}/{@code riskLevel} remain the only,
 *     explicitly non-forensic, legacy signal of that kind - a caller MAY
 *     consider it when setting {@link #extractionConfidence}, but it is
 *     never copied, aliased, or exposed as evidence "authenticity proof"
 *     anywhere on this entity.
 *   - The extracted value/snippet is immutable once created - a corrected
 *     extraction is a new row against a new {@link DocumentVersion}, never
 *     an edit of this one.
 * ============================================================================
 */
@Entity
@Table(
        name = "EVIDENCE_ITEMS",
        indexes = {
                @Index(name = "IDX_EVIDENCE_ITEM_DOC_VERSION", columnList = "DOCUMENT_VERSION_ID"),
                @Index(name = "IDX_EVIDENCE_ITEM_STATUS", columnList = "STATUS")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EvidenceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    /** Null for non-document evidence (e.g. a user statement recorded without a document). */
    @Column(name = "DOCUMENT_VERSION_ID")
    private Long documentVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "SOURCE_TYPE", nullable = false, length = 30)
    private EvidenceSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private EvidenceItemStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "DIRECTNESS", length = 20)
    private EvidenceDirectness directness;

    /**
     * AI-reported extraction confidence, if produced by an automated
     * pipeline. Distinct from, and never propagated directly into,
     * {@code Fact.confidenceScore} - see {@code ConfidenceCalculator}'s
     * existing base score for {@code DOCUMENT_EXTRACTION} provenance, which
     * this value may inform but never override.
     */
    @Column(name = "EXTRACTION_CONFIDENCE", columnDefinition = "NUMBER(5,4)")
    private Double extractionConfidence;

    /** Short excerpt only - same data-minimization discipline as FactEvidence.sourceSnippet. */
    @Column(name = "SOURCE_SNIPPET", length = 500)
    private String sourceSnippet;

    @CreationTimestamp
    @Column(name = "EXTRACTED_AT", nullable = false, updatable = false)
    private LocalDateTime extractedAt;

    /** Set only on a human REJECTED transition - see EvidenceItemLifecycleService. */
    @Column(name = "REJECTION_REASON", length = 500)
    private String rejectionReason;
}
