import {
  files,
  getNextFileId,
  getNextNotificationId,
  getNextStatusHistoryId,
  notifications,
  notifyMockDb,
  tags,
  teams,
  type FileRecord,
  worklogs,
} from "../_mock/worklog.mock"
import { apiClient } from "@/app/_common/service/api-client"
import type { PageResponse } from "@/app/_common/types/api.types"

import type {
  AiProcessingStatus,
  CreateWorklogResponse,
  GetWorklogsParams,
  ImportanceLevel,
  SearchWorklogsParams,
  Worklog,
  WorklogDetailApiResponse,
  WorklogDetailStatusHistoryItem,
  WorklogFilterOptions,
  WorklogFormValues,
  WorklogListApiItem,
  WorklogListItem,
  WorklogOptionsApiResponse,
  WorklogRecord,
  WorklogSearchApiItem,
  WorklogStatus,
} from "../_types/worklog.types"

const worklogStatusCodeMap: Record<string, WorklogStatus> = {
  PENDING: "PENDING",
  IN_PROGRESS: "IN_PROGRESS",
  COMPLETED: "DONE",
  DONE: "DONE",
  ON_HOLD: "ON_HOLD",
  FAILED: "FAILED",
  CANCELLED: "CANCELLED",
}

const importanceCodeMap: Record<string, ImportanceLevel> = {
  URGENT: "URGENT",
  HIGH: "HIGH",
  NORMAL: "NORMAL",
  LOW: "LOW",
}

const aiProcessingStatusCodeMap: Record<string, AiProcessingStatus> = {
  PENDING: "PENDING",
  PROCESSING: "PROCESSING",
  COMPLETED: "DONE",
  DONE: "DONE",
  FAILED: "FAILED",
}

const worklogStatusApiCodeMap: Partial<Record<WorklogStatus, string>> = {
  PENDING: "PENDING",
  IN_PROGRESS: "IN_PROGRESS",
  DONE: "COMPLETED",
  ON_HOLD: "ON_HOLD",
  CANCELLED: "CANCELLED",
}

function toWorklogListItem(item: WorklogListApiItem): WorklogListItem {
  const aiSummary = item.aiSummary?.trim() || "AI 요약을 생성 중입니다."

  return {
    id: item.worklogId,
    title: item.title,
    status: worklogStatusCodeMap[item.statusCode] ?? "PENDING",
    workContent: item.workContent ?? "",
    actualHours: toNumber(item.actualHours),
    importance: importanceCodeMap[item.importanceCode] ?? "NORMAL",
    aiSummary,
    aiStatus: aiProcessingStatusCodeMap[item.aiProcessingStatus] ?? "PENDING",
    aiSummaryEdited: item.aiSummaryEdited ?? false,
    teamId: item.teamId,
    teamName: item.teamName,
    authorId: item.authorId,
    authorName: item.authorName,
    instructionDate: item.instructionDate,
    dueDate: item.dueDate,
    predecessorCount: item.predecessorCount,
  }
}

function toSearchedWorklogListItem(item: WorklogSearchApiItem): WorklogListItem {
  const aiSummary = item.aiSummary?.trim() || "AI 요약을 생성 중입니다."

  return {
    id: item.worklogId,
    title: item.title,
    status: worklogStatusCodeMap[item.statusCode] ?? "PENDING",
    workContent: item.workContent ?? "",
    actualHours: toNumber(item.actualHours),
    importance: importanceCodeMap[item.importanceCode] ?? "NORMAL",
    aiSummary,
    aiStatus: aiProcessingStatusCodeMap[item.aiProcessingStatus] ?? "PENDING",
    aiSummaryEdited: item.aiSummaryEdited ?? false,
    teamId: item.teamId,
    teamName: item.teamName,
    authorId: item.authorId,
    authorName: item.authorName,
    instructionDate: item.instructionDate,
    dueDate: item.dueDate,
    predecessorCount: item.predecessorCount ?? 0,
  }
}

function normalizeSearchParams(params: SearchWorklogsParams) {
  return {
    ...params,
    keyword: params.keyword?.trim() || undefined,
  }
}

function toArray<T>(value: T[] | null | undefined): T[] {
  return Array.isArray(value) ? value : []
}

function toNumber(value: number | string | null | undefined) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

function toWorklogStatus(code: string | null | undefined): WorklogStatus {
  return code ? worklogStatusCodeMap[code] ?? "PENDING" : "PENDING"
}

function toAiProcessingStatus(
  code: string | null | undefined
): AiProcessingStatus {
  return code ? aiProcessingStatusCodeMap[code] ?? "PENDING" : "PENDING"
}

