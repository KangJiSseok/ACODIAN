import type {
  AiProcessingStatus,
  EmploymentStatus,
  ImportanceLevel,
  UserRole,
  WorklogStatus,
} from "../_types/worklog.types"

export function formatDate(date: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
  }).format(new Date(date))
}

export function formatHours(hours: number) {
  return `${hours.toFixed(1)}h`
}

export function formatDateTime(date: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(date))
}

export const roleLabelMap: Record<UserRole, string> = {
  DIRECTOR: "본부장",
  DEPT_HEAD: "사업부장",
  TEAM_LEAD: "팀리더",
  MEMBER: "구성원",
}

export const worklogStatusLabelMap: Record<WorklogStatus, string> = {
  PENDING: "대기",
  IN_PROGRESS: "진행 중",
  DONE: "완료",
  ON_HOLD: "보류",
  FAILED: "실패",
  CANCELLED: "취소",
}

export const importanceLabelMap: Record<ImportanceLevel, string> = {
  URGENT: "긴급",
  HIGH: "높음",
  NORMAL: "보통",
  LOW: "낮음",
}

export const aiStatusLabelMap: Record<AiProcessingStatus, string> = {
  PENDING: "대기",
  PROCESSING: "처리 중",
  DONE: "완료",
  FAILED: "실패",
}

export const employmentStatusLabelMap: Record<EmploymentStatus, string> = {
  ACTIVE: "재직",
  LEAVE: "휴직",
  INACTIVE: "퇴직",
}

export const teamStatusLabelMap: Record<"ACTIVE" | "INACTIVE", string> = {
  ACTIVE: "활성",
  INACTIVE: "비활성",
}

export function getRoleLabel(role: UserRole) {
  return roleLabelMap[role]
}

export function getWorklogStatusLabel(status: WorklogStatus) {
  return worklogStatusLabelMap[status]
}

export function getImportanceLabel(level: ImportanceLevel) {
  return importanceLabelMap[level]
}

export function getAiStatusLabel(status: AiProcessingStatus) {
  return aiStatusLabelMap[status]
}

export function getEmploymentStatusLabel(status: EmploymentStatus) {
  return employmentStatusLabelMap[status]
}

export function getTeamStatusLabel(status: "ACTIVE" | "INACTIVE") {
  return teamStatusLabelMap[status]
}
