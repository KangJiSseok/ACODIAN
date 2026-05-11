"use client"

import { useMemo, useState, type ReactNode } from "react"
import {
  ChevronRight,
  FileText,
  GitBranchPlus,
  Settings2,
  Tag,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { CardSpotlight } from "@/components/ui/card-spotlight"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"
import { useAuth } from "../_hooks/useAuth"
import { tags, teams, users, worklogs } from "../_mock/worklog.mock"
import type {
  WorklogFormDependencyOption,
  WorklogFormTagOption,
  WorklogFormTeamOption,
  WorklogFormUserOption,
  WorklogFormValues,
  WorklogStatus,
} from "../_types/worklog.types"
import { getWorklogStatusLabel } from "../_utils/worklogFormat"
import { worklogStatusLegendOrder } from "./worklogBadgeConfig"
import { WorklogFileUpload } from "./worklogFileUpload"
import {
  WorklogFormModals,
  type WorklogFormModalKey,
} from "./worklogFormModals"

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
  pool: WorklogFormDependencyOption[],
) {
  const adjacency = new Map<number, number[]>()
  pool.forEach((worklog) => adjacency.set(worklog.id, worklog.dependencyIds ?? []))
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
  teamOptionsSource,
  authorOptionsSource,
  dependencyOptionsSource,
  tagOptionsSource,
}: {
  initialValues?: WorklogFormValues
  onSubmit: (values: WorklogFormValues) => Promise<void> | void
  submitLabel: string
  currentWorklogId?: number
  teamOptionsSource?: WorklogFormTeamOption[]
  authorOptionsSource?: WorklogFormUserOption[]
  dependencyOptionsSource?: WorklogFormDependencyOption[]
  tagOptionsSource?: WorklogFormTagOption[]
}) {
  const controlClassName = "h-11 rounded-2xl px-4 text-sm"
  const searchControlClassName = "h-11 rounded-2xl pl-11 pr-4 text-sm"
  const textareaClassName =
    "dashboard-scrollbar resize-none rounded-[1.25rem] px-4 py-3 text-base overflow-y-auto [scrollbar-gutter:stable]"
  const { user } = useAuth()
  const isEditMode = currentWorklogId !== undefined
  const [activeModal, setActiveModal] = useState<WorklogFormModalKey | null>(null)
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
  const [dependencySearchOpen, setDependencySearchOpen] = useState(false)
  const [tagKeywordInput, setTagKeywordInput] = useState("")
  const [tagSearchOpen, setTagSearchOpen] = useState(false)

  const teamSource = useMemo<WorklogFormTeamOption[]>(
    () =>
      teamOptionsSource ??
      teams.map((team) => ({
        id: team.id,
        name: team.name,
      })),
    [teamOptionsSource],
  )
  const authorSource = useMemo<WorklogFormUserOption[]>(
    () =>
      authorOptionsSource ??
      users.map((member) => ({
        id: member.id,
        name: member.name,
        title: member.title,
      })),
    [authorOptionsSource],
  )
  const dependencySource = useMemo<WorklogFormDependencyOption[]>(
    () =>
      dependencyOptionsSource ??
      worklogs.map((worklog) => ({
        id: worklog.id,
        title: worklog.title,
        status: worklog.status,
        teamId: worklog.teamId,
        authorId: worklog.authorId,
        aiSummary: worklog.aiSummary,
        workContent: worklog.workContent,
        requestContent: worklog.requestContent,
        isDeleted: worklog.isDeleted,
        dependencyIds: worklog.dependencyIds,
      })),
    [dependencyOptionsSource],
  )
  const tagSource = useMemo<WorklogFormTagOption[]>(
    () => tagOptionsSource ?? tags,
    [tagOptionsSource],
  )

  const teamOptions = useMemo(
    () => teamSource.map((team) => ({ label: team.name, value: String(team.id) })),
    [teamSource],
  )
  const authorOptions = useMemo(
    () =>
      authorSource.map((member) => ({
        label: member.title ? `${member.name} / ${member.title}` : member.name,
        value: String(member.id),
      })),
    [authorSource],
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
      dependencySource.filter(
        (worklog) => !worklog.isDeleted && worklog.id !== currentWorklogId,
      ),
    [currentWorklogId, dependencySource],
  )
  const filteredDependencyCandidates = useMemo(() => {
    const normalizedKeyword = dependencyKeywordInput.trim().toLowerCase()
    if (!normalizedKeyword) return []

    return dependencyCandidates
      .filter((dependency) => {
        if (values.dependencyIds.includes(dependency.id)) return false

        const teamName =
          teamSource.find((team) => team.id === dependency.teamId)?.name ?? ""
        const authorName =
          authorSource.find((member) => member.id === dependency.authorId)?.name ?? ""

        const searchableText = [
          dependency.title,
          dependency.aiSummary ?? "",
          dependency.workContent ?? "",
          dependency.requestContent ?? "",
          getWorklogStatusLabel(dependency.status),
          teamName,
          authorName,
        ]
          .join(" ")
          .toLowerCase()

        return searchableText.includes(normalizedKeyword)
      })
      .slice(0, 6)
  }, [
    authorSource,
    dependencyCandidates,
    dependencyKeywordInput,
    teamSource,
    values.dependencyIds,
  ])
  const selectedDependencies = useMemo(
    () =>
      dependencyCandidates.filter((dependency) =>
        values.dependencyIds.includes(dependency.id),
      ),
    [dependencyCandidates, values.dependencyIds],
  )
  const selectedTags = useMemo(
    () => tagSource.filter((tag) => values.tagIds.includes(tag.id)),
    [tagSource, values.tagIds],
  )
  const filteredTagCandidates = useMemo(() => {
    const normalizedKeyword = tagKeywordInput.trim().toLowerCase()
    if (!normalizedKeyword) return []

    return tagSource
      .filter((tag) => {
        if (values.tagIds.includes(tag.id)) return false

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
      .slice(0, 8)
  }, [tagKeywordInput, tagSource, values.tagIds])

  const incompleteDependencies = dependencyCandidates.filter(
    (worklog) =>
      values.dependencyIds.includes(worklog.id) && worklog.status !== "DONE",
  )
  const circularDependencyDetected =
    currentWorklogId !== undefined &&
    hasCircularDependency(currentWorklogId, values.dependencyIds, dependencySource)

  const addDependency = (dependencyId: number) => {
    setValues((previous) => ({
      ...previous,
      dependencyIds: Array.from(new Set([...previous.dependencyIds, dependencyId])),
    }))
    setDependencyKeywordInput("")
    setDependencySearchOpen(false)
  }

  const removeDependency = (dependencyId: number) => {
    setValues((previous) => ({
      ...previous,
      dependencyIds: previous.dependencyIds.filter((item) => item !== dependencyId),
    }))
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
    setTagKeywordInput("")
    setTagSearchOpen(false)
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
      <div className="grid gap-5">
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

            <Field
              label="요청/지시 내용"
              actions={
                <div className="flex flex-wrap items-center gap-2">
                  <InlineActionButton
                    icon={<GitBranchPlus className="size-3.5" />}
                    label="선행 업무 선택"
                    count={selectedDependencies.length}
                    onClick={() => setActiveModal("dependencies")}
                  />
                  <InlineActionButton
                    icon={<Settings2 className="size-3.5" />}
                    label="작업 설정 열기"
                    onClick={() => setActiveModal("settings")}
                  />
                  <InlineActionButton
                    icon={<Tag className="size-3.5" />}
                    label="태그 선택"
                    count={selectedTags.length}
                    onClick={() => setActiveModal("tags")}
                  />
                </div>
              }
            >
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
                <div
                  className="min-h-[132px] rounded-[1.25rem] border border-dashed border-border bg-muted/45 px-4 py-4 shadow-inner shadow-background/60"
                  aria-readonly="true"
                >
                  <p className="whitespace-pre-wrap text-sm leading-7 text-muted-foreground">
                    {values.aiSummary?.trim() || "AI 요약 정보가 없습니다."}
                  </p>
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
      </div>

      <WorklogFormModals
        activeModal={activeModal}
        onActiveModalChange={setActiveModal}
        values={values}
        onValuesChange={setValues}
        controlClassName={controlClassName}
        searchControlClassName={searchControlClassName}
        teamOptions={teamOptions}
        authorOptions={authorOptions}
        statusOptions={statusOptions}
        dependencyKeywordInput={dependencyKeywordInput}
        onDependencyKeywordInputChange={setDependencyKeywordInput}
        dependencySearchOpen={dependencySearchOpen}
        onDependencySearchOpenChange={setDependencySearchOpen}
        filteredDependencyCandidates={filteredDependencyCandidates}
        selectedDependencies={selectedDependencies}
        onAddDependency={addDependency}
        onRemoveDependency={removeDependency}
        incompleteDependencies={incompleteDependencies}
        circularDependencyDetected={circularDependencyDetected}
        tagKeywordInput={tagKeywordInput}
        onTagKeywordInputChange={setTagKeywordInput}
        tagSearchOpen={tagSearchOpen}
        onTagSearchOpenChange={setTagSearchOpen}
        filteredTagCandidates={filteredTagCandidates}
        selectedTags={selectedTags}
        onAddTag={addTag}
        onRemoveTag={removeTag}
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

function Field({
  label,
  actions,
  children,
}: {
  label: string
  actions?: ReactNode
  children: ReactNode
}) {
  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <label className="inline-flex items-center gap-2 text-[15px] font-[600] text-foreground">
          {label}
        </label>
        {actions}
      </div>
      <div>{children}</div>
    </div>
  )
}

function InlineActionButton({
  icon,
  label,
  count,
  onClick,
}: {
  icon: ReactNode
  label: string
  count?: number
  onClick: () => void
}) {
  return (
    <button
      type="button"
      className="group inline-flex h-9 items-center gap-2 rounded-2xl border border-primary/25 bg-primary/10 px-3.5 text-xs font-semibold text-primary shadow-[0_12px_28px_-22px_rgba(30,58,138,0.85)] transition-all hover:-translate-y-0.5 hover:border-primary/50 hover:bg-primary hover:text-primary-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
      onClick={onClick}
      aria-label={`${label} 설정 모달 열기`}
    >
      {icon}
      <span>{label}</span>
      {count !== undefined ? (
        <span className="rounded-full bg-primary px-1.5 py-0.5 text-[10px] leading-none text-primary-foreground transition-colors group-hover:bg-primary-foreground group-hover:text-primary">
          {count}개
        </span>
      ) : null}
      <ChevronRight className="size-3.5 transition-transform group-hover:translate-x-0.5" />
    </button>
  )
}
