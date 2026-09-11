package com.godfrey.ai_immigration_document_analyzer.repository;

import com.godfrey.ai_immigration_document_analyzer.entity.ChatLog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * CHAT LOG REPOSITORY
 * ============================================================================
 */
@Repository
public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {

    long countByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    @Query(
            "SELECT AVG(c.responseTimeMs) FROM ChatLog c"
    )
    Double averageResponseTimeMs();
}
