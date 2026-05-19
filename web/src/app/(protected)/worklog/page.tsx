"use client"

import Link from "next/link"
import { type ReactNode, useEffect, useMemo, useState } from "react"
import {
  ArrowLeft,
  ArrowUp,
  ChevronDown,
  Plus,
  RefreshCw,
  Search,
  SlidersHorizontal,
  Sparkles,
} from "lucide-react"
import PageHeader from "@/app/_common/components/layout/pageHeader"
import { Pagination } from "@/app/_common/components/data-display/pagination"
import { useAuth } from "@/app/_common/hooks/useAuth"
import { isDirectorProfile } from "@/app/_common/utils/organizationAccess.utils"
import { getApiErrorMessage } from "@/app/_common/service/api-client"
import { LegendHelpDialog } from "./_components/legendHelpDialog"
import { ImportanceBadge } from "./_components/importanceBadge"
import { StatusBadge } from "./_components/statusBadge"
import {
  useWorklogList,
  useWorklogFilterOptions,
  useWorklogSearch,
  useWorklogSemanticSearch,
} from "./_hooks/useWorklogList"
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
  getImportanceLabel,
  getWorklogStatusLabel,
} from "./_utils/worklogFormat"
import type {
  ImportanceLevel,
  SearchWorklogsParams,
  WorklogStatus,
} from "./_types/worklog.types"

const WORKLOG_PAGE_SIZE = 10

const DEFAULT_FILTERS = {
  teamId: "all",
  status: "all",
  importance: "all",
  authorId: "all",
  tagId: "all",
  period: "ALL",
}

const aiPromptExamples = [
  "지난주 완료된 장애 대응 업무 보여줘",
  "AI 평가셋 관련 업무 요약해줘",
  "담당자별 진행 업무 찾아줘",
  "마감 임박 업무만 우선순위로 정리해줘",
]

const MARKDOWN_EMOJI_SHORTCODES: Record<string, string> = {
  ":ai:": "AI",
  ":books:": "📚",
  ":bulb:": "💡",
  ":calendar:": "📅",
  ":chart_with_upwards_trend:": "📈",
  ":check:": "✓",
  ":clock:": "🕒",
  ":fire:": "🔥",
  ":gear:": "⚙️",
  ":heavy_check_mark:": "✔️",
  ":information_source:": "ℹ️",
  ":link:": "🔗",
  ":mag:": "🔍",
  ":memo:": "📝",
  ":pushpin:": "📌",
  ":robot:": "🤖",
  ":robot_face:": "🤖",
  ":rocket:": "🚀",
  ":smile:": "😄",
  ":sparkles:": "✨",
  ":star:": "⭐",
  ":thumbsup:": "👍",
  ":warning:": "⚠️",
  ":white_check_mark:": "✅",
  ":x:": "❌",
}

type AiMessage = {
  id: number
  role: "user" | "assistant"
  content: string
  isLoading?: boolean
}

type WorklogFilterState = typeof DEFAULT_FILTERS

