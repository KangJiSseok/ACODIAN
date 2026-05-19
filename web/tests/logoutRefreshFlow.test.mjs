import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const authStoreFile = readFileSync(
  new URL("../src/app/_common/store/auth.store.ts", import.meta.url),
  "utf8",
);
const protectedLayoutFile = readFileSync(
  new URL("../src/app/(protected)/layout.tsx", import.meta.url),
  "utf8",
);
const queryProviderFile = readFileSync(
  new URL("../src/app/_common/providers/queryProvider.tsx", import.meta.url),
  "utf8",
);

test("auth reset marks the session as unauthenticated instead of unknown idle", () => {
  assert.match(
    authStoreFile,
    /resetAuth:\s*\(\)\s*=>\s*set\(\{\s*accessToken:\s*null,\s*user:\s*null,\s*status:\s*"unauthenticated"/,
  );
});

test("protected layout does not call refresh after logout has confirmed unauthenticated status", () => {
  const unauthenticatedGuardIndex = protectedLayoutFile.indexOf(
    'status === "unauthenticated"',
  );
  const refreshCallIndex = protectedLayoutFile.indexOf("refreshSession()");

  assert.notEqual(unauthenticatedGuardIndex, -1);
  assert.notEqual(refreshCallIndex, -1);
  assert.ok(unauthenticatedGuardIndex < refreshCallIndex);
});

test("query cache is cleared when auth becomes unauthenticated", () => {
  assert.match(
    queryProviderFile,
    /useAuthStore\(\(state\)\s*=>\s*state\.status\)/,
  );
  assert.match(queryProviderFile, /useQueryClient\(\)/);
  assert.match(queryProviderFile, /status === "unauthenticated"/);
  assert.match(queryProviderFile, /queryClient\.clear\(\)/);
  assert.match(
    queryProviderFile,
    /<QueryClientProvider client=\{queryClient\}>[\s\S]*<ClearQueryCacheOnUnauthenticated \/>[\s\S]*\{children\}[\s\S]*<\/QueryClientProvider>/,
  );
});
