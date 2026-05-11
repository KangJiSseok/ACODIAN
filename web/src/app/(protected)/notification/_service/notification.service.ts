import { apiClient } from "@/app/_common/service/api-client";
import type { EmptyResponse, PageResponse } from "@/app/_common/types/api.types";
import type {
  GetNotificationsParams,
  MarkAllNotificationsReadResponse,
  NotificationApiItem,
  NotificationItem,
  NotificationPageResponse,
} from "../_types/notification.types";

function normalizeNotification(item: NotificationApiItem): NotificationItem {
  return {
    ...item,
    id: item.notificationId,
    type: item.notificationType,
  };
}

function buildNotificationParams(params: GetNotificationsParams) {
  return {
    isRead: params.isRead,
    departmentId: params.departmentId,
    teamId: params.teamId,
    page: params.page,
    pageSize: params.pageSize,
  };
}

export const notificationService = {
  async getNotifications(
    params: GetNotificationsParams = {},
  ): Promise<NotificationPageResponse> {
    const response = await apiClient.get<PageResponse<NotificationApiItem>>(
      "/notifications/me",
      { params: buildNotificationParams(params) },
    );

    return {
      ...response,
      items: response.items.map(normalizeNotification),
    };
  },

  markRead: (id: number) =>
    apiClient.patch<EmptyResponse>(`/notifications/${id}/read`),

  markAllRead: () =>
    apiClient.patch<MarkAllNotificationsReadResponse>("/notifications/me/read-all"),
};
