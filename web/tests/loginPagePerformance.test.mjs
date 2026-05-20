import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const loginPageFile = readFileSync(
  new URL("../src/app/(public)/login/page.tsx", import.meta.url),
  "utf8",
);
const loginClientFile = readFileSync(
  new URL("../src/app/(public)/login/loginClient.tsx", import.meta.url),
  "utf8",
);

test("login page reads redirect on the server without empty suspense fallback", () => {
  assert.doesNotMatch(loginPageFile, /"use client"/);
  assert.match(loginPageFile, /searchParams\?: Promise/);
  assert.match(loginPageFile, /<LoginClient redirectPath=\{redirectPath\} \/>/);
  assert.doesNotMatch(loginClientFile, /useSearchParams/);
  assert.doesNotMatch(loginClientFile, /fallback=\{null\}/);
});

test("login video avoids eager fallback downloads", () => {
  assert.match(loginClientFile, /preload="none"/);
  assert.match(loginClientFile, /login-bg-poster\.webp/);
  assert.match(loginClientFile, /login-bg\.webm/);
  assert.doesNotMatch(loginClientFile, /login-bg\.av1\.webm/);
  assert.doesNotMatch(loginClientFile, /codecs="av01/);
  assert.doesNotMatch(loginClientFile, /login-bg\.mp4/);
  assert.doesNotMatch(loginClientFile, /login-bg-poster\.jpg/);
});

test("login form does not expose test account defaults", () => {
  assert.match(loginClientFile, /const \[email, setEmail\] = useState\(""\)/);
  assert.match(loginClientFile, /const \[password, setPassword\] = useState\(""\)/);
  assert.match(loginClientFile, /placeholder="이메일"/);
  assert.match(loginClientFile, /placeholder="비밀번호"/);
  assert.doesNotMatch(loginClientFile, /director@ibank\.local/);
  assert.doesNotMatch(loginClientFile, /password1!/);
});
