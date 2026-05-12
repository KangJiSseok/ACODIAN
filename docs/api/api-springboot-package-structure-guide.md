# AX-WMS Spring Boot `/api` 패키지 구조 설계서

- 기준 문서: `plan/api-design.md`
- 목적: AX-WMS `/api`를 Spring Boot에서 구현할 때, **기본은 도메인 → 레이어**, **예외는 `organization`만 도메인 → 기능 → 레이어** 구조로 설계하기 위한 실행 기준을 제공한다.
- 범위: 도메인 패키지 구조, repository/entity 배치, 구현 순서, 구조 검증 기준
- 전제: base package 예시는 `com.ibank.axwms`
- 연계 문서: `docs/api/api-springboot-endpoint-class-mapping.md`는 endpoint/API별 class-level ownership 매핑만 별도 관리한다.

---

## 1. 구조 기본 원칙

이 문서의 canonical 구조는 루트 전역 `controller/service/repository/entity` 분리가 아니라,
`global/`, `domain/` 두 축을 먼저 세우는 방식이다.

- `global/`: 응답/예외/config/security/jooq 같은 **횡단 관심사와 기술 인프라**
- `domain/`: `auth`, `organization`, `worklog`, `file`, `tag`, `notification`, `dashboard` 같은 **업무 도메인**

지양하는 legacy root 예시:

legacy root는 공통 shared root와 업무 도메인이 같은 깊이에 섞여 canonical root가 흔들리는 문제가 있다.

따라서 AX-WMS `/api`의 구조 원칙은 다음과 같다.

- 루트는 **`global/`, `domain/` 두 축으로만 정규화**한다.
- 기본 구조는 `domain/<domain>/<layer>` 이다.
- `organization`만 예외적으로 `domain/organization/<feature>/<layer>` 구조를 사용한다.
- `dashboard`는 `domain/dashboard` 아래의 **read/query composition owner**이며 write owner가 아니다.
- `global/* -> domain/*` 의존은 금지하고, `domain/* -> global/*` 의존은 허용한다.
- 이번 작업은 **문서 canonicalization**이며, 실제 코드 패키지 이동은 후속 리팩터링으로 분리한다.


### 1.1 source precedence / 해석 규칙
- endpoint path / method / status는 `plan/api-design.md`의 **상세 endpoint 본문**을 우선한다.
- callback 요약표나 구현 순서 요약은 보조 근거로만 사용한다.
- class/package naming은 현재 코드 구조와 `docs/api/adr.yaml`, `docs/api/code-convention.yaml`을 함께 대조해 해석한다.
- 이번 문서는 **class-level ownership 기준의 canonicalization**이며, method-level / DTO-level 이름 확정이나 실제 코드 이동 지시는 포함하지 않는다.


## 2. 최상위 패키지 구조

```text
com.ibank.axwms
├── AxWmsApplication.java
├── global/
│   ├── response/
│   ├── error/
│   ├── config/
│   ├── security/
│   └── jooq/
└── domain/
    ├── auth/
    ├── organization/
    ├── worklog/
    ├── file/
    ├── tag/
    ├── notification/
    └── dashboard/
```

### 2.1 canonical root 설명
- `global`: 공통 응답, 예외 처리, 보안 설정, 공통 config, JOOQ 공통 설정 같은 shared technical root
- `domain/auth`: 로그인, 로그아웃, me, refresh, change-password 같은 인증/인가 유스케이스
- `domain/organization`: 부서 / 팀 / 사용자 / 사용자-팀 / 스킬 / 평가
- `domain/worklog`: 업무일지, 상태 이력, 의존성, AI callback
- `domain/file`: 파일 업로드/조회/삭제, object storage, 파일 AI callback
- `domain/tag`: 메타 태그 풀, 태그 병합/삭제
- `domain/notification`: 개인 알림 조회/읽음 처리
- `domain/dashboard`: 집계/조회 전용 read model, query composition owner
- base package 예시: `com.ibank.axwms.global`, `com.ibank.axwms.domain.auth`, `com.ibank.axwms.domain.organization`, `com.ibank.axwms.domain.dashboard`

### 2.2 ownership matrix

| 영역 | 소유 책임 | 두지 않는 것 |
|---|---|---|
| `global/security` | SecurityConfig, JWT filter/provider, CustomUserPrincipal, 보안 어노테이션/infra | 로그인/로그아웃 같은 인증 유스케이스 |
| `domain/auth` | 로그인, 토큰 발급/재발급, 로그아웃, 비밀번호 변경, 인증 정책 | JWT filter/config 같은 횡단 보안 인프라 |
| `domain/dashboard` | 여러 도메인의 조회/집계/query composition | 원본 도메인 데이터의 생성/수정/삭제 write ownership |


