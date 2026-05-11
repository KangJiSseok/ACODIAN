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
  changedByName?: string
  reason: string
  changedAt: string
}

export interface Worklog {
  id: number
  title: string
  requestContent: string
  workContent: string
  statusCode?: string
  status: WorklogStatus
  importance: ImportanceLevel
  actualHours: number
  instructionDate: string
  dueDate: string
  completionDate?: string
  teamId: number
  teamName?: string
  authorId: number
  authorName?: string
  dependencyIds: number[]
  dependOnWorklogs?: WorklogDependencyItem[]
  aiSummary: string
  aiSummaryEdited: boolean
  aiStatus: AiProcessingStatus
  tagIds: number[]
  tagNames?: string[]
  fileIds: number[]
  fileItems?: WorklogFileItem[]
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
}

export interface WorklogFormTeamOption {
  id: number
  name: string
}

export interface WorklogFormUserOption {
  id: number
  name: string
  title?: string
}

export interface WorklogFormDependencyOption {
  id: number
  title: string
  status: WorklogStatus
  teamId?: number
  authorId?: number
  aiSummary?: string
  workContent?: string
  requestContent?: string
  isDeleted?: boolean
  dependencyIds?: number[]
}

export interface WorklogFormTagOption {
  id: number
  name: string
  usageCount: number
  category: string
  source: "AI" | "MANUAL"
  reuseHint: string
}

export interface GetWorklogsParams {
  page?: number
  pageSize?: number
}

export interface SearchWorklogsParams extends GetWorklogsParams {
  keyword?: string
  teamId?: number
  teamStatus?: "ACTIVE" | "INACTIVE"
  statusCode?: WorklogStatus
  importanceCode?: ImportanceLevel
  authorId?: number
  tagId?: number
  period?: "LAST_7" | "LAST_30" | "LAST_90"
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

export interface WorklogSearchApiItem {
  worklogId: number
  title: string
  workContent?: string | null
  actualHours?: number | string | null
  aiSummary: string | null
  aiSummaryEdited: boolean | null
  statusCode: string
  importanceCode: string
  aiProcessingStatus: string
  predecessorCount: number | null
  teamId: number
  teamName: string
  authorId: number
  authorName: string
  profileImageUrl: string | null
  instructionDate: string
  dueDate: string
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

export interface WorklogFilterOptions {
  teams: WorklogFilterTeam[]
  tags: WorklogFilterTag[]
}

export interface WorklogFilterTeam {
  teamId: number
  teamName: string
  members: WorklogFilterMember[]
}

export interface WorklogFilterMember {
  userId: number
  userName: string
}

export interface WorklogFilterTag {
  tagId: number
  tagName: string
}

export interface WorklogDetailApiResponse {
  worklogId: number
  teamId: number
  teamName: string
  authorId: number
  authorName: string
  title: string
  requestContent: string
  workContent: string
  aiSummary?: string | null
  aiSummaryEdited?: boolean | null
  aiProcessingStatus?: string | null
  statusCode: string
  importanceCode?: string | null
  actualHours: number | string
  instructionDate: string
  dueDate: string
  completionDate?: string | null
  createdAt: string
  updatedAt: string
  files?: WorklogFileItem[] | null
  tags?: string[] | null
  dependOnWorklogs?: WorklogDependencyItem[] | null
  statusHistories?: WorklogDetailStatusHistoryItem[] | null
}

export interface WorklogFileItem {
  fileId: number
  originalName: string
  storedPath: string
  fileExtension: string
  fileSizeBytes: number
}

export interface WorklogDependencyItem {
  worklogId: number
  title: string
  statusCode: string
}

export interface WorklogDetailStatusHistoryItem {
  historyId: number
  previousStatusCode: string | null
  newStatusCode: string
  reason: string | null
  changedAt: string
  changedBy: number
  changedByName: string
}