function toStatusHistory(
  item: WorklogDetailStatusHistoryItem
): Worklog["statusHistory"][number] {
  return {
    id: item.historyId,
    previousStatus: item.previousStatusCode
      ? toWorklogStatus(item.previousStatusCode)
      : undefined,
    newStatus: toWorklogStatus(item.newStatusCode),
    changedBy: item.changedBy,
    changedByName: item.changedByName,
    reason: item.reason?.trim() || "-",
    changedAt: item.changedAt,
  }
}

function toWorklogDetail(item: WorklogDetailApiResponse): Worklog {
  const files = toArray(item.files)
  const tags = toArray(item.tags)
  const dependOnWorklogs = toArray(item.dependOnWorklogs)
  const statusHistory = toArray(item.statusHistories).map(toStatusHistory)
  const aiSummary = item.aiSummary?.trim() || "AI 요약 정보가 없습니다."

  return {
    id: item.worklogId,
    title: item.title,
    requestContent: item.requestContent,
    workContent: item.workContent,
    statusCode: item.statusCode,
    status: toWorklogStatus(item.statusCode),
    importance: item.importanceCode
      ? importanceCodeMap[item.importanceCode] ?? "NORMAL"
      : "NORMAL",
    actualHours: toNumber(item.actualHours),
    instructionDate: item.instructionDate,
    dueDate: item.dueDate,
    completionDate: item.completionDate ?? undefined,
    teamId: item.teamId,
    teamName: item.teamName,
    authorId: item.authorId,
    authorName: item.authorName,
    dependencyIds: dependOnWorklogs.map((dependency) => dependency.worklogId),
    dependOnWorklogs,
    aiSummary,
    aiSummaryEdited: item.aiSummaryEdited ?? false,
    aiStatus: toAiProcessingStatus(item.aiProcessingStatus),
    tagIds: [],
    tagNames: tags,
    fileIds: files.map((file) => file.fileId),
    fileItems: files,
    isDeleted: false,
    createdAt: item.createdAt,
    updatedAt: item.updatedAt,
    statusHistory,
  }
}

const nextMap: Record<WorklogRecord["status"], WorklogRecord["status"][]> = {
  PENDING: ["IN_PROGRESS"],
  IN_PROGRESS: ["DONE", "ON_HOLD", "FAILED", "CANCELLED"],
  ON_HOLD: ["IN_PROGRESS", "FAILED"],
  DONE: [],
  FAILED: ["IN_PROGRESS"],
  CANCELLED: [],
}

function normalizeAttachmentNames(names: string[]) {
  return Array.from(
    new Set(
      names
        .map((name) => name.trim())
        .filter(Boolean)
    )
  )
}

function getFileType(name: string) {
  const ext = name.split(".").pop()?.toUpperCase() ?? "FILE"
  return ext
}

function buildStoredPath(worklogId: number, teamId: number, filename: string) {
  const team = teams.find((item) => item.id === teamId)
  const departmentCode =
    team?.departmentId === 1 ? "DC" : team?.departmentId === 2 ? "SS" : "SD"
  const teamCode =
    team?.name
      .toUpperCase()
      .replace(/[^A-Z0-9]+/g, "-")
      .replace(/^-|-$/g, "") ?? "TEAM"
  const timestamp = new Date().toISOString().replace(/[-:TZ.]/g, "").slice(0, 14)
  return `${departmentCode}/${teamCode}/2026/04/${worklogId}_${filename}_${timestamp}`
}

function inferAiTagIds(
  values: Pick<WorklogFormValues, "title" | "requestContent" | "workContent">,
  selectedTagIds: number[]
) {
  const selectedSet = new Set(selectedTagIds)
  const searchableText = [values.title, values.requestContent, values.workContent]
    .join(" ")
    .toLowerCase()
  const matchedTagIds = tags
    .filter((tag) => {
      if (selectedSet.has(tag.id)) return false
      const tagName = tag.name.toLowerCase()
      return searchableText.includes(tagName)
    })
    .map((tag) => tag.id)
  const fallbackTagIds = tags
    .filter((tag) => tag.source === "AI" && !selectedSet.has(tag.id))
    .map((tag) => tag.id)

  return Array.from(new Set([...matchedTagIds, ...fallbackTagIds])).slice(0, 3)
}

function mergeSelectedAndAiTags(
  values: Pick<WorklogFormValues, "title" | "requestContent" | "workContent" | "tagIds">
) {
  const selectedTagIds = values.tagIds ?? []
  return Array.from(new Set([...selectedTagIds, ...inferAiTagIds(values, selectedTagIds)]))
}

