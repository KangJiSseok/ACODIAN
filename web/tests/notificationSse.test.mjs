import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const streamServiceFile = readFileSync(
  new URL(
    "../src/app/(protected)/notification/_service/notification-stream.service.ts",
    import.meta.url,
  ),
  "utf8",
);

const streamHookFile = readFileSync(
  new URL(
    "../src/app/(protected)/notification/_hooks/useNotificationStream.ts",
    import.meta.url,
  ),
  "utf8",
);

const hookIndexFile = readFileSync(
  new URL(
    "../src/app/(protected)/notification/_hooks/index.ts",
    import.meta.url,
  ),
  "utf8",
);

const gnbFile = readFileSync(
  new URL("../src/app/_common/components/layout/gnb.tsx", import.meta.url),
  "utf8",
);

test("notification stream service connects to backend SSE with bearer auth", () => {
  assert.match(streamServiceFile, /\/notifications\/stream/);
  assert.match(streamServiceFile, /fetch\(resolveNotificationStreamUrl\(\)/);
  assert.match(streamServiceFile, /Accept:\s*"text\/event-stream"/);
  assert.match(streamServiceFile, /Authorization:\s*`Bearer \$\{accessToken\}`/);
  assert.match(streamServiceFile, /credentials:\s*"include"/);
  assert.match(streamServiceFile, /eventName !== "notification"/);
  assert.match(streamServiceFile, /JSON\.parse\(event\.data\)/);
});

test("notification stream hook invalidates notification queries on SSE message", () => {
  assert.match(streamHookFile, /selectAccessToken/);
  assert.match(streamHookFile, /selectIsAuthenticated/);
  assert.match(streamHookFile, /subscribeNotificationStream\(/);
  assert.match(streamHookFile, /invalidateQueries\(\{\s*queryKey:\s*notificationKeys\.all\s*\}\)/);
  assert.match(streamHookFile, /subscription\.close\(\)/);
});

test("global navigation opens notification stream subscription", () => {
  assert.match(hookIndexFile, /useNotificationStream/);
  assert.match(gnbFile, /useNotificationStream/);
  assert.match(gnbFile, /useNotificationStream\(\);/);
});
