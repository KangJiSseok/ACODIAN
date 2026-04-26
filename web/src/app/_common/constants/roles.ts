// 사용자 권한 코드는 백엔드 UserRole enum과 맞춰 관리합니다.
export const USER_ROLE_CODES = [
  "DIRECTOR",
  "DEPT_HEAD",
  "TEAM_LEAD",
  "MEMBER",
] as const;

export type UserRoleCode = (typeof USER_ROLE_CODES)[number];

// 화면 표시용 라벨은 권한 코드와 분리해 이 파일에서만 바꿀 수 있게 둡니다.
export const USER_ROLE_LABELS: Record<UserRoleCode, string> = {
  DIRECTOR: "본부장",
  DEPT_HEAD: "사업부장",
  TEAM_LEAD: "팀장",
  MEMBER: "팀원",
};
