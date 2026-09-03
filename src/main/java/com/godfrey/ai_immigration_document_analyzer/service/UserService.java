package com.godfrey.ai_immigration_document_analyzer.service;


import com.godfrey.ai_immigration_document_analyzer.dto.request.CreateUserRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.request.UpdateUserRequest;
import com.godfrey.ai_immigration_document_analyzer.dto.response.UserManagementResponse;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.UserStatus;


import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;



/**
 * ============================================================
 * USER SERVICE INTERFACE
 * ============================================================
 *
 * Business service contract for user administration.
 *
 *
 * Responsibilities:
 *
 * - User lifecycle management
 * - User creation
 * - User updates
 * - Account activation
 * - Account suspension
 * - User deletion
 * - User searching
 * - User filtering
 * - User analytics
 *
 *
 * Security:
 *
 * - Passwords are never returned.
 * - Entities are never exposed directly.
 * - DTOs are used for API communication.
 *
 *
 * Architecture:
 *
 *
 * Controller
 *      |
 *      v
 * UserService
 *      |
 *      v
 * UserRepository
 *      |
 *      v
 * Oracle Database
 *
 *
 * Database:
 *
 * Oracle Database 21c
 *
 * ============================================================
 */
@Validated
public interface UserService {



    /**
     * Retrieve users with pagination,
     * searching and filtering.
     *
     *
     * Supported filters:
     *
     * Search:
     *
     * - Full name
     * - Email
     *
     *
     * Role:
     *
     * - ADMIN
     * - USER
     *
     *
     * Status:
     *
     * - ACTIVE
     * - SUSPENDED
     *
     *
     * @param search search keyword
     * @param role role filter
     * @param status account status filter
     * @param pageable pagination configuration
     *
     * @return paginated users
     */
    Page<UserManagementResponse> getUsers(
            String search,
            Role role,
            UserStatus status,
            Pageable pageable
    );





    /**
     * Find user by identifier.
     *
     *
     * @param id user identifier
     *
     * @return user response
     *
     * @throws RuntimeException when user does not exist
     */
    UserManagementResponse getUserById(
            Long id
    );





    /**
     * Create a new user account.
     *
     *
     * Used by:
     *
     * - Administrator user creation
     * - Internal account provisioning
     *
     *
     * @param request user creation payload
     *
     * @return created user response
     */
    UserManagementResponse createUser(
            @Valid CreateUserRequest request
    );





    /**
     * Update an existing user.
     *
     *
     * Supported updates:
     *
     * - Name
     * - Email
     * - Password
     * - Role
     *
     *
     * @param id user identifier
     * @param request update payload
     *
     * @return updated user response
     */
    UserManagementResponse updateUser(
            Long id,
            @Valid UpdateUserRequest request
    );





    /**
     * Suspend user account.
     *
     *
     * Result:
     *
     * enabled = false
     *
     *
     * @param id user identifier
     */
    void suspendUser(
            Long id
    );





    /**
     * Activate user account.
     *
     *
     * Result:
     *
     * enabled = true
     *
     *
     * @param id user identifier
     */
    void activateUser(
            Long id
    );





    /**
     * Permanently remove user account.
     *
     *
     * Warning:
     *
     * This operation deletes the database record.
     *
     *
     * @param id user identifier
     */
    void deleteUser(
            Long id
    );





    /**
     * Count all registered users.
     *
     *
     * Used by:
     *
     * - Admin dashboard
     * - Analytics
     *
     *
     * @return total users
     */
    long countUsers();





    /**
     * Count active users.
     *
     *
     * Maps to:
     *
     * User.enabled = true
     *
     *
     * @return active user count
     */
    long countActiveUsers();





    /**
     * Count disabled users.
     *
     *
     * Maps to:
     *
     * User.enabled = false
     *
     *
     * @return disabled user count
     */
    long countDisabledUsers();





    /**
     * Count users by role.
     *
     *
     * Examples:
     *
     * ADMIN
     * USER
     *
     *
     * @param role user role
     *
     * @return user count
     */
    long countUsersByRole(
            Role role
    );





    /**
     * Check if email already exists.
     *
     *
     * Used during:
     *
     * - User creation
     * - Email updates
     *
     *
     * @param email email address
     *
     * @return true when email exists
     */
    boolean emailExists(
            String email
    );



}