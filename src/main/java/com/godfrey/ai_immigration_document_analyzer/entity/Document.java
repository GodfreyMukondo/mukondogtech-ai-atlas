package com.godfrey.ai_immigration_document_analyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Locale;


/**
 * ============================================================================
 * DOCUMENT ENTITY
 * ============================================================================
 */
@Entity
@Table(
        name = "DOCUMENTS",
        indexes = {

                @Index(
                        name = "IDX_DOCUMENT_USER_ID",
                        columnList = "USER_ID"
                ),

                @Index(
                        name = "IDX_DOCUMENT_TYPE",
                        columnList = "DOCUMENT_TYPE"
                ),

                @Index(
                        name = "IDX_DOCUMENT_STATUS",
                        columnList = "UPLOAD_STATUS"
                ),

                @Index(
                        name = "IDX_DOCUMENT_UPLOADED_AT",
                        columnList = "UPLOADED_AT"
                ),

                @Index(
                        name = "IDX_DOCUMENT_FRAUD",
                        columnList = "FRAUD_DETECTED"
                ),

                @Index(
                        name = "IDX_DOCUMENT_RISK",
                        columnList = "RISK_LEVEL"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(
        exclude = {
                "extractedText",
                "summary"
        }
)
@EqualsAndHashCode(
        onlyExplicitlyIncluded = true
)
public class Document {


    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "document_seq"
    )
    @SequenceGenerator(
            name = "document_seq",
            sequenceName = "DOCUMENT_SEQ",
            allocationSize = 1
    )
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
            name = "DOCUMENT_TYPE",
            nullable = false,
            length = 100
    )
    private String documentType;


    @Column(
            name = "FILE_NAME",
            nullable = false,
            length = 255
    )
    private String fileName;


    /**
     * S3 object key.
     *
     * Example:
     *
     * documents/25/uuid.pdf
     *
     * This field is internal and should not be exposed through the API DTO.
     */
    @Column(
            name = "FILE_PATH",
            nullable = false,
            unique = true,
            length = 500
    )
    private String filePath;


    @Column(
            name = "FILE_SIZE",
            nullable = false
    )
    private Long fileSize;


    @Column(
            name = "MIME_TYPE",
            length = 100
    )
    private String mimeType;


    @Lob
    @Column(
            name = "EXTRACTED_TEXT"
    )
    private String extractedText;


    @Lob
    @Column(
            name = "SUMMARY"
    )
    private String summary;


    @Builder.Default
    @Column(
            name = "FRAUD_DETECTED",
            nullable = false
    )
    private Boolean fraudDetected = false;


    @Builder.Default
    @Column(
            name = "RISK_LEVEL",
            nullable = false,
            length = 20
    )
    private String riskLevel = "LOW";


    @Builder.Default
    @Column(
            name = "UPLOAD_STATUS",
            nullable = false,
            length = 30
    )
    private String uploadStatus = "PENDING";


    @Column(
            name = "UPLOADED_AT",
            nullable = false,
            updatable = false
    )
    private LocalDateTime uploadedAt;


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


        if (uploadedAt == null) {

            uploadedAt = now;
        }


        if (updatedAt == null) {

            updatedAt = now;
        }


        if (fraudDetected == null) {

            fraudDetected = false;
        }


        if (
                riskLevel == null ||
                        riskLevel.isBlank()
        ) {

            riskLevel = "LOW";
        }


        if (
                uploadStatus == null ||
                        uploadStatus.isBlank()
        ) {

            uploadStatus = "PENDING";
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


        if (fraudDetected == null) {

            fraudDetected = false;
        }


        normalizeFields();
    }


    // =========================================================================
    // NORMALIZATION
    // =========================================================================

    private void normalizeFields() {

        if (documentType != null) {

            documentType =
                    documentType.trim();
        }


        if (uploadStatus != null) {

            uploadStatus =
                    uploadStatus
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


        if (fileName != null) {

            fileName =
                    fileName.trim();
        }


        if (mimeType != null) {

            mimeType =
                    mimeType
                            .trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );
        }


        if (filePath != null) {

            filePath =
                    filePath.trim();
        }
    }
}