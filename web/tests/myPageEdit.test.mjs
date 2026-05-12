import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const authService = readFileSync(
  new URL("../src/app/_common/service/auth.ts", import.meta.url),
  "utf8",
);
const useAuthHook = readFileSync(
  new URL("../src/app/_common/hooks/useAuth.ts", import.meta.url),
  "utf8",
);
const myPage = readFileSync(
  new URL("../src/app/(protected)/my-page/page.tsx", import.meta.url),
  "utf8",
);

test("current user profile update is wired to PATCH /users/me", () => {
  assert.match(authService, /export interface UpdateMyProfilePayload/);
  assert.match(authService, /export async function updateMyProfile/);
  assert.match(authService, /apiClient\.patch<EmptyResponse,\s*FormData>\(\s*"\/users\/me"/);
  assert.match(authService, /formData\.append\(\s*"request"/);
  assert.match(authService, /formData\.append\(\s*"profile_image"/);
  assert.match(authService, /fetchCurrentUser\(\)/);
  assert.match(useAuthHook, /updateMyProfile as updateMyProfileRequest/);
  assert.match(useAuthHook, /updateMyProfile,?/);
});

test("current user password update is wired to PATCH /auth/change-password without confirmation payload", () => {
  assert.match(authService, /export interface ChangePasswordPayload/);
  assert.match(authService, /export async function changePassword/);
  assert.match(authService, /apiClient\.patch<EmptyResponse,\s*ChangePasswordPayload>\(\s*"\/auth\/change-password"/);
  assert.match(authService, /currentPassword:\s*payload\.currentPassword/);
  assert.match(authService, /newPassword:\s*payload\.newPassword/);
  assert.doesNotMatch(authService, /passwordConfirm/);
  assert.match(useAuthHook, /changePassword as changePasswordRequest/);
  assert.match(useAuthHook, /changePassword,?/);
});

test("my page exposes editable profile form fields and keeps organization data read-only", () => {
  assert.match(myPage, /const \{ user, updateMyProfile, changePassword \} = useAuth\(\)/);
  assert.match(myPage, /<form[^>]+onSubmit=\{handleSubmit\}/);
  assert.match(myPage, /name="userName"/);
  assert.match(myPage, /name="email"/);
  assert.match(myPage, /name="phone"/);
  assert.match(myPage, /name="profileImage"/);
  assert.match(myPage, /저장/);
  assert.match(myPage, /저장되었습니다/);
  assert.doesNotMatch(myPage, /name="departmentId"/);
  assert.doesNotMatch(myPage, /name="titleName"/);
  assert.doesNotMatch(myPage, /name="employmentStatus"/);
});

test("my page exposes editable password fields and validates confirmation only on the frontend", () => {
  assert.match(myPage, /function PasswordChangeForm/);
  assert.match(myPage, /onChangePassword=\{changePassword\}/);
  assert.match(myPage, /name="currentPassword"/);
  assert.match(myPage, /name="newPassword"/);
  assert.match(myPage, /name="newPasswordConfirm"/);
  assert.match(myPage, /새 비밀번호가 일치하지 않습니다/);
  assert.match(myPage, /newPasswordConfirm:\s*""/);
  assert.match(myPage, /await onChangePassword\(\{\s*currentPassword,\s*newPassword,\s*\}\)/);
  assert.doesNotMatch(myPage, /<ReadOnlyField label="현재 비밀번호"/);
});

test("my page keeps new password and confirmation fields on the same desktop row", () => {
  assert.match(
    myPage,
    /<div className="grid gap-4 md:grid-cols-2">\s*<div className="md:col-span-2">\s*<PasswordField[\s\S]+name="currentPassword"[\s\S]+<\/div>\s*<PasswordField[\s\S]+name="newPassword"[\s\S]+<PasswordField[\s\S]+name="newPasswordConfirm"/,
  );
});

test("my page shows password confirmation mismatch feedback while typing", () => {
  assert.match(myPage, /const passwordMismatch =[\s\S]+values\.newPasswordConfirm\.length > 0[\s\S]+values\.newPassword !== values\.newPasswordConfirm/);
  assert.match(myPage, /errorMessage=\{passwordMismatch \? PASSWORD_MISMATCH_MESSAGE : undefined\}/);
  assert.match(myPage, /aria-invalid=\{Boolean\(errorMessage\)\}/);
  assert.match(myPage, /role="alert"[\s\S]+\{errorMessage\}/);
});

test("my page avoids duplicated read-only HR summary panel", () => {
  assert.doesNotMatch(myPage, /조회 전용 인사 정보/);
  assert.doesNotMatch(myPage, /function InfoCard/);
  assert.doesNotMatch(myPage, /xl:border-l/);
  assert.match(myPage, /조직 정보/);
});

test("my page visually separates editable controls from read-only information", () => {
  assert.match(myPage, /function ReadOnlyField[\s\S]+data-readonly="true"/);
  assert.match(myPage, /function ReadOnlyField[\s\S]+bg-muted\/35/);
  assert.match(myPage, /function ReadOnlyField[\s\S]+border-dashed/);
  assert.match(myPage, /function EditableField[\s\S]+border-primary\/35/);
  assert.match(myPage, /function ProfileImageInput[\s\S]+border-primary\/35/);
});

test("my page places save action at the bottom right of the editable form", () => {
  assert.match(myPage, /function EditableProfileForm[\s\S]+<ProfileImageInput[\s\S]+<Button/);
  assert.match(myPage, /className="flex justify-end"/);
  assert.match(myPage, /type="submit"[\s\S]+저장/);
});

test("my page profile image picker follows signup-style hidden file input", () => {
  assert.match(myPage, /function ProfileImageInput[\s\S]+htmlFor="profileImage"/);
  assert.match(myPage, /function ProfileImageInput[\s\S]+className="absolute inset-0 cursor-pointer opacity-0"/);
  assert.match(myPage, /function ProfileImageInput[\s\S]+프로필 이미지 선택/);
  assert.match(myPage, /function ProfileImageInput[\s\S]+onChange\(event\.target\.files\?\.\[0\] \?\? null\)/);
});
