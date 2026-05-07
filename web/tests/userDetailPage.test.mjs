import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const userRoot = new URL("../src/app/(protected)/user/", import.meta.url);

function readUserFile(path) {
  return readFileSync(new URL(path, userRoot), "utf8");
}

test("사용자 상세 조회 API를 서비스와 훅에 연결한다", () => {
  const service = readUserFile("_service/user.service.ts");
  const hooks = readUserFile("_hooks/useUserDetail.ts");
  const hookIndex = readUserFile("_hooks/index.ts");
  const types = readUserFile("_types/user.types.ts");

  assert.match(service, /getUser:\s*\(userId: number\)/);
  assert.match(service, /`\/users\/\$\{userId\}`/);
  assert.match(service, /getUserSkills:\s*\(userId: number\)/);
  assert.match(service, /`\/users\/\$\{userId\}\/skills`/);
  assert.match(service, /getUserEvaluations:\s*\(userId: number\)/);
  assert.match(service, /`\/users\/\$\{userId\}\/evaluations`/);

  assert.match(hooks, /useUserDetail\(userId: number\)/);
  assert.match(hooks, /useUserSkills\(userId: number\)/);
  assert.match(hooks, /useUserEvaluations\(userId: number\)/);
  assert.match(hookIndex, /export \* from "\.\/useUserDetail"/);

  assert.match(types, /interface UserDetail/);
  assert.match(types, /interface UserTeamSummary/);
  assert.match(types, /interface UserSkillSummary/);
  assert.match(types, /interface UserEvaluationSummary/);
});

test("사용자 상세 화면을 사용자 정보, 스킬 설정, 관리자 평가 탭으로 보여준다", () => {
  const page = readUserFile("detail/[id]/page.tsx");
  const component = readUserFile("_components/userDetail.tsx");

  assert.match(page, /useUserDetail\(userId\)/);
  assert.match(page, /useUserSkills\(userId\)/);
  assert.match(page, /useUserEvaluations\(userId\)/);
  assert.match(page, /<UserDetail/);

  assert.match(component, /사용자 정보/);
  assert.match(component, /스킬 설정/);
  assert.match(component, /관리자 평가/);
  assert.match(component, /수정 저장/);
  assert.match(component, /스킬 추가/);
  assert.match(component, /등록된 평가가 없습니다\./);
  assert.match(component, /disabled/);
});

test("사용자 목록 카드는 별도 상세 보기 버튼 없이 카드 클릭으로 상세 이동한다", () => {
  const page = readUserFile("page.tsx");

  assert.match(page, /href=\{`\/user\/detail\/\$\{user\.userId\}`\}/);
  assert.doesNotMatch(page, /상세 보기/);
});

test("사용자 목록 검색 필터는 파일 탭과 같은 컨트롤 패턴을 사용한다", () => {
  const page = readUserFile("page.tsx");

  assert.match(page, /aria-label="사용자 검색"/);
  assert.match(page, /className="h-12 pl-11"/);
  assert.match(page, /variant="outline"[\s\S]*className="h-10"/);
  assert.match(page, /showFilters && "rotate-180"/);
  assert.match(page, /transition-\[grid-template-rows,opacity,margin\]/);
  assert.match(page, /text-\[11px\] font-bold uppercase tracking-\[0\.16em\]/);
  assert.doesNotMatch(page, /ChevronUp/);
  assert.doesNotMatch(page, /h-14 rounded-2xl pl-12/);
});

test("사용자 목록은 백엔드 PageResponse와 query params로 페이지네이션한다", () => {
  const page = readUserFile("page.tsx");
  const service = readUserFile("_service/user.service.ts");
  const types = readUserFile("_types/user.types.ts");

  assert.match(types, /page\?: number/);
  assert.match(types, /pageSize\?: number/);
  assert.match(service, /apiClient\.get<PageResponse<UserSummary>>\("\/users"/);
  assert.doesNotMatch(service, /apiClient\.get<UserSummary\[]>/);
  assert.match(page, /const \[page, setPage\] = useState\(1\)/);
  assert.match(page, /useUserList\(userListParams\)/);
  assert.match(page, /userPage\?\.items \?\? \[\]/);
  assert.match(page, /totalPages=\{userPage\?\.totalPages \?\? 1\}/);
  assert.doesNotMatch(page, /usePagination/);
});

test("팀 사용자 후보 조회도 사용자 PageResponse의 items를 사용한다", () => {
  const service = readFileSync(
    new URL("../src/app/(protected)/team/_service/team.service.ts", import.meta.url),
    "utf8",
  );

  assert.match(service, /apiClient\.get<PageResponse<TeamUserCandidate>>\("\/users"/);
  assert.match(service, /return page\.items/);
});
