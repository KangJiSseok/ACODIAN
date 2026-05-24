import { expect, test, type Response } from "@playwright/test";
import { loginAs } from "./_support/auth";
import {
  expectLoadedWithoutAppError,
  expectResultOrEmpty,
  waitForResponseOrNull,
} from "./_support/ui";

test.describe("search-tag - 실제 API 검색/태그 탐색", () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, "MEMBER");
  });

  test("시맨틱 검색 라우트는 보호 레이아웃 안에서 열린다", async ({ page }) => {
    await page.goto("/search");

    await expectLoadedWithoutAppError(page);
    await expect(page.getByRole("heading", { name: "시맨틱 검색" })).toBeVisible();
    await expect(
      page.getByText("검색 입력, 필터, 결과 목록을 배치할 페이지 자리입니다."),
    ).toBeVisible();
  });

  test("태그 목록을 조회하고 검색어로 다시 요청한다", async ({ page }) => {
    const initialTagResponsePromise = waitForResponseOrNull(
      page,
      isTagSearchResponse,
    );

    await page.goto("/tag");

    const initialTagResponse = await initialTagResponsePromise;
    test.skip(
      !initialTagResponse?.ok(),
      `현재 실제 API 태그 목록 응답을 받을 수 없습니다: ${initialTagResponse?.status() ?? "no response"}`,
    );

    await expectLoadedWithoutAppError(page);
    await expect(page.getByRole("heading", { name: "태그 목록" })).toBeVisible();
    await expectResultOrEmpty(
      page.getByText(/^#/).first(),
      page.getByText(/표시할 태그가 없습니다|조회된 태그 0개/),
    );

    const searchResponsePromise = waitForResponseOrNull(
      page,
      isTagSearchResponse,
    );
    await page.getByPlaceholder("태그 이름으로 검색하세요").fill("업무");
    await page.getByRole("button", { name: "검색", exact: true }).click();
    const searchResponse = await searchResponsePromise;
    test.skip(
      !searchResponse?.ok(),
      `현재 실제 API 태그 검색 응답을 받을 수 없습니다: ${searchResponse?.status() ?? "no response"}`,
    );
  });

});

function isTagSearchResponse(response: Response) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && url.pathname.endsWith("/tags/search");
}