function syncFiles(target: WorklogRecord, attachmentNames: string[], uploadedBy: number) {
  const normalizedNames = normalizeAttachmentNames(attachmentNames)
  const existingFiles = files.filter(
    (file) => target.fileIds.includes(file.id) && !file.isDeleted
  )

  existingFiles
    .filter((file) => !normalizedNames.includes(file.originalName))
    .forEach((file) => {
      file.isDeleted = true
    })

  const keptIds = existingFiles
    .filter((file) => normalizedNames.includes(file.originalName))
    .map((file) => file.id)

  const createdFiles = normalizedNames
    .filter(
      (filename) =>
        !existingFiles.some(
          (file) => file.originalName === filename && !file.isDeleted
        )
    )
    .map((filename) => {
      const created: FileRecord = {
        id: getNextFileId(),
        worklogId: target.id,
        originalName: filename,
        storedPath: buildStoredPath(target.id, target.teamId, filename),
        type: getFileType(filename),
        size: "1.0MB",
        summaryPreview: `${filename} 파일에 대한 AI 요약이 생성 대기 중입니다.`,
        aiStatus: "PENDING",
        uploadedAt: new Date().toISOString(),
        uploadedBy,
        isDeleted: false,
      }
      files.push(created)
      return created.id
    })

  target.fileIds = [...keptIds, ...createdFiles]
}

function hasCircularDependency(
  worklogId: number,
  dependencyIds: number[],
  pool: WorklogRecord[]
): boolean {
  const adjacency = new Map<number, number[]>()

  pool.forEach((worklog) => {
    adjacency.set(worklog.id, worklog.dependencyIds)
  })
  adjacency.set(worklogId, dependencyIds)

  const visited = new Set<number>()
  const stack = new Set<number>()

  const dfs = (nodeId: number): boolean => {
    if (stack.has(nodeId)) return true
    if (visited.has(nodeId)) return false

    visited.add(nodeId)
    stack.add(nodeId)

    const next = adjacency.get(nodeId) ?? []
    for (const candidate of next) {
      if (dfs(candidate)) return true
    }

    stack.delete(nodeId)
    return false
  }

  return dfs(worklogId)
}

function addDependencyNotifications(sourceWorklog: WorklogRecord) {
  worklogs
    .filter((worklog) => worklog.dependencyIds.includes(sourceWorklog.id))
    .forEach((dependentWorklog) => {
      notifications.unshift({
        id: getNextNotificationId(),
        userId: dependentWorklog.authorId,
        type: "DEPENDENCY",
        title: "선행 업무 상태가 변경되었습니다",
        content: `${sourceWorklog.title} 업무 상태가 변경되어 후행 업무 진행 전 확인이 필요합니다.`,
        referenceId: sourceWorklog.id,
        isRead: false,
        createdAt: new Date().toISOString(),
        sourceScope: "PERSONAL",
        deepLink: `/worklog/detail/${sourceWorklog.id}`,
      })
    })
}

