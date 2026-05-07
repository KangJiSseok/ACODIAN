import type { AuthUser } from "@/app/_common/store/auth.store";
import type { TeamDetail, TeamSummary } from "../_types/team.types";

type ManageableTeam = Pick<TeamSummary | TeamDetail, "deptHeadAdminUserId">;
const DIRECTOR_KEYWORD = "본부장";
const DEPARTMENT_HEAD_KEYWORD = "사업부장";

function hasProfileKeyword(user: AuthUser, keyword: string) {
  return [user.titleName, user.positionName]
    .filter(Boolean)
    .some((text) => text?.includes(keyword));
}

export function canManageTeam(
  user: AuthUser | null | undefined,
  team: ManageableTeam,
) {
  if (!user) {
    return false;
  }

  if (hasProfileKeyword(user, DIRECTOR_KEYWORD)) {
    return true;
  }

  return (
    hasProfileKeyword(user, DEPARTMENT_HEAD_KEYWORD) &&
    user.userId === team.deptHeadAdminUserId
  );
}
