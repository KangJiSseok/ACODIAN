# API 명세 공통 가이드

## 1. 문서 목적
- 이 문서는 `docs/api/spec/` 하위 API 명세 문서가 공통으로 따르는 표현 규칙을 정의한다.
- 이번 문서 세트는 **upstream inventory(checklist/class mapping)** 와 **spec-normalized path** 를 구분해 관리한다.
- 문서 본문은 구현 클래스명보다 **HTTP 계약(request/response/path/rule)** 을 우선 설명한다.

## 2. 문서 세트
- 인덱스: [api-spec-index.md](./api-spec-index.md)
- 도메인 문서: [auth](./api-spec-auth.md), [department](./api-spec-department.md), [team](./api-spec-team.md), [user](./api-spec-user.md), [skill](./api-spec-skill.md), [evaluation](./api-spec-evaluation.md), [worklog](./api-spec-worklog.md), [file](./api-spec-file.md), [tag](./api-spec-tag.md), [notification](./api-spec-notification.md), [dashboard](./api-spec-dashboard.md)

## 3. 상태(Status) 표기 규칙
| Status | 의미 | 사용 기준 |
|---|---|---|
| `Documented` | checklist/class mapping 에서 계약이 비교적 직접 확인되는 endpoint | 기존 inventory 문서에 근거가 명확한 경우 |
| `Proposed-risk-closure` | 운영 완결성/보안/관측성 보강을 위해 먼저 고정한 endpoint | inventory 문서가 보강 필요성을 직접 언급한 경우 |
| `Inferred-required` | checklist coverage 를 닫기 위해 ERD/ADR/class mapping 을 조합해 복원한 endpoint | 직접 계약은 없지만 누락 시 coverage 가 깨지는 경우 |

## 4. `[추론]` 표기 규칙
- 아래 항목이 근거 문서에 직접 고정되지 않았으면 `[추론]` 을 붙인다.
  - 세부 필드명
  - 세부 권한 레벨
  - 성공 HTTP status
  - 운영 보강용 에러 코드명
- `[추론]` 항목에는 가능하면 근거 축(ADR, ERD, class mapping, architecture guide)을 함께 적는다.

## 5. Canonical precedence

### 5.1 Upstream inventory canonical
- **52행 coverage matrix** 의 canonical source 는 아래 순서를 따른다.
  1. `docs/api/api-springboot-endpoint-checklist.md`
  2. `docs/api/api-springboot-endpoint-class-mapping.md`
  3. ERD / ADR / architecture guide
- `api-spec-index.md` 의 matrix row count 는 항상 **52** 를 유지한다.
- checklist/class mapping 의 row identity 는 유지하되, clarified scope 에서 현재 spec path 를 명시적으로 고정한 경우 matrix/display path 는 normalized 표기를 사용할 수 있다.

### 5.2 Spec-normalized path canonical
- 실제 도메인 명세 본문은 deep-interview clarified scope 에 따라 **normalized path** 를 사용한다.
- 정규화 규칙:
  - Auth 제외 전 도메인의 첫 resource segment 는 **복수형** 으로 쓴다.
  - 컬렉션 조회의 `/list` 는 제거한다.
  - action suffix(`upload`, `merge`, `read-all` 등)는 유지한다.
  - checklist inventory 와 도메인 명세 path 가 다를 수 있으며, 이는 문서 오류가 아니라 의도된 이원 구조다.

