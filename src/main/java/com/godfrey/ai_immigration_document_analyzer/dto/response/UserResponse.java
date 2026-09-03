package com.godfrey.ai_immigration_document_analyzer.dto.response;


import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;




/**
 * ============================================================
 * USER RESPONSE DTO
 * ============================================================
 *
 * Returned to frontend after:
 *
 * - Login
 * - Registration
 * - GET /api/auth/me
 * - User management operations
 *
 *
 * Exposes only safe user information.
 *
 * Security:
 *
 * - Password is never returned
 * - Sensitive authentication data is never exposed
 *
 * ============================================================
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {




    /*
    |--------------------------------------------------------------------------
    | USER IDENTIFICATION
    |--------------------------------------------------------------------------
    */


    /**
     * Database user ID.
     */
    private Long id;








    /*
    |--------------------------------------------------------------------------
    | USER PROFILE
    |--------------------------------------------------------------------------
    */


    /**
     * User full name.
     */
    private String fullName;





    /**
     * User email.
     *
     * Used as authentication identity.
     */
    private String email;








    /*
    |--------------------------------------------------------------------------
    | AUTHORIZATION
    |--------------------------------------------------------------------------
    */


    /**
     * User role:
     *
     * USER
     * ADMIN
     */
    private String role;








    /*
    |--------------------------------------------------------------------------
    | ACCOUNT STATUS
    |--------------------------------------------------------------------------
    */


    /**
     * Account enabled status.
     */
    private boolean enabled;









    /**
     * Forces user to change password.
     *
     * Used for:
     *
     * - Initial administrator account
     * - Temporary passwords
     * - Password reset security flow
     *
     *
     * Example:
     *
     * true:
     *      Redirect to change password page
     *
     * false:
     *      Continue normal application access
     */
    private boolean mustChangePassword;









    /*
    |--------------------------------------------------------------------------
    | AUDIT INFORMATION
    |--------------------------------------------------------------------------
    */


    /**
     * Account creation date.
     */
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime createdAt;



}