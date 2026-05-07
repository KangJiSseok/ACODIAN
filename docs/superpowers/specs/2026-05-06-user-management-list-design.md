# 사용자 관리 목록 화면 디자인

## 목표

사용자 관리 1차 범위는 `/user` 목록 화면을 실제 데이터 기반 화면으로 바꾸는 것이다. 사용자는 와이어프레임(`https://wms-wire-frame.vercel.app/user`)과 비슷한 어두운 운영 도구 스타일에서 사용자 목록을 검색하고, 필터링하고, 각 사용자 행을 눌러 상세 페이지로 이동할 수 있어야 한다.

## 범위

이번 작업에 포함한다.

- `/user` 목록 화면 구현
- `GET /users` 기반 사용자 목록 조회
- 이름 검색과 필터 UI 제공
- 사용자별 가로형 리스트 카드 표시
- 카드 전체 클릭 시 `/user/detail/[id]`로 이동
- 목록 카드 내부에는 `상세`, `수정`, `삭제` 버튼을 두지 않음
- `/user/detail/[id]`는 진입 가능한 최소 상세 화면으로 정리하고, 상세 내부에 `수정`, `삭제` 버튼 배치

이번 작업에서 제외한다.

- 사용자 등록 구현
- 사용자 수정 저장 구현
- 사용자 삭제 구현
- 스킬/평가 영역 구현
- 백엔드 API 변경

## 화면 구조

`/user`는 기존 `ScaffoldPage` placeholder를 제거하고 실제 클라이언트 화면으로 구현한다. 상단은 기존 보호 라우트 레이아웃을 그대로 사용하고, 본문은 와이어프레임과 같은 구성으로 둔다.

1. 상단 제목 영역
   - 제목: `사용자 탐색`
   - 부가 설명은 길게 넣지 않고, 목록의 목적이 드러나는 짧은 문구만 사용한다.

2. 검색/필터 영역
   - 큰 검색 input을 왼쪽에 둔다.
   - placeholder는 `이름, 이메일, 부서, 직급으로 검색하세요`를 사용한다.
   - 오른쪽에는 `필터` 버튼을 둔다.
   - 1차 구현에서는 필터 패널을 같은 화면 안에 펼치는 방식으로 둔다.

3. 목록 헤더
   - `표시 중인 사용자 N명`을 보여준다.
   - API 응답이 배열이므로 프론트에서 현재 필터 결과의 개수를 계산한다.

4. 사용자 목록
   - 각 사용자는 가로형 카드 한 줄로 표시한다.
   - 왼쪽에는 프로필 이미지 또는 이니셜 아바타, 이름, 직책/직급, 재직 상태, 이메일, 연락처를 배치한다.
   - 하단 메타 정보로 부서와 대표 소속 팀을 표시한다.
   - 오른쪽에는 입사일을 배치한다.
   - 카드 전체는 링크 역할을 하며 `/user/detail/[id]`로 이동한다.

## 데이터 모델

사용자 목록은 `docs/api/spec/api-spec-user.md`의 `GET /api/users` 문서를 따른다.

목록 항목에서 사용하는 필드는 다음과 같다.

- `userId`
- `userName`
- `email`
- `phone`
- `departmentId`
- `departmentName`
- `profileImageUrl`
- `teamId`
- `teamName`
- `positionName`
- `titleName`
- `employmentStatus`

프론트 전용으로 상태 라벨 유틸을 둔다.

- `ACTIVE` → `재직`
- `LEAVE` → `휴직`
- 알 수 없는 값 → 원본 값 또는 `-`

`GET /users`는 페이지네이션 래퍼 없이 배열을 반환한다. 따라서 `PageResponse`를 사용하지 않고 `UserSummary[]`를 직접 사용한다.

## 필터 동작

검색 input은 사용자가 입력한 문자열을 API query의 `userName`으로 전달한다. 백엔드 문서는 `userName`, `departmentId`, `positionName`, `employmentStatus` query를 지원하므로, 필터 패널은 이 네 가지 축을 기준으로 설계한다.

1차 구현에서는 다음을 우선한다.

- 검색어: `userName`
- 재직 상태: `employmentStatus`

부서와 직급 필터는 UI 구조와 타입을 열어두되, 기존에 안정적으로 쓸 수 있는 부서/직급 옵션 소스가 확인된 범위에서만 연결한다. 옵션 소스가 별도 API 없이 사용자 목록에서만 얻을 수 있다면 현재 목록 데이터에서 중복 제거해 구성한다.

