import assert from "node:assert/strict";
import test from "node:test";
import { getTeamStatusLabel } from "../src/app/(protected)/team/_utils/teamStatus.utils.ts";

test("shows active and inactive team status labels", () => {
  assert.equal(getTeamStatusLabel("ACTIVE"), "활성");
  assert.equal(getTeamStatusLabel("INACTIVE"), "비활성");
});

test("keeps unknown team status label as-is", () => {
  assert.equal(getTeamStatusLabel("ARCHIVED"), "ARCHIVED");
});
