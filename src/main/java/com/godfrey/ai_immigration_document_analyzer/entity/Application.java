package com.godfrey.ai_immigration_document_analyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;


/**
 * ============================================================================
 * APPLICATION ENTITY
 * ============================================================================
 *
 * An immigration application submitted by an authenticated user, together
 * with the personal details required for administrative review.
 *
 * Supporting documents are uploaded separately through the existing
 * {@code /api/documents/upload} endpoint and linked to this application via
 * {@link Document#getApplicationId()}.
 * ============================================================================
 */
@Entity
@Table(
        name = "APPLICATIONS",
        indexes = {

                @Index(
                        name = "IDX_APPLICATION_USER_ID",
                        columnList = "USER_ID"
                ),

                @Index(
                        name = "IDX_APPLICATION_STATUS",
                        columnList = "STATUS"
                ),

                @Index(
                        name = "IDX_APPLICATION_SUBMITTED_AT",
                        columnList = "SUBMITTED_AT"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(
        onlyExplicitlyIncluded = true
)
public class Application {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(
            name = "ID",
            nullable = false
    )
    private Long id;


    @Column(
            name = "USER_ID",
            nullable = false
    )
    private Long userId;


    @Column(
            name = "FULL_NAME",
            nullable = false,
            length = 255
    )
    private String fullName;


    @Column(
            name = "EMAIL",
            nullable = false,
            length = 255
    )
    private String email;


    @Column(
            name = "PHONE",
            length = 50
    )
    private String phone;


    @Column(
            name = "DATE_OF_BIRTH"
    )
    private LocalDate dateOfBirth;


    @Column(
            name = "COUNTRY",
            nullable = false,
            length = 150
    )
    private String country;


    @Column(
            name = "VISA_TYPE",
            nullable = false,
            length = 150
    )
    private String visaType;


    @Column(
            name = "NOTES",
            length = 2000
    )
    private String notes;


    @Builder.Default
    @Column(
            name = "STATUS",
            nullable = false,
            length = 30
    )
    private String status = "PENDING";


    @Builder.Default
    @Column(
            name = "RISK_LEVEL",
            nullable = false,
            length = 20
    )
    private String riskLevel = "LOW";


    @Column(
            name = "AI_CONFIDENCE",
            columnDefinition = "NUMBER(5,2)"
    )
    private Double aiConfidence;


    @Column(
            name = "REJECTION_REASON",
            length = 1000
    )
    private String rejectionReason;


    @Column(
            name = "SUBMITTED_AT",
            nullable = false,
            updatable = false
    )
    private LocalDateTime submittedAt;


    @Column(
            name = "UPDATED_AT"
    )
    private LocalDateTime updatedAt;


    // =========================================================================
    // CREATE
    // =========================================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();


        if (submittedAt == null) {

            submittedAt = now;
        }


        if (updatedAt == null) {

            updatedAt = now;
        }


        if (
                status == null ||
                        status.isBlank()
        ) {

            status = "PENDING";
        }


        if (
                riskLevel == null ||
                        riskLevel.isBlank()
        ) {

            riskLevel = "LOW";
        }


        normalizeFields();
    }


    // =========================================================================
    // UPDATE
    // =========================================================================

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();


        normalizeFields();
    }


    // =========================================================================
    // NORMALIZATION
    // =========================================================================

    private void normalizeFields() {

        if (fullName != null) {

            fullName =
                    fullName.trim();
        }


        if (email != null) {

            email =
                    email
                            .trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );
        }


        if (country != null) {

            country =
                    country.trim();
        }


        if (visaType != null) {

            visaType =
                    visaType.trim();
        }


        if (status != null) {

            status =
                    status
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            );
        }


        if (riskLevel != null) {

            riskLevel =
                    riskLevel
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            );
        }
    }
}
