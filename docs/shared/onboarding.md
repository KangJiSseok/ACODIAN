# AX-WMS 신규 팀원 온보딩

모노레포 4개 영역(api/web/ai/nginx)을 처음 셋업하는 절차.

## 선행 조건

- Git, Docker
- JDK / Node / Python 버전은 각 영역별 README 참조
- Docker Desktop (또는 Docker Engine) 실행 중 — api의 `jooqGenerate`가 PostgreSQL 컨테이너를 bootstrap 함

## 영역별 셋업 순서

### 1. 저장소 복제

```bash
git clone <repo-url> S14P31S209
cd S14P31S209
```

<스텁 — 브랜치 전략, 서브모듈 유무 등 추후 보완>

### 2. api (Spring Boot)

- 기준 문서: 루트 `AGENTS.md`
- ADR/컨벤션: `docs/api/adr.yaml`, `docs/api/code-convention.yaml`

**일반 개발 루프 (스키마 변경 없는 경우)**

```bash
cd api
./gradlew bootRun
```

> 표준 build entrypoint(`bootRun`, `test`, `assemble`, `build`)는 generated source가 없거나 stale 하면 `jooqGenerate`를 자동으로 선행한다.

**스키마 변경자 루프 (Flyway migration 수정 후)**

```bash
cd api
./gradlew jooqGenerate   # Docker PostgreSQL bootstrap → codegen
./gradlew bootRun
```

> jooqGenerate 실패 시 `docs/api/jooq-codegen-policy.md` 참조.

### 3. web (Next.js)

<스텁 — 추후 web 표준가이드(`docs/AX-WMS_NextJS_표준가이드.docx`) 참고>

```bash
# 예시 (미확정)
cd web
npm install
npm run dev
```

### 4. ai (FastAPI)

- 기준 문서: 루트 `AGENTS.md`, **`ai/AGENTS.md`** ("처음 시작하는 분께" 섹션에 셋업/테스트 절차 정리됨)
- ADR/컨벤션: `docs/ai/adr.yaml`, `docs/ai/code-convention.yaml`
- 표준 가이드: `docs/AX-WMS_FastAPI_표준가이드.docx` (디렉토리 구조와 prompt/chain 분리의 근거)

**셋업 (uv 기반, sudo 불필요)**

```bash
# uv 설치 (한 번만)
curl -LsSf https://astral.sh/uv/install.sh | sh
export PATH="$HOME/.local/bin:$PATH"

# Python 3.12 + venv + 의존성
cd ai
uv python install 3.12
uv venv --python 3.12 .venv
uv pip install -r requirements.txt pytest

# 환경변수
cp .env.example .env       # GEMINI_API_KEY 등 채워 넣는다
```

**개발 서버 실행**

```bash
cd ai
.venv/bin/uvicorn app.main:app --reload --port 8000
```

**테스트**

```bash
cd ai
.venv/bin/pytest tests -q
```

**기본 확인**

```bash
curl http://localhost:8000/ai/health      # {"status":"ok"}
# 비-운영 환경에서는 다음 문서 페이지도 노출된다:
#   http://localhost:8000/ai/docs         (Swagger UI)
#   http://localhost:8000/ai/redoc        (ReDoc)
#   http://localhost:8000/ai/openapi.json
```

**Alembic (DB 마이그레이션)**

```bash
cd ai
.venv/bin/alembic heads             # 현재 head 리비전 확인
.venv/bin/alembic upgrade head      # 미적용 마이그레이션 모두 적용
.venv/bin/alembic revision -m "<설명>"
```

> CI(`ai_ci` 잡)는 `python:3.12-slim` 이미지에서 같은 명령으로 검증한다.

### 5. nginx

<스텁 — 추후 nginx 표준가이드(`docs/AX-WMS_Nginx_표준가이드.docx`) 참고>

```bash
# 예시 (미확정)
cd nginx
docker compose up
```

## 통합 확인

- [ ] api `bootRun` 성공 (기본 포트 8080)
- [ ] web dev 서버 성공 (기본 포트 3000)
- [ ] ai 서버 성공 (기본 포트 8000)
- [ ] nginx 라우팅 성공 (각 경로가 올바른 서비스로 프록시되는지 확인)

## 자주 부딪히는 문제

- **jooqGenerate 실패**: Docker 실행 여부 확인 → `docs/api/jooq-codegen-policy.md` 참조
- **api 빌드 실패 (generated source 없음/stale)**: 먼저 표준 build entrypoint를 다시 실행하고, codegen만 빠르게 확인해야 하면 `./gradlew jooqGenerate`로 원인을 좁힌다
- (추후 추가)

## 참고 문서

| 문서 | 경로 |
|---|---|
| 아키텍처 가이드 | `docs/AX-WMS_기획서_아키텍처가이드.md` |
| ERD 초안 | `docs/ax-wms-erd-draft.sql` |
| jOOQ codegen 정책 | `docs/api/jooq-codegen-policy.md` |
| 도메인 용어 사전 | `docs/shared/glossary.md` |
