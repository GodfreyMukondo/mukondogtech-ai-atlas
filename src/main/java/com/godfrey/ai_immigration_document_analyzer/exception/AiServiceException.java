package com.godfrey.ai_immigration_document_analyzer.exception;

/**
 * ============================================================================
 * AI SERVICE EXCEPTION
 * ============================================================================
 *
 * Represents a failure while communicating with or processing a response from
 * an external/internal AI service.
 *
 * This exception should be thrown by LlmService or other AI integration
 * services when an AI dependency is unavailable or fails.
 *
 * ============================================================================
 */
public class AiServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AiServiceException(
            String message
    ) {
        super(message);
    }

    public AiServiceException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}

