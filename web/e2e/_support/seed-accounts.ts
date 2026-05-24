export type E2ERole = "DIRECTOR" | "DEPT_HEAD" | "TEAM_LEAD" | "MEMBER";

export const seedAccounts: Record<
  E2ERole,
  { email: string; password: string; userId: number; displayName: string }
> = {
  DIRECTOR: {
    email: env("WEB_E2E_DIRECTOR_EMAIL", "director@ibank.local"),
    get password() {
      return env("WEB_E2E_DIRECTOR_PASSWORD", "password1!");
    },
    userId: 1,
    displayName: "이지훈",
  },
  DEPT_HEAD: {
    email: env("WEB_E2E_DEPT_HEAD_EMAIL", "u002@ibank.local"),
    get password() {
      return env("WEB_E2E_DEPT_HEAD_PASSWORD", "password1!");
    },
    userId: 2,
    displayName: "김민준",
  },
  TEAM_LEAD: {
    email: env("WEB_E2E_TEAM_LEAD_EMAIL", "u035@ibank.local"),
    get password() {
      return env("WEB_E2E_TEAM_LEAD_PASSWORD", "password1!");
    },
    userId: 35,
    displayName: "정하윤",
  },
  MEMBER: {
    email: env("WEB_E2E_MEMBER_EMAIL", "u033@ibank.local"),
    get password() {
      return env("WEB_E2E_MEMBER_PASSWORD", "password1!");
    },
    userId: 33,
    displayName: "박서준",
  },
};

export function roleFromEmail(email: string): E2ERole | null {
  const found = Object.entries(seedAccounts).find(
    ([, account]) => account.email === email,
  );
  return found ? (found[0] as E2ERole) : null;
}

function env(name: string, fallback: string) {
  return process.env[name]?.trim() || fallback;
}

