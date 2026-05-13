import { apiClient } from "@/app/_common/service/api-client"
import type { EmptyResponse, PageResponse } from "@/app/_common/types/api.types"

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
    const request = {
      title: values.title,
      requestContent: values.requestContent,
      workContent: values.workContent,
      statusCode: worklogStatusApiCodeMap[values.status] ?? "PENDING",
      reason: values.statusChangeReason?.trim() || null,
      importanceCode: values.importance,
      actualHours: values.actualHours,
      instructionDate: values.instructionDate,
      dueDate: values.dueDate,
      predecessorWorklogIds: values.dependencyIds.filter(
        (dependencyId) => dependencyId !== id
      ),
      tagIds: values.tagIds,
      removeTagIds: values.removeTagIds ?? [],
      aiSummary: values.aiSummary ?? "",
      removeFileIds: values.removeFileIds ?? [],
    }
    const formData = new FormData()

    formData.append(
      "request",
      new Blob([JSON.stringify(request)], { type: "application/json" })
    )
    values.attachmentFiles.forEach((file) => {
      formData.append("files", file, file.name)
    })

    return apiClient.patch<void, FormData>(`/worklogs/${id}`, formData)
  },
  async transitionStatus(id: number, nextStatus: WorklogStatus, reason: string) {
    return apiClient.patch<
      EmptyResponse,
      { statusCode: string; reason: string | null }
    >(`/worklogs/${id}/status`, {
      statusCode: worklogStatusApiCodeMap[nextStatus] ?? "PENDING",
      reason: reason.trim() || null,
    })
  },
}
