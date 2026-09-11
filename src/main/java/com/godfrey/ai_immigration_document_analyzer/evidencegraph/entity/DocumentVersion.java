package com.godfrey.ai_immigration_document_analyzer.evidencegraph.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * DOCUMENT VERSION
 * ============================================================================
 *
 * One immutable, point-in-time state of a {@code Document}'s content and
 * extraction (Evidence Intelligence Graph design, section 5). {@code
 * Document} carries a single {@code extractedText} column overwritten in
 * place on re-processing - there is no version axis today, which makes
 * "what did we know as of date X, from which reading of this document"
 * structurally unanswerable. This entity is additive only: it does not
 * modify {@link com.godfrey.ai_immigration_document_analyzer.entity.Document}
 * in any way.
 *
 * Fully immutable once created - a corrected extraction is a new row, never
 * an edit of this one. {@link #supersededByVersionId} is the only field
 * ever set after creation, and only to point at the version that replaced
 * this one - the historical version itself is never deleted or rewritten.
 * ============================================================================
 */
@Entity
@Table(
        name = "DOCUMENT_VERSIONS",
        indexes = {
                @Index(name = "IDX_DOC_VERSION_DOCUMENT", columnList = "DOCUMENT_ID, VERSION_NUMBER")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "DOCUMENT_ID", nullable = false)
    private Long documentId;

    @Column(name = "VERSION_NUMBER", nullable = false)
    private Integer versionNumber;

    /** e.g. "OCR_V2", "MANUAL_ENTRY". Free text - the extraction pipeline's own vocabulary, not a closed enum here. */
    @Column(name = "EXTRACTION_METHOD", length = 60)
    private String extractionMethod;

    /** A pointer, never a copy of Document.extractedText - avoids duplicating large document content. */
    @Column(name = "EXTRACTED_TEXT_REF", length = 500)
    private String extractedTextRef;

    /** The document's own issue date, independent of any Fact's effectiveFrom it happens to support. */
    @Column(name = "DOCUMENT_ISSUE_DATE")
    private LocalDateTime documentIssueDate;

    /** The document's own expiry date (e.g. a passport's expiry) - distinct from Fact.effectiveTo. */
    @Column(name = "DOCUMENT_EXPIRY_DATE")
    private LocalDateTime documentExpiryDate;

    @Column(name = "SUPERSEDED_BY_VERSION_ID")
    private Long supersededByVersionId;

    @CreationTimestamp
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
