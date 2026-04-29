# Team API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
팀 도메인의 조회/상세/멤버십/상태/삭제 계약을 정의한다. 본 문서는 normalized path 인 `/api/teams/*` 를 기준으로 하며,
팀 접근 범위는 access token 의 `role` 과 호출자 소유권(부서/팀 소속)에 따라 달라진다.

이번 계약에서 `tb_team.status_code` 는 운영 상태(`ACTIVE`, `INACTIVE`)이고, `tb_team.deleted_at` 은 soft-delete lifecycle 이다.
팀 삭제는 `deleted_at` 만 설정하며, `tb_user_team` row 가 남아 있어도 팀 삭제를 허용한다.

## 2. 주요 ERD 연관
- `tb_team`
- `tb_user_team`
- `tb_department`
- `tb_user`
- `tb_worklog`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [api-spec-enums.md](./api-spec-enums.md)
- [api-spec-worklog.md](./api-spec-worklog.md)
- [adr.yaml](../adr.yaml)

## 4. 데이터 모델 / 공통 규칙
### 4.1 `tb_team`
- `statusCode` 는 운영 상태 전용 (`ACTIVE`, `INACTIVE`) 이다.
- `deletedAt` 은 soft-delete lifecycle 전용이다.
- `PATCH /api/teams/{id}/status` 는 `statusCode` 만 변경한다.
- `DELETE /api/teams/{id}` 는 `deletedAt` 만 변경한다.
- soft-delete 된 팀이 있어도 동일 `(department_id, team_name)` 재생성을 허용한다.
- uniqueness 판단은 `deleted_at IS NULL` 인 활성 row 기준으로 해석한다.

### 4.2 `tb_user_team`
- `(user_id, team_id)` 당 1행을 유지하는 single-row membership 모델을 사용한다.
- membership 의 운영 상태는 `ACTIVE`, `LEFT` 두 종류만 사용한다.
- 재가입은 신규 row 누적이 아니라 동일 row 를 `ACTIVE` 로 되돌리는 방식으로 처리한다.
- membership 권한은 `teamAuthority(MEMBER, LEADER, ADMIN)` 로 관리한다.
- 팀 내 업무 역할은 `teamRole`, 배치 성격은 `allocation`, 대표 소속 여부는 `isPrimary` 로 표현한다.
- `ACTIVE` membership 기준 팀당 `LEADER` 는 최대 1명이며, `ADMIN` 은 대표자 계산에서 제외한다.

### 4.3 역할별 조회 범위
- `DIRECTOR`: 모든 부서/모든 팀 접근 가능
- `DEPT_HEAD`: 기본 visible scope 는 본인 소속 부서의 전체 팀 + 본인이 현재 소속된 전체 팀의 합집합이다. `departmentId = null` 인 목록/요약 조회에서는 이 합집합을 사용하고, 중복 팀은 DISTINCT 처리한다.
- `TEAM_LEAD`, `MEMBER`: 기본 visible scope 는 본인이 현재 소속된 전체 팀이다. `departmentId = null` 이면 이 전체 visible scope 를 조회하고, 값이 있으면 visible scope 내 추가 필터로만 해석한다.
- controller 는 역할 확인을 수행하고, service 는 실제 부서/팀 ownership 을 검증한다.

### 4.4 집계/정렬 공통 해석
- 별도 언급이 없으면 team 목록의 `memberCount` 는 해당 팀의 `ACTIVE` membership 수다.
- visible scope 가 부서 ownership + membership 합집합으로 계산되는 경우 팀 집합은 먼저 **DISTINCT team** 기준으로 정규화한다.
- 목록 pagination 의 `items`, `totalCount`, `totalPages` 는 정규화된 visible scope 기준으로 계산한다.
- 별도 언급이 없으면 user 집계는 정규화된 visible scope 내 **DISTINCT user 수** 기준이다.
- `GET /api/teams/{id}` 및 `GET /api/teams/{id}/worklogs` 는 soft-delete 되지 않은 업무일지만 대상으로 한다.

