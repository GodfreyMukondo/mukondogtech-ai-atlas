package com.godfrey.ai_immigration_document_analyzer.dto.response;

import com.godfrey.ai_immigration_document_analyzer.entity.Notification;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * NOTIFICATION RESPONSE DTO
 * ============================================================================
 *
 * Public API representation of a notification.
 * ============================================================================
 */
public record NotificationResponse(

        Long id,

        String type,

        String title,

        String message,

        String link,

        boolean read,

        LocalDateTime createdAt

) {

    /**
     * Create API response from entity.
     */
    public static NotificationResponse from(
            Notification notification
    ) {

        if (notification == null) {

            throw new IllegalArgumentException(
                    "Notification cannot be null."
            );
        }

        return new NotificationResponse(

                notification.getId(),

                notification.getType(),

                notification.getTitle(),

                notification.getMessage(),

                notification.getLink(),

                Boolean.TRUE.equals(
                        notification.getRead()
                ),

                notification.getCreatedAt()
        );
    }
}
