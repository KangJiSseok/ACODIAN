"use client";

/* import문 */
import { ChangeEvent, FormEvent, Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import {
  ArrowLeft,
  ArrowRight,
  BriefcaseBusiness,
  Building2,
  CalendarDays,
  Eye,
  EyeOff,
  Image as ImageIcon,
  LockKeyhole,
  Mail,
  Phone,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import { getApiErrorMessage } from "@/app/_common/service/api-client";
import { login, signup } from "@/app/_common/service/auth";
import { cn } from "@/lib/utils";

// 이메일 검증 정규식
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TEST_EMAIL = "director@ibank.com";
const TEST_PASSWORD = "password1!";

const departmentOptions = [
  { departmentId: 1, departmentName: "개발본부" },
  { departmentId: 2, departmentName: "운영지원본부" },
  { departmentId: 3, departmentName: "비상대응본부" },
];

type AuthMode = "login" | "signup";

interface SignupFormState {
  departmentId: string;
  userName: string;
  email: string;
  password: string;
  positionName: string;
  titleName: string;
  joinDate: string;
  phone: string;
  profileImage: File | null;
}

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
      <AuthEntry />
    </Suspense>
  );
}

/* 인증 진입 화면 */
function AuthEntry() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // 현재 표시 중인 인증 패널
  const [mode, setMode] = useState<AuthMode>("login");

  // 로그인 입력값
  const [email, setEmail] = useState(TEST_EMAIL);
  const [password, setPassword] = useState(TEST_PASSWORD);

  // 회원가입 입력값
  const [signupValues, setSignupValues] = useState<SignupFormState>({
    departmentId: String(departmentOptions[0].departmentId),
    userName: "",
    email: "",
    password: "",
    positionName: "",
    titleName: "",
    joinDate: "",
    phone: "",
    profileImage: null,
  });

  // 비밀번호 표시 여부
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);

  // 요청 중인지 표시
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 화면에 보여줄 에러/성공 메시지
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  /* 패널 전환 */
  function changeMode(nextMode: AuthMode) {
    setMode(nextMode);
    setErrorMessage(null);
    setSuccessMessage(null);
    setIsPasswordVisible(false);
  }

  /* 회원가입 입력값 변경 */
  function updateSignupValue<K extends keyof SignupFormState>(
    key: K,
    value: SignupFormState[K],
  ) {
    setSignupValues((current) => ({ ...current, [key]: value }));
  }

  /* 로그인 submit 핸들러 */
  async function handleLoginSubmit(event: FormEvent<HTMLFormElement>) {
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
    setSuccessMessage(null);
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

  /* 회원가입 submit 핸들러 */
  async function handleSignupSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    // form 기본 새로고침 동작 방지

    const normalizedEmail = signupValues.email.trim().toLowerCase();
    const trimmedName = signupValues.userName.trim();
    const trimmedPosition = signupValues.positionName.trim();
    const trimmedTitle = signupValues.titleName.trim();
    const trimmedPhone = signupValues.phone.trim();

    if (!trimmedName) {
      setErrorMessage("이름을 입력해주세요.");
      return;
    }

    if (!EMAIL_REGEX.test(normalizedEmail)) {
      setErrorMessage("올바른 이메일 형식으로 입력해주세요.");
      return;
    }

    if (signupValues.password.length < 8) {
      setErrorMessage("비밀번호는 8자 이상 입력해주세요.");
      return;
    }

    if (!trimmedTitle) {
      setErrorMessage("직책을 입력해주세요.");
      return;
    }

    if (!signupValues.joinDate) {
      setErrorMessage("입사일을 선택해주세요.");
      return;
    }

    setErrorMessage(null);
    setSuccessMessage(null);
    setIsSubmitting(true);

    try {
      await signup({
        departmentId: Number(signupValues.departmentId),
        userName: trimmedName,
        email: normalizedEmail,
        password: signupValues.password,
        positionName: trimmedPosition,
        titleName: trimmedTitle,
        phone: trimmedPhone,
        joinDate: signupValues.joinDate,
        profileImage: signupValues.profileImage,
      });
      // 회원가입은 사용자 정보를 생성하고 로그인 화면으로 돌아가게 합니다.

      changeMode("login");
      setEmail(normalizedEmail);
      setPassword("");
      setSuccessMessage("회원가입 요청이 완료되었습니다. 로그인해주세요.");
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, "회원가입 요청에 실패했습니다."));
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
        poster="/videos/login-bg-poster.webp"
        className="absolute inset-0 h-full w-full object-cover"
      >
        <source src="/videos/login-bg.av1.webm" type='video/webm; codecs="av01"' />
        <source src="/videos/login-bg.webm" type="video/webm" />
        <source src="/videos/login-bg.mp4" type="video/mp4" />
      </video>

      <div className="absolute inset-0 bg-black/28" />
      <div className="absolute inset-x-0 bottom-0 h-72 bg-gradient-to-t from-black/72 via-black/26 to-transparent" />

      <section className="relative z-10 flex min-h-screen flex-col px-5 py-8 sm:px-8">
        <div
          className={cn(
            "flex flex-1 items-center justify-center text-center transition-all duration-500 ease-out",
            mode === "signup" ? "pb-4 sm:pb-6" : "pb-8",
          )}
        >
          <h1
            className={cn(
              "font-black leading-none tracking-tight text-white transition-all duration-500 ease-out",
              mode === "signup"
                ? "text-5xl sm:text-6xl lg:text-7xl"
                : "text-7xl sm:text-8xl lg:text-9xl",
            )}
          >
            AX-WMS
          </h1>
        </div>

        <div className="relative mx-auto mb-4 min-h-[26rem] w-full max-w-sm overflow-visible sm:mb-8">
          <LoginPanel
            email={email}
            errorMessage={mode === "login" ? errorMessage : null}
            isActive={mode === "login"}
            isPasswordVisible={isPasswordVisible}
            isSubmitting={isSubmitting}
            password={password}
            successMessage={mode === "login" ? successMessage : null}
            onEmailChange={setEmail}
            onModeChange={() => changeMode("signup")}
            onPasswordChange={setPassword}
            onPasswordVisibleChange={() =>
              setIsPasswordVisible((visible) => !visible)
            }
            onSubmit={handleLoginSubmit}
          />

          <SignupPanel
            errorMessage={mode === "signup" ? errorMessage : null}
            isActive={mode === "signup"}
            isPasswordVisible={isPasswordVisible}
            isSubmitting={isSubmitting}
            values={signupValues}
            onModeChange={() => changeMode("login")}
            onPasswordVisibleChange={() =>
              setIsPasswordVisible((visible) => !visible)
            }
            onSubmit={handleSignupSubmit}
            onValueChange={updateSignupValue}
          />
        </div>
      </section>
    </main>
  );
}

