import type { NotificationItem } from "../_types/notification.types"

export function resolveNotificationDeepLink(notification: NotificationItem) {
  return notification.deepLink || "/notification"
}
