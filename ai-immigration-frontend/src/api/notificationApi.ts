import API from "./axios";

import type { Notification } from "../types/notification";

/**
 * ============================================================================
 * NOTIFICATION API CLIENT
 * ============================================================================
 *
 * Centralized HTTP client for the authenticated user's in-app notifications.
 *
 * Endpoints:
 * ----------------------------------------------------------------------------
 *
 * GET   /api/notifications
 * GET   /api/notifications/unread-count
 * PATCH /api/notifications/{id}/read
 * PATCH /api/notifications/read-all
 * ============================================================================
 */

const NOTIFICATIONS_BASE_PATH = "/notifications";

interface ApiResponse<T> {
  data: T;
  message?: string;
  success?: boolean;
}

interface UnreadCountResponse {
  unreadCount: number;
}

const unwrapApiResponse = <T>(
  responseData: ApiResponse<T> | T,
): T => {
  if (
    responseData !== null &&
    typeof responseData === "object" &&
    "data" in responseData
  ) {
    return (responseData as ApiResponse<T>).data;
  }

  return responseData as T;
};

export const getNotificationsApi = async (): Promise<Notification[]> => {
  const response = await API.get<ApiResponse<Notification[]> | Notification[]>(
    NOTIFICATIONS_BASE_PATH,
  );

  const notifications = unwrapApiResponse<Notification[]>(response.data);

  return Array.isArray(notifications) ? notifications : [];
};

export const getUnreadNotificationCountApi = async (): Promise<number> => {
  const response = await API.get<
    ApiResponse<UnreadCountResponse> | UnreadCountResponse
  >(`${NOTIFICATIONS_BASE_PATH}/unread-count`);

  const payload = unwrapApiResponse<UnreadCountResponse>(response.data);

  return typeof payload?.unreadCount === "number" ? payload.unreadCount : 0;
};

export const markNotificationAsReadApi = async (
  id: number,
): Promise<void> => {
  await API.patch(`${NOTIFICATIONS_BASE_PATH}/${id}/read`);
};

export const markAllNotificationsAsReadApi = async (): Promise<void> => {
  await API.patch(`${NOTIFICATIONS_BASE_PATH}/read-all`);
};

const notificationApi = {
  getNotifications: getNotificationsApi,
  getUnreadCount: getUnreadNotificationCountApi,
  markAsRead: markNotificationAsReadApi,
  markAllAsRead: markAllNotificationsAsReadApi,
};

export default notificationApi;
