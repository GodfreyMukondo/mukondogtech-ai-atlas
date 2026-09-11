package com.godfrey.ai_immigration_document_analyzer.repository;

import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


/**
 * ============================================================
 * USER REPOSITORY
 * ============================================================
 *
 * Database access layer for User entity.
 *
 * Database:
 *
 * Oracle Database 21c
 *
 * Technologies:
 *
 * - Spring Data JPA
 * - Hibernate ORM
 *
 * Responsibilities:
 *
 * - User lookup
 * - Authentication lookup
 * - Email uniqueness checks
 * - Case-insensitive email lookup
 * - Role queries
 * - Account-status queries
 * - User search
 * - Pagination
 * - Profile lookups
 * - Administrative queries
 *
 * IMPORTANT:
 *
 * Business logic should remain in the service layer.
 * This repository should primarily perform database access.
 *
 * Security-sensitive operations such as password changes,
 * role changes and account activation/deactivation should
 * remain in the service layer.
 *
 * ============================================================
 */
@Repository
public interface UserRepository extends
        JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {


    /*
    |--------------------------------------------------------------------------
    | AUTHENTICATION
    |--------------------------------------------------------------------------
    */

    /**
     * Find user by email.
     *
     * Email is normalized to lowercase by the User entity
     * before persistence.
     *
     * The service layer should normalize incoming authentication
     * emails before calling this method.
     */
    Optional<User> findByEmail(String email);


    /**
     * Find user by email without case sensitivity.
     *
     * This is useful for authentication and profile operations
     * where the email supplied by the client may contain uppercase
     * characters.
     *
     * Example:
     *
     *     Godfrey@example.com
     *
     * will match:
     *
     *     godfrey@example.com
     */
    Optional<User> findByEmailIgnoreCase(
            String email
    );


    /**
     * Find an enabled user by email.
     *
     * Email comparison is case-insensitive.
     *
     * Useful for authentication flows where disabled accounts
     * must not be returned as active users.
     */
    Optional<User> findByEmailIgnoreCaseAndEnabledTrue(
            String email
    );


    /**
     * Check whether an email already exists.
     *
     * The service layer should normally normalize the email
     * before calling this method.
     */
    boolean existsByEmail(String email);


    /**
     * Check whether an email already exists without case sensitivity.
     *
     * Useful for registration and profile email updates.
     */
    boolean existsByEmailIgnoreCase(
            String email
    );


    /**
     * Check whether an email already exists for another user.
     *
     * Useful when implementing profile email updates.
     *
     * The comparison is case-insensitive.
     */
    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Long id
    );


    /**
     * Existing case-sensitive variant preserved.
     *
     * This method remains available for existing application
     * code that already depends on it.
     */
    boolean existsByEmailAndIdNot(
            String email,
            Long id
    );


    /*
    |--------------------------------------------------------------------------
    | ROLE QUERIES
    |--------------------------------------------------------------------------
    */

    /**
     * Count users by role.
     */
    long countByRole(Role role);


    /**
     * Check whether a user exists with a specific role.
     */
    boolean existsByRole(Role role);


    /**
     * Find first user by role.
     */
    Optional<User> findFirstByRole(Role role);


    /**
     * Find users by role.
     */
    List<User> findByRole(Role role);


    /**
     * Find enabled users by role.
     */
    List<User> findByRoleAndEnabledTrue(Role role);


    /**
     * Find disabled users by role.
     */
    List<User> findByRoleAndEnabledFalse(Role role);


    /*
    |--------------------------------------------------------------------------
    | ACCOUNT STATUS
    |--------------------------------------------------------------------------
    */

    /**
     * Count enabled users.
     */
    long countByEnabledTrue();


    /**
     * Count disabled users.
     */
    long countByEnabledFalse();


    /**
     * Check whether an enabled user exists for an email.
     *
     * Existing method preserved.
     */
    boolean existsByEmailAndEnabledTrue(
            String email
    );


    /**
     * Check whether an enabled user exists for an email
     * without case sensitivity.
     */
    boolean existsByEmailIgnoreCaseAndEnabledTrue(
            String email
    );


    /**
     * Find an enabled user by email.
     *
     * Useful when an authentication/profile operation should
     * only operate against active accounts.
     *
     * Existing method preserved.
     */
    Optional<User> findByEmailAndEnabledTrue(
            String email
    );


    /*
    |--------------------------------------------------------------------------
    | SEARCH
    |--------------------------------------------------------------------------
    */

    /**
     * Search users by full name or email.
     *
     * Existing method preserved.
     */
    List<User>
    findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName,
            String email
    );


    /**
     * Search enabled users by full name or email.
     *
     * Existing method preserved.
     */
    List<User>
    findByEnabledTrueAndFullNameContainingIgnoreCaseOrEnabledTrueAndEmailContainingIgnoreCase(
            String fullName,
            String email
    );


    /*
    |--------------------------------------------------------------------------
    | PAGINATED SEARCH
    |--------------------------------------------------------------------------
    */

    /**
     * Paginated user search.
     *
     * This is preferable to loading a large user table into memory
     * in an administrative interface.
     */
    Page<User>
    findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName,
            String email,
            Pageable pageable
    );


    /**
     * Paginated users by role.
     */
    Page<User> findByRole(
            Role role,
            Pageable pageable
    );


    /**
     * Paginated enabled users.
     */
    Page<User> findByEnabledTrue(
            Pageable pageable
    );


    /**
     * Paginated disabled users.
     */
    Page<User> findByEnabledFalse(
            Pageable pageable
    );


    /**
     * Paginated users by role and account status.
     */
    Page<User> findByRoleAndEnabled(
            Role role,
            boolean enabled,
            Pageable pageable
    );


    /*
    |--------------------------------------------------------------------------
    | PROFILE / ID LOOKUPS
    |--------------------------------------------------------------------------
    */

    /**
     * Find an enabled user by ID.
     *
     * Useful for profile operations where disabled accounts
     * should not be treated as active application users.
     *
     * Existing method preserved.
     */
    Optional<User> findByIdAndEnabledTrue(
            Long id
    );


    /**
     * Check whether a user exists and is enabled.
     *
     * Existing method preserved.
     */
    boolean existsByIdAndEnabledTrue(
            Long id
    );


    /**
     * Find a user by ID and role.
     *
     * Useful for administrative authorization and account
     * management operations.
     */
    Optional<User> findByIdAndRole(
            Long id,
            Role role
    );


    /**
     * Find an enabled user by ID and role.
     */
    Optional<User> findByIdAndRoleAndEnabledTrue(
            Long id,
            Role role
    );


    /*
    |--------------------------------------------------------------------------
    | ADMINISTRATION
    |--------------------------------------------------------------------------
    */

    /**
     * Count enabled administrators.
     *
     * Existing method preserved.
     *
     * Although the method name specifically mentions
     * administrators, the Role argument keeps the repository
     * flexible for the existing application design.
     */
    long countByRoleAndEnabledTrue(
            Role role
    );


    /**
     * Find all users ordered by newest first.
     *
     * Existing method preserved.
     */
    List<User> findAllByOrderByCreatedAtDesc();


    /**
     * Find enabled users ordered by newest first.
     *
     * Existing method preserved.
     */
    List<User> findByEnabledTrueOrderByCreatedAtDesc();


    /**
     * Paginated newest users.
     *
     * Existing method preserved.
     */
    Page<User> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );


    /*
    |--------------------------------------------------------------------------
    | GROWTH ANALYTICS
    |--------------------------------------------------------------------------
    */

    /**
     * Count users registered within a date range.
     *
     * Used to compute real period-over-period registration growth for the
     * admin dashboard, rather than a fabricated constant.
     */
    long countByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );
}