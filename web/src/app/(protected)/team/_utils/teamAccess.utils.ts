import type { AuthUser } from "@/app/_common/store/auth.store";
import type { TeamDetail, TeamSummary } from "../_types/team.types";

type ManageableTeam = Pick<TeamSummary | TeamDetail, "deptHeadAdminUserId">;

const DIRECTOR_TITLE_NAME = "본부장";

export function canManageTeam(
  user: AuthUser | null | undefined,
  team: ManageableTeam,
) {
  if (!user) {
    return false;
  }

  if (
    user.titleName === DIRECTOR_TITLE_NAME ||
    user.positionName === DIRECTOR_TITLE_NAME
  ) {
    return true;
  }

  return user.userId === team.deptHeadAdminUserId;
}
