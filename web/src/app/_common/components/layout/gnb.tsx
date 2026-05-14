"use client";

import { useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { Bell, ChevronRight, Moon, Sun } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import { resolveBreadcrumbs } from "@/app/_common/service/breadcrumbs";
import {
  useNotificationCenter,
  useNotificationMutation,
  useNotificationStream,
} from "@/app/(protected)/notification/_hooks";
import { resolveNotificationDeepLink } from "@/app/(protected)/notification/_utils/resolveNotificationDeepLink";
import { getNotificationTypeLabel } from "@/app/(protected)/notification/_utils/notificationLabel";
import {
  NotificationCenterPopover,
  type NotificationCenterItem,
} from "@/app/_common/components/layout/notificationCenterPopover";

export default function Gnb() {
  const pathname = usePathname();
  const breadcrumbs = resolveBreadcrumbs(pathname);
  const { unreadCount, recentUnreadNotifications } = useNotificationCenter();
  const { markAllRead, markRead } = useNotificationMutation();
  useNotificationStream();
  const [isNotificationOpen, setIsNotificationOpen] = useState(false);
  const notificationRef = useRef<HTMLDivElement>(null);
  const notificationCenterItems: NotificationCenterItem[] =
    recentUnreadNotifications.map((notification) => ({
      id: notification.id,
      typeLabel: getNotificationTypeLabel(notification.type),
      title: notification.title,
      content: notification.content ?? "",
      createdAt: notification.createdAt,
      href: resolveNotificationDeepLink(notification),
    }));

  useEffect(() => {
    // 저장된 테마를 최초 렌더 이후 복원해 서버/클라이언트 테마 차이를 줄입니다.
    const root = document.documentElement;
    const storedTheme = window.localStorage.getItem("ax-wms-theme");

    if (storedTheme === "dark" || storedTheme === "light") {
      root.classList.toggle("dark", storedTheme === "dark");
    }
  }, []);

  const toggleTheme = () => {
    const root = document.documentElement;
    const nextDark = !root.classList.contains("dark");

    root.classList.toggle("dark", nextDark);
    window.localStorage.setItem("ax-wms-theme", nextDark ? "dark" : "light");
  };

  useEffect(() => {
    if (!isNotificationOpen) {
      return;
    }

    // 팝오버가 열린 동안에만 바깥 클릭과 Escape 입력으로 닫히도록 이벤트를 연결합니다.
    const handlePointerDown = (event: PointerEvent) => {
      if (!notificationRef.current?.contains(event.target as Node)) {
        setIsNotificationOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsNotificationOpen(false);
      }
    };

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isNotificationOpen]);

  return (
    <header className="workspace-topbar sticky top-0 z-40 flex w-full items-center gap-4 border-b border-white/10 px-4 py-4 text-white shadow-[0_16px_60px_-32px_rgba(0,0,0,0.55)] md:px-8">
      <div className="min-w-0 flex-1">
        <div className="inline-flex items-center rounded border border-white/10 bg-black/10 px-2 py-0.5 text-[8px] font-medium uppercase tracking-[0.13em] text-white/65">
          IBANK AX 사업본부
        </div>

        <nav
          aria-label="breadcrumb"
          className="mt-2 flex flex-wrap items-center gap-1.5 text-[1.05rem] font-semibold tracking-[-0.04em] text-white"
        >
          {breadcrumbs.map((item, index) => {
            const isCurrent = index === breadcrumbs.length - 1;

            return (
              <div
                key={`${item.label}-${index}`}
                className="flex items-center gap-1.5"
              >
                {index > 0 ? (
                  <ChevronRight className="size-4 text-white/42" />
                ) : null}

                <span
                  className={cn(
                    "text-sm",
                    isCurrent ? "text-white" : "text-white/52",
                  )}
                >
                  {item.label}
                </span>
              </div>
            );
          })}
        </nav>
      </div>

      <div className="flex items-center gap-2">
        <Button
          type="button"
          variant="outline"
          size="icon"
          onClick={toggleTheme}
          className="h-10 w-10 border-white/12 bg-black/10 text-white/75 hover:bg-black/18 hover:text-white"
          title="테마 전환"
        >
          <Sun className="hidden size-4 dark:block" />
          <Moon className="size-4 dark:hidden" />
        </Button>

        <div ref={notificationRef} className="relative">
          <Button
            type="button"
            variant="outline"
            className="h-10 border-white/12 bg-black/10 px-3 text-white/80 hover:bg-black/18 hover:text-white active:!text-white aria-expanded:!bg-black/18 aria-expanded:!text-white focus-visible:!text-white"
            onClick={() => setIsNotificationOpen((prev) => !prev)}
            aria-expanded={isNotificationOpen}
            aria-haspopup="dialog"
            aria-label={`알림 ${unreadCount}개`}
          >
            <Bell className="size-4" />
            <span className="hidden text-left md:inline">알림 센터</span>
            {unreadCount > 0 ? (
              <span className="ml-1 inline-flex min-w-5 items-center justify-center rounded-full bg-primary px-1.5 text-[11px] font-semibold text-primary-foreground">
                {unreadCount > 99 ? "99+" : unreadCount}
              </span>
            ) : null}
          </Button>

          {isNotificationOpen ? (
            <NotificationCenterPopover
              unreadCount={unreadCount}
              notifications={notificationCenterItems}
              onMarkRead={markRead}
              onMarkAllRead={markAllRead}
              onClose={() => setIsNotificationOpen(false)}
            />
          ) : null}
        </div>
      </div>
    </header>
  );
}
