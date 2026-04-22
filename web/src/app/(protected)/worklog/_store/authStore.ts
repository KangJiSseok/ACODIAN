import { users } from "../_mock/worklog.mock"

type Listener = () => void

export interface AuthUser {
  id: number
  name: string
  email: string
  role: "DIRECTOR" | "DEPT_HEAD" | "TEAM_LEAD" | "MEMBER"
  departmentId: number
  primaryTeamId: number
  teamIds: number[]
  title: string
  profileImage: string
}

const STORAGE_KEY = "ax-wms-auth-user"
const DEFAULT_USER_EMAIL = "member1@ax-wms.com"
const listeners = new Set<Listener>()

function toAuthUser(record: (typeof users)[number]): AuthUser {
  return {
    id: record.id,
    name: record.name,
    email: record.email,
    role: record.role,
    departmentId: record.departmentId,
    primaryTeamId: record.primaryTeamId,
    teamIds: record.teamIds,
    title: record.title,
    profileImage: record.profileImage,
  }
}

function getDefaultUser() {
  const fallback = users.find((user) => user.email === DEFAULT_USER_EMAIL) ?? users[0]
  return fallback ? toAuthUser(fallback) : null
}

function persistCurrentUser() {
  if (typeof window === "undefined") return

  if (!currentUser) {
    window.localStorage.removeItem(STORAGE_KEY)
    return
  }

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(currentUser))
}

function loadInitialState(): AuthUser | null {
  if (typeof window === "undefined") return getDefaultUser()

  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) return getDefaultUser()

  try {
    const parsed = JSON.parse(raw) as AuthUser & { avatar?: string }
    return {
      ...parsed,
      profileImage: parsed.profileImage ?? parsed.avatar ?? "",
    }
  } catch {
    return getDefaultUser()
  }
}

let currentUser = loadInitialState()

function emit() {
  listeners.forEach((listener) => listener())
}

export function subscribe(listener: Listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function getCurrentUser() {
  return currentUser
}

export function loginAsUser(email: string, password?: string) {
  const match = users.find((user) => user.email === email)
  if (!match) return null

  const resolvedPassword = match.password ?? "password123"
  if (password !== undefined && password !== resolvedPassword) {
    return null
  }

  currentUser = toAuthUser(match)
  persistCurrentUser()
  emit()
  return currentUser
}

export function refreshCurrentUser() {
  if (!currentUser) return getDefaultUser()

  const match = users.find((user) => user.id === currentUser?.id)
  if (!match) {
    currentUser = getDefaultUser()
    persistCurrentUser()
    emit()
    return currentUser
  }

  currentUser = toAuthUser(match)
  persistCurrentUser()
  emit()
  return currentUser
}

export function logout() {
  currentUser = getDefaultUser()
  persistCurrentUser()
  emit()
}
