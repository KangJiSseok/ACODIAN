# Team API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
팀 도메인의 조회/상세/멤버십/상태/삭제 계약을 정의한다. 본 문서는 normalized path 인 `/api/teams/*` 를 기준으로 하며,
팀 접근 범위는 역할별 부서 ownership 이 아니라 `tb_team_admin` grant 보유 팀과 호출자의 `ACTIVE tb_user_team` membership 팀의 DISTINCT 합집합으로 계산한다.

이번 계약에서 `tb_team.status_code` 는 운영 상태(`ACTIVE`, `INACTIVE`)이고, `tb_team.deleted_at` 은 soft-delete lifecycle 이다.
팀 삭제는 `deleted_at` 만 설정하며, `tb_user_team` row 가 남아 있어도 팀 삭제를 허용한다.

`DIRECTOR` 는 시스템에 1명만 존재한다는 전제를 둔다. 이 전제는 `DEPT_HEAD` 가 팀을 생성할 때 단일 `DIRECTOR` 에게 admin grant 를 함께 부여하기 위한 기준이다.

## 2. 주요 ERD 연관
- `tb_team`
- `tb_user_team`
- `tb_team_admin`
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
- `tb_team.department_id` 는 제거 대상으로 계획한다.
- `departmentId` 기반 request/query/example/filter/권한 검증 문구는 Team API 계약에서 제거한다.
- 기존 `(department_id, team_name)` uniqueness 규칙은 `department_id` 제거와 함께 폐기한다.
- 대체 uniqueness 규칙은 이번 문서 수정에서 임의 확정하지 않고 후속 ADR/구현 계획에서 결정한다.

### 4.2 `tb_user_team`
- `(user_id, team_id)` 당 1행을 유지하는 single-row membership 모델을 사용한다.
- membership 의 운영 상태는 `ACTIVE`, `LEFT` 두 종류만 사용한다.
- 재가입은 신규 row 누적이 아니라 동일 row 를 `ACTIVE` 로 되돌리는 방식으로 처리한다.
- membership 리더 여부는 업무 소속 의미의 `isLeader` boolean 으로 표현한다.
- 팀 내 업무 역할은 `teamRole`, 배치 성격은 `allocation`, 대표 소속 여부는 `isPrimary` 로 표현한다.
- `ACTIVE` membership 기준 팀당 `LEADER` 는 최대 1명이다.

### 4.3 `tb_team_admin`
- 팀 관리 권한은 업무 소속 membership 과 분리된 `tb_team_admin` grant 로 모델링한다.
- `tb_team_admin` 은 `(user_id, team_id)` 당 1행을 유지하며, 동일 사용자가 같은 팀에 대해 관리 권한과 업무 소속 membership을 동시에 가질 수 있다.
- `grantedAt` 은 관리 권한을 부여한 시점을 나타낸다.
- `tb_team_admin` 은 Team API의 admin grant 판단 기준이다.
- `DIRECTOR` 는 시스템에 1명만 존재한다.
- `DIRECTOR` 가 생성한 팀은 생성자 본인에게 admin grant 를 부여한다.
- `DEPT_HEAD` 가 생성한 팀은 단일 `DIRECTOR` 와 생성자 본인에게 admin grant 를 부여한다.
- `tb_team_admin` Flyway migration, `TeamAdmin` managed entity/repository, 로컬 시드 grant 보정, `GET /api/teams` read path 는 현재 구현 범위에 포함한다.
- grant 부여/회수 전용 API와 `GET /api/teams` 외 Team endpoint 의 실제 동작 구현은 후속 작업으로 분리한다.

### 4.4 공통 visible scope
- 모든 GET endpoint 의 visible scope 는 다음 두 집합의 DISTINCT 합집합이다.
  1. 호출자가 `tb_team_admin` grant 를 보유한 팀
  2. 호출자가 `ACTIVE tb_user_team` membership 을 보유한 팀
- `DIRECTOR`, `DEPT_HEAD`, `TEAM_LEAD`, `MEMBER` 모두 GET endpoint 에서는 동일한 공통 visible scope 를 따른다.
- access token 의 role hierarchy 는 Controller 진입 허용을 위한 기준이고, GET visible scope 는 Service 계층의 리소스 기준 authorization 결과다.
- Controller 는 인증/role gate 를 수행하고, Service 는 실제 팀 리소스에 대한 admin grant 또는 ACTIVE membership 보유 여부를 검증한다.

