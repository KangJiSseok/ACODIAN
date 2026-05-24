---
title: "GitLab CI/CD 시간 단축 권장 순서"
tags: ["infra", "gitlab-ci", "runner", "cicd", "performance"]
created: 2026-05-23T14:40:57.727Z
updated: 2026-05-23T14:40:57.727Z
sources: []
links: ["runners.md", "api-ci-병렬화-api-test와-api-bootjar-분리-이유.md", "ci-cd-dag-needs-적용-변경사항과-장단점.md"]
category: reference
confidence: medium
schemaVersion: 1
---

# GitLab CI/CD 시간 단축 권장 순서

# GitLab CI/CD 시간 단축 권장 순서

AX-WMS 현재 CI/CD는 GitLab Runner(docker executor + DinD)를 사용하고, `.gitlab-ci.yml` 기준 `validate → test → build → image → deploy` 순서로 동작한다. 현재 약 20분 소요되는 병목을 줄일 때는 아래 순서로 진행하는 것이 좋다.

실제 적용 결정 기록:

- [[api-ci-병렬화-api-test와-api-bootjar-분리-이유]]
- [[ci-cd-dag-needs-적용-변경사항과-장단점]]

## 1. Runner 병렬 수 확인 및 조정

가장 먼저 EC2의 GitLab Runner 설정을 확인한다. `.gitlab-ci.yml`에서 같은 stage job은 병렬 실행 가능하지만, Runner 설정이 `concurrent = 1` 또는 runner별 `limit = 1`이면 실제로는 직렬에 가깝게 돈다.

확인 대상:

```bash
sudo cat /etc/gitlab-runner/config.toml
```

우선 확인할 값:

```toml
concurrent = 1
[[runners]]
  limit = 1
  request_concurrency = 1
```

권장 시작점은 EC2 자원 여유가 있을 때 `concurrent = 3~4`, runner `limit = 3~4`, `request_concurrency = 2` 정도다. 단, ADR-006에 따라 Runner가 배포 대상 EC2에 공존하므로 CPU/RAM 사용량을 보면서 조정한다. 장기적으로는 전용 Runner VM 분리가 가장 안전하다.

## 2. `.gitlab-ci.yml`에 DAG `needs` 적용

Runner 병렬 수만 늘려도 현재 stage 장벽 때문에 완전한 병렬화가 되지 않는다. 현재 구조에서는 `web_image`, `ai_image`가 독립 빌드여도 `api_ci`가 있는 build stage 전체가 끝날 때까지 기다릴 수 있다.

따라서 GitLab CI `needs`를 사용해 필요한 job 완료 직후 다음 job이 실행되도록 바꾼다. 예시 방향:

```yaml
api_ci:
  stage: build
  needs: []

api_image:
  stage: image
  needs:
    - job: api_ci
      artifacts: true

web_image:
  stage: image
  needs:
    - job: web_ci
      optional: true

ai_image:
  stage: image
  needs:
    - job: ai_ci
      optional: true

deploy_dev:
  stage: deploy
  needs:
    - job: api_image
      optional: true
    - job: web_image
      optional: true
    - job: ai_image
      optional: true
```

`rules:changes` 때문에 특정 job이 생성되지 않을 수 있으므로 `optional: true`를 함께 고려한다.

## 3. `changes` 세분화로 불필요한 전체 빌드 제거

현재 `.api_deploy_changes`, `.web_deploy_changes`, `.ai_deploy_changes`가 `infra/**`, `docs/infra/**`, `.gitlab-ci.yml`을 포함한다. 이 때문에 인프라 문서만 바뀌어도 api/web/ai 이미지 빌드가 모두 돌 수 있다.

권장 분리:

```yaml
.api_code_changes:
  - api/**/*

.web_code_changes:
  - web/**/*
  - package.json
  - pnpm-lock.yaml
  - pnpm-workspace.yaml

.ai_code_changes:
  - ai/**/*

.infra_runtime_changes:
  - infra/**/*
  - .gitlab-ci.yml

.infra_docs_changes:
  - docs/infra/**/*
```

운영 정책:

- `docs/infra/**`만 변경: 이미지 빌드/배포를 돌리지 않는다. 필요하면 validate만 수행한다.
- `infra/**` 변경: compose/nginx/runtime 변경이면 deploy만 수행하고, 이미지 재빌드는 최소화한다.
- `api/**`, `web/**`, `ai/**` 변경: 해당 영역의 CI/image만 수행한다.

이 방향은 `docs/infra/pending-decisions.md`의 “파이프라인 changes 세분화” 항목과도 일치한다.

## 4. Docker BuildKit registry cache 도입

현재 `api_image`, `web_image`, `ai_image`는 `docker build`에 layer cache가 거의 없다. 특히 `web/Dockerfile`은 multi-stage이고 `ai/Dockerfile`은 `pip install -r requirements.txt`가 있어 registry cache 효과가 크다.

예시 방향:

```bash
docker buildx create --use --driver docker-container
docker buildx build \
  --push \
  -t "$WEB_IMAGE_BASE:$CI_COMMIT_SHA" \
  -t "$WEB_IMAGE_BASE:$CI_COMMIT_REF_SLUG" \
  --cache-from type=registry,ref="$WEB_IMAGE_BASE:buildcache" \
  --cache-to type=registry,ref="$WEB_IMAGE_BASE:buildcache",mode=max \
  -f web/Dockerfile .
```

동일 패턴을 `ai_image`, 필요 시 `api_image`에도 적용한다.

## 5. API jOOQ generated source cache 추가

`api/src/generated/jooq/main/`은 `.gitignore`에 포함되어 있고 현재 CI cache에도 없다. clean checkout에서 generated source와 fingerprint가 없으면 `api/gradle/jooq-codegen.gradle`이 Docker로 `pgvector/pgvector:pg17`을 띄우고 Flyway migrate 후 jOOQ codegen을 수행한다.

`api_ci.cache.paths`에 아래를 추가하는 방안을 검토한다.

```yaml
cache:
  key:
    files:
      - api/gradle/libs.versions.toml
      - api/build.gradle
      - api/gradle.properties
      - api/gradle/jooq-codegen.gradle
      - api/src/main/resources/db/migration/*
  paths:
    - api/.gradle
    - api/src/generated/jooq/main
```

목표는 migration/build 설정이 바뀌지 않은 파이프라인에서 jOOQ bootstrap 비용을 줄이는 것이다.

## 6. web build 중복 제거

현재 `web_ci`에서 `pnpm build:web`을 실행하고, `web_image`의 `web/Dockerfile`에서도 `pnpm --filter ./web build`를 실행한다. 배포 브랜치에서는 Next.js build가 중복될 수 있다.

개선 방향:

- MR/작업 브랜치: `web_ci = lint + build` 유지
- `dev`/`master` push: `web_ci = lint only`, 실제 build 검증은 `web_image`에서 담당

이렇게 하면 배포 파이프라인에서 web build 1회를 줄일 수 있다.

## 7. DinD 성능 옵션 확인

Docker-in-Docker job에는 아래 변수를 검토한다.

```yaml
variables:
  DOCKER_DRIVER: overlay2
  DOCKER_BUILDKIT: "1"
```

또 base image pull이 느리면 registry mirror도 검토한다.

## 권장 실행 순서 요약

1. Runner `concurrent`, `limit`, `request_concurrency` 확인 및 3~4 수준 조정
2. `.gitlab-ci.yml`에 DAG `needs` 적용
3. `changes` 세분화로 docs/infra-only 변경의 전체 빌드 방지
4. Docker BuildKit registry cache 도입
5. API jOOQ generated source cache 추가
6. web build 중복 제거
7. DinD `overlay2`/BuildKit/registry mirror 및 전용 Runner VM 검토

## 주의

- Runner는 현재 배포 대상 EC2에 공존한다. 병렬 수를 올리면 운영 컨테이너와 CPU/RAM/Disk I/O를 경쟁할 수 있다.
- `deploy_dev`/`deploy_prod`, image push job은 `interruptible: false`로 유지하는 것이 좋다. 이미지 tag push 또는 배포 중간 취소를 막기 위한 ADR-017/OPS-019 정책과 연결된다.
- infra 문서 변경만으로 운영 배포가 도는 문제는 시간 낭비뿐 아니라 배포 리스크도 키운다. `changes` 세분화가 단순 최적화가 아니라 안정성 개선이기도 하다.
