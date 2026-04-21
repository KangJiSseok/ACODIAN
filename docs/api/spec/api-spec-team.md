# Team API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
팀/프로젝트 그룹의 조회·등록·수정·상태 전환 계약을 정의한다. 팀은 부서 소속이며 리더/멤버 관계는 `tb_user_team` 으로 표현한다.

## 2. 주요 ERD 연관
- `tb_team`
- `tb_user_team`
- `tb_department`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [api-springboot-package-structure-guide.md](../api-springboot-package-structure-guide.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/team/list` | `Documented` | 팀 목록과 팀별 상태/소속 정보를 조회한다. |
| `GET` | `/api/team/{id}` | `Documented` | 단일 팀 상세와 리더/멤버 컨텍스트를 조회한다. |
| `POST` | `/api/team` | `Documented` | 새 팀과 기본 소속 관계를 생성한다. |
| `PUT` | `/api/team/{id}` | `Documented` | 팀 기본 정보와 운영 속성을 수정한다. |
| `PATCH` | `/api/team/{id}/status` | `Documented` | 팀 상태 활성/비활성 전환을 담당한다. |

## 5. 엔드포인트 상세

### GET /api/team/list
- 목적: 팀 목록과 팀별 상태/소속 정보를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: `TEAM_LEAD` 이상이 조직 범위로 조회하고, 상위 역할은 더 넓은 범위를 가진다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `departmentId`, `statusCode`, `keyword` [추론]
- 응답 (`data` 기준)
  - `PageResponse<TeamSummary>`
  - `items[*]`: `teamId`, `departmentId`, `teamName`, `statusCode`, `description`, `startDate`, `expectedEndDate`, `leaderUserId` [추론], `memberCount` [추론]
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
        "departmentId": 10,
        "teamName": "물류혁신TF",
        "statusCode": "ACTIVE",
        "description": "물류 개선 프로젝트",
        "startDate": "2026-04-01",
        "expectedEndDate": "2026-12-31",
        "leaderUserId": 101,
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
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
- ERD 연관
  - `tb_team`
  - `tb_user_team`
- 근거
  - class mapping ownership: `domain.organization.team.controller.TeamController` / `domain.organization.team.service.TeamService`
  - 관련 entity/context: domain.organization.team.entity.Team, domain.organization.team.repository.TeamRepository, domain.organization.team.repository.jooq.TeamJooqRepository
  - 메모: 팀 read/write ownership은 team feature가 가진다.
  - source: checklist
  - source: class mapping
  - source: ADR-007
  - source: ERD `tb_team`

### GET /api/team/{id}
- 목적: 단일 팀 상세와 리더/멤버 컨텍스트를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 팀 관리자 또는 소속 사용자 조회를 허용하는 상세 API로 본다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `teamId`, `departmentId`, `teamName`, `statusCode`, `description`, `startDate`, `expectedEndDate`
  - `leader` 요약, `members` 요약 목록 [추론]
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
    "departmentId": 10,
    "teamName": "물류혁신TF",
    "statusCode": "ACTIVE",
    "description": "물류 개선 프로젝트",
    "startDate": "2026-04-01",
    "expectedEndDate": "2026-12-31",
    "leader": {
      "userId": 101,
      "userName": "홍길동"
    },
    "members": [
      {
        "userId": 101,
        "userName": "홍길동",
        "teamRole": "LEADER"
      },
      {
        "userId": 102,
        "userName": "김영희",
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
  - class mapping ownership: `domain.organization.team.controller.TeamController` / `domain.organization.team.service.TeamService`
  - 관련 entity/context: domain.organization.team.entity.Team, domain.organization.team.entity.UserTeam, domain.organization.team.repository.TeamRepository
  - 메모: 팀 상세는 소속 관계(UserTeam)를 함께 참고한다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_team`
  - source: ERD `tb_user_team`

### POST /api/team
- 목적: 새 팀과 기본 소속 관계를 생성한다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상이 자신의 부서 범위에서 생성하는 API로 본다. [추론]
- 요청
  - Body: `departmentId`, `teamName`, `description`, `startDate`, `expectedEndDate`, `leaderUserId` [추론]
  - 선택적으로 초기 멤버 목록 [추론]
- 응답 (`data` 기준)
  - 생성된 팀 기준 정보와 초기 소속 결과 [추론]
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
  "data": {
    "teamId": 22,
    "departmentId": 10,
    "teamName": "재고최적화TF",
    "statusCode": "ACTIVE",
    "leaderUserId": 103
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `201 Created` [추론]
  - 대표 오류: `TEAM_DUPLICATE_NAME` [추론]
  - 대표 오류: `DEPARTMENT_NOT_FOUND` [추론]
  - 대표 오류: `USER_DEPARTMENT_MISMATCH` [추론]
- ERD 연관
  - `tb_team`
  - `tb_user_team`
- 근거
  - class mapping ownership: `domain.organization.team.controller.TeamController` / `domain.organization.team.service.TeamService`
  - 관련 entity/context: domain.organization.team.entity.Team, domain.organization.team.entity.UserTeam, domain.organization.team.repository.TeamRepository, domain.organization.team.repository.UserTeamRepository
  - 메모: user-team 관계 ownership도 team feature에 둔다.
  - source: checklist
  - source: class mapping
  - source: ADR-002
  - source: ERD `tb_team`

### PUT /api/team/{id}
- 목적: 팀 기본 정보와 운영 속성을 수정한다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상 또는 해당 팀 책임자 수정 API로 본다. [추론]
- 요청
  - Path: `id`
  - Body: `teamName`, `description`, `statusCode`, `startDate`, `expectedEndDate`, `leaderUserId` [추론]
- 응답 (`data` 기준)
  - 수정된 팀 기준 정보
- 요청 JSON 예시
```json
{
  "path": {
    "id": 21
  },
  "body": {
    "teamName": "물류혁신TF",
    "description": "물류 개선 프로젝트",
    "statusCode": "ACTIVE",
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
  "data": {
    "teamId": 21,
    "teamName": "물류혁신TF",
    "statusCode": "ACTIVE",
    "leaderUserId": 101
  },
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
  - class mapping ownership: `domain.organization.team.controller.TeamController` / `domain.organization.team.service.TeamService`
  - 관련 entity/context: domain.organization.team.entity.Team, domain.organization.team.repository.TeamRepository, domain.organization.team.repository.jooq.TeamJooqRepository
  - 메모: feature-first 구조에서 team 관련 조회/수정 책임을 한곳에 둔다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_team`

### PATCH /api/team/{id}/status
- 목적: 팀 상태 활성/비활성 전환을 담당한다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상이 활성/비활성 상태를 전환한다. [추론]
- 요청
  - Path: `id`
  - Body: `statusCode` (`ACTIVE`/`INACTIVE`), `reason` [추론]
- 응답 (`data` 기준)
  - 변경 후 `statusCode` 와 반영 시각 [추론]
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
  "data": {
    "teamId": 21,
    "statusCode": "INACTIVE",
    "updatedAt": "2026-04-21T03:00:00Z"
  },
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
  - class mapping ownership: `domain.organization.team.controller.TeamController` / `domain.organization.team.service.TeamService`
  - 관련 entity/context: domain.organization.team.entity.Team, domain.organization.team.repository.TeamRepository
  - 메모: 상태 enum은 domain.organization.team.TeamStatus와 연결된다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_team`

## 6. 추론 메모
- 팀 상세 응답의 `leader`/`members` 구조는 `tb_user_team.team_role` 관계를 기반으로 요약했다. [추론]
- `PATCH /status` 는 상태 enum을 `ACTIVE`/`INACTIVE` 로 제한했다. [추론]
