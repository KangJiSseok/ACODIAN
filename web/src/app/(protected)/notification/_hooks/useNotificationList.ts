"use client";

import { useQuery } from "@tanstack/react-query";
import {
  selectAuthUser,
  selectIsAuthenticated,
  useAuthStore,
} from "@/app/_common/store/auth.store";
import { notificationService } from "../_service/notification.service";
import type { GetNotificationsParams } from "../_types/notification.types";

export const notificationKeys = {
  all: ["notifications"] as const,
  user: (userId: number | null | undefined) =>
    [...notificationKeys.all, "user", userId ?? "anonymous"] as const,
  list: (
    userId: number | null | undefined,
    params: GetNotificationsParams = {},
  ) => [...notificationKeys.user(userId), "list", params] as const,
};

export function useNotificationList(params: GetNotificationsParams = {}) {
  const user = useAuthStore(selectAuthUser);
  const isAuthenticated = useAuthStore(selectIsAuthenticated);
  const userId = user?.userId;

  const query = useQuery({
    queryKey: notificationKeys.list(userId, params),
    queryFn: () => notificationService.getNotifications(params),
    enabled: isAuthenticated && Number.isFinite(userId),
    staleTime: 0,
    refetchOnMount: "always",
  });

  return {
    ...query,
    notificationPage: query.data,
    notifications: query.data?.items ?? [],
  };
}

export function useNotificationCenter() {
  const notificationListQuery = useNotificationList({
    page: 1,
    pageSize: 100,
  });
  const unreadCountQuery = useNotificationList({
    isRead: false,
    page: 1,
    pageSize: 1,
  });

  return {
    ...notificationListQuery,
    unreadCount: unreadCountQuery.notificationPage?.totalCount ?? 0,
    notificationCenterNotifications: notificationListQuery.notifications,
  };
}