### 4.5 집계/정렬 공통 해석
- 별도 언급이 없으면 team 목록의 `memberCount` 는 해당 팀의 `ACTIVE` membership 수다.
- visible scope 는 `tb_team_admin` grant 팀과 `ACTIVE tb_user_team` membership 팀의 합집합으로 계산하며 팀 집합은 먼저 **DISTINCT team** 기준으로 정규화한다.
- 목록 pagination 의 `items`, `totalCount`, `totalPages` 는 정규화된 visible scope 기준으로 계산한다.
- 별도 언급이 없으면 user 집계는 정규화된 visible scope 내 **DISTINCT user 수** 기준이다.
- `GET /api/teams/{id}` 및 `GET /api/teams/{id}/worklogs` 는 soft-delete 되지 않은 업무일지만 대상으로 한다.

## 5. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/teams` | `Documented` | 공통 visible scope 에 맞는 팀 목록을 페이지네이션으로 조회한다. |
| `GET` | `/api/teams/summary` | `Proposed-risk-closure` | 페이지네이션 응답과 분리된 상단 집계 요약을 조회한다. |
| `GET` | `/api/teams/{id}` | `Documented` | 공통 visible scope 에 맞는 단일 팀 상세와 업무일지 집계를 조회한다. |
| `GET` | `/api/teams/{id}/users` | `Documented` | 공통 visible scope 에 맞는 팀 소속 사용자 목록을 조회한다. |
| `GET` | `/api/teams/{id}/worklogs` | `Documented` | 공통 visible scope 에 맞는 팀 업무일지 목록을 조회한다. |
| `POST` | `/api/teams` | `Documented` | 새 팀을 생성한다. |
| `PUT` | `/api/teams/{id}` | `Documented` | 팀 기본 정보를 수정한다. |
| `PATCH` | `/api/teams/{id}/status` | `Documented` | 팀 운영 상태를 전환한다. |
| `POST` | `/api/teams/{id}/users/bulk` | `Documented` | 팀 사용자 추가/제거를 일괄 반영한다. |
| `DELETE` | `/api/teams/{id}` | `Documented` | 팀을 soft-delete 한다. |

## 6. 엔드포인트 상세

### GET /api/teams
- 목적: 공통 visible scope 에 맞는 팀 목록을 페이지네이션으로 조회한다.
- 상태: `Documented`
- 권한/접근 주체
  - 모든 role 은 `admin grant 팀 + 본인 ACTIVE membership 팀` 의 DISTINCT 합집합만 조회한다.
  - `DIRECTOR`, `DEPT_HEAD` 도 GET 목록 조회에서는 위 공통 visible scope 를 따른다.
- 요청
  - Query: `page`, `pageSize`
  - `sortBy`, `sortDirection` 은 공개 query 로 받지 않고 아래 고정 정렬 정책을 사용한다.
- 고정 정렬 정책
  1. `statusCode = ACTIVE` 팀 우선
  2. 호출자의 `myIsLeader = true` 인 팀 우선
  3. 호출자의 `allocation` 이 `주담당` 인 팀 우선 (`allocation` 저장/응답 vocabulary 는 `주담당`, `겸임` 두 값만 사용한다.)
  4. 호출자의 `isPrimary = true` 인 팀 우선
- 응답 (`data` 기준)
  - `PageResponse<TeamSummary>`
  - `items[*]`
    - `teamId`, `teamName`, `statusCode`
    - `description`
    - `teamLeaderId`, `teamLeaderName`
    - `memberCount`
    - `myIsLeader`
    - `teamRole`, `allocation`, `isPrimary`
    - `startDate`, `expectedEndDate`
