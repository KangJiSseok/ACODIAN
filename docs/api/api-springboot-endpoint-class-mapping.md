# AX-WMS Spring Boot `/api` API → 도메인/클래스 매핑

- 기준 문서: `plan/api-design.md`
- 연계 문서: `docs/api/api-springboot-package-structure-guide.md`
- 목적: endpoint/API 기준으로 controller/service/entity/repository ownership을 빠르게 찾을 수 있는 class-level 매핑표를 제공한다.
- 범위: endpoint status, description, controller/service ownership, 관련 entity/repository/JOOQ 문맥
- 전제: base package 예시는 `com.ibank.axwms`

---

## 1. 매핑 원칙

이 문서는 `plan/api-design.md`의 상세 endpoint 본문을 기준으로 **endpoint / status**를 고정하고,
현재 코드와 ADR / code-convention 기준으로 **class-level ownership**만 매핑한다.

- `Controller#method`, `Service#method`, Request/Response DTO 이름은 이 문서에서 확정하지 않는다.
- `ControllerDocs` 인터페이스는 Swagger 문서화 surface이므로 이 매핑표에서 제외한다.
- 관련 Entity·Repository·JOOQ 컬럼은 ownership/context 식별용이며, direct dependency 허용표가 아니다.
- callback 요약표보다 상세 endpoint 본문을 우선하며, 아직 구현 전인 항목도 설계 계약 기준으로 표기한다.
- 조회 endpoint도 기본적으로 `{Domain}Service` 의 readOnly 메서드에 매핑하며, `*QueryService` 분리 방향은 문서 기준으로 사용하지 않는다.

## 2. 도메인별 API 매핑

### 2.1 `auth` 도메인

<div style="overflow-x: auto;">
<table style="width: max-content; min-width: 100%; white-space: nowrap;">
  <thead>
    <tr>
      <th>Endpoint</th>
      <th>Status</th>
      <th>description</th>
      <th>Controller Class</th>
      <th>Service Class</th>
      <th>관련 Entity / Repository / JOOQ</th>
      <th>메모</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>POST /api/auth/login</code></td>
      <td><code>Documented</code></td>
      <td>로그인 후 접근 토큰과 사용자 권한 문맥을 발급하는 인증 진입점이다.</td>
      <td><code>domain.auth.controller.AuthController</code></td>
      <td><code>domain.auth.service.AuthService</code></td>
      <td><code>domain.auth.entity.RefreshToken</code>, <code>domain.auth.repository.RefreshTokenRepository</code></td>
      <td>인증 유스케이스는 <code>domain/auth</code>, JWT 기술 인프라는 <code>global/security</code>가 소유한다.</td>
    </tr>
    <tr>
      <td><code>POST /api/auth/signup</code></td>
      <td><code>Documented</code></td>
      <td>비인증 사용자의 셀프 회원가입 요청을 처리한다.</td>
      <td><code>domain.auth.controller.AuthController</code></td>
      <td><code>domain.auth.service.AuthService</code></td>
      <td><code>domain.organization.user.entity.User</code>, <code>domain.organization.user.repository.UserRepository</code>, <code>domain.organization.department.repository.DepartmentRepository</code></td>
      <td>회원가입은 <code>multipart/form-data</code> 의 <code>request</code> JSON part 와 선택 <code>profile_image</code> file part 를 사용하며, 사용자 도메인의 <code>POST /api/users/signup</code> 이 아니다.</td>
    </tr>
    <tr>
      <td><code>POST /api/auth/logout</code></td>
      <td><code>Documented</code></td>
      <td>세션 종료와 토큰 무효화 정책을 처리하는 로그아웃 API다.</td>
      <td><code>domain.auth.controller.AuthController</code></td>
      <td><code>domain.auth.service.AuthService</code></td>
      <td><code>domain.auth.entity.RefreshToken</code>, <code>domain.auth.repository.RefreshTokenRepository</code></td>
      <td>refresh token 저장 매체를 문서에서 별도 구현체 이름으로 고정하지 않는다.</td>
    </tr>
    <tr>
      <td><code>POST /api/auth/refresh</code></td>
      <td><code>Documented</code></td>
      <td>설정된 refresh cookie 를 검증해 access token 을 재발급하고 필요 시 refresh cookie 를 회전하는 API다.</td>
      <td><code>domain.auth.controller.AuthController</code></td>
      <td><code>domain.auth.service.AuthService</code></td>
      <td><code>domain.auth.entity.RefreshToken</code>, <code>domain.auth.repository.RefreshTokenRepository</code></td>
      <td>응답 바디는 비우고 <code>Authorization</code> 헤더와 조건부 <code>Set-Cookie</code> 로만 토큰을 전달한다.</td>
    </tr>
    <tr>
      <td><code>POST /api/auth/change-password</code></td>
      <td><code>Proposed-risk-closure</code></td>
      <td>계정 운영 보안을 위한 비밀번호 변경 보강 API다.</td>
      <td><code>domain.auth.controller.AuthController</code></td>
      <td><code>domain.auth.service.AuthService</code></td>
      <td><code>domain.auth.policy.PasswordPolicy</code>, <code>domain.organization.user.entity.User</code>, <code>domain.organization.user.repository.UserRepository</code></td>
      <td>인증 정책은 <code>PasswordPolicy</code>로 분리하고 세션 무효화 후속 처리 여지를 둔다.</td>
    </tr>
  </tbody>
</table>
</div>


### 2.2 `organization/department` 도메인

