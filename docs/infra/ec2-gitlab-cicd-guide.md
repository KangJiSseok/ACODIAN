# EC2 + GitLab CI/CD 배포 가이드

이 문서는 **AX-WMS 인프라를 처음 보는 사람도 그대로 따라갈 수 있게** 작성한 1차 운영 가이드다.
현재 범위는 **api / postgres / redis 자동배포**이며, `web`, `ai`, `nginx` 전체 자동화는 후속 작업으로 남겨둔다.

---

## 1. 이 문서가 다루는 것

현재 운영 가정은 아래와 같다.

- 서버: **EC2**
- 소스 저장소: **GitLab**
- 배포 방식: **GitLab CI/CD가 이미지를 빌드해서 Registry에 push → EC2가 pull 받아 compose로 기동**
- 외부 도메인/HTTPS: **이미 준비됨**
- 현재 자동배포 대상: **api + postgres + redis**

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
  -> api Docker image build
  -> GitLab Container Registry push
  -> SSH로 EC2 staging 접속
  -> docker compose pull / up -d

MR merge -> master push
  -> GitLab CI
  -> api Docker image build
  -> GitLab Container Registry push
  -> SSH로 EC2 production 접속
  -> docker compose pull / up -d
```

---

## 4. 저장소 안에서 중요한 파일

| 파일 | 역할 |
|---|---|
| `.gitlab-ci.yml` | GitLab 파이프라인 규칙과 job 정의 |
| `api/Dockerfile` | api 이미지를 만드는 Dockerfile |
| `infra/compose.deploy.yml` | EC2에서 실제로 사용하는 compose 파일 |
| `infra/.env.example` | 서버 `.env` 샘플 |
| `infra/scripts/remote-deploy-api.sh` | 서버에서 실제 배포를 수행하는 스크립트 |
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
│   └── remote-deploy-api.sh
└── production/
    ├── .env
    ├── compose.deploy.yml
    └── remote-deploy-api.sh
```

### 왜 staging/production을 나누는가?

`dev`와 `master`가 같은 서버를 써도,
서로 다른 포트 / 서로 다른 volume / 서로 다른 compose project 로 분리해야 충돌이 없다.

예를 들어 내부 애플리케이션 포트는 아래처럼 둔다.
- staging API: `127.0.0.1:8081`
- production API: `127.0.0.1:8080`

그리고 외부 공개 포트는 같은 도메인에서 아래처럼 분리한다.
- production 외부 URL: `https://k14s209.p.ssafy.io:8989`
- staging 외부 URL: `https://k14s209.p.ssafy.io:8990`

EC2 보안그룹 정책상 외부에서 열 수 있는 포트는 **8000–9000 범위**로 제한되므로, 내부 바인딩 포트도 같은 범위 안에서 고른다.

즉, Nginx가 같은 도메인에서 포트별로 다른 내부 포트로 프록시하는 구조다.

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
API_HOST_PORT=8081
DB_NAME=postgres
DB_USERNAME=postgres
DB_PASSWORD=change-me
JWT_SECRET=change-me-to-a-long-random-string
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=1209600000
```

### production 예시
파일: `/opt/axwms/production/.env`

```dotenv
APP_ENV=production
API_HOST_PORT=8080
DB_NAME=postgres
DB_USERNAME=postgres
DB_PASSWORD=change-me
JWT_SECRET=change-me-to-a-long-random-string
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=1209600000
```

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
- `STAGING_URL` — 외부 공개 URL (예: `https://k14s209.p.ssafy.io:8990`)

### production 변수
- `DEPLOY_HOST_PRODUCTION` — 현재는 staging 과 같은 서버여도 된다
- `PRODUCTION_URL` — 외부 공개 URL (예: `https://k14s209.p.ssafy.io:8989`)

### Registry 자격증명 — ghcr.io 기준

이미지 레지스트리는 GitHub Container Registry(`ghcr.io`) 를 사용한다 (ADR-007).
이미지 네임스페이스는 `ghcr.io/axwms-s209/axwms-api` 로 고정한다.

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

### 10-3. 배포 job

- `deploy_dev`: `dev` push 시 staging 배포
- `deploy_prod`: `master` push 시 production 배포

현재는 **api 관련 변경이 있을 때만** 이미지 빌드와 배포가 일어나도록 `changes:`가 걸려 있다.

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

---

## 12. 실제 배포가 어떻게 진행되는가

