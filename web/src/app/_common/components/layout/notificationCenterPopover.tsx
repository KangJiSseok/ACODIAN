"use client";

import Link from "next/link";
import { CheckCheck, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

export type NotificationCenterItem = {
  id: number;
  typeLabel: string;
  title: string;
  content: string;
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
      <div className="flex items-start justify-between border-b border-slate-200 px-5 py-4 dark:border-slate-800">
        <div>
          <h2 className="text-sm font-semibold">알림 센터</h2>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            내 알림 기준 · 미읽음 {unreadCount}건
          </p>
        </div>

        <Button
          type="button"
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-slate-700 hover:bg-slate-100 hover:text-slate-950 dark:text-slate-200 dark:hover:bg-slate-800 dark:hover:text-slate-50"
          onClick={onMarkAllRead}
          aria-label="전체 읽음 처리"
        >
          <CheckCheck className="size-4" />
        </Button>
      </div>

      <div className="max-h-[320px] overflow-y-auto px-5 py-4">
        {notifications.length > 0 ? (
          <ul className="space-y-4">
            {notifications.map((notification) => (
              <li key={notification.id}>
                <Link
                  href={notification.href}
                  onClick={() => {
                    onMarkRead(notification.id);
                    onClose();
                  }}
                  className="block rounded-xl bg-slate-50 p-3 text-slate-950 transition hover:bg-slate-100 hover:text-slate-950 focus-visible:text-slate-950 active:text-slate-950 dark:bg-slate-900 dark:text-slate-50 dark:hover:bg-slate-800 dark:hover:text-slate-50 dark:focus-visible:text-slate-50 dark:active:text-slate-50"
                >
                  <div className="mb-2 flex items-center gap-2">
                    <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-200">
                      {notification.typeLabel}
                    </span>
                    <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-200">
                      미읽음
                    </span>
                  </div>

                  <div className="flex items-center justify-between gap-3">
                    <strong className="text-sm font-semibold">
                      {notification.title}
                    </strong>
                    <ChevronRight className="size-4 text-slate-500 dark:text-slate-400" />
                  </div>

                  <p className="mt-2 line-clamp-2 text-xs text-slate-500 dark:text-slate-400">
                    {notification.content}
                  </p>

                  <time className="mt-3 block text-xs font-medium text-slate-500 dark:text-slate-400">
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
          <div className="rounded-xl border border-dashed border-slate-200 px-4 py-8 text-center text-sm text-slate-500 dark:border-slate-800 dark:text-slate-400">
            표시할 알림이 없습니다.
          </div>
        )}
      </div>

      <div className="border-t border-slate-200 p-4 dark:border-slate-800">
        <Button
          asChild
          variant="outline"
          className="w-full border-slate-200 bg-white text-slate-950 hover:bg-slate-50 hover:text-slate-950 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-50 dark:hover:bg-slate-900 dark:hover:text-slate-50"
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
