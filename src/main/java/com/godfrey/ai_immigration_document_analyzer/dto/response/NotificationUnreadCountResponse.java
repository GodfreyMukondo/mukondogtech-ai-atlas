package com.godfrey.ai_immigration_document_analyzer.dto.response;

/**
 * ============================================================================
 * NOTIFICATION UNREAD COUNT RESPONSE DTO
 * ============================================================================
 */
public record NotificationUnreadCountResponse(

        long unreadCount

) {

    public static NotificationUnreadCountResponse of(
            long unreadCount
    ) {

        return new NotificationUnreadCountResponse(
                Math.max(unreadCount, 0)
        );
    }
}
