# web → API 라우팅과 환경 분기

## 1. 이 문서가 답하는 질문

- web JS 번들에는 API base URL 을 어떻게 박아야 하는가?
- staging / production 환경 분기는 어디에서 일어나는가?
- `NEXT_PUBLIC_API_BASE_URL` 을 GitLab CI/CD Variable 로 두면 환경 분기가 되는가? (결론: 안 됨)
- 단일 web 이미지로 양쪽 환경을 어떻게 동시에 서비스하는가?

## 2. 결론 요약

- web 의 axios `baseURL` 은 **상대경로 `/api`** 가 기본값. (`web/src/app/_common/service/axios.ts`)
- 환경 분기는 **EC2 nginx** 가 `listen` 포트로 갈라서 한다. (`infra/nginx/sites-available/axwms.conf`)
- `NEXT_PUBLIC_API_BASE_URL` 은 빌드 시점에 JS 번들에 박혀 모든 환경에 동일하게 분포되므로, 빌드 1회 + 배포 N회 구조에서는 **환경 분기 도구로 부적절**하다.
- 같은 web 이미지가 staging / production 양쪽에서 그대로 동작하는 이유: 사용자 브라우저의 **현재 origin** 이 곧 환경 분기 정보이고, 상대경로 `/api` 는 그 origin 을 자동으로 prefix 받기 때문.

## 3. 주요 오해 정정

### 오해 1 — "web 안의 `localhost:8100` 으로 API 를 부르면 된다"

각 컨테이너는 독립된 network namespace 를 가진다.

- web 컨테이너 안의 `localhost` = web 자기 자신 (Node 서버). API 에 못 닿는다.
- api 컨테이너 안의 `localhost` = api 자기 자신.
- EC2 호스트의 `127.0.0.1:8100` (prod) / `127.0.0.1:8101` (staging) 만이 호스트 입장에서 api 컨테이너로 닿는 주소이고, 이 경로를 사용하는 주체는 **nginx** 다. 컨테이너 내부 코드가 호스트 localhost 를 보지 못한다.

만약 web 컨테이너 안의 server-side 코드(SSR / route handler)에서 api 를 직접 부르고 싶다면, docker compose network 의 service name 을 써야 한다 — 예: `http://api:8100`. 이건 `NEXT_PUBLIC_*` 가 아니라 server 전용 환경변수로 두어야 한다 (브라우저에 새지 않게). 현재 코드에는 그런 호출 경로가 없으므로 일단 도입하지 않는다.

### 오해 2 — "`NEXT_PUBLIC_API_BASE_URL` 을 staging/production GitLab Variable 로 분리하면 된다"

GitLab Variable 의 environment scope 는 **environment 가 정의된 job 에만** 적용된다. `web_image` 단계는 environment 에 속하지 않으며, 단 한 번 빌드해 양쪽으로 배포하므로 environment scope 자체가 의미를 갖지 못한다. 그리고 `NEXT_PUBLIC_*` 값은 Next.js 가 빌드 시점에 클라이언트 번들에 정적으로 박으므로, 이미지 안에 한 가지 값만 들어간다. → 이 조합으로는 환경 분기가 불가능.

만약 굳이 빌드 시점 분기로 가려면 `web_image_staging` / `web_image_prod` 두 job 으로 빌드를 나눠야 하는데, 캐시·빌드시간·이미지 관리 비용이 두 배가 된다. 본 프로젝트는 그 대신 **런타임/네트워크 계층 분기**(nginx) 를 택한다.

### 오해 3 — "axios `baseURL` 이 `/api` 면 `http://api` 같은 호스트로 가는 것 아닌가"

`/api` 는 path-only 상대경로다. scheme 도 host 도 없다. 브라우저 fetch 스펙이 "현재 document 의 origin" 을 자동 prefix 한다. 이걸 `http://api` 라고 쓰면 host 가 `api` 인 절대 URL 이 되어 사용자 PC 의 DNS 가 그 호스트를 찾으려 들어 실패한다. 두 표기는 의미가 완전히 다르다.

| 값 | URL 종류 | 브라우저 처리 |
|---|---|---|
| `"http://api/auth/login"` | scheme+host 있는 절대 URL | DNS 로 `api` 호스트 조회 → 실패 |
| `"/api/auth/login"` | scheme/host 없는 path-only 상대경로 | 현재 document origin 을 자동 prefix |

## 4. 실제 동작 흐름

### 4.1 코드 단계

`web/src/app/_common/service/axios.ts` (요지):

```ts
const DEFAULT_API_BASE_URL = "/api";
const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL?.trim() || DEFAULT_API_BASE_URL;

export const api = axios.create({
  baseURL: apiBaseUrl,
  withCredentials: true,
});
```

호출 측 (`web/src/app/_common/service/auth.ts`):

```ts
api.post("/auth/login", credentials, { skipAuthRefresh: true });
```

axios 가 합성하는 요청 URL:

```
"/api"  +  "/auth/login"  =  "/api/auth/login"
```

이 시점까지 URL 에는 scheme 도 host 도 없다.

### 4.2 브라우저가 origin 을 채우는 단계

사용자가 어디에 접속해 있는지에 따라 fetch 가 만드는 절대 URL 이 달라진다.

| 사용자가 접속한 URL | fetch 가 발신하는 절대 URL |
|---|---|
| `https://k14s209.p.ssafy.io/login` (prod) | `https://k14s209.p.ssafy.io/api/auth/login` |
| `https://k14s209.p.ssafy.io:8443/login` (staging) | `https://k14s209.p.ssafy.io:8443/api/auth/login` |

