# Auth API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
인증 진입, 셀프 회원가입, 세션 종료, 토큰 재발급, 비밀번호 변경 계약을 정의한다. Auth 는 인증 유스케이스를 소유하고, JWT 검증/RoleHierarchy 는 global/security 정책을 따른다. 현재 로그인 사용자 문맥 조회는 `GET /api/users/me` 로 분리되어 [api-spec-user.md](./api-spec-user.md) 가 소유한다.

## 2. 주요 ERD 연관
- `tb_user`
- `tb_department`
- `refresh token` 저장소

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `POST` | `/api/auth/login` | `Documented` | 로그인 후 access/refresh token 과 사용자 권한 문맥을 발급한다. |
| `POST` | `/api/auth/signup` | `Documented` | 비인증 사용자의 셀프 회원가입 요청을 처리한다. |
| `POST` | `/api/auth/logout` | `Documented` | 현재 세션을 종료하고 refresh cookie 를 만료시킨다. |
| `POST` | `/api/auth/refresh` | `Documented` | refresh cookie 검증 후 access token 재발급을 수행한다. |
| `POST` | `/api/auth/change-password` | `Proposed-risk-closure` | 본인 비밀번호를 변경한다. |

## 5. 엔드포인트 상세

### POST /api/auth/login
- 목적: 로그인 후 accessToken 을 응답 헤더, refreshToken 을 HttpOnly 쿠키로 발급한다. 사용자 정보는 `/api/users/me` 가 SSOT 로 담당한다.
- 상태: `Documented`
- 권한/접근 주체: 비인증 사용자가 호출한다.
- 요청
  - Body: `email`, `password`
- 응답 (`data` 기준)
  - 빈 객체 (`EmptyResponse`)
  - accessToken: 응답 헤더 `Authorization: Bearer <token>` 으로 전달한다. 클라이언트는 받은 값을 이후 요청의 `Authorization` 헤더에 그대로 재사용한다.
  - refreshToken: `Set-Cookie` 로 전달한다. 쿠키 속성은 `<configured-refresh-cookie-name>=<token>; HttpOnly; Path=/api/auth; Max-Age=<jwt.refresh-token-expiration(초)>` 형식이며, 이름/Path/Secure/SameSite/Domain 은 `auth.refresh-cookie.*` 설정을 따른다. 로컬/테스트 프로파일은 `Secure=false; SameSite=Lax` 로 override 된다.
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
Set-Cookie: <configured-refresh-cookie-name>=refresh-token-sample; Max-Age=1209600; Path=/api/auth; Secure; HttpOnly; SameSite=Strict
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
  - 대표 오류: `AUTH_INVALID_CREDENTIALS`
  - 대표 오류: `AUTH_LOGIN_NOT_ALLOWED`
- ERD 연관
  - `tb_user`
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-002
  - source: ADR — 로그인 토큰 전송 규약 (access=Authorization 헤더, refresh=HttpOnly 쿠키, 바디 비움)


### POST /api/auth/signup
- 목적: 비인증 사용자의 셀프 회원가입 요청을 처리한다.
- 상태: `Documented`
- 권한/접근 주체: 비인증 사용자가 호출한다.
- 요청
  - Content-Type: `multipart/form-data`
  - JSON part: `request`
  - File part: `profile_image` (선택)
  - `request` 필드: `department_id`, `user_name`, `email`, `password`, `position_name`, `title_name`, `join_date`, `phone`, `employment_status`
- 응답 (`data` 기준)
  - 빈 객체 (`EmptyResponse`)
  - signup 성공 시 access token 헤더나 refresh cookie 는 발급하지 않는다.