export default function WorklogPage() {
  const { user } = useAuth()
  const [page, setPage] = useState(1)
  const [searchInput, setSearchInput] = useState("")
  const debouncedKeyword = useDebouncedValue(searchInput.trim(), 400)
  const [aiMode, setAiMode] = useState(false)
  const [aiDraftQuery, setAiDraftQuery] = useState("")
  const [aiMessages, setAiMessages] = useState<AiMessage[]>([])
  const [showFilters, setShowFilters] = useState(false)
  const [filters, setFilters] = useState<WorklogFilterState>(DEFAULT_FILTERS)
  const semanticSearch = useWorklogSemanticSearch()
  const canCreate = Boolean(user && !isDirectorProfile(user))
  const { data: filterOptions } = useWorklogFilterOptions()
  const memberOptions = useMemo(() => {
    const indexed = new Map<number, string>()
    const teams =
      filters.teamId === "all"
        ? filterOptions?.teams ?? []
        : (filterOptions?.teams ?? []).filter(
            (team) => String(team.teamId) === filters.teamId
          )

    teams.forEach((team) => {
      team.members.forEach((member) => {
        indexed.set(member.userId, member.userName)
      })
    })

    return Array.from(indexed, ([userId, userName]) => ({ userId, userName }))
  }, [filterOptions, filters.teamId])
  const hasSearchCondition =
    debouncedKeyword.length > 0 ||
    Object.values(filters).some((value) => value !== "all" && value !== "ALL")
  const listParams = useMemo(
    () => ({
      page,
      pageSize: WORKLOG_PAGE_SIZE,
    }),
    [page]
  )
  const searchParams = useMemo<SearchWorklogsParams>(
    () => ({
      page,
      pageSize: WORKLOG_PAGE_SIZE,
      keyword: debouncedKeyword || undefined,
      teamId: filters.teamId === "all" ? undefined : Number(filters.teamId),
      statusCode:
        filters.status === "all" ? undefined : (filters.status as WorklogStatus),
      importanceCode:
        filters.importance === "all"
          ? undefined
          : (filters.importance as ImportanceLevel),
      authorId:
        filters.authorId === "all" ? undefined : Number(filters.authorId),
      tagId: filters.tagId === "all" ? undefined : Number(filters.tagId),
      period:
        filters.period === "ALL"
          ? undefined
          : (filters.period as SearchWorklogsParams["period"]),
    }),
    [debouncedKeyword, filters, page]
  )
  const listQuery = useWorklogList(listParams, {
    enabled: !hasSearchCondition,
  })
  const searchQuery = useWorklogSearch(searchParams, {
    enabled: hasSearchCondition,
  })
  const activeQuery = hasSearchCondition ? searchQuery : listQuery
  const worklogPage = activeQuery.data
  const isLoading = activeQuery.isLoading
  const isError = activeQuery.isError
  const worklogs = worklogPage?.items ?? []
  const hasAiConversation = aiMessages.length > 0
  const activeFilterCount = Object.values(filters).filter(
    (value) => value !== "all" && value !== "ALL"
  ).length

  function updateFilters(nextFilters: WorklogFilterState) {
    setFilters(nextFilters)
    setPage(1)
  }

  function submitSearch() {
    setPage(1)
  }

  function submitAiQuestion(question: string) {
    const normalizedQuestion = question.trim()

    if (!normalizedQuestion) return

    const nextMessageId = Date.now()
    const loadingMessageId = nextMessageId + 1

    setAiMessages((prev) => [
      ...prev,
      {
        id: nextMessageId,
        role: "user",
        content: normalizedQuestion,
      },
      {
        id: loadingMessageId,
        role: "assistant",
        content: "관련 업무일지를 찾고 답변을 정리하고 있습니다.",
        isLoading: true,
      },
    ])
    setAiDraftQuery("")
    setShowFilters(false)
    semanticSearch.mutate(
      { query: normalizedQuestion },
      {
        onSuccess: (response) => {
          setAiMessages((prev) =>
            prev.map((message) =>
              message.id === loadingMessageId
                ? {
                    ...message,
                    content:
                      response.answer?.trim() ||
                      "조건에 맞는 AI 모드 답변이 없습니다.",
                    isLoading: false,
                  }
                : message
            )
          )
        },
        onError: (error) => {
          setAiMessages((prev) =>
            prev.map((message) =>
              message.id === loadingMessageId
                ? {
                    ...message,
                    content: getApiErrorMessage(
                      error,
                      "AI 모드 요청을 처리하지 못했습니다."
                    ),
                    isLoading: false,
                  }
                : message
            )
          )
        },
      }
    )
  }

  function returnToWorklogList() {
    setAiMode(false)
    setAiDraftQuery("")
    setAiMessages([])
    setShowFilters(false)
  }

  return (
    <div
      className={cn(
        "flex flex-col gap-6",
        aiMode && "worklog-ai-mode h-full min-h-0 overflow-hidden"
      )}
    >
      <PageHeader title="업무 검색" />
      {aiMode ? (
        <div className="flex justify-start">
          <Button
            type="button"
            variant="secondary"
            className="h-10 rounded-xl px-4 text-sm font-semibold"
            onClick={returnToWorklogList}
          >
            <ArrowLeft className="size-4" />
            업무일지 조회로 돌아가기
          </Button>
        </div>
      ) : null}
      <div
        className={cn(
          "space-y-4",
          aiMode && "flex min-h-0 flex-1 flex-col overflow-hidden",
          aiMode && !hasAiConversation && "justify-center"
        )}
      >
        <div
          className={cn(
            "transition-all duration-500 ease-out",
            aiMode &&
              "pointer-events-none max-h-0 -translate-y-2 overflow-hidden opacity-0"
          )}
        >
          <h2 className="text-[20px] font-semibold tracking-[-0.04em] text-foreground">
            업무 탐색
          </h2>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            업무 제목을 검색하고 팀, 상태, 중요도 등 조건을 조합해 필요한
            업무일지를 빠르게 찾아보세요.
          </p>
        </div>

        {!aiMode || !hasAiConversation ? (
        <div
          className={cn(
            "flex flex-col gap-4 transition-all duration-500 ease-out",
            aiMode && "mx-auto mt-0 w-full max-w-5xl -translate-y-10 gap-6 px-0 py-0 ai-search-lift"
          )}
        >
          {aiMode ? (
            <div className="text-center transition-all duration-500 ease-out">
              <p className="text-[clamp(1.7rem,4vw,3.15rem)] font-bold leading-tight text-foreground">
                안녕하세요, 어떤 업무일지를 찾으려고 하시나요?
              </p>
              <p className="mx-auto mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
                현재 화면의 필터 조건을 함께 반영합니다. 업무명, 담당자, 상태,
                마감 조건을 자연스럽게 입력해보세요.
              </p>
            </div>
          ) : null}
          <form
            className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between"
            onSubmit={(event) => {
              event.preventDefault()
              if (aiMode) {
                submitAiQuestion(aiDraftQuery)
                return
              }
              submitSearch()
            }}
          >
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={aiMode ? aiDraftQuery : searchInput}
                onChange={(event) => {
                  if (aiMode) {
                    setAiDraftQuery(event.target.value)
                    return
                  }
                  setSearchInput(event.target.value)
                  setPage(1)
                }}
                className={cn(
                  "h-12 rounded-2xl pl-11 transition-all duration-500",
                  aiMode &&
                    "h-16 border-border/70 bg-card/90 pr-4 text-[16px] shadow-[0_18px_70px_-42px_rgba(15,23,42,0.65),inset_0_1px_0_rgba(255,255,255,0.08)] backdrop-blur",
                  !aiMode && "pr-32"
                )}
                placeholder={
                  aiMode
                    ? "어떤 업무를 찾고 싶으신가요?"
                    : "업무 제목으로 검색하세요"
                }
              />
              {!aiMode ? (
                <Button
                  type="button"
                  variant="secondary"
                  className="ai-mode-trigger absolute right-2 top-1/2 h-9 -translate-y-1/2 rounded-xl px-3 text-sm font-semibold active:not-aria-[haspopup]:-translate-y-1/2"
                  onClick={() => {
                    setAiMode(true)
                    setShowFilters(false)
                    setAiDraftQuery(searchInput)
                  }}
                >
                  <Sparkles className="size-4" />
                  AI 모드
                </Button>
              ) : null}
            </div>
            <div
              className={cn(
                "flex w-full items-center gap-2 sm:w-auto",
                aiMode && "hidden"
              )}
            >
              <Button
                type="submit"
                variant="default"
                className="h-12 min-w-24 justify-center px-5 text-sm font-semibold"
              >
                <Search className="size-4" />
                검색
              </Button>
              <Button
                type="button"
                variant="secondary"
                className="h-12 justify-center"
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
          </form>

          <div
            className={cn(
              "grid overflow-hidden transition-[grid-template-rows,opacity,margin] duration-300 ease-out",
              showFilters && !aiMode
                ? "mt-0 grid-rows-[1fr] opacity-100"
                : "mt-[-4px] grid-rows-[0fr] opacity-0"
            )}
          >
            <div className="overflow-hidden">
              <div className="space-y-4 pb-4 pt-3">
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
                      setSearchInput("")
                      updateFilters(DEFAULT_FILTERS)
                    }}
                  >
                    <RefreshCw className="size-4" />
                  </Button>
                </div>
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                  <div className="space-y-2">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-muted-foreground">
                      팀
                    </p>
                    <Select
                      value={filters.teamId}
                      options={[
                        { label: "전체 팀", value: "all" },
                        ...(filterOptions?.teams ?? []).map((team) => ({
                          label: team.teamName,
                          value: String(team.teamId),
                        })),
                      ]}
                      onChange={(event) =>
                        updateFilters({
                          ...filters,
                          teamId: event.target.value,
                          authorId: "all",
                        })
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
                        updateFilters({ ...filters, status: event.target.value })
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
                        updateFilters({
                          ...filters,
                          importance: event.target.value,
                        })
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
                        ...memberOptions.map((member) => ({
                          label: member.userName,
                          value: String(member.userId),
                        })),
                      ]}
                      onChange={(event) =>
                        updateFilters({ ...filters, authorId: event.target.value })
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
                        ...(filterOptions?.tags ?? []).map((tag) => ({
                          label: `#${tag.tagName}`,
                          value: String(tag.tagId),
                        })),
                      ]}
                      onChange={(event) =>
                        updateFilters({ ...filters, tagId: event.target.value })
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
                        { label: "최근 7일", value: "LAST_7" },
                        { label: "최근 30일", value: "LAST_30" },
                        { label: "최근 90일", value: "LAST_90" },
                      ]}
                      onChange={(event) =>
                        updateFilters({ ...filters, period: event.target.value })
                      }
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {aiMode ? (
            <div className="grid gap-2 pt-1 sm:grid-cols-2">
              {aiPromptExamples.map((example) => (
                <button
                  key={example}
                  type="button"
                  className="rounded-2xl border border-border/70 bg-muted/35 px-4 py-3 text-left text-sm font-medium text-foreground transition-all duration-200 hover:border-primary/35 hover:bg-primary/8 hover:text-primary"
                  onClick={() => {
                    setAiDraftQuery(example)
                    submitAiQuestion(example)
                  }}
                >
                  {example}
                </button>
              ))}
            </div>
          ) : null}
        </div>
        ) : null}

        {aiMode ? (
          <AiConversationWorkspace
            messages={aiMessages}
            composerValue={aiDraftQuery}
            onComposerChange={setAiDraftQuery}
            onComposerSubmit={() => submitAiQuestion(aiDraftQuery)}
            isSubmitting={semanticSearch.isPending}
          />
        ) : null}

        <div className="pt-2">
          <div
            className={cn(
              "transition-all duration-500 ease-out",
              aiMode &&
                "pointer-events-none max-h-0 -translate-y-3 overflow-hidden opacity-0"
            )}
          >
          <div className="grid gap-3 pb-4 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
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
                <Link href="/worklog/create">
                  <Plus className="size-4" />
                  업무 등록
                </Link>
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
    </div>
  )
}

