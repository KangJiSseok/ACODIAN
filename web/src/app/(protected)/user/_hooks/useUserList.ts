import { useQuery } from "@tanstack/react-query";
import { userService } from "../_service/user.service";
import type { GetUsersParams } from "../_types/user.types";

export const userKeys = {
  all: ["users"] as const,
  list: (params: GetUsersParams = {}) =>
    [...userKeys.all, "list", params] as const,
  detail: (userId: number) => [...userKeys.all, "detail", userId] as const,
  skills: (userId: number) => [...userKeys.all, "skills", userId] as const,
  evaluations: (userId: number) =>
    [...userKeys.all, "evaluations", userId] as const,
};

export function useUserList(params: GetUsersParams = {}) {
  return useQuery({
    queryKey: userKeys.list(params),
    queryFn: () => userService.getUsers(params),
  });
}