<div style="overflow-x: auto;">
<table style="width: max-content; min-width: 100%; white-space: nowrap;">
  <thead>
    <tr>
      <th>Endpoint</th>
      <th>Status</th>
      <th>description</th>
      <th>Controller Class</th>
      <th>Service Class</th>
      <th>관련 Entity / Repository / JOOQ</th>
      <th>메모</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>GET /api/departments</code></td>
      <td><code>Documented</code></td>
      <td>부서 목록과 검색/정렬 결과를 조회하는 기준 API다.</td>
      <td><code>domain.organization.department.controller.DepartmentController</code></td>
      <td><code>domain.organization.department.service.DepartmentService</code></td>
      <td><code>domain.organization.department.entity.Department</code>, <code>domain.organization.department.repository.DepartmentRepository</code>, <code>domain.organization.department.repository.jooq.DepartmentJooqRepository</code></td>
      <td><code>organization</code>만 feature-first 구조를 사용한다.</td>
    </tr>
    <tr>
      <td><code>GET /api/departments/{id}/detail</code></td>
      <td><code>Documented</code></td>
      <td>ACTIVE 부서 header와 직접 소유한 ACTIVE/non-deleted 팀 목록을 조회한다.</td>
      <td><code>domain.organization.department.controller.DepartmentController</code></td>
      <td><code>domain.organization.department.service.DepartmentService</code></td>
      <td><code>domain.organization.department.entity.Department</code>, <code>domain.organization.department.repository.DepartmentRepository</code>, <code>domain.organization.department.repository.jooq.DepartmentJooqRepository</code></td>
      <td>부서 상세는 header projection과 단일 leader join 기반 team projection을 분리한다.</td>
    </tr>
    <tr>
      <td><code>GET /api/departments/{id}/users</code></td>
      <td><code>Documented</code></td>
      <td>특정 부서 소속 사용자 목록을 페이지네이션 조회한다.</td>
      <td><code>domain.organization.department.controller.DepartmentController</code></td>
      <td><code>domain.organization.department.service.DepartmentService</code></td>
      <td><code>domain.organization.department.entity.Department</code>, <code>domain.organization.user.entity.User</code>, <code>domain.organization.user.repository.jooq.UserJooqRepository</code></td>
      <td>clarified-scope addendum. 부서 소속 사용자 조회 ownership은 department feature가 가지며 조회 조합에 user jooq를 사용한다.</td>
    </tr>
    <tr>
      <td><code>POST /api/departments</code></td>
      <td><code>Documented</code></td>
      <td>새 부서를 등록하는 관리 API다.</td>
      <td><code>domain.organization.department.controller.DepartmentController</code></td>
      <td><code>domain.organization.department.service.DepartmentService</code></td>
      <td><code>domain.organization.department.entity.Department</code>, <code>domain.organization.department.repository.DepartmentRepository</code></td>
      <td>생성/수정 책임은 <code>DepartmentService</code>에 둔다.</td>
    </tr>
    <tr>
      <td><code>PUT /api/departments/{id}</code></td>
      <td><code>Documented</code></td>
      <td>부서 기본 정보를 수정한다.</td>
      <td><code>domain.organization.department.controller.DepartmentController</code></td>
      <td><code>domain.organization.department.service.DepartmentService</code></td>
      <td><code>domain.organization.department.entity.Department</code>, <code>domain.organization.department.repository.DepartmentRepository</code></td>
      <td>목록/상세 조회와 쓰기 ownership이 동일 feature 안에 있다.</td>
    </tr>
    <tr>
      <td><code>DELETE /api/departments/{id}</code></td>
      <td><code>Documented</code></td>
      <td>부서 삭제 또는 비활성화 정책을 수행한다.</td>
      <td><code>domain.organization.department.controller.DepartmentController</code></td>
      <td><code>domain.organization.department.service.DepartmentService</code></td>
      <td><code>domain.organization.department.entity.Department</code>, <code>domain.organization.department.repository.DepartmentRepository</code></td>
      <td>직접 소유한 ACTIVE/non-deleted 팀 존재 여부 검증은 JOOQ repository 계약으로 처리한다.</td>
    </tr>
  </tbody>
</table>
</div>


### 2.3 `organization/team` 도메인

