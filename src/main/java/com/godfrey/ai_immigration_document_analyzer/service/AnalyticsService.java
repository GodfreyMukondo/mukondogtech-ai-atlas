package com.godfrey.ai_immigration_document_analyzer.service;


import com.godfrey.ai_immigration_document_analyzer.analytics.projection.DashboardMetrics;
import com.godfrey.ai_immigration_document_analyzer.analytics.projection.EventStats;
import com.godfrey.ai_immigration_document_analyzer.analytics.projection.FraudRuleStats;
import com.godfrey.ai_immigration_document_analyzer.analytics.projection.FraudScoreStats;

import com.godfrey.ai_immigration_document_analyzer.analytics.repository.FraudAnalyticsRepository;

import com.godfrey.ai_immigration_document_analyzer.repository.AnalyticsRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.DocumentRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Collections;
import java.util.List;





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