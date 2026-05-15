"use client";

import Link from "next/link";
import { ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export type NotificationCenterItem = {
  id: number;
  title: string;
  isRead: boolean;
  teamName?: string | null;
  createdAt: string;
  href: string;
};

type NotificationCenterPopoverProps = {
  unreadCount: number;
  notifications: NotificationCenterItem[];
  onMarkRead: (id: number) => void;
  onMarkAllRead: () => void;
  onClose: () => void;
};

export function NotificationCenterPopover({
  unreadCount,
  notifications,
  onMarkRead,
  onMarkAllRead,
  onClose,
}: NotificationCenterPopoverProps) {
  return (
    <div className="absolute right-0 top-12 z-50 w-[360px] overflow-hidden rounded-2xl border border-slate-200 bg-white text-slate-950 shadow-2xl dark:border-slate-800 dark:bg-slate-950 dark:text-slate-50">
      <div className="flex items-center justify-between border-b border-slate-200 px-5 py-4 dark:border-slate-800">
        <div>
          <h2 className="text-sm font-semibold">알림 센터</h2>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            최대 10개 표시 · 미읽음 {unreadCount}건
          </p>
        </div>

        <Button
          type="button"
          variant="default"
          className="h-9 rounded-xl bg-primary px-3.5 text-xs font-semibold text-primary-foreground shadow-[0_12px_24px_-18px_rgba(30,64,175,0.9)] hover:bg-primary/90 disabled:pointer-events-none disabled:opacity-45"
          onClick={onMarkAllRead}
          disabled={unreadCount === 0}
          aria-label="전체 읽음 처리"
        >
          전체 읽음
        </Button>
      </div>

      <div className="max-h-[420px] overflow-y-auto px-5 py-4">
        {notifications.length > 0 ? (
          <ul className="space-y-2.5">
            {notifications.map((notification) => (
              <li key={notification.id}>
                <Link
                  href={notification.href}
                  onClick={() => {
                    if (!notification.isRead) {
                      onMarkRead(notification.id);
                    }
                    onClose();
                  }}
                  className="flex items-center justify-between gap-3 rounded-xl border border-slate-200/70 bg-slate-50 px-3.5 py-3 text-slate-950 transition hover:border-slate-300 hover:bg-slate-100 hover:text-slate-950 focus-visible:text-slate-950 active:text-slate-950 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-50 dark:hover:border-slate-700 dark:hover:bg-slate-800 dark:hover:text-slate-50 dark:focus-visible:text-slate-50 dark:active:text-slate-50"
                >
                  <div className="min-w-0">
                    <strong className="line-clamp-1 text-sm font-semibold">
                      {notification.title}
                    </strong>
                    <div className="mt-1.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs font-medium text-slate-500 dark:text-slate-400">
                      {notification.teamName ? (
                        <span className="line-clamp-1">
                          {notification.teamName}
                        </span>
                      ) : null}
                      <time dateTime={notification.createdAt}>
                        {formatNotificationCenterDate(notification.createdAt)}
                      </time>
                    </div>
                  </div>
                  <ChevronRight className="size-4 shrink-0 text-slate-500 dark:text-slate-400" />
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <div className="rounded-xl border border-dashed border-slate-200 px-4 py-8 text-center text-sm text-slate-500 dark:border-slate-800 dark:text-slate-400">
            읽지 않은 알림이 없습니다.
          </div>
        )}
      </div>

      <div className="border-t border-slate-200 p-4 dark:border-slate-800">
        <Button
          asChild
          variant="default"
          className="w-full rounded-xl bg-primary !text-primary-foreground shadow-[0_14px_28px_-20px_rgba(30,64,175,0.9)] hover:bg-primary/90 hover:!text-primary-foreground focus-visible:!text-primary-foreground active:!text-primary-foreground dark:bg-primary dark:!text-primary-foreground dark:hover:bg-primary/90 dark:hover:!text-primary-foreground [&_svg]:!text-primary-foreground"
        >
          <Link href="/notification" onClick={onClose}>
            전체 보기
            <ChevronRight className="size-4" />
          </Link>
        </Button>
      </div>
    </div>
  );
}

function formatNotificationCenterDate(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