function AiConversationWorkspace({
  messages,
  composerValue,
  onComposerChange,
  onComposerSubmit,
  isSubmitting,
}: {
  messages: AiMessage[]
  composerValue: string
  onComposerChange: (value: string) => void
  onComposerSubmit: () => void
  isSubmitting: boolean
}) {
  if (messages.length === 0) {
    return null
  }

  return (
    <section className="mx-auto flex min-h-0 w-full max-w-5xl flex-1 flex-col overflow-hidden pt-0">
      <div className="flex min-h-0 min-w-0 flex-1 flex-col">
        <div className="ai-chat-scroll flex min-h-0 flex-1 flex-col gap-3 overflow-y-auto px-1 pb-6 pr-4">
          {messages.map((message) => (
            <div
              key={message.id}
              className={cn(
                "flex",
                message.role === "user" ? "justify-end" : "justify-start"
              )}
            >
              <div
                className={cn(
                  "rounded-[20px] px-4 py-3 text-sm leading-6",
                  message.role === "user"
                    ? "max-w-[min(28rem,74%)] bg-primary text-primary-foreground"
                    : "max-w-[min(44rem,80%)] border border-border/60 bg-card/78 text-foreground backdrop-blur",
                  message.isLoading &&
                    "min-w-16 animate-pulse text-center text-lg"
                )}
              >
                {message.role === "assistant" && !message.isLoading ? (
                  <MarkdownMessage content={message.content} />
                ) : (
                  message.content
                )}
              </div>
            </div>
          ))}
        </div>

        <AiBottomComposer
          value={composerValue}
          onChange={onComposerChange}
          onSubmit={onComposerSubmit}
          disabled={isSubmitting}
        />
      </div>
    </section>
  )
}