## 5. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/teams` | `Documented` | 역할별 조회 범위에 맞는 팀 목록을 페이지네이션으로 조회한다. |
| `GET` | `/api/teams/summary` | `Proposed-risk-closure` | 페이지네이션 응답과 분리된 상단 집계 요약을 조회한다. |
| `GET` | `/api/teams/{id}` | `Documented` | 역할별 조회 범위에 맞는 단일 팀 상세와 업무일지 집계를 조회한다. |
| `GET` | `/api/teams/{id}/users` | `Documented` | 역할별 조회 범위에 맞는 팀 소속 사용자 목록을 조회한다. |
| `GET` | `/api/teams/{id}/worklogs` | `Documented` | 역할별 조회 범위에 맞는 팀 업무일지 목록을 조회한다. |
| `POST` | `/api/teams` | `Documented` | 새 팀을 생성한다. |
| `PUT` | `/api/teams/{id}` | `Documented` | 팀 기본 정보를 수정한다. |
| `PATCH` | `/api/teams/{id}/status` | `Documented` | 팀 운영 상태를 전환한다. |
| `POST` | `/api/teams/{id}/users/bulk` | `Documented` | 팀 사용자 추가/제거를 일괄 반영한다. |
| `DELETE` | `/api/teams/{id}` | `Documented` | 팀을 soft-delete 한다. |

## 6. 엔드포인트 상세

### GET /api/teams
- 목적: 역할별 조회 범위에 맞는 팀 목록을 페이지네이션으로 조회한다.
- 상태: `Documented`
- 권한/접근 주체
  - `DIRECTOR`: 전체 팀 조회 가능
  - `DEPT_HEAD`: `departmentId = null` 이면 본인 소속 부서의 전체 팀 + 본인 소속 팀 전체를 DISTINCT 기준으로 조회 가능하며, 값이 있으면 `departmentId` 범위만 조회 가능
  - `TEAM_LEAD`, `MEMBER`: `departmentId = null` 이면 본인 소속 팀 전체 조회 가능하며, 값이 있으면 visible scope 내 추가 필터만 허용
- 요청
  - Query: `page`, `pageSize`
  - Query: `departmentId` (선택)
  - `sortBy`, `sortDirection` 은 공개 query 로 받지 않고 아래 고정 정렬 정책을 사용한다. 
- 고정 정렬 정책
  1. `statusCode = ACTIVE` 팀 우선
  2. 호출자의 `myTeamAuthority = LEADER` 인 팀 우선
  3. 호출자의 `allocation` 이 주 담당인 팀 우선 (`PRIMARY`, `MAIN`, `LEAD` 등 실제 enum/string 은 구현 SSOT 를 따른다.)
  4. 호출자의 `isPrimary = true` 인 팀 우선
- `departmentId` 해석
  - `DIRECTOR`: `null` 이면 전체 부서, 값이 있으면 해당 부서만 필터링
  - `DEPT_HEAD`: `null` 이면 본인 부서의 전체 팀 + 본인 소속 팀 전체의 합집합을 조회하며, 중복 팀은 DISTINCT 처리한다.  값이 있으면 `departmentId` 범위만 조회 가능
  - `TEAM_LEAD`, `MEMBER`: `null` 이면 본인 소속 팀 전체를 조회하고, 값이 있으면 visible team scope 내 추가 필터로만 적용한다.
- 응답 (`data` 기준)
  - `PageResponse<TeamSummary>`
  - `items[*]`
    - `teamId`, `teamName`, `statusCode`
    - `departmentId`, `departmentName`, `description`
    - `departmentHeadUserId`, `departmentHeadUserName`
    - `teamLeaderId`, `teamLeaderName`
    - `memberCount`
    - `myTeamAuthority`
    - `teamRole`, `allocation`, `isPrimary`
    - `startDate`, `expectedEndDate`
- 요청 JSON 예시
```json
{
  "query": {
    "page": 1,
    "pageSize": 20,
    "departmentId": 10
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "teamId": 21,
        "teamName": "물류혁신TF",
        "statusCode": "ACTIVE",
        "departmentId": 10,
        "departmentName": "물류본부",
        "description": "창고 자동화 개선 전담",
        "departmentHeadUserId": 1001,
        "departmentHeadUserName": "박본부",
        "teamLeaderId": 101,
        "teamLeaderName": "홍길동",
        "memberCount": 6,
        "myTeamAuthority": "LEADER",
        "teamRole": "플랫폼 총괄",
        "allocation": "PRIMARY",
        "isPrimary": true,
        "startDate": "2026-04-01",
        "expectedEndDate": "2026-12-31"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "totalCount": 1,
    "totalPages": 1,
    "isFirst": true,
    "isLast": true,
    "hasNext": false,
    "hasPrevious": false
  },
  "timestamp": "2026-04-26T12:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_UNAUTHORIZED`
  - 대표 오류: `AUTH_ACCESS_DENIED`
