---
title: "CI/CD DAG needs 적용 변경사항과 장단점"
tags: ["infra", "gitlab-ci", "cicd", "dag", "needs", "parallelization", "deployment"]
created: 2026-05-24T16:11:41.811Z
updated: 2026-05-24T16:11:41.811Z
sources: []
links: ["gitlab-ci-cd-시간-단축-권장-순서.md", "api-ci-병렬화-api-test와-api-bootjar-분리-이유.md"]
category: decision
confidence: medium
schemaVersion: 1
---

# CI/CD DAG needs 적용 변경사항과 장단점

# CI/CD DAG needs 적용 변경사항과 장단점

## 요약

이번 변경은 `.gitlab-ci.yml`에 GitLab DAG `needs`를 추가해 기존 `validate → test → build → image → deploy` stage 구조는 유지하면서도, 필요한 job이 끝나는 즉시 다음 job이 시작될 수 있게 만든 것이다.

핵심 방향은 **전체 stageless 재작성**이 아니라 **hybrid staged + DAG**다. 즉, 사람이 읽기 쉬운 stage는 그대로 두고 병목이 되는 job 간 의존성만 명시했다.

관련 문서:

- [[gitlab-ci-cd-시간-단축-권장-순서]]
- [[api-ci-병렬화-api-test와-api-bootjar-분리-이유]]

## 바뀐 내용

### 1. `api_bootJar` 조기 시작

```yaml
api_bootJar:
  stage: build
  needs: []
```

`api_bootJar`에 `needs: []`를 추가했다. 이 job은 이제 `test` stage 전체가 끝날 때까지 기다리지 않고 pipeline 생성 직후 실행 가능하다.

의도:

- API 테스트와 JAR 패키징을 분리해서 동시에 돌릴 수 있게 한다.
- `api_test`, `web_ci`, `web_e2e`, `ai_ci`가 오래 걸려도 JAR 생성이 불필요하게 대기하지 않게 한다.

### 2. `api_image`가 JAR와 테스트 성공을 모두 기다림

```yaml
api_image:
  needs:
    - job: api_bootJar
      artifacts: true
    - job: api_test
      artifacts: false
```

`api_image`는 두 조건을 기다린다.

- `api_bootJar artifacts: true`: Docker image build에 필요한 JAR를 받는다.
- `api_test artifacts: false`: 테스트 산출물은 필요 없지만 테스트 성공을 image push의 품질 게이트로 유지한다.

의도:

- JAR 생성은 빨리 시작한다.
- 하지만 테스트 실패 코드는 image로 push하지 않는다.

### 3. `web_image`가 web 검증 job을 기다림

```yaml
web_image:
  needs:
    - job: web_ci
      optional: true
      artifacts: false
    - job: web_e2e
      optional: true
      artifacts: false
```

`web_image`가 `web_ci`, `web_e2e`를 기다리도록 했다. 다만 `rules:changes`에 따라 job이 생성되지 않는 pipeline이 있을 수 있으므로 `optional: true`를 사용했다.

의도:

- web image build가 web 검증보다 먼저 진행되지 않게 한다.
- 다른 영역의 stage 완료를 불필요하게 기다리는 시간을 줄인다.

### 4. `ai_image`가 AI 검증 job을 기다림

```yaml
ai_image:
  needs:
    - job: ai_ci
      optional: true
      artifacts: false
```

`ai_image`가 `ai_ci`를 기다리도록 했다. AI도 `rules:changes` 차이로 job 생성 여부가 달라질 수 있어 `optional: true`를 사용했다.

### 5. `deploy_dev`, `deploy_prod`가 생성된 image job을 기다림

```yaml
deploy_dev:
  needs:
    - job: infra_validate
      optional: true
      artifacts: false
    - job: api_image
      optional: true
      artifacts: false
    - job: web_image
      optional: true
      artifacts: false
    - job: ai_image
      optional: true
      artifacts: false
```

`deploy_prod`도 같은 구조다.

의도:

- 배포 job이 생성된 image job보다 먼저 실행되는 것을 막는다.
- 동시에, 특정 영역 job이 `rules`로 생성되지 않은 pipeline에서도 pipeline 생성 실패가 나지 않게 한다.

### 6. 유지한 정책

이번 변경에서 일부러 바꾸지 않은 것들이다.

- stage 목록: `validate → test → build → image → deploy` 유지
- `api_image`, `web_image`, `ai_image`, `deploy_dev`, `deploy_prod`의 `interruptible: false` 유지
- GHCR tag 정책 유지
- Runner `privileged = true` 전제 유지
- Dockerfile, compose, deploy script 변경 없음
- `cache.key.files` 최대 2개 규칙 유지

## 얻을 수 있는 장점

### 1. stage 장벽으로 인한 대기 시간 감소

기존 stage 기반 pipeline은 다음 stage job이 이전 stage의 모든 job 완료를 기다린다. 예를 들어 `api_bootJar`가 web/ai 테스트와 직접 관련이 없어도 test stage 전체 완료를 기다릴 수 있다.

`needs`를 추가하면 실제로 필요한 job만 끝나면 다음 job이 시작될 수 있다. Runner 슬롯이 충분할수록 wall-clock 단축 효과가 커진다.

