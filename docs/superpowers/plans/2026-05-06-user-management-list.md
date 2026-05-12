# 사용자 관리 목록 구현 계획

> **작업자 필수 안내:** 이 계획을 실행할 때는 `superpowers:executing-plans` 방식으로 체크박스를 하나씩 완료한다. 커밋은 사용자가 명시적으로 요청한 경우에만 수행한다.

**목표:** `/user` 사용자 관리 목록을 실제 API 데이터 기반 화면으로 구현하고, 목록 카드 클릭으로 `/user/detail/[id]` 상세 화면에 진입하도록 만든다.

**아키텍처:** 변경 범위는 `web/src/app/(protected)/user` 아래로 제한한다. `apiClient`를 감싼 얇은 service, TanStack Query hook, 목록/상세 UI 컴포넌트, 상태/필터 유틸로 책임을 나눈다. 백엔드 코드는 수정하지 않는다.

**기술 스택:** Next.js App Router, React, TypeScript, TanStack Query, 기존 AX-WMS UI 컴포넌트, Node test runner, ESLint, Next build.

---

## 1. 파일 구조

수정할 파일:

- `web/src/app/(protected)/user/page.tsx`
- `web/src/app/(protected)/user/detail/[id]/page.tsx`
- `web/src/app/(protected)/user/_components/userList.tsx`
- `web/src/app/(protected)/user/_components/userDetail.tsx`
- `web/src/app/(protected)/user/_hooks/index.ts`
- `web/src/app/(protected)/user/_hooks/useUserList.ts`
- `web/src/app/(protected)/user/_hooks/useUserDetail.ts`
- `web/src/app/(protected)/user/_service/user.service.ts`
- `web/src/app/(protected)/user/_types/user.types.ts`

새로 만들 파일:

- `web/src/app/(protected)/user/_utils/userStatus.utils.ts`
- `web/src/app/(protected)/user/_utils/userFilter.utils.ts`
- `web/tests/userManagementList.test.mjs`
- `web/tests/userStatus.utils.test.mjs`
- `web/tests/userFilter.utils.test.mjs`

수정하지 않을 파일:

- `api/**`
- `ai/**`
- `infra/**`

---

## 2. 구현 기준

### `/user` 목록

- 현재 `ScaffoldPage` 자리표시자를 제거한다.
- 와이어프레임 `https://wms-wire-frame.vercel.app/user`와 비슷한 어두운 운영 도구 스타일을 따른다.
- 상단 섹션 제목은 `사용자 탐색`으로 둔다.
- 검색 input placeholder는 `이름, 이메일, 부서, 직급으로 검색하세요`로 둔다.
- `필터` 버튼을 누르면 필터 패널이 펼쳐진다.
- 필터는 다음 값을 다룬다.
  - `userName`
  - `departmentId`
  - `positionName`
  - `employmentStatus`
- 사용자 목록은 가로형 카드로 보여준다.
- 카드 안에는 다음 정보를 표시한다.
  - 프로필 이미지 또는 이름 첫 글자 아바타
  - 사용자 이름
  - 직급
  - 재직 상태
  - 이메일
  - 연락처
  - 부서
  - 주 소속 팀
- 목록 내부에는 `상세`, `수정`, `삭제` 버튼을 두지 않는다.
- 카드 전체를 클릭하면 `/user/detail/${user.userId}`로 이동한다.

### `/user/detail/[id]` 상세

- 현재 `ScaffoldPage` 자리표시자를 제거한다.
- `GET /users/{id}`로 사용자 상세 정보를 조회한다.
- 상세 화면 상단에는 `사용자 목록`으로 돌아가는 버튼을 둔다.
- 상세 화면 내부에 `수정`, `삭제` 버튼을 둔다.
- `수정` 버튼은 `/user/edit/${user.userId}`로 이동한다.
- `삭제` 버튼은 이번 범위에서 실제 API를 연결하지 않고 `disabled` 처리한다.
- 상세에는 기본 정보와 소속 팀 목록을 표시한다.

### 상태 라벨

- `ACTIVE` → `재직`
- `LEAVE` → `휴직`
- 알 수 없는 값 → 원본 값
- 값이 없으면 `-`

---

## 3. 작업 순서

### 작업 1: 사용자 상태 유틸 추가

파일:

