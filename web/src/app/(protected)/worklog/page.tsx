"use client"

import Link from "next/link"
import { useState } from "react"
import { canCreateWorklog } from "./_utils/accessControl"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { Pagination } from "@/app/_common/components/data-display/pagination"
import { LegendHelpDialog } from "./_components/legendHelpDialog"
import { useAuth } from "./_hooks/useAuth"
import { ImportanceBadge } from "./_components/importanceBadge"
import { StatusBadge } from "./_components/statusBadge"
import { useWorklogList } from "./_hooks/useWorklogList"
import { WorklogList } from "./_components/worklogList"
import {
  worklogImportanceLegendOrder,
  worklogStatusLegendOrder,
} from "./_components/worklogBadgeConfig"
import { Button } from "@/components/ui/button"

const WORKLOG_PAGE_SIZE = 10

export default function WorklogPage() {
  const { user } = useAuth()
  const [page, setPage] = useState(1)
  const {
    data: worklogPage,
    isLoading,
    isError,
  } = useWorklogList({ page, pageSize: WORKLOG_PAGE_SIZE })
  const canCreate = canCreateWorklog(user)
  const worklogs = worklogPage?.items ?? []

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="업무 검색" />
      <div className="space-y-4">
        <div>
          <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
            업무 탐색
          </h2>
        </div>

        <div className="pt-2">
          <div className="flex flex-col gap-3 pb-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-2">
              <p className="text-sm text-muted-foreground">
                표시 중인 업무{" "}
                <span className="ml-1 font-semibold text-foreground">
                  {worklogPage?.totalCount ?? 0}건
                </span>
              </p>
              <LegendHelpDialog
                title="업무 아이콘 안내"
                description="업무 카드에서 보이는 상태와 중요도 아이콘 의미를 빠르게 확인할 수 있습니다."
                buttonLabel="업무 아이콘 안내 열기"
                sections={[
                  {
                    title: "상태",
                    content: worklogStatusLegendOrder.map((status) => (
                      <StatusBadge key={status} status={status} />
                    )),
                  },
                  {
                    title: "중요도",
                    content: worklogImportanceLegendOrder.map((importance) => (
                      <ImportanceBadge key={importance} importance={importance} />
                    )),
                  },
                ]}
                className="h-8 w-8"
              />
            </div>
            {canCreate ? (
              <Button
                asChild
                variant="default"
                className="h-10 min-w-32 px-6 text-sm font-semibold"
              >
                <Link href="/worklog/create">업무 등록</Link>
              </Button>
            ) : null}
          </div>

          {isLoading ? (
            <div className="workspace-empty rounded-xl px-6 py-10 text-center text-sm">
              업무 목록을 불러오는 중입니다.
            </div>
          ) : isError ? (
            <div className="workspace-empty rounded-xl px-6 py-10 text-center text-sm">
              업무 목록을 불러오지 못했습니다.
            </div>
          ) : (
            <WorklogList worklogs={worklogs} />
          )}

          <div className="pt-6">
            <Pagination
              page={worklogPage?.page ?? page}
              totalPages={worklogPage?.totalPages ?? 1}
              onPageChange={setPage}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
