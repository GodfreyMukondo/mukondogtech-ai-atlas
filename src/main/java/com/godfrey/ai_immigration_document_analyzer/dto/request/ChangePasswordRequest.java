package com.godfrey.ai_immigration_document_analyzer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ============================================================
 * CHANGE PASSWORD REQUEST DTO
 * ============================================================
 *
 * Used by:
 *
 * POST /api/auth/change-password
 *
 * ============================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    /**
     * New password.
     */
    @NotBlank(
            message = "New password is required."
    )
    @Size(
            min = 12,
            message = "Password must contain at least 12 characters."
    )
    private String newPassword;

}