### 4.3 nginx 분기 단계

`infra/nginx/sites-available/axwms.conf` (요지):

```nginx
upstream axwms_api_prod      { server 127.0.0.1:8100; }
upstream axwms_api_staging   { server 127.0.0.1:8101; }
upstream axwms_web_prod      { server 127.0.0.1:8000; }
upstream axwms_web_staging   { server 127.0.0.1:8001; }

server {                       # production
    listen 443 ssl http2;
    server_name k14s209.p.ssafy.io;
    location /api/ { proxy_pass http://axwms_api_prod; }
    location /     { proxy_pass http://axwms_web_prod; }
}
server {                       # staging
    listen 8443 ssl http2;
    server_name k14s209.p.ssafy.io;
    location /api/ { proxy_pass http://axwms_api_staging; }
    location /     { proxy_pass http://axwms_web_staging; }
}
```

분기 키:
- **listen 포트 (443 vs 8443)** → server 블록 선택 → 환경 결정
- **path prefix (`/api/` vs `/`)** → 같은 환경 안에서 api / web 결정

호스트 포트 (`127.0.0.1:8100` 등) 는 docker compose 가 `compose.deploy.yml` 의 `${API_HOST_PORT}:8100` / `${WEB_HOST_PORT}:8000` 형태로 환경별로 다르게 노출한다.

## 5. 호스트 / 컨테이너 / 외부 포트 매핑 표

| 계층 | production | staging | 누가 봄 |
|---|---|---|---|
| 사용자 브라우저 (외부) | `https://...` (443) | `https://...:8443` | 사용자 PC |
| EC2 nginx listen | 443 | 8443 | nginx |
| EC2 host port (web) | 8000 | 8001 | nginx → upstream |
| EC2 host port (api) | 8100 | 8101 | nginx → upstream |
| 컨테이너 내부 port (web) | 8000 | 8000 | 컨테이너 (격리) |
| 컨테이너 내부 port (api) | 8100 | 8100 | 컨테이너 (격리) |

컨테이너 내부 포트가 같아도 충돌 없는 이유는 각 컨테이너가 독립된 network namespace 라서다. 호스트 입장에서만 8000/8001, 8100/8101 로 다르게 노출되며 nginx 가 그 차이를 사용한다.

## 6. 환경별 적용 가이드

### 6.1 production / staging 배포 환경

- `NEXT_PUBLIC_API_BASE_URL` 을 **설정하지 않는다.** GitLab CI/CD Variable / EC2 `.env` 어디에도 두지 않는다.
- web 컨테이너 안에서 axios 가 `DEFAULT_API_BASE_URL = "/api"` 로 자동 동작한다.
- 환경 분기는 nginx 가 책임진다.

### 6.2 로컬 개발 환경

- web (`pnpm dev`, 보통 `:3000`) 가 다른 host 의 api (`:8100`) 를 직접 부르고 싶을 때만 `web/.env.local` 에 절대 URL 을 적는다:

  ```env
  NEXT_PUBLIC_API_BASE_URL=http://localhost:8100/api
  ```

- 로컬에서도 nginx 를 같이 띄워 path-based 라우팅을 흉내낼 거면 `NEXT_PUBLIC_API_BASE_URL` 을 비우면 된다.

### 6.3 신규 환경(예: QA, dev branch preview) 추가 시

다음 셋이 일관되게 +1 오프셋으로 늘어나야 한다:

- `infra/nginx/sites-available/axwms.conf` 에 server 블록 추가 (listen 포트 + upstream)
- `compose.deploy.yml` 의 `WEB_HOST_PORT` / `API_HOST_PORT` / `POSTGRES_HOST_PORT` / `REDIS_HOST_PORT` 값
- `.gitlab-ci.yml` 의 `deploy_xxx` job + environment

web 코드는 변경하지 않는다.

## 7. 미래 변경 시 주의사항

- `axios.ts` 의 `DEFAULT_API_BASE_URL` 을 절대 URL 로 바꾸면 단일 이미지 + 환경 분기 구조가 깨진다. 변경 전에 이 문서를 업데이트하고 ADR 추가를 검토할 것.
- nginx conf 의 `location /api/` 와 `location /ai/` prefix 를 바꾸면 web 이 가리키는 path 도 같이 바꿔야 한다.
- web 의 server-side(SSR / route handler) 에서 API 를 호출하게 되면, 그 경로용 별도 변수 (예: `INTERNAL_API_BASE_URL=http://api:8100`) 를 도입한다. `NEXT_PUBLIC_*` 으로 두지 말 것 — 브라우저 번들에 새서 사용자 PC 의 DNS 로 `api` 호스트를 찾으려 든다.

## 8. 참고

- `infra/nginx/sites-available/axwms.conf` — 실제 분기 정의
- `infra/compose.deploy.yml` — 호스트 포트 매핑
- `web/src/app/_common/service/axios.ts` — `/api` 기본값 적용 위치
- `web/.env.example` — 로컬에서 절대 URL 덮어쓰기 사용법
- `docs/infra/ec2-gitlab-cicd-guide.md` — 배포 파이프라인 전반
- ADR-010 (외부 진입 포트 / path-based 라우팅), ADR-014 (web 자동배포 편입), ADR-015 (nginx conf 저장소 편입)
