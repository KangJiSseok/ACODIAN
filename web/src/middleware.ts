import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

// 인증이 필요한 업무 화면 경로만 별도로 모아 둡니다.
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

// 임시로 로그인 없이 볼 수 있도록 설정
function isProtectedPath(pathname: string) {
  // return pathname === "/" || protectedPrefixes.some((prefix) => pathname.startsWith(prefix));
  return false;
}

export function middleware(request: NextRequest) {
  const token = request.cookies.get("accessToken")?.value;
  const { pathname } = request.nextUrl;

  // 로그인된 사용자가 로그인 화면에 다시 진입하면 메인으로 이동시킵니다.
  if (pathname === "/login" && token) {
    return NextResponse.redirect(new URL("/", request.url));
  }

  // 보호 라우트는 토큰이 없으면 로그인 화면으로 보내는 기본 정책을 사용합니다.
  if (isProtectedPath(pathname) && !token) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  return NextResponse.next();
}

export const config = {
  // 정적 자원과 Next 내부 경로는 보호 대상에서 제외합니다.
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico).*)"],
};
