"use client"

import { useMemo, useRef, useState, type ReactNode } from "react"
import {
  GitBranchPlus,
  RefreshCw,
  Search,
  Settings2,
  Tag,
  Upload,
  X,
} from "lucide-react"
import { useAuth } from "../_hooks/useAuth"
import { tags, teams, users, worklogs } from "../_mock/worklog.mock"
import { getTagSourceBadgeClass } from "../_utils/tagBadge"
import { worklogStatusLegendOrder } from "./worklogBadgeConfig"
import type { WorklogFormValues } from "../_types/worklog.types"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { CardSpotlight } from "@/components/ui/card-spotlight"
import { Checkbox } from "@/components/ui/checkbox"
import { Input } from "@/components/ui/input"
import { Select } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"
import { getImportanceLabel, getWorklogStatusLabel } from "../_utils/worklogFormat"

function normalizeDuration(actualHours: number) {
  const totalMinutes = Math.max(0, Math.round(actualHours * 60))
  // 시간 select가 5분 단위라 저장된 소수 시간을 가장 가까운 5분 단위로 맞춥니다.
  const snappedMinutes = Math.round(totalMinutes / 5) * 5
  const nextHour = Math.floor(snappedMinutes / 60)
  const nextMinute = snappedMinutes % 60

  return {
    hour: Math.min(nextHour, 24),
    minute: nextMinute,
  }
}

