import { apiClient } from "@/app/_common/service/api-client";
import type { DirectorDashboard } from "../_types/dashboard.types";

export const dashboardService = {
  getDirectorDashboard: () =>
    apiClient.get<DirectorDashboard>("/dashboard", {
      params: { scope: "DEPARTMENT_COMPARISON" },
    }),
};