- ERD 연관
  - `tb_team`
  - `tb_department`
  - `tb_user_team`
  - `tb_user`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### GET /api/teams/summary
- 목적: 페이지네이션 응답과 분리된 상단 집계 요약을 조회한다.
- 상태: `Proposed-risk-closure`
- 권한/접근 주체
  - `DIRECTOR`: 전체 팀 집계 조회 가능
  - `DEPT_HEAD`: `departmentId = null` 이면 본인 소속 부서의 전체 팀 + 본인 소속 팀 전체를 DISTINCT 기준으로 집계 가능하며, 값이 있으면 `departmentId` 범위만 집계 가능
  - `TEAM_LEAD`, `MEMBER`: `departmentId = null` 이면 본인 소속 팀 전체 범위를 집계 가능하며, 값이 있으면 visible scope 내 추가 필터만 허용
- 요청
  - Query: `departmentId` (선택)
- `departmentId` 해석
  - `DIRECTOR`: `null` 이면 전체 부서, 값이 있으면 해당 부서만 필터링
  - `DEPT_HEAD`: `null` 이면 본인 부서의 전체 팀 + 본인 소속 팀 전체의 합집합을 집계하며, 중복 팀은 DISTINCT 처리한다. 값이 있으면 `departmentId` 범위만 허용한다.
  - `TEAM_LEAD`, `MEMBER`: `null` 이면 본인 소속 팀 전체를 집계하고, 값이 있으면 visible team scope 내 추가 필터로만 적용한다.
- 응답 (`data` 기준)
  - `activeTeamCount`: DISTINCT 처리된 visible scope 내 `ACTIVE` team 수
  - `totalTeamCount`: DISTINCT 처리된 visible scope 내 전체 team 수 (`deletedAt IS NULL` 기준)
  - `activeUserCount`: DISTINCT 처리된 visible scope 내 `employmentStatus = ACTIVE` 인 DISTINCT user 수
  - `activeTeamUserCount`: DISTINCT 처리된 visible scope 내 `ACTIVE` team 에 속한 DISTINCT user 수
  - `allTeamUserCount`: DISTINCT 처리된 visible scope 내 `ACTIVE` + `INACTIVE` team 에 속한 DISTINCT user 수
- 요청 JSON 예시
```json
{
  "query": {
    "departmentId": 10
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "activeTeamCount": 3,
    "totalTeamCount": 5,
    "activeUserCount": 12,
    "activeTeamUserCount": 14,
    "allTeamUserCount": 16
  },
  "timestamp": "2026-04-26T12:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_UNAUTHORIZED`
  - 대표 오류: `AUTH_ACCESS_DENIED`
- ERD 연관
  - `tb_team`
  - `tb_department`
  - `tb_user_team`
  - `tb_user`
- 근거
  - source: user clarified scope
  - source: pagination consistency clarification

### GET /api/teams/{id}
- 목적: 역할별 조회 범위에 맞는 단일 팀 상세와 업무일지 집계를 조회한다.
- 상태: `Documented`
- 권한/접근 주체
  - `DIRECTOR`: 모든 팀 접근 가능
  - `DEPT_HEAD`: 본인 소속 부서 팀 + 본인 소속 팀 접근 가능
  - `TEAM_LEAD`, `MEMBER`: 본인 소속 팀 접근 가능
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `teamId`, `teamName`, `statusCode`, `description`
  - `departmentId`, `departmentName`
  - `teamLeaderId`, `teamLeaderName`
  - `startDate`, `expectedEndDate`
  - `totalWorklogCount`: soft-delete 되지 않은 팀 업무일지 총 개수
  - `completedWorklogCount`: soft-delete 되지 않은 팀 업무일지 중 `statusCode = COMPLETED` 개수