## 3. AX-WMS 도메인 패키지 권장안

### 3.1 `global`

```text
global/
├── response/
│   ├── ApiResponse<T>
│   ├── PageResponse<T>
│   └── EmptyResponse
├── config/
│   ├── SecurityConfig
│   ├── JpaConfig
│   ├── RedisConfig
│   ├── CacheConfig
│   └── OpenApiConfig
├── error/
│   ├── ErrorCode
│   ├── ErrorResponse
│   ├── BusinessException
│   └── GlobalExceptionHandler
├── security/
│   ├── JwtAuthenticationFilter
│   ├── JwtTokenProvider
│   └── CustomUserPrincipal
└── util/
```

제한:
- 비즈니스 서비스 두지 않음
- 특정 도메인 entity/repository 두지 않음
- 다른 도메인 의존을 만들지 않음

### 3.2 `auth`

```text
domain/auth/
├── controller/
│   └── AuthController
├── service/
│   ├── AuthService
│   └── TokenService
├── repository/
│   └── RefreshTokenRepository
├── dto/
├── entity/
│   └── RefreshToken
├── policy/
│   └── PasswordPolicy
└── external/
    └── JwtTokenProviderAdapter
```

### 3.3 `organization` — 유일한 예외 구조

`organization`은 `department`, `team`, `user`, `skill`, `evaluation`이 강하게 연결되어 있으므로 이 도메인만 **도메인 → 기능 → 레이어** 구조를 허용한다.

```text
domain/organization/
├── department/
│   ├── controller/
│   │   └── DepartmentController
│   ├── service/
│   │   └── DepartmentService
│   ├── repository/
│   │   └── DepartmentRepository
│   ├── dto/
│   └── entity/
│       └── Department
├── team/
│   ├── controller/
│   │   └── TeamController
│   ├── service/
│   │   └── TeamService
│   ├── repository/
│   │   ├── TeamRepository
│   │   └── UserTeamRepository
│   ├── dto/
│   └── entity/
│       ├── Team
│       └── UserTeam
├── user/
│   ├── controller/
│   │   └── UserController
│   ├── service/
│   │   └── UserService
│   ├── repository/
│   │   └── UserRepository
│   ├── dto/
│   └── entity/
│       └── User
├── skill/
│   ├── controller/
│   │   └── UserSkillController
│   ├── service/
│   │   └── UserSkillService
│   ├── repository/
│   │   └── UserSkillRepository
│   ├── dto/
│   └── entity/
│       └── UserSkill
└── evaluation/
    ├── controller/
    │   └── UserEvaluationController
    ├── service/
    │   └── UserEvaluationService
    ├── repository/
    │   └── UserEvaluationRepository
    ├── dto/
    └── entity/
        └── UserEvaluation
```

규칙:
- `organization` 외 도메인에는 같은 feature-first 규칙을 기본 적용하지 않는다.
- 사용자-팀 관계(`tb_user_team`)처럼 기능 경계가 겹치는 타입은 ownership이 높은 feature 아래에 둔다.
- `skills`, `evaluations`는 릴리스 순서상 뒤로 갈 수 있어도 패키지 구조상으로는 `organization` 하위 feature다.

### 3.4 `worklog`

```text
domain/worklog/
├── controller/
│   ├── WorklogController
│   └── InternalWorklogAiCallbackController
├── service/
│   ├── WorklogService
│   ├── WorklogStatusService
│   └── InternalWorklogAiCallbackService
├── repository/
│   ├── WorklogRepository
│   ├── WorklogStatusHistoryRepository
│   ├── WorklogDependencyRepository
│   └── WorklogTagRepository
├── dto/
├── entity/
│   ├── Worklog
│   ├── WorklogStatusHistory
│   ├── WorklogDependency
│   └── WorklogTag
└── policy/
    ├── WorklogStatusPolicy
    └── WorklogDependencyPolicy
```

### 3.5 `file`

```text
domain/file/
├── controller/
│   ├── FileController
│   └── InternalFileAiCallbackController
├── service/
│   ├── FileService
│   └── InternalFileAiCallbackService
├── repository/
│   └── FileRepository
├── dto/
├── entity/
│   └── File
├── policy/
│   ├── FileUploadPolicy
│   └── FileAccessPolicy
└── external/
    ├── ObjectStoragePort
    ├── S3ObjectStorageAdapter
    └── FilePathGenerator
```

### 3.6 `tag`