function LoginPanel({
  email,
  errorMessage,
  isActive,
  isPasswordVisible,
  isSubmitting,
  password,
  successMessage,
  onEmailChange,
  onModeChange,
  onPasswordChange,
  onPasswordVisibleChange,
  onSubmit,
}: {
  email: string;
  errorMessage: string | null;
  isActive: boolean;
  isPasswordVisible: boolean;
  isSubmitting: boolean;
  password: string;
  successMessage: string | null;
  onEmailChange: (value: string) => void;
  onModeChange: () => void;
  onPasswordChange: (value: string) => void;
  onPasswordVisibleChange: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}) {
  return (
    <form
      className={cn(
        "absolute inset-x-0 bottom-0 rounded-[1.75rem] border border-white/10 bg-black/42 p-5 shadow-[0_24px_90px_-42px_rgba(0,0,0,0.9)] backdrop-blur-xl transition-all duration-500 ease-out",
        isActive
          ? "translate-y-0 opacity-100"
          : "pointer-events-none translate-y-[110%] opacity-0",
      )}
      onSubmit={onSubmit}
    >
      <div className="mb-4 flex items-center justify-center">
        <div className="flex size-10 items-center justify-center rounded-2xl border border-cyan-300/25 bg-cyan-300/10 text-cyan-200">
          <ShieldCheck className="size-5" />
        </div>
      </div>

      <div className="grid gap-3">
        <AuthTextInput
          autoComplete="email"
          icon={UserRound}
          id="email"
          label="이메일"
          placeholder={TEST_EMAIL}
          type="email"
          value={email}
          onChange={onEmailChange}
        />

        <PasswordInput
          id="password"
          isVisible={isPasswordVisible}
          label="비밀번호"
          placeholder={TEST_PASSWORD}
          value={password}
          onChange={onPasswordChange}
          onVisibleChange={onPasswordVisibleChange}
        />

        <FeedbackMessage message={errorMessage} tone="error" />
        <FeedbackMessage message={successMessage} tone="success" />

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
        <button
          type="button"
          onClick={onModeChange}
          className="font-bold text-cyan-200 underline-offset-4 transition hover:text-cyan-100 hover:underline"
        >
          회원가입
        </button>
      </div>
    </form>
  );
}