<div style="overflow-x: auto;">
<table style="width: max-content; min-width: 100%; white-space: nowrap;">
  <thead>
    <tr>
      <th>Endpoint</th>
      <th>Status</th>
      <th>description</th>
      <th>Controller Class</th>
      <th>Service Class</th>
      <th>관련 Entity / Repository / JOOQ</th>
      <th>메모</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>GET /api/teams</code></td>
      <td><code>Documented</code></td>
      <td>역할별 조회 범위에 맞는 팀 목록을 페이지네이션으로 조회한다.</td>
      <td><code>domain.organization.team.controller.TeamController</code></td>
      <td><code>domain.organization.team.service.TeamService</code></td>
      <td><code>domain.organization.team.entity.Team</code>, <code>domain.organization.team.repository.TeamRepository</code>, <code>domain.organization.team.repository.jooq.TeamJooqRepository</code></td>
      <td>팀 read ownership은 <code>team</code> feature가 가지며, soft-delete 된 팀은 기본 조회에서 제외한다.</td>
    </tr>
    <tr>
      <td><code>GET /api/teams/summary</code></td>
      <td><code>Proposed-risk-closure</code></td>
      <td>페이지네이션 응답과 분리된 상단 집계 요약을 조회한다.</td>
      <td><code>domain.organization.team.controller.TeamController</code></td>
      <td><code>domain.organization.team.service.TeamService</code></td>
      <td><code>domain.organization.team.entity.Team</code>, <code>domain.organization.team.repository.TeamRepository</code>, <code>domain.organization.team.repository.jooq.TeamJooqRepository</code>, <code>domain.organization.team.repository.UserTeamRepository</code></td>
      <td>상단 카드 집계와 목록 페이지네이션 응답의 책임을 분리하는 addendum endpoint 다.</td>
    </tr>
    <tr>
      <td><code>GET /api/teams/{id}</code></td>
      <td><code>Documented</code></td>
      <td>역할별 조회 범위에 맞는 단일 팀 상세와 업무일지 집계를 조회한다.</td>
      <td><code>domain.organization.team.controller.TeamController</code></td>
      <td><code>domain.organization.team.service.TeamService</code></td>
      <td><code>domain.organization.team.entity.Team</code>, <code>domain.organization.team.entity.UserTeam</code>, <code>domain.organization.team.repository.TeamRepository</code>, <code>domain.organization.team.repository.UserTeamRepository</code></td>
      <td>교차 부서 참여도 <code>UserTeam</code> membership 으로 해석한다.</td>
    </tr>
    <tr>
      <td><code>GET /api/teams/{id}/users</code></td>
      <td><code>Documented</code></td>
      <td>특정 팀의 ACTIVE membership 사용자 목록을 조회한다.</td>
      <td><code>domain.organization.team.controller.TeamController</code></td>
      <td><code>domain.organization.team.service.TeamService</code></td>
      <td><code>domain.organization.team.entity.UserTeam</code>, <code>domain.organization.team.repository.UserTeamRepository</code>, <code>domain.organization.team.repository.jooq.UserTeamJooqRepository</code></td>
      <td>membership 상태값은 <code>ACTIVE</code>, <code>LEFT</code> 두 종류만 사용한다.</td>
    </tr>
    <tr>
      <td><code>GET /api/teams/{id}/worklogs</code></td>
      <td><code>Documented</code></td>
      <td>특정 팀 기준 업무일지 목록을 조회한다.</td>
      <td><code>domain.organization.team.controller.TeamController</code></td>
      <td><code>domain.organization.team.service.TeamService</code></td>
      <td><code>domain.organization.team.entity.Team</code>, <code>domain.organization.team.repository.jooq.TeamJooqRepository</code>, <code>domain.worklog.repository.WorklogRepository</code></td>
      <td>team feature 가 ownership 을 집행하고, 실제 projection 은 worklog 연동을 포함할 수 있다.</td>
    </tr>
    <tr>
      <td><code>POST /api/teams</code></td>
      <td><code>Documented</code></td>
      <td>새 팀을 생성하고 leaderUserId 기반 팀장 문맥을 설정한다.</td>
      <td><code>domain.organization.team.controller.TeamController</code></td>
      <td><code>domain.organization.team.service.TeamService</code></td>
      <td><code>domain.organization.team.entity.Team</code>, <code>domain.organization.team.entity.UserTeam</code>, <code>domain.organization.team.repository.TeamRepository</code>, <code>domain.organization.team.repository.UserTeamRepository</code></td>
      <td>leader source of truth 는 별도 team 컬럼이 아니라 membership 이다.</td>
    </tr>
    <tr>
      <td><code>PUT /api/teams/{id}</code></td>
      <td><code>Documented</code></td>
      <td>팀 기본 정보와 leaderUserId 기반 팀장 문맥을 수정한다.</td>
      <td><code>domain.organization.team.controller.TeamController</code></td>
      <td><code>domain.organization.team.service.TeamService</code></td>
      <td><code>domain.organization.team.entity.Team</code>, <code>domain.organization.team.entity.UserTeam</code>, <code>domain.organization.team.repository.TeamRepository</code>, <code>domain.organization.team.repository.UserTeamRepository</code></td>
      <td>활성 팀 uniqueness 는 <code>deleted_at IS NULL</code> 기준으로 해석한다.</td>
    </tr>
    <tr>
      <td><code>PATCH /api/teams/{id}/status</code></td>
      <td><code>Documented</code></td>
      <td>팀 운영 상태 활성/비활성 전환을 담당한다.</td>
      <td><code>domain.organization.team.controller.TeamController</code></td>
      <td><code>domain.organization.team.service.TeamService</code></td>
      <td><code>domain.organization.team.TeamStatus</code>, <code>domain.organization.team.entity.Team</code>, <code>domain.organization.team.repository.TeamRepository</code></td>
      <td><code>deleted_at</code> lifecycle 과 분리된 운영 상태만 다룬다.</td>
    </tr>
    <tr>
      <td><code>POST /api/teams/{id}/users/bulk</code></td>
      <td><code>Documented</code></td>
      <td>팀 사용자 추가/제거를 일괄 반영한다.</td>
      <td><code>domain.organization.team.controller.TeamController</code></td>
      <td><code>domain.organization.team.service.TeamService</code></td>
      <td><code>domain.organization.team.entity.UserTeam</code>, <code>domain.organization.team.UserTeamStatus</code>, <code>domain.organization.team.repository.UserTeamRepository</code></td>
      <td>user-team 관계 일괄 반영 ownership 은 team feature 안에 두며, legacy <code>/members/bulk</code> 를 대체한다.</td>
    </tr>
    <tr>
      <td><code>DELETE /api/teams/{id}</code></td>
      <td><code>Documented</code></td>
      <td>팀을 soft-delete 한다.</td>
      <td><code>domain.organization.team.controller.TeamController</code></td>
      <td><code>domain.organization.team.service.TeamService</code></td>
      <td><code>domain.organization.team.entity.Team</code>, <code>domain.organization.team.repository.TeamRepository</code></td>
      <td>삭제는 row 제거가 아니라 <code>deleted_at</code> 변경으로 처리한다.</td>
    </tr>
  </tbody>
</table>
</div>


### 2.4 `organization/user` 도메인