- 요청 JSON 예시
```json
{
  "path": {
    "id": 21
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "teamId": 21,
    "teamName": "물류혁신TF",
    "statusCode": "ACTIVE",
    "description": "창고 자동화 개선 전담",
    "departmentId": 10,
    "departmentName": "물류본부",
    "teamLeaderId": 101,
    "teamLeaderName": "홍길동",
    "startDate": "2026-04-01",
    "expectedEndDate": "2026-12-31",
    "totalWorklogCount": 28,
    "completedWorklogCount": 11
  },
  "timestamp": "2026-04-26T12:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `TEAM_NOT_FOUND`
  - 대표 오류: `AUTH_ACCESS_DENIED`
- ERD 연관
  - `tb_team`
  - `tb_department`
  - `tb_user_team`
  - `tb_worklog`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### GET /api/teams/{id}/users
- 목적: 역할별 조회 범위에 맞는 팀 소속 사용자 목록을 조회한다.
- 상태: `Documented`
- 권한/접근 주체
  - `DIRECTOR`: 모든 팀 접근 가능
  - `DEPT_HEAD`: 본인 소속 부서 팀 + 본인 소속 팀 접근 가능
  - `TEAM_LEAD`, `MEMBER`: 본인 소속 팀 접근 가능
- 요청
  - Path: `id`
  - Query: `page`, `pageSize`
- 고정 정렬 정책
  1. `teamAuthority` 가 `LEADER > MEMBER > ADMIN` 순으로 우선한다.
  2. 동순위에서는 `userId ASC` 로 정렬한다.
- 응답 (`data` 기준)
  - `PageResponse<TeamUserSummary>`
  - `items[*]`
    - `teamAuthority`
    - `userId`, `userName`
    - `positionName`
    - `email`
    - `teamRole`, `allocation`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 21
  },
  "query": {
    "page": 1,
    "pageSize": 20
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "teamAuthority": "LEADER",
        "userId": 101,
        "userName": "홍길동",
        "positionName": "과장",
        "email": "hong@axwms.com",
        "teamRole": "플랫폼 총괄",
        "allocation": "PRIMARY"
      },
      {
        "teamAuthority": "ADMIN",
        "userId": 102,
        "userName": "김영희",
        "positionName": "대리",
        "email": "kim@axwms.com",
        "teamRole": "WMS 운영",
        "allocation": "SECONDARY"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "totalCount": 2,
    "totalPages": 1,
    "isFirst": true,
    "isLast": true,
    "hasNext": false,
    "hasPrevious": false
  },
  "timestamp": "2026-04-26T12:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `TEAM_NOT_FOUND`
  - 대표 오류: `AUTH_ACCESS_DENIED`
- ERD 연관
  - `tb_user_team`
  - `tb_user`
  - `tb_team`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### GET /api/teams/{id}/worklogs
- 목적: 역할별 조회 범위에 맞는 팀 업무일지 목록을 조회한다.
- 상태: `Documented`
- 권한/접근 주체
  - `DIRECTOR`: 모든 팀 접근 가능
  - `DEPT_HEAD`: 본인 소속 부서 팀 + 본인 소속 팀 접근 가능
  - `TEAM_LEAD`, `MEMBER`: 본인 소속 팀 접근 가능
- 요청
  - Path: `id`
  - Query: `page`, `pageSize`
- 고정 정렬 정책
  1. `IN_PROGRESS`
  2. `PENDING`
  3. `ON_HOLD`
  4. `COMPLETED`
  5. `CANCELLED`
- 응답 (`data` 기준)
  - `PageResponse<TeamWorklogSummary>`
  - `items[*]`
    - `worklogId`, `title`
    - `requestContent`, `workContent`
    - `aiSummary`
    - `statusCode`, `importanceCode`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 21
  },
  "query": {
    "page": 1,
    "pageSize": 20
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "worklogId": 9001,
        "title": "피킹 동선 최적화",
        "requestContent": "창고 A존 피킹 경로 개선안 수립",
        "workContent": "실측 동선 기반으로 경로 재정의",
        "aiSummary": "피킹 이동거리 단축을 위한 레이아웃 개선안 검토",
        "statusCode": "IN_PROGRESS",
        "importanceCode": "HIGH"
      }
    ],
    "page": 1,
    "pageSize": 20,
    "totalCount": 1,
    "totalPages": 1,
    "isFirst": true,
    "isLast": true,
    "hasNext": false,
    "hasPrevious": false
  },
  "timestamp": "2026-04-26T12:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `TEAM_NOT_FOUND`
  - 대표 오류: `AUTH_ACCESS_DENIED`
- ERD 연관
  - `tb_worklog`
  - `tb_team`
  - `tb_user_team`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### POST /api/teams
- 목적: 새 팀을 생성한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR`, `DEPT_HEAD`
- 요청
  - Body: `departmentId`, `teamName`, `description`, `leaderUserId`, `leaderMembership`, `statusCode`, `startDate`, `expectedEndDate`
