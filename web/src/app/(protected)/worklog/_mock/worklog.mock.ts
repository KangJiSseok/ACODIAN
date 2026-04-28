import type {
  AiProcessingStatus,
  EmploymentStatus,
  UserRole,
  WorklogRecord,
} from "../_types/worklog.types"

type Listener = () => void

const listeners = new Set<Listener>()

export function subscribeMockDb(listener: Listener) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

export function notifyMockDb() {
  persistMockDb()
  listeners.forEach((listener) => listener())
}

export interface DepartmentRecord {
  id: number
  name: string
  description: string
  leaderId: number
  activeProjects: number
  createdAt: string
  updatedAt: string
}

export interface TeamRecord {
  id: number
  name: string
  departmentId: number
  leaderId: number
  status: "ACTIVE" | "INACTIVE"
  description: string
  members: number[]
  startDate: string
  endDate: string
  createdAt: string
  updatedAt: string
  operationNote: string
}

export interface UserRecord {
  id: number
  name: string
  email: string
  password?: string
  role: UserRole
  departmentId: number
  primaryTeamId: number
  teamIds: number[]
  position: string
  title: string
  phone: string
  employmentStatus: EmploymentStatus
  joinDate: string
  profileImage: string
  skills: { name: string; level: number; selfRated: boolean }[]
  createdAt: string
  updatedAt: string
}

export interface FileRecord {
  id: number
  worklogId: number
  originalName: string
  storedPath: string
  type: string
  size: string
  summaryPreview: string
  aiStatus: AiProcessingStatus
  uploadedAt: string
  uploadedBy: number
  isDeleted: boolean
}

export interface TagRecord {
  id: number
  name: string
  usageCount: number
  category: "AI" | "업무" | "기술" | "부서"
  source: "AI" | "MANUAL"
  mergeState: "ACTIVE" | "REVIEW" | "MERGE_CANDIDATE"
  reuseHint: string
  mergeTargetId?: number
}

export interface NotificationRecord {
  id: number
  userId: number
  type: "URGENT" | "DEADLINE" | "OVERDUE" | "DEPENDENCY" | "WORKLOAD"
  title: string
  content: string
  referenceId?: number
  isRead: boolean
  readAt?: string
  createdAt: string
  sourceScope: "PERSONAL" | "TEAM" | "DEPARTMENT"
  deepLink: string
}

const now = "2026-04-21T14:53:00"

export const departments: DepartmentRecord[] = [
  {
    id: 1,
    name: "데이터컨설팅사업부",
    description: "분석과 업무 데이터 표준화를 담당합니다.",
    leaderId: 2,
    activeProjects: 1,
    createdAt: "2026-01-02T09:00:00",
    updatedAt: now,
  },
  {
    id: 3,
    name: "솔루션개발사업부",
    description: "AX-WMS 제품 개발과 AI 연동을 담당합니다.",
    leaderId: 4,
    activeProjects: 2,
    createdAt: "2026-01-02T09:00:00",
    updatedAt: now,
  },
]

export const teams: TeamRecord[] = [
  {
    id: 11,
    name: "MCP Project",
    departmentId: 3,
    leaderId: 6,
    status: "ACTIVE",
    description: "업무일지와 협업 UI를 구현합니다.",
    members: [6, 7, 8],
    startDate: "2026-03-01",
    endDate: "2026-06-30",
    createdAt: "2026-03-01T09:00:00",
    updatedAt: now,
    operationNote: "UI 검증용 주 프로젝트입니다.",
  },
  {
    id: 12,
    name: "AX Insight",
    departmentId: 1,
    leaderId: 2,
    status: "ACTIVE",
    description: "업무 분석 대시보드를 고도화합니다.",
    members: [2],
    startDate: "2026-02-15",
    endDate: "2026-05-20",
    createdAt: "2026-02-15T09:00:00",
    updatedAt: now,
    operationNote: "부서장 필터 확인용 팀입니다.",
  },
]

