import { useMutation, useQueryClient } from "@tanstack/react-query";
import { teamService } from "../_service/team.service";
import type {
  CreateTeamRequest,
  UpdateTeamRequest,
} from "../_types/team.types";
import { teamKeys } from "./useTeamList";

export function useCreateTeam() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateTeamRequest) => teamService.createTeam(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: teamKeys.all });
    },
  });
}

export function useUpdateTeam(teamId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateTeamRequest) =>
      teamService.updateTeam(teamId, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: teamKeys.all });
    },
  });
}

export function useDeleteTeam() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: teamService.deleteTeam,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: teamKeys.all });
    },
  });
}
