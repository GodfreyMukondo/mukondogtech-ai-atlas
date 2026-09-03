package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.dto.response.ChatResponse;
import com.godfrey.ai_immigration_document_analyzer.service.rag.ScoredChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Production-ready chat orchestration service.
 *
 * Pipeline:
 *
 * User Question
 *      ↓
 * Session Resolution
 *      ↓
 * Conversation History
 *      ↓
 * RAG Retrieval
 *      ↓
 * Prompt Construction
 *      ↓
 * LLM
 *      ↓
 * Conversation Persistence
 *      ↓
 * ChatResponse
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final int MAX_HISTORY_MESSAGES = 20;

    private static final String SYSTEM_PROMPT = """
            You are an AI immigration assistant.

            Your responsibilities:
            - Answer immigration-related questions accurately and clearly.
            - Use retrieved document context when available.
            - Use conversation history when relevant.
            - Never invent immigration laws, regulations, requirements,
              document information, application outcomes, or government
              decisions.
            - Clearly distinguish between information found in uploaded
              documents and general information.
            - If the available information is insufficient, say so clearly.
            - Do not claim to be a lawyer, immigration officer, or government
              representative.
            - Provide concise, professional, useful answers.
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            Answer the user's question using the supplied document context
            and conversation history.

            Prioritize relevant retrieved document information.

            Do not make unsupported assumptions.

            If the available information is insufficient to answer the
            question, explain what information is missing.

            User question:
            %s
            """;

    private static final String MODEL = "gpt-4o-mini";

    private static final String TEMPERATURE = "0.2";

    private final RagRetrievalService retrievalService;
    private final PromptBuilderService promptBuilder;
    private final LlmService llmService;
    private final ConversationService conversationService;

    /**
     * Processes a chat request.
     *
     * @param question user question
     * @param sessionId optional conversation session
     * @return generated chat response
     */
    public ChatResponse ask(
            String question,
            String sessionId
    ) {

        validateQuestion(question);

        final String normalizedQuestion =
                question.trim();

        final String resolvedSessionId =
                resolveSessionId(sessionId);

        log.info(
                "Processing chat request. sessionId={}, questionLength={}",
                resolvedSessionId,
                normalizedQuestion.length()
        );

        /*
         * Store the user message.
         */
        conversationService.addMessage(
                resolvedSessionId,
                "USER: " + normalizedQuestion
        );

        /*
         * RAG retrieval is intentionally isolated.
         *
         * A RAG/database/embedding failure should not prevent
         * the general-purpose AI assistant from answering.
         */
        List<ScoredChunk> chunks =
                retrieveSafely(normalizedQuestion);

        /*
         * Get limited conversation history.
         */
        List<String> history =
                getHistorySafely(resolvedSessionId);

        /*
         * Build the final prompt.
         */
        final String prompt;

        try {

            prompt = promptBuilder.build(
                    normalizedQuestion,
                    chunks,
                    history,
                    SYSTEM_PROMPT,
                    USER_PROMPT_TEMPLATE,
                    MODEL,
                    TEMPERATURE
            );

        } catch (Exception ex) {

            log.error(
                    "Prompt construction failed. sessionId={}",
                    resolvedSessionId,
                    ex
            );

            throw new IllegalStateException(
                    "Failed to construct AI prompt",
                    ex
            );
        }

        if (!StringUtils.hasText(prompt)) {

            throw new IllegalStateException(
                    "Prompt builder returned an empty prompt"
            );
        }

        log.debug(
                "Prompt constructed successfully. sessionId={}, chunks={}, historyMessages={}",
                resolvedSessionId,
                chunks.size(),
                history.size()
        );

        /*
         * LLM call.
         *
         * We deliberately do not swallow this exception.
         * If the OpenAI/Spring AI configuration is broken,
         * the API should report a proper server error.
         */
        final String answer;

        try {

            answer = llmService.ask(prompt);

        } catch (Exception ex) {

            log.error(
                    "LLM request failed. sessionId={}",
                    resolvedSessionId,
                    ex
            );

            throw new IllegalStateException(
                    "AI service is currently unavailable",
                    ex
            );
        }

        final String normalizedAnswer =
                normalizeAnswer(answer);

        /*
         * Store AI response.
         */
        conversationService.addMessage(
                resolvedSessionId,
                "AI: " + normalizedAnswer
        );

        /*
         * Calculate retrieval confidence.
         */
        final double confidence =
                calculateConfidence(chunks);

        /*
         * Extract source text.
         */
        final List<String> sources =
                extractSources(chunks);

        ChatResponse response =
                ChatResponse.builder()
                        .answer(normalizedAnswer)
                        .confidence(confidence)
                        .sources(sources)
                        .sessionId(resolvedSessionId)
                        .build();

        log.info(
                "Chat request completed successfully. " +
                        "sessionId={}, sources={}, confidence={}",
                resolvedSessionId,
                sources.size(),
                confidence
        );

        return response;
    }

    /**
     * Performs RAG retrieval safely.
     *
     * Retrieval failure does not prevent a general AI response.
     */
    private List<ScoredChunk> retrieveSafely(
            String question
    ) {

        try {

            List<ScoredChunk> chunks =
                    retrievalService.retrieve(question);

            if (chunks == null ||
                    chunks.isEmpty()) {

                log.info(
                        "No relevant RAG chunks found."
                );

                return Collections.emptyList();
            }

            return chunks;

        } catch (Exception ex) {

            log.warn(
                    "RAG retrieval failed. " +
                            "Continuing without document context.",
                    ex
            );

            return Collections.emptyList();
        }
    }

    /**
     * Retrieves conversation history safely.
     */
    private List<String> getHistorySafely(
            String sessionId
    ) {

        try {

            List<String> history =
                    conversationService.getHistory(
                            sessionId,
                            MAX_HISTORY_MESSAGES
                    );

            if (history == null ||
                    history.isEmpty()) {

                return Collections.emptyList();
            }

            return history;

        } catch (Exception ex) {

            log.warn(
                    "Conversation history retrieval failed. " +
                            "Continuing without history. sessionId={}",
                    sessionId,
                    ex
            );

            return Collections.emptyList();
        }
    }

    /**
     * Resolves or creates a session ID.
     */
    private String resolveSessionId(
            String sessionId
    ) {

        if (StringUtils.hasText(sessionId)) {

            return sessionId.trim();
        }

        String generatedSessionId =
                UUID.randomUUID().toString();

        log.info(
                "Generated new chat sessionId={}",
                generatedSessionId
        );

        return generatedSessionId;
    }

    /**
     * Normalizes LLM output.
     */
    private String normalizeAnswer(
            String answer
    ) {

        if (!StringUtils.hasText(answer)) {

            log.warn(
                    "LLM returned an empty response"
            );

            return "I could not generate a response at this time. " +
                    "Please try again.";
        }

        return answer.trim();
    }

    /**
     * Extracts valid source text.
     */
    private List<String> extractSources(
            List<ScoredChunk> chunks
    ) {

        if (chunks == null ||
                chunks.isEmpty()) {

            return Collections.emptyList();
        }

        return chunks.stream()
                .filter(chunk -> chunk != null)
                .map(ScoredChunk::text)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * Calculates safe confidence.
     */
    private double calculateConfidence(
            List<ScoredChunk> chunks
    ) {

        if (chunks == null ||
                chunks.isEmpty()) {

            return 0.0;
        }

        ScoredChunk bestChunk =
                chunks.get(0);

        if (bestChunk == null) {

            return 0.0;
        }

        double score =
                bestChunk.score();

        if (Double.isNaN(score) ||
                Double.isInfinite(score)) {

            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(1.0, score)
        );
    }

    /**
     * Validates question input.
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