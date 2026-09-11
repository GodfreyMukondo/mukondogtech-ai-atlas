package com.godfrey.ai_immigration_document_analyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * CHAT LOG ENTITY
 * ============================================================================
 *
 * One row per AI chat exchange, recording usage metrics only.
 *
 * The actual conversation text lives solely in {@link com.godfrey.ai_immigration_document_analyzer.service.ConversationService}'s
 * in-memory, per-session, capped history and is never persisted here - this
 * table exists purely to back real "AI query volume" / "average response
 * time" analytics for the admin dashboard, which otherwise have no data
 * source at all.
 * ============================================================================
 */
@Entity
@Table(
        name = "chat_logs",
        indexes = {
                @Index(
                        name = "idx_chat_log_created_at",
                        columnList = "created_at"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "question_length", nullable = false)
    private int questionLength;

    @Column(name = "response_time_ms", nullable = false)
    private long responseTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
