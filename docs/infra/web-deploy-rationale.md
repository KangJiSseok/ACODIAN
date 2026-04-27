# web 자동배포 편입 — 결정 근거와 코드 리딩 가이드

- 기준 ADR: [`docs/infra/adr.yaml`](adr.yaml) ADR-014
- 컨벤션: [`docs/infra/code-convention.yaml`](code-convention.yaml) DO-003 / DO-004 / OPS-016
- 운영 가이드: [`docs/infra/ec2-gitlab-cicd-guide.md`](ec2-gitlab-cicd-guide.md) 섹션 4 · 6 · 8 · 10 · 11 · 12 · 14 · 16 · 17
- pending: [`docs/infra/pending-decisions.md`](pending-decisions.md) #3 (Nginx conf 저장소 편입), #5 (web 처리됨 / ai 잔존)
- 본 문서의 위치: ADR-014 의 `decision` 본문은 "무엇을 결정했나" 만 적고, **"왜 이 방식이고 어떤 대안을 어떤 이유로 기각했나"** 의 SSOT 는 본 문서다. ADR template 무결성을 유지하기 위한 분리.

---

## 0. 한 줄 요약

ADR-001 의 1차 자동배포 범위(api/postgres/redis) 는 **api 만 컨테이너 자산을 가지고 있었기** 때문에 좁혀진 것이고, ADR-010 의 web 포트 합의(8000) 는 **운영 자산이 없는 상태에서 미리 정한 네이밍 약속**이었다. 본 변경은 그 둘 사이의 빈자리(`web/Dockerfile`, compose web 서비스, `web_image` job) 를 채워 web 을 같은 자동배포 흐름으로 굴린다. 결정 11개의 비교 검토는 아래에 풀어둔다.

---

## 1. 코드 리딩 순서

처음 보는 사람은 아래 순서대로 따라가면 결정 사이의 인과가 한눈에 들어온다.

1. **시작점**: [`docs/infra/adr.yaml`](adr.yaml) ADR-014 의 `decision` — 변경의 헤더
2. **컨테이너 이미지가 어떻게 만들어지는가**: [`web/next.config.ts`](../../web/next.config.ts) → [`web/Dockerfile`](../../web/Dockerfile) → [`.dockerignore`](../../.dockerignore)
3. **컨테이너가 어떻게 떠 있는가**: [`infra/compose.deploy.yml`](../../infra/compose.deploy.yml) 의 `web` 서비스
4. **EC2 가 어떻게 새 이미지를 받아 갈아끼우는가**: [`infra/scripts/remote-deploy.sh`](../../infra/scripts/remote-deploy.sh)
5. **CI 가 언제 무엇을 하는가**: [`.gitlab-ci.yml`](../../.gitlab-ci.yml) 의 anchor(`.web_changes`/`.web_deploy_changes`/`.deploy_changes`) 와 `web_image` job, deploy job 의 `WEB_IMAGE` 주입
6. **운영자가 EC2 에서 무엇을 추가해야 하는가**: [`docs/infra/ec2-gitlab-cicd-guide.md`](ec2-gitlab-cicd-guide.md) 섹션 8 의 `WEB_HOST_PORT`
7. **다음 결정 트리거**: [`docs/infra/pending-decisions.md`](pending-decisions.md) #3 / #5

읽다가 막히면 본 문서로 다시 돌아와 해당 절(2.1, 2.2 …)을 확인한다.

---

## 2. 결정별 비교 검토

각 절은 **선택안 → 비교한 대안 → 기각 사유 → 결정 후 부담 + 완화** 순서다.

### 2.1 빌드 컨텍스트 — 모노레포 루트 vs `web/` 단독

**선택**: `docker build -f web/Dockerfile .` (컨텍스트 = 모노레포 루트)

**대안 A: `docker build web/`** (컨텍스트 = web/ 만)
- 장점: 컨텍스트가 작아 전송 속도 빠름. api/Dockerfile 과 동형.
- 기각 이유: `pnpm install --frozen-lockfile` 은 lockfile (`pnpm-lock.yaml`) 과 workspace 매니페스트 (`pnpm-workspace.yaml`) 가 함께 있어야만 무결성 확인이 가능하다. 둘 다 모노레포 루트에 있다. web/ 만 컨텍스트로 잡으면 `web/package.json` 만 보고 npm-style hoist 추측 install 을 하게 되어 lockfile 보장이 깨진다 — 같은 PR 의 패키지 버전 고정 효과가 사라진다.

