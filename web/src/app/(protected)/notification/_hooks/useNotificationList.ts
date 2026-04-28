"use client"

import { useEffect, useState } from "react"
import { subscribeMockDb } from "../../worklog/_mock/worklog.mock"
import { useAuth } from "../../worklog/_hooks/useAuth"
import {
  getVisibleNotifications,
  notificationService,
} from "../_service/notification.service"

export function useNotificationList() {
  const { user } = useAuth()
  const [, setVersion] = useState(0)

  useEffect(
    () => subscribeMockDb(() => setVersion((current) => current + 1)),
    [],
  )

  const notifications = getVisibleNotifications(user, notificationService.list())
  const unreadCount = notifications.filter((notification) => !notification.isRead).length
  const recentUnreadNotifications = notifications
    .filter((notification) => !notification.isRead)
    .slice(0, 3)

  return {
    notifications,
    unreadCount,
    readCount: notifications.length - unreadCount,
    recentUnreadNotifications,
  }
}
