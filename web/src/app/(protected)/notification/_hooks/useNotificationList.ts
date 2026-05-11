"use client";

import { useQuery } from "@tanstack/react-query";
import { selectIsAuthenticated, useAuthStore } from "@/app/_common/store/auth.store";
import { notificationService } from "../_service/notification.service";
import type { GetNotificationsParams } from "../_types/notification.types";

export const notificationKeys = {
  all: ["notifications"] as const,
  list: (params: GetNotificationsParams = {}) =>
    [...notificationKeys.all, "list", params] as const,
};

export function useNotificationList(params: GetNotificationsParams = {}) {
  const isAuthenticated = useAuthStore(selectIsAuthenticated);

  const query = useQuery({
    queryKey: notificationKeys.list(params),
    queryFn: () => notificationService.getNotifications(params),
    enabled: isAuthenticated,
  });

  return {
    ...query,
    notificationPage: query.data,
    notifications: query.data?.items ?? [],
  };
}

export function useNotificationCenter() {
  const query = useNotificationList({
    isRead: false,
    page: 1,
    pageSize: 3,
  });

  return {
    ...query,
    unreadCount: query.notificationPage?.totalCount ?? 0,
    recentUnreadNotifications: query.notifications,
  };
}
