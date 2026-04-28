"use client"

import { useNotificationList } from "@/app/(protected)/notification/_hooks"

export function useUnreadNotificationCount() {
  const { unreadCount } = useNotificationList()

  return unreadCount
}
