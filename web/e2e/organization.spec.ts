import { expect, test, type Response } from "@playwright/test";
import { loginAs } from "./_support/auth";
import {
  expectLoadedWithoutAppError,
  expectResultOrEmpty,
  waitForResponseOrNull,
} from "./_support/ui";

test.describe.configure({ mode: "serial" });

test.describe("organization - 실제 API 조직 관리", () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, "DIRECTOR");
  });

  test("부서 목록은 실제 서버의 목록 또는 빈 상태를 렌더링한다", async ({ page }) => {
    const departmentsResponsePromise = waitForResponseOrNull(
      page,
      isDepartmentsResponse,
    );

    await page.goto("/department");

    const departmentsResponse = await departmentsResponsePromise;
    test.skip(
      !departmentsResponse?.ok(),
      `현재 실제 API 부서 목록 응답을 받을 수 없습니다: ${departmentsResponse?.status() ?? "no response"}`,
    );

    await expectLoadedWithoutAppError(page);
    await expect(page.getByRole("heading", { name: "부서 관리" }).first()).toBeVisible();
    await expectResultOrEmpty(
      page.getByText(/조회된 부서 \d+개/),
      page.getByText(/표시할 부서가 없습니다|조회된 부서 0개/),
    );
  });

  test("부서 등록 화면은 필수값 검증을 수행한다", async ({ page }) => {
    const adminCandidatesResponsePromise = waitForResponseOrNull(
      page,
      isAdminCandidatesResponse,
    );

    await page.goto("/department/create");

    const adminCandidatesResponse = await adminCandidatesResponsePromise;
    test.skip(
      !adminCandidatesResponse?.ok(),
      `현재 실제 API 부서 관리자 후보 응답을 받을 수 없습니다: ${adminCandidatesResponse?.status() ?? "no response"}`,
    );

    await expectLoadedWithoutAppError(page);
    await expect(page.getByRole("heading", { name: "부서 등록" })).toBeVisible();
    await page.getByRole("button", { name: "부서 등록" }).click();
    expect(
      await page
        .getByLabel("부서명")
        .evaluate((input: HTMLInputElement) => input.validity.valueMissing),
    ).toBe(true);
  });

  test("팀 목록과 팀 등록 유효성 실패를 확인한다", async ({ page }) => {
    const teamsResponsePromise = waitForResponseOrNull(page, isTeamsResponse);
    const teamSummaryResponsePromise = waitForResponseOrNull(
      page,
      isTeamSummaryResponse,
    );

    await page.goto("/team");

    const teamsResponse = await teamsResponsePromise;
    test.skip(
      !teamsResponse?.ok(),
      `현재 실제 API 팀 목록 응답을 받을 수 없습니다: ${teamsResponse?.status() ?? "no response"}`,
    );
    const teamSummaryResponse = await teamSummaryResponsePromise;
    test.skip(
      !teamSummaryResponse?.ok(),
      `현재 실제 API 팀 요약 응답을 받을 수 없습니다: ${teamSummaryResponse?.status() ?? "no response"}`,
    );

    await expectLoadedWithoutAppError(page);
    await expect(page.getByRole("heading", { name: "팀 관리" }).first()).toBeVisible();
    await expectResultOrEmpty(
      page.getByText(/조회된 팀 \d+개/),
      page.getByText(/표시할 팀이 없습니다|조회된 팀 0개/),
    );

    const userCandidatesResponsePromise = waitForResponseOrNull(
      page,
      isTeamUserCandidatesResponse,
    );

    await page.goto("/team/create");

    const userCandidatesResponse = await userCandidatesResponsePromise;
    test.skip(
      !userCandidatesResponse?.ok(),
      `현재 실제 API 팀 사용자 후보 응답을 받을 수 없습니다: ${userCandidatesResponse?.status() ?? "no response"}`,
    );

    await expect(page.getByRole("heading", { name: "팀 등록" })).toBeVisible();
    await page.getByRole("button", { name: "팀 생성" }).click();
    await expect(page.getByText("팀명을 입력해주세요.")).toBeVisible();
  });

  test("MEMBER는 팀 생성 화면에서 관리자 전용 제출 액션을 사용할 수 없다", async ({ page }) => {
    await loginAs(page, "MEMBER");

    await page.goto("/team/create");

    await expectLoadedWithoutAppError(page);
    await expect(
      page.getByText(/팀 등록은 본부장과 사업부장만 사용할 수 있습니다|권한/).first(),
    ).toBeVisible();
    await expect(page.getByRole("button", { name: "팀 생성" })).toHaveCount(0);
  });
});

function isDepartmentsResponse(response: Response) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && url.pathname.endsWith("/departments");
}

function isAdminCandidatesResponse(response: Response) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && url.pathname.endsWith("/users/admin-candidates");
}

function isTeamsResponse(response: Response) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && url.pathname.endsWith("/teams");
}

function isTeamSummaryResponse(response: Response) {
  const url = new URL(response.url());

  return response.request().method() === "GET" && url.pathname.endsWith("/teams/summary");
}

function isTeamUserCandidatesResponse(response: Response) {
  const url = new URL(response.url());

  return (
    response.request().method() === "GET" &&
    url.pathname.endsWith("/users") &&
    url.searchParams.get("employmentStatus") === "ACTIVE"
  );
}
