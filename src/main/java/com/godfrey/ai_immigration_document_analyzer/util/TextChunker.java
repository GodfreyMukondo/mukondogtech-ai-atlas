package com.godfrey.ai_immigration_document_analyzer.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Production-ready text chunking utility for RAG / embeddings.
 * Splits large documents into smaller, clean, model-friendly chunks.
 */
public final class TextChunker {

    private TextChunker() {
        // Prevent instantiation
    }

    /**
     * Splits text into fixed-size chunks with basic sentence awareness.
     */
    public static List<String> chunk(String text, int chunkSize) {

        if (text == null || text.isBlank()) {
            return List.of();
        }

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be greater than 0");
        }

        List<String> chunks = new ArrayList<>();

        int length = text.length();
        int start = 0;

        while (start < length) {

            int end = Math.min(start + chunkSize, length);

            if (end < length) {
                int lastPeriod = text.lastIndexOf(".", end);
                int lastNewLine = text.lastIndexOf("\n", end);
                int lastSpace = text.lastIndexOf(" ", end);

                int breakPoint = Math.max(lastPeriod, Math.max(lastNewLine, lastSpace));

                if (breakPoint > start + (chunkSize / 2)) {
                    end = breakPoint + 1;
                }
            }

            String chunk = text.substring(start, end).trim();

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start = end;
        }

        return chunks;
    }

    /**
     * Default chunk size = 1000 characters.
     */
    public static List<String> chunk(String text) {
        return chunk(text, 1000);
    }
}