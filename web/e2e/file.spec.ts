import { expect, test, type Response } from "@playwright/test";
import { loginAs } from "./_support/auth";
import {
  expectLoadedWithoutAppError,
  expectResultOrEmpty,
  waitForResponseOrNull,
} from "./_support/ui";

test.describe("file - 실제 API 파일 관리", () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, "TEAM_LEAD");
  });

  test("파일 목록을 조회하고 검색/필터 UI를 조작한다", async ({ page }) => {
    const fileListResponsePromise = waitForResponseOrNull(page, isFileListResponse);

    await page.goto("/file");

    const fileListResponse = await fileListResponsePromise;
    test.skip(
      !fileListResponse?.ok(),
      `현재 실제 API 파일 목록 응답을 받을 수 없습니다: ${fileListResponse?.status() ?? "no response"}`,
    );

    await expectLoadedWithoutAppError(page);
    await expect(page.getByRole("heading", { name: "파일 관리" })).toBeVisible();
    await expect(page.getByText("조회된 파일")).toBeVisible();
    await expectResultOrEmpty(
      page.getByText("소속 업무일지").first(),
      page.getByText("표시할 파일이 없습니다."),
    );

    await page.getByLabel("파일 검색").fill("E2E");
    await page.getByRole("button", { name: "검색" }).click();

    await page.getByRole("button", { name: "필터", exact: true }).click();
    await expect(page.getByLabel("파일 형식 필터")).toBeVisible();
    await page.getByLabel("업로드 기간 필터").selectOption("ALL");
  });
});

function isFileListResponse(response: Response) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && url.pathname.endsWith("/files");
}
