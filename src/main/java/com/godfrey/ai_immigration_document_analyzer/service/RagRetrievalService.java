package com.godfrey.ai_immigration_document_analyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godfrey.ai_immigration_document_analyzer.entity.DocumentEmbedding;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentEmbeddingRepository;
import com.godfrey.ai_immigration_document_analyzer.service.rag.ScoredChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagRetrievalService {

    private static final int TOP_K = 5;

    private final DocumentEmbeddingRepository repository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    /**
     * Retrieves the most relevant document chunks for a question.
     */
    public List<ScoredChunk> retrieve(
            String question
    ) {

        validateQuestion(question);

        try {

            log.info(
                    "Starting RAG retrieval. questionLength={}",
                    question.length()
            );

            /*
             * Generate embedding for the user question.
             */
            String embeddingJson =
                    embeddingService.generateEmbedding(
                            question.trim()
                    );

            if (!StringUtils.hasText(embeddingJson)) {

                log.warn(
                        "Embedding service returned empty embedding"
                );

                return Collections.emptyList();
            }

            List<Double> queryVector =
                    parseEmbedding(embeddingJson);

            validateVector(
                    queryVector,
                    "query"
            );

            /*
             * Retrieve stored document embeddings.
             *
             * NOTE:
             * For very large datasets this should eventually be replaced
             * with database/vector-store similarity search.
             */
            List<DocumentEmbedding> storedEmbeddings =
                    repository.findAll();

            if (storedEmbeddings == null ||
                    storedEmbeddings.isEmpty()) {

                log.info(
                        "No document embeddings found"
                );

                return Collections.emptyList();
            }

            /*
             * Score every stored embedding.
             */
            List<ScoredChunk> results =
                    storedEmbeddings.stream()
                            .map(document ->
                                    scoreDocument(
                                            document,
                                            queryVector
                                    )
                            )
                            .filter(chunk ->
                                    chunk != null &&
                                            StringUtils.hasText(
                                                    chunk.text()
                                            )
                            )
                            .sorted(
                                    Comparator.comparingDouble(
                                            ScoredChunk::score
                                    ).reversed()
                            )
                            .limit(TOP_K)
                            .toList();

            log.info(
                    "RAG retrieval completed. storedEmbeddings={}, results={}",
                    storedEmbeddings.size(),
                    results.size()
            );

            return results;

        } catch (Exception ex) {

            log.error(
                    "RAG retrieval failed",
                    ex
            );

            throw new IllegalStateException(
                    "Failed to retrieve relevant document context",
                    ex
            );
        }
    }

    /**
     * Scores one stored document embedding against the query vector.
     */
    private ScoredChunk scoreDocument(
            DocumentEmbedding document,
            List<Double> queryVector
    ) {

        if (document == null) {
            return null;
        }

        try {

            String embeddingJson =
                    document.getEmbeddingJson();

            if (!StringUtils.hasText(embeddingJson)) {

                log.warn(
                        "Skipping document embedding id={} because embedding is empty",
                        document.getId()
                );

                return null;
            }

            String content =
                    document.getContentChunk();

            if (!StringUtils.hasText(content)) {

                log.warn(
                        "Skipping document embedding id={} because content is empty",
                        document.getId()
                );

                return null;
            }

            List<Double> documentVector =
                    parseEmbedding(embeddingJson);

            validateVector(
                    documentVector,
                    "document"
            );

            if (queryVector.size() !=
                    documentVector.size()) {

                log.warn(
                        "Skipping embedding id={} because vector dimensions differ. query={}, document={}",
                        document.getId(),
                        queryVector.size(),
                        documentVector.size()
                );

                return null;
            }

            double similarity =
                    cosineSimilarity(
                            queryVector,
                            documentVector
                    );

            if (Double.isNaN(similarity) ||
                    Double.isInfinite(similarity)) {

                return null;
            }

            return new ScoredChunk(
                    content.trim(),
                    similarity
            );

        } catch (Exception ex) {

            log.warn(
                    "Failed to process document embedding id={}",
                    document.getId(),
                    ex
            );

            return null;
        }
    }

    /**
     * Converts JSON embedding into a numeric vector.
     */
    private List<Double> parseEmbedding(
            String json
    ) throws Exception {

        List<Double> vector =
                objectMapper.readValue(
                        json,
                        new TypeReference<List<Double>>() {}
                );

        if (vector == null) {
            return Collections.emptyList();
        }

        return vector;
    }

    /**
     * Validates an embedding vector.
     */
    private void validateVector(
            List<Double> vector,
            String vectorName
    ) {

        if (vector == null || vector.isEmpty()) {

            throw new IllegalArgumentException(
                    vectorName + " embedding cannot be empty"
            );
        }

        for (Double value : vector) {

            if (value == null ||
                    Double.isNaN(value) ||
                    Double.isInfinite(value)) {

                throw new IllegalArgumentException(
                        vectorName +
                                " embedding contains an invalid numeric value"
                );
            }
        }
    }

    /**
     * Computes cosine similarity.
     *
     * Both vectors must have the same dimensionality.
     */
    private double cosineSimilarity(
            List<Double> first,
            List<Double> second
    ) {

        if (first == null ||
                second == null ||
                first.isEmpty() ||
                second.isEmpty()) {

            return 0.0;
        }

        if (first.size() != second.size()) {

            throw new IllegalArgumentException(
                    "Embedding dimensions do not match"
            );
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < first.size(); i++) {

            double a = first.get(i);
            double b = second.get(i);

            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA <= 0.0 ||
                normB <= 0.0) {

            return 0.0;
        }

        double denominator =
                Math.sqrt(normA) *
                        Math.sqrt(normB);

        return dotProduct / denominator;
    }

    /**
     * Validates the user question.
     */
    private void validateQuestion(
            String question
    ) {

        if (!StringUtils.hasText(question)) {

            throw new IllegalArgumentException(
                    "Question cannot be empty"
            );
        }

        if (question.trim().length() > 10_000) {

            throw new IllegalArgumentException(
                    "Question exceeds the maximum allowed length"
            );
        }
    }
}