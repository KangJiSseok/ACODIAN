# Department API 명세

- 공통 규칙: [api-spec-common.md](./api-spec-common.md)
- 인덱스: [api-spec-index.md](./api-spec-index.md)

## 1. 도메인 목적 / 개요
부서 기준 정보와 부서 접근 수명주기 계약을 정의한다. 본 문서는 normalized path 인 `/api/departments/*` 를 기준으로 하며,
부서 문맥은 `tb_user.department_id` 기준의 **주 소속 부서** 와 `tb_team.department_id` 기준의 **nullable 팀 소유 부서** 를 구분한다.
`tb_user_team` 으로 표현되는 교차 부서 팀 참여/팀장 여부와 Team API visible scope 는 team spec 에서 별도 관리한다.

이번 계약에서 department 삭제는 hard delete 가 아니라 `status_code = INACTIVE` 로 전환하는 soft-delete 이다.
부서 비활성화는 `tb_team.department_id = {departmentId}` 로 직접 소유한 ACTIVE/non-deleted 팀이 0개일 때만 허용하며, 사용자 `employment_status` / `department_id` 는 자동 변경하지 않는다.

## 2. 주요 ERD 연관
- `tb_department`
- `tb_team`
- `tb_user`
- `tb_user_team` (active user 집계 시 ACTIVE 팀 membership 판별용)

## 3. 참조 문서
- [api-spec-common.md](./api-spec-common.md)
- [api-spec-index.md](./api-spec-index.md)
- [adr.yaml](../adr.yaml)
- `.omx/plans/prd-department-access-lifecycle.md`

## 4. 엔드포인트 목록
| Method | Path | Status | 목적 |
|---|---|---|---|
| `GET` | `/api/departments` | `Documented` | 활성 부서 목록과 활성 부서/팀/사용자 집계를 조회한다. |
| `GET` | `/api/departments/department-candidates` | `Documented` | 부서 선택 후보를 권한 범위에 맞춰 페이지네이션 없이 조회한다. |
| `GET` | `/api/departments/{id}/detail` | `Documented` | ACTIVE 부서 header 와 소유 ACTIVE/non-deleted 팀 목록을 조회한다. |
| `POST` | `/api/departments` | `Documented` | 새 부서를 등록한다. |
| `PUT` | `/api/departments/{id}` | `Documented` | 활성 부서 기본 정보를 수정한다. |
| `DELETE` | `/api/departments/{id}` | `Documented` | 부서를 비활성화한다. |

> 범위 제외: `GET /api/departments/{id}/users`

## 5. 엔드포인트 상세

### GET /api/departments
- 목적: 활성 부서 목록과 화면 상단 집계를 조회한다.
- 권한/접근 주체: `DIRECTOR` 전용
- 요청
  - Query parameter 없음
  - Bearer access token 필수
- 응답 (`data` 기준)
  - `activeDepartmentCount`: ACTIVE department 수
  - `activeTeamCount`: ACTIVE department 소속 ACTIVE team 수
  - `activeUserCount`: `employment_status = ACTIVE` 이고 ACTIVE 팀에 최소 1개 소속된 사용자 DISTINCT 수
  - `departments[*]`
    - `departmentId`, `departmentName`, `description`
    - `departmentHeadUserId`, `departmentHeadUserName`
    - `createdAt`, `updatedAt`
