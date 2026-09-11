package com.godfrey.ai_immigration_document_analyzer.controller;

import com.godfrey.ai_immigration_document_analyzer.dto.response.NotificationResponse;
import com.godfrey.ai_immigration_document_analyzer.dto.response.NotificationUnreadCountResponse;
import com.godfrey.ai_immigration_document_analyzer.security.AuthenticatedUser;
import com.godfrey.ai_immigration_document_analyzer.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * ============================================================================
 * NOTIFICATION CONTROLLER
 * ============================================================================
 *
 * REST API for the authenticated user's in-app notifications.
 *
 * Endpoints
 * ----------------------------------------------------------------------------
 *
 * GET   /api/notifications
 * GET   /api/notifications/unread-count
 * PATCH /api/notifications/{notificationId}/read
 * PATCH /api/notifications/read-all
 *
 * Security model
 * ----------------------------------------------------------------------------
 *
 * The authenticated database user is obtained exclusively from
 * {@link AuthenticatedUser}. The frontend must NEVER provide userId.
 * ============================================================================
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // =========================================================================
    // GET NOTIFICATIONS
    // =========================================================================

    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<NotificationResponse>> getNotifications(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
    ) {

        final Long userId = requireAuthenticatedUserId(authenticatedUser);

        final List<NotificationResponse> notifications =
                notificationService.getNotifications(userId);

        return ResponseEntity.ok(notifications);
    }

    // =========================================================================
    // GET UNREAD COUNT
    // =========================================================================

    @GetMapping(
            value = "/unread-count",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<NotificationUnreadCountResponse> getUnreadCount(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
    ) {

        final Long userId = requireAuthenticatedUserId(authenticatedUser);

        final long unreadCount =
                notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(
                NotificationUnreadCountResponse.of(unreadCount)
        );
    }

    // =========================================================================
    // MARK ONE AS READ
    // =========================================================================

    @PatchMapping(
            value = "/{notificationId}/read"
    )
    public ResponseEntity<Void> markAsRead(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable("notificationId")
            Long notificationId
    ) {

        final Long userId = requireAuthenticatedUserId(authenticatedUser);

        notificationService.markAsRead(notificationId, userId);

        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // MARK ALL AS READ
    // =========================================================================

    @PatchMapping(
            value = "/read-all"
    )
    public ResponseEntity<Void> markAllAsRead(

            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser
    ) {

        final Long userId = requireAuthenticatedUserId(authenticatedUser);

        notificationService.markAllAsRead(userId);

        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // AUTHENTICATED USER VALIDATION
    // =========================================================================

    private Long requireAuthenticatedUserId(
            AuthenticatedUser authenticatedUser
    ) {

        if (authenticatedUser == null) {

            log.warn(
                    "Notification endpoint accessed without authenticated principal."
            );

            throw new IllegalStateException(
                    "Authenticated user could not be determined."
            );
        }

        final Long userId = authenticatedUser.getUserId();

        if (userId == null || userId <= 0) {

            log.error(
                    "Authenticated principal contains invalid database user ID."
            );

            throw new IllegalStateException(
                    "Authenticated user could not be determined."
            );
        }

        return userId;
    }
}