export const worklogService = {
  async getWorklogs({
    page = 1,
    pageSize = 20,
  }: GetWorklogsParams = {}): Promise<PageResponse<WorklogListItem>> {
    const response = await apiClient.get<PageResponse<WorklogListApiItem>>(
      "/worklogs",
      {
        params: { page, pageSize },
      }
    )

    return {
      ...response,
      items: response.items.map(toWorklogListItem),
    }
  },
  async searchWorklogs({
    page = 1,
    pageSize = 20,
    ...params
  }: SearchWorklogsParams = {}): Promise<PageResponse<WorklogListItem>> {
    const response = await apiClient.get<PageResponse<WorklogSearchApiItem>>(
      "/worklogs/search",
      {
        params: normalizeSearchParams({ ...params, page, pageSize }),
      }
    )

    return {
      ...response,
      items: response.items.map(toSearchedWorklogListItem),
    }
  },
  async getFilterOptions(): Promise<WorklogFilterOptions> {
    return apiClient.get<WorklogFilterOptions>("/worklogs/filter-options")
  },
  async getOptions(teamId: number): Promise<WorklogOptionsApiResponse> {
    return apiClient.get<WorklogOptionsApiResponse>("/worklogs/options", {
      params: { teamId },
    })
  },
  async getById(id: number): Promise<Worklog | undefined> {
    const response = await apiClient.get<WorklogDetailApiResponse>(
      `/worklogs/${id}`
    )

    return toWorklogDetail(response)
  },
  async create(values: WorklogFormValues) {
    const request = {
      teamId: values.teamId,
      title: values.title,
      requestContent: values.requestContent || null,
      workContent: values.workContent,
      statusCode: worklogStatusApiCodeMap[values.status] ?? "PENDING",
      importanceCode: values.importance,
      actualHours: values.actualHours,
      instructionDate: values.instructionDate,
      dueDate: values.dueDate,
      tagIds: values.tagIds,
      predecessorWorklogIds: values.dependencyIds,
    }
    const formData = new FormData()

    formData.append(
      "request",
      new Blob([JSON.stringify(request)], { type: "application/json" })
    )
    if (values.attachmentFiles.length > 0) {
      values.attachmentFiles.forEach((file) => {
        formData.append("files", file, file.name)
      })
    } else {
      formData.append("files", new Blob([]), "__empty__")
    }

    return apiClient.post<CreateWorklogResponse, FormData>("/worklogs", formData)
  },
  async update(id: number, values: WorklogFormValues) {
    const target = worklogs.find((worklog) => worklog.id === id)
    if (!target) return undefined

    const {
      attachmentNames,
      attachmentFiles: _attachmentFiles,
      tagIds,
      aiSummary,
      aiSummaryEdited,
      ...worklogValues
    } = values
    void _attachmentFiles

    if (
      hasCircularDependency(
        id,
        values.dependencyIds.filter((dependencyId) => dependencyId !== id),
        worklogs
      )
    ) {
      throw new Error("순환 의존성이 감지되었습니다.")
    }

    const previousStatus = target.status
    const statusChanged = previousStatus !== values.status
    const today = new Date().toISOString().slice(0, 10)
    const nextAttachmentNames = normalizeAttachmentNames(attachmentNames)
    const currentAttachmentNames = normalizeAttachmentNames(
      files
        .filter((file) => target.fileIds.includes(file.id) && !file.isDeleted)
        .map((file) => file.originalName)
    )
    const attachmentsChanged =
      nextAttachmentNames.length !== currentAttachmentNames.length ||
      nextAttachmentNames.some((name, index) => name !== currentAttachmentNames[index])
    const contentChanged =
      target.title !== values.title ||
      target.workContent !== values.workContent ||
      target.requestContent !== values.requestContent
    const shouldReprocess = contentChanged || attachmentsChanged

    Object.assign(target, worklogValues, {
      dependencyIds: values.dependencyIds.filter((dependencyId) => dependencyId !== id),
      updatedAt: new Date().toISOString(),
      completionDate: values.status === "DONE" ? today : undefined,
      aiStatus: shouldReprocess ? "PROCESSING" : target.aiStatus,
      aiSummary: shouldReprocess
        ? "업무 내용 또는 첨부 파일 변경이 감지되어 AI 요약/태그/임베딩을 다시 계산하는 mock 상태입니다."
        : aiSummary ?? target.aiSummary,
      aiSummaryEdited: shouldReprocess
        ? false
        : aiSummaryEdited ?? target.aiSummaryEdited,
      tagIds: shouldReprocess
        ? mergeSelectedAndAiTags({ ...values, tagIds })
        : Array.from(new Set(tagIds)),
    })

    syncFiles(target, attachmentNames, target.authorId)

    if (statusChanged) {
      target.statusHistory.unshift({
        id: getNextStatusHistoryId(),
        previousStatus,
        newStatus: values.status,
        changedBy: target.authorId,
        reason: "수정 화면에서 상태가 변경되었습니다.",
        changedAt: new Date().toISOString(),
      })
      addDependencyNotifications(target)
    }

    notifyMockDb()
    return target
  },
  async transitionStatus(
    id: number,
    nextStatus: WorklogRecord["status"],
    changedBy: number,
    reason: string
  ) {
    const target = worklogs.find((worklog) => worklog.id === id && !worklog.isDeleted)
    if (!target) return { worklog: undefined, warning: undefined }

    if (!nextMap[target.status].includes(nextStatus)) {
      throw new Error("허용되지 않은 상태 전이입니다.")
    }

    const predecessorIncomplete =
      nextStatus === "IN_PROGRESS" &&
      target.dependencyIds.some((dependencyId) => {
        const dependency = worklogs.find((worklog) => worklog.id === dependencyId)
        return dependency && dependency.status !== "DONE"
      })

    const previousStatus = target.status
    target.status = nextStatus
    target.updatedAt = new Date().toISOString()
    target.completionDate =
      nextStatus === "DONE" ? new Date().toISOString().slice(0, 10) : undefined
    target.statusHistory.unshift({
      id: getNextStatusHistoryId(),
      previousStatus,
      newStatus: nextStatus,
      changedBy,
      reason: reason.trim() || "상태 전환",
      changedAt: new Date().toISOString(),
    })

    addDependencyNotifications(target)
    notifyMockDb()

    return {
      worklog: target,
      warning: predecessorIncomplete
        ? "선행 업무가 아직 완료되지 않았습니다. 현재 와이어프레임에서는 차단하지 않고 경고만 제공합니다."
        : undefined,
    }
  },
}
