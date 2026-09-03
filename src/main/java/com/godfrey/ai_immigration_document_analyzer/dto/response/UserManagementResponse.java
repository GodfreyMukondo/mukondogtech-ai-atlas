package com.godfrey.ai_immigration_document_analyzer.dto.response;


import com.fasterxml.jackson.annotation.JsonFormat;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.UserStatus;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.time.LocalDateTime;



/**
 * ============================================================
 * USER MANAGEMENT RESPONSE DTO
 * ============================================================
 *
 * Administrative representation of a user account returned
 * from User Management APIs.
 *
 *
 * Responsibilities:
 *
 * - Expose safe user information to administrators
 * - Hide sensitive security information
 * - Support admin dashboard rendering
 * - Support user searching and filtering
 * - Provide audit information
 *
 *
 * Security:
 *
 * The following are NEVER exposed:
 *
 * - Password
 * - JWT tokens
 * - Authentication credentials
 * - Internal security metadata
 *
 *
 * Used by:
 *
 * - GET /api/users
 * - GET /api/users/{id}
 * - Admin dashboard
 * - User administration screens
 * - User analytics
 *
 *
 * Database:
 *
 * Oracle Database 21c
 *
 * ============================================================
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementResponse implements Serializable {


    private static final long serialVersionUID = 1L;



    /*
    |------------------------------------------------------------------
    | USER IDENTIFICATION
    |------------------------------------------------------------------
    */


    /**
     * Unique database identifier.
     */
    private Long id;



    /**
     * User display name.
     *
     * Maps from:
     *
     * User.fullName
     */
    private String name;



    /**
     * Registered email address.
     */
    private String email;



    /*
    |------------------------------------------------------------------
    | PROFILE INFORMATION
    |------------------------------------------------------------------
    */


    /**
     * User contact phone number.
     *
     * Note:
     *
     * Currently reserved for future User entity expansion.
     */
    private String phone;



    /**
     * User country.
     *
     * Note:
     *
     * Currently reserved for future User entity expansion.
     */
    private String country;



    /*
    |------------------------------------------------------------------
    | AUTHORIZATION
    |------------------------------------------------------------------
    */


    /**
     * Assigned application role.
     *
     * Examples:
     *
     * ADMIN
     * USER
     */
    private Role role;



    /**
     * Current account status.
     *
     * Derived from:
     *
     * User.enabled
     *
     * Mapping:
     *
     * true  -> ACTIVE
     *
     * false -> SUSPENDED
     */
    private UserStatus status;



    /*
    |------------------------------------------------------------------
    | IMMIGRATION INFORMATION
    |------------------------------------------------------------------
    */


    /**
     * Number of immigration applications
     * created by this user.
     *
     * Currently:
     *
     * Returns 0 until Application entity/module
     * is connected.
     */
    private Long applications;



    /*
    |------------------------------------------------------------------
    | AUDIT INFORMATION
    |------------------------------------------------------------------
    */


    /**
     * Account creation timestamp.
     *
     * Maps from:
     *
     * User.createdAt
     */
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime joined;



}