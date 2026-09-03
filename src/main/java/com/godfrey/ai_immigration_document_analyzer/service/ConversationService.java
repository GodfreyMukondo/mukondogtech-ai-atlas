package com.godfrey.ai_immigration_document_analyzer.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages short-lived conversation history for chat sessions.
 *
 * <p>
 * This implementation is suitable for a single application instance.
 * For multi-instance production deployments, this should be replaced
 * with persistent storage or a distributed cache such as Redis.
 * </p>
 */
@Service
public class ConversationService {

    private static final int DEFAULT_MAX_MESSAGES = 20;

    private final Map<String, List<String>> memory =
            new ConcurrentHashMap<>();

    /**
     * Adds a message to a conversation.
     */
    public void addMessage(
            String sessionId,
            String message
    ) {

        validateSessionId(sessionId);

        if (message == null || message.isBlank()) {
            return;
        }

        memory.compute(
                sessionId,
                (key, existing) -> {

                    List<String> messages =
                            existing != null
                                    ? existing
                                    : new ArrayList<>();

                    messages.add(message);

                    /*
                     * Prevent unbounded memory growth.
                     */
                    int maxMessages =
                            DEFAULT_MAX_MESSAGES;

                    if (messages.size() > maxMessages) {

                        int removeCount =
                                messages.size() - maxMessages;

                        messages.subList(
                                0,
                                removeCount
                        ).clear();
                    }

                    return messages;
                }
        );
    }

    /**
     * Returns conversation history using the default limit.
     */
    public List<String> getHistory(
            String sessionId
    ) {

        return getHistory(
                sessionId,
                DEFAULT_MAX_MESSAGES
        );
    }

    /**
     * Returns the most recent conversation messages.
     */
    public List<String> getHistory(
            String sessionId,
            int maxMessages
    ) {

        validateSessionId(sessionId);

        if (maxMessages <= 0) {
            return Collections.emptyList();
        }

        List<String> messages =
                memory.get(sessionId);

        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }

        int fromIndex =
                Math.max(
                        0,
                        messages.size() - maxMessages
                );

        return List.copyOf(
                messages.subList(
                        fromIndex,
                        messages.size()
                )
        );
    }

    /**
     * Clears a conversation.
     */
    public void clearHistory(
            String sessionId
    ) {

        validateSessionId(sessionId);

        memory.remove(sessionId);
    }

    /**
     * Checks whether a session exists.
     */
    public boolean exists(
            String sessionId
    ) {

        if (sessionId == null ||
                sessionId.isBlank()) {
            return false;
        }

        return memory.containsKey(sessionId);
    }

    /**
     * Returns the number of messages in a session.
     */
    public int getMessageCount(
            String sessionId
    ) {

        if (sessionId == null ||
                sessionId.isBlank()) {
            return 0;
        }

        List<String> messages =
                memory.get(sessionId);

        return messages == null
                ? 0
                : messages.size();
    }

    private void validateSessionId(
            String sessionId
    ) {

        if (sessionId == null ||
                sessionId.isBlank()) {

            throw new IllegalArgumentException(
                    "Session ID cannot be empty"
            );
        }
    }
}