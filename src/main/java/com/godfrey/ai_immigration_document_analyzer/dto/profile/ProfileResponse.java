package com.godfrey.ai_immigration_document_analyzer.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileResponse(

        Long id,

        String name,

        String email,

        String role,

        String plan,

        long analysesUsed,

        long analysesLimit,

        Instant memberSince,

        boolean verified,

        String avatarUrl

) {
}