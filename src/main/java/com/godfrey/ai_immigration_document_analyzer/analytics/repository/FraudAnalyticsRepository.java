package com.godfrey.ai_immigration_document_analyzer.analytics.repository;

import com.godfrey.ai_immigration_document_analyzer.analytics.entity.FraudAnalyticsEntity;
import com.godfrey.ai_immigration_document_analyzer.analytics.projection.FraudRuleStats;
import com.godfrey.ai_immigration_document_analyzer.analytics.projection.FraudScoreStats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**

 * Fraud analytics repository for dashboards and AI reporting.
 */
@Repository
public interface FraudAnalyticsRepository extends JpaRepository<FraudAnalyticsEntity, Long> {

    /**

     * Fraud cases grouped by rule (for analytics dashboards)
     */
    @Query("""
    SELECT f.ruleId AS ruleId,
    COUNT(f) AS total
    FROM FraudAnalyticsEntity f
    WHERE f.fraudFlag = true
    GROUP BY f.ruleId
    ORDER BY COUNT(f) DESC
    """)
    List<FraudRuleStats> getFraudCountByRule();

    /**

     * Average fraud score in system
     */
    @Query("""
    SELECT COALESCE(AVG(f.fraudScore), 0)
    FROM FraudAnalyticsEntity f
    """)
    Double getAverageFraudScore();

    /**

     * Fraud records (paginated for production safety)
     */
    List<FraudAnalyticsEntity> findByFraudFlagTrue(Pageable pageable);

    /**

     * Fast count for dashboards (NO entity loading)
     */
    long countByFraudFlagTrue();

    /**

     * Fraud risk distribution (AI analytics dashboard)
     */
    @Query("""
    SELECT
    CASE
    WHEN f.fraudScore < 30 THEN 'LOW'
    WHEN f.fraudScore BETWEEN 30 AND 70 THEN 'MEDIUM'
    ELSE 'HIGH'
    END AS riskLevel,
    COUNT(f) AS count
    FROM FraudAnalyticsEntity f
    GROUP BY
    CASE
    WHEN f.fraudScore < 30 THEN 'LOW'
    WHEN f.fraudScore BETWEEN 30 AND 70 THEN 'MEDIUM'
    ELSE 'HIGH'
    END
    """)
    List<FraudScoreStats> getFraudRiskDistribution();
}
