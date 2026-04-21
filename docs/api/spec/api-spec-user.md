# User API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
사용자 계정, 조직 소속 문맥, 역할/재직 상태를 관리하는 계약을 정의한다. 사용자 기본 본체는 `tb_user`, 팀 소속은 `tb_user_team` 을 통해 조회된다.

## 2. 주요 ERD 연관
- `tb_user`
- `tb_user_team`
- `tb_department`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)
- [code-convention.yaml](../code-convention.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/user/list` | `Documented` | 사용자 목록과 조직/권한 필터 결과를 조회한다. |
| `GET` | `/api/user/{id}` | `Documented` | 단일 사용자 상세와 조직 소속 문맥을 조회한다. |
| `POST` | `/api/user` | `Documented` | 새 사용자 계정과 초기 조직 문맥을 등록한다. |
| `PUT` | `/api/user/{id}` | `Documented` | 사용자 기본 정보와 역할/상태를 수정한다. |
| `DELETE` | `/api/user/{id}` | `Documented` | 사용자를 하드 삭제가 아니라 퇴직/비활성 처리하는 관리 API다. |

## 5. 엔드포인트 상세

### GET /api/user/list
- 목적: 사용자 목록과 조직/권한 필터 결과를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 조직 관리자 조회를 기본으로 하며, 역할 계층에 따라 범위를 확장한다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `departmentId`, `teamId`, `roleCode`, `employmentStatus`, `keyword` [추론]
- 응답 (`data` 기준)
  - `PageResponse<UserSummary>`
  - `items[*]`: `userId`, `departmentId`, `userName`, `email`, `positionName`, `titleName`, `roleCode`, `employmentStatus`, `primaryTeamId` [추론]
- 요청 JSON 예시
```json
{
  "query": {
    "page": 1,
    "pageSize": 20,
    "departmentId": 10,
    "teamId": 21,
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
        "departmentId": 10,
        "userName": "홍길동",
        "email": "hong@axwms.com",
        "positionName": "과장",
        "titleName": "팀장",
        "roleCode": "TEAM_LEAD",
        "employmentStatus": "ACTIVE",
        "primaryTeamId": 21
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
  - 대표 오류: `USER_ACCESS_DENIED` [추론]
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
- ERD 연관
  - `tb_user`
  - `tb_user_team`
- 근거
  - class mapping ownership: `domain.organization.user.controller.UserController` / `domain.organization.user.service.UserService`
  - 관련 entity/context: domain.organization.user.entity.User, domain.organization.user.repository.UserRepository, domain.organization.user.repository.jooq.UserJooqRepository
  - 메모: 사용자 본체 ownership은 user feature가 가진다.
  - source: checklist
  - source: class mapping
  - source: ADR-007
  - source: ERD `tb_user`

### GET /api/user/{id}
- 목적: 단일 사용자 상세와 조직 소속 문맥을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 본인 조회 또는 조직 관리자 조회를 허용하는 상세 API로 본다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `userId`, `departmentId`, `userName`, `email`, `positionName`, `titleName`, `joinDate`, `roleCode`, `profileImageUrl`, `phone`, `employmentStatus`
  - `teams` 요약 목록 [추론]
- 요청 JSON 예시
```json
{
  "path": {
    "id": 101
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "userId": 101,
    "departmentId": 10,
    "userName": "홍길동",
    "email": "hong@axwms.com",
    "positionName": "과장",
    "titleName": "팀장",
    "joinDate": "2024-03-01",
    "roleCode": "TEAM_LEAD",
    "profileImageUrl": "https://cdn.axwms.com/profile/101.png",
    "phone": "010-1234-5678",
    "employmentStatus": "ACTIVE",
    "teams": [
      {
        "teamId": 21,
        "teamName": "물류혁신TF",
        "teamRole": "LEADER"
      }
    ]
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `USER_NOT_FOUND` [추론]
  - 대표 오류: `USER_ACCESS_DENIED` [추론]
- ERD 연관
  - `tb_user`
  - `tb_user_team`
- 근거
  - class mapping ownership: `domain.organization.user.controller.UserController` / `domain.organization.user.service.UserService`
  - 관련 entity/context: domain.organization.user.entity.User, domain.organization.user.repository.UserRepository, domain.organization.team.entity.UserTeam, domain.organization.team.repository.UserTeamRepository
  - 메모: 소속 관계는 team feature entity/repository를 함께 참고한다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_user`

### POST /api/user
- 목적: 새 사용자 계정과 초기 조직 문맥을 등록한다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상이 사용자 계정을 생성한다. [추론]
- 요청
  - Body: `departmentId`, `userName`, `email`, `password`, `positionName`, `titleName`, `joinDate`, `roleCode`, `phone`, `profileImageUrl` [추론]
  - 선택적으로 초기 `teamIds` [추론]
- 응답 (`data` 기준)
  - 생성된 사용자 식별자와 기본 프로필
- 요청 JSON 예시
```json
{
  "body": {
    "departmentId": 10,
    "userName": "이재고",
    "email": "lee@axwms.com",
    "password": "********",
    "positionName": "대리",
    "titleName": "팀원",
    "joinDate": "2026-04-15",
    "roleCode": "MEMBER",
    "phone": "010-3333-4444",
    "teamIds": [
      21
    ]
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "userId": 103,
    "departmentId": 10,
    "userName": "이재고",
    "email": "lee@axwms.com",
    "roleCode": "MEMBER",
    "employmentStatus": "ACTIVE"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `201 Created` [추론]
  - 대표 오류: `USER_EMAIL_DUPLICATE` [추론]
  - 대표 오류: `DEPARTMENT_NOT_FOUND` [추론]
  - 대표 오류: `USER_ROLE_INVALID` [추론]
- ERD 연관
  - `tb_user`
  - `tb_user_team`
- 근거
  - class mapping ownership: `domain.organization.user.controller.UserController` / `domain.organization.user.service.UserService`
  - 관련 entity/context: domain.organization.user.entity.User, domain.organization.user.repository.UserRepository, domain.organization.team.repository.UserTeamRepository
  - 메모: 사용자 생성과 초기 팀 연결은 user/team 경계 협력이 필요하다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_user`

### PUT /api/user/{id}
- 목적: 사용자 기본 정보와 역할/상태를 수정한다.
- 상태: `Documented`
- 권한/접근 주체: 조직 관리자 수정 API로 본다. 일부 프로필 수정은 본인에게도 제한 허용 가능성을 남긴다. [추론]
- 요청
  - Path: `id`
  - Body: `departmentId`, `userName`, `positionName`, `titleName`, `roleCode`, `phone`, `profileImageUrl`, `employmentStatus`, `teamIds` [추론]
- 응답 (`data` 기준)
  - 수정된 사용자 프로필과 조직 문맥
- 요청 JSON 예시
```json
{
  "path": {
    "id": 101
  },
  "body": {
    "departmentId": 10,
    "userName": "홍길동",
    "positionName": "차장",
    "titleName": "팀장",
    "roleCode": "TEAM_LEAD",
    "phone": "010-1234-5678",
    "employmentStatus": "ACTIVE",
    "teamIds": [
      21,
      22
    ]
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "userId": 101,
    "departmentId": 10,
    "userName": "홍길동",
    "roleCode": "TEAM_LEAD",
    "employmentStatus": "ACTIVE",
    "teamIds": [
      21,
      22
    ]
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `USER_NOT_FOUND` [추론]
  - 대표 오류: `USER_EMAIL_DUPLICATE` [추론]
  - 대표 오류: `USER_TEAM_DEPARTMENT_MISMATCH` [추론]
- ERD 연관
  - `tb_user`
  - `tb_user_team`
- 근거
  - class mapping ownership: `domain.organization.user.controller.UserController` / `domain.organization.user.service.UserService`
  - 관련 entity/context: domain.organization.user.entity.User, domain.organization.user.repository.UserRepository
  - 메모: 역할 enum은 domain.organization.user.UserRole과 연결된다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_user`
  - source: ERD `tb_user_team`

### DELETE /api/user/{id}
- 목적: 사용자를 하드 삭제가 아니라 퇴직/비활성 처리하는 관리 API다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상이 퇴직/비활성 처리를 수행하는 관리 API로 본다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 처리 결과와 변경된 `employmentStatus` [추론]
- 요청 JSON 예시
```json
{
  "path": {
    "id": 101
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "userId": 101,
    "employmentStatus": "RETIRED",
    "message": "사용자가 퇴직 처리되었습니다."
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `USER_NOT_FOUND` [추론]
  - 대표 오류: `USER_HAS_ACTIVE_OWNERSHIP` [추론]
- ERD 연관
  - `tb_user.employment_status`
- 근거
  - class mapping ownership: `domain.organization.user.controller.UserController` / `domain.organization.user.service.UserService`
  - 관련 entity/context: domain.organization.user.entity.User, domain.organization.user.repository.UserRepository
  - 메모: 실제 비즈니스 의미는 employmentStatus 전환에 가깝다.
  - source: checklist
  - source: class mapping
  - source: ERD `tb_user`

## 6. 추론 메모
- 비밀번호/민감 정보는 사용자 응답에서 제외하는 방향으로 기술했다. [추론]
- 삭제는 `employment_status` 전환 중심의 운영 처리로 문서화했다. [추론]
