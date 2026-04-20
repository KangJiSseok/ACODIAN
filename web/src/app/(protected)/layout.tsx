import type { ReactNode } from "react";
import Gnb from "@/app/_common/components/layout/gnb";
import Sidebar from "@/app/_common/components/layout/sidebar";

export default function ProtectedLayout({ children }: { children: ReactNode }) {
  return (
    // 로그인 이후 화면에서만 공통 셸을 재사용하도록 protected route group에 둡니다.
    <div className="workspace-shell min-h-screen">
      <div className="grid min-h-screen md:grid-cols-[240px_1fr]">
        <Sidebar />
        <div className="flex min-h-screen flex-col">
          <Gnb />
          <main className="workspace-main flex-1 px-4 py-6 md:px-8">{children}</main>
        </div>
      </div>
    </div>
  );
}
