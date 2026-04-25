# Team API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
팀 도메인의 공통 skeleton 계약을 정의한다. 본 문서는 9개 canonical endpoint inventory, soft-delete / membership status semantics, `/users/me` 공유 영향 범위를 먼저 고정해 후속 branch/worktree 구현이 같은 source-of-truth 위에서 출발하도록 만드는 것을 목표로 한다.

## 2. 주요 ERD 연관
- `tb_team`
- `tb_user_team`
- `tb_department`
- `tb_user`
- `tb_worklog`

## 3. 데이터 모델 / 공통 규칙
### 3.1 `tb_team`
- `status_code`: 운영 상태 전용 (`ACTIVE`, `INACTIVE`)
- `deleted_at`: soft-delete lifecycle 전용
- `PATCH /api/teams/{id}/status` 는 `status_code` 만 변경한다.
- `DELETE /api/teams/{id}` 는 `deleted_at` 만 변경한다.
- soft-delete 된 팀이 있어도 동일 `(department_id, team_name)` 재생성을 허용한다.
- uniqueness 판단은 **활성 팀(`deleted_at IS NULL`) 기준** 으로 해석한다.

### 3.2 `tb_user_team`
- `(user_id, team_id)` 당 1행을 유지하는 single-row-status 모델을 사용한다.
- `status_code` 값은 `ACTIVE`, `LEFT` 두 종류만 사용한다.
- 재가입은 신규 row 누적이 아니라 동일 row 의 `status_code` 를 `ACTIVE` 로 되돌리는 방식으로 처리한다.
- 이번 범위에서는 `left_at` 및 별도 history table 을 도입하지 않는다.

### 3.3 `/users/me` 영향
- 현재 사용자 응답의 `teams[*]` 는 `ACTIVE` membership 만 노출한다.
- `UserService.getTeamSummaries()` 와 `UserTeamRepository.findAllByUserIdAndStatusCodeOrderByIsPrimaryDesc(...)` 가 공통 영향 범위다.

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/teams` | `Documented` | 팀 목록과 부서/상태/대표 membership 요약을 조회한다. |
| `GET` | `/api/teams/{id}` | `Documented` | 단일 팀 상세와 대표 membership 컨텍스트를 조회한다. |
| `GET` | `/api/teams/{id}/users` | `Documented` | 팀 소속 사용자 목록을 membership 상태 기준으로 조회한다. |
| `GET` | `/api/teams/{id}/worklogs` | `Documented` | 팀 기준 업무일지 목록을 조회한다. |
| `POST` | `/api/teams` | `Documented` | 새 팀과 초기 membership 을 생성한다. |
| `PUT` | `/api/teams/{id}` | `Documented` | 팀 기본 정보와 대표 membership 운영 정보를 수정한다. |
| `PATCH` | `/api/teams/{id}/status` | `Documented` | 팀 운영 상태를 전환한다. |
| `POST` | `/api/teams/{id}/users/bulk` | `Documented` | 팀 membership 을 일괄 추가/복구/LEFT 전환한다. |
| `DELETE` | `/api/teams/{id}` | `Documented` | 팀을 soft-delete 한다. |

## 5. 엔드포인트 상세

### GET /api/teams
- 목적: 팀 목록과 팀별 부서/상태/대표 membership 요약을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: `TEAM_LEAD` 이상 조회를 기본으로 하고, 상세 ownership 해석은 service policy 가 담당한다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `departmentId`, `statusCode`, `keyword`
- 응답 (`data` 기준)
  - `PageResponse<TeamSummary>`
  - `items[*]`: `teamId`, `teamName`, `departmentId`, `departmentName`, `statusCode`, `deletedAt`, `leaderUserId`, `leaderUserName`, `memberCount`
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_ACCESS_DENIED`, `TEAM_*` [추론]
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-007

### GET /api/teams/{id}
- 목적: 단일 팀 상세와 대표 membership 컨텍스트를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: `TEAM_LEAD` 이상이 기본이며, 자기 팀/부서 ownership 은 service 가 검증한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `teamId`, `teamName`, `departmentId`, `departmentName`, `statusCode`, `description`, `startDate`, `expectedEndDate`, `deletedAt`
  - `leader`: `userId`, `userName`, `positionName`, `titleName`, `teamRole`, `allocation`, `isPrimary`
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `TEAM_NOT_FOUND`, `AUTH_ACCESS_DENIED` [추론]
- 근거
  - source: checklist
  - source: class mapping

