package com.godfrey.ai_immigration_document_analyzer.exception;

/**

 * Thrown when request validation fails
 * or business rules are violated.
 */
public class ValidationException extends RuntimeException {

    private final String field;

    public ValidationException(String message) {
        super(message);
        this.field = null;
    }

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
    }

    /**

     * Returns the field that caused validation failure.
     */
    public String getField() {
        return field;
    }
}
