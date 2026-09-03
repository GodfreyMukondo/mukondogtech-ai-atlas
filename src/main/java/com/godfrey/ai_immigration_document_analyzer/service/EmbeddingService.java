package com.godfrey.ai_immigration_document_analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Production-ready embedding service.
 *
 * Responsibilities:
 * - Generate embeddings using Spring AI.
 * - Validate input.
 * - Validate generated vectors.
 * - Convert float vectors to Double vectors.
 * - Serialize embeddings to JSON.
 * - Deserialize embeddings when required by workers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;

    /**
     * Generates an embedding and returns it as JSON.
     *
     * @param text text to embed
     * @return embedding represented as JSON array
     */
    public String generateEmbedding(String text) {

        validateInput(text);

        final String normalizedText = text.trim();

        try {

            log.debug(
                    "Generating embedding. inputLength={}",
                    normalizedText.length()
            );

            float[] vector =
                    embeddingModel.embed(normalizedText);

            validateVector(vector);

            List<Double> convertedVector =
                    convertToDoubleList(vector);

            String embeddingJson =
                    objectMapper.writeValueAsString(convertedVector);

            log.debug(
                    "Embedding generated successfully. dimensions={}",
                    convertedVector.size()
            );

            return embeddingJson;

        } catch (Exception ex) {

            log.error(
                    "Embedding generation failed. inputLength={}",
                    normalizedText.length(),
                    ex
            );

            throw new IllegalStateException(
                    "Embedding generation failed",
                    ex
            );
        }
    }

    /**
     * Generates an embedding as a list of doubles.
     *
     * This method is useful for RAG workers and similarity calculations.
     */
    public List<Double> embed(String text) {

        validateInput(text);

        try {

            float[] vector =
                    embeddingModel.embed(text.trim());

            validateVector(vector);

            return convertToDoubleList(vector);

        } catch (Exception ex) {

            log.error(
                    "Failed to generate embedding vector. inputLength={}",
                    text.trim().length(),
                    ex
            );

            throw new IllegalStateException(
                    "Failed to generate embedding vector",
                    ex
            );
        }
    }

    /**
     * Parses an embedding JSON string.
     */
    public List<Double> parseEmbedding(String embeddingJson) {

        if (!StringUtils.hasText(embeddingJson)) {
            throw new IllegalArgumentException(
                    "Embedding JSON cannot be empty"
            );
        }

        try {

            List<Double> vector =
                    objectMapper.readValue(
                            embeddingJson,
                            new TypeReference<List<Double>>() {}
                    );

            validateVector(vector);

            return vector;

        } catch (Exception ex) {

            log.error(
                    "Failed to parse embedding JSON",
                    ex
            );

            throw new IllegalStateException(
                    "Invalid embedding JSON",
                    ex
            );
        }
    }

    /**
     * Converts a float array into a Double list.
     */
    private List<Double> convertToDoubleList(
            float[] vector
    ) {

        List<Double> result =
                new ArrayList<>(vector.length);

        for (float value : vector) {

            if (Float.isNaN(value) ||
                    Float.isInfinite(value)) {

                throw new IllegalStateException(
                        "Embedding contains an invalid numeric value"
                );
            }

            result.add((double) value);
        }

        return result;
    }

    /**
     * Validates generated embedding vector.
     */
    private void validateVector(float[] vector) {

        if (vector == null || vector.length == 0) {

            throw new IllegalStateException(
                    "Embedding model returned an empty vector"
            );
        }

        for (float value : vector) {

            if (Float.isNaN(value) ||
                    Float.isInfinite(value)) {

                throw new IllegalStateException(
                        "Embedding model returned an invalid vector"
                );
            }
        }
    }

    /**
     * Validates parsed embedding vector.
     */
    private void validateVector(List<Double> vector) {

        if (vector == null || vector.isEmpty()) {

            throw new IllegalArgumentException(
                    "Embedding vector cannot be empty"
            );
        }

        for (Double value : vector) {

            if (value == null ||
                    value.isNaN() ||
                    value.isInfinite()) {

                throw new IllegalArgumentException(
                        "Embedding vector contains an invalid value"
                );
            }
        }
    }

    /**
     * Validates text input.
     */
    private void validateInput(String text) {

        if (!StringUtils.hasText(text)) {

            throw new IllegalArgumentException(
                    "Text cannot be empty for embedding generation"
            );
        }

        if (text.trim().length() > 20_000) {

            throw new IllegalArgumentException(
                    "Text is too long for embedding generation"
            );
        }
    }
}