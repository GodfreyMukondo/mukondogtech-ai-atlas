package com.godfrey.ai_immigration_document_analyzer.entity;

import jakarta.persistence.*;

import lombok.*;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * PASSWORD RESET TOKEN
 * ============================================================================
 *
 * Represents a single forgot-password reset attempt.
 *
 * Security notes:
 *
 * - Only a SHA-256 hash of the raw token is ever persisted. The raw token
 *   is emailed to the user once and never stored, so a database compromise
 *   alone cannot be used to reset a user's password.
 *
 * - Tokens are single-use: consuming a token sets {@link #usedAt} rather
 *   than deleting the row, preserving an audit trail.
 *
 * - Issuing a new token invalidates any previously outstanding token for
 *   the same user (see PasswordResetTokenRepository#invalidateActiveTokensForUser),
 *   so at most one reset link is ever active at a time.
 * ============================================================================
 */
@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(
                        name = "idx_password_reset_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_password_reset_expires_at",
                        columnList = "expires_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "id",
            nullable = false
    )
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            name = "token_hash",
            nullable = false,
            unique = true,
            length = 255
    )
    private String tokenHash;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;

    @Column(
            name = "used_at"
    )
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return expiresAt == null
                || expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    public void markUsed() {
        this.usedAt = LocalDateTime.now();
    }
}
