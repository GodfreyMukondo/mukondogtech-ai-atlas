package com.godfrey.ai_immigration_document_analyzer.exception;

/**
 * ============================================================================
 * RESOURCE NOT FOUND EXCEPTION
 * ============================================================================
 *
 * Indicates that a requested application resource does not exist or is not
 * accessible within the current security scope.
 *
 * Typical examples:
 *
 * - User not found
 * - Document not found
 * - Application not found
 * - Payment not found
 * - Fraud record not found
 *
 * HTTP mapping:
 *
 *     404 NOT FOUND
 *
 * Security note:
 *
 * Services should prefer generic messages such as "Document not found." when
 * returning resource errors to prevent leaking whether another user's
 * resource exists.
 *
 * ============================================================================
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a resource-not-found exception.
     *
     * @param message safe descriptive message
     */
    public ResourceNotFoundException(
            String message
    ) {
        super(message);
    }

    /**
     * Creates a resource-not-found exception with a cause.
     *
     * @param message safe descriptive message
     * @param cause original exception
     */
    public ResourceNotFoundException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}

