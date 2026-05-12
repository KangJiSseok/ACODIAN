# API jOOQ Codegen Policy

## 현재 정책
- Flyway migration이 schema source of truth이다.
- `./gradlew jooqGenerate` 는 **실제 PostgreSQL(pgvector/pgvector:pg17)** 컨테이너를 bootstrap 한 뒤 메타데이터를 읽어 jOOQ 코드를 생성한다.
- 로컬 `build`/`bootRun`/`test`/`assemble` 진입점과 CI는 **필요 시 `jooqGenerate` 를 선행 보장하는 bootstrap-first workflow**로 운영한다.
- generated source는 **disposable build artifact**로 취급하며 Git에 커밋하지 않는다.
- 현재 범위에서는 table/record 생성만 유지하고, extension routine/UDT generated code는 생성하지 않는다.

## embedding 테이블 제외 정책
아래 테이블은 **실제 DB에는 유지**하지만, 현재는 **jOOQ 생성 대상에서 제외**한다.

- `tb_worklog_embedding`
- `tb_file_embedding`

### 제외 이유
현재 API/Spring 모듈은 vector 컬럼을 직접 읽거나 쓰지 않는다. vector 활용은 이후 AI internal 경로가 담당할 예정이므로, 이번 단계에서는 embedding 테이블을 jOOQ generated schema에 포함하지 않는다.

### 복귀 조건
다음 중 하나가 발생하면 embedding 테이블 제외 정책을 재검토한다.

- API가 embedding 저장을 직접 담당하게 될 때
- API가 embedding 조회/관리 화면을 제공하게 될 때
- API가 pgvector 기반 유사도 검색을 직접 수행하게 될 때

## canonical generated source 경로
- generated jOOQ canonical path는 **`api/src/generated/jooq/main/java`** 로 유지한다.
- 이 경로는 `src/main/java` 와 분리된 **generated-only 루트**다.
- `jooqGenerate` 실행 시 해당 루트는 전량 삭제 후 재생성될 수 있으므로, 사람이 작성한 소스를 두지 않는다.
- generated source는 Git 추적 대상이 아니며, **직접 수정하지 않고 `./gradlew jooqGenerate` 재실행으로만 갱신**한다.

### 왜 아직 `api/build/generated/...` 로 옮기지 않는가
- 현재 Gradle/sourceSets 구성은 `api/src/generated/jooq/main/java` 를 canonical generated root로 사용한다.
- generated source 비커밋 정책과 경로 이동은 별개의 의사결정이다. 경로 이동은 build 설정 단순화와 함께 별도 작업으로 다룬다.

### 왜 `api/src/main/java/...` 를 쓰지 않는가
- `api/src/main/java/com/ibank/axwms/global/jooq/JooqConfig.java` 처럼 hand-written jOOQ 설정 코드가 이미 존재한다.
- generated source를 `src/main/java/...` 아래에 두면 수기 작성 소스와 generated source의 physical root가 섞여 generated-only 경계가 흐려진다.

## 운영 규칙
- 로컬과 CI 모두 generated source를 **체크아웃 결과물**이 아니라 **재생성 가능한 산출물**로 취급한다.
- `build`/`bootRun`/`test`/`assemble` 와 CI는 generated source가 없거나 stale 하면 compile 이전에 `jooqGenerate` 가 선행되도록 운영한다.
- stale 판정은 migration 파일뿐 아니라 **`api/build.gradle` / `api/gradle/jooq-codegen.gradle` / `api/gradle.properties`** 같은 codegen 입력 변경도 함께 반영한다.
- 로컬에서는 스키마 변경 또는 generated source 검증이 필요할 때 `./gradlew jooqGenerate` 를 명시적으로 실행해 빠르게 피드백을 받을 수 있다.
- `jooqGenerate` 실패는 codegen lane의 실패로 간주하며, Docker 실행 상태/이미지 pull/Flyway migration 로그를 우선 확인한다.

## 로컬 개발 순서

### 스키마 변경이 없는 일반 개발 루프
1. `./gradlew bootRun` 또는 `./gradlew test` 또는 `./gradlew assemble`
2. 진입한 빌드 루프가 generated source 보장을 담당한다.

