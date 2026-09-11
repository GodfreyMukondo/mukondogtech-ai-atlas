package com.godfrey.ai_immigration_document_analyzer.repository;

import com.godfrey.ai_immigration_document_analyzer.entity.Notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * NOTIFICATION REPOSITORY
 * ============================================================================
 */
@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // =========================================================================
    // USER NOTIFICATIONS
    // =========================================================================

    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    /**
     * The authenticated admin's own most recent notifications, used as a
     * real recent-activity feed for the admin dashboard's audit log
     * section.
     */
    List<Notification> findTop5ByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    Optional<Notification> findByIdAndUserId(
            Long id,
            Long userId
    );

    long countByUserIdAndReadFalse(
            Long userId
    );

    void deleteByUserId(
            Long userId
    );

    // =========================================================================
    // MARK AS READ
    // =========================================================================

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Transactional
    @Query("""
            UPDATE Notification n
            SET n.read = true, n.readAt = CURRENT_TIMESTAMP
            WHERE n.id = :id AND n.userId = :userId
            """)
    int markAsRead(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

    @Modifying(
            clearAutomatically = true,
            flushAutomatically = true
    )
    @Transactional
    @Query("""
            UPDATE Notification n
            SET n.read = true, n.readAt = CURRENT_TIMESTAMP
            WHERE n.userId = :userId AND n.read = false
            """)
    int markAllAsRead(
            @Param("userId") Long userId
    );
}
