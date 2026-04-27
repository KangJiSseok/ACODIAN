# Pending Decisions (infra)

이 문서는 2026-04-23 ~ 2026-04-24 세션에서 합의됐으나 아직 실행되지 않은 인프라 관련 사항을 기록한다.
다음 세션이 문서만 읽고 곧바로 재개할 수 있도록, 각 항목은 **맥락 / 대안 / 유예 이유 / 다음 트리거** 네 축으로 정리한다.

인덱스:
1. jOOQ codegen bootstrap 의 DinD 호환성
2. 파이프라인 `changes` 세분화 (api_code / infra_runtime / infra_docs)
3. Nginx conf 저장소 편입
4. Let's Encrypt 인증서 실제 발급 (EC2 서버 작업)
5. web / ai 내부 listen 포트 실제 반영
6. DB 외부 개방의 IP 화이트리스트 및 비정상 접속 모니터링
7. TLS 인증서 만료 전 알림 체계

---

## 1. jOOQ codegen bootstrap 의 DinD 호환성

### 맥락
- dev push pipeline 의 `api_ci` job 에서 `:bootstrapJooqCodegenDatabase` 가 `localhost:<random-port>` 로 pgvector 컨테이너에 붙으려다 `Connection refused` 로 120초 백오프 9회 반복 실패가 재현됐다.
- 근본 원인은 `api/gradle/jooq-codegen.gradle` 이 jdbcUrl 의 host 를 `localhost` 로 하드코딩한 것. DinD 환경에서는 `DOCKER_HOST=tcp://docker:2375` 가 가리키는 daemon (docker:dind service 컨테이너) 위에 pgvector 컨테이너가 뜨므로, builder 의 localhost 로는 도달 불가.
- 이전 작업 브랜치(`chore/infra/ec2-gitlab-cicd`) 에서는 녹색 통과 기록이 있으나 `api/build.gradle` / `jooq-codegen.gradle` / `libs.versions.toml` / `.gitlab-ci.yml` 은 그 시점 이후 관련 한 줄도 변경되지 않았다. 즉 코드 문제가 아니라 Runner/DinD 네트워크 구성 변화로 보인다(예: `config.toml` 의 `network_mode = "host"` 가 빠진 상태).

### 대안
1. **`DOCKER_HOST` 파싱으로 host 결정** — `jooq-codegen.gradle` 의 jdbcUrl 을 `tcp://<host>:<port>` 환경변수에서 hostname 추출해 주입. 로컬은 `localhost` 폴백 유지.
2. Runner `config.toml` 에 `network_mode = "host"` 복구 — 코드 무변경으로 과거 동작 재현 가능하지만 근본적으로 환경에 의존한 코드라는 문제는 남는다.
3. `docs/api/jooq-codegen-policy.md` 를 포기하고 generated source 를 커밋 (ADR-008 의 후속 선택지 3번) — 범위가 매우 크다.

### 유예 이유
- 해결책 1이 기술적으로 명확하고 위험이 작지만, 이 모듈을 설계한 팀원과 codegen 정책 소유권을 공유하고 있어 **팀원 합의가 선행되어야 한다**.
- 로컬 브랜치 `fix/api/jooq-codegen-dind-host` 에 제안 패치 커밋(`ad63a51`) 이 이미 존재하지만 push 하지 않는다. 합의되면 그 커밋을 재활용한다.

### 다음 트리거
- jOOQ 관련 팀원과 합의가 끝났을 때.
- 합의 전에도 dev push pipeline 이 계속 깨지는 리스크가 있다. 포트/SSL 재설계 MR 이 merge 되면 `infra/` 및 `.gitlab-ci.yml` 변경으로 api_ci 가 재실행되어 실패할 것이다. **이 실패를 감수한 상태로 merge 가 진행 중임을 팀이 인지해야 한다.**

---

## 2. 파이프라인 `changes` 세분화 (api_code / infra_runtime / infra_docs)

### 맥락
- 현재 `.api_deploy_changes` 는 `api/**`, `infra/**`, `docs/infra/**`, `.gitlab-ci.yml` 을 한 덩어리로 묶는다. 이 때문에 문서만 바뀐 MR 이 dev 에 merge 되어도 api_ci → api_image → deploy_dev 전체가 돈다.
- 리뷰어 지적: "인프라 문서/설정만 수정했을 때도 1시간짜리 Gradle 빌드와 배포가 실행되는 건 과도."
- 현재는 "첫 end-to-end 배포 통과" 를 우선해 과보수 설정을 유지 중.