<div style="overflow-x: auto;">
<table style="width: max-content; min-width: 100%; white-space: nowrap;">
  <thead>
    <tr>
      <th>Endpoint</th>
      <th>Status</th>
      <th>description</th>
      <th>Controller Class</th>
      <th>Service Class</th>
      <th>관련 Entity / Repository / JOOQ</th>
      <th>메모</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>GET /api/users/me</code></td>
      <td><code>Documented</code></td>
      <td>현재 로그인 사용자의 프로필/권한 컨텍스트를 조회한다.</td>
      <td><code>domain.organization.user.controller.UserController</code></td>
      <td><code>domain.organization.user.service.UserService</code></td>
      <td><code>global.security.CustomUserPrincipal</code>, <code>domain.organization.user.entity.User</code>, <code>domain.organization.user.repository.UserRepository</code>, <code>domain.organization.department.repository.DepartmentRepository</code>, <code>domain.organization.team.repository.UserTeamRepository</code>, <code>domain.organization.team.repository.TeamRepository</code></td>
      <td>로그인 컨텍스트는 <code>global.security.CustomUserPrincipal</code>에서 받고, 사용자/부서/팀 문맥은 <code>organization/user</code> feature가 조합한다.</td>
    </tr>
    <tr>
      <td><code>GET /api/users/admin-candidates</code></td>
      <td><code>Documented</code></td>
      <td>호출자 role 기준으로 관리자 선택 후보를 조회한다.</td>
      <td><code>domain.organization.user.controller.UserController</code></td>
      <td><code>domain.organization.user.service.UserService</code></td>
      <td><code>global.security.CustomUserPrincipal</code>, <code>domain.organization.user.entity.User</code>, <code>domain.organization.user.repository.UserRepository</code>, <code>domain.organization.user.repository.jooq.UserJooqRepository</code></td>
      <td><code>DIRECTOR</code>, <code>DEPT_HEAD</code> 만 호출할 수 있다. 분기 기준은 client-supplied role 이 아니라 access token 의 authenticated principal role 이며, <code>DIRECTOR</code> 는 <code>DEPT_HEAD</code> 전체, <code>DEPT_HEAD</code> 는 자기 자신만 조회한다.</td>
    </tr>
    <tr>
      <td><code>GET /api/users/department-candidates</code></td>
      <td><code>Documented</code></td>
      <td>부서에 아직 소속되지 않은 부서장 후보를 조회한다.</td>
      <td><code>domain.organization.user.controller.UserController</code></td>
      <td><code>domain.organization.user.service.UserService</code></td>
      <td><code>domain.organization.user.entity.User</code>, <code>domain.organization.user.repository.UserRepository</code></td>
      <td><code>DIRECTOR</code> 만 호출할 수 있다. <code>role_code = DEPT_HEAD</code> 이고 <code>department_id IS NULL</code> 인 사용자만 <code>userId</code>, <code>userName</code> 으로 반환한다.</td>
    </tr>
    <tr>
      <td><code>GET /api/users</code></td>
      <td><code>Documented</code></td>
      <td>사용자 목록과 조직/권한 필터 결과를 페이지네이션으로 조회한다.</td>
      <td><code>domain.organization.user.controller.UserController</code></td>
      <td><code>domain.organization.user.service.UserService</code></td>
      <td><code>domain.organization.user.entity.User</code>, <code>domain.organization.user.repository.UserRepository</code>, <code>domain.organization.user.repository.jooq.UserJooqRepository</code></td>
      <td>사용자 본체 ownership은 <code>user</code> feature가 가진다.</td>
    </tr>
    <tr>
      <td><code>GET /api/users/{id}</code></td>
      <td><code>Documented</code></td>
      <td>단일 사용자 상세와 조직 소속 문맥을 조회한다.</td>
      <td><code>domain.organization.user.controller.UserController</code></td>
      <td><code>domain.organization.user.service.UserService</code></td>
      <td><code>domain.organization.user.entity.User</code>, <code>domain.organization.user.repository.UserRepository</code>, <code>domain.organization.team.entity.UserTeam</code>, <code>domain.organization.team.repository.UserTeamRepository</code></td>
      <td>소속 관계는 team feature entity/repository를 함께 참고한다.</td>
    </tr>
    <tr>
      <td><code>GET /api/users</code></td>
      <td><code>Documented</code></td>
      <td>사용자 목록을 페이지네이션으로 부서/직급/재직상태 필터에 따라 조회한다.</td>
      <td><code>domain.organization.user.controller.UserController</code></td>
      <td><code>domain.organization.user.service.UserService</code></td>
      <td><code>domain.organization.user.entity.User</code>, <code>domain.organization.user.repository.UserRepository</code>, <code>domain.organization.user.repository.jooq.UserJooqRepository</code>, <code>domain.organization.team.entity.UserTeam</code>, <code>domain.organization.team.repository.jooq.UserTeamJooqRepository</code></td>
      <td><code>DIRECTOR</code>, <code>DEPT_HEAD</code> 만 호출할 수 있다. <code>page</code> 기본값은 1, <code>pageSize</code> 기본값은 20, 최대값은 100 이다. <code>RETIRED</code> 사용자는 제외하고, 대표 팀은 <code>tb_user_team.is_primary = true</code> membership 에서 계산한다. 정렬은 <code>role_code</code> 기준 <code>DIRECTOR</code> → <code>DEPT_HEAD</code> → <code>TEAM_LEAD</code> → <code>MEMBER</code> 순서다.</td>
    </tr>
    <tr>
      <td><code>GET /api/users/{id}</code></td>
      <td><code>Documented</code></td>
      <td>단일 사용자 상세와 전체 소속 팀 문맥을 조회한다.</td>
      <td><code>domain.organization.user.controller.UserController</code></td>
      <td><code>domain.organization.user.service.UserService</code></td>
      <td><code>domain.organization.user.entity.User</code>, <code>domain.organization.user.repository.UserRepository</code>, <code>domain.organization.team.entity.UserTeam</code>, <code>domain.organization.team.repository.UserTeamRepository</code></td>
      <td>상세 응답은 대표 팀을 top-level 필드로 중복 반환하지 않고 <code>teams[*].isPrimary</code> 로 포함한다. 소속 팀 목록은 <code>is_primary</code> 우선 후 <code>is_leader</code> 우선으로 배치한다.</td>
    </tr>
    <tr>
      <td><code>PATCH /api/users/{id}</code></td>
      <td><code>Documented</code></td>
      <td>사용자 기본 정보와 대표 소속 팀을 부분 수정한다.</td>
      <td><code>domain.organization.user.controller.UserController</code></td>
      <td><code>domain.organization.user.service.UserService</code></td>
      <td><code>domain.organization.user.entity.User</code>, <code>domain.organization.user.repository.UserRepository</code>, <code>domain.organization.team.entity.UserTeam</code>, <code>domain.organization.team.repository.UserTeamRepository</code></td>
      <td><code>DIRECTOR</code>, <code>DEPT_HEAD</code> 만 호출할 수 있다. <code>multipart/form-data</code> 의 <code>request</code> JSON part 와 선택 <code>profile_image</code> file part 를 사용한다. null 필드는 기존 값을 유지하고, <code>profile_image</code> 미첨부 시 기존 이미지를 유지한다. <code>primaryTeamId</code> 가 오면 기존 대표 팀을 해제한 뒤 요청 팀만 대표 팀으로 지정한다.</td>
    </tr>
  </tbody>
</table>
</div>

### 2.5 `organization/skill` 도메인

