package com.godfrey.ai_immigration_document_analyzer.repository;

import com.godfrey.ai_immigration_document_analyzer.entity.PasswordResetToken;
import com.godfrey.ai_immigration_document_analyzer.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ============================================================================
 * PASSWORD RESET TOKEN REPOSITORY
 * ============================================================================
 *
 * Database access layer for the forgot-password / reset-password flow.
 * ============================================================================
 */
@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Look up a token by its hash.
     *
     * The caller is responsible for validating {@link PasswordResetToken#isValid()}
     * before trusting the result.
     */
    Optional<PasswordResetToken> findByTokenHash(
            String tokenHash
    );


    /**
     * Invalidate every outstanding (unused) token for a user.
     *
     * Called before issuing a new token so a user can never have more than
     * one active reset link outstanding at a time.
     */
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Transactional
    @Query("""
            UPDATE PasswordResetToken t
            SET t.usedAt = CURRENT_TIMESTAMP
            WHERE t.user = :user
            AND t.usedAt IS NULL
            """)
    void invalidateActiveTokensForUser(
            @Param("user")
            User user
    );


    /**
     * Housekeeping: permanently remove tokens that expired before the
     * given cutoff.
     *
     * Not currently invoked by a scheduled job; kept available for one.
     */
    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Transactional
    @Query("""
            DELETE FROM PasswordResetToken t
            WHERE t.expiresAt < :cutoff
            """)
    void deleteExpiredBefore(
            @Param("cutoff")
            LocalDateTime cutoff
    );


    /**
     * Removes every reset token issued to the user.
     */
    void deleteByUserId(
            Long userId
    );
}
