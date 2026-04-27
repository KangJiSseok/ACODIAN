"use client";

/* 1. import문 */
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { login } from "@/app/_common/service/auth";
import { getApiErrorMessage } from "@/app/_common/service/api-client";

/* 2. 로그인 페이지 */
export default function LoginPage() {
  const router = useRouter();

  // 이메일 입력값
  const [email, setEmail] = useState("");

  // 비밀번호 입력값
  const [password, setPassword] = useState("");

  // 로그인 요청 중인지 표시
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 화면에 보여줄 에러 메시지
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  /* 3. 로그인 submit 핸들러 */
  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    // form 기본 새로고침 동작 방지

    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      await login({ email, password });
      // auth.ts의 login은 토큰 저장 + /users/me 조회 + 전역 상태 설정까지 해줌

      router.replace("/");
      // 로그인 성공 후 보호 영역의 기본 페이지로 이동
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
    <main className="flex min-h-screen items-center justify-center px-4 py-10">
      <section className="workspace-panel registration-surface w-full max-w-md rounded-3xl p-8">
        <p className="workspace-kicker">authentication</p>

        <h1 className="mt-5 text-3xl font-semibold tracking-tight">
          AX-WMS 로그인
        </h1>

        <form className="mt-8 grid gap-4" onSubmit={handleSubmit}>
          <Input
            type="email"
            value={email}
            placeholder="이메일"
            autoComplete="email"
            onChange={(event) => setEmail(event.target.value)}
            required
          />

          <Input
            type="password"
            value={password}
            placeholder="비밀번호"
            autoComplete="current-password"
            onChange={(event) => setPassword(event.target.value)}
            required
          />

          {errorMessage ? (
            <p className="text-sm text-destructive">{errorMessage}</p>
          ) : null}

          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? "로그인 중..." : "로그인"}
          </Button>
        </form>
      </section>
    </main>
  );
}