export const users: UserRecord[] = [
  {
    id: 2,
    name: "한서윤",
    email: "head-data@ax-wms.com",
    role: "DEPT_HEAD",
    departmentId: 1,
    primaryTeamId: 12,
    teamIds: [12],
    position: "부장",
    title: "데이터컨설팅사업부장",
    phone: "010-2222-2222",
    employmentStatus: "ACTIVE",
    joinDate: "2022-01-10",
    profileImage: "https://i.pravatar.cc/150?u=head-data",
    skills: [{ name: "데이터 전략", level: 5, selfRated: false }],
    createdAt: "2026-01-02T09:00:00",
    updatedAt: now,
  },
  {
    id: 4,
    name: "윤지후",
    email: "head-dev@ax-wms.com",
    role: "DEPT_HEAD",
    departmentId: 3,
    primaryTeamId: 11,
    teamIds: [11],
    position: "부장",
    title: "솔루션개발사업부장",
    phone: "010-4444-4444",
    employmentStatus: "ACTIVE",
    joinDate: "2021-11-08",
    profileImage: "https://i.pravatar.cc/150?u=head-dev",
    skills: [{ name: "AI 플랫폼", level: 5, selfRated: false }],
    createdAt: "2026-01-02T09:00:00",
    updatedAt: now,
  },
  {
    id: 6,
    name: "박정민",
    email: "leader-dev@ax-wms.com",
    role: "TEAM_LEAD",
    departmentId: 3,
    primaryTeamId: 11,
    teamIds: [11],
    position: "차장",
    title: "팀리더",
    phone: "010-6666-6666",
    employmentStatus: "ACTIVE",
    joinDate: "2022-07-11",
    profileImage: "https://i.pravatar.cc/150?u=leader-dev",
    skills: [{ name: "프로젝트 리딩", level: 4, selfRated: false }],
    createdAt: "2026-01-02T09:00:00",
    updatedAt: now,
  },
  {
    id: 7,
    name: "최수빈",
    email: "member1@ax-wms.com",
    role: "MEMBER",
    departmentId: 3,
    primaryTeamId: 11,
    teamIds: [11],
    position: "대리",
    title: "프론트엔드 개발자",
    phone: "010-7777-7777",
    employmentStatus: "ACTIVE",
    joinDate: "2024-02-19",
    profileImage: "https://i.pravatar.cc/150?u=member1",
    skills: [{ name: "UI 구현", level: 4, selfRated: true }],
    createdAt: "2026-01-02T09:00:00",
    updatedAt: now,
  },
  {
    id: 8,
    name: "정다희",
    email: "member2@ax-wms.com",
    role: "MEMBER",
    departmentId: 3,
    primaryTeamId: 11,
    teamIds: [11],
    position: "대리",
    title: "백엔드 개발자",
    phone: "010-8888-8888",
    employmentStatus: "ACTIVE",
    joinDate: "2024-03-11",
    profileImage: "https://i.pravatar.cc/150?u=member2",
    skills: [{ name: "API 설계", level: 4, selfRated: true }],
    createdAt: "2026-01-02T09:00:00",
    updatedAt: now,
  },
]

export const tags: TagRecord[] = [
  {
    id: 1,
    name: "MCP",
    usageCount: 18,
    category: "기술",
    source: "AI",
    mergeState: "ACTIVE",
    reuseHint: "관리형 태그 풀의 기준 태그입니다.",
  },
  {
    id: 2,
    name: "Gemini",
    usageCount: 14,
    category: "AI",
    source: "AI",
    mergeState: "ACTIVE",
    reuseHint: "LLM 호출 관련 업무에 자동 재사용됩니다.",
  },
  {
    id: 3,
    name: "pgvector",
    usageCount: 11,
    category: "기술",
    source: "AI",
    mergeState: "ACTIVE",
    reuseHint: "임베딩 저장소 관련 태그입니다.",
  },
  {
    id: 4,
    name: "업무자동화",
    usageCount: 23,
    category: "업무",
    source: "MANUAL",
    mergeState: "ACTIVE",
    reuseHint: "업무일지 자동화 시나리오에 재사용됩니다.",
  },
]

export const files: FileRecord[] = [
  {
    id: 2001,
    worklogId: 1001,
    originalName: "design-system-guide.pdf",
    storedPath: "SD/MCP/2026/04/1001_design-system-guide.pdf",
    type: "PDF",
    size: "2.4MB",
    summaryPreview: "디자인 토큰 운영 원칙이 정리된 문서입니다.",
    aiStatus: "DONE",
    uploadedAt: "2026-04-08T16:10:00",
    uploadedBy: 7,
    isDeleted: false,
  },
  {
    id: 2002,
    worklogId: 1003,
    originalName: "requirements-draft.hwp",
    storedPath: "DC/AXINSIGHT/2026/04/1003_requirements-draft.hwp",
    type: "HWP",
    size: "3.8MB",
    summaryPreview: "요구사항 정의서 초안입니다.",
    aiStatus: "DONE",
    uploadedAt: "2026-04-05T17:40:00",
    uploadedBy: 2,
    isDeleted: false,
  },
]

