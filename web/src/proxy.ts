import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

// 인증이 필요한 업무 화면 경로를 한곳에 모아 관리합니다.
const protectedPrefixes = [
  "/department",
  "/team",
  "/user",
  "/worklog",
  "/search",
  "/file",
  "/notification",
  "/tag",
];

// TODO(auth): 인증 화면 구현 중에는 보호 라우트 차단을 잠시 비활성화합니다.
const ENABLE_ROUTE_PROTECTION = false;

function isProtectedPath(pathname: string) {
  if (!ENABLE_ROUTE_PROTECTION) {
    return false;
  }

  return pathname === "/" || protectedPrefixes.some((prefix) => pathname.startsWith(prefix));
}

export function proxy(request: NextRequest) {
  const refreshToken = request.cookies.get("refreshToken")?.value;
  const { pathname } = request.nextUrl;

  // 로그인된 사용자가 로그인 화면에 다시 접근하면 메인 화면으로 보냅니다.
  if (pathname === "/login" && refreshToken) {
    return NextResponse.redirect(new URL("/", request.url));
  }

  // 보호 라우트는 refreshToken 쿠키가 없으면 로그인 화면으로 보냅니다.
  if (isProtectedPath(pathname) && !refreshToken) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  return NextResponse.next();
}

export const config = {
  // 정적 리소스와 Next 내부 경로는 proxy 검사 대상에서 제외합니다.
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico).*)"],
};
