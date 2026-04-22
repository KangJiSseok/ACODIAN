# Auth API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
인증 진입, 세션 종료, 현재 사용자 문맥, 토큰 재발급, 비밀번호 변경 계약을 정의한다. Auth 는 인증 유스케이스를 소유하고, JWT 검증/RoleHierarchy 는 global/security 정책을 따른다.

## 2. 주요 ERD 연관
- `tb_user`
- `refresh token` 저장소

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `POST` | `/api/auth/login` | `Documented` | 로그인 후 access/refresh token 과 사용자 권한 문맥을 발급한다. |
| `POST` | `/api/auth/logout` | `Documented` | 현재 세션 또는 refresh token 을 무효화한다. |
| `GET` | `/api/auth/me` | `Documented` | 현재 로그인 사용자의 프로필/권한 문맥을 조회한다. |
| `POST` | `/api/auth/refresh` | `Proposed-risk-closure` | access token 재발급을 수행한다. |
| `POST` | `/api/auth/change-password` | `Proposed-risk-closure` | 본인 비밀번호를 변경한다. |

## 5. 엔드포인트 상세

### POST /api/auth/login
- 목적: 로그인 후 accessToken 을 응답 헤더, refreshToken 을 HttpOnly 쿠키로 발급한다. 사용자 정보는 `/api/auth/me` 가 SSOT 로 담당한다.
- 상태: `Documented`
- 권한/접근 주체: 비인증 사용자가 호출한다.
- 요청
  - Body: `email`, `password`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
  - accessToken: 응답 헤더 `Authorization: Bearer <token>` 으로 전달한다. 클라이언트는 받은 값을 이후 요청의 `Authorization` 헤더에 그대로 재사용한다.
  - refreshToken: `Set-Cookie` 로 전달한다. 쿠키 속성은 `Name=refreshToken; HttpOnly; Secure; SameSite=Strict; Path=/api/auth/refresh; Max-Age=<jwt.refresh-token-expiration(초)>` 이며, 로컬/테스트 프로파일은 `Secure=false; SameSite=Lax` 로 override 된다.
- 요청 JSON 예시
```json
{
  "body": {
    "email": "user@axwms.com",
    "password": "********"
  }
}
```
- 응답 헤더 예시
```
Authorization: Bearer eyJhbGciOi...sample
Set-Cookie: refreshToken=refresh-token-sample; Max-Age=1209600; Path=/api/auth/refresh; Secure; HttpOnly; SameSite=Strict
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
  - 성공: `200 OK`
  - 대표 오류: `AUTH_INVALID_CREDENTIALS` [추론]
  - 대표 오류: `AUTH_ACCOUNT_INACTIVE` [추론]
- ERD 연관
  - `tb_user`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-002
  - source: ADR — 로그인 토큰 전송 규약 (access=Authorization 헤더, refresh=HttpOnly 쿠키, 바디 비움)

### POST /api/auth/logout
- 목적: 현재 세션 또는 refresh token 을 무효화한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자만 호출한다.
- 요청
  - Header: `Authorization: Bearer <token>`
  - 선택 Body: `refreshToken` [추론]
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "headers": {
    "Authorization": "Bearer <access-token>"
  },
  "body": {
    "refreshToken": "refresh-token-sample"
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
  - 대표 오류: `AUTH_UNAUTHORIZED` [추론]
  - 대표 오류: `AUTH_REFRESH_TOKEN_NOT_FOUND` [추론]
- ERD 연관
  - refresh token 저장소
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-002

### GET /api/auth/me
- 목적: 현재 로그인 사용자의 프로필/권한 문맥을 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자만 호출한다.
- 요청
  - Header: `Authorization`
- 응답 (`data` 기준)
  - `userId`, `userName`, `email`, `departmentId`, `departmentName`, `roleCode`, `titleName`, `employmentStatus`
  - `primaryTeam`: `teamId`, `teamName` [추론]
  - `authorities` [추론]
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
    "email": "user@axwms.com",
    "departmentId": 10,
    "departmentName": "물류본부",
    "roleCode": "TEAM_LEAD",
    "titleName": "팀장",
    "employmentStatus": "ACTIVE",
    "primaryTeam": {
      "teamId": 21,
      "teamName": "물류혁신TF"
    },
    "authorities": [
      "ROLE_TEAM_LEAD",
      "ROLE_MEMBER"
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
  - `tb_user_team`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-002

### POST /api/auth/refresh
- 목적: access token 재발급을 수행한다.
- 상태: `Proposed-risk-closure`
- 권한/접근 주체: refresh token 을 보유한 클라이언트가 호출한다.
- 요청
  - Body 또는 HttpOnly cookie: `refreshToken` [추론]
- 응답 (`data` 기준)
  - `accessToken`, `expiresAt`
  - 필요 시 rotation 된 `refreshToken` [추론]
- 요청 JSON 예시
```json
{
  "body": {
    "refreshToken": "refresh-token-sample"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...renewed",
    "expiresAt": "2026-04-21T14:00:00Z",
    "refreshToken": "refresh-token-rotated"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `AUTH_REFRESH_TOKEN_EXPIRED` [추론]
  - 대표 오류: `AUTH_REFRESH_TOKEN_NOT_FOUND` [추론]
- ERD 연관
  - refresh token 저장소
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-002

### POST /api/auth/change-password
- 목적: 본인 비밀번호를 변경한다.
- 상태: `Proposed-risk-closure`
- 권한/접근 주체: 인증된 사용자만 호출한다.
- 요청
  - Body: `currentPassword`, `newPassword`, `confirmPassword` [추론]
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
- 요청 JSON 예시
```json
{
  "headers": {
    "Authorization": "Bearer <access-token>"
  },
  "body": {
    "currentPassword": "********",
    "newPassword": "********",
    "confirmPassword": "********"
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
  - 대표 오류: `AUTH_PASSWORD_MISMATCH` [추론]
  - 대표 오류: `AUTH_PASSWORD_POLICY_VIOLATION` [추론]
  - 대표 오류: `AUTH_UNAUTHORIZED` [추론]
- ERD 연관
  - `tb_user.password_hash`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-002

## 6. 추론 메모
- `login` 은 ADR(로그인 토큰 전송 규약)에 따라 응답 바디를 비우고 accessToken 은 Authorization 헤더, refreshToken 은 HttpOnly 쿠키로 전달한다. 사용자 문맥은 `/api/auth/me` 에서 조회한다.
- `refresh` 는 이후 구현 시 동일 규약(쿠키 회전)을 재사용하도록 선결정되어 있다. [추론]
- logout/change-password 는 common 의 non-GET empty 규칙을 그대로 따른다.

