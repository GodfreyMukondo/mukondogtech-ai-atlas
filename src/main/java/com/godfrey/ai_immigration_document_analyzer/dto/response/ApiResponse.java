package com.godfrey.ai_immigration_document_analyzer.dto.response;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**

 * Standard API response wrapper for all endpoints.
 * Ensures consistent response structure across the system.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**

     * Indicates if request was successful.
     */
    private boolean success;

    /**

     * Human-readable message.
     */
    private String message;

    /**

     * Actual response payload.
     */
    private T data;

    /**

     * Response timestamp.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**

     * Creates a success response.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**

     * Creates an error response.
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
