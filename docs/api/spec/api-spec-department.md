# Department API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
부서 기준 정보와 검색/상세/등록/수정/삭제 계약을 정의한다. 조직 도메인은 feature-first 구조를 사용하며, `department` feature가 부서 기준 데이터를 소유한다.

## 2. 주요 ERD 연관
- `tb_department`
- `tb_user` (부서장/구성원 연계)

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [api-springboot-package-structure-guide.md](../api-springboot-package-structure-guide.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/department/list` | `Documented` | 부서 목록과 검색/정렬 결과를 조회하는 기준 API다. |
| `GET` | `/api/department/{id}` | `Documented` | 단일 부서 상세와 집계 정보를 조회한다. |
| `POST` | `/api/department` | `Documented` | 새 부서를 등록하는 관리 API다. |
| `PUT` | `/api/department/{id}` | `Documented` | 부서 기본 정보를 수정한다. |
| `DELETE` | `/api/department/{id}` | `Documented` | 부서 삭제 또는 비활성화 정책을 수행한다. |

## 5. 엔드포인트 상세

### GET /api/department/list
- 목적: 부서 목록과 검색/정렬 결과를 조회하는 기준 API다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상 조직 관리자 조회를 기본으로 본다. `DIRECTOR` 는 상위 권한으로 포함된다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `keyword`(부서명/설명 검색) [추론]
- 응답 (`data` 기준)
  - `PageResponse<DepartmentSummary>`
  - `items[*]`: `departmentId`, `departmentName`, `description`, `departmentHeadUserId`, `teamCount` [추론], `userCount` [추론], `createdAt`, `updatedAt`
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
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
- ERD 연관
  - `tb_department`
  - `tb_user`
- 근거
  - class mapping ownership: `domain.organization.department.controller.DepartmentController` / `domain.organization.department.service.DepartmentService`
  - 관련 entity/context: domain.organization.department.entity.Department, domain.organization.department.repository.DepartmentRepository, domain.organization.department.repository.jooq.DepartmentJooqRepository
  - 메모: organization만 feature-first 구조를 사용한다.
  - source: checklist
  - source: class mapping
  - source: ADR-002
  - source: ADR-007
  - source: ERD `tb_department`

### GET /api/department/{id}
- 목적: 단일 부서 상세와 집계 정보를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 조직 관리자 조회를 기본으로 하되, 본인 소속 부서 조회 허용 가능성을 열어 둔다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `departmentId`, `departmentName`, `description`, `departmentHeadUserId`
  - `teams` 요약 목록 또는 `teamCount` [추론]
  - `users` 집계 또는 `userCount` [추론]
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
  - class mapping ownership: `domain.organization.department.controller.DepartmentController` / `domain.organization.department.service.DepartmentService`
  - 관련 entity/context: domain.organization.department.entity.Department, domain.organization.department.repository.DepartmentRepository, domain.organization.department.repository.jooq.DepartmentJooqRepository
  - 메모: 부서 기준 정보는 department feature가 소유한다.
  - source: checklist
  - source: class mapping
  - source: ADR-002
  - source: ERD `tb_department`

### POST /api/department
- 목적: 새 부서를 등록하는 관리 API다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR` 또는 그에 준하는 최상위 조직 관리자 생성 API로 본다. [추론]
- 요청
  - Body: `departmentName`, `description`, `departmentHeadUserId` [추론]
- 응답 (`data` 기준)
  - 생성된 부서의 식별자와 기준 필드
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
  "data": {
    "departmentId": 11,
    "departmentName": "SCM혁신본부",
    "description": "공급망 혁신 조직",
    "departmentHeadUserId": 1005
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `201 Created` [추론]
  - 대표 오류: `DEPARTMENT_DUPLICATE_NAME` [추론]
  - 대표 오류: `USER_NOT_FOUND` [추론]
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
- ERD 연관
  - `tb_department.department_name`
  - `tb_department.department_head_user_id`
- 근거
  - class mapping ownership: `domain.organization.department.controller.DepartmentController` / `domain.organization.department.service.DepartmentService`
  - 관련 entity/context: domain.organization.department.entity.Department, domain.organization.department.repository.DepartmentRepository
  - 메모: 생성/수정 책임은 DepartmentService에 둔다.
  - source: checklist
  - source: class mapping
  - source: ADR-002
  - source: ERD `tb_department`

### PUT /api/department/{id}
- 목적: 부서 기본 정보를 수정한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR` 또는 해당 부서 최고 책임자 범위 수정 API로 본다. [추론]
- 요청
  - Path: `id`
  - Body: `departmentName`, `description`, `departmentHeadUserId` [추론]
- 응답 (`data` 기준)
  - 수정된 부서 기준 정보
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
  "data": {
    "departmentId": 10,
    "departmentName": "물류운영본부",
    "description": "전사 물류 운영 총괄",
    "departmentHeadUserId": 1001
  },
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
  - class mapping ownership: `domain.organization.department.controller.DepartmentController` / `domain.organization.department.service.DepartmentService`
  - 관련 entity/context: domain.organization.department.entity.Department, domain.organization.department.repository.DepartmentRepository
  - 메모: 목록/상세 조회와 쓰기 ownership이 동일 feature 안에 있다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_department`

### DELETE /api/department/{id}
- 목적: 부서 삭제 또는 비활성화 정책을 수행한다.
- 상태: `Documented`
- 권한/접근 주체: `DIRECTOR` 수준 관리자만 허용하는 관리 API로 본다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 삭제/비활성화 결과만 반환한다. [추론]
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
    "message": "부서가 비활성 처리되었습니다.",
    "departmentId": 10
  },
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
  - class mapping ownership: `domain.organization.department.controller.DepartmentController` / `domain.organization.department.service.DepartmentService`
  - 관련 entity/context: domain.organization.department.entity.Department, domain.organization.department.repository.DepartmentRepository
  - 메모: 하위 팀 존재 여부 검증은 서비스 정책으로 처리한다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_department`
  - source: architecture guide

## 6. 추론 메모
- `GET /list` 의 검색 조건은 목록 API 표준(page/sort) 외에 부서명 검색을 포함하는 것으로 기술했다. [추론]
- 삭제는 hard delete 대신 비활성/검증 플로우를 허용하는 방향으로 서술했다. [추론]
