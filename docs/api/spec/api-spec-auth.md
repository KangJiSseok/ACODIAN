# Auth API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
인증 진입, 세션 종료, 현재 사용자 문맥, 토큰 재발급, 비밀번호 변경을 다룬다. `AuthService` 가 인증 유스케이스를 소유하고 JWT 검증/RoleHierarchy 는 `global/security` 와 ADR-002 정책을 따른다.

## 2. 주요 ERD 연관
- `tb_user`
- `domain.auth.entity.RefreshToken` (매핑 문서 근거)

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [api-springboot-endpoint-class-mapping.md](../api-springboot-endpoint-class-mapping.md)
- [adr.yaml](../adr.yaml)

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `POST` | `/api/auth/login` | `Documented` | 로그인 후 접근 토큰과 사용자 권한 문맥을 발급하는 인증 진입점이다. |
| `POST` | `/api/auth/logout` | `Documented` | 세션 종료와 토큰 무효화 정책을 처리하는 로그아웃 API다. |
| `GET` | `/api/auth/me` | `Documented` | 현재 로그인 사용자의 프로필/권한 컨텍스트를 조회한다. |
| `POST` | `/api/auth/refresh` | `Proposed-risk-closure` | 장시간 세션 운영을 위한 access token 재발급 보강 API다. |
| `POST` | `/api/auth/change-password` | `Proposed-risk-closure` | 계정 운영 보안을 위한 비밀번호 변경 보강 API다. |

## 5. 엔드포인트 상세

### POST /api/auth/login
- 목적: 로그인 후 접근 토큰과 사용자 권한 문맥을 발급하는 인증 진입점이다.
- 상태: `Documented`
- 권한/접근 주체: 비인증 사용자가 호출할 수 있다. 성공 시 이후 호출에 사용할 액세스 토큰과 사용자 권한 문맥을 획득한다.
- 요청
  - Body: `email`, `password`
  - 선택적으로 디바이스/클라이언트 식별자를 함께 전달할 수 있다. [추론]
- 응답 (`data` 기준)
  - `accessToken`, `expiresAt` [추론]
  - `refreshToken` 또는 refresh 토큰 쿠키 연계 정보 [추론]
  - `user`: `userId`, `userName`, `departmentId`, `roleCode`, `employmentStatus`
- 요청 JSON 예시
```json
{
  "body": {
    "email": "user@axwms.com",
    "password": "********",
    "deviceId": "web-chrome-01"
  }
}
```
- 응답 JSON 예시
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOi...sample",
    "expiresAt": "2026-04-21T12:00:00Z",
    "refreshToken": "refresh-token-sample",
    "user": {
      "userId": 101,
      "userName": "홍길동",
      "departmentId": 10,
      "roleCode": "TEAM_LEAD",
      "employmentStatus": "ACTIVE"
    }
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 대표 오류: `AUTH_INVALID_CREDENTIALS` [추론]
  - 대표 오류: `AUTH_ACCOUNT_INACTIVE` [추론]
  - 대표 오류: `COMMON_VALIDATION_FAILED` [추론]
- ERD 연관
  - `tb_user`
  - `RefreshToken` 저장소
- 근거
  - class mapping ownership: `domain.auth.controller.AuthController` / `domain.auth.service.AuthService`
  - 관련 entity/context: domain.auth.entity.RefreshToken, domain.auth.repository.RefreshTokenRepository
  - 메모: 인증 유스케이스는 domain/auth, JWT 기술 인프라는 global/security가 소유한다.
  - source: checklist
  - source: class mapping
  - source: ADR-002
  - source: ADR-015
  - source: ERD `tb_user`

### POST /api/auth/logout
- 목적: 세션 종료와 토큰 무효화 정책을 처리하는 로그아웃 API다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자만 호출한다. 현재 세션 또는 전달된 refresh 토큰을 무효화한다. [추론]
- 요청
  - Header: `Authorization: Bearer <token>`
  - Body 또는 쿠키 기반 refresh 토큰 식별 정보 [추론]
