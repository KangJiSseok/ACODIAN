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

test("my page exposes editable profile form fields and keeps organization data read-only", () => {
  assert.match(myPage, /const \{ user, updateMyProfile \} = useAuth\(\)/);
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
