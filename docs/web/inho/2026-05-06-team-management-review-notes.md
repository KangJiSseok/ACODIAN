# 팀 관리 화면 구현 및 리뷰 내용 정리

- 작성일: 2026-05-06
- 파일명: `2026-05-06-team-management-review-notes.md`
- 대상 파일:
  - `web/src/app/(protected)/team/page.tsx`
  - `web/src/app/(protected)/team/_components/teamForm.tsx`
  - `web/src/app/(protected)/team/_components/teamDetail.tsx`
  - `web/src/app/(protected)/team/_hooks/*`
  - `web/src/app/(protected)/team/_service/team.service.ts`
  - `web/src/app/(protected)/team/_types/team.types.ts`

## 목적

팀 관리 화면을 만들면서 헷갈렸던 내용과 코드 리뷰에서 나온 핵심 논의를 정리한다.

이번 작업은 조직 탭의 팀 관리 기능을 프론트에서 연결하는 작업이었다.

- 팀 목록
- 팀 상세
- 팀 등록
- 팀 수정
- 팀 구성원 선택
- 팀장 지정
- 팀 상태 요약 표시

## 전체 구현 흐름

팀 관리 화면은 다음 순서로 구성했다.

1. 백엔드 응답에 맞는 타입 정의
2. 팀 API service 추가
3. React Query hook 분리
4. 팀 목록 화면 구현
5. 팀 등록/수정 공통 form 구현
6. 팀 상세 화면 구현
7. 리뷰 피드백 반영

## PageResponse 타입

`/teams` API는 페이지네이션 응답을 내려준다.

그래서 공통 타입에 `PageResponse<T>`를 추가했다.

```ts
export interface PageResponse<T> {
  items: T[];
  page: number;
  pageSize: number;
  totalCount: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}
```

여기서 중요한 값은 `items`와 `totalCount`다.

- `items`: 현재 요청한 페이지에 들어있는 데이터
- `totalCount`: 전체 데이터 개수

따라서 전체 팀 개수가 필요할 때 `items.length`를 쓰면 안 된다. 페이지네이션이 걸려 있으면 현재 페이지 개수만 알 수 있기 때문이다.

## 팀 요약 카운트

팀 목록 상단에는 다음 개수가 표시된다.

- 전체 팀
- 활성화된 팀
- 비활성화된 팀

처음에는 `summary`가 없을 때 현재 로드된 `teams` 배열로 fallback 계산을 했다.

```ts
active: summary?.activeTeamCount ?? teams.filter(...).length
```

하지만 이 방식은 정확하지 않을 수 있다.

예를 들어 실제 팀이 150개인데 `/teams?page=1&pageSize=100`으로 100개만 받았다면, `teams.filter(...)`는 전체 150개 기준이 아니라 현재 받은 100개 기준이다.

정리하면 다음과 같다.

- 전체 팀 개수: `/teams` 응답의 `totalCount`로 fallback 가능
- 활성 팀 개수: `/teams/summary` 없이는 전체 기준으로 알 수 없음
- 비활성 팀 개수: `/teams/summary` 없이는 전체 기준으로 알 수 없음

그래서 수정 후에는 `summary`가 없을 때 활성/비활성 개수를 임의 계산하지 않고 `-`로 표시한다.

```ts
const counts = {
  total: summary?.totalTeamCount ?? teamPage?.totalCount ?? null,
  active: summary?.activeTeamCount ?? null,
  inactive: summary?.inactiveTeamCount ?? null,
};
```

## 관리자 후보 필터링

팀 관리자는 정책상 본부장과 사업부장만 가능하다.

그래서 팀 등록/수정 form에서 관리자 후보를 다음 키워드 기준으로 필터링한다.

```ts
const TEAM_ADMIN_TITLE_KEYWORDS = ["본부장", "사업부장"] as const;
```

처음에는 컴포넌트 안에 `"본부장"`, `"부서장"`, `"이사"` 문자열이 직접 들어가 있었다.

리뷰 후 다음 이유로 상수로 분리했다.