**대안 B: web 만 분리한 별도 저장소로 이동**
- 장점: 컨텍스트도 작고 lockfile 도 자기 안에 둠.
- 기각 이유: AX-WMS 모노레포 구조 변경은 본 ADR 범위를 한참 넘는다(저장소 정책·CI 정책·브랜치 정책 모두 변동).

**부담 + 완화**
- 컨텍스트가 모노레포 루트라 `.gitlab-ci.yml` 로 컨텍스트 송신량이 늘 수 있다.
- 완화: 루트 [`.dockerignore`](../../.dockerignore) 신설 — `node_modules`, `.next`, `api/.gradle`, `docs/`, `runbooks/`, `.claude/`, `.omc/` 등 빌드와 무관한 디렉터리를 제외해 컨텍스트가 사실상 web/ + manifest 3개 + 약간의 워크스페이스 메타로 압축된다.
- DO-004 로 컨벤션화: 루트 `.dockerignore` 와 `api/.dockerignore` 는 SSOT 가 분리됨(api 빌드는 컨텍스트가 `api/` 이므로 영향 없음).

---

### 2.2 Dockerfile 단계 — multi-stage vs 단일 stage

**선택**: `deps` → `builder` → `runner` 3-stage

**대안 A: 단일 stage (`node:22-alpine` 한 장에서 install + build + run)**
- 기각 이유: 최종 이미지에 `pnpm`, `corepack`, build 캐시, dev 의존성, 소스 트리가 통째로 남아 이미지가 비대해지고 공격 표면도 넓어진다. Next.js 의 standalone 출력을 쓰지 않는 한 의미가 없다.

**대안 B: 2-stage (`builder` + `runner`)**
- 검토: `deps` 와 `builder` 를 합치는 안. 빌드 시간은 비슷하지만, `pnpm-lock.yaml` 만 바뀌었을 때도 `pnpm install` + `pnpm build` 가 한 layer 로 묶여 캐시 무효화가 동시에 일어남.
- 기각 이유: 향후 BuildKit `--cache-from` 도입 시 `deps` 를 별 stage 로 분리해 두는 편이 캐시 활용이 좋다. 추가 layer 부담은 무시할 수준.

**선택안 구조 (Dockerfile 라인 매핑)**
- `FROM ... AS deps`: pnpm 활성화, 루트 manifest 3개 + web 의 manifest 1개만 복사 후 `pnpm install --frozen-lockfile`. 의존성만 결정.
- `FROM ... AS builder`: `deps` 의 `node_modules` 복사 후 web 소스 추가, `pnpm --filter ./web build`.
- `FROM ... AS runner`: standalone 산출물 + `.next/static` 만 복사. non-root user(`app:app`), `WORKDIR /app/web`, `CMD ["node", "server.js"]`.

---

### 2.3 Next.js standalone output vs 통째 복사

**선택**: `output: "standalone"` 을 켜고, 그 산출물(`.next/standalone/`) 만 runner 이미지에 복사

**대안 A: standalone 끄고 `pnpm start` 또는 `next start` 그대로 실행**
- 기각 이유: runner 이미지에 `node_modules` 전체와 `.next` 전체를 복사해야 한다. dev 의존성은 prune 가능하지만 여전히 무거움. 또한 `pnpm` 자체를 runner 이미지에 둬야 해 surface 가 늘어남.

**대안 B: standalone 켜고도 `next start` 사용**
- 기각 이유: `next start` 는 standalone 산출물의 server.js 와 다르게 동작한다(런타임 의존성 해석 경로 다름). 둘 중 하나로 일관해야 디버깅이 쉬움. standalone 의 server.js 는 hoisted node_modules 를 정확히 트레이스해 두므로 runner 에 넣기 가장 효율적.

**왜 이게 모노레포에서 동작하는가** — 검증된 사실
- 빌드 시 standalone 이 `<workspace-root>/web/.next/standalone/` 안에 `web/server.js`, `web/.next/...`, `node_modules/...` 를 함께 복사한다. node 가 `require` 해석 시 `web/node_modules → ../node_modules` 순으로 올라가므로 hoisted 의존성이 런타임에 실제 발견된다.
- 본 작업 시 로컬에서 `pnpm install --frozen-lockfile` (4분 17초) + `pnpm build:web` 으로 standalone 산출 구조 (`web/.next/standalone/web/server.js` + `node_modules/`) 를 직접 확인했다.

