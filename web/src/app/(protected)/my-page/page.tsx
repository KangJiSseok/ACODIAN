"use client";

import {
  type ChangeEvent,
  type FormEvent,
  useState,
} from "react";
import {
  Image as ImageIcon,
  KeyRound,
  LockKeyhole,
  Phone,
  UserRound,
  type LucideIcon,
} from "lucide-react";
import { useAuth } from "@/app/_common/hooks/useAuth";
import { getApiErrorMessage } from "@/app/_common/service/api-client";
import type {
  ChangePasswordPayload,
  UpdateMyProfilePayload,
} from "@/app/_common/service/auth";
import type { AuthUser } from "@/app/_common/store/auth.store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

// 백엔드 EmploymentStatus enum 값을 화면용 한글 라벨로 변환한다.
const employmentStatusLabels: Record<string, string> = {
  ACTIVE: "재직",
  LEAVE: "휴직",
  RETIRED: "퇴사",
};

function getInitial(name: string | undefined) {
  return name?.trim().slice(0, 1) || "?";
}

interface ProfileFormValues {
  userName: string;
  email: string;
  phone: string;
}

interface PasswordFormValues {
  currentPassword: string;
  newPassword: string;
  newPasswordConfirm: string;
}

const PASSWORD_MISMATCH_MESSAGE = "새 비밀번호가 일치하지 않습니다.";

export default function MyPage() {
  const { user, updateMyProfile, changePassword } = useAuth();

  // /users/me 응답의 팀 목록에서 대표 소속 팀 라벨을 만든다.
  const primaryTeam = user?.teams.find((team) => team.isPrimary);
  const displayStatus = user?.employmentStatus
    ? (employmentStatusLabels[user.employmentStatus] ?? user.employmentStatus)
    : "-";
  const roleLabel = [user?.positionName, user?.titleName]
    .filter(Boolean)
    .join(" · ");

  return (
    <section className="mx-auto max-w-[92rem]">
      <div className="workspace-panel rounded-3xl p-5 shadow-[0_26px_80px_-46px_rgba(15,23,42,0.45)] md:p-8">
        <div className="mb-8 flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.34em] text-muted-foreground">
              나의 정보
            </p>
            <h1 className="mt-2 text-xl font-black tracking-[-0.04em] text-foreground">
              프로필 정보 및 계정 설정
            </h1>
          </div>
          <div className="flex size-11 items-center justify-center rounded-2xl border border-primary/15 bg-primary/5 text-primary">
            <UserRound className="size-5" />
          </div>
        </div>

        <div className="space-y-8">
          <section className="workspace-panel-soft rounded-3xl p-5 md:p-6">
            <div className="flex flex-col gap-5 sm:flex-row sm:items-center">
              <div
                className="flex size-24 shrink-0 items-center justify-center overflow-hidden rounded-full bg-primary/10 text-3xl font-black text-primary ring-1 ring-primary/15"
                style={
                  user?.profileImageUrl
                    ? {
                        backgroundImage: `url(${user.profileImageUrl})`,
                        backgroundPosition: "center",
                        backgroundSize: "cover",
                      }
                    : undefined
                }
              >
                {user?.profileImageUrl ? null : getInitial(user?.userName)}
              </div>

              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="truncate text-2xl font-black tracking-[-0.04em] text-foreground">
                    {user?.userName ?? "-"}
                  </h2>
                  {user?.titleName ? (
                    <span className="rounded-full border border-primary/20 bg-primary/10 px-2.5 py-1 text-xs font-bold text-primary">
                      {user.titleName}
                    </span>
                  ) : null}
                  <span className="rounded-full border border-emerald-400/30 bg-emerald-400/10 px-2.5 py-1 text-xs font-bold text-emerald-600">
                    {displayStatus}
                  </span>
                </div>
                <p className="mt-3 truncate text-sm font-semibold text-muted-foreground">
                  {user?.email ?? "-"}
                </p>
                <p className="mt-3 text-sm font-bold text-foreground">
                  주 소속 팀{" "}
                  <span className="text-primary">
                    {primaryTeam?.teamName ?? "-"}
                  </span>
                </p>
              </div>
            </div>
          </section>

          {user ? (
            <EditableProfileForm
              key={`${user.userId}:${user.userName}:${user.email}:${user.phone ?? ""}`}
              user={user}
              onUpdateProfile={updateMyProfile}
            />
          ) : (
            <div className="workspace-empty rounded-2xl px-6 py-10 text-center text-sm">
              사용자 정보를 불러오는 중입니다.
            </div>
          )}

          <section className="border-t border-border pt-8">
            <p className="text-xs font-bold uppercase tracking-[0.22em] text-muted-foreground">
              조직 정보
            </p>
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              <ReadOnlyField label="소속 부서" value={user?.departmentName ?? "-"} />
              <ReadOnlyField
                label="주 소속 팀"
                value={primaryTeam?.teamName ?? "-"}
              />
              <ReadOnlyField label="직급 / 직책" value={roleLabel || "-"} />
              <ReadOnlyField label="상태" value={displayStatus} />
            </div>
          </section>

          <section className="border-t border-border pt-8">
            <p className="text-xs font-bold uppercase tracking-[0.22em] text-muted-foreground">
              비밀번호 변경
            </p>
            <PasswordChangeForm onChangePassword={changePassword} />
          </section>
        </div>
      </div>
    </section>
  );
}