- 요청 규칙
  - `leaderMembership` 에는 `teamRole`, `allocation`, `isPrimary` 를 nested payload 로 받는다.
  - create/update 는 `teamAuthority` 를 직접 받지 않으며 `leaderUserId` 대상 membership 을 항상 `LEADER` 로 설정한다.
  - `leaderMembership.joinedAt` 은 별도 입력으로 받지 않고 생성 시점 기본값을 사용한다.
  - `DEPT_HEAD` 는 본인 부서에 대해서만 생성할 수 있다. 다른 부서 `departmentId` 는 `AUTH_ACCESS_DENIED` 다.
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "body": {
    "departmentId": 10,
    "teamName": "물류혁신TF",
    "description": "창고 자동화 개선 전담",
    "leaderUserId": 101,
    "leaderMembership": {
      "teamRole": "플랫폼 총괄",
      "allocation": "PRIMARY",
      "isPrimary": true
    },
    "statusCode": "ACTIVE",
    "startDate": "2026-04-01",
    "expectedEndDate": "2026-12-31"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-26T12:00:00Z"
}
```
- 상태/에러
  - 성공: `201 Created`
  - 대표 오류: `TEAM_DUPLICATE_NAME`
  - 대표 오류: `USER_NOT_FOUND`
  - 대표 오류: `AUTH_ACCESS_DENIED`
- ERD 연관
  - `tb_team`
  - `tb_department`
  - `tb_user`
  - `tb_user_team`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### PUT /api/teams/{id}
- 목적: 팀 기본 정보를 수정한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR`, `DEPT_HEAD`
- 요청
  - Path: `id`
  - Body: `departmentId`, `teamName`, `description`, `leaderUserId`, `leaderMembership`, `statusCode`, `startDate`, `expectedEndDate`
