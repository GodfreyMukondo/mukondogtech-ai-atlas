package com.godfrey.ai_immigration_document_analyzer.service.rag;

public record ScoredChunk(
        String text,
        double score
) {}
