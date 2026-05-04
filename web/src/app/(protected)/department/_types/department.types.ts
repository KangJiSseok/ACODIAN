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

// 똑같은 객체지만, 사용 맥락에 따라 부르는 이름을 다르게 두기 위함
export type CreateDepartmentRequest = DepartmentFormValues;
export type UpdateDepartmentRequest = DepartmentFormValues;
