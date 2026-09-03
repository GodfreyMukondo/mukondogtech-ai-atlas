package com.godfrey.ai_immigration_document_analyzer.entity;

/**
 * ============================================================
 * USER STATUS ENUM
 * ============================================================
 *
 * Represents the lifecycle status of a user account.
 *
 * Responsibilities:
 *
 * - User account management
 * - Admin user control
 * - Account suspension
 * - Account activation workflow
 * - Future audit/reporting support
 *
 * Note:
 *
 * This is different from Role.
 *
 * Role controls permissions:
 *
 * ADMIN
 * USER
 *
 * UserStatus controls account state:
 *
 * ACTIVE
 * PENDING
 * SUSPENDED
 *
 * ============================================================
 */
public enum UserStatus {


    /**
     * User account is active
     * and can access the platform.
     */
    ACTIVE,



    /**
     * User account registration
     * is awaiting verification.
     */
    PENDING,



    /**
     * User account has been
     * suspended by administrator.
     */
    SUSPENDED;



    /**
     * Checks whether the account
     * is currently active.
     *
     * @return true when status is ACTIVE
     */
    public boolean isActive() {

        return this == ACTIVE;

    }



    /**
     * Checks whether the account
     * has been suspended.
     *
     * @return true when status is SUSPENDED
     */
    public boolean isSuspended() {

        return this == SUSPENDED;

    }



    /**
     * Checks whether the account
     * requires verification.
     *
     * @return true when status is PENDING
     */
    public boolean isPending() {

        return this == PENDING;

    }


}