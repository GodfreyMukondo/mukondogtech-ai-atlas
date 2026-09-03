package com.godfrey.ai_immigration_document_analyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "visa_rules",
        indexes = {
                @Index(
                        name = "idx_country",
                        columnList = "country"
                ),
                @Index(
                        name = "idx_visa_type",
                        columnList = "visa_type"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_country_visa_type",
                        columnNames = {
                                "country",
                                "visa_type"
                        }
                )
        }
)
@Getter
@Setter
@ToString
public class VisaRule {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(
            name = "country",
            nullable = false,
            length = 100
    )
    private String country;

    @NotBlank
    @Column(
            name = "visa_type",
            nullable = false,
            length = 100
    )
    private String visaType;

    @NotBlank
    @Lob
    @Column(
            name = "requirements",
            nullable = false
    )
    private String requirements;

    @Column(
            name = "processing_time",
            length = 100
    )
    private String processingTime;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;


}
