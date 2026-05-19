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
  assert.match(service, /apiClient\.get<FileTypeOption\[\]>\("\/files\/types"\)/);
  assert.match(service, /params/);
  assert.match(hook, /fileKeys/);
  assert.match(hook, /useFileList\(params: GetFilesParams = \{\}\)/);
  assert.match(hook, /queryFn:\s*\(\) => fileService\.getFiles\(params\)/);
  assert.match(hook, /useFileTypes\(\)/);
  assert.match(hook, /queryFn:\s*\(\) => fileService\.getFileTypes\(\)/);
  assert.match(hookIndex, /export \* from "\.\/useFileList"/);

  assert.match(types, /export interface FileItem/);
  assert.match(types, /export interface FileTypeOption/);
  assert.match(types, /worklog:\s*FileWorklogSummary \| null/);
  assert.doesNotMatch(types, /FileRecord/);

  assert.match(page, /useFileList\(fileListParams\)/);
  assert.match(page, /useFileTypes\(\)/);
  assert.match(page, /fileTypes\.map/);
  assert.match(page, /fileType:\s*fileType === ALL_FILTER_VALUE \? undefined : fileType/);
  assert.match(page, /period:\s*toPeriodDays\(period\)/);
  assert.match(page, /filePage\?\.items \?\? \[\]/);
  assert.match(page, /<ResultCount\s+label="조회된 파일"\s+count=\{visibleFiles\.length\}\s+unit="개"/);
  assert.match(page, /<Pagination/);
  assert.match(page, /handleDownloadSelectedFiles/);
  assert.match(page, /fetch\(file\.storedPath/);
  assert.match(page, /response\.blob\(\)/);
  assert.match(page, /URL\.createObjectURL\(blob\)/);
  assert.match(page, /link\.href = objectUrl/);
  assert.match(page, /link\.download = file\.originalName/);
  assert.match(page, /URL\.revokeObjectURL\(objectUrl\)/);
  assert.match(page, /onClick=\{\(\) => void handleDownloadSelectedFiles\(\)\}/);
  assert.doesNotMatch(page, /\{ label: "PDF", value: "PDF" \}/);
  assert.doesNotMatch(page, /worklog\/_mock\/worklog\.mock/);
  assert.doesNotMatch(page, /sampleFile/);

  assert.match(card, /file\.worklog/);
  assert.match(card, /file\.aiProcessingStatus/);
  assert.doesNotMatch(card, /worklog\/_mock\/worklog\.mock/);
});

test("selected file download continues after failures and reports failed file names", () => {
  const page = readFileArea("page.tsx");

  assert.match(page, /const \[downloadResultMessage, setDownloadResultMessage\]/);
  assert.match(page, /const failedFiles: FileItem\[\] = \[\]/);
  assert.match(page, /successCount \+= 1/);
  assert.match(page, /failedFiles\.push\(file\)/);
  assert.match(page, /createDownloadResultMessage\(successCount, failedFiles\)/);
  assert.match(page, /failedFiles\.map\(\(file\) => file\.originalName\)/);
  assert.match(page, /downloadResultMessage\.tone === "error"/);
  assert.doesNotMatch(page, /파일 다운로드를 완료하지 못했습니다/);
});
