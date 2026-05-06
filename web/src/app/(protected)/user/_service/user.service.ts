import { apiClient } from "@/app/_common/service/api-client";
import type { GetUsersParams, UserSummary } from "../_types/user.types";

export const userService = {
  // 사용자 목록은 백엔드에서 배열로 반환하며, 화면에서 검색/필터/페이지네이션을 적용합니다.
  getUsers: (params: GetUsersParams = {}) =>
    apiClient.get<UserSummary[]>("/users", { params }),
};
