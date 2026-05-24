---
title: "Docker BuildKit registry cache 적용 기록"
tags: ["infra", "gitlab-ci", "docker", "buildkit", "ghcr", "cicd", "performance"]
created: 2026-05-24T17:09:35.882Z
updated: 2026-05-24T17:09:35.882Z
sources: []
links: ["gitlab-ci-cd-시간-단축-권장-순서.md"]
category: decision
confidence: medium
schemaVersion: 1
---

# Docker BuildKit registry cache 적용 기록

## 한 줄 요약

GitLab CI의 `api_image`, `web_image`, `ai_image` job을 `docker build` + 개별 `docker push` 방식에서 `docker buildx build --push` 방식으로 바꾸고, GHCR에 image별 `:buildcache` tag를 저장하도록 했다. 목적은 DinD job마다 사라지는 local Docker layer cache 한계를 registry cache로 보완하는 것이다.

관련 문서:

- [[gitlab-ci-cd-시간-단축-권장-순서]]
- `docs/infra/adr.yaml`의 `ADR-021`
- `docs/infra/code-convention.yaml`의 `OPS-022`
- `docs/infra/ec2-gitlab-cicd-guide.md`

## 왜 했는가

기존 image job은 매번 새 DinD 환경에서 실행되므로 Docker daemon layer cache를 안정적으로 재사용하기 어렵다. 그래서 같은 Dockerfile과 lockfile을 쓰는 파이프라인에서도 의존성 설치 layer를 반복 계산할 수 있었다.

특히 효과가 큰 영역은 다음과 같다.

| 영역 | 이유 | 기대 효과 |
|---|---|---|
| `web_image` | `web/Dockerfile`이 multi-stage이고 `pnpm install --frozen-lockfile`, `pnpm --filter ./web build` 비용이 큼 | warm cache 이후 dependency/build 전단 layer 재사용 |
| `ai_image` | `pip install -r requirements.txt`와 OS package 설치 layer가 반복됨 | requirements가 같을 때 dependency layer 재사용 |
| `api_image` | JAR는 CI에서 이미 만들고 Dockerfile은 복사 중심이라 효과는 작음 | 세 image job의 운영 패턴 통일 |

## `.gitlab-ci.yml`에서 바뀐 핵심

### 1. 공통 buildx template 추가

`.docker_image_buildx` template을 추가해 image job 공통 준비 과정을 한 곳으로 모았다.

핵심 설정:

```yaml
.docker_image_buildx:
  image: docker:27.5.1-cli
  services:
    - name: docker:27.5.1-dind
      command: ["--tls=false"]
  variables:
    DOCKER_HOST: tcp://docker:2375
    DOCKER_TLS_CERTDIR: ""
    DOCKER_BUILDKIT: "1"
  before_script:
    - echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
    - docker buildx create --use --driver docker-container --name "axwms-${CI_JOB_ID}"
    - docker buildx inspect --bootstrap
  after_script:
    - docker buildx rm "axwms-${CI_JOB_ID}" || true
```

`after_script`의 builder 제거는 cleanup 용도다. build나 push 실패를 숨기지 않도록 실제 `docker buildx build --push`에는 실패 무시 옵션을 붙이지 않았다.

### 2. 세 image job이 buildx template을 사용

아래 job들이 모두 `extends: .docker_image_buildx`를 사용한다.

- `api_image`
- `web_image`
- `ai_image`

### 3. build와 push를 한 번의 명령으로 통합

기존에는 image job마다 대략 아래 흐름이었다.

```bash
docker build -t "$IMAGE_BASE:$CI_COMMIT_SHA" -t "$IMAGE_BASE:$CI_COMMIT_REF_SLUG" ...
docker push "$IMAGE_BASE:$CI_COMMIT_SHA"
docker push "$IMAGE_BASE:$CI_COMMIT_REF_SLUG"
```

이제는 아래처럼 `docker buildx build --push` 한 번으로 두 tag push와 cache import/export를 처리한다.

```bash
docker buildx build \
  --push \
  --tag "$IMAGE_BASE:$CI_COMMIT_SHA" \
  --tag "$IMAGE_BASE:$CI_COMMIT_REF_SLUG" \
  --cache-from "type=registry,ref=$IMAGE_BASE:buildcache" \
  --cache-to "type=registry,ref=$IMAGE_BASE:buildcache,mode=max" \
  ...
```

## cache tag는 image별로 분리

