// web/src/app/(protected)/department/_hooks/useDepartmentMutation.ts

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { departmentService } from "../_service/department.service";
import type {
  CreateDepartmentRequest,
  UpdateDepartmentRequest,
} from "../_types/department.types";
import { departmentKeys } from "./useDepartmentList";

export function useCreateDepartment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateDepartmentRequest) =>
      departmentService.createDepartment(payload),
    onSuccess: () => {
      // 등록 성공 후 목록을 다시 조회합니다.
      queryClient.invalidateQueries({ queryKey: departmentKeys.all });
    },
  });
}

export function useUpdateDepartment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      departmentId,
      payload,
    }: {
      departmentId: number;
      payload: UpdateDepartmentRequest;
    }) => departmentService.updateDepartment(departmentId, payload),
    onSuccess: () => {
      // 수정 성공 후 목록을 다시 조회합니다.
      queryClient.invalidateQueries({ queryKey: departmentKeys.all });
    },
  });
}

export function useDeleteDepartment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: departmentService.deleteDepartment,
    onSuccess: () => {
      // 삭제 성공 후 목록을 다시 조회합니다.
      queryClient.invalidateQueries({ queryKey: departmentKeys.all });
    },
  });
}
