"use client"

import { useMemo, useState, type ReactNode } from "react"
import {
  ChevronRight,
  FileText,
  Settings2,
  Tag,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { CardSpotlight } from "@/components/ui/card-spotlight"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { getApiErrorMessage } from "@/app/_common/service/api-client"
import { cn } from "@/lib/utils"
import type {
  WorklogFormDependencyOption,
  WorklogFormTagOption,
  WorklogFormTeamOption,
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
  IN_PROGRESS: ["DONE", "ON_HOLD", "CANCELLED"],
  DONE: [],
  ON_HOLD: ["IN_PROGRESS"],
  FAILED: ["IN_PROGRESS"],
  CANCELLED: [],
}

const creatableStatusOptions = worklogStatusLegendOrder
const TITLE_MAX_LENGTH = 100
const CONTENT_MAX_LENGTH = 2000

type SettingsValidationErrors = {
  actualHours?: string
}

function parseActualHoursInput(value: string) {
  const trimmedValue = value.trim()
  if (!trimmedValue) return 0
  return Number(trimmedValue)
}

function getSettingsValidationErrors(
  actualHoursInput: string,
  options: { validateInvalidNumber?: boolean } = {},
): SettingsValidationErrors {
  const errors: SettingsValidationErrors = {}
  const shouldValidateInvalidNumber = options.validateInvalidNumber ?? false
  const actualHours = parseActualHoursInput(actualHoursInput)

  if (!Number.isFinite(actualHours) && shouldValidateInvalidNumber) {
    errors.actualHours = "업무 소요 예상 시간을 숫자로 입력해주세요."
  } else if (actualHours < 0) {
    errors.actualHours = "업무 소요 예상 시간은 음수로 입력할 수 없습니다."
  }

  return errors
}

function hasSettingsValidationErrors(errors: SettingsValidationErrors) {
  return Boolean(errors.actualHours)
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
  onTeamIdChange,
  submitLabel,
  currentWorklogId,
  teamOptionsSource,
  dependencyOptionsSource,
  tagOptionsSource,
  hasMoreTagCandidates,
  isFetchingMoreTagCandidates,
  onTagSearchKeywordChange,
  onLoadMoreTagCandidates,
}: {
  initialValues?: WorklogFormValues
  onSubmit: (values: WorklogFormValues) => Promise<void> | void
  onTeamIdChange?: (teamId: number) => void
  submitLabel: string
  currentWorklogId?: number
  teamOptionsSource?: WorklogFormTeamOption[]
  dependencyOptionsSource?: WorklogFormDependencyOption[]
  tagOptionsSource?: WorklogFormTagOption[]
  hasMoreTagCandidates?: boolean
  isFetchingMoreTagCandidates?: boolean
  onTagSearchKeywordChange?: (keyword: string) => void
  onLoadMoreTagCandidates?: () => void
}) {
  const controlClassName = "h-11 rounded-2xl px-4 text-sm"
  const searchControlClassName = "h-11 rounded-2xl pl-11 pr-4 text-sm"
  const textareaClassName =
    "dashboard-scrollbar resize-none rounded-[1.25rem] px-4 py-3 text-base overflow-y-auto [scrollbar-gutter:stable]"
  const isEditMode = currentWorklogId !== undefined
  const resolvedInitialValues = initialValues ?? {
    title: "",
    requestContent: "",
    workContent: "",
    status: "PENDING" as const,
    importance: "NORMAL" as const,
    actualHours: 0,
    instructionDate: "2026-04-13",
    dueDate: "2026-04-16",
    teamId: teamOptionsSource?.[0]?.id ?? 0,
    dependencyIds: [],
    attachmentNames: [],
    attachmentFiles: [],
    attachmentFileItems: [],
    removeFileIds: [],
    tagIds: [],
    removeTagIds: [],
    statusChangeReason: "",
  }
  const [activeModal, setActiveModal] = useState<WorklogFormModalKey | null>(null)
  const [values, setValues] = useState<WorklogFormValues>(resolvedInitialValues)
  const [actualHoursInput, setActualHoursInput] = useState(() =>
    String(resolvedInitialValues.actualHours),
  )
  const [submitError, setSubmitError] = useState("")
  const [showSettingsValidationErrors, setShowSettingsValidationErrors] =
    useState(false)
  const [dependencyKeywordInput, setDependencyKeywordInput] = useState("")
  const [dependencySearchOpen, setDependencySearchOpen] = useState(false)
  const [tagKeywordInput, setTagKeywordInput] = useState("")
  const [tagSearchOpen, setTagSearchOpen] = useState(false)
  const settingsValidationErrors = getSettingsValidationErrors(
    actualHoursInput,
    {
      validateInvalidNumber: showSettingsValidationErrors,
    },
  )
  const initialStatus = initialValues?.status ?? resolvedInitialValues.status
  const statusChangeReasonVisible = isEditMode && values.status !== initialStatus

  const teamSource = useMemo<WorklogFormTeamOption[]>(
    () => teamOptionsSource ?? [],
    [teamOptionsSource],
  )
  const dependencySource = useMemo<WorklogFormDependencyOption[]>(
    () => dependencyOptionsSource ?? [],
    [dependencyOptionsSource],
  )
  const tagSource = useMemo<WorklogFormTagOption[]>(
    () => tagOptionsSource ?? [],
    [tagOptionsSource],
  )

  const teamOptions = useMemo(
    () => teamSource.map((team) => ({ label: team.name, value: String(team.id) })),
    [teamSource],
  )
  const statusOptions = useMemo(() => {
    const currentStatus = initialValues?.status ?? values.status
    const statusCandidates = isEditMode
      ? [currentStatus, ...editableStatusTransitionMap[currentStatus]]
      : creatableStatusOptions

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
          dependency.teamName ??
          teamSource.find((team) => team.id === dependency.teamId)?.name ??
          ""

        const searchableText = [
          dependency.title,
          dependency.aiSummary ?? "",
          dependency.workContent ?? "",
          dependency.requestContent ?? "",
          getWorklogStatusLabel(dependency.status),
          teamName,
          dependency.authorName ?? "",
        ]
          .join(" ")
          .toLowerCase()

        return searchableText.includes(normalizedKeyword)
      })
      .slice(0, 6)
  }, [
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
    return tagSource
      .filter((tag) => !values.tagIds.includes(tag.id))
  }, [tagSource, values.tagIds])

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

  const addAttachmentFiles = (files: File[]) => {
    const seenNames = new Set(values.attachmentNames)
    const duplicateNames: string[] = []
    const nextFiles = files.filter((file) => {
      if (seenNames.has(file.name)) {
        duplicateNames.push(file.name)
        return false
      }
      seenNames.add(file.name)
      return true
    })

    if (duplicateNames.length > 0) {
      setSubmitError(
        `이미 같은 이름의 첨부 파일이 있습니다: ${Array.from(new Set(duplicateNames)).join(", ")}`,
      )
    } else {
      setSubmitError("")
    }

    if (nextFiles.length === 0) {
      return
    }

    setValues((previous) => ({
      ...previous,
      attachmentFiles: [
        ...previous.attachmentFiles,
        ...nextFiles.filter(
          (file) =>
            !previous.attachmentFiles.some(
              (item) =>
                item.name === file.name &&
                item.size === file.size &&
                item.lastModified === file.lastModified,
            ),
        ),
      ],
      attachmentNames: Array.from(
        new Set([
          ...previous.attachmentNames,
          ...nextFiles.map((file) => file.name).filter(Boolean),
        ]),
      ),
    }))
  }

  const removeAttachmentName = (name: string) => {
    setValues((previous) => ({
      ...previous,
      attachmentNames: previous.attachmentNames.filter((item) => item !== name),
      attachmentFiles: previous.attachmentFiles.filter(
        (file) => file.name !== name,
      ),
      removeFileIds: appendRemovedFileId(previous, name),
    }))
  }

  const addTag = (tagId: number) => {
    setValues((previous) => ({
      ...previous,
      tagIds: Array.from(new Set([...previous.tagIds, tagId])),
      removeTagIds: (previous.removeTagIds ?? []).filter((item) => item !== tagId),
    }))
    setTagKeywordInput("")
    onTagSearchKeywordChange?.("")
    setTagSearchOpen(false)
  }

  const updateTagKeywordInput = (nextKeyword: string) => {
    setTagKeywordInput(nextKeyword)
    onTagSearchKeywordChange?.(nextKeyword)
  }

  const removeTag = (tagId: number) => {
    setValues((previous) => ({
      ...previous,
      tagIds: previous.tagIds.filter((item) => item !== tagId),
      removeTagIds: Array.from(new Set([...(previous.removeTagIds ?? []), tagId])),
    }))
  }

  const updateValues = (nextValues: WorklogFormValues) => {
    const teamChanged = nextValues.teamId !== values.teamId
    const resolvedValues = teamChanged
      ? { ...nextValues, dependencyIds: [] }
      : nextValues

    setValues(resolvedValues)
    if (teamChanged) {
      setDependencyKeywordInput("")
      setDependencySearchOpen(false)
      onTeamIdChange?.(nextValues.teamId)
    }

    if (
      showSettingsValidationErrors &&
      !hasSettingsValidationErrors(
        getSettingsValidationErrors(actualHoursInput, {
          validateInvalidNumber: true,
        }),
      )
    ) {
      setSubmitError("")
    }
  }

  const updateActualHoursInput = (nextInput: string) => {
    setActualHoursInput(nextInput)

    const nextActualHours = parseActualHoursInput(nextInput)
    if (Number.isFinite(nextActualHours)) {
      setValues((previous) => ({
        ...previous,
        actualHours: nextActualHours,
      }))
    }

    if (
      showSettingsValidationErrors &&
      !hasSettingsValidationErrors(
        getSettingsValidationErrors(nextInput, {
          validateInvalidNumber: true,
        }),
      )
    ) {
      setSubmitError("")
    }
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

        const nextSettingsErrors = getSettingsValidationErrors(actualHoursInput, {
          validateInvalidNumber: true,
        })
        if (hasSettingsValidationErrors(nextSettingsErrors)) {
          setShowSettingsValidationErrors(true)
          setActiveModal("settings")
          setSubmitError(
            "작업 설정에서 업무 소요 예상 시간을 확인해주세요.",
          )
          return
        }

        const submitValues = {
          ...values,
          actualHours: parseActualHoursInput(actualHoursInput),
        }

        setSubmitError("")
        try {
          await onSubmit(submitValues)
        } catch (error) {
          setSubmitError(
            getApiErrorMessage(error, "업무일지 저장 요청을 처리하지 못했습니다.")
          )
        }
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
                maxLength={TITLE_MAX_LENGTH}
                onChange={(event) =>
                  setValues({ ...values, title: event.target.value })
                }
                placeholder="업무의 제목을 간결하게 작성하세요."
              />
              <p className="mt-1 text-right text-xs text-muted-foreground">
                {values.title.length}/{TITLE_MAX_LENGTH}
              </p>
            </Field>

            <Field
              label="요청/지시 내용"
              actions={
                <div className="flex flex-wrap items-center gap-2">
                  <InlineActionButton
                    icon={<Settings2 className="size-3.5" />}
                    label="작업 설정 열기"
                    count={selectedDependencies.length}
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
                maxLength={CONTENT_MAX_LENGTH}
                onChange={(event) =>
                  setValues({ ...values, requestContent: event.target.value })
                }
                className={`h-[220px] ${textareaClassName}`}
                placeholder="이 업무를 수행해야 하는 목적과 배경을 작성합니다."
              />
              <p className="mt-1 text-right text-xs text-muted-foreground">
                {values.requestContent.length}/{CONTENT_MAX_LENGTH}
              </p>
            </Field>

            <Field label="업무 내용">
              <Textarea
                value={values.workContent}
                maxLength={CONTENT_MAX_LENGTH}
                onChange={(event) =>
                  setValues({ ...values, workContent: event.target.value })
                }
                className={`h-[300px] ${textareaClassName}`}
                placeholder="실제로 수행할 업무의 상세 내용을 작성합니다."
              />
              <p className="mt-1 text-right text-xs text-muted-foreground">
                {values.workContent.length}/{CONTENT_MAX_LENGTH}
              </p>
            </Field>

            {isEditMode ? (
              <Field label="AI 요약">
                <Textarea
                  value={values.aiSummary ?? ""}
                  onChange={(event) =>
                    setValues({ ...values, aiSummary: event.target.value })
                  }
                  className={`h-[132px] ${textareaClassName}`}
                  placeholder="AI 요약을 직접 수정할 수 있습니다."
                />
              </Field>
            ) : null}

            <div className="border-t border-border/70 pt-6">
              <WorklogFileUpload
                attachmentNames={values.attachmentNames}
                onAddAttachmentFiles={addAttachmentFiles}
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
        onValuesChange={updateValues}
        controlClassName={controlClassName}
        searchControlClassName={searchControlClassName}
        disableTeamChange={isEditMode}
        teamOptions={teamOptions}
        statusOptions={statusOptions}
        settingsValidationErrors={settingsValidationErrors}
        actualHoursInput={actualHoursInput}
        onActualHoursInputChange={updateActualHoursInput}
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
        statusChangeReasonVisible={statusChangeReasonVisible}
        tagKeywordInput={tagKeywordInput}
        onTagKeywordInputChange={updateTagKeywordInput}
        tagSearchOpen={tagSearchOpen}
        onTagSearchOpenChange={setTagSearchOpen}
        filteredTagCandidates={filteredTagCandidates}
        selectedTags={selectedTags}
        hasMoreTagCandidates={hasMoreTagCandidates}
        isFetchingMoreTagCandidates={isFetchingMoreTagCandidates}
        onLoadMoreTagCandidates={onLoadMoreTagCandidates}
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

function appendRemovedFileId(values: WorklogFormValues, filename: string) {
  const removedFileId = values.attachmentFileItems?.find(
    (file) => file.originalName === filename,
  )?.fileId

  if (!removedFileId) return values.removeFileIds ?? []
  return Array.from(new Set([...(values.removeFileIds ?? []), removedFileId]))
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
          <div className="flex size-11 items-center justify-center rounded-xl border border-primary/20 bg-gradient-to-br from-primary/16 via-primary/8 to-transparent text-primary">
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
      className="group relative inline-flex h-9 items-center gap-2 rounded-lg border border-primary/25 bg-primary/10 px-3.5 text-xs font-semibold text-primary shadow-[0_12px_28px_-22px_rgba(30,58,138,0.85)] transition-all hover:-translate-y-0.5 hover:border-primary/50 hover:bg-primary hover:text-primary-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
      onClick={onClick}
      aria-label={
        count && count > 0
          ? `${label} 설정 모달 열기, 선택 ${count}개`
          : `${label} 설정 모달 열기`
      }
    >
      {icon}
      <span>{label}</span>
      <ChevronRight className="size-3.5 transition-transform group-hover:translate-x-0.5" />
      {count && count > 0 ? (
        <span className="absolute -right-1.5 -top-1.5 flex min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-bold leading-4 text-white ring-2 ring-background group-hover:bg-red-600 group-hover:text-white">
          {count > 99 ? "99+" : count}
        </span>
      ) : null}
    </button>
  )
}
