package com.godfrey.ai_immigration_document_analyzer.exception;

/**

 * Thrown when a user attempts to access a resource
 * without proper authentication or authorization.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("You are not authorized to perform this action.");
    }

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
