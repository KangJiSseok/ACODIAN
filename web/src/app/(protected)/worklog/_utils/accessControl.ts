import type { AuthUser } from "../_store/authStore"
import type { WorklogRecord } from "../_types/worklog.types"

interface DepartmentLike {
  id: number
}

interface TeamLike {
  id: number
  departmentId: number
}

interface UserLike {
  id: number
  departmentId: number
  teamIds: number[]
}

export function isDirector(user: AuthUser | null | undefined) {
  return user?.role === "DIRECTOR"
}

export function isDepartmentHead(user: AuthUser | null | undefined) {
  return user?.role === "DEPT_HEAD"
}

export function isTeamLead(user: AuthUser | null | undefined) {
  return user?.role === "TEAM_LEAD"
}

export function canCreateWorklog(user: AuthUser | null | undefined) {
  return Boolean(user && !isDirector(user))
}

export function getVisibleDepartments(
  user: AuthUser | null | undefined,
  items: DepartmentLike[]
) {
  if (!user) return []
  if (isDirector(user)) return items
  return items.filter((department) => department.id === user.departmentId)
}

export function getVisibleTeams(user: AuthUser | null | undefined, items: TeamLike[]) {
  if (!user) return []
  if (isDirector(user)) return items
  if (isDepartmentHead(user)) {
    return items.filter((team) => team.departmentId === user.departmentId)
  }
  return items.filter((team) => user.teamIds.includes(team.id))
}

export function getVisibleUsers(user: AuthUser | null | undefined, items: UserLike[]) {
  if (!user) return []
  if (isDirector(user)) return items
  if (isDepartmentHead(user)) {
    return items.filter((member) => member.departmentId === user.departmentId)
  }
  if (isTeamLead(user)) {
    return items.filter((member) =>
      member.teamIds.some((teamId) => user.teamIds.includes(teamId))
    )
  }
  return items.filter((member) => member.id === user.id)
}

export function getVisibleWorklogs(
  user: AuthUser | null | undefined,
  items: WorklogRecord[],
  teams: TeamLike[]
) {
  if (!user) return []
  const activeItems = items.filter((worklog) => !worklog.isDeleted)

  if (isDirector(user)) return activeItems

  if (isDepartmentHead(user)) {
    return activeItems.filter((worklog) => {
      const team = teams.find((item) => item.id === worklog.teamId)
      return team?.departmentId === user.departmentId
    })
  }

  if (isTeamLead(user)) {
    return activeItems.filter((worklog) => user.teamIds.includes(worklog.teamId))
  }

  return activeItems.filter((worklog) => worklog.authorId === user.id)
}

export function canEditWorklog(
  user: AuthUser | null | undefined,
  worklog: WorklogRecord
) {
  if (!user) return false
  return user.id === worklog.authorId || isDirector(user) || isDepartmentHead(user)
}

export function canTransitionWorklog(
  user: AuthUser | null | undefined,
  worklog: WorklogRecord
) {
  if (!user) return false
  if (user.id === worklog.authorId) return true
  if (isDirector(user) || isDepartmentHead(user)) return true
  if (isTeamLead(user) && user.teamIds.includes(worklog.teamId)) return true
  return false
}
