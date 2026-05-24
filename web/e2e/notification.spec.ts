import { expect, test, type Response } from "@playwright/test";
import { loginAs } from "./_support/auth";
import {
  expectLoadedWithoutAppError,
  expectResultOrEmpty,
  waitForResponseOrNull,
} from "./_support/ui";

test.describe("notification - 실제 API 알림 확인", () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, "TEAM_LEAD");
  });

  test("알림 목록을 조회하고 읽음/빈 상태를 표시한다", async ({ page }) => {
    const notificationResponsePromise = waitForResponseOrNull(
      page,
      isNotificationListResponse,
    );

    await page.goto("/notification");

    const notificationResponse = await notificationResponsePromise;
    test.skip(
      !notificationResponse?.ok(),
      `현재 실제 API 알림 목록 응답을 받을 수 없습니다: ${notificationResponse?.status() ?? "no response"}`,
    );

    await expectLoadedWithoutAppError(page);
    await expect(page.getByRole("heading", { name: "알림", level: 1 })).toBeVisible();
    await expect(page.getByRole("heading", { name: "알림 현황" })).toBeVisible();
    await expect(page.getByText("읽지 않은 알림")).toBeVisible();
    await expectResultOrEmpty(
      page.getByText(/미읽음|읽음/).first(),
      page.getByText("표시할 알림이 없습니다."),
    );
  });

  test("부서/팀 필터 UI를 실제 데이터 옵션 기준으로 조작할 수 있다", async ({ page }) => {
    const notificationResponsePromise = waitForResponseOrNull(
      page,
      isNotificationListResponse,
    );

    await page.goto("/notification");

    const notificationResponse = await notificationResponsePromise;
    test.skip(
      !notificationResponse?.ok(),
      `현재 실제 API 알림 목록 응답을 받을 수 없습니다: ${notificationResponse?.status() ?? "no response"}`,
    );

    await expectLoadedWithoutAppError(page);
    await page.getByRole("button", { name: "필터", exact: true }).click();
    await expect(page.getByLabel("부서 필터")).toBeVisible();
    await expect(page.getByLabel("팀 필터")).toBeVisible();
    await page.getByLabel("부서 필터").selectOption({ index: 0 });
    await page.getByLabel("팀 필터").selectOption({ index: 0 });
  });

  test("GNB 알림 센터는 실제 초기 알림 목록 또는 빈 상태를 표시한다", async ({ page }) => {
    const notificationResponsePromise = waitForResponseOrNull(
      page,
      isNotificationListResponse,
    );

    await page.goto("/");

    const notificationResponse = await notificationResponsePromise;
    test.skip(
      !notificationResponse?.ok(),
      `현재 실제 API GNB 알림 응답을 받을 수 없습니다: ${notificationResponse?.status() ?? "no response"}`,
    );

    await expectLoadedWithoutAppError(page);
    await page.getByRole("button", { name: /알림 \d+개/ }).click();

    await expect(page.getByRole("heading", { name: "알림 센터" })).toBeVisible();
    await expectResultOrEmpty(
      page.getByRole("link").filter({ hasText: /년|월|일/ }).first(),
      page.getByText("읽지 않은 알림이 없습니다."),
    );
  });
});

function isNotificationListResponse(response: Response) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && url.pathname.endsWith("/notifications/me");
}
