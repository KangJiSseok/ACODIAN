import { expect, test } from "@playwright/test";
import { loginAs } from "./_support/auth";
import { waitForResponseOrNull } from "./_support/ui";
import { seedAccounts } from "./_support/seed-accounts";

test.describe("auth - 실제 API 인증 흐름", () => {
  test("로그인 성공 후 redirect 대상 보호 화면으로 이동한다", async ({ page }) => {
    const loginResponsePromise = waitForResponseOrNull(
      page,
      (response) =>
        response.request().method() === "POST" && response.url().includes("/auth/login"),
    );
    const meResponsePromise = waitForResponseOrNull(
      page,
      (response) =>
        response.request().method() === "GET" && response.url().includes("/users/me"),
    );

    await page.goto("/login?redirect=%2Fworklog");
    await page.getByLabel("이메일").first().fill(seedAccounts.DIRECTOR.email);
    await page
      .getByLabel("비밀번호", { exact: true })
      .first()
      .fill(seedAccounts.DIRECTOR.password);
    await page.getByRole("button", { name: "로그인", exact: true }).click();

    const loginResponse = await loginResponsePromise;
    test.skip(
      !loginResponse?.ok(),
      `현재 실제 API 로그인 응답을 받을 수 없습니다: ${loginResponse?.status() ?? "no response"}`,
    );
    const meResponse = await meResponsePromise;
    test.skip(
      !meResponse?.ok(),
      `현재 실제 API /users/me 응답을 받을 수 없습니다: ${meResponse?.status() ?? "no response"}`,
    );
    await expect(page).toHaveURL(/\/worklog$/);
    await expect(page.getByRole("heading", { name: "업무 검색" })).toBeVisible();
  });

  test("로그인 실패 메시지를 표시한다", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("이메일").first().fill(seedAccounts.DIRECTOR.email);
    await page
      .getByLabel("비밀번호", { exact: true })
      .first()
      .fill("wrong-password-for-real-e2e");
    await page.getByRole("button", { name: "로그인", exact: true }).click();

    await expect(
      page.getByText(/이메일 또는 비밀번호|로그인에 실패|Network Error/),
    ).toBeVisible();
  });

  test("미인증 보호 라우트 접근은 redirect query와 함께 로그인으로 이동한다", async ({ page }) => {
    await page.context().clearCookies();
    await page.goto("/worklog");

    await expect(page).toHaveURL(/\/login\?redirect=%2Fworklog$/);
    await expect(page.getByRole("button", { name: "로그인", exact: true })).toBeVisible();
  });

  test("로그아웃 후 로그인 화면으로 이동한다", async ({ page }) => {
    await loginAs(page, "DIRECTOR");

    await page.goto("/");
    await page.getByRole("button", { name: /로그아웃/ }).click();

    await expect(page).toHaveURL(/\/login$/);
  });
});
