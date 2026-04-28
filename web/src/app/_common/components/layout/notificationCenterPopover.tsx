"use client";

import Link from "next/link";
import { CheckCheck, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  useNotificationList,
  useNotificationMutation,
} from "@/app/(protected)/notification/_hooks";
import { resolveNotificationDeepLink } from "@/app/(protected)/notification/_utils/resolveNotificationDeepLink";

type NotificationCenterPopoverProps = {
  onClose: () => void;
};

export function NotificationCenterPopover({
  onClose,
}: NotificationCenterPopoverProps) {
  const { unreadCount, recentNotifications } = useNotificationList();
  const { markAllRead } = useNotificationMutation();
  const unreadNotifications = recentNotifications.filter(
    (notification) => !notification.isRead,
  );

  return (
    <div className="absolute right-0 top-12 z-50 w-[360px] overflow-hidden rounded-2xl border border-border bg-card text-card-foreground shadow-2xl">
      <div className="flex items-start justify-between border-b border-border px-5 py-4">
        <div>
          <h2 className="text-sm font-semibold">알림 센터</h2>
          <p className="mt-1 text-xs text-muted-foreground">
            선택팀 · MCP Project 기준 · 미읽음 {unreadCount}건
          </p>
        </div>

        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8"
          onClick={markAllRead}
          aria-label="전체 읽음 처리"
        >
          <CheckCheck className="size-4" />
        </Button>
      </div>

      <div className="max-h-[320px] overflow-y-auto px-5 py-4">
        {unreadNotifications.length > 0 ? (
          <ul className="space-y-4">
            {unreadNotifications.slice(0, 3).map((notification) => (
              <li key={notification.id}>
                <Link
                  href={resolveNotificationDeepLink(notification)}
                  onClick={onClose}
                  className="block rounded-xl bg-primary/5 p-3 transition hover:bg-muted"
                >
                  <div className="mb-2 flex items-center gap-2">
                    <span className="rounded-full bg-muted px-3 py-1 text-xs font-medium">
                      {notification.type === "WORKLOAD" ? "업무량" : "알림"}
                    </span>
                    <span className="rounded-full bg-muted px-3 py-1 text-xs font-medium">
                      미읽음
                    </span>
                  </div>

                  <div className="flex items-center justify-between gap-3">
                    <strong className="text-sm font-semibold">
                      {notification.title}
                    </strong>
                    <ChevronRight className="size-4 text-muted-foreground" />
                  </div>

                  <p className="mt-2 line-clamp-2 text-xs text-muted-foreground">
                    {notification.content}
                  </p>

                  <time className="mt-3 block text-xs font-medium text-muted-foreground">
                    {new Date(notification.createdAt).toLocaleString("ko-KR", {
                      year: "numeric",
                      month: "long",
                      day: "numeric",
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </time>
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <div className="rounded-xl border border-dashed border-border px-4 py-8 text-center text-sm text-muted-foreground">
            표시할 알림이 없습니다.
          </div>
        )}
      </div>

      <div className="border-t border-border p-4">
        <Button asChild variant="outline" className="w-full">
          <Link href="/notification" onClick={onClose}>
            전체 보기
            <ChevronRight className="size-4" />
          </Link>
        </Button>
      </div>
    </div>
  );
}