---

### 2.4 `outputFileTracingRoot` — 모노레포 루트 vs web 자기 자신

**선택**: `outputFileTracingRoot: path.join(__dirname, "..")` (모노레포 루트)

**대안 A: 명시 없음 (Next.js 자동 감지)**
- 기각 이유: Next.js 는 모노레포 감지 시 경고와 함께 추정 root 를 잡는다. 추정이 틀리면 standalone 산출에 hoisted 의존성이 빠져 런타임 `Cannot find module` 가 발생. 명시가 가장 안전.

**대안 B: `path.join(__dirname)` (web 자기 자신)**
- 기각 이유: pnpm hoist 가 root 의 `node_modules/` 에 의존성을 둘 수 있는데, web/ 만 root 로 잡으면 그쪽이 standalone 산출에서 누락된다. 결과는 A 와 동일한 런타임 모듈 누락.

**부담 + 완화**
- root 를 모노레포로 잡으면 standalone 산출 구조가 `web/.next/standalone/web/server.js` 가 되어 Dockerfile 의 COPY 와 WORKDIR 이 `/app/web` 형태가 되어야 함.
- 완화: Dockerfile 본문에 명시적으로 `WORKDIR /app` → `COPY --from=builder ... ./` → `WORKDIR /app/web` 순서로 작성해 시각적으로 한눈에 들어오게 했다.

---

### 2.5 `package.json` 의 `start` 를 `next start -p 8000` 으로

**선택**: `next start -p 8000`

**대안 A: 그대로 `next start` 두기 (기본 3000)**
- 기각 이유: 컨테이너는 `node server.js` (PORT=8000) 로 동작하고, 로컬 `pnpm start` 는 3000 으로 동작 — 환경 차이가 디버깅 비용을 만든다. 특히 nginx 가 8000 을 가정한 채 라우팅하므로 로컬에서도 동일 포트 가정이 일관됨.

**대안 B: Dockerfile CMD 에서만 PORT 강제, package.json 은 그대로**
- 기각 이유: 로컬에서 `pnpm start` 와 컨테이너의 동작 포트가 분기됨. 새 팀원이 같은 포트로 검증하지 못해 "내 로컬에선 됐는데" 사고 위험.

**참고**: standalone 의 `server.js` 는 `next start` 가 아니라 자체 listener 다. 거기는 `PORT` / `HOSTNAME` 환경변수만 본다. 그래서 Dockerfile 의 `ENV PORT=8000 HOSTNAME=0.0.0.0` 이 standalone server.js 를 8000 으로 묶고, package.json 의 `-p 8000` 은 로컬 `next start` 를 8000 으로 묶는 별개 설정이다.

---

### 2.6 compose 의 web 서비스에 `depends_on` 없는 이유

**선택**: web 은 postgres/redis/api 어느 것에도 `depends_on` 두지 않음

**대안 A: web 이 api 에 depends_on (api 가 healthy 되어야 web 시작)**
- 기각 이유: web 이 떠 있어도 정적 자산(JS bundle, HTML shell, 이미지) 은 서빙 가능. api 가 잠깐 죽었다고 web 까지 내리면 사용자에게 보이는 화면이 사라져 운영 가시성이 떨어진다. API 호출 실패는 web 단의 에러 처리 책임으로 둔다.

**대안 B: web 이 redis 에 depends_on (캐시용)**
- 기각 이유: 본 시점 web 은 redis 를 직접 쓰지 않는다. 미래에 BFF 로 redis 를 쓰게 되면 그때 의존성 추가.

**완화**: api/redis 가 미준비 상태에서 web 만 떠 있는 동안 발생하는 fetch 실패는 web 측 클라이언트 코드의 표준 에러 처리(timeout / retry / 토스트) 로 흡수. ADR-014 본문에 명시.

---

### 2.7 배포 스크립트 — 영역별 분리 vs 단일 일반화

**선택**: `infra/scripts/remote-deploy-api.sh` → `infra/scripts/remote-deploy.sh` 로 이름 변경 + 한 스크립트가 api/web 을 모두 처리

