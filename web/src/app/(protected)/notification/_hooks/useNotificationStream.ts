"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  selectAccessToken,
  selectIsAuthenticated,
  useAuthStore,
} from "@/app/_common/store/auth.store";
import { subscribeNotificationStream } from "../_service/notification-stream.service";
import { notificationKeys } from "./useNotificationList";

export function useNotificationStream() {
  const queryClient = useQueryClient();
  const accessToken = useAuthStore(selectAccessToken);
  const isAuthenticated = useAuthStore(selectIsAuthenticated);

  useEffect(() => {
    if (!isAuthenticated || !accessToken) {
      return;
    }

    const subscription = subscribeNotificationStream({
      accessToken,
      onNotification: () => {
        void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
      },
    });

    return () => {
      subscription.close();
    };
  }, [accessToken, isAuthenticated, queryClient]);
}
