package com.godfrey.ai_immigration_document_analyzer.service.validation;

import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * PASSWORD POLICY VALIDATOR
 * ============================================================================
 *
 * Single source of truth for password strength rules on the backend.
 *
 * This MUST stay in sync with the client-side checks in:
 *
 *   - ai-immigration-frontend/src/pages/auth/RegisterPage.tsx
 *   - ai-immigration-frontend/src/pages/auth/ResetPasswordPage.tsx
 *
 * Rules (checked in this order, first failure wins, matching the frontend):
 *
 *   1. at least MIN_LENGTH characters
 *   2. at least one uppercase letter
 *   3. at least one digit
 *
 * Enforcing this here as well (not just in the frontend) ensures the
 * requirement cannot be bypassed by calling the API directly.
 * ============================================================================
 */
@Component
public class PasswordPolicyValidator {

    public static final int MIN_LENGTH = 12;

    /**
     * Validate a candidate password against the shared policy.
     *
     * @throws IllegalArgumentException with a user-facing message describing
     *         the first rule that failed.
     */
    public void validate(
            String password
    ) {

        if (
                password == null
                        ||
                        password.length() < MIN_LENGTH
        ) {

            throw new IllegalArgumentException(
                    "Password must contain at least "
                            + MIN_LENGTH
                            + " characters."
            );
        }

        if (
                password.chars()
                        .noneMatch(Character::isUpperCase)
        ) {

            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter."
            );
        }

        if (
                password.chars()
                        .noneMatch(Character::isDigit)
        ) {

            throw new IllegalArgumentException(
                    "Password must contain at least one number."
            );
        }
    }
}
