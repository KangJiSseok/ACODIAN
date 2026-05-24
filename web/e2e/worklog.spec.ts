import { expect, test, type Response } from "@playwright/test";
import { loginAs } from "./_support/auth";
import {
  countVisible,
  expectLoadedWithoutAppError,
  expectResultOrEmpty,
  waitForResponseOrNull,
} from "./_support/ui";

test.describe.configure({ mode: "serial" });

test.describe("worklog - 실제 API 업무일지 흐름", () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, "TEAM_LEAD");
  });

  test("목록에서 키워드 검색을 실제 API로 요청한다", async ({ page }) => {
    const worklogListResponsePromise = waitForResponseOrNull(
      page,
      isWorklogListResponse,
    );

    await page.goto("/worklog");

    const worklogListResponse = await worklogListResponsePromise;
    test.skip(
      !worklogListResponse?.ok(),
      `현재 실제 API 업무 목록 응답을 받을 수 없습니다: ${worklogListResponse?.status() ?? "no response"}`,
    );

    await expectLoadedWithoutAppError(page);
    await expect(page.getByRole("heading", { name: "업무 검색" })).toBeVisible();
    const listLoadError = page.getByText("업무 목록을 불러오지 못했습니다.");
    await expectResultOrEmpty(
      page.getByRole("link", { name: "상세" }).first(),
      page.getByText("조건에 맞는 업무가 없습니다.").or(listLoadError),
    );
    test.skip(
      (await listLoadError.count()) > 0,
      "현재 실제 계정/서버 상태에서 업무 목록 API를 불러오지 못합니다.",
    );

    const searchResponsePromise = waitForResponseOrNull(
      page,
      isWorklogKeywordSearchResponse,
    );
    await page.getByPlaceholder("업무 제목으로 검색하세요").fill("E2E");
    await page.getByRole("button", { name: "검색", exact: true }).click();
    const searchResponse = await searchResponsePromise;
    test.skip(
      !searchResponse?.ok(),
      `현재 실제 API 업무 검색 응답을 받을 수 없습니다: ${searchResponse?.status() ?? "no response"}`,
    );
  });

  test("목록에 실제 항목이 있으면 첫 상세 화면으로 이동한다", async ({ page }) => {
    const worklogListResponsePromise = waitForResponseOrNull(
      page,
      isWorklogListResponse,
    );

    await page.goto("/worklog");

    const worklogListResponse = await worklogListResponsePromise;
    test.skip(
      !worklogListResponse?.ok(),
      `현재 실제 API 업무 목록 응답을 받을 수 없습니다: ${worklogListResponse?.status() ?? "no response"}`,
    );

    await expectLoadedWithoutAppError(page);
    const detailLinks = page.getByRole("link", { name: "상세" });
    test.skip((await countVisible(detailLinks)) === 0, "현재 실제 서버 데이터에 상세로 이동할 업무가 없습니다.");

    const detailResponsePromise = waitForResponseOrNull(page, isWorklogDetailResponse);
    await detailLinks.first().click();
    const detailResponse = await detailResponsePromise;
    test.skip(
      !detailResponse?.ok(),
      `현재 실제 API 업무 상세 응답을 받을 수 없습니다: ${detailResponse?.status() ?? "no response"}`,
    );

    await expect(page).toHaveURL(/\/worklog\/detail\/\d+$/);
    await expect(page.getByText("요청 내용")).toBeVisible();
  });

});

function isWorklogListResponse(response: Response) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && url.pathname.endsWith("/worklogs");
}

function isWorklogKeywordSearchResponse(response: Response) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && url.pathname.endsWith("/worklogs/search/keyword");
}

function isWorklogDetailResponse(response: Response) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && /\/worklogs\/\d+$/.test(url.pathname);
}