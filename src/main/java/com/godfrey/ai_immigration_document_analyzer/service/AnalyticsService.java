package com.godfrey.ai_immigration_document_analyzer.service;


import com.godfrey.ai_immigration_document_analyzer.analytics.projection.DashboardMetrics;
import com.godfrey.ai_immigration_document_analyzer.analytics.projection.EventStats;
import com.godfrey.ai_immigration_document_analyzer.analytics.projection.FraudRuleStats;
import com.godfrey.ai_immigration_document_analyzer.analytics.projection.FraudScoreStats;

import com.godfrey.ai_immigration_document_analyzer.analytics.repository.FraudAnalyticsRepository;

import com.godfrey.ai_immigration_document_analyzer.dto.response.AnalyticsOverviewResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.AnalyticsPointResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.ChatAnalyticsResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.DocumentAnalyticsResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.ProcessingAnalyticsResponse;

import com.godfrey.ai_immigration_document_analyzer.repository.AnalyticsRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.ChatLogRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;





/**
 * ============================================================
 * ANALYTICS SERVICE
 * ============================================================
 *
 * Responsible for:
 *
 * - Admin dashboard metrics
 * - User statistics
 * - Document statistics
 * - Fraud analytics
 * - Platform events
 *
 * Used by:
 *
 * - Admin dashboard
 * - Analytics controller
 * - Reporting module
 *
 * ============================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {





    private final UserRepository userRepository;


    private final DocumentRepository documentRepository;


    private final FraudAnalyticsRepository fraudAnalyticsRepository;


    private final AnalyticsRepository analyticsRepository;


    private final ChatLogRepository chatLogRepository;


    private static final int TREND_WINDOW_DAYS = 7;









    /**
     * Generates complete dashboard statistics.
     *
     * @return DashboardMetrics
     */
    public DashboardMetrics getDashboardMetrics() {


        try {


            log.debug(
                    "Generating dashboard analytics metrics"
            );



            long totalUsers =
                    userRepository.count();




            long activeUsers =
                    userRepository.countByEnabledTrue();




            long totalDocuments =
                    documentRepository.count();




            long documentsToday =
                    documentRepository.countToday();




            long fraudCases =
                    fraudAnalyticsRepository.countByFraudFlagTrue();




            Double averageFraudScore =
                    fraudAnalyticsRepository.getAverageFraudScore();




            double approvalRate =
                    calculateApprovalRate(
                            totalDocuments,
                            fraudCases
                    );







            return DashboardMetrics.builder()


                    .totalUsers(
                            totalUsers
                    )


                    .activeUsers(
                            activeUsers
                    )


                    .totalDocuments(
                            totalDocuments
                    )


                    .documentsToday(
                            documentsToday
                    )


                    .fraudCases(
                            fraudCases
                    )


                    .averageFraudScore(
                            safeDouble(
                                    averageFraudScore
                            )
                    )


                    .approvalRate(
                            approvalRate
                    )


                    .build();





        } catch(Exception exception){



            log.error(
                    "Failed to generate dashboard metrics",
                    exception
            );


            throw exception;

        }

    }




    /**
     * =========================================================================
     * ANALYTICS OVERVIEW
     * =========================================================================
     *
     * Backs the admin Analytics page (GET /api/analytics/overview and its
     * three sub-resources).
     */
    public AnalyticsOverviewResponse getAnalyticsOverview() {

        return new AnalyticsOverviewResponse(
                getDocumentAnalyticsOverview(),
                getChatAnalyticsOverview(),
                getProcessingAnalyticsOverview()
        );
    }




    public DocumentAnalyticsResponse getDocumentAnalyticsOverview() {

        long totalDocuments =
                documentRepository.count();

        long uploadedToday =
                documentRepository.countToday();

        LocalDateTime startOfMonth =
                LocalDate.now()
                        .withDayOfMonth(1)
                        .atStartOfDay();

        long uploadedThisMonth =
                documentRepository.countByUploadedAtBetween(
                        startOfMonth,
                        LocalDateTime.now()
                );

        List<AnalyticsPointResponse> uploadTrend =
                buildDailyTrend(
                        documentRepository::countByUploadedAtBetween
                );

        return new DocumentAnalyticsResponse(
                totalDocuments,
                uploadedToday,
                uploadedThisMonth,
                uploadTrend
        );
    }




    public ChatAnalyticsResponse getChatAnalyticsOverview() {

        long totalQueries =
                chatLogRepository.count();

        LocalDateTime startOfToday =
                LocalDate.now().atStartOfDay();

        long queriesToday =
                chatLogRepository.countByCreatedAtBetween(
                        startOfToday,
                        LocalDateTime.now()
                );

        double averageResponseTime =
                safeDouble(
                        chatLogRepository.averageResponseTimeMs()
                );

        List<AnalyticsPointResponse> queryTrend =
                buildDailyTrend(
                        chatLogRepository::countByCreatedAtBetween
                );

        return new ChatAnalyticsResponse(
                totalQueries,
                queriesToday,
                averageResponseTime,
                queryTrend
        );
    }




    public ProcessingAnalyticsResponse getProcessingAnalyticsOverview() {

        long totalDocuments =
                documentRepository.count();

        long fraudCases =
                fraudAnalyticsRepository.countByFraudFlagTrue();

        double accuracyRate =
                calculateApprovalRate(
                        totalDocuments,
                        fraudCases
                );

        return new ProcessingAnalyticsResponse(
                /*
                 * No per-document processing duration is recorded
                 * anywhere in the application - see
                 * ProcessingAnalyticsResponse's javadoc.
                 */
                0,
                documentRepository.countCompletedDocuments(),
                documentRepository.countFailedDocuments(),
                accuracyRate
        );
    }




    /**
     * Builds a {@link #TREND_WINDOW_DAYS}-day daily count trend, oldest
     * day first, using whichever repository counter is supplied.
     */
    private List<AnalyticsPointResponse> buildDailyTrend(
            BiFunction<LocalDateTime, LocalDateTime, Long> dailyCounter
    ) {

        DateTimeFormatter labelFormat =
                DateTimeFormatter.ofPattern("MMM d");

        LocalDate today =
                LocalDate.now();

        List<AnalyticsPointResponse> points =
                new ArrayList<>();

        for (int daysAgo = TREND_WINDOW_DAYS - 1; daysAgo >= 0; daysAgo--) {

            LocalDate day =
                    today.minusDays(daysAgo);

            LocalDateTime start =
                    day.atStartOfDay();

            LocalDateTime end =
                    day.plusDays(1).atStartOfDay();

            long count =
                    dailyCounter.apply(start, end);

            points.add(
                    new AnalyticsPointResponse(
                            day.format(labelFormat),
                            count
                    )
            );
        }

        return points;
    }









    /**
     * Returns fraud cases grouped by rule.
     *
     * @return fraud rule statistics
     */
    public List<FraudRuleStats> getFraudByRule() {



        try {


            List<FraudRuleStats> results =
                    fraudAnalyticsRepository
                            .getFraudCountByRule();



            return emptyIfNull(results);



        } catch(Exception exception){


            log.error(
                    "Failed to load fraud rule statistics",
                    exception
            );


            return Collections.emptyList();

        }


    }









    /**
     * Returns fraud risk distribution.
     *
     * Example:
     *
     * LOW
     * MEDIUM
     * HIGH
     *
     */
    public List<FraudScoreStats> getFraudRiskDistribution() {



        try {


            List<FraudScoreStats> results =
                    fraudAnalyticsRepository
                            .getFraudRiskDistribution();



            return emptyIfNull(results);



        } catch(Exception exception){


            log.error(
                    "Failed to load fraud risk distribution",
                    exception
            );


            return Collections.emptyList();

        }


    }









    /**
     * Returns platform activity statistics.
     *
     * Example:
     *
     * LOGIN
     * DOCUMENT_UPLOAD
     * AI_ANALYSIS
     *
     */
    public List<EventStats> getEventStats() {



        try {


            List<EventStats> results =
                    analyticsRepository
                            .countEventsByType();



            return emptyIfNull(results);



        } catch(Exception exception){


            log.error(
                    "Failed to load event statistics",
                    exception
            );


            return Collections.emptyList();

        }


    }









    /**
     * Calculates document approval percentage.
     *
     * Formula:
     *
     * ((Total Documents - Fraud Cases)
     * / Total Documents) * 100
     *
     */
    private double calculateApprovalRate(

            long totalDocuments,

            long fraudCases

    ) {



        if(totalDocuments <= 0){


            return 0.0;


        }




        long approvedDocuments =
                Math.max(

                        totalDocuments - fraudCases,

                        0

                );





        return Math.round(

                (
                        approvedDocuments
                                *
                                10000.0

                )
                        /
                        totalDocuments


        )
                /
                100.0;


    }









    /**
     * Converts null database values to zero.
     */
    private double safeDouble(

            Double value

    ){


        return value == null
                ?
                0.0
                :
                value;


    }









    /**
     * Prevents null list responses.
     */
    private <T> List<T> emptyIfNull(

            List<T> list

    ){


        return list == null
                ?
                Collections.emptyList()
                :
                list;


    }



}