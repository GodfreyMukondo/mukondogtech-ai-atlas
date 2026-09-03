package com.godfrey.ai_immigration_document_analyzer.repository;

import com.godfrey.ai_immigration_document_analyzer.entity.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for document embeddings.
 *
 * Responsibilities:
 * - Persist document embeddings.
 * - Retrieve embeddings for RAG.
 * - Retrieve embeddings belonging to a specific document.
 */
@Repository
public interface DocumentEmbeddingRepository
        extends JpaRepository<DocumentEmbedding, Long> {

    /**
     * Finds all embeddings belonging to a document.
     *
     * @param documentId document identifier
     * @return document embeddings
     */
    List<DocumentEmbedding> findByDocumentId(Long documentId);
}