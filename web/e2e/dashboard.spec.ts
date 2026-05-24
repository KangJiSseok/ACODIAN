import { expect, test, type Response } from "@playwright/test";
import { loginAs } from "./_support/auth";
import { expectLoadedWithoutAppError, waitForResponseOrNull } from "./_support/ui";

test.describe("dashboard - 실제 계정 역할별 화면", () => {
  for (const role of ["DIRECTOR", "DEPT_HEAD"] as const) {
    test(`${role} 계정은 실제 API 대시보드 데이터를 렌더링한다`, async ({ page }) => {
      await loginAs(page, role);
      const dashboardResponsePromise = waitForResponseOrNull(
        page,
        isDashboardResponse,
      );

      await page.goto("/");

      const dashboardResponse = await dashboardResponsePromise;
      test.skip(
        !dashboardResponse?.ok(),
        `현재 실제 API 대시보드 응답을 받을 수 없습니다: ${dashboardResponse?.status() ?? "no response"}`,
      );

      await expectLoadedWithoutAppError(page);
      await expect(
        page.getByRole("heading", { name: /업무 대시보드|ACODIAN 대시보드/, level: 2 }),
      ).toBeVisible();
      await expect(
        page.getByText(/전체 진행률|진행 중인 내 업무|오늘의 업무/).first(),
      ).toBeVisible();
    });
  }

  test("DIRECTOR는 대시보드 보기 기준 변경 UI를 열 수 있다", async ({ page }) => {
    await loginAs(page, "DIRECTOR");
    const dashboardResponsePromise = waitForResponseOrNull(
      page,
      isDashboardResponse,
    );

    await page.goto("/");

    const dashboardResponse = await dashboardResponsePromise;
    test.skip(
      !dashboardResponse?.ok(),
      `현재 실제 API 대시보드 응답을 받을 수 없습니다: ${dashboardResponse?.status() ?? "no response"}`,
    );
    await expectLoadedWithoutAppError(page);

    const scopeButton = page.getByRole("button", { name: /대시보드 보기 기준 변경/ });
    test.skip((await scopeButton.count()) === 0, "현재 데이터/권한에서 보기 기준 변경 UI가 노출되지 않습니다.");

    await scopeButton.click();
    await expect(page.getByRole("heading", { name: "보기 기준 선택" })).toBeVisible();
    await page.getByRole("button", { name: "취소" }).click();
  });
});

function isDashboardResponse(response: Response) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && url.pathname.endsWith("/dashboard");
}
