export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center px-4 py-10">
      {/* 실제 인증 로직이 붙기 전까지는 로그인 전용 표면만 먼저 고정해 둡니다. */}
      <section className="workspace-panel registration-surface w-full max-w-md rounded-3xl p-8">
        <p className="workspace-kicker">authentication</p>
        <h1 className="mt-5 text-3xl font-semibold tracking-tight">AX-WMS 로그인</h1>
        <p className="mt-3 text-sm text-muted-foreground">
          인증 UI를 구현하기 전에 라우트와 시맨틱 토큰 연결이 먼저 준비된 상태입니다.
        </p>
        <div className="mt-8 grid gap-4">
          <div className="workspace-panel-soft rounded-2xl px-4 py-3">
            <span className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
              Email
            </span>
            <p className="mt-2 text-sm text-foreground/80">로그인 폼이 들어갈 입력 영역</p>
          </div>
          <div className="workspace-panel-soft rounded-2xl px-4 py-3">
            <span className="text-xs font-semibold uppercase tracking-[0.18em] text-muted-foreground">
              Password
            </span>
            <p className="mt-2 text-sm text-foreground/80">비밀번호 입력 영역</p>
          </div>
          <button
            type="button"
            className="mt-2 rounded-2xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground"
          >
            로그인 버튼 자리
          </button>
        </div>
      </section>
    </main>
  );
}