**대안 A: `remote-deploy-api.sh` + 신규 `remote-deploy-web.sh` 분리**
- 기각 이유: deploy job 이 두 스크립트를 따로 SSH 호출해야 함. 두 호출 사이에 잔존 컨테이너 정리/포트 점유 같은 동기화 이슈가 끼어들 수 있다. OPS-016 의 멱등 흐름은 한 번의 `compose up -d --remove-orphans` 로 보장되는데, 분리하면 그 보장이 깨진다.
- 정량 부담: 영역이 추가될 때마다 deploy job 이 N+1 번 SSH 호출. ai 차례에는 또 늘어남.

**대안 B: 스크립트는 단일 유지, 이름은 `remote-deploy-api.sh` 그대로**
- 기각 이유: 이름이 본문과 거짓말이 된다. 새로 본 사람이 "왜 api 스크립트가 web 도 다루지?" 라고 묻게 됨. 이름이 변경 비용보다 작다.

**구현 핵심 (`remote-deploy.sh`)**
- `require_var APP_ENV / DEPLOY_ROOT / API_IMAGE / WEB_IMAGE / GHCR_USER / GHCR_TOKEN`: 누락 시 즉시 fail-fast.
- 멱등 흐름: `compose ps` (pre-스냅샷) → `pull api web` → `rm -sf api web` → `up -d --remove-orphans postgres redis api web` → `compose ps` (post-스냅샷).
- ai 차례에는 require_var + pull/rm/up 인자에 ai 만 일괄 추가. 프레임은 그대로.

---

### 2.8 deploy 트리거 — 합집합(`.deploy_changes`) vs 영역별 분리

**선택**: `deploy_dev`/`deploy_prod` 의 `changes` 를 합집합 anchor 로 둠

**대안 A: `deploy_web` / `deploy_api` 별 job 분리**
- 기각 이유: EC2 한 대에 staging/production 이 공존하고 한 번의 SSH 세션으로 멱등 배포가 끝나는 구조다. job 분리는 SSH 비용을 늘리고 배포 도중 부분 실패 시 상태 추적이 더 어려움. 또 `compose up -d --remove-orphans` 는 본질적으로 "프로젝트 전체" 를 한 번에 처리하는 명령이라 영역별 호출이 자연스럽지 않다.

**대안 B: 정확히 변경된 영역만 트리거 (web 코드 변경 시 deploy 안 돌게)**
- 기각 이유: 인프라(compose / 스크립트 / `.gitlab-ci.yml`) 만 바뀌어도 EC2 의 compose 파일 동기화를 위해 deploy 가 돌아야 함. 합집합이 가장 단순하면서 안전한 트리거.
- 부담: web/api 코드 무변경 + 인프라만 바뀐 경우에도 deploy 가 돈다. 이 경우 `:<ref-slug>` 태그 이미지를 그대로 pull → 이미지 변동 없으니 컨테이너 recreate 만 일어남(허용 범위). pending-decisions #2 에서 후행 정리 예정.

**보장 메커니즘**: `.web_deploy_changes` 가 인프라 변경도 포함하므로, 본 MR 처럼 "web 자산 신규 + 인프라 변경" 이 함께 들어온 첫 푸시에서 web_image 가 한 번은 돌아 `:dev` 태그가 ghcr 에 생긴다. 이후 deploy 가 그 태그를 pull 가능.

---

### 2.9 ghcr 네임스페이스 — `axwms-web` 별 레포 vs `axwms` 단일 레포 + 태그

**선택**: `ghcr.io/axwms-s209/axwms-web` (api 와 별 레포)

**대안 A: `ghcr.io/axwms-s209/axwms` 단일 레포 + 태그로 영역 구분 (`axwms:web-dev`, `axwms:api-dev`)**
- 기각 이유: 태그 namespace 가 영역 × 환경 × 커밋의 곱집합으로 폭증. 정리·폐기 정책을 영역별로 다르게 두기 어렵고, ghcr UI 에서 영역 구분이 시각적으로 사라짐.

**대안 B: 영역별 organization (`axwms-web-s209`, `axwms-api-s209`)**
- 기각 이유: ADR-007 이 organization `axwms-s209` 단일로 묶는 것을 결정한 직접적 사유(자격증명 단일화) 가 깨짐. PAT 도 영역별로 늘어남.

**부담**: ghcr 의 web 레포 자체는 처음 push 때 자동 생성되므로 별도 작업 없음. 단, ADR-007 의 organization 변경 시 `WEB_IMAGE_BASE` 도 동반 갱신해야 함 — ADR-014 의 재검토 트리거 항목에 명시.

