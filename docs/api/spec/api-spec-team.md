# Team API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
팀/프로젝트 그룹의 조회·등록·수정·상태 전환 및 membership 정렬 계약을 정의한다. 본 문서는 inventory 의 legacy 단수형 path 표기와 분리해 normalized path 인 `/api/teams/*` 를 사용한다. 팀장 source of truth 는 `tb_team.leader_id` 같은 별도 컬럼이 아니라 `tb_user_team.team_leader = true` 인 membership 이며, 사용자는 주 소속 부서와 다른 부서의 팀에도 참여할 수 있다. 본 문서에서 `leaderUserId`, `leaderUserName`, `leader` 객체는 모두 이 membership 에서 파생된 조회값으로 취급한다.

## 2. 주요 ERD 연관
- `tb_team`
- `tb_user_team`
- `tb_department`
- `tb_user`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/teams` | `Documented` | 팀 목록과 팀별 상태/소속 및 대표 membership 정보를 조회한다. |
| `GET` | `/api/teams/{id}` | `Documented` | 단일 팀 상세와 leader/members membership 컨텍스트를 조회한다. |
| `POST` | `/api/teams` | `Documented` | 새 팀과 초기 leader membership 정보를 생성한다. |
| `PUT` | `/api/teams/{id}` | `Documented` | 팀 기본 정보와 leader membership 정보를 수정한다. |
| `PATCH` | `/api/teams/{id}/status` | `Documented` | 팀 상태를 전환한다. |
| `POST` | `/api/teams/{id}/members/bulk` | `Documented` | 팀 멤버 membership 을 일괄 추가/삭제한다. |

## 5. 엔드포인트 상세

### GET /api/teams
- 목적: 팀 목록과 팀별 상태/소속 및 membership 기반 대표 리더 정보를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: `TEAM_LEAD` 이상이 조회하고, 상위 역할은 더 넓은 범위를 가진다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `departmentId`, `statusCode`, `keyword`
- 응답 (`data` 기준)
  - `PageResponse<TeamSummary>`
  - `items[*]`: `teamId`, `teamName`, `departmentId`, `departmentName`, `statusCode`, `description`, `startDate`, `expectedEndDate`, `leaderUserId`, `leaderUserName`, `teamLeader`, `teamRole`, `allocation`, `isPrimary`, `memberCount`
- 응답 필드 메모
  - `leaderUserId`, `leaderUserName`: `tb_user_team.team_leader = true` 인 membership row 에서 파생한 요약 필드
  - `teamLeader`, `teamRole`, `allocation`, `isPrimary`: 목록에서는 현재 leader membership 을 대표값으로 함께 노출한다. 즉, `leaderUserId` / `leaderUserName` 에 대응하는 membership 속성이다.
  - `departmentId`, `departmentName`: 팀의 소유 부서 정보이며, 개별 멤버의 주 소속 부서와 다를 수 있다.
- 요청 JSON 예시
```json
{
  "query": {
    "page": 1,
    "pageSize": 20,
    "departmentId": 10,
    "statusCode": "ACTIVE",
    "keyword": "혁신"
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
        "departmentId": 10,
        "departmentName": "물류본부",
        "statusCode": "ACTIVE",
        "description": "물류 개선 프로젝트",
        "startDate": "2026-04-01",
        "expectedEndDate": "2026-12-31",
        "leaderUserId": 101,
        "leaderUserName": "홍길동",
        "teamLeader": true,
        "teamRole": "플랫폼 총괄",
        "allocation": "주담당",
        "isPrimary": true,
        "memberCount": 8
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
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `TEAM_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_team`
  - `tb_user_team`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-007
  - source: ADR-018
  - source: architecture guide

### GET /api/teams/{id}
- 목적: 단일 팀 상세와 leader/members membership 컨텍스트를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 팀 관리자 또는 소속 사용자 조회를 허용한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `teamId`, `teamName`, `departmentId`, `departmentName`, `statusCode`, `description`, `startDate`, `expectedEndDate`
  - `leader`: `userId`, `userName`, `positionName`, `titleName`, `teamLeader`, `teamRole`, `allocation`, `isPrimary` (`members[*]` 중 `teamLeader = true` 인 membership 에서 파생한 convenience view)
  - `members[*]`: `userId`, `userName`, `positionName`, `titleName`, `teamLeader`, `teamRole`, `allocation`, `isPrimary`
- 응답 필드 메모
  - `teamLeader`: `tb_user_team.team_leader` boolean 이며, 상세 응답의 `leader` 객체와 `members[*]` 모두 같은 membership source of truth 를 따른다.
  - `teamRole`: 사용자의 팀 내 업무 역할명이다. enum 고정값이 아니라 업무 vocabulary 로 본다.
  - `allocation`: 배치 성격이다. 예: `주담당`, `겸임`.
  - `isPrimary`: 사용자 관점의 대표 소속 팀 여부다. 팀 소유 부서와는 별개의 membership 속성이다.
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
    "departmentId": 10,
    "departmentName": "물류본부",
    "statusCode": "ACTIVE",
    "description": "물류 개선 프로젝트",
    "startDate": "2026-04-01",
    "expectedEndDate": "2026-12-31",
    "leader": {
      "userId": 101,
      "userName": "홍길동",
      "positionName": "과장",
      "titleName": "팀장",
      "teamLeader": true,
      "teamRole": "플랫폼 총괄",
      "allocation": "주담당",
      "isPrimary": true
    },
    "members": [
      {
        "userId": 101,
        "userName": "홍길동",
        "positionName": "과장",
        "titleName": "팀장",
        "teamLeader": true,
        "teamRole": "플랫폼 총괄",
        "allocation": "주담당",
        "isPrimary": true
      },
      {
        "userId": 102,
        "userName": "김영희",
        "positionName": "대리",
        "titleName": "팀원",
        "teamLeader": false,
        "teamRole": "SCM 분석",
        "allocation": "겸임",
        "isPrimary": false
      }
    ]
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `TEAM_NOT_FOUND` [추론]
  - 대표 오류: `TEAM_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_team`
  - `tb_user_team`
  - `tb_user`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-018
  - source: architecture guide

### POST /api/teams
- 목적: 새 팀과 초기 leader membership 정보를 생성한다.
- 상태: `Documented`
- 권한/접근 주체: **사업부장 이상** 이 호출한다.
- 요청
  - Body: `departmentId`, `teamName`, `description`, `startDate`, `expectedEndDate`, `leaderUserId`, `teamLeader`, `teamRole`, `allocation`, `isPrimary`
- 요청 필드 메모
  - `leaderUserId`: 생성 직후 leader membership 을 만들 사용자 식별자다. 팀장 source of truth 자체는 membership 이며 `tb_team` 단독 컬럼 계약을 뜻하지 않는다.
  - `teamLeader`, `teamRole`, `allocation`, `isPrimary`: 생성 시점의 leader membership 속성이다. 일반적으로 `teamLeader` 는 `true` 여야 한다.
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "body": {
    "departmentId": 10,
    "teamName": "재고최적화TF",
    "description": "재고 최적화 프로젝트",
    "startDate": "2026-05-01",
    "expectedEndDate": "2026-11-30",
    "leaderUserId": 103,
    "teamLeader": true,
    "teamRole": "재고 전략 리드",
    "allocation": "주담당",
    "isPrimary": true
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
  - 성공: `201 Created` [추론]
  - 대표 오류: `TEAM_DUPLICATE_NAME` [추론]
  - 대표 오류: `DEPARTMENT_NOT_FOUND` [추론]
- ERD 연관
  - `tb_team`
  - `tb_user_team`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-018

### PUT /api/teams/{id}
- 목적: 팀 기본 정보와 leader membership 정보를 수정한다.
- 상태: `Documented`
- 권한/접근 주체: **사업부장 이상** 이 호출한다.
- 요청
  - Path: `id`
  - Body: `teamName`, `description`, `startDate`, `expectedEndDate`, `leaderUserId`, `teamLeader`, `teamRole`, `allocation`, `isPrimary`
- 요청 필드 메모
  - `leaderUserId`: membership 기준 팀장 지정/변경 요청으로 해석한다.
  - `teamLeader`, `teamRole`, `allocation`, `isPrimary`: leader membership 의 현재 속성값으로 본다. `leaderUserId` 변경 시 새 leader membership 에 적용한다.
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
    "description": "물류 개선 프로젝트",
    "startDate": "2026-04-01",
    "expectedEndDate": "2026-12-31",
    "leaderUserId": 101,
    "teamLeader": true,
    "teamRole": "플랫폼 총괄",
    "allocation": "주담당",
    "isPrimary": true
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
  - 성공: `200 OK` [추론]
  - 대표 오류: `TEAM_NOT_FOUND` [추론]
  - 대표 오류: `TEAM_DUPLICATE_NAME` [추론]
- ERD 연관
  - `tb_team`
  - `tb_user_team`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-018

### PATCH /api/teams/{id}/status
- 목적: 팀 상태를 전환한다.
- 상태: `Documented`
- 권한/접근 주체: **사업부장 이상** 이 호출한다.
- 요청
  - Path: `id`
  - Body: `statusCode`, `reason` [추론]
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 21
  },
  "body": {
    "statusCode": "INACTIVE",
    "reason": "프로젝트 종료"
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
  - 성공: `200 OK` [추론]
  - 대표 오류: `TEAM_NOT_FOUND` [추론]
  - 대표 오류: `TEAM_INVALID_STATUS_TRANSITION` [추론]
- ERD 연관
  - `tb_team.status_code`
- 근거
  - source: checklist
  - source: class mapping

### POST /api/teams/{id}/members/bulk
- 목적: 팀 membership 연결을 일괄 추가/삭제한다.
- 상태: `Documented`
- 권한/접근 주체: **사업부장 이상** 이 호출한다.
- 요청
  - Path: `id`
  - Body: `addUserIds[]`, `removeUserIds[]`
- 요청 필드 메모
  - 현행 bulk contract 는 membership 연결 추가/삭제만 다룬다.
  - `teamRole`, `allocation`, `isPrimary` 를 bulk body 로 함께 받을지 여부는 후속 결정으로 남긴다.
  - 사용자의 주 소속 부서와 팀의 소유 부서가 달라도 membership 생성 자체는 허용된다. 따라서 부서 불일치만으로 실패하는 계약은 이번 문서에서 채택하지 않는다.
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 21
  },
  "body": {
    "addUserIds": [103, 104],
    "removeUserIds": [107]
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
  - 성공: `200 OK` [추론]
  - 대표 오류: `TEAM_NOT_FOUND` [추론]
  - 대표 오류: `USER_NOT_FOUND` [추론]
- ERD 연관
  - `tb_user_team`
- 근거
  - source: deep-interview spec
  - source: clarified-scope addendum
  - source: ADR-018
  - source: architecture guide

## 6. 정합 메모
- inventory matrix 는 legacy 단수형 inventory path 를 유지하지만, 본문은 normalized `/api/teams/*` 를 canonical 로 사용한다.
- team domain 의 `leaderUserId`, `leaderUserName`, `leader` 는 모두 `tb_user_team.team_leader` membership 을 요약한 convenience projection 이다.
- `GET /api/teams`, `GET /api/teams/{id}`, `POST /api/teams`, `PUT /api/teams/{id}` 는 leader membership 관점에서 `teamLeader`, `teamRole`, `allocation`, `isPrimary` 를 함께 문서화한다.
- membership vocabulary 는 `teamLeader(boolean)`, `teamRole(업무 역할명)`, `allocation(배치 성격)`, `isPrimary(대표 소속 팀 여부)` 를 기준으로 user spec 과 정렬한다.
