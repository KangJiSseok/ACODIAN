import { useQuery } from "@tanstack/react-query";
import { dashboardService } from "../_service/dashboard.service";

export const dashboardKeys = {
  all: ["dashboard"] as const,
  director: () => [...dashboardKeys.all, "department-comparison"] as const,
  departmentDetail: (departmentId: number) =>
    [...dashboardKeys.all, "department-detail", departmentId] as const,
  my: (teamId: number) => [...dashboardKeys.all, "my", teamId] as const,
};

export function useDirectorDashboard(enabled: boolean) {
  return useQuery({
    queryKey: dashboardKeys.director(),
    queryFn: dashboardService.getDirectorDashboard,
    enabled,
  });
}

export function useDepartmentDashboard(
  departmentId: number | undefined,
  enabled: boolean,
) {
  return useQuery({
    queryKey: dashboardKeys.departmentDetail(departmentId ?? 0),
    queryFn: () => dashboardService.getDepartmentDashboard(departmentId ?? 0),
    // DEPARTMENT_DETAIL은 departmentId가 있어야 백엔드에서 정상 조회됩니다.
    enabled: enabled && Number.isFinite(departmentId),
  });
}

export function useMyDashboard(teamId: number | undefined, enabled: boolean) {
  return useQuery({
    queryKey: dashboardKeys.my(teamId ?? 0),
    queryFn: () => dashboardService.getMyDashboard(teamId ?? 0),
    // ME scope는 사용자가 실제 소속된 teamId를 선택한 뒤에만 호출합니다.
    enabled: enabled && Number.isFinite(teamId),
  });
}