---

### 2.10 `WEB_IMAGE` 가 EC2 `.env` 에 안 들어가는 이유

**선택**: CI 의 deploy job 이 SSH 호출 시점에 `WEB_IMAGE='ghcr.io/...:<ref-slug>'` 를 환경변수로 주입. EC2 `.env` 에는 두지 않음.

**대안: EC2 `.env` 에 `WEB_IMAGE=ghcr.io/...:dev` 정적 기재**
- 기각 이유 1: ADR-003 의 "환경 특화 값과 비밀값은 서버 `.env` 또는 GitLab Variables" 원칙은 맞지만, `WEB_IMAGE` 는 환경 특화 값이 아니라 **현재 배포 중인 커밋과 1:1** 인 값이다. 정적 기재는 "지금 떠 있는 컨테이너가 어떤 커밋이냐" 를 .env 와 ghcr 사이에서 drift 시킨다.
- 기각 이유 2: 운영자가 매 배포마다 `.env` 를 재작성하는 운영 부담. 자동 재작성하면 그게 곧 SSH 환경변수 주입과 같은 흐름.

**대신 `WEB_HOST_PORT` 는 `.env` 에 둔다** — 그건 환경 특화 값(production 8000 / staging 8001) 이고 커밋과 무관하기 때문. ADR-003/009 의 원칙과 정합.

---

### 2.11 Nginx conf 동시 변경 안 한 이유 (path-based 라우팅)

**선택**: 라우팅 규칙(/, /api, /ai)은 ADR-010 + ec2-gitlab-cicd-guide.md 섹션 13 에 골격으로 두고, **저장소 conf 편입과 reload 자동화는 후속 MR**로 분리.

**대안: 본 MR 에 nginx conf 도 함께 커밋**
- 기각 이유 1: nginx conf 가 처음 저장소에 들어오는 변화는 deploy job 의 `nginx -t && systemctl reload nginx` 추가, conf 배포 경로 결정, EC2 `/etc/nginx/sites-available/axwms.conf` 와의 정합성 확인까지 동반된다. 본 MR 의 검증 표면을 한 겹 더 늘림.
- 기각 이유 2: pending-decisions #3 의 트리거(인증서 발급 완료 + conf 안정화) 는 이미 충족 — 다음 MR 의 자연스러운 선임이 본 MR 이다. 지금 묶으면 두 MR 이 한 덩어리가 되어 롤백 단위가 커짐.