```text
domain/tag/
├── controller/
│   └── TagController
├── service/
│   └── TagService
├── repository/
│   ├── TagRepository
│   └── WorklogTagRepository
├── dto/
├── entity/
│   └── MetaTag
└── policy/
    └── TagMergePolicy
```

> **소형 도메인 정책**: 소형 도메인이어도 기본 축은 `도메인 → 레이어`를 유지한다.

### 3.7 `notification`

```text
domain/notification/
├── controller/
│   └── NotificationController
├── service/
│   └── NotificationService
├── repository/
│   └── NotificationRepository
├── dto/
├── entity/
│   └── Notification
└── policy/
    └── NotificationReadPolicy
```

### 3.8 `dashboard`

```text
domain/dashboard/
├── controller/
│   └── DashboardController
├── service/
│   └── DashboardService
├── dto/
├── repository/
│   └── jooq/
└── readmodel/
```

특징:
- write domain이 아니라 **조회용 집계 도메인**으로 다룬다.
- `entity`보다 `readmodel`, `repository/jooq` 비중이 높다.

---

## 4. 도메인 패키지 간 허용 의존 방향

```text
domain/auth -> domain/organization, global/*
domain/worklog -> domain/organization, domain/tag, global/*
domain/file -> domain/worklog, domain/organization, global/*
domain/notification -> domain/organization, global/*
domain/dashboard -> domain/organization, domain/worklog, domain/notification, global/*
```

추가 원칙:
- `global/* -> domain/*` 의존은 금지한다.
- `domain/* -> global/*` 의존은 허용한다.
- `tag`는 가능하면 `domain/worklog`에 종속되지 않고 독립 유지한다.
- `notification`은 다른 도메인을 직접 호출하기보다 event/ID 기반으로 느슨하게 연결한다.
- `dashboard`는 여러 도메인을 읽지만, 다른 도메인이 `domain/dashboard`에 의존하면 안 된다.


## 5. 도메인 내부 표준 구조

기본적으로 각 도메인은 아래 canonical 구조를 따른다.

```text
domain/<domain>/
├── controller/
│   └── <Domain>Controller
├── service/
├── repository/
├── dto/
├── entity/
└── policy/
```

### 역할
- `controller`: HTTP endpoint 진입점
- `dto`: 요청/응답 DTO, query DTO
- `service`: 트랜잭션 경계, 유스케이스 조합, 애플리케이션 서비스. 조회 유스케이스도 기본적으로 같은 Service의 readOnly 메서드로 두며 `*QueryService` 분리 규칙은 사용하지 않는다.
- `repository`: 영속성 접근의 진입점. 기본 Repository와 필요 시 `repository/jooq` 보조 구현을 함께 둔다.
- `entity`: JPA entity 또는 핵심 aggregate
- `policy`: 상태 전이/검증 규칙 등 보조 로직

### `organization` 예외

```text
domain/organization/
└── <feature>/
    ├── controller/
    ├── service/
    ├── repository/
    ├── dto/
    └── entity/
```

### `global` 규칙

```text
global/
├── response/
├── error/
├── config/
├── security/
└── jooq/
```

- `global`에는 공통 응답/예외/config/security/jooq 같은 기술 인프라만 둔다.
- `global`에는 비즈니스 서비스, 특정 도메인 entity/repository를 두지 않는다.
- internal callback controller는 `domain/<module>/controller/Internal{Module}{Purpose}Controller` 패턴으로 둔다.
- Docs 인터페이스는 기본적으로 `SB-018`의 `{Module}ControllerDocs`를 따르되, 동일 패키지에 복수 controller가 있거나 `Internal*Controller`처럼 1:1 대응이 필요할 때는 `{ControllerClassName}Docs`를 허용한다.
- internal callback DTO는 `domain/<module>/dto/Internal*Request|Response` naming 으로 둔다.
- internal orchestration service는 `domain/<module>/service/Internal*Service` naming 으로 둔다.
- 위 `Internal*` prefix 규칙은 internal callback controller/DTO/service 와 private integration DTO/service 같은 비공개 내부 연동 타입에 한정한다.
- read-heavy 도메인은 `domain/dashboard/readmodel`, `domain/dashboard/repository/jooq`처럼 조회 전용 보조 패키지를 추가할 수 있다.
- `ControllerDocs` 인터페이스는 Swagger/OpenAPI 문서화 surface이며, `docs/api/api-springboot-endpoint-class-mapping.md`의 class-level 매핑 범위에서는 제외한다.
- `docs/api/api-springboot-endpoint-class-mapping.md`의 관련 Entity·Repository·JOOQ 컬럼은 ownership/context 식별용이며, direct dependency 허용표가 아니다.

