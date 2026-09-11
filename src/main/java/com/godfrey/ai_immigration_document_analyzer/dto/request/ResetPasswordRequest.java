package com.godfrey.ai_immigration_document_analyzer.dto.request;

import com.godfrey.ai_immigration_document_analyzer.service.validation.PasswordPolicyValidator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    private String token;

    /**
     * Full password strength rules (uppercase letter, digit) are enforced
     * by {@link PasswordPolicyValidator} in the service layer. This bound
     * only fails fast on obviously-too-short input before it gets there.
     */
    @NotBlank(message = "Password is required")
    @Size(
            min = PasswordPolicyValidator.MIN_LENGTH,
            max = 100,
            message = "Password must be between " + PasswordPolicyValidator.MIN_LENGTH + " and 100 characters"
    )
    private String password;
}
