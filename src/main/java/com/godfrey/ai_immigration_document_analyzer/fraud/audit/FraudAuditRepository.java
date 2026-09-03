package com.godfrey.ai_immigration_document_analyzer.fraud.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface FraudAuditRepository extends JpaRepository<FraudAuditEntity, Long> {

    /**
     * FIXED (matches entity field)
     */
    List<FraudAuditEntity> findByFraudFlagTrue();

    List<FraudAuditEntity> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    Page<FraudAuditEntity> findAll(Pageable pageable);

    /**
     * FIXED JPQL query
     */
    @Query("""
        SELECT f FROM FraudAuditEntity f
        WHERE f.fraudFlag = true
        ORDER BY f.createdAt DESC
    """)
    List<FraudAuditEntity> findAllFraudCases();
}