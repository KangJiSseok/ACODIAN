import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const serviceFile = readFileSync(
  new URL(
    "../src/app/(protected)/worklog/_service/worklog.service.ts",
    import.meta.url,
  ),
  "utf8",
);

const hookFile = readFileSync(
  new URL(
    "../src/app/(protected)/worklog/_hooks/useWorklogList.ts",
    import.meta.url,
  ),
  "utf8",
);

const detailPageFile = readFileSync(
  new URL(
    "../src/app/(protected)/worklog/detail/[id]/page.tsx",
    import.meta.url,
  ),
  "utf8",
);

const transitionFile = readFileSync(
  new URL(
    "../src/app/(protected)/worklog/_components/statusTransition.tsx",
    import.meta.url,
  ),
  "utf8",
);

test("worklog status transition service uses the dedicated PATCH endpoint", () => {
  assert.match(
    serviceFile,
    /async transitionStatus\([\s\S]*statusCode:\s*worklogStatusApiCodeMap\[nextStatus\]\s*\?\?\s*"PENDING"/,
  );
  assert.match(serviceFile, /apiClient\.patch</);
  assert.match(serviceFile, /EmptyResponse/);
  assert.match(serviceFile, /reason: string \| null/);
  assert.match(serviceFile, /`\/worklogs\/\$\{id\}\/status`/);
  assert.doesNotMatch(serviceFile, /notifyMockDb/);
  assert.doesNotMatch(serviceFile, /getNextStatusHistoryId/);
});

test("worklog query keys expose list and search prefixes for targeted invalidation", () => {
  assert.match(hookFile, /lists: \(\) => \[\.\.\.worklogKeys\.all, "list"\] as const/);
  assert.match(hookFile, /searches: \(\) => \[\.\.\.worklogKeys\.all, "search"\] as const/);
});

test("worklog detail page invalidates detail, list, and search queries after status change", () => {
  assert.match(detailPageFile, /useMutation\(/);
  assert.match(
    detailPageFile,
    /mutationFn: \(\{ nextStatus, reason \}[\s\S]*\) =>\s*worklogService\.transitionStatus\(worklogId, nextStatus, reason\)/,
  );
  assert.match(detailPageFile, /worklogKeys\.detail\(selectedWorklog\.id\)/);
  assert.match(detailPageFile, /worklogKeys\.lists\(\)/);
  assert.match(detailPageFile, /worklogKeys\.searches\(\)/);
  assert.match(detailPageFile, /getApiErrorMessage\(/);
  assert.doesNotMatch(detailPageFile, /setDisplayWorklog/);
  assert.doesNotMatch(detailPageFile, /team\.isLeader/);
});

test("status transition UI follows server policy and keeps final states closed", () => {
  assert.match(transitionFile, /PENDING: \["IN_PROGRESS"\]/);
  assert.match(transitionFile, /IN_PROGRESS: \["DONE", "ON_HOLD", "CANCELLED"\]/);
  assert.match(transitionFile, /ON_HOLD: \["IN_PROGRESS"\]/);
  assert.match(transitionFile, /DONE: \[\]/);
  assert.match(transitionFile, /FAILED: \[\]/);
  assert.match(transitionFile, /CANCELLED: \[\]/);
  assert.doesNotMatch(transitionFile, /DONE: \["IN_PROGRESS"\]/);
});
