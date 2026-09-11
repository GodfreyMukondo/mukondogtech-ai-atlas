package com.godfrey.ai_immigration_document_analyzer.service;

import com.godfrey.ai_immigration_document_analyzer.dto.response.NotificationResponse;
import com.godfrey.ai_immigration_document_analyzer.entity.Notification;
import com.godfrey.ai_immigration_document_analyzer.entity.Role;
import com.godfrey.ai_immigration_document_analyzer.entity.User;
import com.godfrey.ai_immigration_document_analyzer.exception.ResourceNotFoundException;
import com.godfrey.ai_immigration_document_analyzer.repository.NotificationRepository;
import com.godfrey.ai_immigration_document_analyzer.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * ============================================================================
 * NOTIFICATION SERVICE
 * ============================================================================
 *
 * Central application service for creating and retrieving in-app
 * notifications for the authenticated user.
 *
 * SECURITY MODEL
 * ============================================================================
 *
 * Every read/update operation is scoped to the authenticated user ID passed
 * in by the caller. This service never trusts a user ID supplied by the
 * frontend directly.
 * ============================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private final UserRepository userRepository;

    // =========================================================================
    // CREATE
    // =========================================================================

    /**
     * Creates a notification for a user.
     *
     * This is called internally by other services (document processing,
     * fraud detection, etc.) and never directly from a controller, since
     * notifications must always originate from a trusted server-side event.
     */
    @Transactional
    public void notify(
            Long userId,
            String type,
            String title,
            String message,
            String link
    ) {

        if (userId == null || userId <= 0) {

            log.warn(
                    "Skipped notification creation with invalid userId | type={}",
                    type
            );

            return;
        }

        if (!hasText(title)) {

            log.warn(
                    "Skipped notification creation with empty title | userId={} | type={}",
                    userId,
                    type
            );

            return;
        }

        try {

            Notification notification =
                    Notification.builder()
                            .userId(userId)
                            .type(hasText(type) ? type.trim().toUpperCase() : "GENERAL")
                            .title(title.trim())
                            .message(message != null ? message.trim() : null)
                            .link(link != null ? link.trim() : null)
                            .read(false)
                            .build();

            notificationRepository.save(notification);

            log.debug(
                    "Notification created | userId={} | type={}",
                    userId,
                    type
            );

        } catch (DataAccessException exception) {

            /*
             * A notification is a best-effort, secondary side effect.
             *
             * A failure to persist a notification must never fail the
             * primary operation (e.g. a document upload) that triggered it.
             */
            log.error(
                    "Failed to persist notification | userId={} | type={}",
                    userId,
                    type,
                    exception
            );
        }
    }

    // =========================================================================
    // NOTIFY ADMINS
    // =========================================================================

    /**
     * Creates the same notification for every enabled administrator.
     *
     * Used for events an admin needs to act on (a new application was
     * submitted, an applicant updated their profile) rather than events
     * scoped to a single known user.
     */
    @Transactional
    public void notifyAdmins(
            String type,
            String title,
            String message,
            String link
    ) {

        List<User> admins =
                userRepository.findByRoleAndEnabledTrue(Role.ADMIN);

        for (User admin : admins) {

            notify(
                    admin.getId(),
                    type,
                    title,
                    message,
                    link
            );
        }
    }

    // =========================================================================
    // GET USER NOTIFICATIONS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(
            Long userId
    ) {

        validateUserId(userId);

        List<Notification> notifications =
                notificationRepository
                        .findTop50ByUserIdOrderByCreatedAtDesc(userId);

        if (notifications == null || notifications.isEmpty()) {
            return List.of();
        }

        return notifications
                .stream()
                .filter(Objects::nonNull)
                .map(NotificationResponse::from)
                .toList();
    }

    // =========================================================================
    // GET UNREAD COUNT
    // =========================================================================

    @Transactional(readOnly = true)
    public long getUnreadCount(
            Long userId
    ) {

        validateUserId(userId);

        return notificationRepository
                .countByUserIdAndReadFalse(userId);
    }

    // =========================================================================
    // MARK AS READ
    // =========================================================================

    @Transactional
    public void markAsRead(
            Long notificationId,
            Long userId
    ) {

        validateUserId(userId);

        if (notificationId == null || notificationId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid notification ID."
            );
        }

        notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Notification not found."
                        )
                );

        notificationRepository.markAsRead(notificationId, userId);
    }

    // =========================================================================
    // MARK ALL AS READ
    // =========================================================================

    @Transactional
    public void markAllAsRead(
            Long userId
    ) {

        validateUserId(userId);

        notificationRepository.markAllAsRead(userId);
    }

    // =========================================================================
    // VALIDATION
    // =========================================================================

    private void validateUserId(Long userId) {

        if (userId == null || userId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid authenticated user ID."
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
