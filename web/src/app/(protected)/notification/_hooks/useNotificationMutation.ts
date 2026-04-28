"use client"

import { useAuth } from "../../worklog/_hooks/useAuth"
import { notificationService } from "../_service/notification.service"

export function useNotificationMutation() {
  const { user } = useAuth()

  return {
    markRead: (id: number) => {
      notificationService.markRead(id)
    },
    markAllRead: () => {
      if (!user) return
      notificationService.markAllRead(user.id)
    },
  }
}