- 응답 (`data` 기준)
  - 로그아웃 처리 결과와 무효화 시각 [추론]
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
  "data": {
    "message": "로그아웃이 완료되었습니다.",
    "invalidatedAt": "2026-04-21T03:00:00Z"
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK` [추론]
  - 대표 오류: `AUTH_UNAUTHORIZED` [추론]
  - 대표 오류: `AUTH_REFRESH_TOKEN_NOT_FOUND` [추론]
- ERD 연관
  - `RefreshToken` 저장소
- 근거
  - class mapping ownership: `domain.auth.controller.AuthController` / `domain.auth.service.AuthService`
  - 관련 entity/context: domain.auth.entity.RefreshToken, domain.auth.repository.RefreshTokenRepository
  - 메모: refresh token 저장 매체를 문서에서 별도 구현체 이름으로 고정하지 않는다.
  - source: checklist
  - source: class mapping
  - source: ADR-002

### GET /api/auth/me
- 목적: 현재 로그인 사용자의 프로필/권한 컨텍스트를 조회한다.
- 상태: `Documented`
- 권한/접근 주체: 인증된 사용자만 호출한다. 본인 프로필과 역할/조직 문맥을 반환한다.
- 요청
  - Header: `Authorization`
- 응답 (`data` 기준)
  - `userId`, `userName`, `email`, `departmentId`, `roleCode`, `titleName`, `employmentStatus`
  - `primaryTeam` 요약 [추론]
  - `authorities` 또는 역할 계층 반영 권한 목록 [추론]
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
  - class mapping ownership: `domain.auth.controller.AuthController` / `domain.auth.service.AuthService`
  - 관련 entity/context: global.security.CustomUserPrincipal, domain.organization.user.entity.User, domain.organization.user.repository.UserRepository
  - 메모: 인증 결과 조회지만 사용자 기본 문맥은 organization/user와 연결된다.
  - source: checklist
  - source: class mapping
  - source: ADR-002
  - source: ERD `tb_user`

### POST /api/auth/refresh
- 목적: 장시간 세션 운영을 위한 access token 재발급 보강 API다.
- 상태: `Proposed-risk-closure`
- 권한/접근 주체: 만료 직전/만료된 액세스 토큰을 대체하기 위한 세션 보강 API다. refresh 토큰 보유 클라이언트가 호출한다.
- 요청
  - Body 또는 HttpOnly 쿠키: `refreshToken` [추론]
- 응답 (`data` 기준)
  - 재발급된 `accessToken`, `expiresAt` [추론]
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
  - 대표 오류: `AUTH_TOKEN_REPLAY_DETECTED` [추론]
- ERD 연관
  - `RefreshToken` 저장소
- 근거
  - class mapping ownership: `domain.auth.controller.AuthController` / `domain.auth.service.TokenService`
  - 관련 entity/context: domain.auth.entity.RefreshToken, domain.auth.repository.RefreshTokenRepository
  - 메모: api-design의 운영 완결성 보강 API를 기준으로 유지한다.
  - source: checklist
  - source: class mapping
  - source: ADR-002

### POST /api/auth/change-password
- 목적: 계정 운영 보안을 위한 비밀번호 변경 보강 API다.
- 상태: `Proposed-risk-closure`
- 권한/접근 주체: 인증된 사용자가 본인 비밀번호를 변경한다. 상위 관리자의 강제 초기화는 본 범위에 포함하지 않는다. [추론]
- 요청
  - Body: `currentPassword`, `newPassword`, `confirmPassword` [추론]
- 응답 (`data` 기준)
  - 비밀번호 변경 결과
  - 후속 재로그인/세션 만료 필요 여부 [추론]
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
  "data": {
    "message": "비밀번호가 변경되었습니다.",
    "forceRelogin": true
  },
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
  - class mapping ownership: `domain.auth.controller.AuthController` / `domain.auth.service.AuthService`
  - 관련 entity/context: domain.auth.policy.PasswordPolicy, domain.organization.user.entity.User, domain.organization.user.repository.UserRepository
  - 메모: 인증 정책은 PasswordPolicy로 분리하고 세션 무효화 후속 처리 여지를 둔다.
  - source: checklist
  - source: class mapping
  - source: ADR-002
  - source: ERD `tb_user`

## 6. 추론 메모
- `login`/`refresh` 응답의 토큰 필드명은 DTO를 고정하지 않고 계약 개념만 기술했다. [추론]
- `change-password` 성공 후 세션 무효화 여부는 운영 보강 정책으로 남겨 두었다. [추론]
