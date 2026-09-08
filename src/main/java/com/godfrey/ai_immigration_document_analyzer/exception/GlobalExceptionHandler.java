package com.godfrey.ai_immigration_document_analyzer.exception;



import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.orm.jpa.JpaSystemException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;

import org.springframework.transaction.TransactionSystemException;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.context.request.WebRequest;

import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ============================================================================
 * GLOBAL EXCEPTION HANDLER
 * ============================================================================
 *
 * Centralized REST exception handling.
 *
 * Responsibilities:
 *
 * - Validation errors
 * - Authentication failures
 * - Authorization failures
 * - Resource-not-found errors
 * - Oracle/JPA/database errors
 * - File-upload errors
 * - AI service failures
 * - Illegal application arguments
 * - Safe generic 500 responses
 *
 * IMPORTANT:
 *
 * Internal exception messages are NEVER returned directly for unexpected
 * exceptions.
 *
 * A correlation ID is returned to the client so the corresponding server log
 * can be located safely.
 *
 * ============================================================================
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================================================================
    // RESOURCE NOT FOUND
    // =========================================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex,
            WebRequest request
    ) {

        log.debug(
                "Resource not found | path={} | message={}",
                getPath(request),
                ex.getMessage()
        );

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found.",
                request
        );
    }

    // =========================================================================
    // ENTITY NOT FOUND
    // =========================================================================

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(
            EntityNotFoundException ex,
            WebRequest request
    ) {

        log.warn(
                "JPA entity not found | path={} | message={}",
                getPath(request),
                ex.getMessage()
        );

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found.",
                request
        );
    }

    // =========================================================================
    // AUTHENTICATION
    // =========================================================================

    @ExceptionHandler({
            AuthenticationException.class,
            BadCredentialsException.class,
            InsufficientAuthenticationException.class
    })
    public ResponseEntity<Map<String, Object>> handleAuthentication(
            AuthenticationException ex,
            WebRequest request
    ) {

        log.warn(
                "Authentication failure | path={} | type={}",
                getPath(request),
                ex.getClass().getSimpleName()
        );

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required.",
                request
        );
    }

    // =========================================================================
    // ACCESS DENIED
    // =========================================================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex,
            WebRequest request
    ) {

        log.warn(
                "Access denied | path={}",
                getPath(request)
        );

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource.",
                request
        );
    }

    // =========================================================================
    // VALIDATION
    // =========================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {

        Map<String, String> fieldErrors =
                new HashMap<>();

        for (FieldError fieldError :
                ex.getBindingResult().getFieldErrors()) {

            fieldErrors.put(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        log.debug(
                "Request validation failed | path={} | fields={}",
                getPath(request),
                fieldErrors.keySet()
        );

        Map<String, Object> body =
                buildErrorBody(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed.",
                        request
                );

        body.put(
                "fieldErrors",
                fieldErrors
        );

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    // =========================================================================
    // CONSTRAINT VALIDATION
    // =========================================================================

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request
    ) {

        log.debug(
                "Constraint validation failed | path={}",
                getPath(request)
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed.",
                request
        );
    }

    // =========================================================================
    // ILLEGAL ARGUMENT
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request
    ) {

        log.debug(
                "Invalid request argument | path={} | message={}",
                getPath(request),
                ex.getMessage()
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                safeMessage(
                        ex.getMessage(),
                        "Invalid request."
                ),
                request
        );
    }

    // =========================================================================
    // INVALID STATE TRANSITION
    // =========================================================================

    /**
     * Thrown by the Fact foundation's lifecycle/conflict state machines
     * (e.g. an out-of-order status transition, or resolving an
     * already-resolved conflict) - a client-caused conflict with the
     * resource's current state, not a server fault.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex,
            WebRequest request
    ) {

        log.debug(
                "Invalid state transition | path={} | message={}",
                getPath(request),
                ex.getMessage()
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                safeMessage(
                        ex.getMessage(),
                        "This action cannot be performed in the resource's current state."
                ),
                request
        );
    }

    // =========================================================================
    // DUPLICATE RESOURCE
    // =========================================================================

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResource(
            DuplicateResourceException ex,
            WebRequest request
    ) {

        log.warn(
                "Duplicate resource | path={} | message={}",
                getPath(request),
                ex.getMessage()
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                safeMessage(
                        ex.getMessage(),
                        "Resource already exists."
                ),
                request
        );
    }

    // =========================================================================
    // DATABASE INTEGRITY
    // =========================================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            WebRequest request
    ) {

        final String correlationId =
                generateCorrelationId();

        log.error(
                "Database integrity violation | correlationId={} | path={}",
                correlationId,
                getPath(request),
                ex
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                "The operation could not be completed because it conflicts with existing data.",
                request,
                correlationId
        );
    }

    // =========================================================================
    // DATABASE ACCESS
    // =========================================================================

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(
            DataAccessException ex,
            WebRequest request
    ) {

        final String correlationId =
                generateCorrelationId();

        log.error(
                "Database access failure | correlationId={} | path={} | exception={}",
                correlationId,
                getPath(request),
                ex.getClass().getName(),
                ex
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "A database error occurred. Please try again later.",
                request,
                correlationId
        );
    }

    // =========================================================================
    // JPA
    // =========================================================================

    @ExceptionHandler(JpaSystemException.class)
    public ResponseEntity<Map<String, Object>> handleJpaSystem(
            JpaSystemException ex,
            WebRequest request
    ) {

        final String correlationId =
                generateCorrelationId();

        log.error(
                "JPA system failure | correlationId={} | path={}",
                correlationId,
                getPath(request),
                ex
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "A database processing error occurred. Please try again later.",
                request,
                correlationId
        );
    }

    // =========================================================================
    // TRANSACTION
    // =========================================================================

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<Map<String, Object>> handleTransaction(
            TransactionSystemException ex,
            WebRequest request
    ) {

        final String correlationId =
                generateCorrelationId();

        log.error(
                "Transaction failure | correlationId={} | path={}",
                correlationId,
                getPath(request),
                ex
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The database transaction could not be completed.",
                request,
                correlationId
        );
    }

    // =========================================================================
    // EMPTY DATABASE RESULT
    // =========================================================================

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleEmptyResult(
            EmptyResultDataAccessException ex,
            WebRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found.",
                request
        );
    }

    // =========================================================================
    // FILE SIZE
    // =========================================================================

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex,
            WebRequest request
    ) {

        log.warn(
                "File upload size exceeded | path={}",
                getPath(request)
        );

        return buildResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Uploaded file is too large.",
                request
        );
    }

    // =========================================================================
    // MULTIPART
    // =========================================================================

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipart(
            MultipartException ex,
            WebRequest request
    ) {

        final String correlationId =
                generateCorrelationId();

        log.error(
                "Multipart processing failure | correlationId={} | path={}",
                correlationId,
                getPath(request),
                ex
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "The uploaded file could not be processed.",
                request,
                correlationId
        );
    }

    // =========================================================================
    // AI SERVICE
    // =========================================================================

    /**
     * Handles the application's AI-specific exception.
     *
     * IMPORTANT:
     *
     * Replace AiServiceException with your actual AI exception class if your
     * project already defines a different name.
     */
    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAiService(
            AiServiceException ex,
            WebRequest request
    ) {

        final String correlationId =
                generateCorrelationId();

        log.error(
                "AI service failure | correlationId={} | path={} | type={}",
                correlationId,
                getPath(request),
                ex.getClass().getSimpleName(),
                ex
        );

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The AI service is temporarily unavailable. Please try again later.",
                request,
                correlationId
        );
    }

    // =========================================================================
    // GENERIC APPLICATION EXCEPTION
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex,
            WebRequest request
    ) {

        final String correlationId =
                generateCorrelationId();

        /*
         * NEVER return ex.getMessage() here.
         *
         * It can expose:
         *
         * - Oracle connection information
         * - SQL statements
         * - table names
         * - S3 details
         * - filesystem paths
         * - internal implementation details
         * - API credentials accidentally included in exception messages
         */
        log.error(
                "Unhandled application exception | correlationId={} | path={} | type={}",
                correlationId,
                getPath(request),
                ex.getClass().getName(),
                ex
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred. Please try again later.",
                request,
                correlationId
        );
    }

    // =========================================================================
    // RESPONSE BUILDERS
    // =========================================================================

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message,
            WebRequest request
    ) {

        return ResponseEntity
                .status(status)
                .body(
                        buildErrorBody(
                                status,
                                message,
                                request
                        )
                );
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String message,
            WebRequest request,
            String correlationId
    ) {

        Map<String, Object> body =
                buildErrorBody(
                        status,
                        message,
                        request
                );

        body.put(
                "correlationId",
                correlationId
        );

        return ResponseEntity
                .status(status)
                .body(body);
    }

    private Map<String, Object> buildErrorBody(
            HttpStatus status,
            String message,
            WebRequest request
    ) {

        Map<String, Object> body =
                new HashMap<>();

        body.put(
                "timestamp",
                LocalDateTime.now()
        );

        body.put(
                "status",
                status.value()
        );

        body.put(
                "error",
                status.getReasonPhrase()
        );

        body.put(
                "message",
                message
        );

        body.put(
                "path",
                getPath(request)
        );

        return body;
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private String getPath(
            WebRequest request
    ) {

        if (request == null) {
            return "unknown";
        }

        return request
                .getDescription(false)
                .replace("uri=", "");
    }

    private String generateCorrelationId() {
        return UUID
                .randomUUID()
                .toString();
    }

    private String safeMessage(
            String message,
            String fallback
    ) {

        if (message == null || message.isBlank()) {
            return fallback;
        }

        return message.trim();
    }
}