function ReadOnlyField({
  icon: Icon,
  label,
  value,
}: {
  icon?: LucideIcon;
  label: string;
  value: string;
}) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-bold text-foreground">
        {label}
      </span>
      <span
        data-readonly="true"
        className="flex h-14 cursor-default items-center gap-3 rounded-2xl border border-dashed border-border/70 bg-muted/35 px-4 text-sm font-semibold text-muted-foreground shadow-none"
      >
        {Icon ? (
          <Icon className="size-4 shrink-0 text-muted-foreground" />
        ) : null}
        <span className="truncate text-foreground/75">{value}</span>
      </span>
    </label>
  );
}

function EditableProfileForm({
  user,
  onUpdateProfile,
}: {
  user: AuthUser;
  onUpdateProfile: (payload: UpdateMyProfilePayload) => Promise<AuthUser>;
}) {
  const [values, setValues] = useState<ProfileFormValues>({
    userName: user.userName,
    email: user.email,
    phone: user.phone ?? "",
  });
  const [profileImage, setProfileImage] = useState<File | null>(null);
  const [profileInputKey, setProfileInputKey] = useState(0);
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;

    setValues((prev) => ({
      ...prev,
      [name]: value,
    }));
    setMessage(null);
  }

  function handleProfileImageChange(file: File | null) {
    setProfileImage(file);
    setMessage(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const userName = values.userName.trim();
    const email = values.email.trim();
    const phone = values.phone.trim();

    if (!userName || !email) {
      setMessage({
        type: "error",
        text: "이름과 이메일을 입력해주세요.",
      });
      return;
    }

    setIsSaving(true);
    setMessage(null);

    try {
      await onUpdateProfile({
        userName,
        email,
        phone,
        profileImage,
      });
      setProfileImage(null);
      setProfileInputKey((prev) => prev + 1);
      setMessage({
        type: "success",
        text: "저장되었습니다.",
      });
    } catch (error) {
      setMessage({
        type: "error",
        text: getApiErrorMessage(error, "프로필 저장에 실패했습니다."),
      });
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <form className="space-y-4" onSubmit={handleSubmit}>
      <div>
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.22em] text-muted-foreground">
            직접 수정 가능
          </p>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            이름, 이메일, 연락처와 프로필 이미지를 수정할 수 있습니다.
          </p>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <EditableField
          label="이름"
          name="userName"
          value={values.userName}
          onChange={handleChange}
          disabled={isSaving}
          required
        />
        <EditableField
          label="이메일"
          name="email"
          type="email"
          value={values.email}
          onChange={handleChange}
          disabled={isSaving}
          required
        />
        <EditableField
          icon={Phone}
          label="연락처"
          name="phone"
          value={values.phone}
          onChange={handleChange}
          disabled={isSaving}
          placeholder="010-0000-0000"
        />
        <ProfileImageInput
          inputKey={profileInputKey}
          fileName={profileImage?.name ?? null}
          disabled={isSaving}
          onChange={handleProfileImageChange}
        />
      </div>

      {message ? (
        <p
          className={cn(
            "rounded-2xl border px-4 py-3 text-sm font-semibold",
            message.type === "success"
              ? "border-emerald-400/30 bg-emerald-400/10 text-emerald-700"
              : "border-destructive/30 bg-destructive/5 text-destructive",
          )}
        >
          {message.text}
        </p>
      ) : null}

      <div className="flex justify-end">
        <Button
          type="submit"
          disabled={isSaving}
          className="h-11 min-w-28 rounded-2xl !text-primary-foreground hover:!text-primary-foreground"
        >
          {isSaving ? "저장 중" : "저장"}
        </Button>
      </div>
    </form>
  );
}

