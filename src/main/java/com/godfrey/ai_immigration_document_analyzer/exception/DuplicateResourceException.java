package com.godfrey.ai_immigration_document_analyzer.exception;

/**
 * ============================================================
 * DUPLICATE RESOURCE EXCEPTION
 * ============================================================
 *
 * Custom runtime exception used when attempting to create
 * or update a resource that already exists.
 *
 * Typical use cases:
 *
 * - Duplicate email registration
 * - Duplicate username
 * - Duplicate document reference
 *
 *
 * HTTP Mapping:
 *
 * 409 CONFLICT
 *
 *
 * Example:
 *
 * throw new DuplicateResourceException(
 *      "Email already exists: " + email
 * );
 *
 * ============================================================
 */
public class DuplicateResourceException extends RuntimeException {


    /**
     * Creates a duplicate resource exception.
     *
     * @param message descriptive error message
     */
    public DuplicateResourceException(
            String message
    ) {

        super(message);

    }



    /**
     * Creates a duplicate resource exception
     * with a root cause.
     *
     * @param message descriptive error message
     * @param cause original exception
     */
    public DuplicateResourceException(
            String message,
            Throwable cause
    ) {

        super(
                message,
                cause
        );

    }


}