export type EmploymentStatusCode =
  | "ACTIVE"
  | "LEAVE"
  | "RETIRED"
  | (string & {});

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
  page?: number;
  pageSize?: number;
  userName?: string;
  departmentId?: number | null;
  positionName?: string;
  employmentStatus?: EmploymentStatusCode;
}

export interface UserTeamSummary {
  isPrimary: boolean;
  teamId: number;
  teamName: string;
  isLeader: boolean;
  teamRole: string | null;
}

export interface UserDetail {
  userId: number;
  userName: string;
  email: string;
  phone: string | null;
  departmentId: number | null;
  departmentName: string | null;
  positionName: string | null;
  titleName: string | null;
  joinDate: string | null;
  profileImageUrl: string | null;
  employmentStatus: EmploymentStatusCode;
  teams: UserTeamSummary[];
}

export interface UpdateUserPayload {
  userName: string | null;
  email: string | null;
  positionName: string | null;
  titleName: string | null;
  departmentId: number | null;
  phone: string | null;
  employmentStatus: EmploymentStatusCode;
  joinDate: string | null;
  primaryTeamId: number | null;
  profileImage: File | null;
}

export interface CreateUserSkillPayload {
  skillName: string;
  skillLevel: number;
}

export interface CreateUserEvaluationPayload {
  content: string;
}

export interface UserSkillSummary {
  skillId: number;
  skillName: string;
  skillLevel: number;
  updatedAt: string | null;
}

export interface UserSkillsResponse {
  userId: number;
  skills: UserSkillSummary[];
}

export interface UserEvaluationSummary {
  evaluationId: number;
  evaluateeUserId: number;
  evaluateeUserName: string;
  evaluatorUserId: number;
  evaluatorUserName: string;
  content: string;
  createdAt: string;
}
