package com.godfrey.ai_immigration_document_analyzer.fact.entity;

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

/**
 * A single "derived from" edge: {@code derivedFactId} (provenanceType =
 * DERIVED_FACT) was reasoned from {@code sourceFactId}. A DERIVED_FACT Fact
 * must have at least one row here - this is what makes an inference
 * explainable/replayable and lets a retraction of the source cascade.
 *
 * Not yet populated by any reasoning process in this stage (Assessment/
 * inference logic is a later stage) - the table exists now so provenance is
 * never destroyed or retrofitted later.
 */
@Entity
@Table(
        name = "FACT_DERIVATIONS",
        indexes = {
                @Index(name = "IDX_FACT_DERIV_DERIVED", columnList = "DERIVED_FACT_ID"),
                @Index(name = "IDX_FACT_DERIV_SOURCE", columnList = "SOURCE_FACT_ID")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FactDerivation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "DERIVED_FACT_ID", nullable = false)
    private Long derivedFactId;

    @Column(name = "SOURCE_FACT_ID", nullable = false)
    private Long sourceFactId;
}
