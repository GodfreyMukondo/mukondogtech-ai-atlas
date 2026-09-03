package com.godfrey.ai_immigration_document_analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Vector DB abstraction layer.
 * Replace implementation later with:
 * - Oracle 23ai Vector Search
 * - Pinecone
 * - Milvus
 * - Weaviate
 */
@Service
@Slf4j
public class VectorDbService {

    /**
     * Store embedding in vector database.
     */
    public void store(Long documentId, List<Double> embedding, String text) {

        validate(documentId, embedding);

        try {
            VectorRecord record = new VectorRecord(
                    documentId,
                    embedding,
                    text,
                    LocalDateTime.now()
            );

            // TODO: Replace with real vector DB call
            // oracleVectorClient.insert(record);

            log.info(
                    "Vector stored successfully | documentId={} | dimension={}",
                    documentId,
                    embedding.size()
            );

        } catch (Exception ex) {
            log.error("Failed to store vector for documentId={}", documentId, ex);
            throw new RuntimeException("Vector DB storage failed", ex);
        }
    }

    /**
     * Basic validation for safety in pipelines
     */
    private void validate(Long documentId, List<Double> embedding) {

        if (documentId == null) {
            throw new IllegalArgumentException("documentId cannot be null");
        }

        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalArgumentException("embedding cannot be empty");
        }

        if (embedding.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("embedding contains null values");
        }
    }

    /**
     * Internal record for future vector DB mapping
     */
    private record VectorRecord(
            Long documentId,
            List<Double> embedding,
            String text,
            LocalDateTime createdAt
    ) {}
}