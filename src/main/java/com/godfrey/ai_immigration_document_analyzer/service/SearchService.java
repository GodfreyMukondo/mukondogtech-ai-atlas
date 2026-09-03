package com.godfrey.ai_immigration_document_analyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.godfrey.ai_immigration_document_analyzer.entity.DocumentEmbedding;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Simple semantic search service.
 * Currently uses in-memory cosine similarity fallback.
 * Future upgrade: Oracle VECTOR_DISTANCE / vector index search.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final EmbeddingService embeddingService;
    private final DocumentEmbeddingRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Searches for similar document embeddings.
     */
    public List<DocumentEmbedding> search(String query) {

        validateQuery(query);

        try {
            log.info("Starting search for query");

            String queryEmbeddingJson =
                    embeddingService.generateEmbedding(query);

            List<Double> queryVector =
                    parseVector(queryEmbeddingJson);

            List<DocumentEmbedding> embeddings =
                    repository.findAll();

            if (embeddings.isEmpty()) {
                log.warn("No embeddings found in database");
                return Collections.emptyList();
            }

            // FUTURE: Replace with Oracle VECTOR_DISTANCE
            List<DocumentEmbedding> results = embeddings.stream()
                    .filter(e -> e.getEmbeddingJson() != null)
                    .sorted((a, b) -> {
                        double scoreA = similarity(queryVector, parseSafe(a.getEmbeddingJson()));
                        double scoreB = similarity(queryVector, parseSafe(b.getEmbeddingJson()));
                        return Double.compare(scoreB, scoreA);
                    })
                    .limit(10)
                    .toList();

            log.info("Search completed with {} results", results.size());

            return results;

        } catch (Exception ex) {
            log.error("Search operation failed", ex);
            throw new RuntimeException("Search failed", ex);
        }
    }

    /**
     * Parse JSON vector safely.
     */
    private List<Double> parseVector(String json) throws Exception {
        return objectMapper.readValue(
                json,
                new TypeReference<List<Double>>() {}
        );
    }

    /**
     * Safe parsing with fallback.
     */
    private List<Double> parseSafe(String json) {
        try {
            return parseVector(json);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Cosine similarity calculation.
     */
    private double similarity(List<Double> a, List<Double> b) {

        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }

        int size = Math.min(a.size(), b.size());

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < size; i++) {
            double x = a.get(i);
            double y = b.get(i);

            dot += x * y;
            normA += x * x;
            normB += y * y;
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Input validation.
     */
    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
    }
}