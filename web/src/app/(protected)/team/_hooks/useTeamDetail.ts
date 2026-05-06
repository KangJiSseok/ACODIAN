import { useQuery } from "@tanstack/react-query";
import { teamService } from "../_service/team.service";
import { teamKeys } from "./useTeamList";

export function useTeamDetail(teamId: number) {
  return useQuery({
    queryKey: teamKeys.detail(teamId),
    queryFn: () => teamService.getTeam(teamId),
    enabled: Number.isFinite(teamId) && teamId > 0,
  });
}

export function useTeamUsers(teamId: number) {
  return useQuery({
    queryKey: teamKeys.users(teamId),
    queryFn: () => teamService.getTeamUsers(teamId),
    enabled: Number.isFinite(teamId) && teamId > 0,
  });
}
