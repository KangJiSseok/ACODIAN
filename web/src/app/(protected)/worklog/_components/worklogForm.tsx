"use client"

import { useMemo, useState, type ReactNode } from "react"
import {
  Calendar,
  FileText,
  GitBranchPlus,
  RefreshCw,
  Search,
  Settings2,
  Tag,
  X,
} from "lucide-react"
import { useAuth } from "../_hooks/useAuth"
import { tags, worklogs, teams, users } from "../_mock/worklog.mock"
import { getTagSourceBadgeClass } from "../_utils/tagBadge"
import { WorklogFileUpload } from "./worklogFileUpload"
import { worklogStatusLegendOrder } from "./worklogBadgeConfig"
import type { WorklogFormValues, WorklogStatus } from "../_types/worklog.types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { CardSpotlight } from "@/components/ui/card-spotlight"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { Select } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"
import { getImportanceLabel, getWorklogStatusLabel } from "../_utils/worklogFormat"

const editableStatusTransitionMap: Record<WorklogStatus, WorklogStatus[]> = {
  PENDING: ["IN_PROGRESS"],
  IN_PROGRESS: ["DONE", "ON_HOLD", "FAILED", "CANCELLED"],
  DONE: [],
  ON_HOLD: ["IN_PROGRESS", "FAILED"],
  FAILED: ["IN_PROGRESS"],
  CANCELLED: [],
}

function hasCircularDependency(
  worklogId: number,
  dependencyIds: number[],
  pool: typeof worklogs,
) {
  const adjacency = new Map<number, number[]>()
  pool.forEach((worklog) => adjacency.set(worklog.id, worklog.dependencyIds))
  adjacency.set(worklogId, dependencyIds)

  const visited = new Set<number>()
  const stack = new Set<number>()

  const dfs = (nodeId: number): boolean => {
    if (stack.has(nodeId)) return true
    if (visited.has(nodeId)) return false

    visited.add(nodeId)
    stack.add(nodeId)

    for (const nextId of adjacency.get(nodeId) ?? []) {
      if (dfs(nextId)) return true
    }

    stack.delete(nodeId)
    return false
  }

  return dfs(worklogId)
}

