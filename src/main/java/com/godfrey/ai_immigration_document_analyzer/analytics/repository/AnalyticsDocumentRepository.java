package com.godfrey.ai_immigration_document_analyzer.analytics.repository;

import com.godfrey.ai_immigration_document_analyzer.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyticsDocumentRepository extends JpaRepository<Document, Long> {

    long countByUserId(Long userId);

    long count();

    /**
     * Documents processed today (dashboard metric)
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM documents d
        WHERE TRUNC(d.uploaded_at) = TRUNC(SYSDATE)
    """, nativeQuery = true)
    long countToday();
}