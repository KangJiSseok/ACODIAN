"use client";

/* import문 */
import { type ReactNode, useEffect, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";
import Gnb from "@/app/_common/components/layout/gnb";
import Sidebar from "@/app/_common/components/layout/sidebar";
import { useAuth } from "@/app/_common/hooks/useAuth";

/* 보호 라우트 레이아웃 */
export default function ProtectedLayout({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();

  // 인증 상태와 세션 복구 함수
  const { status, isAuthenticated, refreshSession } = useAuth();

  // refresh 요청 중복 방지
  const hasTriedRefresh = useRef(false);

  // 인증 확인 중인지 표시
  const [isChecking, setIsChecking] = useState(() => !isAuthenticated);

  /* 인증 상태 확인 */
  useEffect(() => {
    // 이미 로그인된 상태면 세션 복구를 시도하지 않음
    if (isAuthenticated) return;

    // refreshSession 중복 호출 방지
    if (hasTriedRefresh.current) {
      return;
    }

    hasTriedRefresh.current = true;

    refreshSession()
      .then((user) => {
        // 세션 복구 실패 시 로그인 페이지로 이동
        if (!user) {
          router.replace(`/login?redirect=${encodeURIComponent(pathname)}`);
        }
      })
      .finally(() => {
        // 인증 확인 종료
        setIsChecking(false);
      });
  }, [isAuthenticated, pathname, refreshSession, router]);

  /* 로딩 화면 */
  if (isChecking || status === "loading") {
    return (
      <main className="flex min-h-screen items-center justify-center">
        <p className="text-sm text-muted-foreground">로그인 상태 확인 중...</p>
      </main>
    );
  }

  /* 미인증 상태 */
  if (!isAuthenticated) {
    return null;
  }

  /* 인증된 화면 */
  return (
    <div className="grid min-h-screen md:grid-cols-[17.5rem_minmax(0,1fr)]">
      <Sidebar />
      <div className="flex min-h-screen min-w-0 flex-col">
        <Gnb />
        <main className="workspace-main flex-1 px-4 py-6 md:px-8">
          {children}
        </main>
      </div>
    </div>
  );
}