- 정책성 값이라는 의도가 명확해진다.
- 이후 정책 변경 시 수정 위치를 찾기 쉽다.
- 잘못된 후보 직책이 섞이는 것을 줄일 수 있다.

추후 백엔드에서 관리자 후보 여부를 명확히 내려준다면, 프론트의 문자열 필터링보다 그 값을 기준으로 처리하는 편이 더 안정적이다.

예:

```ts
candidate.isTeamAdminCandidate
```

## 팀장 삭제 시 자동 재지정

처음 구현에서는 팀장을 팀원 목록에서 제거하면 남은 첫 번째 팀원을 자동으로 팀장으로 지정했다.

의도는 팀장 없는 팀이 저장되는 것을 막기 위한 방어 로직이었다.

하지만 이 방식은 사용자 경험상 애매하다.

- 사용자가 의도하지 않은 사람이 팀장으로 저장될 수 있다.
- 자동으로 바뀐 사실을 사용자가 놓칠 수 있다.
- 팀장은 중요한 값이므로 사용자가 직접 선택하는 편이 명확하다.

그래서 팀장을 삭제해도 자동 재지정하지 않도록 수정했다.

```ts
function removeMember(userId: number) {
  setMembers((current) => current.filter((member) => member.userId !== userId));
}
```

대신 저장 시점에 기존 검증 로직이 동작한다.

```ts
if (!normalizedMembers.some((member) => member.isLeader)) {
  setErrorMessage("팀장을 지정해주세요.");
  return;
}
```

즉, 팀장이 없으면 저장을 막고 사용자가 직접 새 팀장을 선택하게 한다.

## 팀원 목록 문구 정리

팀 상세 화면의 팀원 목록에서 다음 문구가 불필요하게 노출되고 있었다.

```text
팀 내 역할: 백엔드 개발
```

리뷰 후 라벨을 제거하고 값만 표시하도록 정리했다.

```tsx
{user.teamRole ?? "-"}
```

팀 카드에서도 `대표 소속` 문구를 `주 소속`으로 바꾸고, 운영 범위 하단의 `주담당`, `주 소속` 태그는 제거했다.

## 이번 작업에서 기억할 점

1. 페이지네이션 응답의 `items.length`는 전체 개수가 아니다.
2. 전체 개수는 가능하면 백엔드의 `totalCount`나 summary API를 사용한다.
3. 활성/비활성처럼 집계 기준이 필요한 값은 현재 페이지 데이터로 임의 계산하면 안 된다.
4. 정책성 문자열은 컴포넌트 내부에 직접 쓰기보다 상수로 분리한다.
5. 팀장처럼 중요한 값은 자동 변경보다 사용자의 명시적 선택이 안전하다.
6. 리뷰 코멘트는 무조건 반영하기보다 실제 API 응답과 정책을 확인한 뒤 반영한다.

## 검증

리뷰 반영 후 다음 명령을 실행했다.

```bash
pnpm lint:web
pnpm build:web
```

결과:

- `pnpm lint:web` 통과
- `pnpm build:web` 통과

## MR 답변 방향

리뷰 코멘트에는 다음처럼 짧게 답변할 수 있다.

### 관리자 후보 필터링

팀 관리자는 정책상 본부장/사업부장만 가능해서 해당 직책만 후보로 노출했습니다. 다만 컴포넌트 내부에 문자열이 직접 들어가 있어 유지보수성이 떨어질 수 있으니, 관리자 후보 직책 키워드를 상수로 분리했습니다.

### 팀장 자동 재지정

팀장을 삭제했을 때 첫 번째 팀원을 자동 지정하면 사용자가 의도하지 않은 팀장이 저장될 수 있어 자동 재지정 로직을 제거했습니다. 팀장이 없는 경우 저장 시 검증 메시지로 직접 선택하도록 유도합니다.

### summary fallback

summary가 없을 때 현재 로드된 `items` 기준으로 활성/비활성 개수를 계산하면 페이지네이션 상황에서 전체 기준 개수가 아닐 수 있습니다. 따라서 summary가 없을 때는 임의 계산하지 않고 `-`로 표시하도록 수정했습니다.
