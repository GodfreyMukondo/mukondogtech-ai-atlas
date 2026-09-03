package com.godfrey.ai_immigration_document_analyzer.repository;

import com.godfrey.ai_immigration_document_analyzer.entity.AnalyticsLog;
import com.godfrey.ai_immigration_document_analyzer.analytics.projection.EventStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalyticsRepository extends JpaRepository<AnalyticsLog, Long> {

    /**
     * Event count grouped by type (dashboard)
     */
    @Query("""
        SELECT a.eventType AS eventType,
               COUNT(a) AS total
        FROM AnalyticsLog a
        GROUP BY a.eventType
    """)
    List<EventStats> countEventsByType();

    /**
     * Events in time range (analytics engine)
     */
    List<AnalyticsLog> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );
}