- 요청 JSON 예시
```json
{
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
        "teamId": 21,
        "teamName": "물류혁신TF",
        "statusCode": "ACTIVE",
        "description": "창고 자동화 개선 전담",
        "teamLeaderId": 101,
        "teamLeaderName": "홍길동",
        "memberCount": 6,
        "myIsLeader": true,
        "teamRole": "플랫폼 총괄",
        "allocation": "주담당",
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
  - `tb_team_admin`
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
  - 모든 role 은 `admin grant 팀 + 본인 ACTIVE membership 팀` 의 DISTINCT 합집합만 집계한다.
  - `DIRECTOR`, `DEPT_HEAD` 도 summary 조회에서는 위 공통 visible scope 를 따른다.
- 요청
  - 없음
- 응답 (`data` 기준)
  - `activeTeamCount`: DISTINCT 처리된 visible scope 내 `ACTIVE` team 수
  - `totalTeamCount`: DISTINCT 처리된 visible scope 내 전체 team 수 (`deletedAt IS NULL` 기준)
  - `activeUserCount`: DISTINCT 처리된 visible scope 내 `employmentStatus = ACTIVE` 인 DISTINCT user 수
  - `activeTeamUserCount`: DISTINCT 처리된 visible scope 내 `ACTIVE` team 에 속한 DISTINCT user 수
  - `allTeamUserCount`: DISTINCT 처리된 visible scope 내 `ACTIVE` + `INACTIVE` team 에 속한 DISTINCT user 수
- 요청 JSON 예시
```json
{
  "query": {}
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
  - `tb_team_admin`
  - `tb_user_team`
  - `tb_user`
- 근거
  - source: user clarified scope
  - source: pagination consistency clarification

### GET /api/teams/{id}
- 목적: 공통 visible scope 에 맞는 단일 팀 상세와 업무일지 집계를 조회한다.
- 상태: `Documented`
- 권한/접근 주체
  - 호출자가 대상 팀의 `tb_team_admin` grant 를 보유하거나 대상 팀의 `ACTIVE tb_user_team` membership 을 보유해야 한다.
  - `DIRECTOR`, `DEPT_HEAD` 도 단일 팀 상세 조회에서는 위 공통 visible scope 를 따른다.
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `teamId`, `teamName`, `statusCode`, `description`
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
  - `tb_team_admin`
  - `tb_user_team`
  - `tb_worklog`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### GET /api/teams/{id}/users
- 목적: 공통 visible scope 에 맞는 팀 소속 사용자 목록을 조회한다.
- 상태: `Documented`
- 권한/접근 주체
  - 호출자가 대상 팀의 `tb_team_admin` grant 를 보유하거나 대상 팀의 `ACTIVE tb_user_team` membership 을 보유해야 한다.
  - `DIRECTOR`, `DEPT_HEAD` 도 팀 사용자 목록 조회에서는 위 공통 visible scope 를 따른다.
- 요청
  - Path: `id`
  - Query: `page`, `pageSize`
- 고정 정렬 정책
  1. 응답 대상은 `tb_user_team` 의 업무 소속 membership 사용자다.
  2. isLeader = true` 인 사용자를 우선한다.
  3. 동순위에서는 `userId ASC` 로 정렬한다.
- 응답 (`data` 기준)
  - `PageResponse<TeamUserSummary>`
  - `items[*]`
    - `isLeader`
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
        "isLeader": true,
        "userId": 101,
        "userName": "홍길동",
        "positionName": "과장",
        "email": "hong@axwms.com",
        "teamRole": "플랫폼 총괄",
        "allocation": "주담당"
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
  - `tb_user_team`
  - `tb_team_admin`
  - `tb_user`
  - `tb_team`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### GET /api/teams/{id}/worklogs
- 목적: 공통 visible scope 에 맞는 팀 업무일지 목록을 조회한다.
- 상태: `Documented`
- 권한/접근 주체
  - 호출자가 대상 팀의 `tb_team_admin` grant 를 보유하거나 대상 팀의 `ACTIVE tb_user_team` membership 을 보유해야 한다.
  - `DIRECTOR`, `DEPT_HEAD` 도 팀 업무일지 목록 조회에서는 위 공통 visible scope 를 따른다.
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
  - `tb_team_admin`
  - `tb_user_team`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### POST /api/teams
- 목적: 새 팀을 생성한다.
- 상태: `Documented`
- Controller role gate: `DIRECTOR`, `DEPT_HEAD`
- Service authorization
  - 생성 전 대상 팀 grant 는 존재하지 않으므로 `DIRECTOR`, `DEPT_HEAD` 모두 부서 제한 없이 팀을 생성할 수 있다.
- 요청
  - Body: `teamName`, `description`, `leaderUserId`, `leaderMembership`, `statusCode`, `startDate`, `expectedEndDate`
- 요청 규칙
  - `leaderMembership` 에는 `teamRole`, `allocation`, `isPrimary` 를 nested payload 로 받는다.
  - create/update 는 `isLeader` 를 직접 받지 않으며 `leaderUserId` 대상 membership 을 항상 `isLeader = true` 로 설정한다.
  - `DIRECTOR` 생성 시 생성자 본인에게 `tb_team_admin` grant 를 부여한다.
  - `DEPT_HEAD` 생성 시 시스템의 단일 `DIRECTOR` 와 생성자 본인에게 `tb_team_admin` grant 를 부여한다.
  - `leaderMembership.joinedAt` 은 별도 입력으로 받지 않고 생성 시점 기본값을 사용한다.
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "body": {
    "teamName": "물류혁신TF",
    "description": "창고 자동화 개선 전담",
    "leaderUserId": 101,
    "leaderMembership": {
      "teamRole": "플랫폼 총괄",
      "allocation": "주담당",
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
  - `tb_team_admin`
  - `tb_user`
  - `tb_user_team`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### PUT /api/teams/{id}
- 목적: 팀 기본 정보를 수정한다.
- 상태: `Documented`
- Controller role gate: `DIRECTOR`, `DEPT_HEAD`
- Service authorization
  - 호출자가 대상 팀의 `tb_team_admin` grant 를 보유해야 한다.
- 요청
  - Path: `id`
  - Body: `teamName`, `description`, `leaderUserId`, `leaderMembership`, `statusCode`, `startDate`, `expectedEndDate`
- 요청 규칙
  - `leaderMembership` 에는 `teamRole`, `allocation`, `isPrimary` 를 nested payload 로 받는다.
  - `leaderUserId` 를 받으면 대상 `(user_id, team_id)` membership 을 재활성화/갱신 후 `isLeader = true` 로 승격한다.
  - 기존 ACTIVE leader membership 은 같은 트랜잭션에서 `isLeader = false` 로 강등한다.
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 21
  },
  "body": {
    "teamName": "물류혁신TF",
    "description": "창고 자동화 및 운영 고도화",
    "leaderUserId": 101,
    "leaderMembership": {
      "teamRole": "플랫폼 총괄",
      "allocation": "주담당",
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
  - `tb_team_admin`
  - `tb_user`
  - `tb_user_team`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### PATCH /api/teams/{id}/status
- 목적: soft-delete 와 분리된 팀 운영 상태를 전환한다.
- 상태: `Documented`
- Controller role gate: `DIRECTOR`, `DEPT_HEAD`
- Service authorization
  - 호출자가 대상 팀의 `tb_team_admin` grant 를 보유해야 한다.
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
  - `tb_team_admin`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### POST /api/teams/{id}/users/bulk
- 목적: 팀 사용자 추가/제거를 일괄 반영한다.
- 상태: `Documented`
- 권한/접근 주체
  - 인증된 사용자 principal 을 Service 에 전달한다.
  - Service 는 대상 팀 기준으로 `tb_team_admin` grant 보유자 또는 대상 팀의 `ACTIVE tb_user_team.isLeader = true` membership 사용자인지 검증한다.
  - 여기서 leader 는 access token 의 `TEAM_LEAD` role 이 아니라 팀별 membership 의 `isLeader = true` 의미다.
- 요청
  - Path: `id`
  - Body
    - `addUsers[*]`: `userId`, `isLeader`, `teamRole`, `allocation`, `isPrimary`, `joinedAt`
    - `removeUserIds[*]`: 제거 대상 `userId`
- 요청 규칙
  - 기존 legacy members bulk path 는 사용하지 않는다.
  - `isLeader` 는 필수이며 팀 대표 여부로 사용한다.
  - 추가 대상 사용자가 기존 `LEFT` membership row 를 가지면 복구 처리한다.
  - 팀 관리 권한 부여/회수는 `tb_user_team` membership 이 아니라 `tb_team_admin` grant 를 사용하는 별도 후속 유스케이스로 구현한다.
  - `GET /api/teams/{id}/users` 에서 admin-only 사용자는 목록 응답에서 제외될 수 있지만, 이 bulk endpoint 호출 권한으로는 `tb_team_admin` grant 를 인정한다.
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
        "isLeader": false,
        "teamRole": "WMS 운영",
        "allocation": "겸임",
        "isPrimary": false,
        "joinedAt": "2026-04-10"
      },
      {
        "userId": 103,
        "isLeader": true,
        "teamRole": "현장 총괄",
        "allocation": "주담당",
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
  - `tb_team_admin`
  - `tb_team`
  - `tb_user`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

### DELETE /api/teams/{id}
- 목적: 팀을 soft-delete 한다.
- 상태: `Documented`
- Controller role gate: `DIRECTOR`, `DEPT_HEAD`
- Service authorization
  - 호출자가 대상 팀의 `tb_team_admin` grant 를 보유해야 한다.
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
  - `tb_team_admin`
  - `tb_user_team`
- 근거
  - source: user clarified scope
  - source: checklist
  - source: class mapping

## 7. 공통 검수 포인트
- `DIRECTOR` 단일성 전제가 명시되어 있는가.
- 모든 GET endpoint 의 visible scope 가 `admin grant 팀 + 본인 ACTIVE membership 팀` 의 DISTINCT 합집합으로 통일되어 있는가.
- GET endpoint 에 role별 전체/부서 ownership 접근 규칙이 남아 있지 않은가.
- `POST`, `PUT`, `PATCH /api/teams/{id}/status`, `DELETE` 의 Controller role gate 가 `DIRECTOR`, `DEPT_HEAD` 로 명시되어 있는가.
- 기존 팀을 대상으로 하는 `PUT`, `PATCH /api/teams/{id}/status`, `DELETE` 의 Service authorization 이 대상 팀 `tb_team_admin` grant 기준으로 명시되어 있는가.
- `POST /api/teams` 의 생성 후 grant 부여 정책이 생성자 role 별로 분리되어 있는가.
- `POST /api/teams/{id}/users/bulk` 의 호출 권한이 대상 팀 admin grant 또는 대상 팀의 `ACTIVE tb_user_team.isLeader = true` membership 기준으로 명시되어 있는가.
- `POST /api/teams/{id}/users/bulk` 의 leader 가 token role `TEAM_LEAD` 와 다른 membership 속성임이 명시되어 있는가.
- `POST /api/teams/{id}/users/bulk` 에서 admin-only 사용자는 목록 응답에서 제외될 수 있으나 호출 권한으로는 admin grant 를 인정한다는 구분이 유지되어 있는가.
- `departmentId` 기반 request/query/example/filter/권한 검증 문구가 endpoint 상세에서 제거되어 있는가.
- 부서 표시용 응답 필드는 이번 계약에서 제거되어 있으며, 재도입 시 출처와 권한/필터 기준이 아님을 별도 명시해야 하는가.
- `(department_id, team_name)` uniqueness 제거 이후 대체 uniqueness 규칙을 임의 확정하지 않고 후속 결정으로 분리했는가.
- `GET /api/teams` 가 순수 `PageResponse<TeamSummary>` 로 설명되어 있는가.
- `GET /api/teams` 가 `myIsLeader` 를 노출하고 기존 권한 문자열 필드를 설명하지 않는가.
- `GET /api/teams/summary` 가 상단 집계를 별도 endpoint 로 설명하는가.
- `GET /api/teams/{id}` 가 soft-delete 되지 않은 업무일지 집계를 포함하는가.
- `GET /api/teams/{id}/users` 가 ADMIN-only grant 사용자를 제외하고 `isLeader = true` 우선 정렬을 설명하는가.
- `GET /api/teams/{id}/worklogs` 가 `requestContent`, `workContent`, `aiSummary`, `importanceCode` 를 포함하는가.
- `POST` / `PUT /api/teams/{id}` request 에 `leaderMembership.teamRole`, `leaderMembership.allocation`, `leaderMembership.isPrimary` 가 nested payload 로 설명되는가.
- `PATCH /api/teams/{id}/status` 가 body 에 `statusCode` 만 받는가.
- legacy members bulk path 없이 `POST /api/teams/{id}/users/bulk` 만 남아 있는가.
- `DELETE /api/teams/{id}` 가 membership 잔존과 무관하게 soft-delete 가능함을 명시하는가.

## 8. 후속 범위 / 비수정 감사
- 현재 구현 범위에는 `tb_team_admin` Flyway migration, `TeamAdmin` managed entity/repository, 로컬 시드 grant 보정, `GET /api/teams` Controller/Service/Repository/JOOQ/DTO/test code 변경이 포함된다.
- `GET /api/teams` 외 실제 endpoint 구현, grant 부여/회수 API, mutation 권한 gate, 팀 상세/사용자/업무일지/생성·수정·상태변경·bulk·삭제 동작은 후속 작업으로 분리한다.
- `.omx/specs/deep-interview-api-spec-team-auth-plan.md` 의 과거 Decision Boundary 중 DEPT_HEAD 의 부서 밖 확대를 막는 취지의 항목은 최신 Team API 권한 정정에 의해 superseded 된 stale bullet 로 취급한다.
- `docs/api/spec/api-spec-common.md` 와 `docs/api/spec/api-spec-index.md` 는 이번 작업에서 수정하지 않는다. 현재 Team endpoint index 와 공통 role hierarchy 는 직접 권한 충돌을 만들지 않으므로 비수정 cross-doc audit 대상으로만 남긴다.
- `api-spec-index.md` 의 `/api/teams/summary` 상태 표기 불일치는 auth/access 변경과 직접 관련 없는 후속 문서 정합성 검토 항목으로 기록한다.