## 6. 공통 응답 봉투 규칙
- 성공 응답 기본형:
```json
{
  "success": true,
  "data": { },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- 에러 응답 기본형:
```json
{
  "success": false,
  "error": {
    "code": "COMMON_VALIDATION_FAILED",
    "message": "요청 값이 올바르지 않습니다.",
    "statusCode": 400
  },
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- `응답(data 기준)` 섹션은 항상 `data` 내부 구조만 설명한다.
- `응답 JSON 예시` 는 최종 envelope 까지 포함한다.
- 파일 다운로드처럼 stream/redirect 가 본질인 endpoint 는 envelope 예외가 될 수 있음을 문서에 명시한다.

## 7. non-GET empty 응답 규칙

### 7.1 기본 규칙
- non-GET endpoint 의 성공 응답은 원칙적으로 **`ApiResponse.empty()`** 로 표기한다.
- 표기 방식은 아래 두 줄로 고정한다.
  - `응답(data 기준): 빈 객체 (ApiResponse.empty())`
  - `응답 JSON 예시`:
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-21T03:00:00Z"
}
```
- `{}` 반환, 메시지 객체 반환, 예시 생략은 허용하지 않는다.

### 7.2 Payload 유지 예외(폐쇄형 2건)
아래 2건만 payload 유지가 허용된다.

| Method | Path | 유지 이유 |
|---|---|---|
| `POST` | `/api/files/upload` | 업로드 직후 `fileId` 와 파일 메타데이터를 후속 연결에 재사용한다. |
| `POST` | `/api/tags/merge` | 병합 결과 `targetTagId` 와 재배치 결과를 후속 처리에 사용한다. |

- 위 2건 외의 non-GET payload 예외는 이번 범위에서 허용하지 않는다.
- `POST /api/auth/login` 은 ADR(로그인 토큰 전송 규약)에 따라 accessToken 은 `Authorization` 응답 헤더, refreshToken 은 HttpOnly 쿠키로 전송하고 응답 바디는 비운다. 따라서 payload 유지 예외에서 제외된다. 사용자 문맥은 `GET /api/users/me` 가 SSOT 로 담당한다.
- `POST /api/auth/refresh` 도 현재 구현 기준으로 access token 은 `Authorization` 응답 헤더, refresh token 은 필요 시 `Set-Cookie` 로만 전송하고 응답 바디는 비운다.

## 8. 권한 표기 규칙
- 역할 계층: `DIRECTOR > DEPT_HEAD > TEAM_LEAD > MEMBER`.
- 도메인 문서에는 먼저 **호출 주체** 를 적고, 역할 해석이 필요한 부분만 `[추론]` 으로 남긴다.
- 데이터 소유권 검증은 controller 가 아니라 service 레이어에서 추가 수행된다고 설명한다.

## 9. 에러 표기 규칙
- 에러 코드는 `ErrorCode enum` 이 단일 출처다.
- 문서에는 endpoint contract 이해에 필요한 대표 family 만 적는다.
- 예시 family: `COMMON_*`, `AUTH_*`, `DEPARTMENT_*`, `TEAM_*`, `USER_*`, `WORKLOG_*`, `FILE_*`, `TAG_*`, `NOTIFICATION_*`, `DASHBOARD_*`.

## 10. 목록 API / PageResponse 표준
- 기본 query: `page=1`, `pageSize=20`, `max=100`
- 정렬 query: `sortBy`, `sortDirection=ASC|DESC`
- 응답 payload: `PageResponse<T>`
  - `items`, `page`, `pageSize`, `totalCount`, `totalPages`, `isFirst`, `isLast`, `hasNext`, `hasPrevious`
- 도메인 문서의 목록 API 예시는 모두 `data` 내부에 `PageResponse<T>` 구조를 둔다.

## 11. `*Id` / `*Name` 병기 규칙
- response 에서 다른 테이블/집계 객체의 `*Id` 를 노출할 때, 대응 `*Name` 또는 동등한 식별 이름이 존재하면 함께 적는다.
- 예시:
  - `departmentId` ↔ `departmentName`
  - `teamId` ↔ `teamName`
  - `departmentHeadUserId` ↔ `departmentHeadUserName`
  - `leaderUserId` ↔ `leaderUserName`
- team domain 의 `leaderUserId` / `leaderUserName` 는 `tb_user_team.team_leader = true` membership 에서 계산한 convenience summary 일 수 있다. 이런 경우 원본 boolean/역할 필드(`teamLeader`, `teamRole`, `allocation`, `isPrimary`) 의미를 도메인 문서에서 함께 설명한다.
- 대응 이름이 없거나 title/date 처럼 name 축이 아닌 경우에는 억지로 추가하지 않는다.

## 12. 문서 작성 규칙
- 엔드포인트별 최소 섹션:
  - 목적
  - 상태
  - 권한/접근 주체
  - 요청
  - 응답(`data` 기준)
  - 요청 JSON 예시
  - 응답 JSON 예시
  - 상태/에러
  - ERD 연관
  - 근거
- spec 본문 변경으로 checklist/class mapping 의 상태·경로·설명과 불일치가 생기면 관련 markdown 을 함께 갱신한다.

## 13. 공통 검수 체크리스트
- [x] `api-spec-index.md` matrix row 수가 52인가?
- [x] addendum row 수가 3인가?
- [x] non-GET payload 유지 예외가 정확히 2건인가? (login, refresh 는 헤더/쿠키 전송 규약으로 제외)
- [ ] 예외 외 non-GET 응답이 모두 `data: {}` 인가?
- [x] domain spec path 가 Auth 제외 모두 복수형 / no-`/list` 인가?
- [ ] `*Id` 노출 시 대응 `*Name` 누락이 없는가?
