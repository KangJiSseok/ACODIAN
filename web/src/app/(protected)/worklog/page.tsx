"use client"

import Link from "next/link"
import { useMemo, useState } from "react"
import { ChevronDown, RefreshCw, Search, SlidersHorizontal } from "lucide-react"
import {
  canCreateWorklog,
  getVisibleDepartments,
  getVisibleTeams,
  getVisibleUsers,
} from "./_utils/accessControl"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { Pagination } from "@/app/_common/components/data-display/pagination"
import { LegendHelpDialog } from "./_components/legendHelpDialog"
import { usePagination } from "@/app/_common/hooks/usePagination"
import { useAuth } from "./_hooks/useAuth"
import { departments, tags, teams, users } from "./_mock/worklog.mock"
import { ImportanceBadge } from "./_components/importanceBadge"
import { StatusBadge } from "./_components/statusBadge"
import { useWorklog } from "./_hooks/useWorklog"
import { WorklogList } from "./_components/worklogList"
import {
  worklogImportanceLegendOrder,
  worklogStatusLegendOrder,
} from "./_components/worklogBadgeConfig"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Select } from "@/components/ui/select"
import { cn } from "@/lib/utils"
import {
  getAiStatusLabel,
  getImportanceLabel,
  getWorklogStatusLabel,
} from "./_utils/worklogFormat"

const DEFAULT_FILTERS = {
  departmentId: "all",
  teamId: "all",
  status: "all",
  importance: "all",
  authorId: "all",
  tagId: "all",
  period: "ALL" as const,
}

