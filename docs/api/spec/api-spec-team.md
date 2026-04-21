# Team API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
팀/프로젝트 그룹의 조회·등록·수정·상태 전환 계약을 정의한다. 본 문서는 inventory 의 legacy 단수형 path 표기와 분리해 normalized path 인 `/api/teams/*` 를 사용한다.

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
| `GET` | `/api/teams` | `Documented` | 팀 목록과 팀별 상태/소속 정보를 조회한다. |
| `GET` | `/api/teams/{id}` | `Documented` | 단일 팀 상세와 리더/멤버 컨텍스트를 조회한다. |
| `POST` | `/api/teams` | `Documented` | 새 팀을 생성한다. |
| `PUT` | `/api/teams/{id}` | `Documented` | 팀 기본 정보를 수정한다. |
| `PATCH` | `/api/teams/{id}/status` | `Documented` | 팀 상태를 전환한다. |
| `POST` | `/api/teams/{id}/members/bulk` | `Documented` | 팀 멤버를 일괄 추가/삭제한다. |

## 5. 엔드포인트 상세

### GET /api/teams
- 목적: 팀 목록과 팀별 상태/소속 정보를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: `TEAM_LEAD` 이상이 조회하고, 상위 역할은 더 넓은 범위를 가진다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `departmentId`, `statusCode`, `keyword`
- 응답 (`data` 기준)
  - `PageResponse<TeamSummary>`
  - `items[*]`: `teamId`, `teamName`, `departmentId`, `departmentName`, `statusCode`, `description`, `startDate`, `expectedEndDate`, `leaderUserId`, `leaderUserName`, `memberCount`
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

### GET /api/teams/{id}
- 목적: 단일 팀 상세와 리더/멤버 컨텍스트를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 팀 관리자 또는 소속 사용자 조회를 허용한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `teamId`, `teamName`, `departmentId`, `departmentName`, `statusCode`, `description`, `startDate`, `expectedEndDate`
  - `leader`: `userId`, `userName`, `positionName`, `titleName`
  - `members[*]`: `userId`, `userName`, `positionName`, `titleName`, `teamRole`
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
      "titleName": "팀장"
    },
    "members": [
      {
        "userId": 101,
        "userName": "홍길동",
        "positionName": "과장",
        "titleName": "팀장",
        "teamRole": "LEADER"
      },
      {
        "userId": 102,
        "userName": "김영희",
        "positionName": "대리",
        "titleName": "팀원",
        "teamRole": "MEMBER"
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

### POST /api/teams
- 목적: 새 팀을 생성한다.
- 상태: `Documented`
- 권한/접근 주체: **사업부장 이상** 이 호출한다.
- 요청
  - Body: `departmentId`, `teamName`, `description`, `startDate`, `expectedEndDate`, `leaderUserId`
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
    "leaderUserId": 103
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
- 근거
  - source: checklist
  - source: class mapping

### PUT /api/teams/{id}
- 목적: 팀 기본 정보를 수정한다.
- 상태: `Documented`
- 권한/접근 주체: **사업부장 이상** 이 호출한다.
- 요청
  - Path: `id`
  - Body: `teamName`, `description`, `startDate`, `expectedEndDate`, `leaderUserId`
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
    "leaderUserId": 101
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
- 근거
  - source: checklist
  - source: class mapping

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
- 목적: 팀 멤버를 일괄 추가/삭제한다.
- 상태: `Documented`
- 권한/접근 주체: **사업부장 이상** 이 호출한다.
- 요청
  - Path: `id`
  - Body: `addUserIds[]`, `removeUserIds[]`
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
  - 대표 오류: `USER_DEPARTMENT_MISMATCH` [추론]
- ERD 연관
  - `tb_user_team`
- 근거
  - source: deep-interview spec
  - source: clarified-scope addendum

## 6. 추론 메모
- inventory matrix 는 legacy 단수형 inventory path 를 유지하지만, 본문은 normalized `/api/teams/*` 를 canonical 로 사용한다.

