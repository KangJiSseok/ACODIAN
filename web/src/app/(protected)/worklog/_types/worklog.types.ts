export type UserRole = "DIRECTOR" | "DEPT_HEAD" | "TEAM_LEAD" | "MEMBER"
export type EmploymentStatus = "ACTIVE" | "LEAVE" | "INACTIVE"
export type WorklogStatus =
  | "PENDING"
  | "IN_PROGRESS"
  | "DONE"
  | "ON_HOLD"
  | "FAILED"
  | "CANCELLED"
export type ImportanceLevel = "URGENT" | "HIGH" | "NORMAL" | "LOW"
export type AiProcessingStatus = "PENDING" | "PROCESSING" | "DONE" | "FAILED"

export interface WorklogStatusHistoryRecord {
  id: number
  previousStatus?: WorklogStatus
  newStatus: WorklogStatus
  changedBy: number
  reason: string
  changedAt: string
}

export interface Worklog {
  id: number
  title: string
  requestContent: string
  workContent: string
  status: WorklogStatus
  importance: ImportanceLevel
  actualHours: number
  instructionDate: string
  dueDate: string
  completionDate?: string
  teamId: number
  authorId: number
  dependencyIds: number[]
  aiSummary: string
  aiSummaryEdited: boolean
  aiStatus: AiProcessingStatus
  tagIds: number[]
  fileIds: number[]
  isDeleted: boolean
  createdAt: string
  updatedAt: string
  statusHistory: WorklogStatusHistoryRecord[]
}

export type WorklogRecord = Worklog

export interface WorklogFormValues {
  title: string
  requestContent: string
  workContent: string
  status: Worklog["status"]
  importance: Worklog["importance"]
  actualHours: number
  instructionDate: string
  dueDate: string
  teamId: number
  authorId: number
  dependencyIds: number[]
  attachmentNames: string[]
  tagIds: number[]
  aiSummary?: string
  aiSummaryEdited?: boolean
  aiRegenerateRequested?: boolean
}

export interface GetWorklogsParams {
  page?: number
  pageSize?: number
}

export interface WorklogListApiItem {
  worklogId: number
  title: string
  statusCode: string
  workContent: string
  actualHours: number
  importanceCode: string
  aiSummary: string | null
  aiProcessingStatus: string
  aiSummaryEdited: boolean | null
  teamId: number
  teamName: string
  authorId: number
  authorName: string
  instructionDate: string
  dueDate: string
  predecessorCount: number
}

export interface WorklogListItem {
  id: number
  title: string
  status: WorklogStatus
  workContent: string
  actualHours: number
  importance: ImportanceLevel
  aiSummary: string
  aiStatus: AiProcessingStatus
  aiSummaryEdited: boolean
  teamId: number
  teamName: string
  authorId: number
  authorName: string
  instructionDate: string
  dueDate: string
  predecessorCount: number
}