export default function WorklogPage() {
  const { user } = useAuth()
  const { worklogs } = useWorklog()
  const canCreate = canCreateWorklog(user)
  const [query, setQuery] = useState("")
  const [showFilters, setShowFilters] = useState(false)
  const [filters, setFilters] = useState(DEFAULT_FILTERS)
  const visibleDepartments = getVisibleDepartments(user, departments)
  const visibleTeams = getVisibleTeams(user, teams)
  const visibleUsers = getVisibleUsers(user, users)
  const activeFilterCount = Object.values(filters).filter(
    (value) => value !== "all" && value !== "ALL"
  ).length

  const filteredWorklogs = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    const now = new Date()

    return worklogs.filter((worklog) => {
      const team = teams.find((team) => team.id === worklog.teamId)
      const teamName = team?.name.toLowerCase() ?? ""
      const author = users.find((member) => member.id === worklog.authorId)
      const authorName = author?.name.toLowerCase() ?? ""
      const tagNames = tags
        .filter((tag) => worklog.tagIds.includes(tag.id))
        .map((tag) => tag.name.toLowerCase())
        .join(" ")
      const searchableText = [
        worklog.title,
        worklog.aiSummary,
        teamName,
        authorName,
        tagNames,
        getWorklogStatusLabel(worklog.status),
        getImportanceLabel(worklog.importance),
        getAiStatusLabel(worklog.aiStatus),
      ]
        .join(" ")
        .toLowerCase()

      const queryMatch = !normalizedQuery || searchableText.includes(normalizedQuery)
      const departmentMatch =
        filters.departmentId === "all" || String(team?.departmentId) === filters.departmentId
      const teamMatch = filters.teamId === "all" || String(worklog.teamId) === filters.teamId
      const statusMatch = filters.status === "all" || worklog.status === filters.status
      const importanceMatch =
        filters.importance === "all" || worklog.importance === filters.importance
      const authorMatch =
        filters.authorId === "all" || String(worklog.authorId) === filters.authorId
      const tagMatch =
        filters.tagId === "all" || worklog.tagIds.includes(Number(filters.tagId))
      const diffDays = Math.floor(
        (now.getTime() - new Date(worklog.createdAt).getTime()) / (1000 * 60 * 60 * 24)
      )
      const periodMatch =
        filters.period === "ALL" ||
        (filters.period === "7D" && diffDays <= 7) ||
        (filters.period === "30D" && diffDays <= 30) ||
        (filters.period === "90D" && diffDays <= 90)

      return (
        queryMatch &&
        departmentMatch &&
        teamMatch &&
        statusMatch &&
        importanceMatch &&
        authorMatch &&
        tagMatch &&
        periodMatch
      )
    })
  }, [filters, query, worklogs, visibleDepartments, visibleTeams, visibleUsers])
  const worklogPagination = usePagination(filteredWorklogs, 4)

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="업무 검색" />
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
            업무 탐색
          </h2>
          {canCreate ? (
            <Button asChild variant="default" className="h-10 min-w-32 px-6 text-sm font-semibold">
              <Link href="/worklog/create">업무 등록</Link>
            </Button>
          ) : null}
        </div>
        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                className="h-12 pl-11"
                placeholder="업무 제목, 요약, 작성자, 팀, 상태로 검색하세요"
              />
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                className="h-10"
                onClick={() => setShowFilters((prev) => !prev)}
              >
                <SlidersHorizontal className="size-4" />
                필터
                {activeFilterCount > 0 ? (
                  <span className="ml-1 rounded-full bg-primary px-2 py-0.5 text-[11px] font-semibold text-primary-foreground">
                    {activeFilterCount}
                  </span>
                ) : null}
                <ChevronDown
                  className={cn(
                    "ml-1 size-4 transition-transform duration-300 ease-out",
                    showFilters && "rotate-180"
                  )}
                />
              </Button>
            </div>
          </div>

          <div
            className={cn(
              "grid overflow-hidden transition-[grid-template-rows,opacity,margin] duration-300 ease-out",
              showFilters ? "mt-0 grid-rows-[1fr] opacity-100" : "mt-[-4px] grid-rows-[0fr] opacity-0"
            )}
          >
            <div className="overflow-hidden">
              <div className="space-y-4 pt-3">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                    Worklog Filters
                  </p>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-9 w-9"
                    aria-label="필터 초기화"
                    onClick={() => {
                      setQuery("")
                      setFilters(DEFAULT_FILTERS)
                    }}
                  >
                    <RefreshCw className="size-4" />
                  </Button>
                </div>
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      부서
                    </p>
                    <Select
                      value={filters.departmentId}
                      options={[
                        { label: "전체 부서", value: "all" },
                        ...visibleDepartments.map((department) => ({
                          label: department.name,
                          value: String(department.id),
                        })),
                      ]}
                      onChange={(event) =>
                        setFilters((prev) => ({ ...prev, departmentId: event.target.value }))
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      팀
                    </p>
                    <Select
                      value={filters.teamId}
                      options={[
                        { label: "전체 팀", value: "all" },
                        ...visibleTeams.map((team) => ({
                          label: team.name,
                          value: String(team.id),
                        })),
                      ]}
                      onChange={(event) =>
                        setFilters((prev) => ({ ...prev, teamId: event.target.value }))
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      상태
                    </p>
                    <Select
                      value={filters.status}
                      options={[
                        { label: "전체 상태", value: "all" },
                        ...worklogStatusLegendOrder.map((status) => ({
                          label: getWorklogStatusLabel(status),
                          value: status,
                        })),
                      ]}
                      onChange={(event) =>
                        setFilters((prev) => ({ ...prev, status: event.target.value }))
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      중요도
                    </p>
                    <Select
                      value={filters.importance}
                      options={[
                        { label: "전체 중요도", value: "all" },
                        { label: getImportanceLabel("URGENT"), value: "URGENT" },
                        { label: getImportanceLabel("HIGH"), value: "HIGH" },
                        { label: getImportanceLabel("NORMAL"), value: "NORMAL" },
                        { label: getImportanceLabel("LOW"), value: "LOW" },
                      ]}
                      onChange={(event) =>
                        setFilters((prev) => ({ ...prev, importance: event.target.value }))
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      작성자
                    </p>
                    <Select
                      value={filters.authorId}
                      options={[
                        { label: "전체 작성자", value: "all" },
                        ...visibleUsers.map((member) => ({
                          label: member.name,
                          value: String(member.id),
                        })),
                      ]}
                      onChange={(event) =>
                        setFilters((prev) => ({ ...prev, authorId: event.target.value }))
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      태그
                    </p>
                    <Select
                      value={filters.tagId}
                      options={[
                        { label: "전체 태그", value: "all" },
                        ...tags.map((tag) => ({
                          label: `#${tag.name}`,
                          value: String(tag.id),
                        })),
                      ]}
                      onChange={(event) =>
                        setFilters((prev) => ({ ...prev, tagId: event.target.value }))
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      기간
                    </p>
                    <Select
                      value={filters.period}
                      options={[
                        { label: "전체 기간", value: "ALL" },
                        { label: "최근 7일", value: "7D" },
                        { label: "최근 30일", value: "30D" },
                        { label: "최근 90일", value: "90D" },
                      ]}
                      onChange={(event) =>
                        setFilters((prev) => ({
                          ...prev,
                          period: event.target.value as typeof DEFAULT_FILTERS.period,
                        }))
                      }
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div className="pt-2">
          <div className="pb-4">
            <div className="flex items-center gap-2">
              <p className="text-sm text-muted-foreground">
                표시 중인 업무{" "}
                <span className="ml-1 font-semibold text-foreground">{filteredWorklogs.length}건</span>
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
          </div>
          <WorklogList worklogs={worklogPagination.items} />
          <div className="pt-6">
            <Pagination
              page={worklogPagination.page}
              totalPages={worklogPagination.totalPages}
              onPageChange={worklogPagination.setPage}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