- 생성: `web/tests/userStatus.utils.test.mjs`
- 생성: `web/src/app/(protected)/user/_utils/userStatus.utils.ts`

단계:

- [ ] `web/tests/userStatus.utils.test.mjs`를 작성한다.

```js
import assert from "node:assert/strict";
import test from "node:test";
import {
  getEmploymentStatusBadgeVariant,
  getEmploymentStatusLabel,
} from "../src/app/(protected)/user/_utils/userStatus.utils.ts";

test("재직 상태 코드를 한글 라벨로 변환한다", () => {
  assert.equal(getEmploymentStatusLabel("ACTIVE"), "재직");
  assert.equal(getEmploymentStatusLabel("LEAVE"), "휴직");
});

test("알 수 없는 재직 상태는 원본 값을 유지한다", () => {
  assert.equal(getEmploymentStatusLabel("RETIRED"), "RETIRED");
  assert.equal(getEmploymentStatusLabel("UNKNOWN"), "UNKNOWN");
  assert.equal(getEmploymentStatusLabel(null), "-");
});

test("재직 상태별 badge variant를 반환한다", () => {
  assert.equal(getEmploymentStatusBadgeVariant("ACTIVE"), "success");
  assert.equal(getEmploymentStatusBadgeVariant("LEAVE"), "warning");
  assert.equal(getEmploymentStatusBadgeVariant("RETIRED"), "outline");
  assert.equal(getEmploymentStatusBadgeVariant("UNKNOWN"), "outline");
});
```

- [ ] 실패 확인 명령을 실행한다.

```bash
node --test --experimental-strip-types web/tests/userStatus.utils.test.mjs
```

예상 결과: `userStatus.utils.ts`가 없어서 실패한다.

- [ ] `web/src/app/(protected)/user/_utils/userStatus.utils.ts`를 구현한다.

```ts
import type { BadgeProps } from "@/components/ui/badge";

export type EmploymentStatusCode = "ACTIVE" | "LEAVE" | string;

export function getEmploymentStatusLabel(
  status: EmploymentStatusCode | null | undefined,
) {
  if (!status) {
    return "-";
  }

  const labels: Record<string, string> = {
    ACTIVE: "재직",
    LEAVE: "휴직",
  };

  return labels[status] ?? status;
}

export function getEmploymentStatusBadgeVariant(
  status: EmploymentStatusCode | null | undefined,
): NonNullable<BadgeProps["variant"]> {
  if (status === "ACTIVE") {
    return "success";
  }

  if (status === "LEAVE") {
    return "warning";
  }

  return "outline";
}
```

- [ ] 테스트 통과를 확인한다.

```bash
node --test --experimental-strip-types web/tests/userStatus.utils.test.mjs
```

예상 결과: 3개 테스트 통과.

---

### 작업 2: 사용자 필터 유틸 추가

파일:

- 생성: `web/tests/userFilter.utils.test.mjs`
- 생성: `web/src/app/(protected)/user/_utils/userFilter.utils.ts`

단계:

- [ ] `buildUserListParams`와 `getUserFilterOptions` 테스트를 작성한다.

검증 내용:

- 빈 검색어, `all`, 빈 문자열은 query에서 제거된다.
- 검색어는 trim 처리되어 `userName`으로 전달된다.
- `departmentId`는 문자열 선택값에서 숫자로 변환된다.
- 부서 옵션은 `전체 부서`를 첫 항목으로 가진다.
- 직급 옵션은 `전체 직급`을 첫 항목으로 가진다.
- 중복 부서와 중복 직급은 한 번만 나온다.

- [ ] 실패 확인 명령을 실행한다.

```bash
node --test --experimental-strip-types web/tests/userFilter.utils.test.mjs
```

예상 결과: `userFilter.utils.ts`가 없어서 실패한다.

- [ ] `web/src/app/(protected)/user/_utils/userFilter.utils.ts`를 구현한다.

핵심 구현:

```ts
import type {
  GetUsersParams,
  UserFilterState,
  UserSummary,
} from "../_types/user.types";

export function buildUserListParams(filters: UserFilterState): GetUsersParams {
  const params: GetUsersParams = {};
  const keyword = filters.keyword.trim();

  if (keyword) {
    params.userName = keyword;
  }

  if (filters.departmentId !== "all") {
    params.departmentId = Number(filters.departmentId);
  }

  if (filters.positionName.trim() && filters.positionName !== "all") {
    params.positionName = filters.positionName;
  }

  if (filters.employmentStatus !== "all") {
    params.employmentStatus = filters.employmentStatus;
  }

  return params;
}

export function getUserFilterOptions(users: UserSummary[]) {
  const departments = new Map<string, string>();
  const positions: string[] = [];

  for (const user of users) {
    if (user.departmentId && user.departmentName) {
      departments.set(String(user.departmentId), user.departmentName);
    }

    if (user.positionName && !positions.includes(user.positionName)) {
      positions.push(user.positionName);
    }
  }

  return {
    departments: [
      { label: "전체 부서", value: "all" },
      ...Array.from(departments.entries()).map(([value, label]) => ({
        label,
        value,
      })),
    ],
    positions: [
      { label: "전체 직급", value: "all" },
      ...positions.map((position) => ({
        label: position,
        value: position,
      })),
    ],
  };
}
```

---

### 작업 3: 사용자 타입과 service 추가

파일:

- 수정: `web/src/app/(protected)/user/_types/user.types.ts`
- 수정: `web/src/app/(protected)/user/_service/user.service.ts`
- 생성: `web/tests/userManagementList.test.mjs`

단계:

- [ ] `web/tests/userManagementList.test.mjs`를 작성한다.

검증 내용:

- `user.service.ts`는 `apiClient.get<UserSummary[]>("/users"`를 사용한다.
- `PageResponse`를 사용하지 않는다.
- `getUser(userId)`는 `/users/${userId}`를 호출한다.
- `UserSummary`, `UserDetail` 타입이 존재한다.
- `/user/page.tsx`는 `ScaffoldPage`가 아니라 `UserList`를 렌더링한다.
- 목록 컴포넌트는 `/user/detail/${user.userId}` 링크를 가진다.
- 목록 컴포넌트에는 `상세`, `수정`, `삭제` 버튼이 없다.
- 상세 컴포넌트에는 `수정`, `삭제` 버튼이 있다.

- [ ] 실패 확인 명령을 실행한다.

```bash
node --test --experimental-strip-types web/tests/userManagementList.test.mjs
```

예상 결과: 현재 user 파일들이 비어 있거나 스캐폴드라 실패한다.

- [ ] `user.types.ts`에 목록/상세 타입을 정의한다.

필수 타입:

- `UserSummary`
- `UserTeamSummary`
- `UserDetail`
- `GetUsersParams`
- `UserFilterState`

- [ ] `user.service.ts`를 구현한다.

```ts
import { apiClient } from "@/app/_common/service/api-client";
import type {
  GetUsersParams,
  UserDetail,
  UserSummary,
} from "../_types/user.types";

export const userService = {
  getUsers: (params: GetUsersParams = {}) =>
    apiClient.get<UserSummary[]>("/users", { params }),

  getUser: (userId: number) =>
    apiClient.get<UserDetail>(`/users/${userId}`),
};
```

---

### 작업 4: 사용자 React Query hook 추가

파일:

- 수정: `web/src/app/(protected)/user/_hooks/useUserList.ts`
- 수정: `web/src/app/(protected)/user/_hooks/useUserDetail.ts`
- 수정: `web/src/app/(protected)/user/_hooks/index.ts`

단계:

- [ ] `useUserList.ts`를 구현한다.

```ts
import { useQuery } from "@tanstack/react-query";
import { userService } from "../_service/user.service";
import type { GetUsersParams } from "../_types/user.types";

export const userKeys = {
  all: ["users"] as const,
  list: (params: GetUsersParams = {}) =>
    [...userKeys.all, "list", params] as const,
  detail: (userId: number) => [...userKeys.all, "detail", userId] as const,
};

export function useUserList(params: GetUsersParams = {}) {
  return useQuery({
    queryKey: userKeys.list(params),
    queryFn: () => userService.getUsers(params),
  });
}
```

- [ ] `useUserDetail.ts`를 구현한다.

```ts
import { useQuery } from "@tanstack/react-query";
import { userService } from "../_service/user.service";
import { userKeys } from "./useUserList";

export function useUserDetail(userId: number) {
  return useQuery({
    queryKey: userKeys.detail(userId),
    queryFn: () => userService.getUser(userId),
    enabled: Number.isFinite(userId),
  });
}
```

- [ ] `index.ts`에서 hook을 export한다.

