package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.entity.PasswordResetToken;
import com.godfrey.ai_immigration_document_analyzer.entity.User;

import com.godfrey.ai_immigration_document_analyzer.repository.PasswordResetTokenRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;

import com.godfrey.ai_immigration_document_analyzer.service.validation.PasswordPolicyValidator;

import jakarta.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

/**
 * ============================================================================
 * PASSWORD RESET SERVICE
 * ============================================================================
 *
 * Implements the forgot-password / reset-password flow end to end:
 *
 *   1. forgotPassword(email)
 *        - looks up an enabled account for the email
 *        - invalidates any previously issued (unused) reset token
 *        - generates a new cryptographically random token
 *        - persists only a SHA-256 hash of that token
 *        - emails the raw token as a reset link
 *
 *   2. resetPassword(token, newPassword)
 *        - re-hashes the supplied token and looks up the matching row
 *        - rejects the request if the token is missing, unknown, expired,
 *          or already used
 *        - enforces the shared password policy
 *        - updates the user's password and marks the token consumed
 *
 * ----------------------------------------------------------------------------
 * SECURITY DESIGN
 * ----------------------------------------------------------------------------
 *
 * - No user enumeration: forgotPassword() always completes successfully and
 *   logs at DEBUG (never at a level that would alarm/expose in production)
 *   regardless of whether the email matched an account. The controller
 *   layer returns one generic message for every outcome.
 *
 * - Tokens are single-use: a successful reset marks the token as used
 *   instead of deleting it, preserving an audit trail while preventing
 *   replay.
 *
 * - At most one active token per user: requesting a new reset link
 *   invalidates any token issued earlier that has not yet been consumed,
 *   so an intercepted stale link cannot be used after a newer one was
 *   requested.
 *
 * - Tokens are never stored in plaintext: only a SHA-256 hash of the raw
 *   token is persisted, so a database compromise alone cannot be used to
 *   reset a user's password. Lookups are performed by hash, which avoids
 *   comparing secrets with a manual (potentially timing-unsafe) equality
 *   check.
 *
 * - Tokens are generated with {@link SecureRandom} (32 bytes / 256 bits of
 *   entropy), URL-safe Base64 encoded, and expire after
 *   {@link #TOKEN_EXPIRY_MINUTES} minutes.
 * ============================================================================
 */
@Service
@Slf4j
public class PasswordResetService {

    private static final int TOKEN_BYTE_LENGTH = 32;

    private static final int TOKEN_EXPIRY_MINUTES = 30;

    private static final String HASH_ALGORITHM = "SHA-256";

    private static final String INVALID_RESET_TOKEN =
            "Invalid or expired password reset link.";

    private final UserRepository userRepository;

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private final PasswordEncoder passwordEncoder;

    private final PasswordPolicyValidator passwordPolicyValidator;

    private final EmailService emailService;

    private final SecureRandom secureRandom =
            new SecureRandom();

