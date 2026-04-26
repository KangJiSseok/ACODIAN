export const USER_ROLE_CODES = [
  "DIRECTOR",
  "DEPT_HEAD",
  "TEAM_LEAD",
  "MEMBER",
] as const;

export type UserRoleCode = (typeof USER_ROLE_CODES)[number];

export const USER_ROLE_LABELS: Record<UserRoleCode, string> = {
  DIRECTOR: "본부장",
  DEPT_HEAD: "사업부장",
  TEAM_LEAD: "팀장",
  MEMBER: "팀원",
};
