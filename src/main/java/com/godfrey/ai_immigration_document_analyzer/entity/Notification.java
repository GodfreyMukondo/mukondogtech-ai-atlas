package com.godfrey.ai_immigration_document_analyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * NOTIFICATION ENTITY
 * ============================================================================
 *
 * Represents a single in-app notification delivered to a user, such as a
 * document finishing analysis or a fraud/risk indicator being detected.
 * ============================================================================
 */
@Entity
@Table(
        name = "NOTIFICATIONS",
        indexes = {

                @Index(
                        name = "IDX_NOTIFICATION_USER_ID",
                        columnList = "USER_ID"
                ),

                @Index(
                        name = "IDX_NOTIFICATION_USER_READ",
                        columnList = "USER_ID, IS_READ"
                ),

                @Index(
                        name = "IDX_NOTIFICATION_CREATED_AT",
                        columnList = "CREATED_AT"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(
        onlyExplicitlyIncluded = true
)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(
            name = "ID",
            nullable = false
    )
    private Long id;

    @Column(
            name = "USER_ID",
            nullable = false
    )
    private Long userId;

    @Column(
            name = "TYPE",
            nullable = false,
            length = 50
    )
    private String type;

    @Column(
            name = "TITLE",
            nullable = false,
            length = 255
    )
    private String title;

    @Column(
            name = "MESSAGE",
            length = 1000
    )
    private String message;

    @Column(
            name = "LINK",
            length = 500
    )
    private String link;

    @Builder.Default
    @Column(
            name = "IS_READ",
            nullable = false
    )
    private Boolean read = false;

    @Column(
            name = "CREATED_AT",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "READ_AT"
    )
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (read == null) {
            read = false;
        }
    }
}
