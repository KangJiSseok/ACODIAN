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
});

test("사용자 상세 화면은 부서, 직급, 직책, 상태만 바로 수정할 수 있다", () => {
  const service = readUserFile("_service/user.service.ts");
  const mutation = readUserFile("_hooks/useUserMutation.ts");
  const component = readUserFile("_components/userDetail.tsx");

  assert.match(service, /updateUser:\s*\(userId: number, payload: UpdateUserPayload\)/);
  assert.match(service, /apiClient\.patch<EmptyResponse, FormData>\(`\/users\/\$\{userId\}`/);
  assert.match(mutation, /useUpdateUser\(userId: number\)/);
  assert.match(component, /<form className="space-y-8" onSubmit=\{handleSubmit\}>/);
  assert.match(component, /name="departmentId"/);
  assert.match(component, /name="positionName"/);
  assert.match(component, /name="titleName"/);
  assert.match(component, /name="employmentStatus"/);
  assert.doesNotMatch(component, /name="userName"/);
  assert.doesNotMatch(component, /name="email"/);
  assert.doesNotMatch(component, /name="profileImage"/);
  assert.doesNotMatch(component, /name="phone"/);
  assert.doesNotMatch(component, /name="joinDate"/);
  assert.doesNotMatch(component, /name="primaryTeamId"/);
  assert.doesNotMatch(component, /주소속팀/);
  assert.doesNotMatch(component, /title="사용자 수정 기능은 다음 단계에서 연결합니다\."/);
});

test("사용자 상세 직급과 직책은 표준 옵션 select로 수정한다", () => {
  const component = readUserFile("_components/userDetail.tsx");
  const options = readUserFile("_utils/userSelectOptions.ts");

  assert.match(options, /export const positionOptions: SelectOption\[\] = \[/);
  assert.match(options, /value: "사원"[\s\S]*value: "이사"/);
  assert.match(options, /export const titleOptions: SelectOption\[\] = \[/);
  assert.match(options, /value: "본부장"[\s\S]*value: "팀장"[\s\S]*value: "팀원"/);
  assert.match(component, /from "\.\.\/_utils\/userSelectOptions"/);
  assert.doesNotMatch(component, /const positionOptions: SelectOption\[\] = \[/);
  assert.doesNotMatch(component, /const titleOptions: SelectOption\[\] = \[/);
  assert.match(component, /buildOptionsWithCurrentValue\(positionOptions, values\.positionName\)/);
  assert.match(component, /buildOptionsWithCurrentValue\(titleOptions, values\.titleName\)/);
  assert.match(component, /<SelectField[\s\S]*label="직급"[\s\S]*name="positionName"[\s\S]*options=\{userPositionOptions\}/);
  assert.match(component, /<SelectField[\s\S]*label="직책"[\s\S]*name="titleName"[\s\S]*options=\{userTitleOptions\}/);
  assert.doesNotMatch(component, /<EditableField[\s\S]*name="positionName"/);
  assert.doesNotMatch(component, /<EditableField[\s\S]*name="titleName"/);
});

test("사용자 상세 스킬과 관리자 평가는 탭 안에서 추가할 수 있다", () => {
  const service = readUserFile("_service/user.service.ts");
  const mutation = readUserFile("_hooks/useUserMutation.ts");
  const types = readUserFile("_types/user.types.ts");
  const component = readUserFile("_components/userDetail.tsx");

  assert.match(types, /interface CreateUserSkillPayload/);
  assert.match(types, /interface CreateUserEvaluationPayload/);
  assert.match(service, /createUserSkill:\s*\(userId: number, payload: CreateUserSkillPayload\)/);
  assert.match(service, /apiClient\.post<EmptyResponse, CreateUserSkillPayload>\(`\/users\/\$\{userId\}\/skills`/);
  assert.match(service, /createUserEvaluation:\s*\([\s\S]*userId: number,[\s\S]*payload: CreateUserEvaluationPayload/);
  assert.match(service, /apiClient\.post<EmptyResponse, CreateUserEvaluationPayload>\(`\/users\/\$\{userId\}\/evaluations`/);
  assert.match(mutation, /useCreateUserSkill\(userId: number\)/);
  assert.match(mutation, /useCreateUserEvaluation\(userId: number\)/);
  assert.match(component, /<SkillsTab\s+userId=\{user\.userId\}/);
  assert.match(component, /<EvaluationsTab\s+userId=\{user\.userId\}/);
  assert.match(component, /name="skillName"/);
  assert.match(component, /name="skillLevel"/);
  assert.match(component, /name="content"/);
  assert.match(component, /스킬 저장/);
  assert.match(component, /평가 저장/);
  assert.match(component, /setIsFormOpen\(\(prev\) => !prev\)[\s\S]*스킬 추가/);
  assert.match(component, /setIsFormOpen\(\(prev\) => !prev\)[\s\S]*평가 작성/);
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
  assert.match(page, /variant="outline"[\s\S]*className="h-12 justify-center"/);
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
  const authStore = readFileSync(
    new URL("../src/app/_common/store/auth.store.ts", import.meta.url),
    "utf8",
  );

  assert.match(types, /page\?: number/);
  assert.match(types, /pageSize\?: number/);
  assert.match(types, /departmentId\?: number \| null/);
  assert.match(authStore, /departmentId: number \| null/);
  assert.match(service, /apiClient\.get<PageResponse<UserSummary>>\("\/users"/);
  assert.doesNotMatch(service, /apiClient\.get<UserSummary\[]>/);
  assert.match(page, /useAuthStore\(selectAuthUser\)/);
  assert.match(page, /resolveUserListDepartmentId\([\s\S]*currentUser,[\s\S]*departmentId,[\s\S]*ALL_FILTER_VALUE/);
  assert.match(page, /departmentId: scopedDepartmentId/);
  assert.match(page, /resolveUserListDepartmentId\([\s\S]*currentUser,[\s\S]*ALL_FILTER_VALUE,[\s\S]*ALL_FILTER_VALUE/);
  assert.match(page, /canUseDepartmentFilter \? \(/);
  assert.match(page, /const \[page, setPage\] = useState\(1\)/);
  assert.match(page, /useUserList\(userListParams\)/);
  assert.match(page, /dedupeUsersById\(userPage\?\.items \?\? \[\]\)/);
  assert.match(page, /function dedupeUsersById\(users: UserSummary\[\]\)/);
  assert.match(page, /seenUserIds/);
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
