import {
  notifications,
  notifyMockDb,
} from "../../worklog/_mock/worklog.mock"
import type { AuthUser } from "../../worklog/_store/authStore"
import type { NotificationItem } from "../_types/notification.types"

export function getVisibleNotifications(
  user: AuthUser | null | undefined,
  items: NotificationItem[],
) {
  if (!user) return []
  if (user.role === "DIRECTOR") return items

  return items.filter((notification) => notification.userId === user.id)
}

export const notificationService = {
  list(): NotificationItem[] {
    return [...notifications].sort(
      (left, right) =>
        new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime(),
    )
  },

  markRead(id: number) {
    const target = notifications.find((notification) => notification.id === id)
    if (!target || target.isRead) return target

    target.isRead = true
    target.readAt = new Date().toISOString()
    notifyMockDb()
    return target
  },

  markAllRead(userId: number) {
    notifications
      .filter((notification) => notification.userId === userId && !notification.isRead)
      .forEach((notification) => {
        notification.isRead = true
        notification.readAt = new Date().toISOString()
      })

    notifyMockDb()
  },
}
