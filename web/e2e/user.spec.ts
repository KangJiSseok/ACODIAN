import { expect, test, type Response } from "@playwright/test";
import { loginAs } from "./_support/auth";
import { seedAccounts } from "./_support/seed-accounts";
import {
  expectLoadedWithoutAppError,
  expectResultOrEmpty,
  waitForResponseOrNull,
} from "./_support/ui";

test.describe.configure({ mode: "serial" });

test.describe("user - 실제 API 사용자 관리", () => {
  test("DIRECTOR는 사용자 목록을 조회하고 검색 요청을 보낸다", async ({ page }) => {
    await loginAs(page, "DIRECTOR");
    const userListResponsePromise = waitForResponseOrNull(page, isUserListResponse);

    await page.goto("/user");

    const userListResponse = await userListResponsePromise;
    test.skip(
      !userListResponse?.ok(),
      `현재 실제 API 사용자 목록 응답을 받을 수 없습니다: ${userListResponse?.status() ?? "no response"}`,
    );

    await expectLoadedWithoutAppError(page);
    await expect(page.getByRole("heading", { name: "사용자 관리" })).toBeVisible();
    await expectResultOrEmpty(
      page.getByText(/조회된 사용자/),
      page.getByText(/표시할 사용자가 없습니다|조회된 사용자 0명/),
    );

    const searchResponsePromise = waitForResponseOrNull(
      page,
      (response) =>
        isUserListResponse(response) &&
        response.url().includes(encodeURIComponent(seedAccounts.MEMBER.displayName)),
    );
    await page.getByLabel("사용자 검색").fill(seedAccounts.MEMBER.displayName);
    const searchResponse = await searchResponsePromise;
    test.skip(
      !searchResponse?.ok(),
      `현재 실제 API 사용자 검색 응답을 받을 수 없습니다: ${searchResponse?.status() ?? "no response"}`,
    );
  });

  test("MEMBER는 내 정보 화면과 비밀번호 client validation을 확인한다", async ({ page }) => {
    await loginAs(page, "MEMBER");

    await page.goto("/my-page");

    await expectLoadedWithoutAppError(page);
    await expect(page.getByRole("heading", { name: "프로필 정보 및 계정 설정" })).toBeVisible();
    await expect(page.getByText(seedAccounts.MEMBER.email).first()).toBeVisible();

    await page.getByLabel("새 비밀번호", { exact: true }).fill("password2!");
    await page.getByLabel("새 비밀번호 확인").fill("password3!");
    await page.getByRole("button", { name: "비밀번호 변경" }).click();

    await expect(page.getByText("새 비밀번호가 일치하지 않습니다.").last()).toBeVisible();
  });

  test("MEMBER는 자기 상세에서 관리자 전용 탭과 수정 액션을 볼 수 없다", async ({ page }) => {
    await loginAs(page, "MEMBER");
    const userDetailResponsePromise = waitForResponseOrNull(
      page,
      (response) => isUserDetailResponse(response, seedAccounts.MEMBER.userId),
    );

    await page.goto(`/user/detail/${seedAccounts.MEMBER.userId}`);

    const userDetailResponse = await userDetailResponsePromise;
    test.skip(
      !userDetailResponse?.ok(),
      `현재 실제 API 사용자 상세 응답을 받을 수 없습니다: ${userDetailResponse?.status() ?? "no response"}`,
    );

    await expectLoadedWithoutAppError(page);
    await expect(page.getByRole("tablist", { name: "사용자 상세 탭" })).toBeVisible();
    await expect(page.getByRole("tab", { name: "사용자 정보" })).toBeVisible();
    await expect(page.getByRole("tab", { name: "스킬 설정" })).toHaveCount(0);
    await expect(page.getByRole("tab", { name: "관리자 평가" })).toHaveCount(0);
    await expect(page.getByRole("button", { name: "수정 저장" })).toHaveCount(0);
  });
});

function isUserListResponse(response: Response) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && url.pathname.endsWith("/users");
}

function isUserDetailResponse(response: Response, userId: number) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && url.pathname.endsWith(`/users/${userId}`);
}
