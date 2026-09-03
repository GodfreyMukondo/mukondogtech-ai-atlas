package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.entity.DocumentEmbedding;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentEmbeddingRepository;
import com.godfrey.ai_immigration_document_analyzer.util.TextChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for generating and storing document embeddings.
 * Used by the RAG pipeline for semantic search and retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentEmbeddingService {

    private static final int DEFAULT_CHUNK_SIZE = 1000;

    private final EmbeddingService embeddingService;
    private final DocumentEmbeddingRepository repository;

    /**
     * Generates embeddings for a document and stores them.
     *
     * @param documentId document ID
     * @param text full extracted document text
     */
    @Transactional
    public void createEmbeddings(Long documentId, String text) {

        validateInput(documentId, text);

        log.info("Starting embedding generation for documentId={}", documentId);

        List<String> chunks = TextChunker.chunk(text, DEFAULT_CHUNK_SIZE);

        if (chunks.isEmpty()) {
            log.warn("No chunks generated for documentId={}", documentId);
            return;
        }

        List<DocumentEmbedding> embeddings = new ArrayList<>();

        for (String chunk : chunks) {
            try {
                String vector = embeddingService.generateEmbedding(chunk);

                if (vector == null || vector.isBlank()) {
                    log.warn("Skipping empty embedding for documentId={}", documentId);
                    continue;
                }

                DocumentEmbedding embedding = DocumentEmbedding.builder()
                        .documentId(documentId)
                        .contentChunk(chunk)
                        .embeddingJson(vector)
                        .build();

                embeddings.add(embedding);

            } catch (Exception ex) {
                log.error(
                        "Failed generating embedding for documentId={} chunk={}",
                        documentId,
                        chunk,
                        ex
                );
            }
        }

        if (!embeddings.isEmpty()) {
            repository.saveAll(embeddings);
            log.info(
                    "Successfully saved {} embeddings for documentId={}",
                    embeddings.size(),
                    documentId
            );
        } else {
            log.warn("No embeddings were saved for documentId={}", documentId);
        }
    }

    /**
     * Validates input before processing.
     */
    private void validateInput(Long documentId, String text) {
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("Invalid documentId");
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Document text cannot be empty");
        }
    }
}