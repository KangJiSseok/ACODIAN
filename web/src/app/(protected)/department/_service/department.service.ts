import { apiClient } from "@/app/_common/service/api-client";
import type { EmptyResponse } from "@/app/_common/types/api.types";
import type {
  CreateDepartmentRequest,
  DepartmentDetail,
  GetDepartmentsResponse,
  UpdateDepartmentRequest,
} from "../_types/department.types";

export const departmentService = {
  // 부서 목록과 상단 집계를 함께 조회합니다.
  getDepartments: () => apiClient.get<GetDepartmentsResponse>("/departments"),

  getDepartmentDetail: (departmentId: number) =>
    apiClient.get<DepartmentDetail>(`/departments/${departmentId}/detail`),

  // 새 부서를 등록합니다.
  createDepartment: (payload: CreateDepartmentRequest) =>
    apiClient.post<EmptyResponse, CreateDepartmentRequest>(
      "/departments",
      payload,
    ),

  // 기존 부서 정보를 수정합니다.
  updateDepartment: (departmentId: number, payload: UpdateDepartmentRequest) =>
    apiClient.put<EmptyResponse, UpdateDepartmentRequest>(
      `/departments/${departmentId}`,
      payload,
    ),

  // 부서를 비활성화합니다.
  deleteDepartment: (departmentId: number) =>
    apiClient.delete<EmptyResponse>(`/departments/${departmentId}`),
};