function AiBottomComposer({
  value,
  onChange,
  onSubmit,
  disabled = false,
}: {
  value: string
  onChange: (value: string) => void
  onSubmit: () => void
  disabled?: boolean
}) {
  const canSubmit = value.trim().length > 0 && !disabled

  return (
    <div className="z-10 mx-auto mb-3 w-full max-w-5xl shrink-0 px-1 pr-2">
      <div className="ai-bottom-composer w-full rounded-[20px] border border-border/80 bg-card/95 px-4 py-3 backdrop-blur-xl">
        <div className="relative min-w-0">
          <textarea
            value={value}
            disabled={disabled}
            onChange={(event) => onChange(event.target.value)}
            onKeyDown={(event) => {
              if (event.key !== "Enter" || event.shiftKey) return
              event.preventDefault()
              onSubmit()
            }}
            className="min-h-[4.5rem] w-full resize-none rounded-[16px] border border-transparent bg-transparent py-0.5 pl-1 pr-14 text-[15px] leading-6 text-foreground outline-none placeholder:text-muted-foreground"
            placeholder="업무일지에 대해 이어서 물어보세요"
          />
          <button
            type="button"
            className="absolute right-1 top-1 flex size-10 items-center justify-center rounded-full bg-foreground text-background shadow-sm transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-35"
            aria-label="AI 모드 질문 전송"
            disabled={!canSubmit}
            onClick={onSubmit}
          >
            <ArrowUp className="size-5" />
          </button>
          <div className="mt-1 flex items-center gap-2 text-muted-foreground">
            <p className="text-xs">Enter로 전송 · Shift+Enter로 줄바꿈</p>
          </div>
        </div>
      </div>
    </div>
  )
}

