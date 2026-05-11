import { useQuery } from "@tanstack/react-query";
import { dashboardService } from "../_service/dashboard.service";

export const dashboardKeys = {
  all: ["dashboard"] as const,
  director: () => [...dashboardKeys.all, "department-comparison"] as const,
};

export function useDirectorDashboard(enabled: boolean) {
  return useQuery({
    queryKey: dashboardKeys.director(),
    queryFn: dashboardService.getDirectorDashboard,
    enabled,
  });
}
