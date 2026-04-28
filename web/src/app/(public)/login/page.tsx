"use client";

/* import문 */
import Link from "next/link";
import { FormEvent, Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  ArrowRight,
  Eye,
  EyeOff,
  LockKeyhole,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { login } from "@/app/_common/service/auth";
import { getApiErrorMessage } from "@/app/_common/service/api-client";

// 이메일 검증 정규식
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TEST_EMAIL = "director@ibank.com";
const TEST_PASSWORD = "password1!";

/* redirect 경로 검증 */
function getSafeRedirectPath(redirectPath: string | null): string {
  // redirect 값이 없으면 홈으로 이동
  if (!redirectPath) {
    return "/";
  }

  // 외부 URL 이동 방지
  if (!redirectPath.startsWith("/") || redirectPath.startsWith("//")) {
    return "/";
  }

  return redirectPath;
}

/* 로그인 페이지 */
export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  );
}

/* 로그인 form */
function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // 이메일 입력값
  const [email, setEmail] = useState(TEST_EMAIL);

  // 비밀번호 입력값
  const [password, setPassword] = useState(TEST_PASSWORD);

  // 비밀번호 표시 여부
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);

  // 로그인 요청 중인지 표시
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 화면에 보여줄 에러 메시지
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  /* 로그인 submit 핸들러 */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    // form 기본 새로고침 동작 방지

    const normalizedEmail = email.trim().toLowerCase();

    if (!normalizedEmail) {
      setErrorMessage("이메일을 입력해주세요.");
      return;
    }

    if (!EMAIL_REGEX.test(normalizedEmail)) {
      setErrorMessage("올바른 이메일 형식으로 입력해주세요.");
      return;
    }

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await login({ email: normalizedEmail, password });
      // auth.ts의 login은 토큰 저장 + /users/me 조회 + 전역 상태 설정까지 해줌

      router.replace(getSafeRedirectPath(searchParams.get("redirect")));
      // 로그인 전 접근하려던 보호 페이지가 있으면 그곳으로 이동
    } catch (error) {
      setErrorMessage(
        getApiErrorMessage(error, "이메일 또는 비밀번호를 확인해주세요."),
      );
      // API 에러를 사용자에게 보여줄 문자열로 변환
    } finally {
      setIsSubmitting(false);
      // 성공/실패와 관계없이 버튼 로딩 상태 해제
    }
  }

  return (
    <main className="relative min-h-screen overflow-hidden bg-[#030712] text-white">
      <video
        autoPlay
        muted
        loop
        playsInline
        preload="metadata"
        poster="/videos/login-bg-poster.jpg"
        className="absolute inset-0 h-full w-full object-cover"
      >
        <source src="/videos/login-bg.mp4" type="video/mp4" />
      </video>

      <div className="absolute inset-0 bg-black/28" />
      <div className="absolute inset-x-0 bottom-0 h-72 bg-gradient-to-t from-black/72 via-black/26 to-transparent" />

      <section className="relative z-10 flex min-h-screen flex-col px-5 py-8 sm:px-8">
        <div className="flex flex-1 items-center justify-center pb-8 text-center">
          <h1 className="text-7xl font-black leading-none tracking-tight text-white sm:text-8xl lg:text-9xl">
            AX-WMS
          </h1>
        </div>

        <form
          className="mx-auto mb-4 w-full max-w-sm rounded-[1.75rem] border border-white/10 bg-black/42 p-5 shadow-[0_24px_90px_-42px_rgba(0,0,0,0.9)] backdrop-blur-xl sm:mb-8"
          onSubmit={handleSubmit}
        >
          <div className="mb-4 flex items-center justify-center">
            <div className="flex size-10 items-center justify-center rounded-2xl border border-cyan-300/25 bg-cyan-300/10 text-cyan-200">
              <ShieldCheck className="size-5" />
            </div>
          </div>

          <div className="grid gap-3">
            <div className="relative">
              <label htmlFor="email" className="sr-only">
                이메일
              </label>
              <UserRound className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-black/80" />
              <input
                id="email"
                type="email"
                value={email}
                placeholder={TEST_EMAIL}
                autoComplete="email"
                onChange={(event) => setEmail(event.target.value)}
                required
                className="h-12 w-full rounded-2xl border border-white/10 bg-white/[0.08] pl-11 pr-4 text-sm font-semibold text-white outline-none transition placeholder:text-slate-400 focus:border-cyan-300/60 focus:bg-white/[0.12]"
              />
            </div>

            <div className="relative">
              <label htmlFor="password" className="sr-only">
                비밀번호
              </label>
              <LockKeyhole className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-black/80" />
              <input
                id="password"
                type={isPasswordVisible ? "text" : "password"}
                value={password}
                placeholder={TEST_PASSWORD}
                autoComplete="current-password"
                onChange={(event) => setPassword(event.target.value)}
                required
                className="h-12 w-full rounded-2xl border border-white/10 bg-white/[0.08] pl-11 pr-12 text-sm font-semibold text-white outline-none transition placeholder:text-slate-400 focus:border-cyan-300/60 focus:bg-white/[0.12]"
              />
              <button
                type="button"
                onClick={() => setIsPasswordVisible((visible) => !visible)}
                className="absolute right-3 top-1/2 flex size-8 -translate-y-1/2 items-center justify-center rounded-lg text-slate-400 transition hover:bg-white/8 hover:text-cyan-100"
                aria-label={
                  isPasswordVisible ? "비밀번호 숨기기" : "비밀번호 보기"
                }
              >
                {isPasswordVisible ? (
                  <Eye className="size-5" />
                ) : (
                  <EyeOff className="size-5" />
                )}
              </button>
            </div>

            {errorMessage ? (
              <p className="rounded-2xl border border-red-300/25 bg-red-500/10 px-4 py-3 text-sm font-semibold text-red-100">
                {errorMessage}
              </p>
            ) : null}

            <button
              type="submit"
              disabled={isSubmitting}
              className="mt-1 flex h-12 w-full items-center justify-center gap-2 rounded-2xl bg-cyan-300 text-sm font-black text-[#06142f] transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isSubmitting ? "로그인 중..." : "로그인"}
              <ArrowRight className="size-4" />
            </button>
          </div>

          <div className="mt-4 flex items-center justify-center gap-2 text-xs font-medium text-slate-400">
            <span>아직 계정이 없으신가요?</span>
            <Link
              href="/signup"
              className="font-bold text-cyan-200 underline-offset-4 transition hover:text-cyan-100 hover:underline"
            >
              회원가입
            </Link>
          </div>
        </form>
      </section>
    </main>
  );
}