### 2. 서비스별 경로가 더 독립적으로 움직임

API, Web, AI가 모노레포 안에 있어도 실제 검증/이미지 빌드 경로는 상당 부분 독립적이다. 이번 변경으로 각 서비스 image job은 자기 영역의 검증 job을 중심으로 움직인다.

### 3. 품질 게이트를 유지하면서 속도를 높임

`api_image`는 `api_test`를 계속 기다린다. 즉, 빠르게 만들기 위해 테스트 게이트를 제거하지 않았다.

속도 개선과 안정성 사이에서 다음 균형을 선택한 셈이다.

```text
가능한 작업은 먼저 시작한다.
하지만 image push와 deploy는 필요한 검증을 통과한 뒤에만 진행한다.
```

### 4. 변경량이 작고 되돌리기 쉽다

전체 pipeline을 stageless로 갈아엎지 않고 `needs`만 추가했다. 문제가 생기면 변경 지점이 명확하고 rollback도 쉽다.

### 5. GitLab pipeline graph에서 의존성이 명확해짐

각 image/deploy job이 무엇을 기다리는지 YAML에 드러난다. 이후 병목 분석 시 pipeline graph를 보고 어떤 job이 왜 대기했는지 파악하기 쉬워진다.

## 단점과 주의점

### 1. Runner 자원 경합이 커질 수 있음

`needs`는 job을 더 빨리 시작하게 만든다. Runner 슬롯이 늘어났거나 동시에 실행되는 job이 많으면 EC2의 CPU/RAM/Disk I/O, Docker daemon, DinD service 부하가 커질 수 있다.

AX-WMS는 Runner가 배포 대상 EC2에 공존하는 구조이므로, 실제 적용 후에는 pipeline 시간뿐 아니라 EC2 자원 사용량도 같이 봐야 한다.

### 2. `optional: true`가 많아지면 의존성 해석이 복잡해짐

`rules:changes`로 job 생성 여부가 달라지는 구조에서는 `optional: true`가 필요하다. 하지만 optional needs가 많아지면 “이 pipeline에서 실제로 어떤 job이 생성됐고 deploy가 무엇을 기다렸는지”를 graph로 확인해야 한다.

따라서 MR 전/후 GitLab CI Lint와 pipeline graph 확인이 중요하다.

### 3. 원격 GitLab CI Lint 없이는 최종 확정이 아님

로컬 YAML 파싱과 구조 검증은 통과했지만, GitLab의 실제 rules 평가, protected variable, branch/MR 조건 조합은 GitLab CI Lint 또는 실제 pipeline에서 확인해야 한다.

특히 확인할 케이스:

- `refactor/infra/cicd` push pipeline
- `dev` push pipeline
- `master` push pipeline
- MR to `dev`
- MR to `master`
- docs-only 변경 pipeline

### 4. 병렬화가 항상 시간 단축을 보장하지는 않음

Runner 슬롯이 부족하거나 DinD/Gradle/pnpm/pip cache hit가 낮으면 DAG를 적용해도 체감 단축이 작을 수 있다. 반대로 너무 많은 job이 동시에 돌면 경합 때문에 개별 job 시간이 늘어날 수 있다.

그래서 이번 변경은 “무조건 빠르게 만든다”라기보다 “불필요한 stage 대기를 제거해 빠르게 실행될 수 있는 구조를 만든다”에 가깝다.

### 5. 이후 `changes` 세분화가 필요함

이번 변경은 job 의존성만 다뤘다. 아직 `infra/**`, `docs/infra/**`, `.gitlab-ci.yml` 변경이 여러 deploy/image 경로를 넓게 깨울 수 있다.

다음 단계에서는 다음을 검토한다.

- docs-only 변경은 validate 중심으로 제한
- infra runtime 변경과 infra docs 변경 분리
- api/web/ai 코드 변경 시 해당 영역 image만 실행

## 검증 기록

로컬에서 확인한 항목:

```bash
ruby -e 'require "yaml"; YAML.load_file(".gitlab-ci.yml"); puts "yaml ok"'
# => yaml ok
```

구조 검증 결과:

- `api_bootJar.needs == []`
- `api_image`가 `api_bootJar artifacts:true`, `api_test artifacts:false`를 가진다.
- `web_image`가 `web_ci`, `web_e2e`를 optional needs로 가진다.
- `ai_image`가 `ai_ci`를 optional needs로 가진다.
- `deploy_dev`, `deploy_prod`가 `infra_validate`, `api_image`, `web_image`, `ai_image`를 optional needs로 가진다.
- image/deploy job의 `interruptible:false` 유지.
- `cache.key.files` 최대 2개 유지.

추가 검증:

```bash
git diff --check
# => 통과
```

아키텍트 검토 결과:

- `APPROVED`
- 필수 수정 사항 없음

## 다음에 볼 것

1. GitLab CI Lint로 rules/needs 조합 확인
2. 실제 pipeline graph에서 조기 시작 여부 확인
3. pipeline 전체 시간과 job별 pending/running time 비교
4. Runner EC2 자원 사용량 확인
5. 이후 `changes` 세분화와 web build 중복 제거 검토