| job | cache ref | 이유 |
|---|---|---|
| `api_image` | `$API_IMAGE_BASE:buildcache` | api Dockerfile layer 전용 |
| `web_image` | `$WEB_IMAGE_BASE:buildcache` | web multi-stage layer 전용 |
| `ai_image` | `$AI_IMAGE_BASE:buildcache` | ai dependency layer 전용 |

서로 다른 image가 하나의 cache manifest를 덮어쓰지 않도록 image별로 cache ref를 분리했다.

## 유지한 정책

이번 작업은 image build 방식만 바꾸고 운영 tag/deploy 정책은 유지했다.

- image tag는 계속 `:$CI_COMMIT_SHA`, `:$CI_COMMIT_REF_SLUG` 두 개를 사용한다.
- `deploy_dev`, `deploy_prod`는 계속 ref-slug tag 이미지를 pull한다.
- `api_image`, `web_image`, `ai_image`, deploy job의 `interruptible: false` 정책은 유지한다.
- `api_image`는 계속 `api_bootJar` artifact와 `api_test` 성공을 모두 기다린다.
- 사용자가 이번에 제외하겠다고 한 3번 과제, 즉 `changes` 세분화는 건드리지 않았다.

## 같이 갱신한 문서

| 파일 | 내용 |
|---|---|
| `docs/infra/adr.yaml` | `ADR-021`로 BuildKit registry cache 도입 결정, 리스크, 재검토 트리거 기록 |
| `docs/infra/code-convention.yaml` | `OPS-022`로 image job buildx/cache 운영 규칙 추가 |
| `docs/infra/ec2-gitlab-cicd-guide.md` | api/web/ai 자동배포 기준, GHCR cache tag, warm cache 확인 방법 반영 |
| `omx_wiki/gitlab-ci-cd-시간-단축-권장-순서.md` | 4번 항목을 적용 완료로 표시하고 적용 결과 요약 추가 |

## 확인 방법

### 로컬에서 이미 확인한 것

- `.gitlab-ci.yml`, `docs/infra/adr.yaml`, `docs/infra/code-convention.yaml` YAML parse 통과
- `.docker_image_buildx` 존재와 `DOCKER_BUILDKIT="1"` 확인
- `api_image`, `web_image`, `ai_image`가 `docker buildx build`, `--push`, `--cache-from`, `--cache-to`, `mode=max`를 사용하는지 확인
- 기존 SHA/ref-slug tag 정책과 deploy ref-slug image injection 유지 확인
- `git diff --check` 통과
- secret 패턴 스캔 통과

### GitLab에서 확인할 것

실제 시간 단축은 원격 GitLab Runner와 GHCR에서 확인해야 한다.

1. 같은 Dockerfile/lockfile/requirements 입력으로 첫 pipeline 실행
2. 첫 pipeline에서 `exporting cache to registry` 계열 로그 확인
3. 같은 입력으로 두 번째 pipeline 실행
4. 두 번째 pipeline에서 `importing cache manifest` 계열 로그 확인
5. `web_image`, `ai_image` duration이 줄었는지 비교

첫 pipeline은 cold cache라 더 빠르지 않을 수 있다. 판단 기준은 두 번째 warm cache pipeline이다.

## 주의할 점

- `:buildcache` tag는 GHCR 저장소 사용량을 늘릴 수 있다. 커지면 `mode=min` 전환이나 retention cleanup을 별도 검토한다.
- 동시 pipeline이 같은 image의 `:buildcache`를 마지막 writer 기준으로 갱신할 수 있다. branch별 cache 격리가 필요해지면 `:buildcache-$CI_COMMIT_REF_SLUG` 같은 구조를 검토한다.
- Dockerfile에 secret build arg나 secret 파일 copy를 넣으면 cache image를 통한 유출 경로가 생길 수 있다. 현재 정책은 secret을 Docker build 입력에 넣지 않고 EC2 `.env` 또는 GitLab protected variable로만 주입하는 것이다.

## 다음 사람이 보면 좋은 순서

1. `omx_wiki/gitlab-ci-cd-시간-단축-권장-순서.md`의 4번 항목으로 전체 맥락 확인
2. `docs/infra/adr.yaml`의 `ADR-021`로 왜 이 결정을 했는지 확인
3. `.gitlab-ci.yml`의 `.docker_image_buildx`, `api_image`, `web_image`, `ai_image` 확인
4. GitLab pipeline에서 cold/warm cache 로그와 duration 비교

