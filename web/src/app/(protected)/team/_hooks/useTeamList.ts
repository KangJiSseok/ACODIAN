import { useQuery } from "@tanstack/react-query";
import { teamService } from "../_service/team.service";
import type { GetTeamsParams } from "../_types/team.types";

export const teamKeys = {
  all: ["teams"] as const,
  list: (params: GetTeamsParams = {}) =>
    [...teamKeys.all, "list", params] as const,
  summary: () => [...teamKeys.all, "summary"] as const,
  detail: (teamId: number) => [...teamKeys.all, "detail", teamId] as const,
  users: (teamId: number) => [...teamKeys.all, "users", teamId] as const,
  userCandidates: () => [...teamKeys.all, "user-candidates"] as const,
};

export function useTeamList(params: GetTeamsParams = {}) {
  return useQuery({
    queryKey: teamKeys.list(params),
    queryFn: () => teamService.getTeams(params),
  });
}

export function useTeamSummary() {
  return useQuery({
    queryKey: teamKeys.summary(),
    queryFn: teamService.getTeamSummary,
  });
}

export function useTeamUserCandidates() {
  return useQuery({
    queryKey: teamKeys.userCandidates(),
    queryFn: teamService.getUserCandidates,
  });
}
