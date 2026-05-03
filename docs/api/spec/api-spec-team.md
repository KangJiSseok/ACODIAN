# Team API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
팀 도메인의 조회/상세/멤버십/생성/부분 수정/삭제 계약을 정의한다. 본 문서는 normalized path 인 `/api/teams/*` 를 기준으로 하며,
팀 접근 범위는 역할별 부서 ownership 이 아니라 `tb_team_admin` grant 보유 팀과 호출자의 `ACTIVE tb_user_team` membership 팀의 DISTINCT 합집합으로 계산한다.

이번 계약에서 `tb_team.status_code` 는 운영 상태(`ACTIVE`, `INACTIVE`)이고, `tb_team.deleted_at` 은 soft-delete lifecycle 이다.
팀 삭제는 `deleted_at` 만 설정하며, `tb_user_team` row 가 남아 있어도 팀 삭제를 허용한다.

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
- `PATCH /api/teams/{id}` 는 null 이 아닌 필드만 반영하는 부분 업데이트 endpoint 다.
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
- `ACTIVE` membership 기준 팀당 `isLeader = true` 사용자는 최대 1명이다.
- `POST /api/teams` 와 `PATCH /api/teams/{id}` 의 `addUsers[*].isLeader` 값은 `tb_user_team.is_leader` 에 반영한다.

### 4.3 `tb_team_admin`
- 팀 관리 권한은 업무 소속 membership 과 분리된 `tb_team_admin` grant 로 모델링한다.
- `tb_team_admin` 은 `(user_id, team_id)` 당 1행을 유지하며, 동일 사용자가 같은 팀에 대해 관리 권한과 업무 소속 membership 을 동시에 가질 수 있다.
- `grantedAt` 은 관리 권한을 부여한 시점을 나타낸다.
- `tb_team_admin` 은 Team API의 admin grant 판단 기준이다.
- `POST /api/teams` 의 `addAdmin` 은 단일 `userId` 숫자 필드이며, 생성된 팀의 `tb_team_admin` 에 해당 사용자를 추가한다.
- `PATCH /api/teams/{id}` 의 `addAdmin` / `removeAdmin` 은 각각 단일 `userId` 숫자 필드이며, 대상 팀의 admin grant 를 추가/회수한다.

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
- `GET /api/teams/summary` 의 팀 상태 집계는 `deletedAt IS NULL` 인 visible team 만 대상으로 한다.
- `GET /api/teams/{id}` 의 업무일지 집계는 soft-delete 되지 않은 업무일지만 대상으로 한다.

## 5. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/teams` | `Documented` | 공통 visible scope 에 맞는 팀 목록을 페이지네이션으로 조회한다. |
| `GET` | `/api/teams/summary` | `Proposed-risk-closure` | 공통 visible scope 내 활성/비활성/전체 팀 개수 요약을 조회한다. |
| `GET` | `/api/teams/{id}` | `Documented` | 공통 visible scope 에 맞는 단일 팀 상세와 업무일지 상태 집계를 조회한다. |
| `GET` | `/api/teams/{id}/users` | `Documented` | 공통 visible scope 에 맞는 ACTIVE 팀원 전체 목록을 조회한다. |
| `POST` | `/api/teams` | `Documented` | 새 팀과 admin grant, 팀원을 함께 생성한다. |
| `PATCH` | `/api/teams/{id}` | `Documented` | 팀 기본 정보, admin grant, 팀원을 부분 업데이트한다. |
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
- 목적: 공통 visible scope 내 활성/비활성/전체 팀 개수 요약을 조회한다.
- 상태: `Proposed-risk-closure`
- 권한/접근 주체
  - 모든 role 은 `admin grant 팀 + 본인 ACTIVE membership 팀` 의 DISTINCT 합집합만 집계한다.
  - `DIRECTOR`, `DEPT_HEAD` 도 summary 조회에서는 위 공통 visible scope 를 따른다.
- 요청
  - 없음
- 응답 (`data` 기준)
  - `activeTeamCount`: DISTINCT 처리된 visible scope 내 `statusCode = ACTIVE` team 수
  - `inactiveTeamCount`: DISTINCT 처리된 visible scope 내 `statusCode = INACTIVE` team 수
  - `totalTeamCount`: DISTINCT 처리된 visible scope 내 전체 team 수 (`deletedAt IS NULL` 기준)
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
    "inactiveTeamCount": 2,
    "totalTeamCount": 5
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
- 근거
  - source: user clarified scope
  - source: pagination consistency clarification