## 상세 이동

부서/팀 관리 탭과 동일하게 목록 안에서 직접 액션을 수행하지 않는다. 사용자는 사용자 카드를 눌러 상세 페이지로 이동한다.

목록 카드 내부에는 `상세`, `수정`, `삭제` 버튼을 두지 않는다. 실제 상세 확인, 수정, 삭제 액션은 상세 페이지 내부에서 다룬다.

## 상세 페이지 최소 범위

`/user/detail/[id]`는 현재 placeholder 대신 최소 상세 화면으로 정리한다. `GET /users/{id}`를 사용해 사용자 기본 정보와 소속 팀 목록을 보여준다.

상세 화면에는 다음 액션을 둔다.

- `사용자 목록`으로 돌아가기
- `수정` 버튼
- `삭제` 버튼

수정 저장과 삭제 실행 기능은 이번 범위가 아니므로 `/user/edit/[id]` 실제 구현과 사용자 삭제 API 연동은 진행하지 않는다. 다만 상세 페이지의 액션 위치를 확정하기 위해 `수정`, `삭제` 버튼은 상세 화면 안에 배치한다.

## 상태 처리

목록과 상세는 기존 부서/팀 화면과 같은 상태 표현을 사용한다.

- 로딩: `사용자 목록을 불러오는 중입니다.`
- 목록 에러: `사용자 목록을 불러오지 못했습니다.`
- 빈 목록: `조건에 맞는 사용자가 없습니다.`
- 상세 로딩: `사용자 정보를 불러오는 중입니다.`
- 상세 에러: `사용자 정보를 불러오지 못했습니다.`
- 상세 없음: `사용자를 찾을 수 없습니다.`

## 컴포넌트와 파일 경계

기존 `/user` 폴더의 빈 파일을 실제 역할별 파일로 채운다.

- `web/src/app/(protected)/user/page.tsx`: 목록 화면 조합
- `web/src/app/(protected)/user/_components/userList.tsx`: 검색/필터/목록 카드 렌더링
- `web/src/app/(protected)/user/_components/userDetail.tsx`: 상세 화면 본문
- `web/src/app/(protected)/user/_hooks/useUserList.ts`: 목록 query key와 query hook
- `web/src/app/(protected)/user/_hooks/useUserDetail.ts`: 상세 query hook
- `web/src/app/(protected)/user/_hooks/index.ts`: hook export
- `web/src/app/(protected)/user/_service/user.service.ts`: `/users` API 호출
- `web/src/app/(protected)/user/_types/user.types.ts`: 목록/상세 타입
- `web/src/app/(protected)/user/_utils/userStatus.utils.ts`: 사용자 상태 라벨/variant 유틸
- `web/src/app/(protected)/user/detail/[id]/page.tsx`: 상세 라우트 조합

## 테스트 전략

DOM 렌더링 테스트보다 현재 프로젝트의 기존 테스트 스타일을 따른다. 파일 내용을 읽어 핵심 계약이 유지되는지 확인하는 Node test를 추가한다.

검증할 계약은 다음과 같다.

- `/user/page.tsx`가 placeholder `ScaffoldPage`가 아니라 실제 사용자 목록 컴포넌트를 사용한다.
- 사용자 목록 service가 `GET /users`를 호출하고 `PageResponse`가 아닌 배열 타입을 사용한다.
- 사용자 카드는 `/user/detail/${user.userId}`로 이동한다.
- 목록 내부에는 `상세`, `수정`, `삭제` 버튼이 존재하지 않는다.
- 상세 페이지 내부에 `수정`, `삭제` 버튼이 존재한다.
- 사용자 상태 라벨은 `ACTIVE`, `LEAVE`를 한국어 라벨로 변환하고, 그 외 값은 원본 값을 유지한다.

## 수용 기준

- `/user`에서 사용자 목록이 실제 API 데이터로 표시된다.
- 검색/필터 조작 시 API query가 갱신된다.
- 각 사용자 카드를 클릭하면 `/user/detail/[id]`로 이동한다.
- 목록 내부에는 별도 액션 버튼이 없다.
- 상세 화면에서 사용자 기본 정보를 확인할 수 있고, `수정`, `삭제` 버튼이 상세 내부에 배치된다.
- 백엔드 코드는 수정하지 않는다.
- `pnpm --filter ./web lint`가 통과한다.