<div style="overflow-x: auto;">
<table style="width: max-content; min-width: 100%; white-space: nowrap;">
  <thead>
    <tr>
      <th>Endpoint</th>
      <th>Status</th>
      <th>description</th>
      <th>Controller Class</th>
      <th>Service Class</th>
      <th>관련 Entity / Repository / JOOQ</th>
      <th>메모</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>GET /api/users/{userId}/skills</code></td>
      <td><code>Documented</code></td>
      <td>특정 사용자의 보유 스킬 목록을 조회한다.</td>
      <td><code>domain.organization.skill.controller.UserSkillController</code></td>
      <td><code>domain.organization.skill.service.UserSkillService</code></td>
      <td><code>domain.organization.skill.entity.UserSkill</code>, <code>domain.organization.skill.repository.UserSkillRepository</code>, <code>domain.organization.skill.repository.jooq.UserSkillJooqRepository</code></td>
      <td><code>DIRECTOR</code>, <code>DEPT_HEAD</code> 만 호출한다. 자기 자신 조회와 <code>DEPT_HEAD</code> 의 <code>DIRECTOR</code> 조회는 빈 <code>skills</code> 목록으로 비노출한다. <code>DEPT_HEAD</code> 는 같은 부서 사용자만 조회한다.</td>
    </tr>
    <tr>
      <td><code>POST /api/users/{userId}/skills</code></td>
      <td><code>Documented</code></td>
      <td>특정 사용자에게 스킬을 추가한다.</td>
      <td><code>domain.organization.skill.controller.UserSkillController</code></td>
      <td><code>domain.organization.skill.service.UserSkillService</code></td>
      <td><code>domain.organization.skill.entity.UserSkill</code>, <code>domain.organization.skill.repository.UserSkillRepository</code></td>
      <td><code>DIRECTOR</code> 는 자기 자신을 제외한 사용자를 대상으로 등록한다. <code>DEPT_HEAD</code> 는 자기 자신과 <code>DIRECTOR</code> 를 제외한 같은 부서 사용자만 대상으로 등록한다.</td>
    </tr>
    <tr>
      <td><code>PATCH /api/users/{userId}/skills/{id}</code></td>
      <td><code>Documented</code></td>
      <td>특정 사용자의 특정 스킬 정보를 수정한다.</td>
      <td><code>domain.organization.skill.controller.UserSkillController</code></td>
      <td><code>domain.organization.skill.service.UserSkillService</code></td>
      <td><code>domain.organization.skill.entity.UserSkill</code>, <code>domain.organization.skill.repository.UserSkillRepository</code></td>
      <td><code>userId</code>는 사용자 식별자, <code>id</code>는 스킬 레코드 식별자다.</td>
    </tr>
    <tr>
      <td><code>DELETE /api/users/{userId}/skills/{id}</code></td>
      <td><code>Documented</code></td>
      <td>특정 사용자의 특정 스킬을 삭제한다.</td>
      <td><code>domain.organization.skill.controller.UserSkillController</code></td>
      <td><code>domain.organization.skill.service.UserSkillService</code></td>
      <td><code>domain.organization.skill.entity.UserSkill</code>, <code>domain.organization.skill.repository.UserSkillRepository</code></td>
      <td><code>userId</code>는 사용자 식별자, <code>id</code>는 스킬 레코드 식별자다.</td>
    </tr>
  </tbody>
</table>
</div>


### 2.6 `organization/evaluation` 도메인

<div style="overflow-x: auto;">
<table style="width: max-content; min-width: 100%; white-space: nowrap;">
  <thead>
    <tr>
      <th>Endpoint</th>
      <th>Status</th>
      <th>description</th>
      <th>Controller Class</th>
      <th>Service Class</th>
      <th>관련 Entity / Repository / JOOQ</th>
      <th>메모</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>GET /api/users/{userId}/evaluations</code></td>
      <td><code>Documented</code></td>
      <td>특정 사용자의 평가 이력을 조회한다.</td>
      <td><code>domain.organization.evaluation.controller.UserEvaluationController</code></td>
      <td><code>domain.organization.evaluation.service.UserEvaluationService</code></td>
      <td><code>domain.organization.evaluation.entity.UserEvaluation</code>, <code>domain.organization.evaluation.repository.UserEvaluationRepository</code>, <code>domain.organization.evaluation.repository.jooq.UserEvaluationJooqRepository</code></td>
      <td>평가 read/write ownership은 <code>organization/evaluation</code> feature가 가진다.</td>
    </tr>
    <tr>
      <td><code>POST /api/users/{userId}/evaluations</code></td>
      <td><code>Documented</code></td>
      <td>특정 사용자에 대한 평가를 등록한다.</td>
      <td><code>domain.organization.evaluation.controller.UserEvaluationController</code></td>
      <td><code>domain.organization.evaluation.service.UserEvaluationService</code></td>
      <td><code>domain.organization.evaluation.entity.UserEvaluation</code>, <code>domain.organization.evaluation.repository.UserEvaluationRepository</code></td>
      <td>평가 생성 규칙과 권한 검증은 서비스에서 처리한다.</td>
    </tr>
    <tr>
      <td><code>PATCH /api/users/{userId}/evaluations/{id}</code></td>
      <td><code>Documented</code></td>
      <td>특정 사용자의 평가 내용을 수정한다.</td>
      <td><code>domain.organization.evaluation.controller.UserEvaluationController</code></td>
      <td><code>domain.organization.evaluation.service.UserEvaluationService</code></td>
      <td><code>domain.organization.evaluation.entity.UserEvaluation</code>, <code>domain.organization.evaluation.repository.UserEvaluationRepository</code></td>
      <td><code>userId</code>는 사용자 식별자, <code>id</code>는 평가 레코드 식별자다.</td>
    </tr>
    <tr>
      <td><code>DELETE /api/users/{userId}/evaluations/{id}</code></td>
      <td><code>Documented</code></td>
      <td>특정 사용자의 평가를 삭제한다.</td>
      <td><code>domain.organization.evaluation.controller.UserEvaluationController</code></td>
      <td><code>domain.organization.evaluation.service.UserEvaluationService</code></td>
      <td><code>domain.organization.evaluation.entity.UserEvaluation</code>, <code>domain.organization.evaluation.repository.UserEvaluationRepository</code></td>
      <td><code>userId</code>는 사용자 식별자, <code>id</code>는 평가 레코드 식별자다.</td>
    </tr>
  </tbody>
</table>
</div>


### 2.7 `worklog` 도메인

