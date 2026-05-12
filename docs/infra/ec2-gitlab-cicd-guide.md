# EC2 + GitLab CI/CD 배포 가이드

- 기준 문서: [`docs/infra/adr.yaml`](adr.yaml), [`docs/infra/code-convention.yaml`](code-convention.yaml)
- 목적: AX-WMS 인프라를 처음 보는 사람도 EC2 + GitLab CI/CD 자동배포를 그대로 재현할 수 있도록 1차 운영 절차를 제공한다.
- 범위: api/web/postgres/redis 자동배포, GitLab Runner 운영, 브랜치별 파이프라인 동작, 서버 `.env` 와 GitLab CI/CD Variables 운영, Nginx 진입 예시.
- 전제: EC2 호스트, SSafy GitLab 사용, ghcr.io 이미지 레지스트리(ADR-007), 외부 도메인/HTTPS 사전 준비.
- 연계 문서: [`runbooks/infra/ci-auth-troubleshoot.md`](../../runbooks/infra/ci-auth-troubleshoot.md) (CI 인증 자격증명 장애 대응), [`docs/api/jooq-codegen-policy.md`](../api/jooq-codegen-policy.md) (`api_ci` 의 DinD 의존 배경).

이 문서는 **AX-WMS 인프라를 처음 보는 사람도 그대로 따라갈 수 있게** 작성한 1차 운영 가이드다.
현재 범위는 **api / web / postgres / redis 자동배포 + nginx conf 저장소 동기화**이며, `ai` 자동배포 편입만 후속 작업으로 남겨둔다(ADR-014/ADR-015, pending-decisions #5 참조).

---

## 1. 이 문서가 다루는 것

현재 운영 가정은 아래와 같다.

- 서버: **EC2**
- 소스 저장소: **GitLab**
- 배포 방식: **GitLab CI/CD가 이미지를 빌드해서 Registry에 push → EC2가 pull 받아 compose로 기동**
- 외부 도메인/HTTPS: **이미 준비됨**
- 현재 자동배포 대상: **api + web + postgres + redis** (ai 는 후속, ADR-014 / pending-decisions #5)

즉, 이 문서는 “EC2에 어떻게 올리고, GitLab을 어떻게 연결하고, 브랜치별로 언제 배포되는가”를 설명한다.

---

## 2. 브랜치 전략

### 2-1. 우리가 실제로 해석하는 브랜치 규칙

이 프로젝트에서 중요한 기준은 `feat` 접두사 자체가 아니다.
핵심은 아래 두 그룹이다.

1. **작업 브랜치**
   - `dev`, `master`가 아닌 모든 브랜치
   - 예: `feat/...`, `fix/...`, `chore/...`, `refactor/...`, `docs/...`

2. **배포 브랜치**
   - `dev`
   - `master`

### 2-2. 이벤트별 동작

#### A. 작업 브랜치 push
- CI만 수행
- 배포 없음

#### B. `dev`, `master` 대상 Merge Request 생성/업데이트
- CI만 수행
- 배포 없음

#### C. MR merge 후 `dev` push
- CI + CD 수행
- staging 환경 반영

#### D. MR merge 후 `master` push
- CI + CD 수행
- production 환경 반영

---

## 3. 전체 배포 흐름 한눈에 보기

```text
개발자 작업 브랜치 push
  -> GitLab CI (테스트/빌드만)

작업 브랜치 -> dev/master MR 생성
  -> GitLab MR Pipeline (테스트/빌드만)

MR merge -> dev push
  -> GitLab CI
  -> api / web Docker image build (변경 영역에 해당하는 것만)
  -> ghcr.io push
  -> SSH로 EC2 staging 접속
  -> docker compose pull api web / up -d

MR merge -> master push
  -> GitLab CI
  -> api / web Docker image build (변경 영역에 해당하는 것만)
  -> ghcr.io push
  -> SSH로 EC2 production 접속
  -> docker compose pull api web / up -d
```

---

## 4. 저장소 안에서 중요한 파일

| 파일 | 역할 |
|---|---|
| `.gitlab-ci.yml` | GitLab 파이프라인 규칙과 job 정의 |
| `api/Dockerfile` | api 이미지를 만드는 Dockerfile (api 단일 컨텍스트) |
| `web/Dockerfile` | web 이미지를 만드는 Dockerfile (모노레포 루트 컨텍스트, multi-stage, ADR-014) |
| `.dockerignore` (루트) | web 빌드 시 모노레포 루트 컨텍스트에서 제외할 파일 (DO-004) |
| `infra/compose.deploy.yml` | EC2에서 실제로 사용하는 compose 파일 |
| `infra/.env.example` | compose 렌더용 기본값 파일 (ADR-009) |
| `infra/scripts/remote-deploy.sh` | 서버에서 api/web 컨테이너를 멱등 배포하는 스크립트 (OPS-016) |
| `infra/nginx/sites-available/axwms.conf` | EC2 systemd nginx 의 단일 진입점 conf SSOT (ADR-015 / OPS-017) |
| `infra/nginx/snippets/proxy-headers.conf` | 모든 location 의 공통 proxy header 묶음 |
| `docs/infra/adr.yaml` | 인프라 의사결정 기록 |
| `docs/infra/code-convention.yaml` | 인프라 문서/설정 변경 시 지켜야 할 규칙 |

---

## 5. 왜 EC2에서 직접 빌드하지 않는가

초심자가 가장 많이 헷갈리는 부분이라 먼저 정리한다.

### 직접 빌드 방식
EC2에서:
- git pull
- gradle build
- docker build
- docker compose up

이렇게 할 수도 있다. 하지만 이 방식은 아래 문제가 있다.

- 서버에 JDK/Gradle/Docker 빌드 의존성을 계속 유지해야 한다.
- 서버 상태에 따라 빌드 결과가 흔들릴 수 있다.
- 어떤 커밋이 어떤 이미지로 올라갔는지 추적이 어렵다.

### 현재 채택한 방식
GitLab CI에서:
- 테스트
- JAR 빌드
- Docker image build
- Registry push

EC2에서는:
- Registry image pull
- compose up -d

이렇게 나누면 서버는 **실행 전용 호스트** 역할만 하면 된다.

---

## 6. EC2 서버 구조

권장 디렉터리 구조는 아래와 같다.

```text
/opt/axwms/
├── staging/
│   ├── .env
│   ├── compose.deploy.yml
│   └── remote-deploy.sh
└── production/
    ├── .env
    ├── compose.deploy.yml
    └── remote-deploy.sh
```

### 왜 staging/production을 나누는가?

`dev`와 `master`가 같은 EC2 를 써도,
서로 다른 포트 / 서로 다른 volume / 서로 다른 compose project 로 분리해야 충돌이 없다.

포트 스킴은 ADR-010 / OPS-009 / OPS-010 으로 아래와 같이 확정돼 있다.

| 대상 | 컨테이너 내부 listen | production 호스트 | staging 호스트 |
|---|---|---|---|
| Nginx (EC2 systemd) | — | **80, 443** | **8080, 8443** |
| web (Next.js) | 8000 | `127.0.0.1:8000:8000` | `127.0.0.1:8001:8000` |
| api (Spring) | 8100 | `127.0.0.1:8100:8100` | `127.0.0.1:8101:8100` |
| ai (FastAPI) | 8200 | `127.0.0.1:8200:8200` | `127.0.0.1:8201:8200` |
| postgres | 5432 (표준) | `8300:5432` | `8301:5432` |
| redis | 6379 (표준) | `8400:6379` | `8401:6379` |

핵심 규칙:
- Nginx 가 외부 진입점이고, web/api/ai 컨테이너는 `127.0.0.1:` 로 묶여 외부에서 직접 접근할 수 없다.
- staging 은 production 호스트 포트 **+1 오프셋** 이다. Nginx listen 만 예외.
- postgres/redis 는 개발 편의(ADR-011) 로 `0.0.0.0` 바인딩을 유지하되 OPS-011 의 강한 비밀번호 정책으로 완화한다.

외부에서 보이는 URL:
- production: `https://k14s209.p.ssafy.io` (443 기본)
- staging:    `https://k14s209.p.ssafy.io:8443`

---

## 7. EC2 1회 준비 작업

### 7-1. Docker 설치

```bash
sudo apt update
sudo apt install -y curl git
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"
```

재로그인 후 확인:

```bash
docker version
docker compose version
```

### 7-2. 디렉터리 준비

```bash
sudo mkdir -p /opt/axwms/staging /opt/axwms/production
sudo chown -R "$USER":"$USER" /opt/axwms
```

### 7-3. GitLab Runner 설치 및 등록

SSafy GitLab 은 프로젝트에 공유 Runner 를 제공하지 않으므로, 이 EC2 에 `gitlab-runner` 를 직접 설치해 프로젝트 전용 Runner 로 붙인다.

```bash
curl -L "https://packages.gitlab.com/install/repositories/runner/gitlab-runner/script.deb.sh" | sudo bash
sudo apt install -y gitlab-runner
sudo usermod -aG docker gitlab-runner
sudo systemctl restart gitlab-runner
```

GitLab UI 에서 등록 토큰 발급:
- **Settings → CI/CD → Runners → New project runner**
- Configuration 권장값
  - Paused: 해제
  - **Protected: 해제** (체크 시 작업 브랜치 CI 가 영영 pending 이 된다)
  - Lock to current projects: 체크
  - Maximum job timeout: `1800` (첫 Gradle 빌드가 10분 이상 걸릴 수 있어 여유 필요)
  - Run untagged jobs: 체크

EC2 에서 `sudo gitlab-runner register` 를 실행해 아래 값으로 응답한다.

| 질문 | 입력 값 |
|---|---|
| GitLab instance URL | 프로젝트의 GitLab origin (예: `https://lab.ssafy.com/`) |
| Registration token | UI 에서 발급한 `glrt-...` |
| Description | `axwms-ec2-runner` |
| Executor | `docker` |
| Default image | `docker:27.5.1-cli` |

#### privileged 필수

`api_ci`, `api_image` 는 `docker:27.5.1-dind` service 를 쓰므로 Runner 가 privileged 모드로 동작해야 한다. 등록 직후 `/etc/gitlab-runner/config.toml` 을 열어 `[runners.docker]` 섹션의 `privileged` 를 `true` 로 바꾸고 서비스를 재시작한다.

```toml
[runners.docker]
  image = "docker:27.5.1-cli"
  privileged = true
```

```bash
sudo systemctl restart gitlab-runner
```

#### 검증 포인트
- GitLab UI → Runners 에 해당 runner 가 **online** 상태
- `sudo gitlab-runner list` 결과에 등록된 runner 표시
- `config.toml` 의 `privileged = true` 확인

---

## 8. 서버 `.env` 파일 준비

### staging 예시
파일: `/opt/axwms/staging/.env`

```dotenv
APP_ENV=staging
API_HOST_PORT=8101
WEB_HOST_PORT=8001
POSTGRES_HOST_PORT=8301
REDIS_HOST_PORT=8401
DB_NAME=postgres
DB_USERNAME=postgres
DB_PASSWORD=<staging 전용 강한 무작위 값, 최소 24자>
JWT_SECRET=<staging 전용 최소 32바이트 무작위 값>
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=1209600000
AI_HOST_PORT=8201
GEMINI_API_KEY=<staging 전용 실키>
AWS_S3_BUCKET=<staging/prod 공용 또는 staging 전용 버킷명>
AWS_REGION=<예: ap-northeast-2>
AWS_S3_BASE_PREFIX=<예: staging>
AWS_S3_PUBLIC_BASE_URL=<CDN 미사용 시 S3 기본 URL 또는 CloudFront URL>
AWS_ACCESS_KEY_ID=<staging 전용 IAM access key id>
AWS_SECRET_ACCESS_KEY=<staging 전용 IAM secret access key>
PROFILE_IMAGE_CLEANUP_CRON=0 20 22 * * *
PROFILE_IMAGE_TEMP_PREFIX=temp/
PROFILE_IMAGE_RETENTION_DAYS=1
```

### production 예시
파일: `/opt/axwms/production/.env`

```dotenv
APP_ENV=production
API_HOST_PORT=8100
WEB_HOST_PORT=8000
POSTGRES_HOST_PORT=8300
REDIS_HOST_PORT=8400
DB_NAME=postgres
DB_USERNAME=postgres
DB_PASSWORD=<production 전용 강한 무작위 값, staging 과 반드시 다른 값>
JWT_SECRET=<production 전용 최소 32바이트 무작위 값, staging 과 반드시 다른 값>
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=1209600000
AI_HOST_PORT=8200
GEMINI_API_KEY=<production 전용 실키>
AWS_S3_BUCKET=<production 버킷명>
AWS_REGION=<예: ap-northeast-2>
AWS_S3_BASE_PREFIX=<예: production>
AWS_S3_PUBLIC_BASE_URL=<CDN 미사용 시 S3 기본 URL 또는 CloudFront URL>
AWS_ACCESS_KEY_ID=<production 전용 IAM access key id>
AWS_SECRET_ACCESS_KEY=<production 전용 IAM secret access key>
PROFILE_IMAGE_CLEANUP_CRON=0 20 22 * * *
PROFILE_IMAGE_TEMP_PREFIX=temp/
PROFILE_IMAGE_RETENTION_DAYS=1
```

> `PROFILE_IMAGE_CLEANUP_CRON` 의 cron 식은 컨테이너 TZ 기준이다. compose 가 api 서비스에 `TZ=Asia/Seoul` 을 기본 주입하므로 위 값은 **KST 22:20** 으로 해석된다. UTC 운영이 필요하면 `.env` 에 `API_TZ=UTC` 를 추가하거나 cron 식 자체를 UTC 기준으로 다시 쓴다.

> `WEB_IMAGE` / `API_IMAGE` 는 `.env` 에 두지 않는다. CI 가 `deploy_dev` / `deploy_prod` 단계에서 `WEB_IMAGE='ghcr.io/...:<ref-slug>'` / `API_IMAGE='...'` 형태로 SSH 호출 환경변수로 직접 주입한다 (ADR-003 의 "환경별 값은 서버 `.env` 또는 GitLab Variables" 원칙 + ADR-007 의 ghcr 네임스페이스).

OPS-011 에 따라 `DB_PASSWORD` 는 staging/production 서로 다른 강한 무작위 값으로 유지한다. DB 가 외부 개방된 구조(ADR-011) 에서 비밀번호가 1차 방어선이 된다.

### 중요한 점
- `.env`는 **서버에만** 둔다.
- Git 저장소에는 절대 실제 값을 올리지 않는다.
- `infra/.env.example`는 샘플일 뿐이다.

---

## 9. GitLab CI/CD Variables 설정

GitLab 경로:
- **Settings > CI/CD > Variables**

모든 변수는 **Protected** 로 등록한다.
`SSH_PRIVATE_KEY` 와 `SSH_KNOWN_HOSTS` 는 값 안에 `$` 가 섞여도 그대로 전달되도록 **Expand variable reference 를 끈다**.

### 공통 변수
- `SSH_PRIVATE_KEY` — CI 전용 SSH 개인키 전체 내용 (`-----BEGIN ~ END-----` 포함)
- `SSH_KNOWN_HOSTS` — `ssh-keyscan <EC2_HOST>` 결과 전체
- `DEPLOY_USER` — EC2 로그인 계정 (예: `ubuntu`)

### staging 변수
- `DEPLOY_HOST_STAGING` — EC2 SSH 외부 주소
- `STAGING_URL` — 외부 공개 URL (예: `https://k14s209.p.ssafy.io:8443`)

### production 변수
- `DEPLOY_HOST_PRODUCTION` — 현재는 staging 과 같은 서버여도 된다
- `PRODUCTION_URL` — 외부 공개 URL (예: `https://k14s209.p.ssafy.io`)

### Storage (S3) 자격증명

API 컨테이너가 파일 업로드/다운로드에 사용하는 AWS S3 자격증명이다.
운영 모델은 "GitLab CI/CD Variables → 서버 `.env`" 가 아니라 **서버 `.env` 직접 주입** 을 우선한다 (ADR-003 / GEN-002).
GitLab Variables 에 둘 필요는 일반적으로 없으나, CI 단계에서 S3 접근이 필요해지면(예: 스모크 검증) 그 시점에 아래 정책으로 등록한다.

- `AWS_S3_BUCKET` — staging/production 별 버킷명 (또는 prefix 분리 시 공용 버킷 가능)
- `AWS_REGION` — 예: `ap-northeast-2`
- `AWS_S3_BASE_PREFIX` — 환경별 prefix (예: `staging` / `production`). 같은 버킷 공용 시 데이터 격리 1차 방어선
- `AWS_S3_PUBLIC_BASE_URL` — CDN(CloudFront) 또는 S3 기본 URL
- `AWS_ACCESS_KEY_ID` — staging/production 분리 IAM access key id
- `AWS_SECRET_ACCESS_KEY` — staging/production 분리 IAM secret access key

GitLab Variables 등록 시 정책:
- 6개 모두 Protected.
- `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` 는 단일 줄 + 8자+ 조건을 만족하므로 **Mask** 적용 (OPS-006 / OPS-012 5단계 검증 대상).
- 나머지 4개는 비밀값이 아니므로 Mask 미적용. 단, prefix/버킷명이 운영 토폴로지를 노출할 수 있으면 운영 판단으로 Mask 적용.
- staging/production 별로 IAM key 를 반드시 분리한다 (OPS-011 의 정신을 S3 자격증명에도 적용 — 외부 개방 리소스의 1차 방어선이 키 자체이기 때문).

### Registry 자격증명 — ghcr.io 기준

이미지 레지스트리는 GitHub Container Registry(`ghcr.io`) 를 사용한다 (ADR-007).
이미지 네임스페이스는 영역별로 분리한다 — api 는 `ghcr.io/axwms-s209/axwms-api`, web 은 `ghcr.io/axwms-s209/axwms-web` (ADR-014).

- `GHCR_USER` — PAT 을 발급한 **GitHub 개인 계정 username** (Organization 이름 아님 ⚠)
- `GHCR_TOKEN` — GitHub classic PAT (scopes: `write:packages`, `read:packages`, `repo`)

두 변수 모두 Protected 로 등록하고 `GHCR_TOKEN` 은 Mask 를 적용한다.
PAT 은 만료 기한이 있으므로 만료 전에 갱신하지 않으면 CI 가 `unauthorized` 로 실패한다.
갱신 일정을 캘린더에 미리 등록한다.

### protected variable 주의
`master`는 protected branch라 production 변수와 잘 맞는다.
`dev`에서 staging 자동배포를 하려면 아래 둘 중 하나를 선택해야 한다.

1. `dev`도 protected branch 로 운영한다.
2. staging용 변수만 protected 해제한다.

초기 운영에서는 **`dev`도 보호 브랜치처럼 관리하는 방식**이 더 안전하다.

### 등록/변경 후 5분 체크 (ADR-013 · OPS-012)

신규 변수 등록 · 값 수정은 육안 확인만으로 "완료" 로 간주하지 않는다. 아래 5단계 체크를 거친다.

1. **참조명 정합** — `grep -n '<VARNAME>' .gitlab-ci.yml` 로 CI 파일 안의 실제 참조 Key 원본을 확정한다.
2. **Key 필드 타이핑** — UI 의 Key 필드를 복붙 대신 키보드로 직접 입력한다. 등록 후 커서를 좌우 끝으로 이동해 잉여 공백·유사 유니코드 문자가 없는지 재확인한다.
3. **간접 probe** — 실패/의심 job 의 `before_script` 최상단에 아래 2~3줄만 임시로 추가한 뒤 Retry:

   ```yaml
   - env | awk -F= '/^PREFIX/ {print $1}'
   - echo "V set?=${VARNAME+yes} len=${#VARNAME}"
   ```

   값 자체는 어떤 형태로도 echo 금지(OPS-015). `set?=yes len>0` 이 확인되기 전에는 해당 변수에 의존하는 단계를 운영 경로에서 신뢰하지 않는다.
4. **Masked 조건** — Masked 를 켜려면 값이 단일 줄 · 최소 8자 · 허용 문자 집합 조건을 만족해야 한다. PEM / multi-line / 공백 포함 값은 Masked 대상이 아니다.
5. **Protected × scope 정합** — Protected 변수는 Protected branch/tag 의 job 에만 주입된다. Environments 는 특별한 사정이 없으면 `All (default) *` 로 둔다.

probe 라인은 원인 확정 후 동일 MR 또는 후속 MR 로 반드시 제거한다. 진단 결과 발췌(예: `GHCR_USER set?=yes len=8`) 를 MR 설명에 남겨 OPS-012 의 검증 증거로 사용한다.

증상별 진단 플로우는 [`runbooks/infra/ci-auth-troubleshoot.md`](../../runbooks/infra/ci-auth-troubleshoot.md) 에서 유지한다.

### PEM 기반 변수의 개행 규약 (OPS-013)

`SSH_PRIVATE_KEY` 처럼 `-----BEGIN ... -----` 로 시작하는 PEM 값은:

- 마지막 `-----END ... -----` 뒤에 **LF 개행 1개**를 반드시 포함해 저장한다. 누락 시 `Load key ... error in libcrypto` 로 즉시 실패한다.
- 개행은 LF 고정. Windows 메모장·웹 편집기를 경유하지 말고 서버에서 `cat` 결과를 그대로 붙여넣는다.
- Masked 는 켜지 않는다(multi-line 조건 미충족).
- "Expand variable reference" 를 비활성화한다(값 내 `$` 보호).

---

## 10. `.gitlab-ci.yml` 해설

### 10-1. workflow rules

파이프라인은 아래 3가지 경우만 생성한다.

1. `dev` / `master` 대상 MR
2. `dev`, `master`가 아닌 작업 브랜치 push
3. `dev` / `master` push

이렇게 해야 작업 브랜치 검증과 배포 브랜치 반영이 섞이지 않는다.

`dev` / `master` push pipeline 은 배포 반영 기록이므로 새 commit 이 같은 branch 에 들어와도 auto-cancel 하지 않는다.
GitLab auto-cancel 은 변경 영역(web/api/ai)이 아니라 ref 단위로 동작하므로, 단일 pipeline 구조에서는 "web 중복만 취소하고 api push 로 web 을 취소하지 않기" 같은 영역별 취소를 안전하게 표현할 수 없다.
작업 브랜치와 MR pipeline 은 기존처럼 중복 pipeline 정리를 허용하되, 배포 브랜치에서는 image build 와 deploy 가 끝까지 진행되도록 둔다.

### 10-2. CI job

- `web_ci`: web lint/build
- `ai_ci`: ai pytest
- `api_ci`: api test + bootJar
- `infra_validate`: compose 렌더링 검증

### 10-3. 이미지 빌드 job

- `api_image`: `api/Dockerfile` (컨텍스트 `api/`) 빌드 후 ghcr push, 트리거는 `.api_deploy_changes` (api 코드 + 인프라 변경 시).
- `web_image`: `web/Dockerfile` (컨텍스트 모노레포 루트, `-f web/Dockerfile`) 빌드 후 ghcr push, 트리거는 `.web_deploy_changes` (web 코드 + 모노레포 manifest + 인프라 변경 시). ADR-014.
- `api_image`, `web_image`, `ai_image` 는 `interruptible: false` 로 둔다. 이미지 태그(`:<sha>`, `:<ref-slug>`) push 도중 취소되면 이후 deploy 가 이전 이미지를 재사용할 수 있기 때문이다.

### 10-4. 배포 job

- `deploy_dev`: `dev` push 시 staging 배포
- `deploy_prod`: `master` push 시 production 배포

배포 트리거(`.deploy_changes`) 는 **api/web/모노레포 manifest/infra/docs/infra/.gitlab-ci.yml 중 하나라도 변경**되면 작동한다. 변경 영역에 해당하는 이미지 job 이 함께 돌고, deploy job 은 `API_IMAGE`/`WEB_IMAGE` 두 변수를 SSH 환경변수로 같이 주입한다. 인프라-only 변경에서도 기존 `:<ref-slug>` 태그 이미지로 재배포된다.
`deploy_dev`, `deploy_prod` 도 `interruptible: false` 로 둔다. 배포 중간 취소는 서버 상태와 GitLab pipeline 상태를 어긋나게 만들 수 있으므로 허용하지 않는다.

---

## 11. `infra/compose.deploy.yml` 해설

이 파일은 EC2 배포의 source of truth 다.

### postgres
- `pgvector/pgvector:pg17`
- 데이터는 named volume에 저장
- 환경별 volume 이름 분리

### redis
- `redis:7-alpine`
- healthcheck 포함

### api
- `build:`가 아니라 `image: ${API_IMAGE}` 사용
- 즉, CI가 미리 만든 이미지를 서버가 pull 받아 실행
- postgres/redis가 healthy 상태가 될 때까지 기다림

### web
- `image: ${WEB_IMAGE}` (CMP-004), `build:` 사용 안 함
- 컨테이너 내부 listen 8000, 호스트 바인딩 `127.0.0.1:${WEB_HOST_PORT:-8000}:8000` (production 8000:8000 / staging 8001:8000)
- `depends_on` 없음 — api/redis 가 미준비 상태여도 정적 자산 + 클라이언트 라우팅은 떠 있어야 한다는 판단(ADR-014). API 호출 실패는 web 단에서 처리.
- 환경변수: `NODE_ENV=production`, `PORT=8000`, `HOSTNAME=0.0.0.0` (Next.js standalone server.js 가 인식)

---

## 12. 실제 배포가 어떻게 진행되는가

### `dev` merge 후

1. GitLab이 변경 영역의 이미지를 빌드 (api 변경이면 `api_image`, web 변경이면 `web_image`, 인프라-only 면 둘 다 스킵)
2. ghcr.io 에 `axwms-api:dev`, `axwms-web:dev` 등 `:<ref-slug>` + `:<sha>` 두 태그로 push
3. CI가 staging 서버에 SSH 접속
4. `compose.deploy.yml`, `remote-deploy.sh` 를 서버에 복사
5. 서버에서 `docker compose pull api web` / `up -d --remove-orphans postgres redis api web` (OPS-016 의 멱등 흐름)

### `master` merge 후
같은 흐름으로 production 에 반영된다.

---

## 13. Nginx 진입 구조

Nginx 를 EC2 systemd 로 운영해 **모든 외부 요청의 단일 진입점** 으로 둔다 (ADR-010).
컨테이너(web/api/ai) 는 `127.0.0.1:` 로만 바인딩되어 외부에서 Nginx 를 우회할 수 없다.

### 13-1. 진입 경로 요약

```text
외부 사용자
    │
    ▼  https://k14s209.p.ssafy.io[:8443]
┌──────────────────────────────────────────────┐
│  systemd Nginx (EC2 호스트)                   │  ← 외부에 보이는 유일한 문
│  ├─ TLS 종료 (Let's Encrypt 인증서)           │
│  ├─ 80  → 443 redirect (production)           │
│  ├─ 443 (production) / 8443 (staging): HTTPS  │
│  │  ├─ /       → 127.0.0.1:8000|8001 (web)   │
│  │  ├─ /api/   → 127.0.0.1:8100|8101 (api)   │
│  │  └─ /ai/    → 127.0.0.1:8200|8201 (ai)    │
└──────────────────────────────────────────────┘
```

### 13-2. Nginx 설정의 SSOT

저장소의 다음 3개 파일이 SSOT 다(OPS-017, OPS-020).

| 저장소 경로 | EC2 경로 | 역할 |
|---|---|---|
| `infra/nginx/sites-available/axwms.conf` | `/etc/nginx/sites-available/axwms.conf` | server 블록 본체 + 라우팅 |
| `infra/nginx/snippets/proxy-headers.conf` | `/etc/nginx/snippets/proxy-headers.conf` | upstream 공통 proxy header set |
| `infra/nginx/snippets/rate-limit.conf` | `/etc/nginx/snippets/rate-limit.conf` | per-IP rate/connection limit + slowloris timeout (ADR-018) |

EC2 측 세 파일은 deploy job 이 매 배포마다 저장소 conf 로 덮어쓴다 — SSH 로 EC2 에 접속해서 직접 수정하지 말 것. 변경은 PR 리뷰 후 dev/master push pipeline 의 `deploy_dev` / `deploy_prod` 가 자동 동기화한다.

본문 골격은 아래와 같다(저장소 conf 와 1:1 일치). server 블록 밖에 둔 `include` 는 nginx.conf 의 `http {}` 가 `sites-enabled/*` 를 include 하므로 http 컨텍스트로 진입한다.

```nginx
# http 컨텍스트로 진입 — zone 정의·응답 코드·timeout 은 모두 snippet 안에서 관리.
include /etc/nginx/snippets/rate-limit.conf;

upstream axwms_web_prod      { server 127.0.0.1:8000; }
upstream axwms_web_staging   { server 127.0.0.1:8001; }
upstream axwms_api_prod      { server 127.0.0.1:8100; }
upstream axwms_api_staging   { server 127.0.0.1:8101; }
upstream axwms_ai_prod       { server 127.0.0.1:8200; }
upstream axwms_ai_staging    { server 127.0.0.1:8201; }

# ACME challenge + HTTPS redirect (production)
server {
    listen 80;
    server_name k14s209.p.ssafy.io;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    location / {
        return 301 https://$host$request_uri;
    }
}

# production (HTTPS)
server {
    listen 443 ssl http2;
    server_name k14s209.p.ssafy.io;

    ssl_certificate     /etc/letsencrypt/live/k14s209.p.ssafy.io/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/k14s209.p.ssafy.io/privkey.pem;

    limit_conn perip 100;

    location /api/auth/ { limit_req zone=req_auth    burst=30  nodelay; proxy_pass http://axwms_api_prod; include /etc/nginx/snippets/proxy-headers.conf; }
    location /api/      { limit_req zone=req_general burst=200 nodelay; proxy_pass http://axwms_api_prod; include /etc/nginx/snippets/proxy-headers.conf; }
    location /ai/       { limit_req zone=req_ai      burst=60  nodelay; proxy_read_timeout 180s; proxy_pass http://axwms_ai_prod; include /etc/nginx/snippets/proxy-headers.conf; }
    location /          { limit_req zone=req_general burst=200 nodelay; proxy_pass http://axwms_web_prod; include /etc/nginx/snippets/proxy-headers.conf; }
}

# staging (HTTPS) — production 과 동일 패턴, upstream 만 staging 쪽
server {
    listen 8080;
    server_name k14s209.p.ssafy.io;
    return 301 https://$host:8443$request_uri;
}
server {
    listen 8443 ssl http2;
    server_name k14s209.p.ssafy.io;

    ssl_certificate     /etc/letsencrypt/live/k14s209.p.ssafy.io/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/k14s209.p.ssafy.io/privkey.pem;

    limit_conn perip 100;

    location /api/auth/ { limit_req zone=req_auth    burst=30  nodelay; proxy_pass http://axwms_api_staging; include /etc/nginx/snippets/proxy-headers.conf; }
    location /api/      { limit_req zone=req_general burst=200 nodelay; proxy_pass http://axwms_api_staging; include /etc/nginx/snippets/proxy-headers.conf; }
    location /ai/       { limit_req zone=req_ai      burst=60  nodelay; proxy_read_timeout 180s; proxy_pass http://axwms_ai_staging; include /etc/nginx/snippets/proxy-headers.conf; }
    location /          { limit_req zone=req_general burst=200 nodelay; proxy_pass http://axwms_web_staging; include /etc/nginx/snippets/proxy-headers.conf; }
}
```

`/etc/nginx/snippets/proxy-headers.conf` 예시:

```nginx
proxy_http_version 1.1;
proxy_set_header Host              $host;
proxy_set_header X-Real-IP         $remote_addr;
proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
```

`/etc/nginx/snippets/rate-limit.conf` 의 zone 정의는 13-7 절에서 별도로 다룬다.

### 13-3. deploy user 의 sudoers 등록 (1회)

deploy job 이 EC2 의 nginx conf 를 덮어쓰고 reload 하려면 deploy user(보통 `ubuntu`)가 비밀번호 없이 sudo 로 6개 명령을 실행할 수 있어야 한다(OPS-018). EC2 에서 1회 등록한다. ADR-018 의 `rate-limit.conf` 가 동일 흐름으로 편입되면서 5개 → 6개로 늘었다.

```bash
sudo tee /etc/sudoers.d/axwms-deploy <<'EOF'
ubuntu ALL=(root) NOPASSWD: /usr/bin/install -o root -g root -m 0644 /tmp/axwms.conf /etc/nginx/sites-available/axwms.conf
ubuntu ALL=(root) NOPASSWD: /usr/bin/install -o root -g root -m 0644 /tmp/proxy-headers.conf /etc/nginx/snippets/proxy-headers.conf
ubuntu ALL=(root) NOPASSWD: /usr/bin/install -o root -g root -m 0644 /tmp/rate-limit.conf /etc/nginx/snippets/rate-limit.conf
ubuntu ALL=(root) NOPASSWD: /usr/bin/mkdir -p /etc/nginx/snippets
ubuntu ALL=(root) NOPASSWD: /usr/sbin/nginx -t
ubuntu ALL=(root) NOPASSWD: /usr/bin/systemctl reload nginx
EOF
sudo chmod 440 /etc/sudoers.d/axwms-deploy
sudo visudo -c
```

`visudo -c` 가 `/etc/sudoers: parsed OK` 와 `/etc/sudoers.d/axwms-deploy: parsed OK` 를 모두 표시해야 한다. 한 줄이라도 문법 오류면 sudo 자체가 잠겨 EC2 작업이 막힐 위험 — 등록 직후 새 SSH 세션에서 `sudo -n nginx -t` 가 비밀번호 없이 통과하는지 한 번 확인한다.

`ubuntu` 외 다른 deploy 계정이라면 모든 줄의 첫 단어를 그 계정으로 바꾼다. 권한 최소화 원칙(OPS-018)에 따라 `ALL=(ALL) NOPASSWD: ALL` 같은 광역 부여는 사용하지 않는다.

### 13-3a. sites-enabled symlink (1회)

Debian/Ubuntu 의 nginx 는 `/etc/nginx/nginx.conf` 가 `/etc/nginx/sites-enabled/*` 를 include 한다. 저장소 conf 는 deploy job 이 `/etc/nginx/sites-available/axwms.conf` 로 install 하므로, **nginx 가 그 파일을 실제로 로드하려면 `sites-enabled` 에서 그 파일을 가리키는 symlink 가 한 번 만들어져 있어야 한다**.

```bash
sudo ln -sf /etc/nginx/sites-available/axwms.conf /etc/nginx/sites-enabled/axwms.conf
sudo rm -f /etc/nginx/sites-enabled/default        # 기본 server 블록(80 포트) 충돌 방지
sudo nginx -t
sudo systemctl reload nginx
```

- `-sf` 의 `-f` 는 이미 symlink 가 있어도 덮어쓴다(idempotent).
- `default` site 를 제거하지 않으면 우리 production 의 80 server 와 listen 포트가 겹쳐 어느 한쪽이 무시되거나 `nginx -t` 가 충돌 경고를 띄운다.
- 본 절차는 **새 EC2 1회**만 필요. 그 이후 conf 변경은 deploy job 이 install 단계에서 원본을 갱신하고 reload 해주므로 추가 symlink 작업은 없다.

> **주의 — silent failure**: symlink 누락 시 deploy job 의 `nginx -t` 는 (sites-enabled 의 다른 파일만 검증하기 때문에) 그대로 통과하고 reload 도 성공한다. job 로그는 녹색인데 외부 동작은 변함 없는 가장 헷갈리는 사고 패턴이다. 새 EC2 작업 시 본 절차를 빼먹지 말 것.

### 13-4. nginx conf 자동 동기화 흐름

`deploy_dev` / `deploy_prod` 가 다음 한 묶음을 SSH 한 번으로 실행한다(`.gitlab-ci.yml` 의 deploy job script 끝).

```bash
scp infra/nginx/sites-available/axwms.conf  $DEPLOY_USER@$TARGET_HOST:/tmp/axwms.conf
scp infra/nginx/snippets/proxy-headers.conf $DEPLOY_USER@$TARGET_HOST:/tmp/proxy-headers.conf
scp infra/nginx/snippets/rate-limit.conf    $DEPLOY_USER@$TARGET_HOST:/tmp/rate-limit.conf
ssh $DEPLOY_USER@$TARGET_HOST '
  sudo install -o root -g root -m 0644 /tmp/axwms.conf /etc/nginx/sites-available/axwms.conf &&
  sudo mkdir -p /etc/nginx/snippets &&
  sudo install -o root -g root -m 0644 /tmp/proxy-headers.conf /etc/nginx/snippets/proxy-headers.conf &&
  sudo install -o root -g root -m 0644 /tmp/rate-limit.conf /etc/nginx/snippets/rate-limit.conf &&
  sudo nginx -t &&
  sudo systemctl reload nginx &&
  rm -f /tmp/axwms.conf /tmp/proxy-headers.conf /tmp/rate-limit.conf
'
```

핵심 포인트:
- `&&` 로 묶여 있어 `nginx -t` 가 실패하면 `systemctl reload nginx` 는 실행되지 않는다 — 깨진 conf 가 운영에 반영될 가능성을 차단한다.
- staging 과 production 은 **같은 EC2 의 같은 nginx** 를 공유하므로 두 deploy 가 모두 reload 해도 결과는 동일(멱등). 인프라 변경 시 한 push 에서 reload 가 두 번 일어날 수 있지만 기능에는 문제 없음.
- conf 가 PR 단계에서 검증되지 않은 채 deploy 까지 흘러가면 nginx-t 실패로 운영이 잠시 멈출 수 있다. PR 리뷰에서 `nginx -t` 사고 패턴(중괄호 짝, listen 포트 충돌, certificate 경로 오타) 을 확인한다.

### 13-5. TLS 인증서 (Let's Encrypt + certbot)

ADR-012 에 따라 certbot 으로 발급·갱신한다.

초기 발급 (서버 작업 1회):

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d k14s209.p.ssafy.io
```

자동 갱신: certbot 설치 시 `certbot.timer` 가 자동 활성화되며 하루 2회 `certbot renew` 를 수행한다. 갱신 후 Nginx reload 를 위해 아래를 등록한다.

```bash
sudo tee /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh <<'EOF'
#!/usr/bin/env bash
systemctl reload nginx
EOF
sudo chmod +x /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh
```

주의: 80 포트는 ACME HTTP-01 challenge 를 위해 Nginx 가 항상 listen 해야 한다. production 이 443 만 쓰더라도 80 은 redirect + challenge 용도로 남긴다.

### 13-6. 꼭 같이 확인할 것

1. SSAFY 보안그룹에 `80`, `443`, `8080`, `8443`, `8300/8301`(postgres), `8400/8401`(redis) 인바운드가 열려 있는지
2. 서버 내부 방화벽(`ufw`) 사용 시 같은 포트 허용 여부
3. staging `.env` 의 `API_HOST_PORT=8101` / production `.env` 의 `API_HOST_PORT=8100` 이 실제 compose 와 일치하는지 (+1 오프셋 규칙 재확인)
4. Nginx reload 전에 `sudo nginx -t` 로 설정 검증을 했는지
5. `sudo certbot certificates` 로 인증서 만료 일자 확인 (갱신 실패 시 수동 `certbot renew`)
6. `/etc/nginx/nginx.conf` 의 `server_tokens` 가 활성 정의되어 있지 않은지 — `grep -nE '^[^#]*server_tokens' /etc/nginx/nginx.conf` 가 0줄이어야 snippet 의 `server_tokens off;` 가 충돌 없이 적용된다. 활성 정의가 있다면 nginx.conf 측을 코멘트 처리.
7. `/etc/nginx/nginx.conf` 의 `keepalive_timeout` 값 — Debian/Ubuntu 기본 65s 와 본 snippet 이 중복 정의되면 `nginx -t` 가 `directive is duplicate` 로 실패한다(OPS-020). slowloris 강화가 필요하면 snippet 이 아니라 nginx.conf 측 값을 조정한다.

### 13-7. Per-IP rate limit / connection limit (ADR-018)

#### 무엇을 막는가
- 한 IP 가 만드는 비정상 부하(brute-force, scraper, 실수 무한루프, slowloris) 를 nginx 진입 시점에 즉시 거부(429) 한다.
- 분산 DDoS 는 단일 nginx 만으로 흡수 불가 — CDN/WAF 가 별도 트랙으로 필요(섹션 17 참고).

#### 동작 모델 — leaky bucket
nginx `limit_req` 는 **시간 차단이 아니라 토큰 잔량 검사** 다.
- `rate` 는 평균 처리 속도(= 토큰 회복 속도). 예: `20r/s` = 50ms 마다 토큰 1개.
- `burst` 는 토큰 부족 시 임시로 쌓을 수 있는 큐 크기. 큐가 차면 즉시 429.
- `nodelay` 는 큐의 요청을 평균 rate 로 천천히 풀지 않고 즉시 처리(burst 까지는 빠르게 통과, 그 이상은 즉시 거부).
- "IP 를 N분 차단" 같은 영속 차단은 하지 않는다 — 토큰 회복되면 자동으로 다시 통과. 영속 차단이 필요하면 fail2ban 또는 CDN/WAF.

#### 적용 zone

zone 키는 모두 `$binary_remote_addr` 라 **IP 당** 카운터다. 같은 IP 뒤 다수 유저(NAT) 는 공동 영향.

**값 가정**: SSAFY 캠퍼스 NAT 환경에서 **한 외부 IP 뒤 약 30명 동시 사용자** (ADR-018).

| zone | 대상 | rate | burst | 의미 |
|---|---|---|---|---|
| `perip` | 모든 라우트 | 동시 100 connection | — | 30명 × 1 connection + HTTP/1.1 fallback 마진 |
| `req_auth` | `/api/auth/*` | 5r/m | 30 | brute-force 페이스 cap 유지(5r/m), burst 만 30명 동시 로그인 흡수 |
| `req_ai` | `/ai/*` | 30r/m | 60 | 30명 × 분당 1회 검색 + 2회 burst. Gemini 비용 보호선 |
| `req_general` | `/`, `/api/*` 일반 | 30r/s | 200 | 30명 × 평균 1 req/s + 첫 페이지 로드 burst 흡수 |

값은 `infra/nginx/snippets/rate-limit.conf` 한 파일에서 관리한다(OPS-020). 튜닝은 PR 리뷰 후 자동 동기화 흐름(13-4) 으로 반영하고, EC2 SSH 직접 수정 금지.

NAT 가정이 안 맞는 환경(사용자 대부분이 분산 IP)이라면 값이 과도하게 느슨할 수 있다 — 운영 관측에서 정상 사용자 429 가 거의 안 나오면 값 하향 또는 `geo` map 분기(별 MR) 검토.

#### 운영 점검 — 1주 관측 후 튜닝

```bash
# 429 응답 빈도 — 이상치 IP 와 시간대 파악
sudo grep ' 429 ' /var/log/nginx/access.log | awk '{print $1}' | sort | uniq -c | sort -rn | head

# 특정 zone 의 거부량 — error.log 에서 limit_req 로그 (기본 level=error)
sudo grep 'limiting requests' /var/log/nginx/error.log | tail
```

- 정상 사용자 IP 가 429 를 받고 있으면 → 값 완화(또는 인증된 사용자 IP 화이트리스트 별 MR).
- 의심 IP 만 429 를 받고 있고 빈도가 반복적이면 → fail2ban 도입 시그널(섹션 17 #1).

#### 운영자 1회 후속 작업 — 새 EC2 부트스트랩 시

1. `sudoers.d/axwms-deploy` 등록 시 13-3 의 6개 명령 그대로 (rate-limit.conf install 줄 포함).
2. 첫 배포 후 `sudo grep -c 'limit_req' /etc/nginx/sites-available/axwms.conf` 가 0 보다 큰 값 → 본 ADR 적용 확인.
3. `curl -sI -X POST https://k14s209.p.ssafy.io/api/auth/login` 을 빠르게 31회 호출(`for i in $(seq 31); do curl -sI -o /dev/null -w "%{http_code}\n" -X POST https://k14s209.p.ssafy.io/api/auth/login; done`) → 처음 30개는 통과 응답코드, 31번째부터 `429` 가 나와야 한다(스모크 검증).

---

## 14. 처음 적용할 때 추천 순서

1. EC2에 Docker 설치
2. `/opt/axwms/staging`, `/opt/axwms/production` 생성
3. GitLab Runner 설치 및 프로젝트 register (privileged 포함)
4. 각 환경 `.env` 작성 (600, 배포 계정 소유) — `WEB_HOST_PORT` / `API_HOST_PORT` / `POSTGRES_HOST_PORT` / `REDIS_HOST_PORT` 모두 OPS-009 의 +1 오프셋 규칙대로, `DB_PASSWORD` 는 staging/production 각각 강한 무작위 값 (OPS-011)
5. CI 전용 SSH 키페어 생성 및 EC2 `authorized_keys` 등록
6. GitLab Variables 등록 + `master` / `dev` Protected Branch 설정
7. Nginx 설치 + sites-enabled symlink 생성(섹션 13-3a) + deploy user sudoers 등록(섹션 13-3) — 이후 conf 본체는 `deploy_dev` / `deploy_prod` 가 자동 동기화하므로 EC2 에서 직접 작성하지 않는다
8. `sudo certbot --nginx -d k14s209.p.ssafy.io` 로 인증서 초기 발급 + renewal hook 등록 (섹션 13-5)
9. 작업 브랜치 -> `dev` MR 생성
10. merge 후 staging 배포 로그 확인
11. staging 확인 후 `master` 반영

---

## 15. 자주 틀리는 포인트

### 15-1. `dev` 배포가 안 된다
보통 원인은 두 가지다.

- `dev`에서 protected variable 을 읽지 못함
- `DEPLOY_HOST_STAGING` / `SSH_PRIVATE_KEY` 미설정

### 15-2. 서버 접속은 되는데 배포가 안 된다
주로 아래를 본다.

- `/opt/axwms/{env}/.env`가 실제로 있는가
- docker compose 가 설치되어 있는가
- registry login 이 되는가

### 15-3. 컨테이너는 떴는데 외부 접속이 안 된다
이 경우는 대부분 애플리케이션이 아니라 **Nginx 또는 보안그룹/포트 연결 문제**다.

확인 순서:
1. 서버 내부에서 `curl http://127.0.0.1:<API_HOST_PORT>/api-docs`
2. Nginx 가 해당 외부 포트(`8989` 또는 `8990`)를 실제로 listen 하는지 확인
3. `sudo nginx -t` 와 `sudo systemctl reload nginx` 결과 확인
4. 마지막으로 AWS Security Group / 방화벽 확인

### 15-4. `api_image` 의 `docker login` 이 `Must provide --username with --password-stdin` 로 실패
대부분 `GHCR_USER` 가 runtime 에 주입되지 않아 `-u ""` 가 전달된 경우다.
간접 probe(`env | awk /^GHCR/`, `${GHCR_USER+yes}`) 로 주입된 Key 목록과 set 여부를 확인한다.
유사 이름(`GHCR_NAME` 등)으로 등록됐거나 값이 빈 문자열일 가능성이 높다.
복구 절차는 [`runbooks/infra/ci-auth-troubleshoot.md`](../../runbooks/infra/ci-auth-troubleshoot.md) A 케이스.

### 15-5. `deploy_*` 가 `error in libcrypto` / `Permission denied (publickey)` 로 실패
`libcrypto` 에러가 앞서 나오면 SSH 서버 권한 문제가 아니라 **키 파일 파싱 실패**다.
`SSH_PRIVATE_KEY` 의 `-----END ... -----` 뒤 trailing newline 누락 또는 CRLF 개행 오염이 주 원인이다.
복구는 [`runbooks/infra/ci-auth-troubleshoot.md`](../../runbooks/infra/ci-auth-troubleshoot.md) B 케이스.

### 15-6. 정상 사용자가 `429 Too Many Requests` 를 받는다
ADR-018 의 rate limit 이 너무 빡빡하거나, NAT 뒤 다수 사용자가 같은 외부 IP 로 묶여 공동 영향을 받는 경우.

확인 순서:
1. `/var/log/nginx/access.log` 에서 그 IP 의 429 패턴 — 단일 사용자인지, 같은 IP 뒤 다수 사용자인지 행동 패턴으로 추정.
2. `error.log` 의 `limiting requests, excess: ... by zone "<zone 이름>"` 로그로 어느 zone 이 차단했는지 확인.
3. 차단 zone 이 `req_general` 이고 정상 패턴이면 → `infra/nginx/snippets/rate-limit.conf` 의 burst 값을 단계적으로 상향(40 → 60). PR 리뷰 후 자동 동기화.
4. 차단 zone 이 `req_auth` 이고 정상 로그인 패턴이면 → 클라이언트가 토큰 갱신을 과도하게 호출하는 버그 가능성 먼저 점검(rate cap 완화는 마지막 수단).
5. 사내 IP 공동 영향이면 → `geo` map 화이트리스트 별 MR 검토(ADR-018 의 본 ADR 범위 밖 항목).

값 튜닝은 EC2 SSH 직접 수정 금지(OPS-020). PR 리뷰 + 13-4 자동 동기화 흐름 그대로.

---

## 16. 현재 의도적으로 하지 않은 것

이번 단계에서 일부러 제외한 것:

- 과거 임시 검증 절차 재사용
- EC2에서 소스 직접 빌드
- 전체 스택 단일 compose 통합
- **fail2ban** — 반복 429/401 발생 IP 의 영속 차단. nginx 의 즉시 거부(429) 가 1차 cap 으로 들어왔으므로 fail2ban 은 그 위 층으로 별 ADR 검토.
- **CDN/WAF (Cloudflare 등)** — 분산 DDoS / 봇 관리. SSAFY DNS 정책 의존이라 pending-decisions 항목으로 분리.
- **앱 레벨 throttle** — Spring Security 의 로그인 lockout, FastAPI 의 Gemini quota. nginx 앞단 cap 과 책임 분담은 별 ADR.
- **nginx access log 보관 / 로테이션** — 운영 안정성 보강 항목으로 별 MR.

이유는 지금은 **배포 경로를 단순하게 만들고, 자동 동기화 흐름의 일관성을 먼저 안정화하는 것**이 우선이기 때문이다.
TLS 자동 갱신은 ADR-012 로, web/ai 자동배포 편입은 ADR-014/016 으로, nginx conf 저장소 편입은 ADR-015 로, per-IP rate limit 은 ADR-018 로 범위에 포함되었다.

---

## 17. 다음 단계 후보

기본 자동배포 + nginx conf 동기화 + per-IP rate limit 이 안정화된 시점에서 우선순위 순서로 확장한다.

1. **fail2ban 도입** — 반복 429/401 IP 를 iptables 수준에서 N분~N시간 차단. nginx 의 즉시 거부 위 층 보호.
2. **앱 레벨 throttle** — Spring Security 의 로그인 lockout, FastAPI 의 Gemini quota(외부 유료 호출 비용 보호). nginx 값과 책임 분담을 본 결정에서 명문화.
3. **`deploy_prod` 를 manual 승인형으로 변경** — 운영 반영 게이팅.
4. **nginx access log 보관 / 로테이션** — 429 빈도 관측 + 인증 실패 패턴 분석을 위한 보관 정책 확정.
5. **`geo` map 화이트리스트** — 사내 IP / 신뢰 IP 의 429 면제. NAT 뒤 공동 영향이 관측될 때.
6. **CDN/WAF (Cloudflare 등) 도입** — 분산 DDoS / 봇 관리. SSAFY 의 `k14s209.p.ssafy.io` DNS CNAME / NS 변경 가능 여부를 먼저 확인.
7. **nginx keep-alive 풀** — `upstream { keepalive N; }` + `proxy_set_header Connection ""` 짝꿍 한 번에 추가 (ADR-015 의 6번 "본 ADR 범위 밖" 항목).
8. **파이프라인 `changes` 세분화** (pending-decisions #2) — 문서-only MR 이 배포까지 도는 비용을 줄이는 후행 정리.

---

## 18. 한 줄 요약

현재 기준의 정답은 아래다.

- **작업 브랜치에서는 CI만**
- **dev/master merge 결과에서만 CD**
- **EC2는 빌드 서버가 아니라 실행 서버**
- **배포 source of truth는 `infra/compose.deploy.yml`와 `.gitlab-ci.yml`**
