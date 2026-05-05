import { useQuery } from "@tanstack/react-query";
import { departmentService } from "../_service/department.service";

export const departmentKeys = {
  all: ["departments"] as const,
  list: () => [...departmentKeys.all, "list"] as const,
};

export function useDepartmentList() {
  return useQuery({
    queryKey: departmentKeys.list(),
    queryFn: departmentService.getDepartments,
  });
}