- 요청 규칙
  - `leaderMembership` 에는 `teamRole`, `allocation`, `isPrimary` 를 nested payload 로 받는다.
  - `leaderUserId` 를 받으면 대상 `(user_id, team_id)` membership 을 재활성화/갱신 후 `LEADER` 로 승격한다.
  - 기존 ACTIVE `LEADER` 는 같은 트랜잭션에서 `MEMBER` 로 강등한다.
  - `DEPT_HEAD` 는 본인 부서 팀만 수정할 수 있다.
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 21
  },
  "body": {
    "departmentId": 10,
    "teamName": "물류혁신TF",
    "description": "창고 자동화 및 운영 고도화",
    "leaderUserId": 101,
    "leaderMembership": {
      "teamRole": "플랫폼 총괄",
      "allocation": "PRIMARY",
      "isPrimary": true
    },
    "statusCode": "ACTIVE",
    "startDate": "2026-04-01",
    "expectedEndDate": "2026-12-31"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-26T12:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `TEAM_NOT_FOUND`
  - 대표 오류: `TEAM_DUPLICATE_NAME`
  - 대표 오류: `USER_NOT_FOUND`
  - 대표 오류: `AUTH_ACCESS_DENIED`
- ERD 연관
  - `tb_team`
  - `tb_department`
  - `tb_user`
  - `tb_user_team`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### PATCH /api/teams/{id}/status
- 목적: soft-delete 와 분리된 팀 운영 상태를 전환한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR`, `DEPT_HEAD`
- 요청
  - Path: `id`
  - Body: `statusCode`
- 요청 규칙
  - path 의 `id` 로 team 을 식별하므로 request body 에 `teamId` 는 중복으로 받지 않는다.
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 21
  },
  "body": {
    "statusCode": "INACTIVE"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-26T12:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `TEAM_NOT_FOUND`
  - 대표 오류: `AUTH_ACCESS_DENIED`
- ERD 연관
  - `tb_team`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### POST /api/teams/{id}/users/bulk
- 목적: 팀 사용자 추가/제거를 일괄 반영한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR`, `DEPT_HEAD`
- 요청
  - Path: `id`
  - Body
    - `addUsers[*]`: `userId`, `teamAuthority`, `teamRole`, `allocation`, `isPrimary`, `joinedAt`
    - `removeUserIds[*]`: 제거 대상 `userId`
- 요청 규칙
  - 기존 legacy path `/api/teams/{id}/members/bulk` 는 사용하지 않는다.
  - `teamAuthority` 는 필수이며 그대로 사용한다.
  - 추가 대상 사용자가 기존 `LEFT` membership row 를 가지면 복구 처리한다.
  - bulk upsert 에서만 `ADMIN` 부여를 허용한다.
  - `DEPT_HEAD` 는 본인 부서 팀에 대해서만 수행할 수 있다.
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 21
  },
  "body": {
    "addUsers": [
      {
        "userId": 102,
        "teamAuthority": "ADMIN",
        "teamRole": "WMS 운영",
        "allocation": "SECONDARY",
        "isPrimary": false,
        "joinedAt": "2026-04-10"
      },
      {
        "userId": 103,
        "teamAuthority": "LEADER",
        "teamRole": "현장 총괄",
        "allocation": "PRIMARY",
        "isPrimary": true,
        "joinedAt": "2026-04-10"
      }
    ],
    "removeUserIds": [
      104,
      105
    ]
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-26T12:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `TEAM_NOT_FOUND`
  - 대표 오류: `USER_NOT_FOUND`
  - 대표 오류: `AUTH_ACCESS_DENIED`
- ERD 연관
  - `tb_user_team`
  - `tb_team`
  - `tb_user`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### DELETE /api/teams/{id}
- 목적: 팀을 soft-delete 한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR`, `DEPT_HEAD`
- 요청
  - Path: `id`
- 동작
  - `tb_user_team` 에 사용자가 남아 있어도 삭제를 허용한다.
  - 팀 row 는 hard delete 하지 않고 `deletedAt` 만 설정한다.
  - 관련 membership row 는 자동 삭제하지 않는다.
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 21
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-26T12:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `TEAM_NOT_FOUND`
  - 대표 오류: `AUTH_ACCESS_DENIED`
- ERD 연관
  - `tb_team`
  - `tb_user_team`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

## 7. 공통 검수 포인트
- `DIRECTOR`, `DEPT_HEAD`, `TEAM_LEAD`, `MEMBER` 별 조회 범위가 각 endpoint 에 명시되어 있는가
- `GET /api/teams` 가 순수 `PageResponse<TeamSummary>` 로 설명되어 있는가
- `GET /api/teams` 가 `myTeamAuthority` 만 노출하고 `myTeamLeader` 를 설명하지 않는가
- `GET /api/teams/summary` 가 상단 집계를 별도 endpoint 로 설명하는가
- `GET /api/teams/{id}` 가 soft-delete 되지 않은 업무일지 집계를 포함하는가
- `GET /api/teams/{id}/users` 가 `teamAuthority` 를 노출하고 `LEADER > MEMBER > ADMIN` 정렬을 설명하는가
- `GET /api/teams/{id}/worklogs` 가 `requestContent`, `workContent`, `aiSummary`, `importanceCode` 를 포함하는가
- `POST` / `PUT /api/teams/{id}` request 에 `leaderMembership.teamRole`, `leaderMembership.allocation`, `leaderMembership.isPrimary` 가 nested payload 로 설명되는가
- `PATCH /api/teams/{id}/status` 가 body 에 `statusCode` 만 받는가
- `POST /api/teams/{id}/members/bulk` 잔존 없이 `POST /api/teams/{id}/users/bulk` 만 남아 있는가
- `DELETE /api/teams/{id}` 가 membership 잔존과 무관하게 soft-delete 가능함을 명시하는가
