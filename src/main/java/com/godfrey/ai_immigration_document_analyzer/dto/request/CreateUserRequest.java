package com.godfrey.ai_immigration_document_analyzer.dto.request;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * ============================================================
 * CREATE USER REQUEST DTO
 * ============================================================
 *
 * Data transfer object used when creating a new user account.
 *
 * Used by:
 *
 * - UserController
 * - UserService
 * - UserServiceImpl
 * - Administrative user management APIs
 *
 * Responsibilities:
 *
 * - Validate incoming user creation data
 * - Prevent direct entity exposure
 * - Carry user registration information
 * - Support administrator account creation
 *
 * Security:
 *
 * - Password is accepted only for creation.
 * - Password is never returned in responses.
 *
 * ============================================================
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {



    /**
     * User full name.
     */
    @NotBlank(
            message = "Full name is required"
    )
    @Size(
            min = 2,
            max = 150,
            message = "Full name must contain between 2 and 150 characters"
    )
    private String fullName;




    /**
     * Unique email address.
     */
    @NotBlank(
            message = "Email is required"
    )
    @Email(
            message = "Email must be valid"
    )
    @Size(
            max = 255,
            message = "Email cannot exceed 255 characters"
    )
    private String email;





    /**
     * Initial account password.
     */
    @NotBlank(
            message = "Password is required"
    )
    @Size(
            min = 8,
            max = 255,
            message = "Password must contain between 8 and 255 characters"
    )
    private String password;





    /**
     * User phone number.
     *
     * Note:
     *
     * Your current User entity does not
     * contain this column yet.
     *
     * Kept for future expansion.
     */
    @Size(
            max = 30,
            message = "Phone cannot exceed 30 characters"
    )
    private String phone;





    /**
     * User country.
     *
     * Note:
     *
     * Your current User entity does not
     * contain this column yet.
     */
    @Size(
            max = 100,
            message = "Country cannot exceed 100 characters"
    )
    private String country;





    /**
     * Assigned application role.
     *
     * Example:
     *
     * USER
     * ADMIN
     */
    @NotNull(
            message = "Role is required"
    )
    private Role role;



}