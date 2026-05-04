import { apiClient } from "@/app/_common/service/api-client";
import type { EmptyResponse } from "@/app/_common/types/api.types";
import type {
  CreateDepartmentRequest,
  GetDepartmentsResponse,
  UpdateDepartmentRequest,
} from "../_types/department.types";
import { Update } from "next/dist/build/swc/types";

export const departmentService = {
  // 부서 목록 + 상단 집계 조회
  getDepartments: () => apiClient.get<GetDepartmentsResponse>("/departments"),

  // 부서 등록
  createDepartment: (payload: CreateDepartmentRequest) =>
    apiClient.post<EmptyResponse, CreateDepartmentRequest>(
      "/departments",
      payload,
    ),

  // 부서 수정
  updateDepartment: (departmentId: number, payload: UpdateDepartmentRequest) =>
    apiClient.put<EmptyResponse>("/departments/${departmentId}", payload),

  // 부서 비활성화
  deleteDepartment: (departmentId: number) =>
    apiClient.delete<EmptyResponse>("/departments/${departmentId}"),
};
