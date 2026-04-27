# EC2 + GitLab CI/CD 배포 가이드

- 기준 문서: [`docs/infra/adr.yaml`](adr.yaml), [`docs/infra/code-convention.yaml`](code-convention.yaml)
- 목적: AX-WMS 인프라를 처음 보는 사람도 EC2 + GitLab CI/CD 자동배포를 그대로 재현할 수 있도록 1차 운영 절차를 제공한다.
- 범위: api/web/postgres/redis 자동배포, GitLab Runner 운영, 브랜치별 파이프라인 동작, 서버 `.env` 와 GitLab CI/CD Variables 운영, Nginx 진입 예시.
- 전제: EC2 호스트, SSafy GitLab 사용, ghcr.io 이미지 레지스트리(ADR-007), 외부 도메인/HTTPS 사전 준비.
- 연계 문서: [`runbooks/infra/ci-auth-troubleshoot.md`](../../runbooks/infra/ci-auth-troubleshoot.md) (CI 인증 자격증명 장애 대응), [`docs/api/jooq-codegen-policy.md`](../api/jooq-codegen-policy.md) (`api_ci` 의 DinD 의존 배경).

이 문서는 **AX-WMS 인프라를 처음 보는 사람도 그대로 따라갈 수 있게** 작성한 1차 운영 가이드다.
현재 범위는 **api / web / postgres / redis 자동배포**이며, `ai`, `nginx conf 저장소 편입` 은 후속 작업으로 남겨둔다(ADR-014, pending-decisions #3 / #5 참조).

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
| `docs/infra/adr.yaml` | 인프라 의사결정 기록 |
| `docs/infra/code-convention.yaml` | 인프라 문서/설정 변경 시 지켜야 할 규칙 |
| `docs/infra/web-deploy-rationale.md` | ADR-014 결정의 대안 비교·기각 근거 + 코드 리딩 가이드 |

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
```

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

### 10-2. CI job

- `web_ci`: web lint/build
- `ai_ci`: ai pytest
- `api_ci`: api test + bootJar
- `infra_validate`: compose 렌더링 검증

### 10-3. 이미지 빌드 job

- `api_image`: `api/Dockerfile` (컨텍스트 `api/`) 빌드 후 ghcr push, 트리거는 `.api_deploy_changes` (api 코드 + 인프라 변경 시).
- `web_image`: `web/Dockerfile` (컨텍스트 모노레포 루트, `-f web/Dockerfile`) 빌드 후 ghcr push, 트리거는 `.web_deploy_changes` (web 코드 + 모노레포 manifest + 인프라 변경 시). ADR-014.

### 10-4. 배포 job

- `deploy_dev`: `dev` push 시 staging 배포
- `deploy_prod`: `master` push 시 production 배포

배포 트리거(`.deploy_changes`) 는 **api/web/모노레포 manifest/infra/docs/infra/.gitlab-ci.yml 중 하나라도 변경**되면 작동한다. 변경 영역에 해당하는 이미지 job 이 함께 돌고, deploy job 은 `API_IMAGE`/`WEB_IMAGE` 두 변수를 SSH 환경변수로 같이 주입한다. 인프라-only 변경에서도 기존 `:<ref-slug>` 태그 이미지로 재배포된다.

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

### 13-2. Nginx 예시 설정

실제 conf 저장소 편입은 후속 MR 예정이며(아래 섹션 17 참고), 현재는 EC2 의 `/etc/nginx/sites-available/axwms.conf` 에 아래 골격을 유지한다.

```nginx
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

    location /api/ { proxy_pass http://axwms_api_prod; include /etc/nginx/snippets/proxy-headers.conf; }
    location /ai/  { proxy_pass http://axwms_ai_prod;  include /etc/nginx/snippets/proxy-headers.conf; }
    location /     { proxy_pass http://axwms_web_prod; include /etc/nginx/snippets/proxy-headers.conf; }
}

# staging (HTTPS)
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

    location /api/ { proxy_pass http://axwms_api_staging; include /etc/nginx/snippets/proxy-headers.conf; }
    location /ai/  { proxy_pass http://axwms_ai_staging;  include /etc/nginx/snippets/proxy-headers.conf; }
    location /     { proxy_pass http://axwms_web_staging; include /etc/nginx/snippets/proxy-headers.conf; }
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

### 13-3. TLS 인증서 (Let's Encrypt + certbot)

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

### 13-4. 꼭 같이 확인할 것

1. SSAFY 보안그룹에 `80`, `443`, `8080`, `8443`, `8300/8301`(postgres), `8400/8401`(redis) 인바운드가 열려 있는지
2. 서버 내부 방화벽(`ufw`) 사용 시 같은 포트 허용 여부
3. staging `.env` 의 `API_HOST_PORT=8101` / production `.env` 의 `API_HOST_PORT=8100` 이 실제 compose 와 일치하는지 (+1 오프셋 규칙 재확인)
4. Nginx reload 전에 `sudo nginx -t` 로 설정 검증을 했는지
5. `sudo certbot certificates` 로 인증서 만료 일자 확인 (갱신 실패 시 수동 `certbot renew`)

---

## 14. 처음 적용할 때 추천 순서

1. EC2에 Docker 설치
2. `/opt/axwms/staging`, `/opt/axwms/production` 생성
3. GitLab Runner 설치 및 프로젝트 register (privileged 포함)
4. 각 환경 `.env` 작성 (600, 배포 계정 소유) — `WEB_HOST_PORT` / `API_HOST_PORT` / `POSTGRES_HOST_PORT` / `REDIS_HOST_PORT` 모두 OPS-009 의 +1 오프셋 규칙대로, `DB_PASSWORD` 는 staging/production 각각 강한 무작위 값 (OPS-011)
5. CI 전용 SSH 키페어 생성 및 EC2 `authorized_keys` 등록
6. GitLab Variables 등록 + `master` / `dev` Protected Branch 설정
7. Nginx 설치 + `/etc/nginx/sites-available/axwms.conf` 적용 + `sudo nginx -t` + reload (섹션 13-2)
8. `sudo certbot --nginx -d k14s209.p.ssafy.io` 로 인증서 초기 발급 + renewal hook 등록 (섹션 13-3)
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

---

## 16. 현재 의도적으로 하지 않은 것

이번 단계에서 일부러 제외한 것:

- 과거 임시 검증 절차 재사용
- EC2에서 소스 직접 빌드
- ai 운영 자동배포 (web 은 ADR-014 로 처리됨)
- 전체 스택 단일 compose 통합
- Nginx conf 저장소 편입 (서버 작업 1회 발급과 동기화가 필요, `docs/infra/pending-decisions.md` #3 참고)

이유는 지금은 **배포 경로를 단순하게 만들고, api 자동배포를 먼저 안정화하는 것**이 우선이기 때문이다.
TLS 자동 갱신은 ADR-012 로 범위에 포함되었다.

---

## 17. 다음 단계 후보

api/web 자동배포가 안정화되면 다음 순서로 확장하면 된다.

1. `deploy_prod`를 manual 승인형으로 변경할지 결정
2. ai Dockerfile 추가 + 자동배포 편입 (pending-decisions #5 의 ai 잔여 항목)
3. Nginx 설정 파일을 저장소 기준으로 통합 관리 + reload 자동화 (pending-decisions #3)
4. 파이프라인 `changes` 세분화 (pending-decisions #2) — 문서-only MR 이 배포까지 도는 비용을 줄이는 후행 정리

---

## 18. 한 줄 요약

현재 기준의 정답은 아래다.

- **작업 브랜치에서는 CI만**
- **dev/master merge 결과에서만 CD**
- **EC2는 빌드 서버가 아니라 실행 서버**
- **배포 source of truth는 `infra/compose.deploy.yml`와 `.gitlab-ci.yml`**