function useDebouncedValue<T>(value: T, delayMs: number) {
  const [debouncedValue, setDebouncedValue] = useState(value)

  useEffect(() => {
    const debounceTimer = window.setTimeout(() => {
      setDebouncedValue(value)
    }, delayMs)

    return () => window.clearTimeout(debounceTimer)
  }, [delayMs, value])

  return debouncedValue
}

function MarkdownMessage({ content }: { content: string }) {
  const lines = content
    .trim()
    .split("\n")
    .map((line) => line.trim())

  if (lines.every((line) => line.length === 0)) {
    return null
  }

  const elements: ReactNode[] = []
  let paragraphLines: string[] = []
  let unorderedItems: string[] = []
  let orderedItems: string[] = []

  const flushParagraph = () => {
    if (paragraphLines.length === 0) return

    const paragraph = paragraphLines.join("\n")
    elements.push(
      <p key={`paragraph-${elements.length}`} className="whitespace-pre-line">
        {renderInlineMarkdown(paragraph)}
      </p>
    )
    paragraphLines = []
  }

  const flushUnorderedList = () => {
    if (unorderedItems.length === 0) return

    elements.push(
      <ul key={`unordered-${elements.length}`} className="list-disc space-y-1 pl-5">
        {unorderedItems.map((item, index) => (
          <li key={`${item}-${index}`}>{renderInlineMarkdown(item)}</li>
        ))}
      </ul>
    )
    unorderedItems = []
  }

  const flushOrderedList = () => {
    if (orderedItems.length === 0) return

    elements.push(
      <ol key={`ordered-${elements.length}`} className="list-decimal space-y-1 pl-5">
        {orderedItems.map((item, index) => (
          <li key={`${item}-${index}`}>{renderInlineMarkdown(item)}</li>
        ))}
      </ol>
    )
    orderedItems = []
  }

  const flushLists = () => {
    flushUnorderedList()
    flushOrderedList()
  }

  lines.forEach((line) => {
    if (!line) {
      flushParagraph()
      flushLists()
      return
    }

    const heading = line.match(/^(#{1,3})\s+(.+)$/)
    const unorderedItem = line.match(/^[-*]\s+(.+)$/)
    const orderedItem = line.match(/^\d+[.)]\s+(.+)$/)

    if (heading) {
      flushParagraph()
      flushLists()

      const headingLevel = heading[1].length
      const HeadingTag =
        headingLevel === 1 ? "h2" : headingLevel === 2 ? "h3" : "h4"

      elements.push(
        <HeadingTag
          key={`heading-${elements.length}`}
          className={cn(
            "font-semibold text-foreground",
            headingLevel === 1 && "text-base",
            headingLevel === 2 && "text-[15px]",
            headingLevel === 3 && "text-sm"
          )}
        >
          {renderInlineMarkdown(heading[2])}
        </HeadingTag>
      )
      return
    }

    if (unorderedItem) {
      flushParagraph()
      flushOrderedList()
      unorderedItems.push(unorderedItem[1])
      return
    }

    if (orderedItem) {
      flushParagraph()
      flushUnorderedList()
      orderedItems.push(orderedItem[1])
      return
    }

    flushLists()
    paragraphLines.push(line)
  })

  flushParagraph()
  flushLists()

  return <div className="space-y-3">{elements}</div>
}

function renderMarkdownLink(
  label: ReactNode,
  href: string,
  key: string,
  fallbackText = href
): ReactNode {
  const normalizedHref = href.trim()

  if (!isSafeMarkdownHref(normalizedHref)) {
    return renderEmojiShortcodes(fallbackText)
  }

  return (
    <a
      key={key}
      href={normalizedHref}
      target={normalizedHref.startsWith("/") ? undefined : "_blank"}
      rel={normalizedHref.startsWith("/") ? undefined : "noreferrer"}
      className="font-medium text-primary underline underline-offset-4"
    >
      {label}
    </a>
  )
}

function renderInlineMarkdown(text: string): ReactNode[] {
  const parts = text.split(
    /(\[[^\]\n]+\]\([^)]+\)|\*\*[^*\n]+\*\*|`[^`\n]+`|https?:\/\/[^\s<)]+)/g
  )

  return parts.reduce<ReactNode[]>((nodes, part, index) => {
    if (!part) {
      return nodes
    }

    const link = part.match(/^\[([^\]\n]+)\]\(([^)]+)\)$/)

    if (link) {
      const [, label, href] = link
      nodes.push(
        renderMarkdownLink(
          renderEmojiShortcodes(label),
          href,
          `${part}-${index}`,
          part
        )
      )

      return nodes
    }

    if (/^https?:\/\/[^\s<)]+$/.test(part)) {
      const trailingMark = part.match(/[.,;:!?)]$/)?.[0] ?? ""
      const href = trailingMark ? part.slice(0, -1) : part

      nodes.push(renderMarkdownLink(href, href, `${part}-${index}`))

      if (trailingMark) {
        nodes.push(trailingMark)
      }

      return nodes
    }

    if (part.startsWith("**") && part.endsWith("**")) {
      nodes.push(
        <strong key={`${part}-${index}`}>
          {renderEmojiShortcodes(part.slice(2, -2))}
        </strong>
      )

      return nodes
    }

    if (part.startsWith("`") && part.endsWith("`")) {
      nodes.push(
        <code
          key={`${part}-${index}`}
          className="rounded bg-muted px-1.5 py-0.5 text-[0.92em]"
        >
          {part.slice(1, -1)}
        </code>
      )

      return nodes
    }

    nodes.push(...renderEmojiShortcodes(part))
    return nodes
  }, [])
}

function renderEmojiShortcodes(text: string): ReactNode[] {
  const parts = text.split(/(:[a-z0-9_+-]+:)/gi)

  return parts.map((part) => MARKDOWN_EMOJI_SHORTCODES[part.toLowerCase()] ?? part)
}

function isSafeMarkdownHref(href: string) {
  const trimmedHref = href.trim()

  if (trimmedHref.startsWith("/")) {
    return true
  }

  try {
    const url = new URL(trimmedHref)
    return url.protocol === "http:" || url.protocol === "https:"
  } catch {
    return false
  }
}