### GET /api/teams/{id}
- 목적: 공통 visible scope 에 맞는 단일 팀 상세를 조회한다.
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
    "deptHeadAdminUserId": 101,
    "deptHeadAdminUsername": "김부장"
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
- 목적: 공통 visible scope 에 맞는 ACTIVE 팀원 전체 목록을 조회한다.
- 상태: `Documented`
- 권한/접근 주체
  - 호출자가 대상 팀의 `tb_team_admin` grant 를 보유하거나 대상 팀의 `ACTIVE tb_user_team` membership 을 보유해야 한다.
  - `DIRECTOR`, `DEPT_HEAD` 도 팀 사용자 목록 조회에서는 위 공통 visible scope 를 따른다.
- 요청
  - Path: `id`
  - Query: 없음
- 고정 정렬 정책
  1. 응답 대상은 `tb_user_team.statusCode = ACTIVE` 인 업무 소속 membership 사용자 전체다.
  2. `isLeader = true` 인 사용자를 우선한다.
  3. 동순위에서는 `userId ASC` 로 정렬한다.
- 응답 (`data` 기준)
  - `List<TeamUserSummary>`
  - `items[*]`
    - `isLeader`
    - `userId`, `userName`
    - `positionName`
    - `teamRole`
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
    "items": [
      {
        "isLeader": true,
        "userId": 101,
        "userName": "홍길동",
        "positionName": "과장",
        "teamRole": "플랫폼 총괄"
      },
      {
        "isLeader": false,
        "userId": 102,
        "userName": "김영희",
        "positionName": "대리",
        "teamRole": "WMS 운영"
      }
    ]
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

### POST /api/teams
- 목적: 새 팀과 admin grant, 팀원을 함께 생성한다.
- 상태: `Documented`
- Controller role gate: `DIRECTOR`, `DEPT_HEAD`
- Service authorization
  - 생성 전 대상 팀 grant 는 존재하지 않으므로 `DIRECTOR`, `DEPT_HEAD` 모두 팀을 생성할 수 있다.
- 요청
  - Body: `teamName`, `description`, `addAdmin`, `addUsers`, `statusCode`, `startDate`, `expectedEndDate`
