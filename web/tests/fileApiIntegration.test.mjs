import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const fileRoot = new URL("../src/app/(protected)/file/", import.meta.url);

function readFileArea(path) {
  return readFileSync(new URL(path, fileRoot), "utf8");
}

test("file list uses the real /files PageResponse API instead of worklog mock data", () => {
  const page = readFileArea("page.tsx");
  const card = readFileArea("_components/fileCard.tsx");
  const service = readFileArea("_service/file.service.ts");
  const hook = readFileArea("_hooks/useFileList.ts");
  const hookIndex = readFileArea("_hooks/index.ts");
  const types = readFileArea("_types/file.types.ts");

  assert.match(service, /apiClient\.get<PageResponse<FileItem>>\("\/files"/);
  assert.match(service, /params/);
  assert.match(hook, /fileKeys/);
  assert.match(hook, /useFileList\(params: GetFilesParams = \{\}\)/);
  assert.match(hook, /queryFn:\s*\(\) => fileService\.getFiles\(params\)/);
  assert.match(hookIndex, /export \* from "\.\/useFileList"/);

  assert.match(types, /export interface FileItem/);
  assert.match(types, /worklog:\s*FileWorklogSummary \| null/);
  assert.doesNotMatch(types, /FileRecord/);

  assert.match(page, /useFileList\(fileListParams\)/);
  assert.match(page, /filePage\?\.items \?\? \[\]/);
  assert.match(page, /totalCount/);
  assert.match(page, /<Pagination/);
  assert.match(page, /handleDownloadSelectedFiles/);
  assert.match(page, /fetch\(file\.storedPath/);
  assert.match(page, /response\.blob\(\)/);
  assert.match(page, /URL\.createObjectURL\(blob\)/);
  assert.match(page, /link\.href = objectUrl/);
  assert.match(page, /link\.download = file\.originalName/);
  assert.match(page, /URL\.revokeObjectURL\(objectUrl\)/);
  assert.match(page, /onClick=\{\(\) => void handleDownloadSelectedFiles\(\)\}/);
  assert.doesNotMatch(page, /worklog\/_mock\/worklog\.mock/);
  assert.doesNotMatch(page, /sampleFile/);

  assert.match(card, /file\.worklog/);
  assert.match(card, /file\.aiProcessingStatus/);
  assert.doesNotMatch(card, /worklog\/_mock\/worklog\.mock/);
});
