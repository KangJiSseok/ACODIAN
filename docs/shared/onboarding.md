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

<스텁 — 추후 ai 표준가이드(`docs/AX-WMS_FastAPI_표준가이드.docx`) 참고>

```bash
# 예시 (미확정)
cd ai
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload
```

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