function PasswordChangeForm({
  onChangePassword,
}: {
  onChangePassword: (payload: ChangePasswordPayload) => Promise<unknown>;
}) {
  const [values, setValues] = useState<PasswordFormValues>({
    currentPassword: "",
    newPassword: "",
    newPasswordConfirm: "",
  });
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const passwordMismatch =
    values.newPasswordConfirm.length > 0 &&
    values.newPassword !== values.newPasswordConfirm;

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const { name, value } = event.target;

    setValues((prev) => ({
      ...prev,
      [name]: value,
    }));
    setMessage(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const { currentPassword, newPassword, newPasswordConfirm } = values;

    if (
      !currentPassword.trim() ||
      !newPassword.trim() ||
      !newPasswordConfirm.trim()
    ) {
      setMessage({
        type: "error",
        text: "비밀번호를 모두 입력해주세요.",
      });
      return;
    }

    if (newPassword !== newPasswordConfirm) {
      setMessage({
        type: "error",
        text: PASSWORD_MISMATCH_MESSAGE,
      });
      return;
    }

    setIsSaving(true);
    setMessage(null);

    try {
      await onChangePassword({
        currentPassword,
        newPassword,
      });
      setValues({
        currentPassword: "",
        newPassword: "",
        newPasswordConfirm: "",
      });
      setMessage({
        type: "success",
        text: "비밀번호가 변경되었습니다.",
      });
    } catch (error) {
      setMessage({
        type: "error",
        text: getApiErrorMessage(error, "비밀번호 변경에 실패했습니다."),
      });
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <form className="mt-4 space-y-4" onSubmit={handleSubmit}>
      <div className="grid gap-4 md:grid-cols-2">
        <div className="md:col-span-2">
          <PasswordField
            label="현재 비밀번호"
            name="currentPassword"
            value={values.currentPassword}
            autoComplete="current-password"
            disabled={isSaving}
            onChange={handleChange}
          />
        </div>
        <PasswordField
          label="새 비밀번호"
          name="newPassword"
          value={values.newPassword}
          autoComplete="new-password"
          disabled={isSaving}
          onChange={handleChange}
        />
        <PasswordField
          label="새 비밀번호 확인"
          name="newPasswordConfirm"
          value={values.newPasswordConfirm}
          autoComplete="new-password"
          disabled={isSaving}
          onChange={handleChange}
          errorMessage={passwordMismatch ? PASSWORD_MISMATCH_MESSAGE : undefined}
        />
      </div>

      {message ? (
        <p
          className={cn(
            "rounded-2xl border px-4 py-3 text-sm font-semibold",
            message.type === "success"
              ? "border-emerald-400/30 bg-emerald-400/10 text-emerald-700"
              : "border-destructive/30 bg-destructive/5 text-destructive",
          )}
        >
          {message.text}
        </p>
      ) : null}

      <div className="flex justify-end">
        <Button
          type="submit"
          disabled={isSaving}
          className="h-11 min-w-36 rounded-2xl !text-primary-foreground hover:!text-primary-foreground"
        >
          <KeyRound className="size-4" />
          {isSaving ? "변경 중" : "비밀번호 변경"}
        </Button>
      </div>
    </form>
  );
}

function EditableField({
  icon: Icon,
  label,
  name,
  value,
  type = "text",
  placeholder,
  disabled,
  required = false,
  onChange,
}: {
  icon?: LucideIcon;
  label: string;
  name: keyof ProfileFormValues;
  value: string;
  type?: string;
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
}) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-bold text-foreground">
        {label}
      </span>
      <span className="relative block">
        {Icon ? (
          <Icon className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        ) : null}
        <Input
          name={name}
          type={type}
          value={value}
          onChange={onChange}
          disabled={disabled}
          required={required}
          placeholder={placeholder}
          className={cn(
            "h-14 border-primary/35 bg-background font-semibold shadow-sm focus-visible:ring-primary/30",
            Icon && "pl-11",
          )}
        />
      </span>
    </label>
  );
}

function PasswordField({
  label,
  name,
  value,
  autoComplete,
  disabled,
  errorMessage,
  onChange,
}: {
  label: string;
  name: keyof PasswordFormValues;
  value: string;
  autoComplete: string;
  disabled?: boolean;
  errorMessage?: string;
  onChange: (event: ChangeEvent<HTMLInputElement>) => void;
}) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm font-bold text-foreground">
        {label}
      </span>
      <span className="relative block">
        <LockKeyhole className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          name={name}
          type="password"
          value={value}
          autoComplete={autoComplete}
          onChange={onChange}
          disabled={disabled}
          aria-invalid={Boolean(errorMessage)}
          className={cn(
            "h-14 border-primary/35 bg-background pl-11 font-semibold shadow-sm focus-visible:ring-primary/30",
            errorMessage &&
              "border-destructive/70 focus-visible:ring-destructive/30",
          )}
        />
      </span>
      {errorMessage ? (
        <p role="alert" className="mt-2 text-sm font-semibold text-destructive">
          {errorMessage}
        </p>
      ) : null}
    </label>
  );
}

function ProfileImageInput({
  inputKey,
  fileName,
  disabled,
  onChange,
}: {
  inputKey: number;
  fileName: string | null;
  disabled?: boolean;
  onChange: (file: File | null) => void;
}) {
  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    onChange(event.target.files?.[0] ?? null);
  }

  return (
    <div className="block">
      <label
        htmlFor="profileImage"
        className="mb-2 block text-sm font-bold text-foreground"
      >
        프로필 이미지
      </label>
      <div className="relative">
        <ImageIcon className="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          key={inputKey}
          id="profileImage"
          name="profileImage"
          type="file"
          accept="image/*"
          className="absolute inset-0 cursor-pointer opacity-0"
          disabled={disabled}
          onChange={handleChange}
        />
        <div
          className={cn(
            "flex h-14 w-full items-center rounded-2xl border border-primary/35 bg-background pl-11 pr-4 text-sm font-semibold shadow-sm transition",
            disabled && "opacity-60",
          )}
        >
          <span
            className={cn(
              "truncate",
              fileName ? "text-foreground" : "text-muted-foreground",
            )}
          >
            {fileName || "프로필 이미지 선택"}
          </span>
        </div>
      </div>
    </div>
  );
}