export const worklogs: WorklogRecord[] = [
  {
    id: 1001,
    title: "AX-WMS 디자인 시스템 정리",
    requestContent: "업무일지 화면의 토큰과 공통 UI 기준을 맞춰주세요.",
    workContent: "라이트 본문 영역, 배지, 상세 카드의 시각 기준을 정리했습니다.",
    status: "IN_PROGRESS",
    importance: "HIGH",
    actualHours: 7.5,
    instructionDate: "2026-04-13",
    dueDate: "2026-04-16",
    teamId: 11,
    authorId: 7,
    dependencyIds: [1003],
    aiSummary: "디자인 토큰과 업무일지 UI 기준을 맞추는 작업이 진행 중입니다.",
    aiSummaryEdited: false,
    aiStatus: "DONE",
    tagIds: [1, 4],
    fileIds: [2001],
    isDeleted: false,
    createdAt: "2026-04-13T09:00:00",
    updatedAt: now,
    statusHistory: [
      {
        id: 5001,
        newStatus: "PENDING",
        changedBy: 7,
        reason: "업무일지 생성",
        changedAt: "2026-04-13T09:00:00",
      },
      {
        id: 5002,
        previousStatus: "PENDING",
        newStatus: "IN_PROGRESS",
        changedBy: 6,
        reason: "UI 정리 작업을 시작했습니다.",
        changedAt: "2026-04-13T11:00:00",
      },
    ],
  },
  {
    id: 1002,
    title: "시맨틱 검색 API 설계 리뷰",
    requestContent: "pgvector와 role-based filter가 함께 반영되도록 검토해주세요.",
    workContent: "검색 요청 구조와 결과 카드 구성을 문서화했습니다.",
    status: "DONE",
    importance: "NORMAL",
    actualHours: 5,
    instructionDate: "2026-04-10",
    dueDate: "2026-04-13",
    completionDate: "2026-04-13",
    teamId: 11,
    authorId: 8,
    dependencyIds: [],
    aiSummary: "검색 API와 역할 기반 필터 구조가 정리되었습니다.",
    aiSummaryEdited: true,
    aiStatus: "DONE",
    tagIds: [2, 3],
    fileIds: [],
    isDeleted: false,
    createdAt: "2026-04-10T10:00:00",
    updatedAt: "2026-04-13T16:10:00",
    statusHistory: [
      {
        id: 5003,
        newStatus: "PENDING",
        changedBy: 8,
        reason: "업무일지 생성",
        changedAt: "2026-04-10T10:00:00",
      },
    ],
  },
  {
    id: 1003,
    title: "요구사항 상세 문서 초안",
    requestContent: "프로젝트 요구사항을 업무 단위로 구조화해주세요.",
    workContent: "업무일지, 검색, 알림 요구사항을 상세 항목으로 정리했습니다.",
    status: "DONE",
    importance: "URGENT",
    actualHours: 9,
    instructionDate: "2026-04-01",
    dueDate: "2026-04-05",
    completionDate: "2026-04-05",
    teamId: 12,
    authorId: 2,
    dependencyIds: [],
    aiSummary: "전체 기획서를 업무 단위로 정리한 요구사항 문서가 완성되었습니다.",
    aiSummaryEdited: false,
    aiStatus: "DONE",
    tagIds: [4],
    fileIds: [2002],
    isDeleted: false,
    createdAt: "2026-04-01T11:00:00",
    updatedAt: "2026-04-05T18:00:00",
    statusHistory: [
      {
        id: 5004,
        newStatus: "PENDING",
        changedBy: 2,
        reason: "업무일지 생성",
        changedAt: "2026-04-01T11:00:00",
      },
    ],
  },
  {
    id: 1004,
    title: "임베딩 재처리 배치 복구",
    requestContent: "실패한 임베딩 재처리 배치의 복구 방안을 정리해주세요.",
    workContent: "실패 원인을 확인했고 재시도 정책 보완이 필요합니다.",
    status: "FAILED",
    importance: "HIGH",
    actualHours: 3.5,
    instructionDate: "2026-04-11",
    dueDate: "2026-04-13",
    teamId: 11,
    authorId: 7,
    dependencyIds: [],
    aiSummary: "임베딩 재처리 배치가 실패해 복구 방안 정리가 필요한 상태입니다.",
    aiSummaryEdited: false,
    aiStatus: "FAILED",
    tagIds: [2, 3],
    fileIds: [],
    isDeleted: false,
    createdAt: "2026-04-11T14:10:00",
    updatedAt: "2026-04-13T11:25:00",
    statusHistory: [
      {
        id: 5005,
        newStatus: "PENDING",
        changedBy: 7,
        reason: "업무일지 생성",
        changedAt: "2026-04-11T14:10:00",
      },
      {
        id: 5006,
        previousStatus: "PENDING",
        newStatus: "FAILED",
        changedBy: 6,
        reason: "배치 실패 원인이 확인되어 실패 상태로 전환했습니다.",
        changedAt: "2026-04-13T11:25:00",
      },
    ],
  },
]

