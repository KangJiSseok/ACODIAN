# Dashboard API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
대시보드 집계/지연업무/workload/내 화면 묶음 조회 계약을 정의한다. 대시보드는 원본 CRUD 가 아니라 조직·업무·알림 데이터를 조합한 읽기 모델이다.

## 2. 주요 ERD 연관
- `tb_worklog`
- `tb_user_team`
- `tb_notification`
- `tb_department`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)
- [../../AX-WMS_기획서_아키텍처가이드.md](../../AX-WMS_기획서_아키텍처가이드.md)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/dashboard/summary` | `Documented` | 대시보드 핵심 집계 요약을 조회한다. |
| `GET` | `/api/dashboard/overdue` | `Documented` | 지연 업무 목록을 집계 조회한다. |
| `GET` | `/api/dashboard/workload` | `Documented` | 사용자/팀별 workload 분포를 조회한다. |
| `GET` | `/api/dashboard/my` | `Documented` | 로그인 사용자 기준 개인 대시보드 묶음을 조회한다. |

## 5. 엔드포인트 상세

### GET /api/dashboard/summary
- 목적: 대시보드 핵심 집계 요약을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자가 자신의 조직 범위 집계를 조회한다. 상위 역할일수록 더 넓은 범위를 본다. [추론]
- 요청
  - 선택 Query: `departmentId`, `teamId`, `dateRange` [추론]
- 응답 (`data` 기준)
  - `totalWorklogs`, `completedWorklogs`, `inProgressWorklogs`, `overdueWorklogs`, `unreadNotifications`, `activeTeams` [추론]
- 요청 JSON 예시
```json
{
  "query": {
    "departmentId": 10,
    "teamId": 21,
    "dateRange": "THIS_MONTH"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "totalWorklogs": 32,
    "completedWorklogs": 18,
    "inProgressWorklogs": 10,
    "overdueWorklogs": 4,
    "unreadNotifications": 3,
    "activeTeams": 5
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `DASHBOARD_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_worklog`
  - `tb_notification`
  - `tb_team`
- 근거
  - class mapping ownership: `domain.dashboard.controller.DashboardController` / `domain.dashboard.service.DashboardService`
  - 관련 entity/context: domain.dashboard.readmodel.*, domain.dashboard.repository.jooq.DashboardJooqRepository
  - 메모: dashboard는 query-only 조합 계층이다.
  - source: checklist
  - source: class mapping
  - source: architecture guide
  - source: ERD `tb_worklog`

### GET /api/dashboard/overdue
- 목적: 지연 업무 목록을 집계 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 대시보드 조회 권한이 있는 사용자가 지연 업무만 필터링해 본다. [추론]
- 요청
  - Query: `page`, `pageSize`, `teamId`, `departmentId` [추론]
- 응답 (`data` 기준)
  - `PageResponse<OverdueWorklog>`
  - `items[*]`: `worklogId`, `title`, `dueDate`, `statusCode`, `author`, `team`, `delayDays` [추론]
- 요청 JSON 예시
```json
{
  "query": {
    "page": 1,
    "pageSize": 20,
    "departmentId": 10,
    "teamId": 21
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
        "worklogId": 9010,
        "title": "WMS 인터페이스 점검",
        "dueDate": "2026-04-15",
        "statusCode": "IN_PROGRESS",
        "author": {
          "userId": 101,
          "userName": "홍길동"
        },
        "team": {
          "teamId": 21,
          "teamName": "물류혁신TF"
        },
        "delayDays": 6
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
  - 대표 오류: `DASHBOARD_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_worklog.due_date`
  - `tb_worklog.status_code`
- 근거
  - class mapping ownership: `domain.dashboard.controller.DashboardController` / `domain.dashboard.service.DashboardService`
  - 관련 entity/context: domain.dashboard.readmodel.*, domain.dashboard.repository.jooq.DashboardJooqRepository
  - 메모: 원본 상태 변경 ownership은 다른 도메인에 남긴다.
  - source: checklist
  - source: class mapping
  - source: ADR-007
  - source: ERD `tb_worklog`

### GET /api/dashboard/workload
- 목적: 사용자/팀별 workload 분포를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 리더 이상이 팀/사용자 workload 분포를 집계 조회한다. [추론]
- 요청
  - Query: `departmentId`, `teamId`, `baseDate` [추론]
- 응답 (`data` 기준)
  - `items[*]`: `scopeType`(`TEAM`/`USER`) [추론], `scopeId`, `scopeName`, `pendingCount`, `inProgressCount`, `completedCount`, `overdueCount`
- 요청 JSON 예시
```json
{
  "query": {
    "departmentId": 10,
    "teamId": 21,
    "baseDate": "2026-04-21"
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
        "scopeType": "TEAM",
        "scopeId": 21,
        "scopeName": "물류혁신TF",
        "pendingCount": 5,
        "inProgressCount": 10,
        "completedCount": 18,
        "overdueCount": 4
      },
      {
        "scopeType": "USER",
        "scopeId": 101,
        "scopeName": "홍길동",
        "pendingCount": 1,
        "inProgressCount": 3,
        "completedCount": 6,
        "overdueCount": 0
      }
    ]
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `DASHBOARD_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_worklog`
  - `tb_team`
  - `tb_user`
- 근거
  - class mapping ownership: `domain.dashboard.controller.DashboardController` / `domain.dashboard.service.DashboardService`
  - 관련 entity/context: domain.dashboard.readmodel.*, domain.dashboard.repository.jooq.DashboardJooqRepository
  - 메모: read-heavy 조합 로직은 repository/jooq 쪽에 두는 기준과 맞춘다.
  - source: checklist
  - source: class mapping
  - source: architecture guide
  - source: ERD `tb_worklog`

### GET /api/dashboard/my
- 목적: 로그인 사용자 기준 개인 대시보드 묶음을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자가 자신의 홈 대시보드 묶음을 조회한다.
- 요청
  - 추가 파라미터 없음 또는 `dateRange` [추론]
- 응답 (`data` 기준)
  - `myPendingCount`, `myInProgressCount`, `myCompletedCount`, `myOverdueCount`, `recentNotifications[]`, `upcomingDeadlines[]` [추론]
- 요청 JSON 예시
```json
{}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "myPendingCount": 2,
    "myInProgressCount": 4,
    "myCompletedCount": 12,
    "myOverdueCount": 1,
    "recentNotifications": [
      {
        "notificationId": 801,
        "title": "업무 상태 변경"
      }
    ],
    "upcomingDeadlines": [
      {
        "worklogId": 9001,
        "title": "재고 동기화 개선",
        "dueDate": "2026-04-30"
      }
    ]
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_UNAUTHORIZED` [추론]
- ERD 연관
  - `tb_worklog`
  - `tb_notification`
  - `tb_user_team`
- 근거
  - class mapping ownership: `domain.dashboard.controller.DashboardController` / `domain.dashboard.service.DashboardService`
  - 관련 entity/context: domain.dashboard.readmodel.*, domain.dashboard.repository.jooq.DashboardJooqRepository
  - 메모: 개인화 조회지만 write ownership은 가지지 않는다.
  - source: checklist
  - source: class mapping
  - source: architecture guide
  - source: ERD `tb_notification`

## 6. 추론 메모
- 대시보드 응답은 read model 성격이므로 집계 필드명을 DTO가 아니라 의미 단위로 기술했다. [추론]
