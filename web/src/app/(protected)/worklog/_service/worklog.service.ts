import {
  departments,
  files,
  getNextFileId,
  getNextNotificationId,
  getNextStatusHistoryId,
  getNextWorklogId,
  notifications,
  notifyMockDb,
  tags,
  teams,
  type FileRecord,
  worklogs,
} from "../_mock/worklog.mock"
import type {
  Worklog,
  WorklogFormValues,
  WorklogRecord,
} from "../_types/worklog.types"

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

function addUrgentNotifications(created: WorklogRecord) {
  if (created.importance !== "URGENT") return

  const team = teams.find((item) => item.id === created.teamId)
  const department = departments.find((item) => item.id === team?.departmentId)
  const targets = [team?.leaderId, department?.leaderId].filter(
    (targetId): targetId is number =>
      typeof targetId === "number" && targetId !== created.authorId
  )

  targets.forEach((targetId) => {
    notifications.unshift({
      id: getNextNotificationId(),
      userId: targetId,
      type: "URGENT",
      title: "긴급 업무가 등록되었습니다",
      content: `${created.title} 업무가 긴급 우선순위로 등록되었습니다.`,
      referenceId: created.id,
      isRead: false,
      createdAt: new Date().toISOString(),
      sourceScope: "TEAM",
      deepLink: `/worklog/detail/${created.id}`,
    })
  })
}

export const worklogService = {
  async list(): Promise<Worklog[]> {
    return worklogs.filter((worklog) => !worklog.isDeleted).map((worklog) => ({ ...worklog }))
  },
  async getById(id: number): Promise<Worklog | undefined> {
    const target = worklogs.find((worklog) => worklog.id === id && !worklog.isDeleted)
    return target ? { ...target } : undefined
  },
  async create(values: WorklogFormValues) {
    const { attachmentNames } = values
    const worklogValues = {
      title: values.title,
      requestContent: values.requestContent,
      workContent: values.workContent,
      status: values.status,
      importance: values.importance,
      actualHours: values.actualHours,
      instructionDate: values.instructionDate,
      dueDate: values.dueDate,
      teamId: values.teamId,
      authorId: values.authorId,
      dependencyIds: values.dependencyIds,
    }
    const today = new Date().toISOString().slice(0, 10)
    const created: Worklog = {
      id: getNextWorklogId(),
      completionDate: values.status === "DONE" ? today : undefined,
      aiSummary:
        "업무 저장 직후 AI 파이프라인 mock 상태가 시작되며 요약/태그/임베딩을 비동기로 처리합니다.",
      aiSummaryEdited: false,
      aiStatus: "PROCESSING",
      tagIds: mergeSelectedAndAiTags(values),
      fileIds: [],
      isDeleted: false,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      statusHistory: [
        {
          id: getNextStatusHistoryId(),
          newStatus: values.status,
          changedBy: values.authorId,
          reason: "업무일지 생성",
          changedAt: new Date().toISOString(),
        },
      ],
      ...worklogValues,
    }

    syncFiles(created, attachmentNames, values.authorId)
    worklogs.unshift(created)
    addUrgentNotifications(created)
    notifyMockDb()
    return created
  },
  async update(id: number, values: WorklogFormValues) {
    const target = worklogs.find((worklog) => worklog.id === id)
    if (!target) return undefined

    const {
      attachmentNames,
      tagIds,
      aiSummary,
      aiSummaryEdited,
      aiRegenerateRequested,
      ...worklogValues
    } = values

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
    const shouldReprocess =
      contentChanged || attachmentsChanged || Boolean(aiRegenerateRequested)
    const summaryChanged =
      aiSummary !== undefined && aiSummary.trim() !== target.aiSummary.trim()

    Object.assign(target, worklogValues, {
      dependencyIds: values.dependencyIds.filter((dependencyId) => dependencyId !== id),
      updatedAt: new Date().toISOString(),
      completionDate: values.status === "DONE" ? today : undefined,
      aiStatus: shouldReprocess ? "PROCESSING" : target.aiStatus,
      aiSummary: shouldReprocess
        ? aiRegenerateRequested
          ? "사용자가 AI 요약 재생성을 요청하여 요약/태그/임베딩을 다시 계산하는 mock 상태입니다."
          : "업무 내용 또는 첨부 파일 변경이 감지되어 AI 요약/태그/임베딩을 다시 계산하는 mock 상태입니다."
        : aiSummary ?? target.aiSummary,
      aiSummaryEdited: shouldReprocess
        ? false
        : summaryChanged
          ? true
          : aiSummaryEdited ?? target.aiSummaryEdited,
      tagIds: shouldReprocess
        ? mergeSelectedAndAiTags({ ...values, tagIds })
        : Array.from(new Set(tagIds)),
    })

    syncFiles(target, attachmentNames, values.authorId)

    if (statusChanged) {
      target.statusHistory.unshift({
        id: getNextStatusHistoryId(),
        previousStatus,
        newStatus: values.status,
        changedBy: values.authorId,
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
