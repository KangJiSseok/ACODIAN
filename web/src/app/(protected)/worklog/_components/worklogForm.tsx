"use client"

import { useMemo, useState, type ReactNode } from "react"
import {
  Check,
  ChevronRight,
  FileText,
  Loader2,
  Settings2,
  Sparkles,
  Tag,
  Wand2,
} from "lucide-react"
import { Button } from "@/components/ui/button"
import { CardSpotlight } from "@/components/ui/card-spotlight"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { getApiErrorMessage } from "@/app/_common/service/api-client"
import { cn } from "@/lib/utils"
import { worklogService } from "../_service/worklog.service"
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
const FILE_SIZE_UNIT = 1024 * 1024
const MAX_ATTACHMENT_COUNT = 10
const MAX_ATTACHMENT_FILE_SIZE_BYTES = 20 * FILE_SIZE_UNIT
const MAX_ATTACHMENT_TOTAL_SIZE_BYTES = 200 * FILE_SIZE_UNIT

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

function getTodayDateString() {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, "0")
  const date = String(today.getDate()).padStart(2, "0")

  return `${year}-${month}-${date}`
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
  const fallbackDate = getTodayDateString()
  const resolvedInitialValues = initialValues ?? {
    title: "",
    requestContent: "",
    workContent: "",
    status: "PENDING" as const,
    importance: "NORMAL" as const,
    actualHours: 0,
    instructionDate: fallbackDate,
    dueDate: fallbackDate,
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
  const [titleAssistError, setTitleAssistError] = useState("")
  const [polishAssistError, setPolishAssistError] = useState("")
  const [titleRecommendations, setTitleRecommendations] = useState<string[]>([])
  const [polishedWorkContent, setPolishedWorkContent] = useState("")
  const [polishedWorkContentSourceKey, setPolishedWorkContentSourceKey] =
    useState<string | null>(null)
  const [lastTitleRecommendationKey, setLastTitleRecommendationKey] = useState<
    string | null
  >(null)
  const [lastPolishAssistKey, setLastPolishAssistKey] = useState<string | null>(
    null,
  )
  const [isRecommendingTitles, setIsRecommendingTitles] = useState(false)
  const [isPolishingWorkContent, setIsPolishingWorkContent] = useState(false)
  const [showSettingsValidationErrors, setShowSettingsValidationErrors] =
    useState(false)
  const [dependencyKeywordInput, setDependencyKeywordInput] = useState("")
  const [dependencySearchOpen, setDependencySearchOpen] = useState(false)
  const [tagKeywordInput, setTagKeywordInput] = useState("")
  const [tagSearchOpen, setTagSearchOpen] = useState(false)
  const [selectedTagOptions, setSelectedTagOptions] = useState<
    WorklogFormTagOption[]
  >(() =>
    (tagOptionsSource ?? []).filter((tag) =>
      resolvedInitialValues.tagIds.includes(tag.id),
    ),
  )
  const settingsValidationErrors = getSettingsValidationErrors(
    actualHoursInput,
    {
      validateInvalidNumber: showSettingsValidationErrors,
    },
  )
  const initialStatus = initialValues?.status ?? resolvedInitialValues.status
  const statusChangeReasonVisible = isEditMode && values.status !== initialStatus
  const canRequestWritingAssist = values.workContent.trim().length > 0
  const writingAssistRequest = useMemo(
    () => ({
      requestContent: values.requestContent.trim() || null,
      workContent: values.workContent.trim(),
    }),
    [values.requestContent, values.workContent],
  )
  const writingAssistKey = useMemo(
    () => JSON.stringify(writingAssistRequest),
    [writingAssistRequest],
  )
  const isTitleRecommendationRepeated =
    lastTitleRecommendationKey === writingAssistKey
  const isPolishAssistRepeated = lastPolishAssistKey === writingAssistKey
  const canRecommendTitles =
    canRequestWritingAssist && !isRecommendingTitles && !isTitleRecommendationRepeated
  const canPolishWorkContent =
    canRequestWritingAssist && !isPolishingWorkContent && !isPolishAssistRepeated
  const canApplyPolishedWorkContent =
    polishedWorkContent.trim().length > 0 &&
    polishedWorkContentSourceKey === writingAssistKey

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
    () =>
      mergeTagOptions([...selectedTagOptions, ...tagSource]).filter((tag) =>
        values.tagIds.includes(tag.id),
      ),
    [selectedTagOptions, tagSource, values.tagIds],
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
    const tooLargeNames: string[] = []
    const limitExceededNames: string[] = []
    let currentTotalSize = getCurrentAttachmentSizeBytes(values)
    const nextFiles = files.filter((file) => {
      if (seenNames.has(file.name)) {
        duplicateNames.push(file.name)
        return false
      }
      if (file.size > MAX_ATTACHMENT_FILE_SIZE_BYTES) {
        tooLargeNames.push(file.name)
        return false
      }
      if (seenNames.size >= MAX_ATTACHMENT_COUNT) {
        limitExceededNames.push(file.name)
        return false
      }
      if (currentTotalSize + file.size > MAX_ATTACHMENT_TOTAL_SIZE_BYTES) {
        limitExceededNames.push(file.name)
        return false
      }
      seenNames.add(file.name)
      currentTotalSize += file.size
      return true
    })

    const errors = [
      duplicateNames.length > 0
        ? `이미 같은 이름의 첨부 파일이 있습니다: ${formatUniqueNames(duplicateNames)}`
        : "",
      tooLargeNames.length > 0
        ? `개당 최대 20MB를 초과한 파일은 제외했습니다: ${formatUniqueNames(tooLargeNames)}`
        : "",
      limitExceededNames.length > 0
        ? `첨부 파일은 최대 10개, 전체 200MB까지 업로드할 수 있습니다: ${formatUniqueNames(limitExceededNames)}`
        : "",
    ].filter(Boolean)

    if (errors.length > 0) {
      setSubmitError(errors.join(" "))
    } else if (submitError) {
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
    const selectedTag = tagSource.find((tag) => tag.id === tagId)

    if (selectedTag) {
      setSelectedTagOptions((current) =>
        mergeTagOptions([...current, selectedTag]),
      )
    }

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
    setSelectedTagOptions((current) => current.filter((tag) => tag.id !== tagId))
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

  const clearPolishedWorkContent = () => {
    setPolishedWorkContent("")
    setPolishedWorkContentSourceKey(null)
  }

  const recommendTitles = async () => {
    if (!canRequestWritingAssist) {
      setTitleAssistError("업무 내용을 먼저 입력해주세요.")
      return
    }
    if (isRecommendingTitles) return
    if (isTitleRecommendationRepeated) {
      setTitleAssistError(
        "이미 같은 내용으로 제목을 추천했습니다. 내용을 수정하면 다시 요청할 수 있습니다.",
      )
      return
    }

    setIsRecommendingTitles(true)
    setTitleAssistError("")
    try {
      const response = await worklogService.recommendTitles(writingAssistRequest)
      setTitleRecommendations(response.titles)
      setLastTitleRecommendationKey(writingAssistKey)
      if (response.titles.length === 0) {
        setTitleAssistError("추천할 수 있는 제목 후보가 없습니다.")
      }
    } catch (error) {
      setTitleAssistError(
        getApiErrorMessage(error, "AI 제목 추천을 처리하지 못했습니다.")
      )
    } finally {
      setIsRecommendingTitles(false)
    }
  }

  const polishWorkContent = async () => {
    if (!canRequestWritingAssist) {
      setPolishAssistError("업무 내용을 먼저 입력해주세요.")
      return
    }
    if (isPolishingWorkContent) return
    if (isPolishAssistRepeated) {
      setPolishAssistError(
        "이미 같은 내용으로 AI 내용 작성을 완료했습니다. 내용을 수정하면 다시 요청할 수 있습니다.",
      )
      return
    }

    setIsPolishingWorkContent(true)
    setPolishAssistError("")
    try {
      const response = await worklogService.polishDraft(writingAssistRequest)
      setPolishedWorkContent(response.workContent)
      setPolishedWorkContentSourceKey(writingAssistKey)
      setLastPolishAssistKey(writingAssistKey)
    } catch (error) {
      setPolishAssistError(
        getApiErrorMessage(error, "AI 내용 작성을 처리하지 못했습니다.")
      )
    } finally {
      setIsPolishingWorkContent(false)
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
            <Field
              label="제목"
              actions={
                <Button
                  type="button"
                  variant="secondary"
                  size="lg"
                  className="font-semibold"
                  disabled={!canRecommendTitles}
                  onClick={recommendTitles}
                  title={
                    isTitleRecommendationRepeated
                      ? "이미 같은 내용으로 제목을 추천했습니다."
                      : canRequestWritingAssist
                      ? "업무 내용을 바탕으로 제목 후보를 추천합니다."
                      : "업무 내용을 먼저 입력해주세요."
                  }
                >
                  {isRecommendingTitles ? (
                    <Loader2 className="size-4 animate-spin" />
                  ) : (
                    <Sparkles className="size-4" />
                  )}
                  AI 제목 추천
                </Button>
              }
            >
              <div className="space-y-3">
                <Input
                  className={controlClassName}
                  value={values.title}
                  maxLength={TITLE_MAX_LENGTH}
                  onChange={(event) =>
                    setValues({ ...values, title: event.target.value })
                  }
                  placeholder="업무의 제목을 간결하게 작성하세요."
                />
                {titleRecommendations.length > 0 ? (
                  <div className="flex flex-wrap gap-2">
                    {titleRecommendations.map((title) => (
                      <button
                        key={title}
                        type="button"
                        className="rounded-full border border-primary/20 bg-primary/10 px-3 py-1.5 text-xs font-semibold text-primary transition-colors hover:border-primary/40 hover:bg-primary/15"
                        onClick={() =>
                          setValues((previous) => ({ ...previous, title }))
                        }
                      >
                        {title}
                      </button>
                    ))}
                  </div>
                ) : null}
                {titleAssistError ? (
                  <p className="text-xs text-destructive">{titleAssistError}</p>
                ) : null}
              </div>
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
                onChange={(event) => {
                  setValues({ ...values, requestContent: event.target.value })
                  setTitleRecommendations([])
                  setTitleAssistError("")
                  setPolishAssistError("")
                  clearPolishedWorkContent()
                }}
                className={`h-[220px] ${textareaClassName}`}
                placeholder="이 업무를 수행해야 하는 목적과 배경을 작성합니다."
              />
              <p className="mt-1 text-right text-xs text-muted-foreground">
                {values.requestContent.length}/{CONTENT_MAX_LENGTH}
              </p>
            </Field>

            <Field label="업무 내용">
              <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_32px_minmax(0,1fr)]">
                <div className="space-y-2">
                  <div className="flex min-h-9 flex-wrap items-center justify-between gap-3">
                    <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                      원문
                    </p>
                    <Button
                      type="button"
                      variant="secondary"
                      size="lg"
                      className="font-semibold"
                      disabled={!canPolishWorkContent}
                      onClick={polishWorkContent}
                      title={
                        isPolishAssistRepeated
                          ? "이미 같은 내용으로 AI 내용 작성을 완료했습니다."
                          : canRequestWritingAssist
                          ? "업무 내용을 더 명확한 문장으로 정리합니다."
                          : "업무 내용을 먼저 입력해주세요."
                      }
                    >
                      {isPolishingWorkContent ? (
                        <Loader2 className="size-4 animate-spin" />
                      ) : (
                        <Wand2 className="size-4" />
                      )}
                      AI 내용 작성
                    </Button>
                  </div>
                  <Textarea
                    value={values.workContent}
                    maxLength={CONTENT_MAX_LENGTH}
                    onChange={(event) => {
                      setValues({ ...values, workContent: event.target.value })
                      setTitleRecommendations([])
                      setTitleAssistError("")
                      setPolishAssistError("")
                      clearPolishedWorkContent()
                    }}
                    className={`h-[300px] ${textareaClassName}`}
                    placeholder="실제로 수행할 업무의 상세 내용을 작성합니다."
                  />
                </div>
                <div className="hidden items-center justify-center pt-12 text-muted-foreground xl:flex">
                  <ChevronRight className="size-5" />
                </div>
                <div className="space-y-2">
                  <div className="flex min-h-9 flex-wrap items-center justify-between gap-2">
                    <p className="text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
                      정리된 업무 내용
                    </p>
                    <Button
                      type="button"
                      variant="outline"
                      className="h-9 rounded-lg px-3.5 text-xs font-semibold"
                      disabled={!canApplyPolishedWorkContent}
                      onClick={() => {
                        setValues((previous) => ({
                          ...previous,
                          workContent: polishedWorkContent,
                        }))
                        setTitleRecommendations([])
                        setTitleAssistError("")
                        setPolishAssistError("")
                      }}
                    >
                      <Check className="size-3.5" />
                      본문에 반영
                    </Button>
                  </div>
                  <Textarea
                    value={polishedWorkContent}
                    maxLength={CONTENT_MAX_LENGTH}
                    onChange={(event) => setPolishedWorkContent(event.target.value)}
                    className={`h-[300px] ${textareaClassName}`}
                    placeholder="AI 내용 작성 결과가 여기에 표시됩니다."
                  />
                </div>
              </div>
              {polishAssistError ? (
                <p className="mt-2 text-xs text-destructive">
                  {polishAssistError}
                </p>
              ) : null}
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
                maxFileCount={MAX_ATTACHMENT_COUNT}
                maxFileSizeMb={MAX_ATTACHMENT_FILE_SIZE_BYTES / FILE_SIZE_UNIT}
                maxTotalSizeMb={MAX_ATTACHMENT_TOTAL_SIZE_BYTES / FILE_SIZE_UNIT}
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

function getCurrentAttachmentSizeBytes(values: WorklogFormValues) {
  const removedFileIds = new Set(values.removeFileIds ?? [])
  const existingFileSizeBytes =
    values.attachmentFileItems
      ?.filter((file) => !removedFileIds.has(file.fileId))
      .reduce((sum, file) => sum + file.fileSizeBytes, 0) ?? 0

  return values.attachmentFiles.reduce(
    (sum, file) => sum + file.size,
    existingFileSizeBytes,
  )
}

function formatUniqueNames(names: string[]) {
  return Array.from(new Set(names)).join(", ")
}

function mergeTagOptions(tags: WorklogFormTagOption[]) {
  return Array.from(new Map(tags.map((tag) => [tag.id, tag])).values())
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
    <CardSpotlight className={cn("rounded-[28px]", className)} disableSpotlight>
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
      className="relative inline-flex h-9 items-center gap-2 rounded-lg border border-primary/25 bg-primary/10 px-3.5 text-xs font-semibold text-primary shadow-[0_12px_28px_-22px_rgba(30,58,138,0.85)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
      onClick={onClick}
      aria-label={
        count && count > 0
          ? `${label} 설정 모달 열기, 선택 ${count}개`
          : `${label} 설정 모달 열기`
      }
    >
      {icon}
      <span>{label}</span>
      <ChevronRight className="size-3.5" />
      {count && count > 0 ? (
        <span className="absolute -right-1.5 -top-1.5 flex min-w-4 items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-bold leading-4 text-white ring-2 ring-background">
          {count > 99 ? "99+" : count}
        </span>
      ) : null}
    </button>
  )
}
