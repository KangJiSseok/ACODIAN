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

  // 부서장은 전체 알림을 확인할 수 있고, 일반 사용자는 본인 알림만 노출합니다.
  if (user.role === "DIRECTOR") return items

  return items.filter((notification) => notification.userId === user.id)
}

// API 연동 전까지 mock DB를 단일 진입점으로 다루기 위한 알림 서비스입니다.
export const notificationService = {
  list(): NotificationItem[] {
    // 최신 알림이 화면 상단에 오도록 조회 시점에만 정렬합니다.
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
    // mock DB 구독자에게 변경을 알려 GNB 배지와 알림 목록을 즉시 갱신합니다.
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

    // 현재 사용자의 미읽음 알림이 모두 읽음 처리된 뒤 화면 상태를 동기화합니다.
    notifyMockDb()
  },
}
