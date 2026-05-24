---
title: "API CI 병렬화: api_test와 api_bootJar 분리 이유"
tags: ["infra", "gitlab-ci", "api", "runner", "parallelization", "cicd"]
created: 2026-05-24T15:31:10.869Z
updated: 2026-05-24T15:31:10.869Z
sources: []
links: ["gitlab-ci-cd-시간-단축-권장-순서.md"]
category: decision
confidence: medium
schemaVersion: 1
---

# API CI 병렬화: api_test와 api_bootJar 분리 이유

# API CI 병렬화: `api_test`와 `api_bootJar` 분리 이유

## 요약

기존 API CI는 `.gitlab-ci.yml`의 단일 `api_ci` job에서 다음 명령을 한 번에 실행했다.

```bash
cd api
./gradlew --no-daemon clean test bootJar
```

이번 변경은 이 단일 job을 다음 두 job으로 나눈다.

| job | stage | 역할 | 명령 |
|---|---|---|---|
| `api_test` | `test` | API 테스트와 테스트 리포트 수집 | `./gradlew --no-daemon clean test` |
| `api_bootJar` | `build` | Docker image에 넣을 JAR 생성 | `./gradlew --no-daemon clean bootJar -x test` |

공통 DinD/JDK/Gradle 설정은 `.api_gradle_dind` hidden template으로 묶고, `api_image`는 `api_bootJar` artifact와 `api_test` 성공을 모두 기다리도록 바꾼다.

## 변경점

### 1. `api_ci` 단일 job 제거

기존 `api_ci`는 `test`와 `bootJar`를 같은 Gradle invocation 안에서 순차 실행했다. 이 구조에서는 Runner 슬롯이 여러 개 있어도 API 테스트와 JAR 생성이 같은 job 안에서 직렬로 묶인다.

### 2. `.api_gradle_dind` hidden template 추가

API Gradle job은 jOOQ codegen과 Testcontainers 때문에 Docker daemon이 필요하다. 그래서 기존 `api_ci`가 갖고 있던 다음 설정을 template으로 공통화했다.

- `docker:27.5.1-cli` image
- `docker:27.5.1-dind` service
- `DOCKER_HOST=tcp://docker:2375`
- `DOCKER_TLS_CERTDIR=""`
- `GRADLE_USER_HOME=$CI_PROJECT_DIR/api/.gradle`
- `openjdk21-jdk` 설치와 `JAVA_HOME` 설정
- Gradle cache: `api/gradle/libs.versions.toml`, `api/build.gradle`

`cache.key.files`는 GitLab 제약과 `docs/infra/code-convention.yaml` 규칙에 맞춰 2개를 유지한다.

### 3. `api_test` 추가

`api_test`는 테스트 전용 job이다.

```yaml
api_test:
  extends: .api_gradle_dind
  stage: test
  script:
    - cd api
    - ./gradlew --no-daemon clean test
```

실패 시에도 원인을 확인할 수 있도록 test result와 HTML report를 항상 artifact로 남긴다.

```yaml
artifacts:
  when: always
  reports:
    junit: api/build/test-results/test/TEST-*.xml
  paths:
    - api/build/test-results/test
    - api/build/reports/tests/test
```

### 4. `api_bootJar` 추가

`api_bootJar`는 JAR 생성 전용 job이다.

```yaml
api_bootJar:
  extends: .api_gradle_dind
  stage: build
  needs: []
  script:
    - cd api
    - ./gradlew --no-daemon clean bootJar -x test
```

핵심은 `needs: []`다. 이 설정으로 `api_bootJar`는 무관한 `test` stage job 전체가 끝나기를 기다리지 않고 조기 시작할 수 있다. Runner 슬롯이 충분하면 `api_test`, `web_ci`, `web_e2e`, `ai_ci`와 겹쳐 실행될 수 있다.

JAR는 기존처럼 `api/build/libs/*.jar` artifact로 남긴다.

### 5. `api_image.needs` 변경

기존에는 `api_image`가 `api_ci` artifact만 기다렸다. 변경 후에는 다음 두 조건을 모두 기다린다.

```yaml
needs:
  - job: api_bootJar
    artifacts: true
  - job: api_test
    artifacts: false
```

의미는 다음과 같다.

- `api_bootJar artifacts: true`: Docker image build에 필요한 JAR를 내려받는다.
- `api_test artifacts: false`: 테스트 artifact는 image build에 필요 없지만, 테스트 성공은 품질 게이트로 유지한다.