export function WorklogForm({
  initialValues,
  onSubmit,
  submitLabel,
  currentWorklogId,
}: {
  initialValues?: WorklogFormValues
  onSubmit: (values: WorklogFormValues) => Promise<void> | void
  submitLabel: string
  currentWorklogId?: number
}) {
  const controlClassName = "h-11 rounded-2xl px-4 text-sm"
  const searchControlClassName = "h-11 rounded-2xl pl-11 pr-4 text-sm"
  const textareaClassName =
    "dashboard-scrollbar resize-none rounded-[1.25rem] px-4 py-3 text-base overflow-y-auto [scrollbar-gutter:stable]"
  const { user } = useAuth()
  const isEditMode = currentWorklogId !== undefined
  const [values, setValues] = useState<WorklogFormValues>(
    initialValues ?? {
      title: "",
      requestContent: "",
      workContent: "",
      status: "PENDING",
      importance: "NORMAL",
      actualHours: 1,
      instructionDate: "2026-04-13",
      dueDate: "2026-04-16",
      teamId: user?.primaryTeamId ?? 11,
      authorId: user?.id ?? 7,
      dependencyIds: [],
      attachmentNames: [],
      tagIds: [],
    },
  )
  const [submitError, setSubmitError] = useState("")
  const [dependencyKeywordInput, setDependencyKeywordInput] = useState("")
  const [dependencyKeyword, setDependencyKeyword] = useState("")
  const [tagKeywordInput, setTagKeywordInput] = useState("")
  const [tagKeyword, setTagKeyword] = useState("")

  const teamOptions = useMemo(
    () => teams.map((team) => ({ label: team.name, value: String(team.id) })),
    [],
  )
  const authorOptions = useMemo(
    () =>
      users.map((member) => ({
        label: `${member.name} / ${member.title}`,
        value: String(member.id),
      })),
    [],
  )
  const statusOptions = useMemo(() => {
    const currentStatus = initialValues?.status ?? values.status
    const statusCandidates = isEditMode
      ? [currentStatus, ...editableStatusTransitionMap[currentStatus]]
      : worklogStatusLegendOrder

    return statusCandidates.map((status) => ({
      label: getWorklogStatusLabel(status),
      value: status,
    }))
  }, [initialValues?.status, isEditMode, values.status])
  const dependencyCandidates = useMemo(
    () =>
      worklogs.filter(
        (worklog) => !worklog.isDeleted && worklog.id !== currentWorklogId,
      ),
    [currentWorklogId],
  )
  const filteredDependencyCandidates = useMemo(() => {
    const normalizedKeyword = dependencyKeyword.trim().toLowerCase()
    if (!normalizedKeyword) return dependencyCandidates

    return dependencyCandidates.filter((dependency) => {
      const teamName =
        teams.find((team) => team.id === dependency.teamId)?.name ?? ""
      const authorName =
        users.find((member) => member.id === dependency.authorId)?.name ?? ""

      const searchableText = [
        dependency.title,
        dependency.aiSummary,
        dependency.workContent,
        dependency.requestContent,
        getWorklogStatusLabel(dependency.status),
        teamName,
        authorName,
      ]
        .join(" ")
        .toLowerCase()

      return searchableText.includes(normalizedKeyword)
    })
  }, [dependencyCandidates, dependencyKeyword])
  const selectedTags = useMemo(
    () => tags.filter((tag) => values.tagIds.includes(tag.id)),
    [values.tagIds],
  )
  const filteredTagCandidates = useMemo(() => {
    const normalizedKeyword = tagKeyword.trim().toLowerCase()

    return tags.filter((tag) => {
      if (values.tagIds.includes(tag.id)) return false
      if (!normalizedKeyword) return true

      const searchableText = [
        tag.name,
        tag.category,
        tag.source,
        tag.reuseHint,
      ]
        .join(" ")
        .toLowerCase()

      return searchableText.includes(normalizedKeyword)
    })
  }, [tagKeyword, values.tagIds])

  const incompleteDependencies = dependencyCandidates.filter(
    (worklog) =>
      values.dependencyIds.includes(worklog.id) && worklog.status !== "DONE",
  )
  const circularDependencyDetected =
    currentWorklogId !== undefined &&
    hasCircularDependency(currentWorklogId, values.dependencyIds, worklogs)

  const toggleDependency = (dependencyId: number, checked: boolean) => {
    const nextDependencyIds = checked
      ? Array.from(new Set([...values.dependencyIds, dependencyId]))
      : values.dependencyIds.filter((item) => item !== dependencyId)

    setValues({ ...values, dependencyIds: nextDependencyIds })
  }

  const addAttachmentNames = (names: string[]) => {
    setValues((previous) => ({
      ...previous,
      attachmentNames: Array.from(
        new Set([...previous.attachmentNames, ...names.filter(Boolean)]),
      ),
    }))
  }

  const removeAttachmentName = (name: string) => {
    setValues((previous) => ({
      ...previous,
      attachmentNames: previous.attachmentNames.filter((item) => item !== name),
    }))
  }

  const addTag = (tagId: number) => {
    setValues((previous) => ({
      ...previous,
      tagIds: Array.from(new Set([...previous.tagIds, tagId])),
    }))
  }

  const removeTag = (tagId: number) => {
    setValues((previous) => ({
      ...previous,
      tagIds: previous.tagIds.filter((item) => item !== tagId),
    }))
  }

  return (
    <form
      className="registration-surface flex w-full max-w-[1760px] flex-col gap-5 pb-10"
      onSubmit={async (event) => {
        event.preventDefault()

        if (circularDependencyDetected) {
          setSubmitError(
            "순환 의존성이 감지되었습니다. 현재 업무를 다시 참조하는 연결을 해제해주세요.",
          )
          return
        }

        setSubmitError("")
        await onSubmit(values)
      }}
    >
      <div className="grid gap-5 xl:grid-cols-[minmax(0,1.55fr)_minmax(440px,0.9fr)]">
        <FormPanel
          eyebrow="WORK SUMMARY"
          title="핵심 정보"
          icon={<FileText className="size-4" />}
          className="min-h-[720px]"
        >
          <div className="space-y-4">
            <p className="text-xs uppercase tracking-[0.2em] text-muted-foreground">
              핵심 정보
            </p>
            <Field label="제목">
              <Input
                className={controlClassName}
                value={values.title}
                onChange={(event) =>
                  setValues({ ...values, title: event.target.value })
                }
                placeholder="업무의 제목을 간결하게 작성하세요."
              />
            </Field>

            <Field label="요청/지시 내용">
              <Textarea
                value={values.requestContent}
                onChange={(event) =>
                  setValues({ ...values, requestContent: event.target.value })
                }
                className={`h-[220px] ${textareaClassName}`}
                placeholder="이 업무를 수행해야 하는 목적과 배경을 작성합니다."
              />
            </Field>

            <Field label="업무 내용">
              <Textarea
                value={values.workContent}
                onChange={(event) =>
                  setValues({ ...values, workContent: event.target.value })
                }
                className={`h-[300px] ${textareaClassName}`}
                placeholder="실제로 수행할 업무의 상세 내용을 작성합니다."
              />
            </Field>

            {isEditMode ? (
              <Field label="AI 요약">
                <div className="space-y-3">
                  <Textarea
                    value={values.aiSummary ?? ""}
                    onChange={(event) =>
                      setValues({
                        ...values,
                        aiSummary: event.target.value,
                        aiSummaryEdited: true,
                        aiRegenerateRequested: false,
                      })
                    }
                    className={`h-[132px] ${textareaClassName}`}
                    placeholder="AI가 생성한 요약을 확인하고 필요하면 직접 수정하세요."
                  />
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <p className="text-xs text-muted-foreground">
                      {values.aiRegenerateRequested
                        ? "저장하면 AI 요약 재생성 요청이 비동기로 시작됩니다."
                        : values.aiSummaryEdited
                          ? "직접 수정한 요약으로 저장됩니다."
                          : "현재 완료된 AI 요약입니다."}
                    </p>
                    <Button
                      type="button"
                      variant="secondary"
                      className="h-10 rounded-2xl px-4 text-sm"
                      disabled={values.aiRegenerateRequested}
                      onClick={() =>
                        setValues({
                          ...values,
                          aiRegenerateRequested: true,
                          aiSummaryEdited: false,
                        })
                      }
                    >
                      <RefreshCw className="size-4" />
                      {values.aiRegenerateRequested ? "재생성 요청됨" : "AI 요약 재생성 요청"}
                    </Button>
                  </div>
                </div>
              </Field>
            ) : null}
            <div className="border-t border-border/70 pt-6">
              <WorklogFileUpload
                attachmentNames={values.attachmentNames}
                onAddAttachmentNames={addAttachmentNames}
                onRemoveAttachmentName={removeAttachmentName}
              />
            </div>

            <div className="flex justify-end border-t border-border/70 pt-6">
              <Button
                type="submit"
                size="lg"
                className="h-12 min-w-[180px] rounded-2xl px-7 font-semibold shadow-[0_14px_40px_-20px_rgba(59,130,246,0.8)]"
              >
                {submitLabel}
              </Button>
            </div>
          </div>
          </FormPanel>

        <div className="space-y-5">
          <FormPanel
            eyebrow="WORK SETTINGS"
            title="작업 설정"
            icon={<Settings2 className="size-4" />}
          >
            <div className="grid gap-5">
              <Field label="상태 및 중요도">
                <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
                  <Select
                    className={controlClassName}
                    value={values.status}
                    options={statusOptions}
                    onChange={(event) =>
                      setValues({
                        ...values,
                        status: event.target.value as WorklogFormValues["status"],
                      })
                    }
                  />
                  <Select
                    className={controlClassName}
                    value={values.importance}
                    options={[
                      { label: getImportanceLabel("URGENT"), value: "URGENT" },
                      { label: getImportanceLabel("HIGH"), value: "HIGH" },
                      { label: getImportanceLabel("NORMAL"), value: "NORMAL" },
                      { label: getImportanceLabel("LOW"), value: "LOW" },
                    ]}
                    onChange={(event) =>
                      setValues({
                        ...values,
                        importance: event.target.value as WorklogFormValues["importance"],
                      })
                    }
                  />
                </div>
              </Field>

              <Field label="담당자">
                <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
                  <Select
                    className={controlClassName}
                    value={String(values.teamId)}
                    options={teamOptions}
                    onChange={(event) =>
                      setValues({ ...values, teamId: Number(event.target.value) })
                    }
                  />
                  <Select
                    className={controlClassName}
                    value={String(values.authorId)}
                    options={authorOptions}
                    onChange={(event) =>
                      setValues({ ...values, authorId: Number(event.target.value) })
                    }
                  />
                </div>
              </Field>

              <Field label="시간">
                <div className="space-y-2">
                  <Input
                    className={controlClassName}
                    type="number"
                    min="0"
                    step="0.5"
                    value={values.actualHours}
                    onChange={(event) =>
                      setValues({
                        ...values,
                        actualHours: Number(event.target.value),
                      })
                    }
                    placeholder="예: 1.5"
                  />
                  <p className="text-xs leading-5 text-muted-foreground">
                    소수 입력이 가능합니다. 예: 1.5 = 1시간 30분
                  </p>
                </div>
              </Field>

              <Field label="진행 일정">
                <div className="grid gap-3 sm:grid-cols-[1fr_auto_1fr] sm:items-center xl:grid-cols-1 2xl:grid-cols-[1fr_auto_1fr]">
                  <div className="relative">
                    <Calendar className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      className={cn(controlClassName, "worklog-date-input pl-11 pr-4")}
                      type="date"
                      value={values.instructionDate}
                      onChange={(event) =>
                        setValues({ ...values, instructionDate: event.target.value })
                      }
                    />
                  </div>
                  <span className="hidden text-center text-sm text-muted-foreground sm:block xl:hidden 2xl:block">
                    ~
                  </span>
                  <div className="relative">
                    <Calendar className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      className={cn(controlClassName, "worklog-date-input pl-11 pr-4")}
                      type="date"
                      value={values.dueDate}
                      onChange={(event) =>
                        setValues({ ...values, dueDate: event.target.value })
                      }
                    />
                  </div>
                </div>
              </Field>
            </div>
          </FormPanel>

          <FormPanel
            eyebrow="DEPENDENCIES"
            title="선행 업무"
            icon={<GitBranchPlus className="size-4" />}
          >
            <div className="flex gap-3">
              <div className="relative flex-1">
                <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  className={searchControlClassName}
                  value={dependencyKeywordInput}
                  onChange={(event) => setDependencyKeywordInput(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      event.preventDefault()
                      setDependencyKeyword(dependencyKeywordInput.trim())
                    }
                  }}
                  placeholder="제목, 요약, 담당자, 팀으로 검색"
                />
              </div>
              <Button
                type="button"
                variant="secondary"
                className="h-11 rounded-2xl px-5 text-sm"
                onClick={() => setDependencyKeyword(dependencyKeywordInput.trim())}
              >
                검색
              </Button>
            </div>

            <div className="grid gap-2 rounded-2xl border border-border/70 bg-muted/25 p-4">
              {filteredDependencyCandidates.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  조건에 맞는 선행 업무가 없습니다.
                </p>
              ) : (
                filteredDependencyCandidates.map((dependency) => (
                  <button
                    key={dependency.id}
                    type="button"
                    onClick={() =>
                      toggleDependency(
                        dependency.id,
                        !values.dependencyIds.includes(dependency.id),
                      )
                    }
                    className={cn(
                      "flex w-full items-start gap-3 rounded-2xl border px-3 py-3 text-left text-sm shadow-sm transition-colors",
                      "border-border/70 bg-muted/35 text-foreground hover:border-primary/30 hover:bg-primary/6",
                      values.dependencyIds.includes(dependency.id)
                        ? "border-primary/35 bg-primary/8 ring-1 ring-primary/20"
                        : "",
                    )}
                  >
                    <Checkbox
                      checked={values.dependencyIds.includes(dependency.id)}
                      readOnly
                      className="pointer-events-none"
                    />
                    <span className="space-y-1">
                      <span className="block font-medium">{dependency.title}</span>
                      <span className="block text-xs text-muted-foreground">
                        현재 상태: {getWorklogStatusLabel(dependency.status)}
                      </span>
                    </span>
                  </button>
                ))
              )}
            </div>

            {incompleteDependencies.length > 0 && values.status === "IN_PROGRESS" ? (
              <p className="text-xs text-[color:var(--warning)]">
                선행 업무가 아직 완료되지 않았습니다. 현재 와이어프레임에서는 경고만
                하고 저장은 허용합니다.
              </p>
            ) : null}
            {circularDependencyDetected ? (
              <p className="text-xs text-destructive">
                순환 의존성이 감지되었습니다. A → B → C → A 형태의 연결은 저장되지
                않습니다.
              </p>
            ) : null}
          </FormPanel>

          <FormPanel
            eyebrow="WORK SETTINGS"
            title="작업 설정"
            icon={<Settings2 className="size-4" />}
          >
            <div className="grid gap-5">
              <Field label="상태 및 중요도">
                <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
                  <Select
                    className={controlClassName}
                    value={values.status}
                    options={statusOptions}
                    onChange={(event) =>
                      setValues({
                        ...values,
                        status: event.target.value as WorklogFormValues["status"],
                      })
                    }
                  />
                  <Select
                    className={controlClassName}
                    value={values.importance}
                    options={[
                      { label: getImportanceLabel("URGENT"), value: "URGENT" },
                      { label: getImportanceLabel("HIGH"), value: "HIGH" },
                      { label: getImportanceLabel("NORMAL"), value: "NORMAL" },
                      { label: getImportanceLabel("LOW"), value: "LOW" },
                    ]}
                    onChange={(event) =>
                      setValues({
                        ...values,
                        importance: event.target.value as WorklogFormValues["importance"],
                      })
                    }
                  />
                </div>
              </Field>

              <Field label="담당자">
                <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-1 2xl:grid-cols-2">
                  <Select
                    className={controlClassName}
                    value={String(values.teamId)}
                    options={teamOptions}
                    onChange={(event) =>
                      setValues({ ...values, teamId: Number(event.target.value) })
                    }
                  />
                  <Select
                    className={controlClassName}
                    value={String(values.authorId)}
                    options={authorOptions}
                    onChange={(event) =>
                      setValues({ ...values, authorId: Number(event.target.value) })
                    }
                  />
                </div>
              </Field>

              <Field label="시간">
                <div className="space-y-2">
                  <div className="relative">
                    <Input
                      className={controlClassName}
                      type="number"
                      min="0"
                      step="0.5"
                      value={values.actualHours}
                      onChange={(event) =>
                        setValues({
                          ...values,
                          actualHours: Number(event.target.value),
                        })
                      }
                      placeholder="예: 1.5"
                    />
                  </div>
                  <p className="text-xs leading-5 text-muted-foreground">
                    소수 입력이 가능합니다. 예: 1.5 = 1시간 30분
                  </p>
                </div>
              </Field>

              <Field label="진행 일정">
                <div className="grid gap-3 sm:grid-cols-[1fr_auto_1fr] sm:items-center xl:grid-cols-1 2xl:grid-cols-[1fr_auto_1fr]">
                  <div className="relative">
                    <Calendar className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      className={cn(controlClassName, "worklog-date-input pl-11 pr-4")}
                      type="date"
                      value={values.instructionDate}
                      onChange={(event) =>
                        setValues({ ...values, instructionDate: event.target.value })
                      }
                    />
                  </div>
                  <span className="hidden text-center text-sm text-muted-foreground sm:block xl:hidden 2xl:block">
                    ~
                  </span>
                  <div className="relative">
                    <Calendar className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      className={cn(controlClassName, "worklog-date-input pl-11 pr-4")}
                      type="date"
                      value={values.dueDate}
                      onChange={(event) =>
                        setValues({ ...values, dueDate: event.target.value })
                      }
                    />
                  </div>
                </div>
              </Field>

            </div>
          </FormPanel>

          <FormPanel
            eyebrow="TAGS"
            title="태그 등록"
            icon={<Tag className="size-4" />}
          >
            <div className="flex gap-3">
              <div className="relative flex-1">
                <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  className={searchControlClassName}
                  value={tagKeywordInput}
                  onChange={(event) => setTagKeywordInput(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      event.preventDefault()
                      setTagKeyword(tagKeywordInput.trim())
                    }
                  }}
                  placeholder="태그명, 분류, 힌트로 검색"
                />
              </div>
              <Button
                type="button"
                variant="secondary"
                className="h-11 rounded-2xl px-5 text-sm"
                onClick={() => setTagKeyword(tagKeywordInput.trim())}
              >
                검색
              </Button>
            </div>

            <div className="space-y-2">
              {selectedTags.length === 0 ? (
                <p className="rounded-2xl border border-dashed border-border/70 px-4 py-3 text-sm text-muted-foreground">
                  선택한 태그가 없습니다. 저장 시 AI가 관련 태그를 자동으로 추가합니다.
                </p>
              ) : (
                selectedTags.map((tag) => (
                  <div
                    key={tag.id}
                    className="flex items-start justify-between gap-3 rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm"
                  >
                    <div className="min-w-0">
                      <Badge
                        variant="outline"
                        className={cn(
                          "rounded-full px-3 py-1",
                          getTagSourceBadgeClass(tag.source),
                        )}
                      >
                        #{tag.name}
                      </Badge>
                      <p className="mt-2 text-xs text-muted-foreground">
                        {tag.category} / {tag.usageCount}회 사용
                      </p>
                    </div>
                    <button
                      type="button"
                      className="flex size-7 shrink-0 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                      aria-label={`${tag.name} 태그 제거`}
                      onClick={() => removeTag(tag.id)}
                    >
                      <X className="size-4" />
                    </button>
                  </div>
                ))
              )}
            </div>

            <div className="grid max-h-[220px] gap-2 overflow-auto pr-1">
              {filteredTagCandidates.length === 0 ? (
                <p className="rounded-2xl border border-dashed border-border/70 px-4 py-3 text-sm text-muted-foreground">
                  조건에 맞는 태그가 없습니다.
                </p>
              ) : (
                filteredTagCandidates.map((tag) => (
                  <button
                    key={tag.id}
                    type="button"
                    className="rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-left text-sm transition-colors hover:border-primary/45 hover:bg-primary/8"
                    onClick={() => addTag(tag.id)}
                  >
                    <span className="block font-semibold text-foreground">#{tag.name}</span>
                    <span className="mt-1 block text-xs text-muted-foreground">
                      {tag.category} / {tag.usageCount}회 사용
                    </span>
                  </button>
                ))
              )}
            </div>
          </FormPanel>
        </div>
      </div>

      {submitError ? (
        <div className="rounded-xl border border-destructive/30 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          {submitError}
        </div>
      ) : null}
    </form>
  )
}

function FormPanel({
  eyebrow,
  title,
  icon,
  className,
  children,
}: {
  eyebrow: string
  title: string
  icon: ReactNode
  className?: string
  children: ReactNode
}) {
  return (
    <CardSpotlight className={cn("rounded-[28px]", className)}>
      <div className="space-y-6 p-6">
        <div className="flex items-start justify-between gap-4">
          <div className="space-y-2">
            <p className="text-xs uppercase tracking-[0.22em] text-muted-foreground">
              {eyebrow}
            </p>
            <h2 className="text-[22px] font-semibold tracking-[-0.05em] text-foreground">
              {title}
            </h2>
          </div>
          <div className="flex size-11 items-center justify-center rounded-2xl border border-primary/20 bg-gradient-to-br from-primary/16 via-primary/8 to-transparent text-primary">
            {icon}
          </div>
        </div>
        <div className="space-y-4">{children}</div>
      </div>
    </CardSpotlight>
  )
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="space-y-3">
      <label className="inline-flex items-center gap-2 text-[15px] font-[600] text-foreground">
        {label}
      </label>
      <div>{children}</div>
    </div>
  )
}