export const notifications: NotificationRecord[] = [
  {
    id: 3001,
    userId: 7,
    type: "URGENT",
    title: "긴급 업무가 배정되었습니다.",
    content: "AX-WMS 디자인 시스템 정리 업무의 중요도가 긴급으로 변경되었습니다.",
    referenceId: 1001,
    isRead: false,
    createdAt: "2026-04-21T13:40:00",
    sourceScope: "PERSONAL",
    deepLink: "/worklog/detail/1001",
  },
  {
    id: 3002,
    userId: 7,
    type: "DEADLINE",
    title: "마감 예정 업무가 있습니다.",
    content: "MCP 연동 PoC 업무의 마감일이 가까워지고 있습니다.",
    referenceId: 1002,
    isRead: false,
    createdAt: "2026-04-21T11:20:00",
    sourceScope: "TEAM",
    deepLink: "/worklog/detail/1002",
  },
  {
    id: 3003,
    userId: 7,
    type: "DEPENDENCY",
    title: "선행 업무 상태가 변경되었습니다.",
    content: "업무자동화 태그 정리의 선행 업무가 진행 중 상태로 전환되었습니다.",
    referenceId: 1003,
    isRead: true,
    readAt: "2026-04-21T09:34:00",
    createdAt: "2026-04-21T09:10:00",
    sourceScope: "TEAM",
    deepLink: "/worklog/detail/1003",
  },
  {
    id: 3004,
    userId: 7,
    type: "WORKLOAD",
    title: "이번 주 업무량이 증가했습니다.",
    content: "팀 평균 대비 배정 업무 시간이 높습니다. 우선순위를 확인해주세요.",
    isRead: true,
    readAt: "2026-04-20T16:10:00",
    createdAt: "2026-04-20T15:45:00",
    sourceScope: "PERSONAL",
    deepLink: "/worklog",
  },
  {
    id: 3005,
    userId: 2,
    type: "OVERDUE",
    title: "부서 업무가 지연되었습니다.",
    content: "데이터 컨설팅 업무 중 지연 항목이 있어 부서장 확인이 필요합니다.",
    referenceId: 1004,
    isRead: false,
    createdAt: "2026-04-21T10:15:00",
    sourceScope: "DEPARTMENT",
    deepLink: "/worklog/detail/1004",
  },
]

let nextWorklogId = 1005
let nextFileId = 2003
let nextStatusHistoryId = 5007
let nextNotificationId = 3006

const MOCK_STORAGE_KEY = "ax-wms-worklog-mock-db"

interface MockDbSnapshot {
  worklogs?: WorklogRecord[]
  files?: FileRecord[]
  notifications?: NotificationRecord[]
  nextWorklogId?: number
  nextFileId?: number
  nextStatusHistoryId?: number
  nextNotificationId?: number
}

function replaceRecords<T>(target: T[], source: T[]) {
  target.splice(0, target.length, ...source)
}

function getSessionStorage() {
  if (typeof window === "undefined") return undefined
  return window.sessionStorage
}

function persistMockDb() {
  const storage = getSessionStorage()
  if (!storage) return

  const snapshot: MockDbSnapshot = {
    worklogs,
    files,
    notifications,
    nextWorklogId,
    nextFileId,
    nextStatusHistoryId,
    nextNotificationId,
  }

  storage.setItem(MOCK_STORAGE_KEY, JSON.stringify(snapshot))
}

function hydrateMockDb() {
  const storage = getSessionStorage()
  if (!storage) return

  const rawSnapshot = storage.getItem(MOCK_STORAGE_KEY)
  if (!rawSnapshot) return

  try {
    const snapshot = JSON.parse(rawSnapshot) as MockDbSnapshot

    if (Array.isArray(snapshot.worklogs)) {
      replaceRecords(worklogs, snapshot.worklogs)
    }
    if (Array.isArray(snapshot.files)) {
      replaceRecords(files, snapshot.files)
    }
    if (Array.isArray(snapshot.notifications)) {
      replaceRecords(notifications, snapshot.notifications)
    }
    if (typeof snapshot.nextWorklogId === "number") {
      nextWorklogId = snapshot.nextWorklogId
    }
    if (typeof snapshot.nextFileId === "number") {
      nextFileId = snapshot.nextFileId
    }
    if (typeof snapshot.nextStatusHistoryId === "number") {
      nextStatusHistoryId = snapshot.nextStatusHistoryId
    }
    if (typeof snapshot.nextNotificationId === "number") {
      nextNotificationId = snapshot.nextNotificationId
    }
  } catch {
    storage.removeItem(MOCK_STORAGE_KEY)
  }
}

hydrateMockDb()

export function getNextWorklogId() {
  return nextWorklogId++
}

export function getNextFileId() {
  return nextFileId++
}

export function getNextStatusHistoryId() {
  return nextStatusHistoryId++
}

export function getNextNotificationId() {
  return nextNotificationId++
}
