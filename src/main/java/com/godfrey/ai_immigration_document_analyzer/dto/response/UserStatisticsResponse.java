package com.godfrey.ai_immigration_document_analyzer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


/**
 * ============================================================
 * USER STATISTICS RESPONSE DTO
 * ============================================================
 *
 * Used by admin dashboard user analytics.
 *
 * Contains:
 *
 * - Total users
 * - Active users
 * - Disabled users
 * - Admin count
 *
 * ============================================================
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsResponse {


    private long totalUsers;


    private long activeUsers;


    private long disabledUsers;


    private long admins;


}