즉 `api_image`가 테스트 실패 코드를 이미지로 push하지 않도록 막으면서도, JAR 생성은 테스트와 병렬로 당겨 실행한다.

## 이렇게 한 이유

### 이유 1. API 내부 직렬 병목 제거

단일 `api_ci`에서 `clean test bootJar`를 실행하면 테스트가 끝난 뒤에야 JAR 생성이 이어진다. CI stage 병렬성이 있어도 job 내부 순서는 바꿀 수 없다.

`api_test`와 `api_bootJar`를 분리하면 GitLab Runner가 두 job을 독립 작업으로 스케줄링할 수 있다. Runner 슬롯이 2개 이상이면 wall-clock 시간이 줄어들 가능성이 생긴다.

### 이유 2. 품질 게이트는 유지

단순히 `bootJar`만 먼저 만들고 image를 push하면 테스트 실패 코드가 image로 올라갈 수 있다. 그래서 `api_image.needs`에는 반드시 `api_test`를 포함한다.

이 구조의 의도는 다음 균형이다.

```text
JAR 생성은 빨리 시작한다.
하지만 image push는 테스트 성공 후에만 허용한다.
```

### 이유 3. JAR artifact 흐름 유지

`api_image`는 Docker build context에서 API JAR를 필요로 한다. 따라서 `api_bootJar`는 기존 `api_ci`가 제공하던 `api/build/libs/*.jar` artifact 계약을 그대로 이어받는다.

### 이유 4. Testcontainers/jOOQ 리스크를 Phase 1에서 키우지 않음

API 테스트를 unit/integration/e2e로 더 쪼개면 병렬성은 더 좋아질 수 있다. 하지만 현재 API는 jOOQ codegen과 Testcontainers PostgreSQL/Redis 경로가 Docker daemon에 의존한다. 여러 테스트 job을 동시에 늘리면 Docker daemon, CPU, RAM 경합이 커질 수 있다.

그래서 Phase 1에서는 테스트 job을 하나로 유지하고, 테스트와 패키징만 분리했다. 이 방식은 변경량이 작고 품질 게이트도 명확하다.

### 이유 5. ADR-017의 image/deploy 보호 정책 유지

`api_image`의 `interruptible: false`는 유지한다. image tag push가 중간 취소되면 `CI_COMMIT_SHA`와 `CI_COMMIT_REF_SLUG` tag 상태가 어긋날 수 있기 때문이다. 이 정책은 `docs/infra/code-convention.yaml`의 image/deploy auto-cancel 방지 규칙과 연결된다.

## 기대되는 CI 그래프

```text
validate: infra_validate

test:    api_test        web_ci / web_e2e / ai_ci와 병렬 가능
build:   api_bootJar     needs: []로 조기 시작 가능
image:   api_image       api_test 성공 + api_bootJar artifact 이후 실행
deploy:  deploy_dev/prod 기존 정책 유지
```

## 검증한 내용

로컬에서 다음을 확인했다.

```bash
ruby -e 'require "yaml"; YAML.load_file(".gitlab-ci.yml"); puts "yaml ok"'
python3 구조 검증 스크립트
git diff --check
cd api && ./gradlew --no-daemon clean test
cd api && ./gradlew --no-daemon clean bootJar -x test
```

검증 결과:

- YAML parse 통과
- `api_test`, `api_bootJar`, `api_image.needs` 구조 확인
- `git diff --check` 통과
- `clean test` 성공
- `clean bootJar -x test` 성공

## 아직 원격 CI에서 확인해야 할 것

다음은 GitLab Runner에서 실제 pipeline을 돌려야 확인할 수 있다.

- GitLab CI Lint의 최종 렌더링 결과
- `api_test`와 `api_bootJar`가 실제로 동시에 pending/running 되는지
- `api_image`가 `api_bootJar` artifact를 정상 다운로드하는지
- 기존 `api_ci` 대비 API 구간 wall-clock이 줄었는지
- 줄지 않았다면 Runner 슬롯 부족인지, Docker daemon/CPU/RAM 경합인지

## 다음 개선 후보

Phase 1 이후에도 API 테스트가 병목이면 다음을 별도 검토한다.

1. `unitTest`, `integrationTest`, `e2eTest` Gradle task 분리
2. `AxWmsApplicationTests`를 integration lane에 배치
3. Testcontainers 병렬 실행 시 Docker daemon 자원 경합 확인
4. jOOQ generated source/cache 전략 재검토

관련 참고: [[gitlab-ci-cd-시간-단축-권장-순서]]