- 요청 규칙
  - `addAdmin` 은 단일 `userId` 숫자 필드이다.
  - `addAdmin` 으로 지정된 사용자는 생성된 팀의 `tb_team_admin` grant 로 추가된다.
  - 생성된 팀의 `tb_team_admin` grant 로 추가되면서 DIRECTOR들도 grant로 추가되어야한다. 
  - `addUsers[*]` 는 `userId`, `isLeader`, `teamRole` 을 받는다.
  - `addUsers[*].isLeader = true` 로 온 사용자는 `tb_user_team.is_leader = true` 로 추가된다.
  - `addUsers[*].isLeader = false` 로 온 사용자는 `tb_user_team.is_leader = false` 로 추가된다.
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "body": {
    "teamName": "물류혁신TF",
    "description": "창고 자동화 및 운영 고도화",
    "addAdmin": 100,
    "addUsers": [
      {
        "userId": 102,
        "isLeader": true,
        "teamRole": "WMS 운영"
      },
      {
        "userId": 103,
        "isLeader": false,
        "teamRole": "현장 총괄"
      }
    ],
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

### PATCH /api/teams/{id}
- 목적: 팀 기본 정보, admin grant, 팀원을 부분 업데이트한다.
- 상태: `Documented`
- Controller role gate: `DIRECTOR`, `DEPT_HEAD`
- Service authorization
  - 호출자가 대상 팀의 `tb_team_admin` grant 를 보유해야 한다.
- 요청
  - Path: `id`
  - Body: `teamName`, `description`, `addAdmin`, `removeAdmin`, `addUsers`, `removeUsers`, `editUsers`, `statusCode`, `startDate`, `expectedEndDate`
- 요청 규칙
  - 각 body 필드가 `null` 이면 해당 필드는 업데이트하지 않는다.
  - `addAdmin` / `removeAdmin` 은 각각 단일 `userId` 숫자 필드이다.
  - `addAdmin` 은 대상 팀의 `tb_team_admin` grant 를 추가한다.
  - `removeAdmin` 은 대상 팀의 `tb_team_admin` grant 를 회수한다.
  - `addUsers[*]` 는 `userId`, `isLeader`, `teamRole` 을 받는다.
  - `addUsers[*].isLeader = true` 로 온 사용자는 `tb_user_team.is_leader = true` 로 추가/재활성화된다.
  - `removeUsers[*]` 는 제거할 사용자 `userId` 목록이며, 대상 membership 을 `LEFT` 상태로 전환한다.
  - `editUsers[*]` 는 `userId`, `teamRole` 을 받으며, 대상 ACTIVE membership 의 팀 내 업무 역할을 수정한다.
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
    "addAdmin": 110,
    "removeAdmin": 100,
    "addUsers": [
      {
        "userId": 102,
        "isLeader": true,
        "teamRole": "WMS 운영"
      },
      {
        "userId": 103,
        "isLeader": false,
        "teamRole": "현장 총괄"
      }
    ],
    "removeUsers": [
      101,
      100
    ],
    "editUsers": [
      {
        "userId": 105,
        "teamRole": "WMS 운영"
      }
    ],
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
- 모든 GET endpoint 의 visible scope 가 `admin grant 팀 + 본인 ACTIVE membership 팀` 의 DISTINCT 합집합으로 통일되어 있는가.
- GET endpoint 에 role별 전체/부서 ownership 접근 규칙이 남아 있지 않은가.
- `POST`, `PATCH /api/teams/{id}`, `DELETE` 의 Controller role gate 가 `DIRECTOR`, `DEPT_HEAD` 로 명시되어 있는가.
- 기존 팀을 대상으로 하는 `PATCH /api/teams/{id}`, `DELETE` 의 Service authorization 이 대상 팀 `tb_team_admin` grant 기준으로 명시되어 있는가.
- `POST /api/teams` 와 `PATCH /api/teams/{id}` request 에 `isAdmin` 필드가 없는가.
- `POST /api/teams` 와 `PATCH /api/teams/{id}` request 에 `addAdmin` 이 단일 `userId` 숫자 필드로 설명되어 있는가.
- `PATCH /api/teams/{id}` request 에 `removeAdmin` 이 단일 `userId` 숫자 필드로 설명되어 있는가.
- `POST /api/teams` 와 `PATCH /api/teams/{id}` 의 `addUsers[*].isLeader` 가 `tb_user_team.is_leader` 로 반영됨이 명시되어 있는가.
- `PATCH /api/teams/{id}` 가 null 필드는 업데이트하지 않는 부분 업데이트 endpoint 로 설명되어 있는가.
- `departmentId` 기반 request/query/example/filter/권한 검증 문구가 endpoint 상세에서 제거되어 있는가.
- `(department_id, team_name)` uniqueness 제거 이후 대체 uniqueness 규칙을 임의 확정하지 않고 후속 결정으로 분리했는가.
- `GET /api/teams` 가 순수 `PageResponse<TeamSummary>` 로 설명되어 있는가.
- `GET /api/teams/summary` 가 `activeTeamCount`, `inactiveTeamCount`, `totalTeamCount` 를 반환하는가.
- `GET /api/teams/{id}` 가 `totalWorklogCount`, `completedWorklogCount`, `inProgressWorklogCount` 를 포함하는가.
- `GET /api/teams/{id}/users` 가 페이지네이션 없이 ACTIVE 팀원 전체를 반환하고 `isLeader = true` 우선 정렬을 설명하는가.
- `DELETE /api/teams/{id}` 가 membership 잔존과 무관하게 soft-delete 가능함을 명시하는가.

## 8. 후속 범위 / 비수정 감사
- 이번 작업은 `docs/api/spec/api-spec-team.md` 및 연결된 API spec index/count 정합성 문서 수정에 한정한다.
- 실제 Controller/Service/Repository/DTO/test 구현은 이번 문서 작업에 포함하지 않는다.
- 삭제 대상 endpoint 3개는 연결 문서에서도 완전히 제거하며, `Removed` 또는 `Deprecated` row 로 남기지 않는다.
