"use client";

import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import { formatDateTime } from "../../worklog/_utils/worklogFormat";
import type { NotificationItem } from "../_types/notification.types";
import { getNotificationTypeLabel } from "../_utils/notificationLabel";
import { resolveNotificationDeepLink } from "../_utils/resolveNotificationDeepLink";

export function NotificationList({
  notifications,
  onMarkRead,
}: {
  notifications: NotificationItem[];
  onMarkRead: (id: number) => void;
}) {
  if (notifications.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-border/70 px-6 py-12 text-center text-sm text-muted-foreground">
        표시할 알림이 없습니다.
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {notifications.map((notification) => (
        <CardSpotlight
          key={notification.id}
          className="rounded-[24px] transition-transform duration-300 hover:-translate-y-1"
        >
          <div className="flex flex-col gap-4 p-5">
            <div className="min-w-0 space-y-3">
              <div className="flex items-start justify-between gap-3">
                <div className="flex flex-wrap gap-2">
                  <Badge variant={notification.isRead ? "outline" : "default"}>
                    {getNotificationTypeLabel(notification.type)}
                  </Badge>
                  <Badge variant={notification.isRead ? "outline" : "secondary"}>
                    {notification.isRead ? "읽음" : "미읽음"}
                  </Badge>
                </div>
                <span className="text-xs text-muted-foreground">
                  {getNotificationScopeLabel(notification)}
                </span>
              </div>

              <div className="space-y-2">
                <p className="text-[18px] font-semibold tracking-[-0.03em] text-foreground">
                  {notification.title}
                </p>
                {notification.content ? (
                  <p className="text-sm leading-7 text-muted-foreground">
                    {notification.content}
                  </p>
                ) : null}
              </div>

              <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                <span>{formatDateTime(notification.createdAt)}</span>
                {notification.readAt ? (
                  <>
                    <span>/</span>
                    <span>읽음 {formatDateTime(notification.readAt)}</span>
                  </>
                ) : null}
              </div>
            </div>

            <div className="flex flex-wrap justify-end gap-2">
              {!notification.isRead ? (
                <Button
                  type="button"
                  variant="outline"
                  className="h-10 rounded-2xl px-4 text-sm"
                  onClick={() => onMarkRead(notification.id)}
                >
                  읽음 처리
                </Button>
              ) : null}
              <Button
                asChild
                variant="secondary"
                className="h-10 rounded-2xl px-4 text-sm"
              >
                <Link
                  href={resolveNotificationDeepLink(notification)}
                  onClick={() => {
                    if (!notification.isRead) {
                      onMarkRead(notification.id);
                    }
                  }}
                >
                  관련 화면 이동
                </Link>
              </Button>
            </div>
          </div>
        </CardSpotlight>
      ))}
    </div>
  );
}

function getNotificationScopeLabel(notification: NotificationItem) {
  if (notification.teamId) return "팀";
  if (notification.departmentId) return "부서";
  return "개인";
}