<div style="overflow-x: auto;">
<table style="width: max-content; min-width: 100%; white-space: nowrap;">
  <thead>
    <tr>
      <th>Endpoint</th>
      <th>Status</th>
      <th>description</th>
      <th>Controller Class</th>
      <th>Service Class</th>
      <th>관련 Entity / Repository / JOOQ</th>
      <th>메모</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>GET /api/worklogs</code></td>
      <td><code>Documented</code></td>
      <td>업무일지 목록과 필터링/정렬 결과를 조회한다.</td>
      <td><code>domain.worklog.controller.WorklogController</code></td>
      <td><code>domain.worklog.service.WorklogService</code></td>
      <td><code>domain.worklog.entity.Worklog</code>, <code>domain.worklog.repository.WorklogRepository</code>, <code>domain.worklog.repository.jooq.WorklogJooqRepository</code></td>
      <td>목록 조회는 Service의 readOnly 메서드 + JOOQ 조합 여지를 전제로 둔다.</td>
    </tr>
    <tr>
      <td><code>GET /api/worklogs/{id}</code></td>
      <td><code>Documented</code></td>
      <td>단일 업무일지 상세와 AI/태그/소속 문맥을 조회한다.</td>
      <td><code>domain.worklog.controller.WorklogController</code></td>
      <td><code>domain.worklog.service.WorklogService</code></td>
      <td><code>domain.worklog.entity.Worklog</code>, <code>domain.worklog.entity.WorklogTag</code>, <code>domain.worklog.repository.WorklogRepository</code>, <code>domain.worklog.repository.jooq.WorklogJooqRepository</code></td>
      <td>상세 조회는 태그/파일/조직 문맥을 함께 조합할 수 있다.</td>
    </tr>
    <tr>
      <td><code>POST /api/worklogs</code></td>
      <td><code>Documented</code></td>
      <td>새 업무일지를 생성하고 초기 AI 파이프라인을 트리거한다.</td>
      <td><code>domain.worklog.controller.WorklogController</code></td>
      <td><code>domain.worklog.service.WorklogService</code></td>
      <td><code>domain.worklog.entity.Worklog</code>, <code>domain.worklog.repository.WorklogRepository</code></td>
      <td>생성 ownership은 <code>WorklogService</code>가 가진다.</td>
    </tr>
    <tr>
      <td><code>PUT /api/worklogs/{id}</code></td>
      <td><code>Documented</code></td>
      <td>업무일지 기본 정보와 재처리 플래그를 수정한다.</td>
      <td><code>domain.worklog.controller.WorklogController</code></td>
      <td><code>domain.worklog.service.WorklogService</code></td>
      <td><code>domain.worklog.entity.Worklog</code>, <code>domain.worklog.repository.WorklogRepository</code></td>
      <td>수정 시 AI 재생성 정책을 함께 다룰 수 있다.</td>
    </tr>
    <tr>
      <td><code>DELETE /api/worklogs/{id}</code></td>
      <td><code>Documented</code></td>
      <td>업무일지를 삭제 또는 soft delete 처리한다.</td>
      <td><code>domain.worklog.controller.WorklogController</code></td>
      <td><code>domain.worklog.service.WorklogService</code></td>
      <td><code>domain.worklog.entity.Worklog</code>, <code>domain.worklog.repository.WorklogRepository</code></td>
      <td>실제 삭제 정책은 후속 구현에서 결정하되 ownership은 동일하다.</td>
    </tr>
    <tr>
      <td><code>PATCH /api/worklogs/{id}/status</code></td>
      <td><code>Documented</code></td>
      <td>업무 상태 전이와 이력 기록을 담당한다.</td>
      <td><code>domain.worklog.controller.WorklogController</code></td>
      <td><code>domain.worklog.service.WorklogStatusService</code></td>
      <td><code>domain.worklog.entity.Worklog</code>, <code>domain.worklog.entity.WorklogStatusHistory</code>, <code>domain.worklog.repository.WorklogStatusHistoryRepository</code>, <code>domain.worklog.repository.jooq.WorklogStatusHistoryJooqRepository</code></td>
      <td>상태 전이 규칙은 <code>WorklogStatusPolicy</code>와 함께 읽는다.</td>
    </tr>
    <tr>
      <td><code>GET /api/worklogs/{id}/history</code></td>
      <td><code>Documented</code></td>
      <td>업무 상태 변경 이력을 조회한다.</td>
      <td><code>domain.worklog.controller.WorklogController</code></td>
      <td><code>domain.worklog.service.WorklogStatusService</code></td>
      <td><code>domain.worklog.entity.WorklogStatusHistory</code>, <code>domain.worklog.repository.WorklogStatusHistoryRepository</code>, <code>domain.worklog.repository.jooq.WorklogStatusHistoryJooqRepository</code></td>
      <td>이력 조회 ownership은 status 서비스에 둔다.</td>
    </tr>
    <tr>
      <td><code>PATCH /api/worklogs/{id}/summary</code></td>
      <td><code>Documented</code></td>
      <td>AI가 생성한 업무 요약을 원장 DB에 반영한다.</td>
      <td><code>domain.worklog.controller.InternalWorklogAiCallbackController</code></td>
      <td><code>domain.worklog.service.InternalWorklogAiCallbackService</code></td>
      <td><code>domain.worklog.entity.Worklog</code>, <code>domain.worklog.repository.WorklogRepository</code></td>
      <td>현행 코드 기준 internal callback controller base path는 <code>/api/internal/worklog</code> 이지만, endpoint/status 표기는 <code>api-design</code> 상세 본문 계약(<code>/api/{domain}/{id}/{action}</code>)을 우선 따른다.</td>
    </tr>
    <tr>
      <td><code>PUT /api/worklogs/{id}/tags</code></td>
      <td><code>Documented</code></td>
      <td>AI가 생성/정규화한 태그 연결을 업무일지에 반영한다.</td>
      <td><code>domain.worklog.controller.InternalWorklogAiCallbackController</code></td>
      <td><code>domain.worklog.service.InternalWorklogAiCallbackService</code></td>
      <td><code>domain.worklog.entity.WorklogTag</code>, <code>domain.worklog.repository.WorklogTagRepository</code>, <code>domain.worklog.repository.jooq.WorklogTagJooqRepository</code></td>
      <td>현행 코드 기준 internal callback controller base path는 <code>/api/internal/worklog</code> 이지만, endpoint/status 표기는 <code>api-design</code> 상세 본문 계약(<code>/api/{domain}/{id}/{action}</code>)을 우선 따른다.</td>
    </tr>
    <tr>
      <td><code>PATCH /api/worklogs/{id}/ai-status</code></td>
      <td><code>Documented</code></td>
      <td>업무일지 AI 처리 진행 상태를 반영한다.</td>
      <td><code>domain.worklog.controller.InternalWorklogAiCallbackController</code></td>
      <td><code>domain.worklog.service.InternalWorklogAiCallbackService</code></td>
      <td><code>domain.worklog.entity.Worklog</code>, <code>domain.worklog.repository.WorklogRepository</code></td>
      <td>현행 코드 기준 internal callback controller base path는 <code>/api/internal/worklog</code> 이지만, endpoint/status 표기는 <code>api-design</code> 상세 본문 계약(<code>/api/{domain}/{id}/{action}</code>)을 우선 따른다.</td>
    </tr>
  </tbody>
