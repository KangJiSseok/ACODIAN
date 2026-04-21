# Department API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
부서 기준 정보와 부서 소속 사용자 조회 계약을 정의한다. 본 문서는 inventory 의 legacy 단수형 path 표기와 분리해 normalized path 인 `/api/departments/*` 를 사용한다.

## 2. 주요 ERD 연관
- `tb_department`
- `tb_user`
- `tb_team`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/departments` | `Documented` | 부서 목록과 검색/정렬 결과를 조회한다. |
| `GET` | `/api/departments/{id}` | `Documented` | 단일 부서 상세와 집계 정보를 조회한다. |
| `GET` | `/api/departments/{id}/users` | `Documented` | 특정 부서 소속 사용자 목록을 페이지네이션 조회한다. |
| `POST` | `/api/departments` | `Documented` | 새 부서를 등록한다. |
| `PUT` | `/api/departments/{id}` | `Documented` | 부서 기본 정보를 수정한다. |
| `DELETE` | `/api/departments/{id}` | `Documented` | 부서를 삭제 또는 비활성 처리한다. |

## 5. 엔드포인트 상세

### GET /api/departments
- 목적: 부서 목록과 검색/정렬 결과를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상 조직 관리자 조회를 기본으로 본다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`, `keyword`
- 응답 (`data` 기준)
  - `PageResponse<DepartmentSummary>`
  - `items[*]`: `departmentId`, `departmentName`, `description`, `departmentHeadUserId`, `departmentHeadUserName`, `teamCount`, `userCount`, `createdAt`, `updatedAt`
- 요청 JSON 예시
```json
{
  "query": {
    "page": 1,
    "pageSize": 20,
    "sortBy": "departmentName",
    "sortDirection": "ASC",
    "keyword": "물류"
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
        "departmentId": 10,
        "departmentName": "물류본부",
        "description": "전사 물류 운영 총괄",
        "departmentHeadUserId": 1001,
        "departmentHeadUserName": "박본부",
        "teamCount": 3,
        "userCount": 25,
        "createdAt": "2026-04-01T09:00:00Z",
        "updatedAt": "2026-04-20T09:00:00Z"
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
  - 대표 오류: `DEPARTMENT_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_department`
  - `tb_user`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-007

### GET /api/departments/{id}
- 목적: 단일 부서 상세와 집계 정보를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 조직 관리자 조회를 기본으로 하되, 본인 소속 부서 조회 허용 가능성을 남긴다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `departmentId`, `departmentName`, `description`, `departmentHeadUserId`, `departmentHeadUserName`, `teamCount`, `userCount`
  - `teams[*]`: `teamId`, `teamName`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 10
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "departmentId": 10,
    "departmentName": "물류본부",
    "description": "전사 물류 운영 총괄",
    "departmentHeadUserId": 1001,
    "departmentHeadUserName": "박본부",
    "teamCount": 3,
    "userCount": 25,
    "teams": [
      {
        "teamId": 21,
        "teamName": "물류혁신TF"
      }
    ]
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `DEPARTMENT_NOT_FOUND` [추론]
  - 대표 오류: `DEPARTMENT_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_department`
  - `tb_team`
  - `tb_user`
- 근거
  - source: checklist
  - source: class mapping

### GET /api/departments/{id}/users
- 목적: 특정 부서 소속 사용자 목록을 페이지네이션 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 조직 관리자 또는 해당 부서 책임자가 조회한다. [추론]
- 요청
  - Path: `id`
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`, `teamId`, `positionName`, `titleName`, `roleCode`, `employmentStatus`, `keyword`
- 응답 (`data` 기준)
  - `PageResponse<UserSummary>`
  - `items[*]`: `userId`, `userName`, `departmentId`, `departmentName`, `teamId`, `teamName`, `positionName`, `titleName`, `roleCode`, `employmentStatus`
- 요청 JSON 예시
```json
{
  "path": {
    "id": 10
  },
  "query": {
    "page": 1,
    "pageSize": 20,
    "teamId": 21,
    "positionName": "과장",
    "titleName": "팀장",
    "roleCode": "TEAM_LEAD",
    "employmentStatus": "ACTIVE",
    "keyword": "홍"
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
        "userId": 101,
        "userName": "홍길동",
        "departmentId": 10,
        "departmentName": "물류본부",
        "teamId": 21,
        "teamName": "물류혁신TF",
        "positionName": "과장",
        "titleName": "팀장",
        "roleCode": "TEAM_LEAD",
        "employmentStatus": "ACTIVE"
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
  - 대표 오류: `DEPARTMENT_NOT_FOUND` [추론]
  - 대표 오류: `DEPARTMENT_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_department`
  - `tb_user`
- 근거
  - source: deep-interview spec
  - source: clarified-scope addendum
  - source: ADR-007

### POST /api/departments
- 목적: 새 부서를 등록한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR` 또는 그에 준하는 최상위 조직 관리자만 호출한다. [추론]
- 요청
  - Body: `departmentName`, `description`, `departmentHeadUserId`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "body": {
    "departmentName": "SCM혁신본부",
    "description": "공급망 혁신 조직",
    "departmentHeadUserId": 1005
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
  - 대표 오류: `DEPARTMENT_DUPLICATE_NAME` [추론]
  - 대표 오류: `USER_NOT_FOUND` [추론]
- ERD 연관
  - `tb_department`
- 근거
  - source: checklist
  - source: class mapping

### PUT /api/departments/{id}
- 목적: 부서 기본 정보를 수정한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR` 또는 해당 부서 최고 책임자가 호출한다. [추론]
- 요청
  - Path: `id`
  - Body: `departmentName`, `description`, `departmentHeadUserId`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 10
  },
  "body": {
    "departmentName": "물류운영본부",
    "description": "전사 물류 운영 총괄",
    "departmentHeadUserId": 1001
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
  - 대표 오류: `DEPARTMENT_NOT_FOUND` [추론]
  - 대표 오류: `DEPARTMENT_DUPLICATE_NAME` [추론]
- ERD 연관
  - `tb_department`
- 근거
  - source: checklist
  - source: class mapping

### DELETE /api/departments/{id}
- 목적: 부서를 삭제 또는 비활성 처리한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR` 수준 관리자만 호출한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "path": {
    "id": 10
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
  - 대표 오류: `DEPARTMENT_NOT_FOUND` [추론]
  - 대표 오류: `DEPARTMENT_HAS_ACTIVE_TEAMS` [추론]
- ERD 연관
  - `tb_department`
  - `tb_team`
- 근거
  - source: checklist
  - source: class mapping

## 6. 추론 메모
- inventory matrix 는 legacy 단수형 inventory path 를 유지하지만, 본문은 normalized `/api/departments/*` 를 canonical 로 사용한다.

