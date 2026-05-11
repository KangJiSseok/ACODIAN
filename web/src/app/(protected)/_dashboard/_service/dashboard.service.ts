import { apiClient } from "@/app/_common/service/api-client";
import type {
  DepartmentDashboard,
  DirectorDashboard,
  MyDashboard,
  TeamDashboard,
} from "../_types/dashboard.types";

export const dashboardService = {
  // 응답 data에 scope discriminator가 없어서 호출 함수 단위로 응답 타입을 좁힙니다.
  getDirectorDashboard: () =>
    apiClient.get<DirectorDashboard>("/dashboard", {
      params: { scope: "DEPARTMENT_COMPARISON" },
    }),

  getDepartmentDashboard: (departmentId: number) =>
    apiClient.get<DepartmentDashboard>("/dashboard", {
      params: {
        scope: "DEPARTMENT_DETAIL",
        departmentId,
      },
    }),

  getMyDashboard: (teamId: number) =>
    apiClient.get<MyDashboard>("/dashboard", {
      params: {
        scope: "ME",
        teamId,
      },
    }),

  getTeamDashboard: (teamId: number) =>
    apiClient.get<TeamDashboard>("/dashboard", {
      params: {
        scope: "TEAM_DETAIL",
        teamId,
      },
    }),
};
