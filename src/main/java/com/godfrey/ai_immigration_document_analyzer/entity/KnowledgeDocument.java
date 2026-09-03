package com.godfrey.ai_immigration_document_analyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * KNOWLEDGE DOCUMENT ENTITY
 * ============================================================================
 *
 * Stores immigration knowledge-base articles and policy documents.
 *
 * Oracle table:
 *
 *     KNOWLEDGE_DOCUMENTS
 *
 * Oracle sequence:
 *
 *     KNOWLEDGE_DOCUMENTS_SEQ
 *
 * Used by:
 *
 *     - Admin Knowledge Base
 *     - AI/RAG indexing
 *     - Immigration policy management
 *     - Compliance review
 *     - Knowledge search
 *
 * ============================================================================
 */
@Entity
@Table(name = "KNOWLEDGE_DOCUMENTS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {

    /**
     * =========================================================================
     * PRIMARY KEY
     * =========================================================================
     *
     * Oracle:
     *
     *     ID NUMBER(19,0)
     *
     * Uses the existing Oracle sequence:
     *
     *     KNOWLEDGE_DOCUMENTS_SEQ
     *
     * IMPORTANT:
     *
     * Do NOT use GenerationType.IDENTITY here because the Oracle table
     * was created using a sequence rather than an identity column.
     */
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "knowledge_document_seq"
    )
    @SequenceGenerator(
            name = "knowledge_document_seq",
            sequenceName = "KNOWLEDGE_DOCUMENTS_SEQ",
            allocationSize = 1
    )
    @Column(
            name = "ID",
            nullable = false,
            updatable = false
    )
    private Long id;


    /**
     * =========================================================================
     * DOCUMENT TITLE
     * =========================================================================
     *
     * Oracle:
     *
     *     TITLE VARCHAR2(500 CHAR) NOT NULL
     */
    @Column(
            name = "TITLE",
            nullable = false,
            length = 500
    )
    private String title;


    /**
     * =========================================================================
     * CATEGORY
     * =========================================================================
     *
     * Examples:
     *
     *     Work Visa
     *     Student Visa
     *     Residency
     *     Compliance
     *     Family Visa
     *     Visitor Visa
     *
     * Oracle:
     *
     *     CATEGORY VARCHAR2(150 CHAR) NOT NULL
     */
    @Column(
            name = "CATEGORY",
            nullable = false,
            length = 150
    )
    private String category;


    /**
     * =========================================================================
     * COUNTRY
     * =========================================================================
     *
     * Oracle:
     *
     *     COUNTRY VARCHAR2(150 CHAR) NOT NULL
     */
    @Column(
            name = "COUNTRY",
            nullable = false,
            length = 150
    )
    private String country;


    /**
     * =========================================================================
     * DOCUMENT STATUS
     * =========================================================================
     *
     * Supported database values:
     *
     *     DRAFT
     *     REVIEW
     *     PUBLISHED
     *
     * Oracle:
     *
     *     STATUS VARCHAR2(30 CHAR) DEFAULT 'DRAFT' NOT NULL
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "STATUS",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private KnowledgeDocumentStatus status =
            KnowledgeDocumentStatus.DRAFT;


    /**
     * =========================================================================
     * DOCUMENT VERSION
     * =========================================================================
     *
     * IMPORTANT:
     *
     * The Oracle table column is:
     *
     *     VERSION
     *
     * NOT:
     *
     *     VERSION_NUMBER
     *
     * Oracle:
     *
     *     VERSION VARCHAR2(50 CHAR) NOT NULL
     */
    @Column(
            name = "VERSION",
            nullable = false,
            length = 50
    )
    @Builder.Default
    private String version = "1.0";


    /**
     * =========================================================================
     * DOCUMENT CONTENT
     * =========================================================================
     *
     * Stored as Oracle CLOB.
     *
     * This contains the actual immigration knowledge content that can
     * later be processed by:
     *
     *     - RAG
     *     - embeddings
     *     - vector databases
     *     - LangChain
     *     - IBM watsonx
     *     - AI search
     */
    @Lob
    @Column(
            name = "CONTENT",
            columnDefinition = "CLOB"
    )
    private String content;


    /**
     * =========================================================================
     * SOURCE URL
     * =========================================================================
     *
     * Optional official immigration source.
     *
     * Oracle:
     *
     *     SOURCE_URL VARCHAR2(2000 CHAR)
     */
    @Column(
            name = "SOURCE_URL",
            length = 2000
    )
    private String sourceUrl;


    /**
     * =========================================================================
     * AI INDEX STATUS
     * =========================================================================
     *
     * Oracle:
     *
     *     AI_INDEXED NUMBER(1,0) DEFAULT 0 NOT NULL
     *
     * Java:
     *
     *     Boolean
     *
     * Hibernate maps Boolean to the Oracle numeric representation.
     */
    @Column(
            name = "AI_INDEXED",
            nullable = false
    )
    @Builder.Default
    private Boolean aiIndexed = false;


    /**
     * =========================================================================
     * AI INDEXED AT
     * =========================================================================
     *
     * Stores the time at which the document was successfully indexed.
     *
     * Oracle:
     *
     *     AI_INDEXED_AT TIMESTAMP(6)
     */
    @Column(
            name = "AI_INDEXED_AT"
    )
    private LocalDateTime aiIndexedAt;


    /**
     * =========================================================================
     * CREATED BY
     * =========================================================================
     *
     * Stores the username/email of the user who created the document.
     *
     * Oracle:
     *
     *     CREATED_BY VARCHAR2(255 CHAR) NOT NULL
     */
    @Column(
            name = "CREATED_BY",
            nullable = false,
            length = 255
    )
    private String createdBy;


    /**
     * =========================================================================
     * UPDATED BY
     * =========================================================================
     *
     * Stores the username/email of the user who last modified the document.
     *
     * Oracle:
     *
     *     UPDATED_BY VARCHAR2(255 CHAR) NOT NULL
     */
    @Column(
            name = "UPDATED_BY",
            nullable = false,
            length = 255
    )
    private String updatedBy;


    /**
     * =========================================================================
     * CREATED AT
     * =========================================================================
     *
     * Oracle:
     *
     *     CREATED_AT TIMESTAMP(6)
     *     DEFAULT SYSTIMESTAMP
     *     NOT NULL
     */
    @Column(
            name = "CREATED_AT",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    /**
     * =========================================================================
     * UPDATED AT
     * =========================================================================
     *
     * Oracle:
     *
     *     UPDATED_AT TIMESTAMP(6)
     *     DEFAULT SYSTIMESTAMP
     *     NOT NULL
     */
    @Column(
            name = "UPDATED_AT",
            nullable = false
    )
    private LocalDateTime updatedAt;


    /**
     * =========================================================================
     * PRE-PERSIST
     * =========================================================================
     *
     * Initializes application-managed defaults before inserting a new
     * knowledge document.
     */
    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();


        /*
         * Creation timestamp.
         */
        if (createdAt == null) {
            createdAt = now;
        }


        /*
         * Modification timestamp.
         */
        if (updatedAt == null) {
            updatedAt = now;
        }


        /*
         * AI indexing defaults to false.
         */
        if (aiIndexed == null) {
            aiIndexed = false;
        }


        /*
         * New documents default to DRAFT.
         */
        if (status == null) {
            status = KnowledgeDocumentStatus.DRAFT;
        }


        /*
         * New documents default to version 1.0.
         */
        if (version == null || version.isBlank()) {
            version = "1.0";
        }
    }


    /**
     * =========================================================================
     * PRE-UPDATE
     * =========================================================================
     *
     * Automatically updates the modification timestamp whenever the
     * entity is updated.
     */
    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}

