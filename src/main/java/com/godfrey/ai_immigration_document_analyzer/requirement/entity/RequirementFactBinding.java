package com.godfrey.ai_immigration_document_analyzer.requirement.entity;

import com.godfrey.ai_immigration_document_analyzer.fact.entity.FactProvenanceType;

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

/**
 * Declares one Fact key a {@code Requirement} can read, and the minimum
 * evidence quality expected of it (Requirement/Pathway Architecture
 * Specification, section 4). This is the requirement's explicit contract
 * with the Fact Foundation - a Requirement never "discovers" relevant Facts
 * at evaluation time, it only ever reads keys declared here.
 *
 * A Fact that exists but fails {@link #requiresVerification}/
 * {@link #minimumProvenanceType} yields INSUFFICIENT_EVIDENCE, never
 * NOT_SATISFIED - see {@code LogicEvaluationService}.
 */
@Entity
@Table(
        name = "REQUIREMENT_FACT_BINDINGS",
        indexes = {
                @Index(name = "IDX_REQ_BINDING_REQUIREMENT", columnList = "REQUIREMENT_ID"),
                @Index(name = "IDX_REQ_BINDING_FACT_KEY", columnList = "FACT_KEY")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RequirementFactBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "REQUIREMENT_ID", nullable = false)
    private Long requirementId;

    @Column(name = "FACT_KEY", nullable = false, length = 150)
    private String factKey;

    @Builder.Default
    @Column(name = "REQUIRES_VERIFICATION", nullable = false)
    private Boolean requiresVerification = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "MINIMUM_PROVENANCE_TYPE", length = 40)
    private FactProvenanceType minimumProvenanceType;
}
