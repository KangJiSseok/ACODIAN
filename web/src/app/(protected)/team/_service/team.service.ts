import { apiClient } from "@/app/_common/service/api-client";
import type { EmptyResponse, PageResponse } from "@/app/_common/types/api.types";
import type {
  CreateTeamRequest,
  GetTeamsParams,
  TeamDetail,
  TeamStatusSummary,
  TeamSummary,
  TeamUserCandidate,
  TeamUsersResponse,
  UpdateTeamRequest,
} from "../_types/team.types";

export const teamService = {
  // visible scope 기준 팀 목록을 조회합니다.
  getTeams: ({ page = 1, pageSize = 100 }: GetTeamsParams = {}) =>
    apiClient.get<PageResponse<TeamSummary>>("/teams", {
      params: { page, pageSize },
    }),

  // visible scope 기준 팀 상태 집계를 조회합니다.
  getTeamSummary: () => apiClient.get<TeamStatusSummary>("/teams/summary"),

  // 팀 상세 정보를 조회합니다.
  getTeam: (teamId: number) => apiClient.get<TeamDetail>(`/teams/${teamId}`),

  // 팀의 ACTIVE 구성원을 조회합니다.
  getTeamUsers: (teamId: number) =>
    apiClient.get<TeamUsersResponse>(`/teams/${teamId}/users`),

  // 팀 생성/수정 폼에서 선택할 활성 사용자를 조회합니다.
  getUserCandidates: () =>
    apiClient.get<TeamUserCandidate[]>("/users", {
      params: { employmentStatus: "ACTIVE" },
    }),

  // 새 팀을 등록합니다.
  createTeam: (payload: CreateTeamRequest) =>
    apiClient.post<EmptyResponse, CreateTeamRequest>("/teams", payload),

  // 팀 정보를 부분 수정합니다.
  updateTeam: (teamId: number, payload: UpdateTeamRequest) =>
    apiClient.patch<EmptyResponse, UpdateTeamRequest>(
      `/teams/${teamId}`,
      payload,
    ),

  // 팀을 삭제합니다.
  deleteTeam: (teamId: number) =>
    apiClient.delete<EmptyResponse>(`/teams/${teamId}`),
};
