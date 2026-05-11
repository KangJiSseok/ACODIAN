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

export interface DepartmentDetailTeamSummary {
  teamId: number;
  teamName: string;
  leaderId: number | null;
  leaderName: string | null;
  startDate: string | null;
  expectedEndDate: string | null;
  memberCount: number;
}

export interface DepartmentDetail {
  departmentId: number;
  departmentName: string;
  departmentHeadUserId: number | null;
  departmentHeadUserName: string | null;
  teams: DepartmentDetailTeamSummary[];
}

export interface DepartmentFormValues {
  departmentName: string;
  description: string | null;
  departmentHeadUserId: number | null;
}

export type CreateDepartmentRequest = DepartmentFormValues;
export type UpdateDepartmentRequest = DepartmentFormValues;