</table>
</div>


### 2.8 `file` 도메인

<div style="overflow-x: auto;">
<table style="width: max-content; min-width: 100%; white-space: nowrap;">
  <thead>
    <tr>
      <th>Endpoint</th>
      <th>Status</th>
      <th>description</th>
      <th>Controller Class</th>
      <th>Service Class</th>
      <th>관련 Entity / Repository / JOOQ</th>
      <th>메모</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>POST /api/files/upload</code></td>
      <td><code>Documented</code></td>
      <td>파일 업로드와 메타데이터 저장을 담당한다.</td>
      <td><code>domain.file.controller.FileController</code></td>
      <td><code>domain.file.service.FileService</code></td>
      <td><code>domain.file.entity.File</code>, <code>domain.file.repository.FileRepository</code>, <code>domain.file.external.ObjectStoragePort</code></td>
      <td>저장소 추상화는 <code>ObjectStoragePort</code> / <code>S3ObjectStorageAdapter</code>로 분리한다.</td>
    </tr>
    <tr>
      <td><code>GET /api/files</code></td>
      <td><code>Inferred-required</code></td>
      <td>파일 목록/필터/AI 요약 검색을 지원하는 파일 관리 조회 API다.</td>
      <td><code>domain.file.controller.FileController</code></td>
      <td><code>domain.file.service.FileService</code></td>
      <td><code>domain.file.entity.File</code>, <code>domain.file.repository.FileRepository</code>, <code>domain.file.repository.jooq.FileJooqRepository</code></td>
      <td><code>api-design</code> 기준 필수 추론 API이며 목록 화면 요구를 따른다.</td>
    </tr>
    <tr>
      <td><code>GET /api/files/{id}</code></td>
      <td><code>Proposed-risk-closure</code></td>
      <td>파일 상세 메타데이터와 AI 상태를 안정적으로 조회하는 운영 보강 API다.</td>
      <td><code>domain.file.controller.FileController</code></td>
      <td><code>domain.file.service.FileService</code></td>
      <td><code>domain.file.entity.File</code>, <code>domain.file.repository.FileRepository</code>, <code>domain.file.repository.jooq.FileJooqRepository</code></td>
      <td>목록/다운로드와 별도로 상세 UX 완결성을 위해 제안된 API다.</td>
    </tr>
    <tr>
      <td><code>GET /api/files/{id}/download</code></td>
      <td><code>Documented</code></td>
      <td>원본 파일 다운로드 또는 presigned URL 발급을 담당한다.</td>
      <td><code>domain.file.controller.FileController</code></td>
      <td><code>domain.file.service.FileService</code></td>
      <td><code>domain.file.entity.File</code>, <code>domain.file.repository.FileRepository</code>, <code>domain.file.external.ObjectStoragePort</code></td>
      <td>파일 접근 정책은 <code>FileAccessPolicy</code>와 함께 검토한다.</td>
    </tr>
    <tr>
      <td><code>DELETE /api/files/{id}</code></td>
      <td><code>Documented</code></td>
      <td>파일 soft delete와 후처리 정리를 시작한다.</td>
      <td><code>domain.file.controller.FileController</code></td>
      <td><code>domain.file.service.FileService</code></td>
      <td><code>domain.file.entity.File</code>, <code>domain.file.repository.FileRepository</code></td>
      <td>실제 저장소 정리는 비동기 후속 처리로 분리할 수 있다.</td>
    </tr>
    <tr>
      <td><code>PUT /api/files/{id}/summary</code></td>
      <td><code>Inferred-required</code></td>
      <td>AI가 생성한 파일 요약을 파일 메타데이터에 반영한다.</td>
      <td><code>domain.file.controller.InternalFileAiCallbackController</code></td>
      <td><code>domain.file.service.InternalFileAiCallbackService</code></td>
      <td><code>domain.file.entity.File</code>, <code>domain.file.repository.FileRepository</code></td>
      <td>현행 코드 기준 internal callback controller base path는 <code>/api/internal/files</code> 이지만, endpoint/status 표기는 <code>api-design</code> 상세 본문 계약(<code>/api/{domain}/{id}/{action}</code>)을 우선 따른다.</td>
    </tr>
    <tr>
      <td><code>PATCH /api/files/{id}/ai-status</code></td>
      <td><code>Inferred-required</code></td>
      <td>파일 AI 처리 진행 상태를 반영한다.</td>
      <td><code>domain.file.controller.InternalFileAiCallbackController</code></td>
      <td><code>domain.file.service.InternalFileAiCallbackService</code></td>
      <td><code>domain.file.entity.File</code>, <code>domain.file.repository.FileRepository</code></td>
      <td>현행 코드 기준 internal callback controller base path는 <code>/api/internal/files</code> 이지만, endpoint/status 표기는 <code>api-design</code> 상세 본문 계약(<code>/api/{domain}/{id}/{action}</code>)을 우선 따른다.</td>
    </tr>
  </tbody>
</table>
</div>


### 2.9 `tag` 도메인

<div style="overflow-x: auto;">
<table style="width: max-content; min-width: 100%; white-space: nowrap;">
  <thead>
    <tr>
      <th>Endpoint</th>
      <th>Status</th>
      <th>description</th>
      <th>Controller Class</th>
      <th>Service Class</th>
      <th>관련 Entity / Repository / JOOQ</th>
      <th>메모</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>GET /api/tags</code></td>
      <td><code>Documented</code></td>
      <td>태그 풀 목록과 사용량을 조회한다.</td>
      <td><code>domain.tag.controller.TagController</code></td>
      <td><code>domain.tag.service.TagService</code></td>
      <td><code>domain.tag.entity.MetaTag</code>, <code>domain.tag.repository.TagRepository</code>, <code>domain.tag.repository.jooq.MetaTagJooqRepository</code></td>
      <td>태그 풀은 worklog에서 재사용하지만 ownership은 <code>tag</code> 도메인에 둔다.</td>
    </tr>
    <tr>
      <td><code>POST /api/tags/merge</code></td>
      <td><code>Documented</code></td>
      <td>중복/유사 태그를 병합해 태그 품질을 유지한다.</td>
      <td><code>domain.tag.controller.TagController</code></td>
      <td><code>domain.tag.service.TagService</code></td>
      <td><code>domain.tag.entity.MetaTag</code>, <code>domain.worklog.entity.WorklogTag</code>, <code>domain.tag.repository.TagRepository</code>, <code>domain.worklog.repository.WorklogTagRepository</code></td>
      <td>태그 merge는 worklog-tag 연결 재배치를 동반한다.</td>
    </tr>
    <tr>
      <td><code>DELETE /api/tags/{id}</code></td>
      <td><code>Inferred-required</code></td>
      <td>사용 중단 태그를 정리하는 운영 관리 API다.</td>
      <td><code>domain.tag.controller.TagController</code></td>
      <td><code>domain.tag.service.TagService</code></td>
      <td><code>domain.tag.entity.MetaTag</code>, <code>domain.worklog.entity.WorklogTag</code>, <code>domain.tag.repository.TagRepository</code>, <code>domain.worklog.repository.WorklogTagRepository</code></td>
      <td>삭제 가능 여부는 태그 사용량과 연결 상태를 함께 본다.</td>
    </tr>
  </tbody>
