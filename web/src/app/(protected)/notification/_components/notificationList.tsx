"use client";

import Link from "next/link";
import { CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { CardSpotlight } from "@/components/ui/card-spotlight";
import { cn } from "@/lib/utils";
import type { NotificationItem } from "../_types/notification.types";
import { resolveNotificationDeepLink } from "../_utils/resolveNotificationDeepLink";

const TRAILING_NOTIFICATION_DATE_PATTERN =
  /\s*마감일\s*:\s*\d{4}-\d{2}-\d{2}(?:\s*,\s*확인 기준일\s*:\s*\d{4}-\d{2}-\d{2})?\s*$/;

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
        const content = getNotificationDisplayContent(notification.content);

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

              <div className="flex flex-wrap justify-end gap-2">
                {!notification.isRead ? (
                  <Button
                    type="button"
                    variant="default"
                    className="h-11 px-5 text-sm font-semibold sm:min-w-32"
                    onClick={() => onMarkRead(notification.id)}
                  >
                    읽음 처리
                  </Button>
                ) : null}
                <Button
                  asChild
                  variant="secondary"
                  className="h-11 px-5 text-sm font-semibold sm:min-w-32"
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
        );
      })}
    </div>
  );
}

function getNotificationDisplayContent(content?: string | null) {
  return content?.replace(TRAILING_NOTIFICATION_DATE_PATTERN, "").trim() ?? "";
}

function getNotificationReadLabel(notification: NotificationItem) {
  return notification.isRead ? "읽음" : "미읽음";
}
