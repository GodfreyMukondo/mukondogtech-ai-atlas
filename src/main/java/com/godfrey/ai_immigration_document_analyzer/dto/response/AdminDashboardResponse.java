package com.godfrey.ai_immigration_document_analyzer.dto.response;


import lombok.Builder;
import lombok.Data;

import java.util.List;



/**
 * ============================================================================
 * ADMIN DASHBOARD RESPONSE DTO
 * ============================================================================
 *
 * Enterprise dashboard response contract.
 *
 * Used by:
 *
 * - Admin Dashboard API
 * - Analytics dashboard
 * - Platform monitoring
 *
 * ============================================================================
 */


@Data
@Builder
public class AdminDashboardResponse {



    /*
    |--------------------------------------------------------------------------
    | EXECUTIVE KPI METRICS
    |--------------------------------------------------------------------------
    */


    private Executive executive;



    /*
    |--------------------------------------------------------------------------
    | IMMIGRATION OPERATIONS
    |--------------------------------------------------------------------------
    */


    private Operations operations;



    /*
    |--------------------------------------------------------------------------
    | ARTIFICIAL INTELLIGENCE METRICS
    |--------------------------------------------------------------------------
    */


    private AI ai;



    /*
    |--------------------------------------------------------------------------
    | SECURITY MONITORING
    |--------------------------------------------------------------------------
    */


    private Security security;



    /*
    |--------------------------------------------------------------------------
    | PLATFORM STATUS
    |--------------------------------------------------------------------------
    */


    private Platform platform;



    /*
    |--------------------------------------------------------------------------
    | DOCUMENT INTELLIGENCE
    |--------------------------------------------------------------------------
    */


    private List<DocumentMetric> documentMetrics;



    /*
    |--------------------------------------------------------------------------
    | AUDIT TRAIL
    |--------------------------------------------------------------------------
    */


    private List<AuditLog> auditLogs;



    /*
    |--------------------------------------------------------------------------
    | SYSTEM ALERTS
    |--------------------------------------------------------------------------
    */


    private List<AlertItem> alerts;







    /**
     * =========================================================================
     * EXECUTIVE DASHBOARD METRICS
     * =========================================================================
     */


    @Data
    @Builder
    public static class Executive {


        private long activeUsers;


        private double activeUsersChange;



        private long activeCases;


        private double activeCasesChange;



        private double monthlyRevenue;


        private double revenueChange;



        private double aiAccuracy;


        private double aiAccuracyChange;


    }









    /**
     * =========================================================================
     * IMMIGRATION OPERATIONS
     * =========================================================================
     */


    @Data
    @Builder
    public static class Operations {


        private long pendingApplications;


        private long approvedCases;


        private long rejectedApplications;


        private long awaitingDocuments;


        private long slaBreaches;


    }









    /**
     * =========================================================================
     * AI INTELLIGENCE
     * =========================================================================
     */


    @Data
    @Builder
    public static class AI {


        private long requestsProcessed;


        private double accuracy;


        private double hallucinationRate;


        private String averageResponseTime;


        private int activeModels;


    }









    /**
     * =========================================================================
     * SECURITY OPERATIONS
     * =========================================================================
     */


    @Data
    @Builder
    public static class Security {


        private long alerts;


        private long blockedAttacks;


        private long failedLogins;



        private String status;


    }









    /**
     * =========================================================================
     * PLATFORM HEALTH
     * =========================================================================
     */


    @Data
    @Builder
    public static class Platform {


        private String status;



        private double uptime;



        private int aiModels;



        private int countriesCovered;


    }









    /**
     * =========================================================================
     * DOCUMENT AI METRICS
     * =========================================================================
     */


    @Data
    @Builder
    public static class DocumentMetric {


        private String title;



        private String value;


    }









    /**
     * =========================================================================
     * AUDIT LOG ENTRY
     * =========================================================================
     */


    @Data
    @Builder
    public static class AuditLog {


        private Long id;



        private String message;



        private String time;


    }









    /**
     * =========================================================================
     * SYSTEM ALERT
     * =========================================================================
     */


    @Data
    @Builder
    public static class AlertItem {


        private Long id;



        private String message;


    }




}