</table>
</div>


### 2.10 `notification` 도메인

<div style="overflow-x: auto;">
<table style="width: max-content; min-width: 100%; white-space: nowrap;">
  <thead>
    <tr>
      <th>Endpoint</th>
      <th>Status</th>
      <th>description</th>
      <th>Controller Class</th>
      <th>Service Class</th>
      <th>관련 Entity / Repository / JOOQ</th>
      <th>메모</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>GET /api/notifications</code></td>
      <td><code>Documented</code></td>
      <td>사용자 개인 알림 목록을 조회한다.</td>
      <td><code>domain.notification.controller.NotificationController</code></td>
      <td><code>domain.notification.service.NotificationService</code></td>
      <td><code>domain.notification.entity.Notification</code>, <code>domain.notification.repository.NotificationRepository</code>, <code>domain.notification.repository.jooq.NotificationJooqRepository</code></td>
      <td>알림 조회 ownership은 NotificationService의 readOnly 메서드가 가진다.</td>
    </tr>
    <tr>
      <td><code>GET /api/notifications/unread-count</code></td>
      <td><code>Documented</code></td>
      <td>읽지 않은 알림 수를 경량 조회한다.</td>
      <td><code>domain.notification.controller.NotificationController</code></td>
      <td><code>domain.notification.service.NotificationService</code></td>
      <td><code>domain.notification.entity.Notification</code>, <code>domain.notification.repository.NotificationRepository</code>, <code>domain.notification.repository.jooq.NotificationJooqRepository</code></td>
      <td>GNB count 용도라 단건 카운트에 집중한다.</td>
    </tr>
    <tr>
      <td><code>PATCH /api/notifications/read-all</code></td>
      <td><code>Documented</code></td>
      <td>본인 알림을 일괄 읽음 처리한다.</td>
      <td><code>domain.notification.controller.NotificationController</code></td>
      <td><code>domain.notification.service.NotificationService</code></td>
      <td><code>domain.notification.entity.Notification</code>, <code>domain.notification.repository.NotificationRepository</code></td>
      <td>쓰기 ownership은 <code>NotificationService</code>에 둔다.</td>
    </tr>
    <tr>
      <td><code>PATCH /api/notifications/{id}/read</code></td>
      <td><code>Inferred-required</code></td>
      <td>개별 알림 클릭 시 단건 읽음 처리를 수행한다.</td>
      <td><code>domain.notification.controller.NotificationController</code></td>
      <td><code>domain.notification.service.NotificationService</code></td>
      <td><code>domain.notification.entity.Notification</code>, <code>domain.notification.repository.NotificationRepository</code></td>
      <td>UI 흐름 기반의 필수 추론 API다.</td>
    </tr>
  </tbody>
</table>
</div>


### 2.11 `dashboard` 도메인

<div style="overflow-x: auto;">
<table style="width: max-content; min-width: 100%; white-space: nowrap;">
  <thead>
    <tr>
      <th>Endpoint</th>
      <th>Status</th>
      <th>description</th>
      <th>Controller Class</th>
      <th>Service Class</th>
      <th>관련 Entity / Repository / JOOQ</th>
      <th>메모</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>GET /api/dashboards/summary</code></td>
      <td><code>Documented</code></td>
      <td>대시보드 핵심 집계 요약을 조회한다.</td>
      <td><code>domain.dashboard.controller.DashboardController</code></td>
      <td><code>domain.dashboard.service.DashboardService</code></td>
      <td><code>domain.dashboard.readmodel.*</code>, <code>domain.dashboard.repository.jooq.DashboardJooqRepository</code></td>
      <td><code>dashboard</code>는 query-only 조합 계층이다.</td>
    </tr>
    <tr>
      <td><code>GET /api/dashboards/overdue</code></td>
      <td><code>Documented</code></td>
      <td>지연 업무 목록을 집계 조회한다.</td>
      <td><code>domain.dashboard.controller.DashboardController</code></td>
      <td><code>domain.dashboard.service.DashboardService</code></td>
      <td><code>domain.dashboard.readmodel.*</code>, <code>domain.dashboard.repository.jooq.DashboardJooqRepository</code></td>
      <td>원본 상태 변경 ownership은 다른 도메인에 남긴다.</td>
    </tr>
    <tr>
      <td><code>GET /api/dashboards/workload</code></td>
      <td><code>Documented</code></td>
      <td>사용자/팀별 workload 분포를 조회한다.</td>
      <td><code>domain.dashboard.controller.DashboardController</code></td>
      <td><code>domain.dashboard.service.DashboardService</code></td>
      <td><code>domain.dashboard.readmodel.*</code>, <code>domain.dashboard.repository.jooq.DashboardJooqRepository</code></td>
      <td>read-heavy 조합 로직은 <code>repository/jooq</code> 쪽에 두는 기준과 맞춘다.</td>
    </tr>
    <tr>
      <td><code>GET /api/dashboards/my</code></td>
      <td><code>Documented</code></td>
      <td>로그인 사용자 기준 개인 대시보드 묶음을 조회한다.</td>
      <td><code>domain.dashboard.controller.DashboardController</code></td>
      <td><code>domain.dashboard.service.DashboardService</code></td>
      <td><code>domain.dashboard.readmodel.*</code>, <code>domain.dashboard.repository.jooq.DashboardJooqRepository</code></td>
      <td>개인화 조회지만 write ownership은 가지지 않는다.</td>
    </tr>
  </tbody>
</table>
</div>
