export interface DepartmentSummary {
  departmentId: number;
  departmentName: string;
  description: string | null;
  departmentHeadUserId: number | null;
  departmentHeadUserName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface GetDepartmentsResponse {
  activeDepartmentCount: number;
  activeTeamCount: number;
  activeUserCount: number;
  departments: DepartmentSummary[];
}

export interface DepartmentFormValues {
  departmentName: string;
  description: string | null;
  departmentHeadUserId: number | null;
}

export type CreateDepartmentRequest = DepartmentFormValues;
export type UpdateDepartmentRequest = DepartmentFormValues;
