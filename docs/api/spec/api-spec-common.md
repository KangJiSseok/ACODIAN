# API 명세 공통 가이드

## 1. 문서 목적
- 이 문서는 `docs/api/api-springboot-endpoint-checklist.md` 의 52개 endpoint 명세서가 공통으로 따르는 표현 규칙을 정의한다.
- 각 도메인 문서는 **계약 중심**으로 작성하며, DTO 클래스명/컨트롤러 메서드명/내부 알고리즘은 고정하지 않는다.
- 응답 항목 설명은 ADR-015에 따라 **`data` payload 중심**으로 정리하고, 별도 `응답 JSON 예시` 블록에서는 최종 HTTP 응답 봉투까지 함께 보여준다.

## 2. 문서 세트
- 문서 경로: `docs/api/spec/`
- 인덱스: [api-spec-index.md](./api-spec-index.md)
- 도메인: [auth](./api-spec-auth.md), [department](./api-spec-department.md), [team](./api-spec-team.md), [user](./api-spec-user.md), [skill](./api-spec-skill.md), [evaluation](./api-spec-evaluation.md), [worklog](./api-spec-worklog.md), [file](./api-spec-file.md), [tag](./api-spec-tag.md), [notification](./api-spec-notification.md), [dashboard](./api-spec-dashboard.md)

## 3. 상태(Status) 표기 규칙
| Status | 의미 | 사용 기준 |
|---|---|---|
| `Documented` | checklist/class mapping 에서 이미 설계 계약이 비교적 명시된 endpoint | class mapping status 가 `Documented` 인 경우 |
| `Proposed-risk-closure` | 운영 완결성/보안/관측성 보강을 위해 설계서가 먼저 고정한 endpoint | class mapping status 가 `Proposed-risk-closure` 인 경우 |
| `Inferred-required` | checklist coverage 를 맞추기 위해 근거 문서를 조합해 계약을 복원한 endpoint | class mapping status 가 `Inferred-required` 인 경우 |

## 4. `[추론]` 표기 규칙
- 다음 중 하나라도 근거 문서에 직접 명시되지 않으면 `[추론]` 을 붙인다.
  - 정확한 요청/응답 필드명
  - 세부 권한 레벨(`DEPT_HEAD`, `TEAM_LEAD` 등)
  - 성공 HTTP status (`201 Created`, `204 No Content` 등)
  - 운영 보강용 에러 코드명
- `[추론]` 은 허용되지만, **근거 축**(ERD / ADR / class mapping / architecture guide)은 함께 적는다.

## 5. 근거 우선순위 (Canonical precedence)
| 속성 | 1차 근거 | 2차 근거 | 메모 |
|---|---|---|---|
| endpoint 존재 / method / path | `api-springboot-endpoint-checklist.md` | `api-springboot-endpoint-class-mapping.md` | checklist + mapping 조합을 canonical 로 사용 |
| endpoint status / ownership | `api-springboot-endpoint-class-mapping.md` | `api-springboot-package-structure-guide.md` | 상태 표기는 mapping 우선 |
| 응답 봉투 | `ADR-015` | `ADR-001` | `ResponseEnvelopePolicy` 공유 규칙 우선 |
| 권한 / 역할 계층 | `ADR-002` | architecture guide | 역할 계층 + service ownership 검증 동시 반영 |
| 에러 원칙 | `ADR-003` | mapping description | ErrorCode enum 단일 출처 |
| 페이지네이션 | `ADR-007` | 없음 | 모든 목록 API 공통 |
| 데이터 필드/ERD 연관 | `docs/ax-wms-erd-draft.sql` | architecture guide | 구조적 진실원 우선 |
| 오래된 설명 보강 | architecture guide Markdown | `.docx` | `.docx` 는 충돌 없을 때만 보조 근거 |

## 6. 공통 응답 봉투 규칙
- ADR-001 / ADR-015 기준 성공 응답:
  - `{ success: true, data: { ... }, timestamp: "ISO8601" }`
- 에러 응답:
  - `{ success: false, error: { code, message, statusCode }, timestamp: "ISO8601" }`
- 문서 본문의 `응답 (`data` 기준)` 섹션은 `data` 내부 payload 만 설명한다.
- 파일 다운로드처럼 `Resource`/streaming 응답은 `ResponseEnvelopePolicy` 제외 대상이 될 수 있다.

## 7. 권한 표기 규칙
- 역할 계층: `DIRECTOR > DEPT_HEAD > TEAM_LEAD > MEMBER` (ADR-002)
- 표기 방식:
  1. **호출 주체**를 먼저 적는다. 예: “인증된 사용자”, “조직 관리자”, “내부 AI callback”
  2. 역할 추정이 필요한 경우 `[추론]` 표시와 함께 적는다.
  3. 데이터 소유권 검증은 controller가 아니라 service 레이어에서 추가 수행된다고 설명한다.

## 8. 에러 표기 규칙
- ErrorCode enum 이 단일 출처다 (ADR-003).
- 본 문서의 구체 코드명은 endpoint contract 설명을 위한 **권장 family 예시**로 본다.
- 도메인 family 예시:
  - `COMMON_*`, `AUTH_*`, `DEPARTMENT_*`, `TEAM_*`, `USER_*`
  - `WORKLOG_*`, `FILE_*`, `TAG_*`, `NOTIFICATION_*`, `DASHBOARD_*` [추론]

## 9. 목록 API / PageResponse 표준
- 기본 query: `page=1`, `pageSize=20`, `max=100`
- 정렬 query: `sortBy`, `sortDirection=ASC|DESC`
- 응답 payload: `PageResponse<T>`
  - `items`, `page`, `pageSize`, `totalCount`, `totalPages`, `isFirst`, `isLast`, `hasNext`, `hasPrevious`
- 이 구조는 최종적으로 `data` 내부에 위치한다.

## 10. 문서 작성 규칙
- 엔드포인트별 최소 섹션:
  - 목적
  - 상태
  - 권한/접근 주체
  - 요청
  - 응답(`data` 기준)
  - 상태/에러
  - ERD 연관
  - 근거
- 구현 세부(메서드명/DTO 클래스명/내부 알고리즘)는 본문에서 배제한다.
- `Proposed-risk-closure`, `Inferred-required` endpoint 도 checklist coverage 에 포함해 동일 수준으로 문서화한다.

## 11. 공통 검수 체크리스트
- 52개 endpoint 가 인덱스와 도메인 문서에 1회 이상 매핑되었는가?
- 모든 목록 API가 ADR-007 PageResponse 표준을 따르는가?
- 파일 다운로드 예외가 ADR-015 와 충돌 없이 설명되었는가?
- 모든 `[추론]` 항목이 근거와 함께 남아 있는가?

## 12. JSON 예시 표기 규칙
- 각 endpoint 상세에는 `요청 JSON 예시` 와 `응답 JSON 예시` 를 추가한다.
- `GET`/`DELETE` 처럼 body가 없는 요청은 문서 편의를 위해 `path`, `query`, `headers` 객체를 포함한 정규화 JSON으로 표기한다.
- 실제 전송이 JSON이 아닌 경우(예: 파일 업로드 `multipart/form-data`, 파일 다운로드 stream)는 예시 상단에 transport 차이를 명시한다.
- 응답 JSON 예시는 특별한 예외가 없는 한 ADR-001/ADR-015의 최종 응답 봉투(`success`, `data`, `timestamp`)까지 포함한다.
