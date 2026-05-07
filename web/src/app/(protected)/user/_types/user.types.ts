export type EmploymentStatusCode = "ACTIVE" | "LEAVE" | "RETIRED" | string;

export interface UserSummary {
  userId: number;
  userName: string;
  email: string;
  phone: string | null;
  departmentId: number | null;
  departmentName: string | null;
  profileImageUrl: string | null;
  teamId: number | null;
  teamName: string | null;
  positionName: string | null;
  titleName: string | null;
  employmentStatus: EmploymentStatusCode;
  joinDate?: string | null;
}

export interface GetUsersParams {
  userName?: string;
  departmentId?: number;
  positionName?: string;
  employmentStatus?: EmploymentStatusCode;
}