**현재 상태가 어떻게 되는가** — 사용자 가시성
- web 컨테이너는 본 MR 후 EC2 에 떠 있음(pending-decisions #5 종결 기준).
- 그러나 nginx 가 web upstream 으로 라우팅하지 않으므로 외부에서 `https://k14s209.p.ssafy.io/` 는 여전히 api 또는 404 응답. 본 MR 으로 외부 접근이 새로 열리지 않는다.
- pending-decisions #3 후속 MR 에서 nginx conf 의 `location /` 추가 + reload → 그때 web 이 외부에 처음 보임.

---

### 2.12 점진(web 먼저) vs 일괄(web + ai 한 번에)

**선택**: 점진 — 본 MR 은 web 만, ai 는 후속 ADR 로 분리

**대안: 한 MR 으로 web + ai 동시 편입**
- 기각 이유 1: ai 의 컨테이너화는 결정 4번(Celery worker 컨테이너 동반 여부) 의 사전 확인이 필요했고, 본 시점 ai 코드에 Celery 사용이 없다는 것을 직접 확인했다(`ai/app/` grep, `app/task/` 빈 디렉터리). 그러나 이 확인 자체가 별 검토 단계라 web 결정의 검증을 늦춤.
- 기각 이유 2: dev push pipeline 이 깨졌을 때 web 인지 ai 인지 분리되어 디버깅이 쉬움. 메모리 숙제(2026-04-24 후속 세션) 의 "구세대 컨테이너 잔존 사고" 같은 운영 시점 위험은 영역이 한 번에 둘이면 두 배.
- 기각 이유 3: pending-decisions #5 의 본문이 web/ai 를 의도적으로 묶어두지 않았다(둘은 독립 트리거).

**부담**: ai 차례에 다시 한 번 ADR + DO + scripts/.gitlab-ci.yml 패치가 필요. 다만 본 MR 의 패턴(스크립트 일반화, web_deploy_changes anchor) 이 그대로 ai 에 적용 가능 — 절은 2.7/2.8 의 "ai 차례에는 인자 한 줄 추가" 항목 참고.

---

## 3. 운영자 측에서 해야 하는 일 (코드 변경 아님)

본 MR 은 **저장소 자산만** 바꾼다. 외부에서 web 이 보이려면 운영자가 EC2 에서 다음을 한 번 수행해야 한다.

1. **`/opt/axwms/staging/.env`** 에 `WEB_HOST_PORT=8001` 추가
2. **`/opt/axwms/production/.env`** 에 `WEB_HOST_PORT=8000` 추가
3. master MR 직전 production 잔존 점검: `sudo docker ps -a | grep production-` (메모리 숙제 — 2026-04-24 사고 재발 방지)
4. 후속 MR(pending-decisions #3) 로 nginx conf 의 `location /` 블록 추가 + `nginx -t` + `systemctl reload nginx`

---

## 4. 검증 증거 (본 MR 시점)

- **YAML 문법**: `.gitlab-ci.yml`, `infra/compose.deploy.yml`, `docs/infra/adr.yaml`, `docs/infra/code-convention.yaml` 모두 `python3 -c "import yaml; yaml.safe_load(open(...))"` 통과
- **`code-convention.yaml` 구조**: 4개 top-level 섹션(`general`, `docker`, `compose`, `ops`), `docker` = `[DO-001, DO-002, DO-003, DO-004]`
- **`adr.yaml` 순서**: `[ADR-001 … ADR-014]` 시간/id 순서 정합
- **shell 문법**: `sh -n infra/scripts/remote-deploy.sh` 통과
- **Next.js 로컬 빌드**: `pnpm install --frozen-lockfile` (4분 17초) + `pnpm build:web` 로 16/16 정적 페이지 생성, standalone 산출물 `web/.next/standalone/web/server.js` + `web/.next/standalone/node_modules/` 직접 확인
- **docker build**: WSL distro 에 docker 미설치라 로컬 검증 불가. CI 의 `web_image` job 이 처음 push 시점에 실제 빌드를 수행하며, 빌드 컨텍스트(루트) + Dockerfile 경로(`-f web/Dockerfile`) 는 `.gitlab-ci.yml` 에 명시되어 있다.

---

## 5. 다음 작업자에게 — ai 차례에 무엇이 동형인가

ai 자동배포 편입(잠정 ADR-015) 시점에 **본 MR 의 패턴이 그대로 복제**되는 자리:

| 영역 | web 시점 (본 MR) | ai 시점 (후속) |
|---|---|---|
| Dockerfile | `web/Dockerfile` (multi-stage, monorepo root context) | `ai/Dockerfile` (단일 stage, ai/ 컨텍스트, `python:3.12-slim` + uvicorn) |
| 내부 listen | 8000 | 8200 |
| 호스트 포트 | prod 8000 / staging 8001 | prod 8200 / staging 8201 |
| compose 서비스 | `web` (depends_on 없음) | `ai` (postgres healthy 의존 여부 사용자 결정) |
| 이미지 base | `ghcr.io/axwms-s209/axwms-web` | `ghcr.io/axwms-s209/axwms-ai` |
| 스크립트 | `pull api web` / `rm -sf api web` / `up ... api web` | `pull api web ai` / `rm -sf api web ai` / `up ... api web ai` |
| CI anchor | `.web_changes`, `.web_deploy_changes`, deploy_changes 합집합 | `.ai_changes` 의 deploy_change 확장 + `ai_image` job 신규 |
| `.env` | `WEB_HOST_PORT` | `AI_HOST_PORT` |
| 운영 가이드 | 섹션 8/11/14 갱신 | 동일 절 추가 |
| pending-decisions | #5 web 종결 | #5 ai 종결 |

ai 시점에 별도로 결정해야 하는 것:
- **Celery worker 컨테이너 동반 여부** — 본 시점에는 ai 코드에 Celery 사용 0건이라 worker 없이 FastAPI 단일 컨테이너로 시작 가능. ai 코드에 Celery 가 도입되는 시점에 `ai-worker` 서비스 추가 결정.
- **ai 의 redis/postgres `depends_on`** — ai 가 부팅 시 두 의존성에 즉시 붙는지에 따라.
