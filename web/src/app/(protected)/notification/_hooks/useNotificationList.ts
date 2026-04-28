"use client"

import { useEffect, useState, useSyncExternalStore } from "react"
import { subscribeMockDb } from "../../worklog/_mock/worklog.mock"
import { useAuth } from "../../worklog/_hooks/useAuth"
import {
  getVisibleNotifications,
  notificationService,
} from "../_service/notification.service"

function subscribeClientReady() {
  return () => undefined
}

function getClientReadySnapshot() {
  return true
}

function getServerReadySnapshot() {
  return false
}

export function useNotificationList() {
  const { user } = useAuth()
  const isClientReady = useSyncExternalStore(
    subscribeClientReady,
    getClientReadySnapshot,
    getServerReadySnapshot,
  )
  const [, setVersion] = useState(0)

  useEffect(
    () => subscribeMockDb(() => setVersion((current) => current + 1)),
    [],
  )

  const notifications = isClientReady
    ? getVisibleNotifications(user, notificationService.list())
    : []
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