### `dev` merge 후

1. GitLab이 `api` 이미지 빌드
2. Registry에 `api:dev`, `api:<sha>` push
3. CI가 staging 서버에 SSH 접속
4. `compose.deploy.yml`, `remote-deploy-api.sh`를 서버에 복사
5. 서버에서 `docker compose pull` / `up -d`

### `master` merge 후
같은 흐름으로 production 에 반영된다.

---

## 13. Nginx 연결 예시

현재는 별도 서브도메인을 추가하지 않고, **같은 도메인 + 다른 외부 포트**로 staging/production 을 분리하는 것이 가장 현실적이다.

### 13-1. 권장 포트 매핑

- production 외부 포트: `8989`
- staging 외부 포트: `8990`
- production 내부 API 포트: `8080`
- staging 내부 API 포트: `8081`

> 참고: EC2 보안그룹이 **8000–9000 범위만 허용**하므로 내부 바인딩도 이 범위 안으로 고정한다.

즉, 브라우저 기준으로는 아래처럼 접근한다.

- production: `https://k14s209.p.ssafy.io:8989`
- staging: `https://k14s209.p.ssafy.io:8990`

### 13-2. Nginx 예시 설정

```nginx
upstream axwms_api_prod {
    server 127.0.0.1:8080;
}

upstream axwms_api_staging {
    server 127.0.0.1:8081;
}

server {
    listen 8989 ssl http2;
    server_name k14s209.p.ssafy.io;

    ssl_certificate     /etc/letsencrypt/live/k14s209.p.ssafy.io/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/k14s209.p.ssafy.io/privkey.pem;

    location / {
        proxy_pass http://axwms_api_prod;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen 8990 ssl http2;
    server_name k14s209.p.ssafy.io;

    ssl_certificate     /etc/letsencrypt/live/k14s209.p.ssafy.io/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/k14s209.p.ssafy.io/privkey.pem;

    location / {
        proxy_pass http://axwms_api_staging;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 13-3. 꼭 같이 확인할 것

1. AWS Security Group 에 `8989`, `8990` 인바운드가 열려 있는지
2. 서버 내부 방화벽(`ufw`)이 있다면 `8990`도 허용했는지
3. staging `.env`의 `API_HOST_PORT=8081`, production `.env`의 `API_HOST_PORT=8080` 이 실제 compose 와 맞는지 (둘 다 8000–9000 범위인지 재확인)
4. Nginx reload 전에 `sudo nginx -t` 로 설정 검증을 했는지

---

## 14. 처음 적용할 때 추천 순서

1. EC2에 Docker 설치
2. `/opt/axwms/staging`, `/opt/axwms/production` 생성
3. GitLab Runner 설치 및 프로젝트 register (privileged 포함)
4. 각 환경 `.env` 작성 (600, 배포 계정 소유)
5. CI 전용 SSH 키페어 생성 및 EC2 `authorized_keys` 등록
6. GitLab Variables 등록 + `master` / `dev` Protected Branch 설정
7. Nginx가 staging/prod 포트를 바라보도록 확인
8. 작업 브랜치 -> `dev` MR 생성
9. merge 후 staging 배포 로그 확인
10. staging 확인 후 `master` 반영

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

---

## 16. 현재 의도적으로 하지 않은 것

이번 단계에서 일부러 제외한 것:

- 과거 임시 검증 절차 재사용
- EC2에서 소스 직접 빌드
- web/ai 운영 자동배포
- 전체 스택 단일 compose 통합
- 인증서 자동 발급/갱신 자동화

이유는 지금은 **배포 경로를 단순하게 만들고, api 자동배포를 먼저 안정화하는 것**이 우선이기 때문이다.

---

## 17. 다음 단계 후보

api 자동배포가 안정화되면 다음 순서로 확장하면 된다.

1. `deploy_prod`를 manual 승인형으로 변경할지 결정
2. web Dockerfile 추가
3. ai Dockerfile 추가
4. web/api/ai 전체 운영 compose 정리
5. Nginx 설정 파일도 저장소 기준으로 통합 관리

---

## 18. 한 줄 요약

현재 기준의 정답은 아래다.

- **작업 브랜치에서는 CI만**
- **dev/master merge 결과에서만 CD**
- **EC2는 빌드 서버가 아니라 실행 서버**
- **배포 source of truth는 `infra/compose.deploy.yml`와 `.gitlab-ci.yml`**
