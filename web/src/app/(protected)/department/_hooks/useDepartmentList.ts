import { useQuery } from "@tanstack/react-query";
import { departmentService } from "../_service/department.service";

export const departmentKeys = {
  all: ["departments"] as const,
  list: () => [...departmentKeys.all, "list"] as const,
  detail: (departmentId: number) => [...departmentKeys.all, "detail", departmentId] as const,
};

export function useDepartmentList() {
  return useQuery({
    queryKey: departmentKeys.list(),
    queryFn: departmentService.getDepartments,
  });
}

export function useDepartmentDetail(departmentId: number) {
  return useQuery({
    queryKey: departmentKeys.detail(departmentId),
    queryFn: () => departmentService.getDepartmentDetail(departmentId),
    enabled: Number.isFinite(departmentId),
  });
}