function SignupPanel({
  errorMessage,
  isActive,
  isPasswordVisible,
  isSubmitting,
  values,
  onModeChange,
  onPasswordVisibleChange,
  onSubmit,
  onValueChange,
}: {
  errorMessage: string | null;
  isActive: boolean;
  isPasswordVisible: boolean;
  isSubmitting: boolean;
  values: SignupFormState;
  onModeChange: () => void;
  onPasswordVisibleChange: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onValueChange: <K extends keyof SignupFormState>(
    key: K,
    value: SignupFormState[K],
  ) => void;
}) {
  return (
    <form
      className={cn(
        "absolute inset-x-0 bottom-0 rounded-[1.75rem] border border-white/10 bg-black/46 p-5 shadow-[0_24px_90px_-42px_rgba(0,0,0,0.9)] backdrop-blur-xl transition-all duration-500 ease-out",
        isActive
          ? "translate-y-0 opacity-100"
          : "pointer-events-none translate-y-[112%] opacity-0",
      )}
      onSubmit={onSubmit}
    >
      <div className="mb-4 flex items-center justify-between">
        <button
          type="button"
          onClick={onModeChange}
          className="flex size-10 items-center justify-center rounded-2xl border border-white/10 bg-white/[0.08] text-slate-300 transition hover:bg-white/[0.12] hover:text-white"
          aria-label="로그인으로 돌아가기"
        >
          <ArrowLeft className="size-5" />
        </button>
        <div className="text-right">
          <p className="text-sm font-black text-white">회원가입</p>
          <p className="mt-0.5 text-xs font-medium text-slate-400">
            AX-WMS 계정을 요청합니다
          </p>
        </div>
      </div>

      <div className="grid max-h-[66vh] gap-3 overflow-y-auto pr-1">
        <label htmlFor="departmentId" className="sr-only">
          부서
        </label>
        <div className="relative">
          <Building2 className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-black/80" />
          <select
            id="departmentId"
            value={values.departmentId}
            onChange={(event) =>
              onValueChange("departmentId", event.target.value)
            }
            className="h-12 w-full appearance-none rounded-2xl border border-white/10 bg-white/[0.08] pl-11 pr-4 text-sm font-semibold text-white outline-none transition focus:border-cyan-300/60 focus:bg-white/[0.12]"
          >
            {departmentOptions.map((department) => (
              <option
                key={department.departmentId}
                className="bg-slate-950 text-white"
                value={department.departmentId}
              >
                {department.departmentName}
              </option>
            ))}
          </select>
        </div>

        <AuthTextInput
          autoComplete="name"
          icon={UserRound}
          id="signup-userName"
          label="이름"
          placeholder="이름"
          value={values.userName}
          onChange={(value) => onValueChange("userName", value)}
        />

        <AuthTextInput
          autoComplete="email"
          icon={Mail}
          id="signup-email"
          label="이메일"
          placeholder="email@ibank.com"
          type="email"
          value={values.email}
          onChange={(value) => onValueChange("email", value)}
        />

        <PasswordInput
          id="signup-password"
          isVisible={isPasswordVisible}
          label="비밀번호"
          placeholder="비밀번호"
          value={values.password}
          onChange={(value) => onValueChange("password", value)}
          onVisibleChange={onPasswordVisibleChange}
        />

        <div className="grid grid-cols-2 gap-3">
          <AuthTextInput
            icon={BriefcaseBusiness}
            id="positionName"
            label="직급"
            placeholder="직급"
            required={false}
            value={values.positionName}
            onChange={(value) => onValueChange("positionName", value)}
          />
          <AuthTextInput
            icon={ShieldCheck}
            id="titleName"
            label="직책"
            placeholder="직책"
            value={values.titleName}
            onChange={(value) => onValueChange("titleName", value)}
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <AuthTextInput
            icon={CalendarDays}
            id="joinDate"
            label="입사일"
            type="date"
            value={values.joinDate}
            onChange={(value) => onValueChange("joinDate", value)}
          />
          <AuthTextInput
            autoComplete="tel"
            icon={Phone}
            id="phone"
            label="연락처"
            placeholder="010-0000-0000"
            type="tel"
            required={false}
            value={values.phone}
            onChange={(value) => onValueChange("phone", value)}
          />
        </div>

        <ProfileImageInput
          fileName={values.profileImage?.name ?? ""}
          onChange={(file) => onValueChange("profileImage", file)}
        />

        <FeedbackMessage message={errorMessage} tone="error" />

        <button
          type="submit"
          disabled={isSubmitting}
          className="mt-1 flex h-12 w-full items-center justify-center gap-2 rounded-2xl bg-cyan-300 text-sm font-black text-[#06142f] transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isSubmitting ? "요청 중..." : "가입 요청"}
          <ArrowRight className="size-4" />
        </button>
      </div>
    </form>
  );
}