- 요청 예시
```json
{
  "multipart": {
    "request": {
      "department_id": 10,
      "user_name": "신입사원",
      "email": "new@axwms.com",
      "password": "********",
      "position_name": "사원",
      "title_name": "팀원",
      "join_date": "2026-04-21",
      "phone": "010-5555-6666",
      "employment_status": "ACTIVE"
    },
    "profile_image": "<optional file>"
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
  - 성공: `201 Created`
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
  - 대표 오류: `USER_EMAIL_DUPLICATE` [추론]
  - 대표 오류: `DEPARTMENT_NOT_FOUND` [추론]
  - 대표 오류: `AUTH_PASSWORD_POLICY_VIOLATION` [추론]
- ERD 연관
  - `tb_user`
  - `tb_department`
- 근거
  - source: `domain.auth.controller.AuthController#signup`
  - source: `domain.auth.controller.AuthControllerDocs#signup`
  - source: `domain.auth.dto.SignupApiDto.Request`

### POST /api/auth/logout
- 목적: 현재 세션을 종료하고 refresh cookie 를 만료한다.
- 상태: `Documented`
- 권한/접근 주체: 현재 브라우저 세션을 가진 클라이언트가 호출한다.
- 요청
  - HttpOnly cookie: `refreshToken`
- 응답 (`data` 기준)
  - 빈 객체 (`ApiResponse.empty()`)
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
  - refresh cookie 가 없거나 이미 만료된 경우에도 멱등적으로 성공한다.
- ERD 연관
  - refresh token 저장소
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-002

### POST /api/auth/refresh
- 목적: 설정된 refresh cookie 를 검증해 access token 을 재발급하고, 필요 시 refresh cookie 를 회전한다.
- 상태: `Documented`
- 권한/접근 주체: refresh token 을 보유한 클라이언트가 호출한다.
- 요청
  - Cookie: `auth.refresh-cookie.name` 설정값과 일치하는 HttpOnly refresh cookie
- 응답 (`data` 기준)
  - 빈 객체 (`EmptyResponse`)
  - accessToken: 응답 헤더 `Authorization: Bearer <new-access-token>` 으로 전달한다.
  - rotatedRefreshToken: refresh token 남은 유효 시간이 전체 TTL 의 절반 이하일 때만 `Set-Cookie` 로 다시 기록한다.
- 요청 예시
```json
{
  "cookies": {
    "<configured-refresh-cookie-name>": "refresh-token-sample"
  }
}
```
- 응답 헤더 예시
```
Authorization: Bearer eyJhbGciOi...renewed
Set-Cookie: <configured-refresh-cookie-name>=refresh-token-rotated; Max-Age=1209600; Path=/api/auth/refresh; Secure; HttpOnly; SameSite=Strict
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
  - 대표 오류: `AUTH_INVALID_REFRESH_TOKEN`
  - 대표 오류: `AUTH_LOGIN_NOT_ALLOWED`
- ERD 연관
  - refresh token 저장소
- 근거
  - source: checklist
  - source: class mapping
  - source: ADR-002
  - source: `domain.auth.controller.AuthController#refresh`
  - source: `domain.auth.service.AuthService#refresh`

### POST /api/auth/change-password
- 목적: 본인 비밀번호를 변경한다.
- 상태: `Proposed-risk-closure`
- 권한/접근 주체: 인증된 사용자만 호출한다.
- 요청
  - Body: `currentPassword`, `newPassword` [추론]
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
    "newPassword": "********"
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
- `login` 은 ADR(로그인 토큰 전송 규약)에 따라 응답 바디를 비우고 accessToken 은 Authorization 헤더, refreshToken 은 HttpOnly 쿠키로 전달한다. 사용자 문맥은 `/api/users/me` 에서 조회한다.
- `refresh` 는 현재 구현 기준으로 request body 를 사용하지 않고 설정된 쿠키 이름으로 refresh token 을 추출한다.
- `refresh` 는 성공 시 항상 Authorization 헤더를 내려주고, refresh cookie 회전은 남은 유효 시간이 절반 이하일 때만 수행한다.
- signup 은 `POST /api/users/signup` 이 아니라 `POST /api/auth/signup` 이 canonical path 이며, 실제 Controller 기준으로 `multipart/form-data` 의 `request` JSON part 와 선택 `profile_image` file part 를 사용한다.
- logout/change-password 는 common 의 non-GET empty 규칙을 그대로 따른다.