### 대안 (세분화 설계 초안)
- `.api_code_changes = [api/**]` → `api_ci`, `api_image`
- `.infra_runtime_changes = [infra/**, .gitlab-ci.yml (except docs)]` → `deploy_dev` / `deploy_prod`
- `docs/infra/**` 는 **어떤 trigger 에도 포함하지 않음**
- `api_image.needs: api_ci` 에 `optional: true` 를 달아 infra-only 변경 시 기존 `:<CI_COMMIT_REF_SLUG>` 태그 이미지로 재배포 가능하게 한다.

### 유예 이유
- 첫 dev push pipeline 이 아직 한 번도 녹색으로 끝나지 않았다. 이 시점에 CI 규칙 재설계까지 얹으면 실패 원인 분리가 어려워진다.
- 이번 포트/SSL 재설계 MR 과도 범위가 다르므로 별개 MR 로 분리한다.

### 다음 트리거
- dev push pipeline 이 최초 1회 정상 녹색으로 완주한 뒤.
- 또는 문서-only PR 이 실제로 자주 merge 되기 시작해 Runner 시간 낭비가 누적되는 시점.

---

## 3. Nginx conf 저장소 편입

### 맥락
- ADR-010 으로 Nginx 를 EC2 systemd 로 전면에 두는 구조가 확정됐으나, 현재 저장소에는 `nginx/` 디렉터리도 `*.conf` 파일도 없다.
- `docs/infra/ec2-gitlab-cicd-guide.md` 섹션 13 에 conf 골격만 기록되어 있다.
- 가이드 섹션 17 의 후속 과제 5번 "Nginx 설정 파일도 저장소 기준으로 통합 관리" 가 이 항목이다.

### 대안
1. **EC2 의 `/etc/nginx/sites-available/axwms.conf` 를 1:1 로 커밋** — 변경이 있을 때 git 기준으로 검토, scp 또는 deploy job 으로 배포.
2. 템플릿화 — `axwms.conf.tpl` 에 환경 변수 자리만 두고 배포 스크립트에서 렌더.
3. Ansible / 간단한 rsync 배포 스크립트 추가.

### 유예 이유
- Nginx conf 초기 세팅은 **EC2 서버 작업과 반드시 동기화** 되어야 한다. 저장소에 커밋된 conf 가 서버에 반영되지 않으면 drift 가 즉시 발생한다.
- 서버 측 작업 절차와 자동 배포 경로를 먼저 설계하고 나서 편입하는 편이 안전.

### 다음 트리거
- Let's Encrypt 초기 발급(항목 4) 완료 후, `/etc/nginx/sites-available/axwms.conf` 가 실제로 안정적인 상태가 된 시점.
- 이후 `nginx/` 디렉터리를 신규 브랜치에서 만들고, 배포 경로(예: `deploy_dev` / `deploy_prod` job 에 nginx conf scp + `sudo nginx -t && systemctl reload nginx`) 를 추가한다.

---

## 4. Let's Encrypt 인증서 실제 발급 (EC2 서버 작업)

### 맥락
- ADR-012 로 인증서 출처는 Let's Encrypt + certbot 으로 확정됐다.
- 그러나 실제 초기 발급은 EC2 에서 `sudo certbot --nginx -d k14s209.p.ssafy.io` 를 한 번 실행해야 완료된다 — 저장소 커밋만으로는 못 한다.

### 유예 이유
- 서버 측 1회성 작업이라 코드 변경 MR 범위 밖.
- 선행 조건:
  - Nginx 가 EC2 에 설치되고 `/etc/nginx/sites-available/axwms.conf` 가 `server_name k14s209.p.ssafy.io;` 를 포함
  - 80 포트가 SSAFY 보안그룹 + `ufw` 에서 열려 있음
  - `k14s209.p.ssafy.io` 가 이 EC2 퍼블릭 IP 로 DNS 매핑되어 있음

### 다음 트리거
- 위 선행 조건 세 가지가 모두 충족된 시점에 서버에서 certbot 실행.
- 실행 후 `/etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh` 를 생성해 자동 갱신 후 Nginx reload 가 돌도록 한다 (가이드 섹션 13-3).

---

## 5. web / ai 내부 listen 포트 실제 반영

### 맥락
- ADR-010 으로 web(Next.js) 은 8000, ai(FastAPI) 는 8200 을 컨테이너 내부 listen 포트로 쓰기로 결정.
- ADR-001 의 자동배포 범위는 1차 시점에 "api + postgres + redis" 로 한정되어 있었다.

