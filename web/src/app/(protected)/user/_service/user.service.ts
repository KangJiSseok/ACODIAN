import { apiClient } from "@/app/_common/service/api-client";
import type { EmptyResponse, PageResponse } from "@/app/_common/types/api.types";
import type {
  CreateUserEvaluationPayload,
  CreateUserSkillPayload,
  GetUsersParams,
  UpdateUserPayload,
  UserDetail,
  UserEvaluationSummary,
  UserSkillsResponse,
  UserSummary,
} from "../_types/user.types";

export const userService = {
  // 사용자 목록은 백엔드 PageResponse 기준으로 검색/필터/페이지네이션을 적용합니다.
  getUsers: (params: GetUsersParams = {}) =>
    apiClient.get<PageResponse<UserSummary>>("/users", { params }),

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

  // 사용자 상세 화면에서 부서, 직급, 직책, 상태만 바로 부분 수정합니다.
  updateUser: (userId: number, payload: UpdateUserPayload) => {
    const request = {
      userName: payload.userName,
      email: payload.email,
      positionName: payload.positionName,
      titleName: payload.titleName,
      departmentId: payload.departmentId,
      phone: payload.phone,
      employmentStatus: payload.employmentStatus,
      joinDate: payload.joinDate,
      primaryTeamId: payload.primaryTeamId,
    };
    const formData = new FormData();

    formData.append(
      "request",
      new Blob([JSON.stringify(request)], { type: "application/json" }),
    );

    if (payload.profileImage) {
      formData.append("profile_image", payload.profileImage);
    }

    return apiClient.patch<EmptyResponse, FormData>(`/users/${userId}`, formData);
  },

  // 사용자 상세의 스킬 탭에서 새 스킬을 등록합니다.
  createUserSkill: (userId: number, payload: CreateUserSkillPayload) =>
    apiClient.post<EmptyResponse, CreateUserSkillPayload>(`/users/${userId}/skills`, payload),

  // 사용자 상세의 관리자 평가 탭에서 새 평가 메모를 등록합니다.
  createUserEvaluation: (
    userId: number,
    payload: CreateUserEvaluationPayload,
  ) =>
    apiClient.post<EmptyResponse, CreateUserEvaluationPayload>(`/users/${userId}/evaluations`, payload),
};