### GET /api/teams/{id}/users
- 목적: 특정 팀의 ACTIVE membership 사용자 목록을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 팀/부서 ownership 검증을 통과한 사용자만 조회한다. [추론]
- 요청
  - Path: `id`
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`, `keyword`, `statusCode`
- 응답 (`data` 기준)
  - `PageResponse<TeamUserSummary>`
  - `items[*]`: `userId`, `userName`, `positionName`, `titleName`, `teamLeader`, `teamRole`, `allocation`, `isPrimary`, `statusCode`
- 상태/에러
  - 성공: `200 OK`
- 근거
  - source: latest canonical inventory
  - source: PRD signature matrix

### GET /api/teams/{id}/worklogs
- 목적: 특정 팀 기준 업무일지 목록을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 팀/부서 ownership 검증을 통과한 사용자만 조회한다. [추론]
- 요청
  - Path: `id`
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`, `statusCode`, `keyword`
- 응답 (`data` 기준)
  - `PageResponse<TeamWorklogSummary>`
  - `items[*]`: `worklogId`, `title`, `statusCode`, `authorUserId`, `authorUserName`, `createdAt`, `updatedAt`
- 상태/에러
  - 성공: `200 OK`
- 근거
  - source: latest canonical inventory
  - source: PRD signature matrix

### POST /api/teams
- 목적: 새 팀과 초기 membership 을 생성한다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상이 호출한다. [추론]
- 요청
  - Body: `departmentId`, `teamName`, `description`, `startDate`, `expectedEndDate`, `leaderUserId`, `teamRole`, `allocation`, `isPrimary`
- 응답 (`data` 기준)
  - `teamId`
- 상태/에러
  - 성공: `201 Created` [추론]
  - 대표 오류: `TEAM_DUPLICATE_NAME`, `AUTH_ACCESS_DENIED` [추론]
- 근거
  - source: checklist
  - source: class mapping

### PUT /api/teams/{id}
- 목적: 팀 기본 정보와 대표 membership 운영 정보를 수정한다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상이 호출한다. [추론]
- 요청
  - Path: `id`
  - Body: `teamName`, `description`, `startDate`, `expectedEndDate`, `leaderUserId`, `teamRole`, `allocation`, `isPrimary`
- 응답 (`data` 기준)
  - `teamId`
- 상태/에러
  - 성공: `200 OK`
- 근거
  - source: checklist
  - source: class mapping

### PATCH /api/teams/{id}/status
- 목적: soft-delete 와 분리된 운영 상태를 전환한다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상이 호출한다. [추론]
- 요청
  - Path: `id`
  - Body: `statusCode`
- 응답 (`data` 기준)
  - `teamId`, `statusCode`
- 상태/에러
  - 성공: `200 OK`
- 근거
  - source: checklist
  - source: class mapping

### POST /api/teams/{id}/users/bulk
- 목적: 팀 membership 을 일괄 추가/복구/LEFT 전환한다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상이 호출하고, 교차 부서 참여 허용 여부는 service ownership 으로 통제한다. [추론]
- 요청
  - Path: `id`
  - Body: `items[*]`: `userId`, `teamLeader`, `teamRole`, `allocation`, `isPrimary`, `statusCode`
- 응답 (`data` 기준)
  - `teamId`, `processedCount`
- 상태/에러
  - 성공: `200 OK`
- 근거
  - source: checklist
  - source: class mapping
  - source: latest canonical inventory

### DELETE /api/teams/{id}
- 목적: 팀을 soft-delete 하고 active-team uniqueness 재사용 가능 상태로 전환한다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상이 호출한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `teamId`, `deletedAt`
- 상태/에러
  - 성공: `200 OK`
- 근거
  - source: latest canonical inventory
  - source: PRD data model rules

## 6. 공통 검수 포인트
- `POST /api/teams/{id}/members/bulk` 잔존 금지
- `POST /api/teams/{id}/users/bulk` canonical 반영
- `GET /api/teams/{id}/users`, `GET /api/teams/{id}/worklogs`, `DELETE /api/teams/{id}` 반영
- `deleted_at` / `status_code` 의미 분리 명시
- `/users/me` 는 `ACTIVE` membership 만 노출한다고 명시
