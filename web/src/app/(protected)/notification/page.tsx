"use client"

import { useMemo, useState } from "react"
import { Search } from "lucide-react"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { Pagination } from "@/app/_common/components/data-display/pagination"
import { usePagination } from "@/app/_common/hooks/usePagination"
import { Button } from "@/components/ui/button"
import { CardSpotlight } from "@/components/ui/card-spotlight"
import { Input } from "@/components/ui/input"
import { cn } from "@/lib/utils"
import { NotificationList } from "./_components/notificationList"
import {
  useNotificationList,
  useNotificationMutation,
} from "./_hooks"
import type { NotificationView } from "./_types/notification.types"

const notificationTypeLabelMap = {
  URGENT: "긴급",
  DEADLINE: "마감",
  OVERDUE: "지연",
  DEPENDENCY: "의존성",
  WORKLOAD: "업무량",
} as const

const sourceScopeLabelMap = {
  PERSONAL: "개인",
  TEAM: "팀",
  DEPARTMENT: "부서",
} as const

export default function NotificationPage() {
  const { notifications, unreadCount, readCount } = useNotificationList()
  const { markAllRead, markRead } = useNotificationMutation()
  const [query, setQuery] = useState("")
  const [activeView, setActiveView] = useState<NotificationView>("TOTAL")

  const filteredNotifications = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()

    return notifications.filter((notification) => {
      const readMatch =
        activeView === "TOTAL" ||
        (activeView === "UNREAD" && !notification.isRead) ||
        (activeView === "READ" && notification.isRead)
      const searchableText = [
        notification.title,
        notification.content,
        notificationTypeLabelMap[notification.type],
        sourceScopeLabelMap[notification.sourceScope],
      ]
        .join(" ")
        .toLowerCase()

      return readMatch && (!normalizedQuery || searchableText.includes(normalizedQuery))
    })
  }, [activeView, notifications, query])

  const pagination = usePagination(filteredNotifications, 4)

  return (
    <section className="space-y-6">
      <PageHeader title="알림" description="업무와 파일 변경 사항을 확인합니다." />

      <div className="space-y-6">
        <section className="space-y-4">
          <div className="flex flex-wrap items-center gap-3">
            <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
              알림 현황
            </h2>
            <Button
              type="button"
              variant="outline"
              className="h-10 rounded-2xl px-4 text-sm"
              onClick={markAllRead}
              disabled={unreadCount === 0}
            >
              전체 읽음 처리
            </Button>
          </div>

          <div className="grid gap-3 md:grid-cols-3">
            <SummaryCard
              label="전체 알림"
              value={notifications.length}
              active={activeView === "TOTAL"}
              onClick={() => {
                setActiveView("TOTAL")
                pagination.setPage(1)
              }}
            />
            <SummaryCard
              label="읽지 않은 알림"
              value={unreadCount}
              active={activeView === "UNREAD"}
              onClick={() => {
                setActiveView("UNREAD")
                pagination.setPage(1)
              }}
            />
            <SummaryCard
              label="읽은 알림"
              value={readCount}
              active={activeView === "READ"}
              onClick={() => {
                setActiveView("READ")
                pagination.setPage(1)
              }}
            />
          </div>
        </section>

        <section className="space-y-4">
          <div className="flex flex-wrap items-center gap-3">
            <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
              알림 탐색
            </h2>
            <p className="text-sm text-muted-foreground">
              표시 중인 알림{" "}
              <span className="text-lg font-semibold text-foreground">
                {filteredNotifications.length}건
              </span>
            </p>
          </div>

          <div className="relative">
            <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              value={query}
              onChange={(event) => {
                setQuery(event.target.value)
                pagination.setPage(1)
              }}
              className="h-12 rounded-2xl pl-11 text-sm"
              placeholder="알림 제목, 내용, 유형으로 검색하세요"
            />
          </div>

          <NotificationList notifications={pagination.items} onMarkRead={markRead} />
          <Pagination
            page={pagination.page}
            totalPages={pagination.totalPages}
            onPageChange={pagination.setPage}
          />
        </section>
      </div>
    </section>
  )
}

function SummaryCard({
  label,
  value,
  active,
  onClick,
}: {
  label: string
  value: number
  active: boolean
  onClick: () => void
}) {
  return (
    <button type="button" className="text-left" onClick={onClick}>
      <CardSpotlight
        className={cn(
          "rounded-[24px] p-5 transition-all duration-200",
          active && "ring-2 ring-primary/70",
        )}
      >
        <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
          {label}
        </p>
        <p className="mt-2 text-2xl font-semibold text-foreground">{value}</p>
      </CardSpotlight>
    </button>
  )
}
