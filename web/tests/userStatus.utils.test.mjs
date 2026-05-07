import assert from "node:assert/strict";
import test from "node:test";
import {
  getEmploymentStatusBadgeVariant,
  getEmploymentStatusLabel,
} from "../src/app/(protected)/user/_utils/userStatus.utils.ts";

test("재직 상태 코드를 한글 라벨로 변환한다", () => {
  assert.equal(getEmploymentStatusLabel("ACTIVE"), "재직");
  assert.equal(getEmploymentStatusLabel("LEAVE"), "휴직");
});

test("알 수 없는 재직 상태는 원본 값을 유지한다", () => {
  assert.equal(getEmploymentStatusLabel("RETIRED"), "RETIRED");
  assert.equal(getEmploymentStatusLabel("UNKNOWN"), "UNKNOWN");
  assert.equal(getEmploymentStatusLabel(null), "-");
});

test("재직 상태별 badge variant를 반환한다", () => {
  assert.equal(getEmploymentStatusBadgeVariant("ACTIVE"), "success");
  assert.equal(getEmploymentStatusBadgeVariant("LEAVE"), "warning");
  assert.equal(getEmploymentStatusBadgeVariant("RETIRED"), "outline");
  assert.equal(getEmploymentStatusBadgeVariant("UNKNOWN"), "outline");
});
