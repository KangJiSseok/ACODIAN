import type { NotificationItem } from "../_types/notification.types"

export function resolveNotificationDeepLink(notification: NotificationItem) {
  if (notification.referenceType === "WORKLOG" && notification.referenceId) {
    return `/worklog/detail/${notification.referenceId}`
  }

  return "/notification"
}
