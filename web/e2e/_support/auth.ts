import type { APIRequestContext, Page } from "@playwright/test";
import { seedAccounts, type E2ERole } from "./seed-accounts";

const E2E_REFRESH_ROUTE = "**/auth/refresh";
const E2E_LOGOUT_ROUTE = "**/auth/logout";

type ApiResponse<T> = {
  success: boolean;
  data: T;
  error?: {
    code?: string;
    message?: string;
  };
};

type AuthUser = {
  userId: number;
  userName: string;
  email: string;
  phone: string | null;
  departmentId: number | null;
  departmentName: string;
  positionName: string | null;
  titleName: string | null;
  joinDate: string | null;
  employmentStatus: string;
  profileImageUrl: string | null;
  teams: Array<{
    isPrimary: boolean;
    teamId: number;
    teamName: string;
    isLeader: boolean;
    teamRole: string;
    allocation: string | null;
  }>;
};

type AuthBootstrapState = {
  accessToken: string;
  user: AuthUser;
};

const authCache = new Map<E2ERole, AuthBootstrapState>();

export type { E2ERole } from "./seed-accounts";

export async function loginAs(page: Page, role: E2ERole = "DIRECTOR") {
  const auth = authCache.get(role) ?? (await authenticate(page.request, role));
  authCache.set(role, auth);

  await page.context().clearCookies();
  await page.unroute(E2E_REFRESH_ROUTE).catch(() => undefined);
  await page.unroute(E2E_LOGOUT_ROUTE).catch(() => undefined);

  await page.route(E2E_REFRESH_ROUTE, async (route) => {
    await route.fulfill({
      status: 200,
      headers: {
        authorization: `Bearer ${auth.accessToken}`,
        "access-control-expose-headers": "Authorization",
        "content-type": "application/json",
      },
      body: JSON.stringify({ success: true, data: {} }),
    });
  });

  await page.route(E2E_LOGOUT_ROUTE, async (route) => {
    await route.fulfill({
      status: 200,
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ success: true, data: {} }),
    });
  });

  await page.context().addCookies([
    {
      name: "refreshToken",
      value: "e2e",
      url: localBaseURL(),
      sameSite: "Lax",
    },
  ]);
}

export async function authenticate(request: APIRequestContext, role: E2ERole) {
  const account = seedAccounts[role];
  const loginResponse = await postLoginWithRetry(request, account);

  if (!loginResponse.ok()) {
    throw new Error(await responseDebugMessage(loginResponse, role));
  }

  const authorization = loginResponse.headers().authorization;
  const accessToken = extractAccessToken(authorization);

  if (!accessToken) {
    throw new Error(`${role} 로그인 응답에 Authorization header가 없습니다.`);
  }

  const meResponse = await request.get(`${apiBaseURL()}/users/me`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  if (!meResponse.ok()) {
    throw new Error(await responseDebugMessage(meResponse, role));
  }

  const me = (await meResponse.json()) as ApiResponse<AuthUser>;
  if (!me.success || !me.data) {
    throw new Error(`${role} /users/me 응답이 성공 data를 포함하지 않습니다.`);
  }

  return {
    accessToken,
    user: me.data,
  };
}

async function postLoginWithRetry(
  request: APIRequestContext,
  account: { email: string; password: string },
) {
  let lastResponse = await request.post(`${apiBaseURL()}/auth/login`, {
    data: {
      email: account.email,
      password: account.password,
    },
  });

  for (const delayMs of [5_000, 10_000, 20_000, 40_000]) {
    if (lastResponse.status() !== 429) {
      return lastResponse;
    }

    await new Promise((resolve) => setTimeout(resolve, delayMs));
    lastResponse = await request.post(`${apiBaseURL()}/auth/login`, {
      data: {
        email: account.email,
        password: account.password,
      },
    });
  }

  return lastResponse;
}

export function apiBaseURL() {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL?.trim();
  if (!value) {
    throw new Error("NEXT_PUBLIC_API_BASE_URL 환경변수가 필요합니다.");
  }

  return value.replace(/\/$/, "");
}

function localBaseURL() {
  const host = process.env.WEB_E2E_HOST ?? "localhost";
  return process.env.WEB_E2E_BASE_URL ?? `http://${host}:${process.env.WEB_E2E_PORT ?? 3000}`;
}

function extractAccessToken(authorization?: string) {
  if (!authorization) {
    return null;
  }

  return authorization.startsWith("Bearer ")
    ? authorization.slice("Bearer ".length)
    : authorization;
}

async function responseDebugMessage(response: { status: () => number; text: () => Promise<string>; url: () => string }, role: E2ERole) {
  const text = await response.text().catch(() => "");
  return `${role} API 인증 요청 실패: ${response.status()} ${response.url()} ${text.slice(0, 500)}`;
}
