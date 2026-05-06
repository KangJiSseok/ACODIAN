import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const globalsCss = readFileSync(new URL("../src/app/globals.css", import.meta.url), "utf8");
const selectComponent = readFileSync(
  new URL("../src/components/ui/select.tsx", import.meta.url),
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