function hasCircularDependency(
  worklogId: number,
  dependencyIds: number[],
  pool: typeof worklogs
) {
  // 현재 편집 중인 dependencyIds를 임시 그래프에 반영해 저장 전 순환을 미리 감지합니다.
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
  const controlClassName = "h-14 rounded-2xl px-4 text-base"
  const textareaClassName = "rounded-2xl px-4 py-3 text-base"
  const { user } = useAuth()
  const isEditMode = currentWorklogId !== undefined
  const fileInputRef = useRef<HTMLInputElement>(null)
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
    }
  )
  const [submitError, setSubmitError] = useState("")
  const [dependencyKeywordInput, setDependencyKeywordInput] = useState("")
  const [dependencyKeyword, setDependencyKeyword] = useState("")
  const [tagKeywordInput, setTagKeywordInput] = useState("")
  const [tagKeyword, setTagKeyword] = useState("")

  const teamOptions = useMemo(
    () => teams.map((team) => ({ label: team.name, value: String(team.id) })),
    []
  )
  const authorOptions = useMemo(
    () =>
      users.map((member) => ({
        label: `${member.name} / ${member.title}`,
        value: String(member.id),
      })),
    []
  )
  const hourOptions = useMemo(
    () =>
      Array.from({ length: 25 }, (_, hour) => ({
        label: `${hour}시간`,
        value: String(hour),
      })),
    []
  )
  const minuteOptions = useMemo(
    () =>
      Array.from({ length: 12 }, (_, index) => index * 5).map((minute) => ({
        label: `${minute}분`,
        value: String(minute),
      })),
    []
  )
  const dependencyCandidates = useMemo(
    () =>
      worklogs.filter(
        (worklog) => !worklog.isDeleted && worklog.id !== currentWorklogId
      ),
    [currentWorklogId]
  )
  const filteredDependencyCandidates = useMemo(() => {
    const normalizedKeyword = dependencyKeyword.trim().toLowerCase()
    if (!normalizedKeyword) return dependencyCandidates

    // 선행 업무 검색은 제목뿐 아니라 요약, 업무 내용, 담당자, 팀, 상태까지 함께 탐색합니다.
    return dependencyCandidates.filter((dependency) => {
      const teamName = teams.find((team) => team.id === dependency.teamId)?.name ?? ""
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
    [values.tagIds]
  )
  const filteredTagCandidates = useMemo(() => {
    const normalizedKeyword = tagKeyword.trim().toLowerCase()

    // 이미 선택된 태그는 후보에서 제외해 중복 추가를 막습니다.
    return tags.filter((tag) => {
      if (values.tagIds.includes(tag.id)) return false
      if (!normalizedKeyword) return true

      const searchableText = [tag.name, tag.category, tag.source, tag.reuseHint]
        .join(" ")
        .toLowerCase()

      return searchableText.includes(normalizedKeyword)
    })
  }, [tagKeyword, values.tagIds])

  const incompleteDependencies = dependencyCandidates.filter(
    (worklog) =>
      values.dependencyIds.includes(worklog.id) && worklog.status !== "DONE"
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

  const { hour: normalizedHour, minute: normalizedMinute } = normalizeDuration(
    values.actualHours
  )

  const updateActualHours = (nextHour: number, nextMinute: number) => {
    // UI는 시간/분을 나눠 받지만 mock service에는 시간 단위 number로 전달합니다.
    setValues({
      ...values,
      actualHours: nextHour + nextMinute / 60,
    })
  }

  const addAttachmentNames = (names: string[]) => {
    setValues((previous) => ({
      ...previous,
      attachmentNames: Array.from(
        new Set([...previous.attachmentNames, ...names.filter(Boolean)])
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

        // 순환 의존성은 UI에서도 막고 service에서도 한 번 더 검증합니다.
        if (circularDependencyDetected) {
          setSubmitError(
            "순환 의존성이 감지되었습니다. 현재 업무를 다시 참조하는 연결을 해제해주세요."
          )
          return
        }

        setSubmitError("")
        await onSubmit(values)
      }}
    >
      <div className="grid gap-5 xl:grid-cols-[minmax(0,2.1fr)_minmax(420px,0.72fr)]">
        <FormPanel
          eyebrow="WORK SUMMARY & WORKFLOW"
          title="핵심 정보 및 작업 설정"
          icon={<Settings2 className="size-4" />}
          className="min-h-[820px]"
        >
          <div className="space-y-10">
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
                  className={`min-h-[220px] ${textareaClassName}`}
                  placeholder="이 업무를 수행해야 하는 목적과 배경을 작성합니다."
                />
              </Field>

              <Field label="업무 내용">
                <Textarea
                  value={values.workContent}
                  onChange={(event) =>
                    setValues({ ...values, workContent: event.target.value })
                  }
                  className={`min-h-[300px] ${textareaClassName}`}
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
                      className={`min-h-[132px] ${textareaClassName}`}
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
            </div>

            <div className="flex flex-col gap-5 border-t border-border/70 pt-6">
              <p className="text-xs uppercase tracking-[0.2em] text-muted-foreground">
                작업 설정
              </p>
              <div className="grid gap-5">
                <Field label="상태 및 중요도">
                  <div className="grid gap-4 sm:grid-cols-2">
                    <Select
                      className={controlClassName}
                      value={values.status}
                      options={[
                        ...worklogStatusLegendOrder.map((status) => ({
                          label: getWorklogStatusLabel(status),
                          value: status,
                        })),
                      ]}
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
                  <div className="grid gap-4 sm:grid-cols-2">
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
                  <div className="grid gap-4 sm:grid-cols-2">
                    <Select
                      className={controlClassName}
                      value={String(normalizedHour)}
                      options={hourOptions}
                      onChange={(event) =>
                        updateActualHours(Number(event.target.value), normalizedMinute)
                      }
                    />
                    <Select
                      className={controlClassName}
                      value={String(normalizedMinute)}
                      options={minuteOptions}
                      onChange={(event) =>
                        updateActualHours(normalizedHour, Number(event.target.value))
                      }
                    />
                  </div>
                </Field>

                <Field label="진행 일정">
                  <div className="grid gap-4 sm:grid-cols-[1fr_auto_1fr] sm:items-center">
                    <Input
                      className={controlClassName}
                      type="date"
                      value={values.instructionDate}
                      onChange={(event) =>
                        setValues({ ...values, instructionDate: event.target.value })
                      }
                    />
                    <span className="text-sm text-muted-foreground">~</span>
                    <Input
                      className={controlClassName}
                      type="date"
                      value={values.dueDate}
                      onChange={(event) =>
                        setValues({ ...values, dueDate: event.target.value })
                      }
                    />
                  </div>
                </Field>
              </div>

              <div className="flex justify-end pt-1">
                <Button
                  type="submit"
                  size="lg"
                  className="h-12 min-w-[168px] rounded-2xl px-7 font-semibold shadow-lg shadow-primary/15"
                >
                  {submitLabel}
                </Button>
              </div>
            </div>
          </div>
        </FormPanel>

        <div className="space-y-5">
          <FormPanel
            eyebrow="DEPENDENCIES"
            title="선행 업무"
            icon={<GitBranchPlus className="size-4" />}
          >
            <div className="flex gap-3">
              <div className="relative flex-1">
                <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  className="h-11 rounded-2xl pl-11"
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
                className="h-11 rounded-2xl px-5"
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
                        !values.dependencyIds.includes(dependency.id)
                      )
                    }
                    className={cn(
                      "flex w-full items-start gap-3 rounded-2xl border px-3 py-3 text-left text-sm shadow-sm transition-colors",
                      "border-border/70 bg-muted/35 text-foreground hover:border-primary/30 hover:bg-primary/6",
                      values.dependencyIds.includes(dependency.id)
                        ? "border-primary/35 bg-primary/8 ring-1 ring-primary/20"
                        : ""
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

          <FormPanel eyebrow="FILES" title="첨부 파일" icon={<Upload className="size-4" />}>
            <div className="rounded-2xl border border-border/70 bg-muted/25 p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-foreground">파일 업로드</p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    업무와 관련된 문서, 이미지, 자료 파일을 첨부합니다.
                  </p>
                </div>
                <Button
                  type="button"
                  variant="secondary"
                  className="h-11 rounded-2xl px-5"
                  onClick={() => fileInputRef.current?.click()}
                >
                  <Upload className="size-4" />
                  파일 선택
                </Button>
              </div>
            </div>
            <div className="space-y-2">
              {values.attachmentNames.length === 0 ? (
                <p className="rounded-2xl border border-dashed border-border/70 px-4 py-3 text-sm text-muted-foreground">
                  아직 업로드된 파일이 없습니다.
                </p>
              ) : (
                values.attachmentNames.map((name) => (
                  <div
                    key={name}
                    className="flex items-center justify-between gap-3 rounded-2xl border border-border/70 bg-muted/25 px-4 py-3 text-sm"
                  >
                    <span className="min-w-0 truncate font-medium text-foreground">
                      {name}
                    </span>
                    <button
                      type="button"
                      className="flex size-7 shrink-0 items-center justify-center rounded-lg text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
                      aria-label={`${name} 제거`}
                      onClick={() => removeAttachmentName(name)}
                    >
                      <X className="size-4" />
                    </button>
                  </div>
                ))
              )}
            </div>
          </FormPanel>

          <FormPanel eyebrow="TAGS" title="태그 등록" icon={<Tag className="size-4" />}>
            <div className="flex gap-3">
              <div className="relative flex-1">
                <Search className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
                <Input
                  className="h-11 rounded-2xl pl-11"
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
                className="h-11 rounded-2xl px-5"
                onClick={() => setTagKeyword(tagKeywordInput.trim())}
              >
                검색
              </Button>
            </div>

            <div className="flex flex-wrap gap-2">
              {selectedTags.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  선택한 태그가 없습니다. 저장 시 AI가 관련 태그를 자동으로 추가합니다.
                </p>
              ) : (
                selectedTags.map((tag) => (
                  <Badge
                    key={tag.id}
                    variant="outline"
                    className={cn(
                      "gap-1.5 rounded-full px-3 py-1.5",
                      getTagSourceBadgeClass(tag.source)
                    )}
                  >
                    #{tag.name}
                    <button
                      type="button"
                      className="rounded-full text-muted-foreground hover:text-foreground"
                      aria-label={`${tag.name} 태그 제거`}
                      onClick={() => removeTag(tag.id)}
                    >
                      <X className="size-3.5" />
                    </button>
                  </Badge>
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

      <input
        ref={fileInputRef}
        type="file"
        multiple
        className="hidden"
        onChange={(event) => {
          const nextNames = Array.from(event.target.files ?? []).map((file) => file.name)
          addAttachmentNames(nextNames)
          event.target.value = ""
        }}
      />

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
