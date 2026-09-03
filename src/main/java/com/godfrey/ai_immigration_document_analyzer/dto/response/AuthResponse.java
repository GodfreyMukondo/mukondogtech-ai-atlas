package com.godfrey.ai_immigration_document_analyzer.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;





/**
 * ============================================================
 * AUTH RESPONSE DTO
 * ============================================================
 *
 * Response returned after successful authentication actions.
 *
 * Used by:
 *
 * - User login
 * - User registration
 * - JWT authentication flow
 *
 *
 * Contains:
 *
 * - JWT access token
 * - Token type
 * - Authenticated user details
 *
 *
 * Security:
 *
 * - Does not expose password
 * - Does not expose sensitive authentication data
 *
 * ============================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {





    /*
    |--------------------------------------------------------------------------
    | JWT TOKEN
    |--------------------------------------------------------------------------
    */


    /**
     * JWT access token.
     *
     * Frontend sends this token in:
     *
     * Authorization: Bearer <token>
     *
     */
    private String token;








    /*
    |--------------------------------------------------------------------------
    | TOKEN INFORMATION
    |--------------------------------------------------------------------------
    */


    /**
     * JWT authentication scheme.
     *
     * Default:
     *
     * Bearer
     *
     */
    @Builder.Default
    private String tokenType = "Bearer";








    /*
    |--------------------------------------------------------------------------
    | AUTHENTICATED USER
    |--------------------------------------------------------------------------
    */


    /**
     * Logged-in user information.
     *
     * Contains:
     *
     * - ID
     * - Name
     * - Email
     * - Role
     * - Account status
     * - Password change requirement
     *
     */
    private UserResponse user;





}