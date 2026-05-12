"use client";

import Link from "next/link";
import { CheckCircle2, Clock3 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import { cn } from "@/lib/utils";
import type { NotificationItem } from "../_types/notification.types";
import { getNotificationTypeLabel } from "../_utils/notificationLabel";
import { resolveNotificationDeepLink } from "../_utils/resolveNotificationDeepLink";

const DUE_DATE_PATTERN = /\s*마감일\s*:\s*(\d{4}-\d{2}-\d{2})\s*$/;

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
      {notifications.map((notification) => {
        const content = getNotificationContentWithoutDueDate(
          notification.content,
        );
        const dueDateLabel = getNotificationDueDateLabel(notification.content);

        return (
          <CardSpotlight
            key={notification.id}
            className={cn(
              "rounded-[24px] transition-transform duration-300 hover:-translate-y-1",
              !notification.isRead && "ring-1 ring-primary/25",
            )}
          >
            <div className="flex flex-col gap-4 p-5">
              <div className="min-w-0 flex-1 space-y-3">
                <div className="space-y-2.5">
                  <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                    <p className="text-[18px] font-semibold tracking-[-0.03em] text-foreground">
                      {notification.title}
                    </p>
                    <span className="inline-flex items-center gap-1 text-xs font-semibold text-[color:var(--warning)]">
                      <Clock3 className="size-3.5" />
                      {getNotificationTypeLabel(notification.type)}
                    </span>
                    <span
                      className={cn(
                        "inline-flex items-center gap-1 text-xs font-semibold",
                        notification.isRead
                          ? "text-[color:var(--success)]"
                          : "text-primary",
                      )}
                    >
                      <CheckCircle2 className="size-3.5" />
                      {getNotificationReadLabel(notification)}
                    </span>
                  </div>

                  {content ? (
                    <p className="line-clamp-2 text-sm leading-7 text-muted-foreground">
                      {content}
                    </p>
                  ) : null}
                </div>

              </div>

              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                {dueDateLabel ? (
                  <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                    <span>{dueDateLabel}</span>
                  </div>
                ) : (
                  <span aria-hidden="true" />
                )}
                <div className="flex flex-wrap justify-end gap-2">
                  {!notification.isRead ? (
                    <Button
                      type="button"
                      variant="outline"
                      className="h-9 rounded-2xl px-4 text-sm"
                      onClick={() => onMarkRead(notification.id)}
                    >
                      읽음 처리
                    </Button>
                  ) : null}
                  <Button
                    asChild
                    variant="default"
                    className="h-10 min-w-32 px-6 text-sm font-semibold !text-primary-foreground shadow-[0_12px_28px_-20px_rgba(37,99,235,0.7)] hover:!text-primary-foreground"
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
            </div>
          </CardSpotlight>
        );
      })}
    </div>
  );
}

function getNotificationContentWithoutDueDate(content?: string | null) {
  return content?.replace(DUE_DATE_PATTERN, "").trim() ?? "";
}

function getNotificationDueDateLabel(content?: string | null) {
  const dueDate = content?.match(DUE_DATE_PATTERN)?.[1];
  return dueDate ? `마감일 : ${dueDate}` : "";
}

function getNotificationReadLabel(notification: NotificationItem) {
  return notification.isRead ? "읽음" : "미읽음";
}