## 6. Entity / Repository 배치 원칙

### 6.1 권장 원칙
- JPA entity를 꼭 별도 domain model과 분리할 필요는 없다.
- AX-WMS처럼 CRUD + 정책 로직이 혼합된 서버에서는 아래 둘 중 하나를 선택하면 된다.

#### 옵션 A — entity = JPA entity
- 장점: 단순하고 빠르다.
- 단점: 도메인과 persistence가 강하게 묶인다.
- 추천 상황: 초기 구현 속도가 중요할 때

#### 옵션 B — domain model / persistence entity 분리
- 장점: 도메인 경계와 순도가 높다.
- 단점: 매핑 비용이 크다.
- 추천 상황: 장기적으로 복잡도가 매우 높아질 때

### 6.2 AX-WMS 추천
AX-WMS 초기/중기 단계에서는 **옵션 A를 기본으로 하되, `repository/jooq`, `domain.dashboard.readmodel`, `external` adapter만 선택적으로 분리**하는 방식이 가장 현실적이다.

즉:
- `domain.organization.user.entity.User` = JPA entity 가능
- `domain.worklog.entity.Worklog` = JPA entity 가능
- `domain.dashboard.readmodel.*` = 별도 projection
- 복잡한 조회는 각 도메인의 `repository/jooq` 보조 구현으로 분리 가능
- 외부 연동(S3, refresh token 저장 전략 등)만 adapter로 분리



## 7. 구조 검증 시 권장 점검 항목

### 7.1 패키지 규칙 점검
- 새 클래스 추가 시 먼저 `global`인지 `domain`인지 ownership을 정하고, 그다음 세부 레이어를 결정한다.
- `organization` 외 도메인에서 feature 하위 패키지를 만들 때는 별도 근거를 남긴다.
- `global`에는 공통 기술 요소만 두고, 비즈니스 로직은 각 `domain/*`으로 되돌린다.

### 7.2 의존 방향 리뷰
- `global/* -> domain/*` 금지
- `domain/auth -> domain/organization, global/*`
- `domain/worklog -> domain/organization, domain/tag, global/*`
- `domain/file -> domain/worklog, domain/organization, global/*`
- `domain/notification -> domain/organization, global/*`
- `domain/dashboard -> domain/organization, domain/worklog, domain/notification, global/*`

검토 포인트:
- 역방향 의존이 생기지 않는가
- `domain/dashboard`가 write 책임을 가져가지 않는가
- `domain/auth`와 `global/security`의 ownership이 섞이지 않는가
- internal callback이 public controller와 클래스/Docs/DTO 수준에서 명확히 구분되는가
- internal controller/service/DTO가 모두 `Internal*` prefix naming 규칙을 따르는가

### 8.3 문서화 권장 항목
- 이 도메인이 제공하는 공개 controller/service
- 의존 가능한 다른 도메인
- internal controller/service/DTO naming 및 `Internal*` prefix 적용 여부
- 공개 controller/service와 internal callback entrypoint가 문서상 구분되는지 여부
- 외부 연동(`external`) 또는 read model(`readmodel`) 사용 여부
- 이번 정리가 문서 canonicalization인지, 실제 코드 이동인지 범위 분리가 되어 있는지


## 8. 최종 추천

AX-WMS `/api` 서버의 구조 기준은 다음과 같다.

1. 루트는 `global`, `domain` 두 canonical root로만 나눈다.
2. `global`은 응답/예외/config/security/jooq 같은 **횡단 기술 인프라**를 담당한다.
3. 기본 구조는 `domain/<domain>/<layer>`를 따르는 **도메인 → 레이어**다.
4. `organization`만 `domain/organization/<feature>/<layer>` 같은 **도메인 → 기능 → 레이어** 예외를 허용한다.
5. `domain/auth`는 인증/인가 유스케이스를, `global/security`는 보안 기술 인프라를 담당한다.
6. internal callback은 레이어 depth 대신 `Internal*` prefix naming으로 정리한다. controller 는 `Internal*Controller`, DTO 는 `Internal*Request/Response`, service 는 `Internal*Service` 기준을 사용한다.
7. `domain/dashboard`는 조회 전용 도메인으로 두고 `readmodel`, `repository/jooq`를 활용하는 query-only 계층으로 유지한다.
8. 이번 변경은 문서 canonicalization이며 실제 코드 패키지 이동은 후속 작업으로 분리한다.

이 구조는 AX-WMS의 현재 복잡도, 초기 구현 속도, 이후 확장성을 함께 고려한 canonical 문서 기준이다.


## 10. 참고 자료

- Spring Boot — Structuring Your Code  
  https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html
