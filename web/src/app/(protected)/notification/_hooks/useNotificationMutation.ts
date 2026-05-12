"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { notificationService } from "../_service/notification.service";
import { notificationKeys } from "./useNotificationList";

export function useNotificationMutation() {
  const queryClient = useQueryClient();

  const markReadMutation = useMutation({
    mutationFn: notificationService.markRead,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });

  const markAllReadMutation = useMutation({
    mutationFn: notificationService.markAllRead,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });

  return {
    markRead: (id: number) => {
      markReadMutation.mutate(id);
    },
    markAllRead: () => {
      markAllReadMutation.mutate();
    },
    isMarkingRead: markReadMutation.isPending,
    isMarkingAllRead: markAllReadMutation.isPending,
  };
}
