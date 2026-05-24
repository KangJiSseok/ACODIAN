import { expect, type Locator, type Page, type Response } from "@playwright/test";

export async function expectLoadedWithoutAppError(page: Page) {
  await expect(page.getByText("로그인 상태 확인 중...")).toHaveCount(0, {
    timeout: 15_000,
  });
  await expect(page.getByText(/문제가 발생했습니다|오류가 발생했습니다/)).toHaveCount(0);
}

export async function expectResultOrEmpty(
  resultLocator: Locator,
  emptyLocator: Locator,
) {
  await expect(resultLocator.or(emptyLocator).first()).toBeVisible({
    timeout: 15_000,
  });
}

export async function countVisible(locator: Locator) {
  return locator.evaluateAll((elements) =>
    elements.filter((element) => {
      const style = window.getComputedStyle(element);
      const rect = element.getBoundingClientRect();
      return (
        style.visibility !== "hidden" &&
        style.display !== "none" &&
        rect.width > 0 &&
        rect.height > 0
      );
    }).length,
  );
}

export async function waitForResponseOrNull(
  page: Page,
  predicate: (response: Response) => boolean,
  timeout = 15_000,
) {
  return page.waitForResponse(predicate, { timeout }).catch(() => null);
}