    /**
     * Public URL of the frontend, used to build the link sent by email.
     *
     * Must not have a trailing slash. See application.yml (app.frontend-url).
     */
    @Value("${app.frontend-url}")
    private String frontendUrl;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.emailService = emailService;
    }

    /**
     * Begin the forgot-password flow.
     *
     * Always completes successfully. Callers (the controller) must return
     * the same generic response whether or not an account exists for the
     * supplied email, so this endpoint cannot be used to enumerate
     * registered users.
     */
    @Transactional
    public void forgotPassword(
            String email
    ) {

        String normalizedEmail =
                normalizeEmail(email);

        userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .filter(User::isEnabled)
                .ifPresentOrElse(
                        this::issueResetToken,
                        () ->
                                log.debug(
                                        "Password reset requested for an email with no matching enabled account."
                                )
                );

        log.info(
                "Password reset requested."
        );
    }

    /**
     * Complete the forgot-password flow by consuming a reset token.
     *
     * @throws IllegalArgumentException if the token is missing, unknown,
     *         expired, or already used, or if the new password does not
     *         satisfy the shared password policy.
     */
    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword
    ) {

        if (
                rawToken == null
                        ||
                        rawToken.isBlank()
        ) {

            throw new IllegalArgumentException(
                    INVALID_RESET_TOKEN
            );
        }

        passwordPolicyValidator.validate(
                newPassword
        );

        String tokenHash =
                hashToken(rawToken);

        PasswordResetToken resetToken =
                passwordResetTokenRepository
                        .findByTokenHash(tokenHash)
                        .filter(PasswordResetToken::isValid)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        INVALID_RESET_TOKEN
                                )
                        );

        User user =
                resetToken.getUser();

        user.setPassword(
                passwordEncoder.encode(
                        newPassword
                )
        );

        user.completePasswordChange();

        userRepository.save(user);

        resetToken.markUsed();

        passwordResetTokenRepository.save(
                resetToken
        );

        log.info(
                "Password reset completed successfully. userId={}",
                user.getId()
        );
    }

    /**
     * Generate, persist, and email a new reset token for a known,
     * enabled user.
     */
    private void issueResetToken(
            User user
    ) {

        passwordResetTokenRepository
                .invalidateActiveTokensForUser(user);

        String rawToken =
                generateSecureToken();

        PasswordResetToken resetToken =
                PasswordResetToken.builder()
                        .user(user)
                        .tokenHash(
                                hashToken(rawToken)
                        )
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusMinutes(TOKEN_EXPIRY_MINUTES)
                        )
                        .build();

        passwordResetTokenRepository.save(
                resetToken
        );

        String resetLink =
                frontendUrl
                        + "/reset-password?token="
                        + rawToken;

        /*
         * Email delivery failures (SMTP outage, bad credentials, network
         * issues, ...) must never surface as a failed HTTP response here.
         *
         * The token has already been persisted above, so the reset link
         * remains valid even if this particular delivery attempt fails.
         * Letting the exception propagate would:
         *
         *   1. Roll back the transaction, silently discarding the token.
         *   2. Return HTTP 500 for a registered email but HTTP 200 for an
         *      unregistered one, which is a user-enumeration side channel
         *      through the response status code alone.
         */
        try {

            emailService.send(
                    user.getEmail(),
                    "Reset your password",
                    "We received a request to reset your password.\n\n"
                            + "Click the link below to choose a new password. "
                            + "This link expires in "
                            + TOKEN_EXPIRY_MINUTES
                            + " minutes and can only be used once.\n\n"
                            + resetLink
                            + "\n\n"
                            + "If you did not request this, you can safely ignore this "
                            + "email. Your password will not be changed."
            );

            log.info(
                    "Password reset token issued. userId={}",
                    user.getId()
            );

        } catch (RuntimeException ex) {

            log.error(
                    "Password reset token was issued but delivery email "
                            + "failed to send. userId={}",
                    user.getId(),
                    ex
            );
        }
    }

    /**
     * Generate a cryptographically random, URL-safe reset token.
     */
    private String generateSecureToken() {

        byte[] randomBytes =
                new byte[TOKEN_BYTE_LENGTH];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    /**
     * Hash a raw token for storage/lookup.
     *
     * The raw token is never persisted; only this hash is.
     */
    private String hashToken(
            String rawToken
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(HASH_ALGORITHM);

            byte[] hashBytes =
                    digest.digest(
                            rawToken.getBytes(StandardCharsets.UTF_8)
                    );

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hashBytes);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "Required hashing algorithm is unavailable: "
                            + HASH_ALGORITHM,
                    ex
            );
        }
    }

    /**
     * Normalize email consistently with AuthService.
     */
    private String normalizeEmail(
            String email
    ) {

        if (
                email == null
                        ||
                        email.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Email cannot be empty."
            );
        }

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
