import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const globalsCss = readFileSync(new URL("../src/app/globals.css", import.meta.url), "utf8");
const selectComponent = readFileSync(
  new URL("../src/components/ui/select.tsx", import.meta.url),
  "utf8",
);
const departmentForm = readFileSync(
  new URL("../src/app/(protected)/department/_components/departmentForm.tsx", import.meta.url),
  "utf8",
);
const teamForm = readFileSync(
  new URL("../src/app/(protected)/team/_components/teamForm.tsx", import.meta.url),
  "utf8",
);
const teamMemberSearchDialog = readFileSync(
  new URL("../src/app/(protected)/team/_components/teamMemberSearchDialog.tsx", import.meta.url),
  "utf8",
);

test("native select options keep readable contrast in browser popup menus", () => {
  assert.match(globalsCss, /select\s+option\s*{[^}]*color:\s*#0f172a/i);
  assert.match(globalsCss, /select\s+option\s*{[^}]*background(?:-color)?:\s*#fff(?:fff)?/i);
});

test("native select popup menus use a stable light palette across themes", () => {
  assert.match(
    globalsCss,
    /select\s+option,\s*select\s+optgroup\s*{[^}]*color-scheme:\s*light/i,
  );
  assert.match(globalsCss, /select\s+option:checked\s*{[^}]*color:\s*#ffffff/i);
  assert.match(
    globalsCss,
    /select\s+option:checked\s*{[^}]*background(?:-color)?:\s*#2563eb/i,
  );
});

test("shared Select options do not force dark theme foreground into native popup menus", () => {
  assert.doesNotMatch(selectComponent, /option[^]*var\(--foreground\)/);
});

test("shared Select gives duplicate option values unique React keys", () => {
  assert.match(selectComponent, /options\.map\(\(option, index\) =>/);
  assert.match(selectComponent, /key=\{`\$\{option\.value\}-\$\{index\}`\}/);
});

test("organization forms use the same shared select sizing as worklog form", () => {
  for (const source of [departmentForm, teamForm, teamMemberSearchDialog]) {
    assert.match(source, /from "@\/components\/ui\/select"/);
    assert.match(source, /h-11 rounded-2xl px-4 text-sm/);
  }

  assert.doesNotMatch(teamForm, /<select/);
  assert.doesNotMatch(teamMemberSearchDialog, /<select/);
});
