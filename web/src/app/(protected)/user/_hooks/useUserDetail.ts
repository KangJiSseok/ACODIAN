import { useQuery } from "@tanstack/react-query";
import { userService } from "../_service/user.service";
import { userKeys } from "./useUserList";

function isValidUserId(userId: number) {
  return Number.isFinite(userId) && userId > 0;
}

export function useUserDetail(userId: number) {
  return useQuery({
    queryKey: userKeys.detail(userId),
    queryFn: () => userService.getUser(userId),
    enabled: isValidUserId(userId),
  });
}

export function useUserSkills(userId: number, enabled = true) {
  return useQuery({
    queryKey: userKeys.skills(userId),
    queryFn: () => userService.getUserSkills(userId),
    enabled: enabled && isValidUserId(userId),
  });
}

export function useUserEvaluations(userId: number, enabled = true) {
  return useQuery({
    queryKey: userKeys.evaluations(userId),
    queryFn: () => userService.getUserEvaluations(userId),
    enabled: enabled && isValidUserId(userId),
  });
}
