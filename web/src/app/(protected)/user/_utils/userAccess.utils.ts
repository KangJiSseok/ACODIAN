import type { AuthUser } from "@/app/_common/store/auth.store";
import type { UserDetail } from "../_types/user.types";

type InsightAccessUser = Pick<
  AuthUser | UserDetail,
  "userId" | "departmentId" | "positionName" | "titleName"
>;

const ROLE_RANK = {
  member: 1,
  teamLead: 2,
  departmentHead: 3,
  director: 4,
} as const;

const MINIMUM_INSIGHT_WRITER_RANK = ROLE_RANK.departmentHead;

export function getUserRoleRank(user: InsightAccessUser | null | undefined) {
  const roleText = `${user?.titleName ?? ""} ${user?.positionName ?? ""}`;

  if (roleText.includes("본부장")) {
    return ROLE_RANK.director;
  }
  if (roleText.includes("사업부장")) {
    return ROLE_RANK.departmentHead;
  }
  if (roleText.includes("팀장")) {
    return ROLE_RANK.teamLead;
  }

  return ROLE_RANK.member;
}

export function canWriteUserInsight(
  actor: InsightAccessUser | null | undefined,
  target: InsightAccessUser | null | undefined,
) {
  if (!actor || !target || actor.userId === target.userId) {
    return false;
  }

  const actorRank = getUserRoleRank(actor);
  const targetRank = getUserRoleRank(target);

  if (actorRank < MINIMUM_INSIGHT_WRITER_RANK || actorRank <= targetRank) {
    return false;
  }
  if (actorRank === ROLE_RANK.director) {
    return true;
  }

  return (
    actor.departmentId != null &&
    target.departmentId != null &&
    actor.departmentId === target.departmentId
  );
}

export function canUseUserDepartmentFilter(
  actor: InsightAccessUser | null | undefined,
) {
  return getUserRoleRank(actor) === ROLE_RANK.director;
}

export function resolveUserListDepartmentId(
  actor: InsightAccessUser | null | undefined,
  selectedDepartmentId: string,
  allFilterValue = "all",
) {
  if (!actor) {
    return undefined;
  }
  if (!canUseUserDepartmentFilter(actor)) {
    return actor.departmentId;
  }
  if (selectedDepartmentId === allFilterValue) {
    return actor.departmentId;
  }

  return Number(selectedDepartmentId);
}