### Flyway migration을 변경한 경우
1. Flyway migration 수정
2. `./gradlew jooqGenerate` 또는 `./gradlew build`/`test`/`bootRun`/`assemble` 로 codegen 성공 여부 확인
3. `api/src/generated/jooq/main/java` 기준 generated source diff는 **로컬 검토용**으로만 확인
4. `./gradlew bootRun` 또는 `./gradlew test` 또는 `./gradlew assemble`
5. migration + hand-written source/docs 만 커밋

### 왜 generated source를 더 이상 커밋하지 않는가
- 로컬 빌드와 CI가 같은 PostgreSQL bootstrap 기반 codegen lane을 재현하므로, 저장소에 generated source를 넣지 않아도 재현성이 유지된다.
- 팀 전원이 Docker/pgvector bootstrap 전제를 받아들인 상태에서는 generated source 부재가 개발 시작의 blocker가 되지 않는다.
- generated source를 비커밋으로 유지하면 schema 변경 시 PR diff 노이즈를 줄이고, hand-written 변경과 migration 변경에 리뷰를 집중할 수 있다.

## CI 검증 순서
CI는 fresh checkout 기준으로 아래 순서를 기본으로 한다.

1. `./gradlew jooqGenerate` 또는 이를 포함한 표준 build entrypoint 실행
2. `./gradlew test` 또는 프로젝트 표준 검증 태스크 실행
3. codegen/bootstrap/compile/test 중 하나라도 실패하면 fail

이 순서는 generated source를 Git diff로 비교하는 대신, **clean workspace에서 재생성 가능한지**를 최종 검증하기 위한 것이다.

## 현재 저장소에 반영된 운영 전제
- PostgreSQL bootstrap 기반 `jooqGenerate` lane
- embedding 테이블 제외 정책
- generated source canonical path: `api/src/generated/jooq/main/java`
- 로컬 build/CI가 generated source를 선행 보장하는 bootstrap-first workflow
- 팀 차원의 Docker/pgvector bootstrap 전제 수용

## 현 시점 운영 모델

### 확정안
- **실DB 기반 codegen 유지 + embedding 제외 유지 + generated source 비커밋 + bootstrap-first local/CI workflow**

### 왜 이 운영 모델인가
- pgvector/extension이 포함된 실제 PostgreSQL 메타데이터 fidelity를 유지할 수 있다.
- clean checkout 상태에서도 로컬 빌드와 CI가 동일한 방식으로 generated source를 재생성하므로 재현성이 높다.
- migration 변경뿐 아니라 codegen 관련 Gradle 설정 변경도 자동 regenerate 대상에 포함해 drift 누락 가능성을 더 줄인다.
- schema 변경 시 generated source 대량 diff를 커밋하지 않아도 되어 PR review noise가 줄어든다.
- canonical path를 유지하면서도 generated source를 disposable artifact로 다뤄 build 책임과 Git 책임을 분리할 수 있다.

### 현재 범위에서 하지 않는 것
- `pgvector-java`, forcedType, custom Binding 같은 runtime vector binding은 도입하지 않는다.
- embedding 테이블을 jOOQ 생성 대상에 재포함하지 않는다.
- generated source canonical path를 이번 정책 변경과 함께 다른 경로로 이동하지 않는다.
- generated-only 루트에 수기 작성 소스를 두지 않는다.

## 역할 분담
- **schema 변경자**
  - Flyway migration 수정
  - `./gradlew jooqGenerate` 또는 표준 build entrypoint로 codegen 성공 여부 확인
  - 필요 시 `api/src/generated/jooq/main/java` 기준 로컬 diff 검토
  - migration + hand-written source/docs 커밋
- **일반 개발자**
  - schema를 건드리지 않는 작업에서는 기존 `build`/`test`/`bootRun`/`assemble` 루프를 우선 사용
  - generated source가 저장소에 없다는 가정 하에 build lane을 사용한다.
- **CI**
  - fresh checkout에서 `jooqGenerate` 또는 이를 포함한 build entrypoint 실행
  - compile/test 실패를 통해 codegen/bootstrap 문제를 조기 검출
  - embedding excludes 정책 이탈 여부를 후속 검증 단계에서 조기 검출

## 후속 정렬 항목
- build 설정 단순화가 필요해지면 canonical generated path를 `build/generated/...` 계열로 옮길지 검토한다.
- Docker/pgvector bootstrap 전제와 로컬 setup 점검 항목을 온보딩 문서에 더 명확히 연결할지 검토한다.
- codegen bootstrap 시간을 줄이기 위한 캐시/최적화 여지를 검토한다.
