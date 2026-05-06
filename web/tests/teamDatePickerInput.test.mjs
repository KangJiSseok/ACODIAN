import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const teamForm = readFileSync(
  new URL("../src/app/(protected)/team/_components/teamForm.tsx", import.meta.url),
  "utf8",
);

test("team date inputs expose a calendar picker button", () => {
  assert.match(teamForm, /CalendarDays/);
  assert.match(teamForm, /showPicker/);
  assert.match(teamForm, /시작일 선택 달력 열기/);
  assert.match(teamForm, /종료 예정일 선택 달력 열기/);
});
