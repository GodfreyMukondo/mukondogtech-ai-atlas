package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.service.rag.ScoredChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Builds structured prompts for the immigration RAG pipeline.
 * Combines user question, retrieved context, chat history,
 * and system instructions into a single LLM-ready prompt.
 */
@Service
@Slf4j
public class PromptBuilderService {

    private static final int MAX_TOKENS = 4000;
    private static final int AVG_CHARS_PER_TOKEN = 4;

    /**
     * Creates the final prompt for the language model.
     */
    public String build(
            String question,
            List<ScoredChunk> chunks,
            List<String> history,
            String role,
            String language,
            String country,
            String visaType
    ) {
        validateQuestion(question);

        List<ScoredChunk> retrievedChunks =
                chunks == null ? Collections.emptyList() : chunks;

        List<String> chatHistory =
                history == null ? Collections.emptyList() : history;

        String systemInstructions =
                buildSystemPrompt(role, country, visaType, language);

        String contextBlock =
                buildContext(retrievedChunks);

        String historyBlock =
                buildHistory(chatHistory);

        String prompt = """
                %s

                ========================================
                CHAT HISTORY:
                %s

                ========================================
                CONTEXT:
                %s

                ========================================
                QUESTION:
                %s

                ========================================
                RESPONSE:
                """.formatted(
                systemInstructions,
                historyBlock,
                contextBlock,
                sanitize(question)
        );

        prompt = truncateByTokenLimit(prompt);

        log.info(
                "Prompt generated successfully | role={} | language={} | country={} | visaType={}",
                role,
                language,
                country,
                visaType
        );

        return prompt;
    }

    /**
     * Builds system instructions based on user role,
     * country, visa type, and preferred language.
     */
    private String buildSystemPrompt(
            String role,
            String country,
            String visaType,
            String language
    ) {
        String normalizedRole = normalize(role);
        String normalizedCountry = normalize(country);
        String normalizedVisaType = normalize(visaType);
        String normalizedLanguage = normalize(language);

        String roleInstructions = switch (normalizedRole) {
            case "lawyer" -> """
                    You are an experienced immigration lawyer AI.
                    Provide legally structured and accurate responses.
                    """;

            case "consultant" -> """
                    You are an immigration consultant AI.
                    Explain processes clearly and simply for applicants.
                    """;

            case "officer" -> """
                    You are an immigration compliance officer AI.
                    Focus on policy, verification, and document compliance.
                    """;

            default -> """
                    You are an expert AI immigration assistant.
                    """;
        };

        return """
                %s

                COUNTRY CONTEXT:
                Use immigration laws and procedures specific to %s.

                VISA CONTEXT:
                Focus on %s visa requirements and eligibility.

                LANGUAGE:
                Respond only in %s.

                RULES:
                - Use only the provided context.
                - Do not make assumptions.
                - Be accurate and structured.
                - If information is missing, clearly state it.
                """.formatted(
                roleInstructions,
                normalizedCountry,
                normalizedVisaType,
                normalizedLanguage
        );
    }

    /**
     * Converts retrieved chunks into a context block.
     */
    private String buildContext(List<ScoredChunk> chunks) {

        if (chunks.isEmpty()) {
            return "No relevant context available.";
        }

        return chunks.stream()
                .map(ScoredChunk::text)
                .map(this::sanitize)
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Converts previous conversation into a history block.
     */
    private String buildHistory(List<String> history) {

        if (history.isEmpty()) {
            return "No previous conversation history.";
        }

        return history.stream()
                .map(this::sanitize)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Rough token-size control.
     * This uses character length as an approximation until
     * a real tokenizer is introduced.
     */
    private String truncateByTokenLimit(String prompt) {

        int estimatedTokens =
                prompt.length() / AVG_CHARS_PER_TOKEN;

        if (estimatedTokens <= MAX_TOKENS) {
            return prompt;
        }

        int maxChars =
                MAX_TOKENS * AVG_CHARS_PER_TOKEN;

        log.warn(
                "Prompt exceeded limit. Truncating from {} chars to {} chars",
                prompt.length(),
                maxChars
        );

        return prompt.substring(0, maxChars);
    }

    /**
     * Cleans text input before prompt insertion.
     */
    private String sanitize(String text) {
        return text == null ? "" : text.trim();
    }

    /**
     * Normalizes text for consistent prompt building.
     */
    private String normalize(String text) {
        return text == null
                ? "unknown"
                : text.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Ensures question input is valid.
     */
    private void validateQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question cannot be empty");
        }
    }
}