function AuthTextInput({
  autoComplete,
  icon: Icon,
  id,
  label,
  placeholder,
  required = true,
  type = "text",
  value,
  onChange,
}: {
  autoComplete?: string;
  icon: typeof UserRound;
  id: string;
  label: string;
  placeholder?: string;
  required?: boolean;
  type?: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="relative">
      <label htmlFor={id} className="sr-only">
        {label}
      </label>
      <Icon className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-black/80" />
      <input
        id={id}
        type={type}
        value={value}
        placeholder={placeholder}
        autoComplete={autoComplete}
        onChange={(event) => onChange(event.target.value)}
        required={required}
        className="h-12 w-full rounded-2xl border border-white/10 bg-white/[0.08] pl-11 pr-4 text-sm font-semibold text-white outline-none transition placeholder:text-slate-400 focus:border-cyan-300/60 focus:bg-white/[0.12]"
      />
    </div>
  );
}

function ProfileImageInput({
  fileName,
  onChange,
}: {
  fileName: string;
  onChange: (file: File | null) => void;
}) {
  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    onChange(event.target.files?.[0] ?? null);
  }

  return (
    <div className="relative">
      <label htmlFor="profileImage" className="sr-only">
        프로필 이미지
      </label>
      <ImageIcon className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-black/80" />
      <input
        id="profileImage"
        type="file"
        accept="image/*"
        onChange={handleChange}
        className="absolute inset-0 cursor-pointer opacity-0"
      />
      <div className="flex h-12 w-full items-center rounded-2xl border border-white/10 bg-white/[0.08] pl-11 pr-4 text-sm font-semibold text-white transition">
        <span className={cn("truncate", fileName ? "text-white" : "text-slate-400")}>
          {fileName || "프로필 이미지 선택"}
        </span>
      </div>
    </div>
  );
}

function PasswordInput({
  id,
  isVisible,
  label,
  placeholder,
  value,
  onChange,
  onVisibleChange,
}: {
  id: string;
  isVisible: boolean;
  label: string;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
  onVisibleChange: () => void;
}) {
  return (
    <div className="relative">
      <label htmlFor={id} className="sr-only">
        {label}
      </label>
      <LockKeyhole className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-black/80" />
      <input
        id={id}
        type={isVisible ? "text" : "password"}
        value={value}
        placeholder={placeholder}
        autoComplete="current-password"
        onChange={(event) => onChange(event.target.value)}
        required
        className="h-12 w-full rounded-2xl border border-white/10 bg-white/[0.08] pl-11 pr-12 text-sm font-semibold text-white outline-none transition placeholder:text-slate-400 focus:border-cyan-300/60 focus:bg-white/[0.12]"
      />
      <button
        type="button"
        onClick={onVisibleChange}
        className="absolute right-3 top-1/2 flex size-8 -translate-y-1/2 items-center justify-center rounded-lg text-slate-400 transition hover:bg-white/8 hover:text-cyan-100"
        aria-label={isVisible ? "비밀번호 숨기기" : "비밀번호 보기"}
      >
        {isVisible ? <Eye className="size-5" /> : <EyeOff className="size-5" />}
      </button>
    </div>
  );
}

function FeedbackMessage({
  message,
  tone,
}: {
  message: string | null;
  tone: "error" | "success";
}) {
  if (!message) {
    return null;
  }

  return (
    <p
      className={cn(
        "rounded-2xl border px-4 py-3 text-sm font-semibold",
        tone === "error"
          ? "border-red-300/25 bg-red-500/10 text-red-100"
          : "border-emerald-300/25 bg-emerald-500/10 text-emerald-100",
      )}
    >
      {message}
    </p>
  );
}
