# User API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
사용자 계정, 조직 소속, 역할/재직 상태를 조회하고 관리하는 계약을 정의한다. 본 문서는 `/api/users/*` 를 사용한다. 사용자 membership 정보는 주 소속 부서와 별개인 교차 부서 팀 참여까지 포함할 수 있으며, `isLeader`/`teamRole`/`isPrimary` vocabulary 를 `tb_user_team` 기준으로 설명한다. 셀프 회원가입은 인증 유스케이스로 이동했으므로 [api-spec-auth.md](./api-spec-auth.md)의 `POST /api/auth/signup` 이 소유한다.

## 2. 주요 ERD 연관
- `tb_user`
- `tb_user_team`
- `tb_department`
- `tb_team`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [api-spec-auth.md](./api-spec-auth.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/users/me` | `Documented` | 현재 로그인 사용자의 프로필/권한 문맥을 조회한다. |
| `GET` | `/api/users/manager-candidates` | `Documented` | 호출자 role 기준으로 관리자 선택 후보를 조회한다. |
| `GET` | `/api/users` | `Documented` | 사용자 목록을 페이지네이션 없이 필터 조건에 따라 조회한다. |
| `GET` | `/api/users/{id}` | `Documented` | 단일 사용자 상세와 전체 소속 팀 문맥을 조회한다. |
| `PATCH` | `/api/users/{id}` | `Documented` | 사용자 기본 정보와 대표 소속 팀을 부분 수정한다. |

## 5. 엔드포인트 상세

### GET /api/users/me
- 목적: 현재 로그인 사용자의 프로필 및 소속 팀 문맥을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자만 호출한다. accessToken 으로 인증한다.
- 요청
  - Header: `Authorization: Bearer <access-token>`
- 응답 (`data` 기준)
  - `userId`, `userName`, `email`, `phone`, `departmentId`, `departmentName`, `positionName`, `titleName`, `joinDate`, `employmentStatus`, `profileImageUrl`
  - `teams[*]`: `isPrimary`, `teamId`, `teamName`, `isLeader`, `teamRole`, `allocation`
- 응답 필드 메모
  - `isLeader`: 팀 대표 membership 여부다.
  - `teamRole`: 사용자의 팀 내 업무 역할명이다.
  - `allocation`: 배치 성격이다.
  - `isPrimary`: 사용자 관점의 대표 소속 팀 여부다. 팀의 소유 부서와는 별개다.
- 요청 JSON 예시
```json
{
  "headers": {
    "Authorization": "Bearer <access-token>"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "userId": 101,
    "userName": "홍길동",
    "email": "hong@axwms.com",
    "phone": "010-1234-5678",
    "departmentId": 10,
    "departmentName": "물류본부",
    "positionName": "과장",
    "titleName": "팀장",
    "joinDate": "2024-03-01",
    "employmentStatus": "ACTIVE",
    "profileImageUrl": "https://cdn.axwms.com/profile/101.png",
    "teams": [
      {
        "isPrimary": true,
        "teamId": 21,
        "teamName": "물류혁신TF",
        "isLeader": true,
        "teamRole": "플랫폼 총괄",
        "allocation": "주담당"
      },
      {
        "isPrimary": false,
        "teamId": 22,
        "teamName": "SCM분석팀",
        "isLeader": false,
        "teamRole": "SCM 분석",
        "allocation": "겸임"
      }
    ]
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_UNAUTHORIZED`
  - 대표 오류: `USER_NOT_FOUND`
- ERD 연관
  - `tb_user`
  - `tb_department`
  - `tb_user_team`
  - `tb_team`
- 근거
  - source: `domain.organization.user.controller.UserController#getMyProfile`
  - source: `domain.organization.user.dto.GetMyProfileApiDto.Response`
  - source: ADR-002

### GET /api/users/manager-candidates
- 목적: 호출자 role 기준으로 관리자 선택 후보를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR`, `DEPT_HEAD` 만 호출할 수 있다. accessToken 으로 인증한다.
- 요청
  - Header: `Authorization: Bearer <access-token>`
  - Query/Body 로 role 값을 받지 않는다.
- 조회 규칙
  - 분기 기준 role 은 클라이언트가 전달한 값이 아니라 access token 의 인증된 사용자 role 이다.
  - 인증된 사용자 role 이 `DIRECTOR` 이면 `role_code = DEPT_HEAD` 인 사용자를 모두 반환한다.
  - 인증된 사용자 role 이 `DEPT_HEAD` 이면 호출자 자기 자신만 반환한다.
  - `TEAM_LEAD`, `MEMBER` 는 controller role gate 에서 차단한다.
- 응답 (`data` 기준)
  - 페이지네이션 래퍼를 사용하지 않고 `UserSimpleSummary[]` 배열을 반환한다.
  - 각 항목: `userId`, `userName`, `titleName`, `positionName`
- 응답 필드 메모
  - `positionName` 은 사용자의 직급명이 없으면 `null` 로 반환할 수 있다.
  - `titleName` 은 사용자의 직책명을 나타낸다.
- 요청 JSON 예시
```json
{
  "headers": {
    "Authorization": "Bearer <access-token>"
  }
}
```
- 응답 JSON 예시 (`DIRECTOR` 호출)
```json
{
  "success": true,
  "data": [
    {
      "userId": 201,
      "userName": "김부서",
      "titleName": "부서장",
      "positionName": "부장"
    },
    {
      "userId": 202,
      "userName": "이본부",
      "titleName": "부서장",
      "positionName": null
    }
  ],
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 응답 JSON 예시 (`DEPT_HEAD` 호출)
```json
{
  "success": true,
  "data": [
    {
      "userId": 201,
      "userName": "김부서",
      "titleName": "부서장",
      "positionName": "부장"
    }
  ],
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_UNAUTHORIZED` [추론]
  - 대표 오류: `AUTH_ACCESS_DENIED` [추론]
  - 대표 오류: `USER_NOT_FOUND` [추론]
- ERD 연관
  - `tb_user`
- 근거
  - source: user-request clarification
  - source: deep-interview spec `.omx/specs/deep-interview-user-lookup-endpoints.md`

### GET /api/users
- 목적: 사용자 목록을 페이지네이션 없이 필터 조건에 따라 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자만 호출한다. accessToken 으로 인증한다.
- 요청
  - Query: `departmentId`, `positionName`, `employmentStatus`
  - 각 query 필드는 `null` 이거나 생략되면 전체 조건으로 해석한다.
  - `employmentStatus` 필터가 없으면 `ACTIVE`, `LEAVE` 사용자만 조회한다. `RETIRED` 사용자는 조회 대상에서 항상 제외한다.
- 정렬
  - `role_code` 기준으로 `DIRECTOR` → `DEPT_HEAD` → `TEAM_LEAD` → `MEMBER` 순서로 정렬한다.
- 응답 (`data` 기준)
  - 페이지네이션 래퍼를 사용하지 않고 `UserSummary[]` 배열을 반환한다.
  - `items`/`page`/`pageSize`/`totalCount` 같은 `PageResponse` 필드를 포함하지 않는다.
  - 각 항목: `userId`, `userName`, `email`, `departmentId`, `departmentName`, `profileImageUrl`, `teamId`, `teamName`, `positionName`, `titleName`, `employmentStatus`
- 응답 필드 메모
  - `teamId`, `teamName` 은 `tb_user_team.is_primary = true` 인 대표 소속 팀이다.
  - 대표 소속 팀은 사용자별로 항상 1개 이하이며, 존재하지 않으면 `teamId`, `teamName` 을 `null` 로 반환한다.
- 요청 JSON 예시
```json
{
  "query": {
    "departmentId": 10,
    "positionName": "과장",
    "employmentStatus": "ACTIVE"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": [
    {
      "userId": 101,
      "userName": "홍길동",
      "email": "hong@axwms.com",
      "departmentId": 10,
      "departmentName": "물류본부",
      "profileImageUrl": "https://cdn.axwms.com/profile/101.png",
      "teamId": 21,
      "teamName": "물류혁신TF",
      "positionName": "과장",
      "titleName": "팀장",
      "employmentStatus": "ACTIVE"
    },
    {
      "userId": 102,
      "userName": "김휴직",
      "email": "leave@axwms.com",
      "departmentId": 10,
      "departmentName": "물류본부",
      "profileImageUrl": null,
      "teamId": null,
      "teamName": null,
      "positionName": "대리",
      "titleName": "팀원",
      "employmentStatus": "LEAVE"
    }
  ],
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_UNAUTHORIZED` [추론]
  - 대표 오류: `USER_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_user`
  - `tb_department`
  - `tb_user_team`
  - `tb_team`
- 근거
  - source: user-request clarification
  - source: ADR-007 예외 — 본 API는 명시적으로 페이지네이션을 사용하지 않는다.

### GET /api/users/{id}
- 목적: 단일 사용자 상세와 전체 소속 팀 문맥을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자만 호출한다. accessToken 으로 인증한다.
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `userId`, `userName`, `email`, `departmentId`, `departmentName`, `positionName`, `titleName`, `joinDate`, `profileImageUrl`, `phone`, `employmentStatus`
  - `teams[*]`: `isPrimary`, `teamId`, `teamName`, `isLeader`, `teamRole`
- 응답 필드 메모
  - `teams` 는 대상 사용자가 소속된 모든 팀 정보를 반환한다.
  - `teams[*].teamId`, `teams[*].teamName` 은 각 소속 팀의 식별자와 이름이다.
  - `teams[*].isPrimary` 는 `tb_user_team.is_primary` 값을 나타내며, 대표 소속 팀 여부다.
  - 대표 소속 팀은 `tb_user_team.is_primary = true` 인 팀이며, 사용자별로 항상 1개 이하로 존재한다.
  - 대표 소속 팀이 없어도 top-level `teamId`, `teamName` 은 별도로 반환하지 않고, `teams` 목록만 반환한다.
  - `teams` 정렬은 `is_primary = true` 우선, 그 다음 `is_leader = true` 우선 순서로 배치한다.
  - `isLeader`: 해당 팀에서 팀 대표 membership 인지 여부다.
  - `teamRole`: 사용자의 팀 내 업무 역할명이다.
- 요청 JSON 예시
```json
{
  "path": {
    "id": 101
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "userId": 101,
    "userName": "홍길동",
    "email": "hong@axwms.com",
    "departmentId": 10,
    "departmentName": "물류본부",
    "positionName": "과장",
    "titleName": "팀장",
    "joinDate": "2024-03-01",
    "profileImageUrl": "https://cdn.axwms.com/profile/101.png",
    "phone": "010-1234-5678",
    "employmentStatus": "ACTIVE",
    "teams": [
      {
        "isPrimary": true,
        "teamId": 21,
        "teamName": "웹서비스 개발",
        "isLeader": true,
        "teamRole": "플랫폼 총괄"
      },
      {
        "isPrimary": false,
        "teamId": 23,
        "teamName": "AWS 개발",
        "isLeader": false,
        "teamRole": "플랫폼 총괄"
      }
    ]
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_UNAUTHORIZED` [추론]
  - 대표 오류: `USER_NOT_FOUND` [추론]
  - 대표 오류: `USER_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_user`
  - `tb_department`
  - `tb_user_team`
  - `tb_team`
- 근거
  - source: user-request clarification

### PATCH /api/users/{id}
- 목적: 사용자 기본 정보와 대표 소속 팀을 부분 수정한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR`, `DEPT_HEAD` 만 호출할 수 있다. `DIRECTOR` 는 역할 계층상 `DEPT_HEAD` 권한을 포함한다.
- 요청
  - Path: `id`
  - Body: `userName`, `email`, `profileImageUrl`, `positionName`, `titleName`, `departmentId`, `phone`, `employmentStatus`, `joinDate`, `primaryTeamId`
  - 각 Body 필드는 `null` 이거나 생략되면 수정하지 않고 기존 값을 유지한다.
  - `primaryTeamId` 는 사용자 요청 초안의 `privateTeamId` 의미를 `tb_user_team.is_primary` vocabulary 에 맞춰 정규화한 필드명이다.
- 수정 규칙
  - 값이 새로 전달된 필드만 업데이트한다.
  - `primaryTeamId` 가 전달되면 대상 사용자의 해당 팀 membership 을 `is_primary = true` 로 설정한다.
  - 대상 사용자에게 기존 대표 소속 팀이 없으면 요청된 팀을 대표 소속 팀으로 지정한다.
  - 다른 팀이 이미 `is_primary = true` 이면 기존 팀은 `false` 로 변경하고 요청된 팀만 `true` 로 변경한다.
  - 사용자별 대표 소속 팀은 1개 이하만 존재할 수 있다.
- 응답 (`data` 기준)
  - 빈 객체 (`EmptyResponse`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 101
  },
  "body": {
    "userName": "홍길동",
    "email": "hong@axwms.com",
    "profileImageUrl": "https://cdn.axwms.com/profile/101.png",
    "positionName": "차장",
    "titleName": "팀장",
    "departmentId": 10,
    "phone": "010-1234-5678",
    "employmentStatus": "ACTIVE",
    "joinDate": "2024-03-01",
    "primaryTeamId": 21
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_UNAUTHORIZED` [추론]
  - 대표 오류: `AUTH_ACCESS_DENIED` [추론]
  - 대표 오류: `USER_NOT_FOUND` [추론]
  - 대표 오류: `DEPARTMENT_NOT_FOUND` [추론]
  - 대표 오류: `TEAM_NOT_FOUND` [추론]
- ERD 연관
  - `tb_user`
  - `tb_department`
  - `tb_user_team`
  - `tb_team`
- 근거
  - source: user-request clarification
  - source: ADR-002

## 6. 변경/제외 메모
- `POST /api/users/signup` 은 사용자 도메인이 아니라 Auth 도메인의 `POST /api/auth/signup` 으로 이동했다.
- `POST /api/users` 는 현재 사용자 명세에서 삭제한다.
- `DELETE /api/users/{id}` 는 현재 사용자 명세에서 삭제한다.
- 기존 `PUT /api/users/{id}` 표기는 `PATCH /api/users/{id}` 로 정정한다.
- `GET /api/users` 는 ADR-007의 일반 목록 페이지네이션 표준과 달리, 본 요구사항에 따라 페이지네이션 없이 배열을 반환한다.