### 진행 현황
- **web — 처리됨 (2026-04-27, ADR-014)**
  - `web/next.config.ts` 에 `output: "standalone"` + `outputFileTracingRoot` 추가, `web/package.json` 의 `start` 를 `next start -p 8000` 으로 갱신, `web/Dockerfile` 신규(monorepo 루트 컨텍스트, multi-stage, 8000 EXPOSE), `infra/compose.deploy.yml` 에 web 서비스 추가(production `127.0.0.1:8000:8000` / staging `127.0.0.1:8001:8000`).
  - `infra/scripts/remote-deploy-api.sh` 를 `remote-deploy.sh` 로 일반화하고 `WEB_IMAGE` `require_var`, `.gitlab-ci.yml` 에 `web_image` job 신규.
  - 운영 가이드(섹션 4/6/8/10/11/12/14/16/17) 와 code-convention(DO-003/DO-004, OPS-016) 동반 갱신.
- **ai — 잔존**
  - 컨테이너 자산 0(`ai/Dockerfile` 없음), `ai/main.py` 에서 uvicorn 실행 포트 미지정.
  - `ai/requirements.txt` 에 celery 의존성이 박혀 있으나 `ai/app/` 안에 `@task`/`Celery(...)` 사용 0건이라 본 시점엔 worker 컨테이너 없이 FastAPI 단독으로 1차 편입 가능.

### 다음 트리거
- ai 차례:
  - `ai/Dockerfile` 신규(python:3.12-slim 단일 stage, `pip install -r requirements.txt`, `uvicorn app.main:app --host 0.0.0.0 --port 8200`).
  - `infra/compose.deploy.yml` 에 ai 서비스 추가(production `127.0.0.1:8200:8200` / staging `127.0.0.1:8201:8200`).
  - `remote-deploy.sh` / `.gitlab-ci.yml` 에 ai 도 일괄 확장(`pull api web ai`, `rm -sf api web ai`, `up -d ... ai`, `ai_image` job).
  - 후속 ADR(잠정 ADR-015) 로 `ai_change_detect` 정책과 함께 결정 기록.

---

## 6. DB 외부 개방의 IP 화이트리스트 및 비정상 접속 모니터링

### 맥락
- ADR-011 로 postgres/redis 를 production 포함 외부 포트로 개방했다. 비밀번호(OPS-011) 가 1차 방어선.
- 완화책으로 "SSAFY 보안그룹 IP 화이트리스트 + 비정상 접속 모니터링" 이 거론됐으나 이번 MR 범위 밖으로 두었다.

### 대안
1. SSAFY 측에 **DB 포트(8300/8301/8400/8401) 한정 IP 화이트리스트** 허용을 요청 — 팀원 개인 IP 목록 관리 필요.
2. `fail2ban` 또는 `pg_hba.conf` 수준 접근 제한 추가.
3. GitLab 이나 외부 툴 (e.g. Grafana, Loki) 로 postgres 로그 기반 비정상 접속 알람.

### 유예 이유
- 1번은 SSAFY 측 정책 확인이 필요하고, 2/3번은 postgres 컨테이너 내부 설정이나 모니터링 파이프라인 구축이 필요해 포트 재설계 범위를 넘는다.
- 우선 비밀번호 정책(OPS-011) 만으로 최소 방어선을 확보하고 운영하면서 위험 수준을 관찰한다.

### 다음 트리거
- SSAFY 가 보안그룹 IP 제한을 허용한다는 답변을 받은 시점.
- 또는 실제 비정상 접속 시도가 로그상 관측되기 시작한 시점.

---

## 7. TLS 인증서 만료 전 알림 체계

### 맥락
- ADR-012 는 certbot systemd timer 로 자동 갱신이 돌 것을 가정한다.
- 다만 Let's Encrypt 서비스 장애, DNS 문제, Nginx 설정 drift 등으로 갱신이 실패할 가능성은 남아 있다. 만료 직전에 깨닫게 되면 service down.

### 대안
1. `certbot certificates` 결과를 하루 1회 cron 으로 파싱, 만료 D-14 이내면 Slack/Discord/이메일 알림.
2. 외부 모니터링 (e.g. UptimeRobot, Better Uptime) 의 SSL expiry check 기능.
3. GitLab scheduled pipeline 으로 `curl --resolve` + `openssl s_client` 로 만료일 점검.

### 유예 이유
- 인증서 초기 발급(항목 4) 이 먼저 성공해야 의미가 있다.
- 팀의 알림 채널 (Slack? Discord? 이메일?) 이 어디인지 아직 확정되지 않았다.

### 다음 트리거
- 인증서 초기 발급 완료 + 팀 알림 채널 확정 시점에 별도 MR 로 알림 스크립트(옵션 1) 추가.
