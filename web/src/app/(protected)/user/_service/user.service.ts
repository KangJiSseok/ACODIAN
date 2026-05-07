import { apiClient } from "@/app/_common/service/api-client";
import type { PageResponse } from "@/app/_common/types/api.types";
import type {
  GetUsersParams,
  UserDetail,
  UserEvaluationSummary,
  UserSkillsResponse,
  UserSummary,
} from "../_types/user.types";

export const userService = {
  // 사용자 목록은 백엔드에서 배열로 반환하며, 화면에서 검색/필터/페이지네이션을 적용합니다.
  getUsers: (params: GetUsersParams = {}) =>
    apiClient.get<UserSummary[]>("/users", { params }),

  // 단일 사용자 상세와 전체 소속 팀 문맥을 조회합니다.
  getUser: (userId: number) => apiClient.get<UserDetail>(`/users/${userId}`),

  // 사용자 상세의 스킬 탭에서 보유 스킬을 조회합니다.
  getUserSkills: (userId: number) =>
    apiClient.get<UserSkillsResponse>(`/users/${userId}/skills`),

  // 사용자 상세의 관리자 평가 탭에서 최근 평가를 조회합니다.
  getUserEvaluations: (userId: number) =>
    apiClient.get<PageResponse<UserEvaluationSummary>>(
      `/users/${userId}/evaluations`,
      { params: { page: 1, pageSize: 20 } },
    ),
};