- 응답 예시
```json
{
  "success": true,
  "data": {
    "activeDepartmentCount": 3,
    "activeTeamCount": 5,
    "activeUserCount": 18,
    "departments": [
      {
        "departmentId": 10,
        "departmentName": "물류본부",
        "description": "전사 물류 운영 총괄",
        "departmentHeadUserId": 1001,
        "departmentHeadUserName": "박본부",
        "createdAt": "2026-04-01T09:00:00",
        "updatedAt": "2026-04-20T09:00:00"
      },
      {
        "departmentId": 11,
        "departmentName": "무부장본부",
        "description": "부서장 없는 활성 부서",
        "departmentHeadUserId": null,
        "departmentHeadUserName": null,
        "createdAt": "2026-04-01T09:00:00",
        "updatedAt": "2026-04-20T09:00:00"
      }
    ]
  },
  "timestamp": "2026-04-24T15:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 인증 없음/유효하지 않음: `AUTH_UNAUTHORIZED`
  - DIRECTOR 권한 없음: `AUTH_ACCESS_DENIED`

### GET /api/departments/department-candidates
- 목적: 부서 선택 UI에서 필요한 최소 부서 후보 목록을 조회한다.
- 권한/접근 주체: `DIRECTOR`, `DEPT_HEAD`
- 요청
  - Query parameter 없음
  - Bearer access token 필수
- 범위
  - `DIRECTOR`: 모든 ACTIVE 부서를 반환한다.
  - `DEPT_HEAD`: 현재 로그인 사용자의 주 소속 ACTIVE 부서만 반환한다.
  - `DEPT_HEAD` 의 현재 사용자 row 가 없으면 `USER_NOT_FOUND` 로 응답한다.
  - `DEPT_HEAD` 의 주 소속 부서가 없거나 INACTIVE 이면 빈 목록을 반환한다.
  - `TEAM_LEAD`, `MEMBER` 는 호출할 수 없다.
- 정렬
  - `departmentId ASC`
- 응답 (`data` 기준)
  - `departments[]`: 페이지네이션 wrapper 없이 반환하며 후보가 없으면 빈 배열
    - `departmentId`, `departmentName`
- 응답 예시
```json
{
  "success": true,
  "data": {
    "departments": [
      {
        "departmentId": 10,
        "departmentName": "물류본부"
      },
      {
        "departmentId": 11,
        "departmentName": "솔루션사업부"
      }
    ]
  },
  "timestamp": "2026-05-07T12:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 인증 없음/유효하지 않음: `AUTH_UNAUTHORIZED`
  - DIRECTOR/DEPT_HEAD 권한 없음: `AUTH_ACCESS_DENIED`
  - 현재 사용자 없음: `USER_NOT_FOUND`

### GET /api/departments/{id}/detail
- 목적: ACTIVE 부서의 top-level header 와 해당 부서가 직접 소유한 팀 목록을 조회한다.
- 권한/접근 주체: `DIRECTOR` 전용
- 요청
  - Path: `id`
  - Bearer access token 필수
- 응답 (`data` 기준)
  - `departmentId`, `departmentName`
  - `departmentHeadUserId`, `departmentHeadUserName`: 부서 header 에서 head user left join 으로 조회하며 없으면 null
  - `teams[]`: 페이지네이션 wrapper 없이 반환하며 팀이 없으면 빈 배열
    - `teamId`, `teamName`, `leaderId`, `leaderName`, `startDate`, `expectedEndDate`, `memberCount`
- 팀 포함 기준
  - `tb_team.department_id = {id}`
  - `tb_team.status_code = ACTIVE`
  - `tb_team.deleted_at IS NULL`
  - `department_id IS NULL`, 타 부서 소유, INACTIVE, soft-deleted 팀은 제외한다.
- 집계/리더 기준
  - leader 는 ACTIVE membership 중 `is_leader = true` 인 단일 사용자다.
  - `memberCount` 는 leader join 과 분리해 계산한 ACTIVE membership 의 DISTINCT user 수 기준이다.
  - 정렬은 `teamId ASC` 다.
- 응답 예시
```json
{
  "success": true,
  "data": {
    "departmentId": 10,
    "departmentName": "물류본부",
    "departmentHeadUserId": 1001,
    "departmentHeadUserName": "박본부",
    "teams": [
      {
        "teamId": 21,
        "teamName": "플랫폼개발팀",
        "leaderId": 2001,
        "leaderName": "류팀장",
        "startDate": "2026-04-01",
        "expectedEndDate": null,
        "memberCount": 3
      }
    ]
  },
  "timestamp": "2026-05-06T12:00:00Z"
}
```
- 상태/에러
  - 성공: `200 OK`
  - 인증 없음/유효하지 않음: `AUTH_UNAUTHORIZED`
  - DIRECTOR 권한 없음: `AUTH_ACCESS_DENIED`
  - ACTIVE 부서 없음: `DEPARTMENT_NOT_FOUND`

### POST /api/departments
- 목적: 새 부서를 등록한다.
- 권한/접근 주체: `DIRECTOR` 전용
- 요청 Body
  - `departmentName` (필수)
  - `description` (선택)
  - `departmentHeadUserId` (선택)
- 제약
  - request body 에 `status` / `statusCode` 를 받지 않는다.
  - `departmentHeadUserId` 가 있으면 사용자 존재를 검증한다.
  - `departmentHeadUserId` 가 있으면 사용자 역할은 `DEPT_HEAD` 또는 `DIRECTOR` 여야 한다.
  - 이미 다른 부서의 head 로 지정된 사용자는 `DEPARTMENT_DUPLICATE_HEAD_USER` 로 거부한다.
  - 비활성 부서도 기존 UNIQUE 를 유지하므로, inactive row 의 이름/부서장도 재사용 대상이 아니다.
- 응답 예시
```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-04-24T15:00:00Z"
}
```
- 상태/에러
  - 성공: `201 Created`
  - 부서명 중복: `DEPARTMENT_DUPLICATE_NAME`
  - 부서장 사용자 없음: `USER_NOT_FOUND`
  - 부서장 역할 부적합: `DEPARTMENT_INVALID_HEAD_USER_ROLE`
  - 부서장 중복: `DEPARTMENT_DUPLICATE_HEAD_USER`

### PUT /api/departments/{id}
- 목적: 활성 부서의 기본 정보를 수정한다.
- 권한/접근 주체: `DIRECTOR` 전용
- 요청 Body
  - `departmentName` (필수)
  - `description` (선택)
  - `departmentHeadUserId` (선택, `DIRECTOR` 또는 `DEPT_HEAD` 역할 사용자만 허용)
- 제약
  - `status` / `statusCode` 는 수정 입력으로 받지 않는다.
  - `INACTIVE` 부서는 수정 대상이 아니며 `DEPARTMENT_NOT_FOUND` 로 응답한다.
  - duplicate name / duplicate head-user 정책은 POST 와 동일하다.
  - `departmentHeadUserId` 가 `TEAM_LEAD`, `MEMBER` 등 비허용 역할 사용자를 가리키면 `DEPARTMENT_HEAD_ROLE_NOT_ALLOWED` 로 응답한다.
  - `departmentHeadUserId` 가 `null` 이거나 필드가 생략되면 기존 부서장을 해제한다.
  - 비허용 역할 검증이 부서장 중복 검증보다 먼저 수행되므로, 비허용 역할 사용자는 다른 부서의 head 여부와 무관하게 `DEPARTMENT_HEAD_ROLE_NOT_ALLOWED` 로 실패한다.
  - `DEPARTMENT_DUPLICATE_HEAD_USER` 는 `DIRECTOR` 또는 `DEPT_HEAD` 역할 사용자에 대해서만 검사된다.
- 상태/에러
  - 성공: `200 OK`
  - 활성 부서 없음: `DEPARTMENT_NOT_FOUND`
  - 부서명 중복: `DEPARTMENT_DUPLICATE_NAME`
  - 부서장 사용자 없음: `USER_NOT_FOUND`
  - 부서장 역할 제한 위반: `DEPARTMENT_HEAD_ROLE_NOT_ALLOWED`
  - 부서장 중복: `DEPARTMENT_DUPLICATE_HEAD_USER`

### DELETE /api/departments/{id}
- 목적: 부서를 soft-delete 한다.
- 권한/접근 주체: `DIRECTOR` 전용
- 동작
  - 직접 소유한 ACTIVE/non-deleted 팀이 하나라도 있으면 실패한다.
  - 직접 소유한 팀이 0개이거나 전부 `INACTIVE`/soft-deleted 이거나 `department_id IS NULL` 팀뿐이면 department 상태만 `INACTIVE` 로 바꾼다.
  - 이미 `INACTIVE` 인 department 에 대한 DELETE 는 no-op 성공이다.
  - 사용자 재직 상태/주 소속 부서/팀 membership 은 자동 변경하지 않는다.
- 상태/에러
  - 성공: `200 OK`
  - 부서 없음: `DEPARTMENT_NOT_FOUND`
  - ACTIVE 팀 존재: `DEPARTMENT_HAS_ACTIVE_TEAMS`

## 6. 수동 검증용 데이터 시나리오
- `솔루션개발사업부`: ACTIVE + 직접 소유 ACTIVE 팀 존재 → DETAIL teams 노출 / DELETE 실패
- `솔루션사업부`: ACTIVE + 직접 소유 팀이 모두 INACTIVE → DELETE 성공
- `휴면본부`: INACTIVE → DELETE no-op 성공
- `비상대응본부`: ACTIVE + 직접 소유 팀 0개 + head 없음 → 목록/detail null head / 빈 teams / DELETE 허용
- `member@ibank.com`: ACTIVE 사용자지만 INACTIVE 팀에만 소속 → `activeUserCount` 제외

## 7. 메모
- `departmentHeadUserId` / `department_name` UNIQUE 는 inactive row 에도 유지된다.
- RoleHierarchy 는 이번 기능 범위 밖이며, 현재 권한 체크는 `hasRole('DIRECTOR')` 기준으로 닫는다.
