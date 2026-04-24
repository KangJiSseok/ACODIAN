# User API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
사용자 계정, 조직 소속, 역할/재직 상태를 관리하는 계약을 정의한다. 본 문서는 inventory 의 legacy 단수형 path 표기와 분리해 normalized path 인 `/api/users/*` 를 사용한다. 사용자 membership 정보는 주 소속 부서와 별개인 교차 부서 팀 참여까지 포함할 수 있으며, `teamLeader`/`teamRole`/`allocation`/`isPrimary` vocabulary 를 `tb_user_team` 기준으로 설명한다.

## 2. 주요 ERD 연관
- `tb_user`
- `tb_user_team`
- `tb_department`

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/users/me` | `Documented` | 현재 로그인 사용자의 프로필/권한 문맥을 조회한다. |
| `GET` | `/api/users` | `Documented` | 사용자 목록과 조직/권한 필터 결과를 조회한다. |
| `GET` | `/api/users/{id}` | `Documented` | 단일 사용자 상세와 조직 소속 문맥을 조회한다. |
| `POST` | `/api/users/signup` | `Documented` | 셀프 회원가입 요청을 처리한다. |
| `POST` | `/api/users` | `Documented` | 관리자가 사용자 계정과 초기 조직 문맥을 등록한다. |
| `PUT` | `/api/users/{id}` | `Documented` | 사용자 기본 정보와 역할/상태를 수정한다. |
| `DELETE` | `/api/users/{id}` | `Documented` | 사용자를 퇴직/비활성 처리한다. |

## 5. 엔드포인트 상세

### GET /api/users/me
- 목적: 현재 로그인 사용자의 프로필 및 소속 팀 문맥을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자만 호출한다. accessToken 으로 인증한다.
- 요청
  - Header: `Authorization: Bearer <access-token>`
- 응답 (`data` 기준)
  - `userId`, `userName`, `departmentId`, `departmentName`, `positionName`, `titleName`, `profileImageUrl`
  - `teams[*]`: `isPrimary`, `teamId`, `teamName`, `teamLeader`, `teamRole`, `allocation`
- 응답 필드 메모
  - `teamLeader`: `tb_user_team.team_leader` boolean 이다.
  - `teamRole`: 사용자의 팀 내 업무 역할명이다.
  - `allocation`: 배치 성격이다.
  - `isPrimary`: 사용자 관점의 대표 소속 팀 여부다. 팀의 소유 부서와는 별개다.
- 요청 JSON 예시
```json
{
  "headers": {
    "Authorization": "Bearer <access-token>"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "userId": 101,
    "userName": "홍길동",
    "departmentId": 10,
    "departmentName": "물류본부",
    "positionName": "과장",
    "titleName": "팀장",
    "profileImageUrl": "https://cdn.axwms.com/profile/101.png",
    "teams": [
      {
        "isPrimary": true,
        "teamId": 21,
        "teamName": "물류혁신TF",
        "teamLeader": true,
        "teamRole": "플랫폼 총괄",
        "allocation": "주담당"
      },
      {
        "isPrimary": false,
        "teamId": 22,
        "teamName": "SCM분석팀",
        "teamLeader": false,
        "teamRole": "SCM 분석",
        "allocation": "겸임"
      }
    ]
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_UNAUTHORIZED` [추론]
  - 대표 오류: `USER_NOT_FOUND` [추론]
- ERD 연관
  - `tb_user`
  - `tb_department`
  - `tb_user_team`
  - `tb_team`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-002

### GET /api/users
- 목적: 사용자 목록과 조직/권한 필터 결과를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 조직 관리자 조회를 기본으로 하며, 상위 역할은 더 넓은 범위를 가진다. [추론]
- 요청
  - Query: `page`, `pageSize`, `sortBy`, `sortDirection`
  - Query: `departmentId`, `teamId`, `positionName`, `titleName`, `roleCode`
- 응답 (`data` 기준)
  - `PageResponse<UserSummary>`
  - `items[*]`: `userId`, `userName`, `email`, `departmentId`, `departmentName`, `teamId`, `teamName`, `positionName`, `titleName`, `roleCode`
- 요청 JSON 예시
```json
{
  "query": {
    "page": 1,
    "pageSize": 20,
    "departmentId": 10,
    "teamId": 21,
    "positionName": "과장",
    "titleName": "팀장",
    "roleCode": "TEAM_LEAD"
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
        "email": "hong@axwms.com",
        "departmentId": 10,
        "departmentName": "물류본부",
        "teamId": 21,
        "teamName": "물류혁신TF",
        "positionName": "과장",
        "titleName": "팀장",
        "roleCode": "TEAM_LEAD"
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
- ERD 연관
  - `tb_user`
  - `tb_user_team`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-007

### GET /api/users/{id}
- 목적: 단일 사용자 상세와 조직 소속 문맥을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 본인 조회 또는 조직 관리자 조회를 허용한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - `userId`, `userName`, `email`, `departmentId`, `departmentName`, `positionName`, `titleName`, `joinDate`, `roleCode`, `profileImageUrl`, `phone`, `employmentStatus`
  - `teams[*]`: `isPrimary`, `teamId`, `teamName`, `teamLeader`, `teamRole`, `allocation`
- 응답 필드 메모
  - `teamLeader`: `tb_user_team.team_leader` boolean 이다.
  - `teamRole`: 사용자의 팀 내 업무 역할명이다.
  - `allocation`: 배치 성격이다.
  - `isPrimary`: 사용자 관점의 대표 소속 팀 여부다. 교차 부서 팀 참여가 가능하더라도 주 소속 부서를 대체하지 않는다.
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
    "userName": "홍길동",
    "email": "hong@axwms.com",
    "departmentId": 10,
    "departmentName": "물류본부",
    "positionName": "과장",
    "titleName": "팀장",
    "joinDate": "2024-03-01",
    "roleCode": "TEAM_LEAD",
    "profileImageUrl": "https://cdn.axwms.com/profile/101.png",
    "phone": "010-1234-5678",
    "employmentStatus": "ACTIVE",
    "teams": [
      {
        "isPrimary": true,
        "teamId": 21,
        "teamName": "물류혁신TF",
        "teamLeader": true,
        "teamRole": "플랫폼 총괄",
        "allocation": "주담당"
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
  - source: checklist
  - source: class mapping

### POST /api/users/signup
- 목적: 셀프 회원가입 요청을 처리한다.
- 상태: `Documented`
- 권한/접근 주체: 비인증 사용자가 호출한다.
- 요청
  - Body: `userName`, `email`, `password`, `phone`, `joinDate`, `profileImageUrl`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "body": {
    "userName": "신입사원",
    "email": "new@axwms.com",
    "password": "********",
    "phone": "010-5555-6666",
    "joinDate": "2026-04-21",
    "profileImageUrl": "https://cdn.axwms.com/profile/new.png"
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
  - 대표 오류: `USER_EMAIL_DUPLICATE` [추론]
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
- ERD 연관
  - `tb_user`
- 근거
  - source: deep-interview spec
  - source: clarified-scope addendum

### POST /api/users
- 목적: 관리자가 사용자 계정과 초기 조직 문맥을 등록한다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상이 호출한다. [추론]
- 요청
  - Body: `userId`, `userName`, `email`, `password`, `departmentId`, `positionName`, `titleName`, `roleCode`, `phone`, `joinDate`, `profileImageUrl`, `teamIds`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "body": {
    "userId": "EMP-2026-001",
    "userName": "이재고",
    "email": "lee@axwms.com",
    "password": "********",
    "departmentId": 10,
    "positionName": "대리",
    "titleName": "팀원",
    "roleCode": "MEMBER",
    "phone": "010-3333-4444",
    "joinDate": "2026-04-15",
    "profileImageUrl": "https://cdn.axwms.com/profile/103.png",
    "teamIds": [21]
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
  - 대표 오류: `USER_EMAIL_DUPLICATE` [추론]
  - 대표 오류: `DEPARTMENT_NOT_FOUND` [추론]
  - 대표 오류: `USER_ROLE_INVALID` [추론]
- ERD 연관
  - `tb_user`
  - `tb_user_team`
- 근거
  - source: checklist
  - source: class mapping

### PUT /api/users/{id}
- 목적: 사용자 기본 정보와 역할/상태를 수정한다.
- 상태: `Documented`
- 권한/접근 주체: 조직 관리자 수정 API로 본다. [추론]
- 요청
  - Path: `id`
  - Body: `departmentId`, `userName`, `positionName`, `titleName`, `roleCode`, `phone`, `profileImageUrl`, `employmentStatus`, `teamIds`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
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
    "profileImageUrl": "https://cdn.axwms.com/profile/101.png",
    "employmentStatus": "ACTIVE",
    "teamIds": [21, 22]
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
  - 대표 오류: `USER_NOT_FOUND` [추론]
  - 대표 오류: `USER_TEAM_DEPARTMENT_MISMATCH` [추론]
- ERD 연관
  - `tb_user`
  - `tb_user_team`
- 근거
  - source: checklist
  - source: class mapping

### DELETE /api/users/{id}
- 목적: 사용자를 퇴직/비활성 처리한다.
- 상태: `Documented`
- 권한/접근 주체: `DEPT_HEAD` 이상이 호출한다. [추론]
- 요청
  - Path: `id`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
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
  "data": {},
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
  - source: checklist
  - source: class mapping

## 6. 추론 메모
- inventory matrix 는 legacy 단수형 inventory path 를 유지하지만, 본문은 normalized `/api/users/*` 를 canonical 로 사용한다.
- signup 과 admin create 는 요청 목적과 요청 필드가 다르므로 분리 문서화했다.
