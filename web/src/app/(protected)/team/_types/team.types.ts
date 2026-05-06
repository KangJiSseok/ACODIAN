export type TeamStatusCode = "ACTIVE" | "INACTIVE" | string;

export interface TeamSummary {
  teamId: number;
  teamName: string;
  statusCode: TeamStatusCode;
  description: string | null;
  teamLeaderId: number | null;
  teamLeaderName: string | null;
  deptHeadAdminUserId?: number | null;
  deptHeadAdminUsername?: string | null;
  memberCount: number;
  myIsLeader: boolean;
  teamRole: string | null;
  allocation: string | null;
  isPrimary: boolean;
  startDate: string | null;
  expectedEndDate: string | null;
}

export interface TeamDetail {
  teamId: number;
  teamName: string;
  statusCode: TeamStatusCode;
  description: string | null;
  teamLeaderId: number | null;
  teamLeaderName: string | null;
  startDate: string | null;
  expectedEndDate: string | null;
  deptHeadAdminUserId: number | null;
  deptHeadAdminUsername: string | null;
}

export interface TeamUserSummary {
  isLeader: boolean;
  userId: number;
  userName: string;
  positionName: string | null;
  teamRole: string | null;
}

export interface TeamUsersResponse {
  items: TeamUserSummary[];
}

export interface TeamStatusSummary {
  activeTeamCount: number;
  inactiveTeamCount: number;
  totalTeamCount: number;
}

export interface GetTeamsParams {
  page?: number;
  pageSize?: number;
}

export interface TeamUserCandidate {
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
  employmentStatus: string;
}

export interface TeamUserInput {
  userId: number;
  isLeader: boolean;
  teamRole: string;
}

export interface CreateTeamRequest {
  teamName: string;
  description: string;
  addAdmin: number;
  addUsers: TeamUserInput[];
  statusCode: TeamStatusCode;
  startDate: string | null;
  expectedEndDate: string | null;
}

export interface UpdateTeamRequest {
  teamName?: string;
  description?: string;
  addAdmin?: number | null;
  removeAdmin?: number | null;
  addUsers?: TeamUserInput[];
  removeUsers?: number[];
  editUsers?: Array<{
    userId: number;
    teamRole: string;
  }>;
  statusCode?: TeamStatusCode;
  startDate?: string | null;
  expectedEndDate?: string | null;
}
