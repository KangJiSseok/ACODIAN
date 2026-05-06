import type { TeamStatusCode } from "../_types/team.types";

export function getTeamStatusLabel(status: TeamStatusCode) {
  if (status === "ACTIVE") {
    return "활성";
  }

  if (status === "INACTIVE") {
    return "비활성";
  }

  return status;
}
