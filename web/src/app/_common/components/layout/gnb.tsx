export default function Gnb() {
  return (
    // 상단 바는 protected 레이아웃에서만 쓰는 기본 워크스페이스 헤더입니다.
    <header className="workspace-topbar border-b border-sidebar-border/60 px-4 py-4 text-sidebar-foreground md:px-8">
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-sidebar-primary">
            AX-WMS
          </p>
          <h1 className="mt-1 text-lg font-semibold">Workspace Shell</h1>
        </div>
        <p className="text-sm text-sidebar-foreground/70">UI 구조와 라우팅 기본 세팅</p>
      </div>
    </header>
  );
}
