package com.godfrey.ai_immigration_document_analyzer.dto.request;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * ============================================================
 * UPDATE USER REQUEST DTO
 * ============================================================
 *
 * Data transfer object used for updating existing users.
 *
 * Used by:
 *
 * - UserController
 * - UserService
 * - UserServiceImpl
 * - Admin user management dashboard
 *
 * Supports:
 *
 * - Profile updates
 * - Role changes
 * - Password reset
 *
 * Design:
 *
 * Fields are optional to allow partial updates.
 *
 * ============================================================
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {



    /**
     * Updated full name.
     */
    @Size(
            min = 2,
            max = 150,
            message = "Full name must contain between 2 and 150 characters"
    )
    private String fullName;





    /**
     * Updated email address.
     */
    @Email(
            message = "Email must be valid"
    )
    @Size(
            max = 255,
            message = "Email cannot exceed 255 characters"
    )
    private String email;





    /**
     * Updated password.
     *
     * Optional.
     *
     * If null:
     *
     * Existing password remains unchanged.
     */
    @Size(
            min = 8,
            max = 255,
            message = "Password must contain between 8 and 255 characters"
    )
    private String password;





    /**
     * Updated phone number.
     */
    @Size(
            max = 30,
            message = "Phone cannot exceed 30 characters"
    )
    private String phone;





    /**
     * Updated country.
     */
    @Size(
            max = 100,
            message = "Country cannot exceed 100 characters"
    )
    private String country;





    /**
     * Updated role.
     *
     * Example:
     *
     * ADMIN
     * USER
     */
    private Role role;



}