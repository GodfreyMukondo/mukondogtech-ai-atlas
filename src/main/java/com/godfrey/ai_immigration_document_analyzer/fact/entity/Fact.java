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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * FACT
 * ============================================================================
 *
 * The atomic, provenance-tagged, temporally-scoped unit of knowledge the
 * MukondoGTech AI Fact Model is built on (Fact Model Architecture
 * Specification, sections 1-21; policy validation turns).
 *
 * A Fact is a claim the system holds, never automatically the truth - its
 * standing is expressed entirely through {@link #provenanceType},
 * {@link #confidenceScore}/{@link #confidenceLevel}, and {@link #status},
 * never through its mere existence.
 *
 * IMMUTABILITY: the value/provenance/source columns are never mutated after
 * creation. "Updating" a Fact means creating a new row and pointing
 * {@link #supersedesFactId} at the one it replaces (see
 * {@code FactLifecycleService}). The two documented exceptions are: closing
 * an open-ended {@link #effectiveTo}, and the verification/status overlay
 * fields, both of which record when something stopped being true or was
 * corroborated - not a change to what was claimed.
 *
 * Strongly typed by design: exactly one of {@link #stringValue},
 * {@link #dateValue}, {@link #numberValue}, {@link #booleanValue} is
 * populated, selected by {@link #valueType} - deliberately not a generic
 * JSON payload.
 * ============================================================================
 */
@Entity
@Table(
        name = "FACTS",
        indexes = {
                @Index(name = "IDX_FACT_SUBJECT", columnList = "SUBJECT_USER_ID"),
                @Index(name = "IDX_FACT_SUBJECT_KEY", columnList = "SUBJECT_USER_ID, FACT_KEY"),
                @Index(name = "IDX_FACT_STATUS", columnList = "STATUS"),
                @Index(name = "IDX_FACT_CATEGORY", columnList = "CATEGORY"),
                @Index(name = "IDX_FACT_SUPERSEDES", columnList = "SUPERSEDES_FACT_ID")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Fact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    /** The person this Fact is about. Facts are subject-scoped, never pathway/application-scoped. */
    @Column(name = "SUBJECT_USER_ID", nullable = false)
    private Long subjectUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "CATEGORY", nullable = false, length = 40)
    private FactCategory category;

    /** Fine-grained predicate identifier, e.g. "EMPLOYMENT.CURRENT_EMPLOYER". Validated against FactTypeRegistry. */
    @Column(name = "FACT_KEY", nullable = false, length = 150)
    private String factKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "VALUE_TYPE", nullable = false, length = 20)
    private FactValueType valueType;

    @Column(name = "STRING_VALUE", length = 2000)
    private String stringValue;

    @Column(name = "DATE_VALUE")
    private LocalDateTime dateValue;

    @Column(name = "NUMBER_VALUE", columnDefinition = "NUMBER(19,4)")
    private Double numberValue;

    @Column(name = "BOOLEAN_VALUE")
    private Boolean booleanValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 30)
    private FactStatus status;

    // =========================================================================
    // PROVENANCE
    // =========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "PROVENANCE_TYPE", nullable = false, length = 40)
    private FactProvenanceType provenanceType;

    /** Who actually performed the creating action - distinct from provenanceType (how the claim originated). */
    @Enumerated(EnumType.STRING)
    @Column(name = "CREATED_BY_ACCESSOR_TYPE", nullable = false, length = 20)
    private AccessorType createdByAccessorType;

    @Column(name = "CREATED_BY_USER_ID")
    private Long createdByUserId;

    @Column(name = "SOURCE_DOCUMENT_ID")
    private Long sourceDocumentId;

    @Column(name = "SOURCE_LOCATOR", length = 255)
    private String sourceLocator;

    /** Short excerpt only - data minimization. Never the full document content. */
    @Column(name = "SOURCE_SNIPPET", length = 500)
    private String sourceSnippet;

    /** Free-text origin note for non-document provenance (e.g. "self-reported via profile form"). */
    @Column(name = "SOURCE_DESCRIPTION", length = 500)
    private String sourceDescription;

    // =========================================================================
    // SENSITIVITY
    // =========================================================================

    @Enumerated(EnumType.STRING)
    @Column(name = "SENSITIVITY_TIER", nullable = false, length = 30)
    private FactSensitivityTier sensitivityTier;

    // =========================================================================
    // CONFIDENCE (kept structurally distinct from verification/status)
    // =========================================================================

    @Column(name = "CONFIDENCE_SCORE", nullable = false, columnDefinition = "NUMBER(5,4)")
    private Double confidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "CONFIDENCE_LEVEL", nullable = false, length = 20)
    private FactConfidenceLevel confidenceLevel;

    @Column(name = "CONFIDENCE_EXPLANATION", length = 500)
    private String confidenceExplanation;

    // =========================================================================
    // VERIFICATION OVERLAY (never the same as "confidence" or "accepted")
    // =========================================================================

    @Builder.Default
    @Column(name = "IS_VERIFIED", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "VERIFIED_AT")
    private LocalDateTime verifiedAt;

    @Column(name = "VERIFIED_BY_USER_ID")
    private Long verifiedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "VERIFICATION_METHOD", length = 40)
    private VerificationMethod verificationMethod;

    @Column(name = "VERIFICATION_NOTES", length = 500)
    private String verificationNotes;

    // =========================================================================
    // TEMPORAL
    // =========================================================================

    /** When the claim became/was true in the real world. Null = unknown/not applicable. */
    @Column(name = "EFFECTIVE_FROM")
    private LocalDateTime effectiveFrom;

    /** Null = open-ended (current). The one column ever closed post-creation without being a "value change". */
    @Column(name = "EFFECTIVE_TO")
    private LocalDateTime effectiveTo;

    @Column(name = "OBSERVED_AT", nullable = false)
    private LocalDateTime observedAt;

    @Column(name = "LAST_OBSERVED_AT", nullable = false)
    private LocalDateTime lastObservedAt;

    @CreationTimestamp
    @Column(name = "RECORDED_AT", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @UpdateTimestamp
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    // =========================================================================
    // SUPERSESSION / TERMINAL REASONS
    // =========================================================================

    @Column(name = "SUPERSEDES_FACT_ID")
    private Long supersedesFactId;

    @Column(name = "RETRACTION_REASON", length = 500)
    private String retractionReason;

    @Column(name = "REJECTION_REASON", length = 500)
    private String rejectionReason;
}
