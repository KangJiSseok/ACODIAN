import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import test from "node:test";

function readSource(path) {
  return readFileSync(new URL(`../src/${path}`, import.meta.url), "utf8");
}

test("tag navigation exposes list and merge pages", () => {
  const sidebarConfig = readSource(
    "app/_common/components/layout/sidebar.config.ts",
  );
  const breadcrumbs = readSource("app/_common/service/breadcrumbs.ts");

  assert.match(sidebarConfig, /label:\s*"태그"/);
  assert.match(sidebarConfig, /submenus:\s*\[/);
  assert.match(sidebarConfig, /label:\s*"태그 목록",\s*href:\s*"\/tag"/);
  assert.match(sidebarConfig, /label:\s*"태그 병합",\s*href:\s*"\/tag\/merge"/);
  assert.match(breadcrumbs, /findNavSubItemByHref\(pathname\)/);
});

test("tag merge page renders one mocked merge candidate", () => {
  const mergePagePath = new URL(
    "../src/app/(protected)/tag/merge/page.tsx",
    import.meta.url,
  );

  assert.equal(existsSync(mergePagePath), true);

  const page = readFileSync(mergePagePath, "utf8");
  const component = readSource(
    "app/(protected)/tag/_components/tagMergeList.tsx",
  );
  const service = readSource("app/(protected)/tag/_service/tag.service.ts");
  const hook = readSource("app/(protected)/tag/_hooks/useTagList.ts");
  const hookIndex = readSource("app/(protected)/tag/_hooks/index.ts");
  const types = readSource("app/(protected)/tag/_types/tag.types.ts");

  assert.match(page, /태그 병합/);
  assert.match(page, /<TagMergeList \/>/);
  assert.doesNotMatch(page, /HelpCircle/);
  assert.doesNotMatch(page, /태그 병합 안내/);

  assert.match(service, /mockTagMergeCandidates/);
  assert.match(service, /targetTagName:\s*"결산"/);
  assert.match(service, /sourceTagNames:\s*\["월말결산",\s*"결산업무"\]/);
  assert.match(service, /toTagMergeCandidates/);

  assert.match(hook, /mergeCandidates/);
  assert.match(hook, /useTagMergeCandidates/);
  assert.match(hookIndex, /export \{ useTagList, useTagMergeCandidates \}/);

  assert.match(types, /export interface TagMergeCandidate/);
  assert.match(types, /targetTagName: string/);
  assert.match(types, /sourceTagNames: string\[\]/);

  assert.match(component, /병합될 태그명/);
  assert.match(component, /합쳐질 태그들/);
  assert.match(component, /rounded-full/);
  assert.doesNotMatch(component, /CardSpotlight/);
  assert.match(component, /tags=\{\[candidate\.targetTagName\]\}/);
  assert.match(component, /#\{tag\}/);
  assert.match(component, /tags=\{sourceTagNames\}/);
  assert.match(component, /tags\.map/);
  assert.match(component, /Pencil/);
  assert.match(component, /GitMerge/);
  assert.match(component, /variant="secondary"[\s\S]*>\s*<Pencil/);
  assert.match(component, /variant="default"[\s\S]*>\s*<GitMerge/);
  assert.match(component, /h-11 px-5 text-sm font-semibold/);
  assert.match(component, />\s*수정\s*</);
  assert.match(component, />\s*병합\s*</);
  assert.match(component, /Dialog/);
  assert.match(component, /DialogTitle>\s*합쳐질 태그 수정\s*<\/DialogTitle/);
  assert.match(component, /useTagList/);
  assert.match(component, /tagSearchQuery/);
  assert.match(component, /onAddTag/);
  assert.match(component, /onRemoveTag/);
  assert.match(component, /<X className="size-3\.5" \/>/);
  assert.match(component, /태그 검색/);
  assert.match(component, /변경사항 반영/);
});