```ts
export * from "./useUserDetail";
export * from "./useUserList";
```

---

### 작업 5: 사용자 목록 화면 구현

파일:

- 수정: `web/src/app/(protected)/user/page.tsx`
- 수정: `web/src/app/(protected)/user/_components/userList.tsx`

단계:

- [ ] `page.tsx`에서 `UserList`를 렌더링한다.

```tsx
import UserList from "./_components/userList";

export default function UserPage() {
  return <UserList />;
}
```

- [ ] `userList.tsx`를 클라이언트 컴포넌트로 구현한다.

필수 구현:

- `"use client";`
- `useUserList(buildUserListParams(filters))`
- `Input` 검색창
- `Button` 필터 토글
- `Select` 기반 부서/직급/상태 필터
- `표시 중인 사용자 N명`
- loading/error/empty 상태
- `Link href={`/user/detail/${user.userId}`}`
- 기존 `Avatar`, `AvatarImage`, `AvatarFallback` 사용
- 목록 내부 액션 버튼 없음

- [ ] 집중 테스트를 실행한다.

```bash
node --test --experimental-strip-types web/tests/userManagementList.test.mjs
```

예상 결과: 상세 페이지 작업 전까지 일부 테스트는 실패할 수 있다.

---

### 작업 6: 사용자 상세 최소 화면 구현

파일:

- 수정: `web/src/app/(protected)/user/detail/[id]/page.tsx`
- 수정: `web/src/app/(protected)/user/_components/userDetail.tsx`

단계:

- [ ] `detail/[id]/page.tsx`에서 `useUserDetail(userId)`를 사용한다.

필수 상태 문구:

- 로딩: `사용자 정보를 불러오는 중입니다.`
- 에러: `사용자 정보를 불러오지 못했습니다.`
- 없음: `사용자를 찾을 수 없습니다.`

- [ ] `userDetail.tsx`를 구현한다.

필수 구현:

- `사용자 목록` 버튼
- `수정` 버튼
  - `href={`/user/edit/${user.userId}`}`
- `삭제` 버튼
  - 이번 범위에서는 `disabled`
  - `title="사용자 삭제 기능은 다음 단계에서 연결합니다."`
- 사용자 이름, 재직 상태, 이메일, 연락처, 부서, 입사일
- 소속 팀 목록
- 프로필 이미지는 기존 `Avatar` 컴포넌트 사용

- [ ] 집중 테스트를 실행한다.

```bash
node --test --experimental-strip-types web/tests/userManagementList.test.mjs
```

예상 결과: 4개 테스트 통과.

---

### 작업 7: 검증과 정리

단계:

- [ ] 사용자 관리 관련 Node 테스트를 실행한다.

```bash
node --test --experimental-strip-types web/tests/userStatus.utils.test.mjs web/tests/userFilter.utils.test.mjs web/tests/userManagementList.test.mjs
```

예상 결과: 모든 테스트 통과.

- [ ] web lint를 실행한다.

```bash
pnpm --filter ./web lint
```

예상 결과: exit code 0.

- [ ] web build를 실행한다.

```bash
pnpm --filter ./web build
```

예상 결과: exit code 0.

- [ ] 브라우저에서 `/user`를 확인한다.

확인 항목:

- `사용자 탐색`이 보인다.
- 검색창과 필터 버튼이 보인다.
- 필터 버튼을 누르면 필터 패널이 열린다.
- 사용자 목록은 가로형 카드로 보인다.
- 목록 내부에는 `상세`, `수정`, `삭제` 버튼이 없다.
- 사용자 카드를 클릭하면 `/user/detail/{id}`로 이동한다.
- 상세 페이지 내부에는 `수정`, `삭제` 버튼이 있다.

---

## 4. 최종 검증 명령

작업 완료 보고 전에 아래 명령을 새로 실행한다.

```bash
node --test --experimental-strip-types web/tests/userStatus.utils.test.mjs web/tests/userFilter.utils.test.mjs web/tests/userManagementList.test.mjs
pnpm --filter ./web lint
pnpm --filter ./web build
```

## 5. 커밋 기준

사용자가 커밋을 명시적으로 요청하기 전에는 커밋하지 않는다.

사용자가 커밋을 요청하면 이 기능에 해당하는 파일만 명시적으로 stage하고 아래 메시지를 사용한다.

```text
feat(web): 사용자 관리 목록 화면 구현
```
