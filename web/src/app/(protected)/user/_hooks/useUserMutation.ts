import { useMutation, useQueryClient } from "@tanstack/react-query";
import { userService } from "../_service/user.service";
import type {
  CreateUserEvaluationPayload,
  CreateUserSkillPayload,
  UpdateUserPayload,
} from "../_types/user.types";
import { userKeys } from "./useUserList";

export function useUpdateUser(userId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateUserPayload) =>
      userService.updateUser(userId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: userKeys.all });
    },
  });
}

export function useCreateUserSkill(userId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateUserSkillPayload) =>
      userService.createUserSkill(userId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: userKeys.skills(userId) });
    },
  });
}

export function useCreateUserEvaluation(userId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateUserEvaluationPayload) =>
      userService.createUserEvaluation(userId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: userKeys.evaluations(userId),
      });
    },
  });
}
