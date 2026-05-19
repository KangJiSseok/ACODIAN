import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const worklogPage = readFileSync(
  new URL("../src/app/(protected)/worklog/page.tsx", import.meta.url),
  "utf8",
);

const worklogForm = readFileSync(
  new URL(
    "../src/app/(protected)/worklog/_components/worklogForm.tsx",
    import.meta.url,
  ),
  "utf8",
);

const worklogFileUpload = readFileSync(
  new URL(
    "../src/app/(protected)/worklog/_components/worklogFileUpload.tsx",
    import.meta.url,
  ),
  "utf8",
);

test("worklog form limits match current API validation contract", () => {
  assert.match(worklogForm, /const TITLE_MAX_LENGTH = 50/);
  assert.match(worklogForm, /const CONTENT_MAX_LENGTH = 10000/);
  assert.doesNotMatch(worklogForm, /const TITLE_MAX_LENGTH = 100/);
  assert.doesNotMatch(worklogForm, /const CONTENT_MAX_LENGTH = 2000/);
});

test("worklog list keyword copy describes title-only fallback search", () => {
  assert.match(worklogPage, /업무 제목을 검색하고 팀, 상태, 중요도/);
  assert.match(worklogPage, /"업무 제목으로 검색하세요"/);
  assert.doesNotMatch(worklogPage, /업무 제목과 내용을 검색/);
  assert.doesNotMatch(worklogPage, /업무 제목 또는 내용으로 검색하세요/);
});

test("worklog ai entry point uses mode copy", () => {
  assert.match(worklogPage, /AI 모드/);
  assert.match(worklogPage, /aria-label="AI 모드 질문 전송"/);
  assert.doesNotMatch(worklogPage, /AI 검색/);
});

test("worklog create action is aligned with the visible worklog count", () => {
  assert.match(worklogPage, /표시 중인 업무[\s\S]*canCreate \? \(/);
  assert.match(worklogPage, /표시 중인 업무[\s\S]*href="\/worklog\/create"[\s\S]*업무 등록/);
  assert.doesNotMatch(worklogPage, /<PageHeader[\s\S]*actions=\{/);
});

test("worklog file upload avoids nested interactive controls in the dropzone", () => {
  assert.doesNotMatch(worklogFileUpload, /role="button"/);
  assert.doesNotMatch(worklogFileUpload, /tabIndex=\{0\}/);
  assert.match(worklogFileUpload, /<Button[\s\S]*파일 선택/);
});
