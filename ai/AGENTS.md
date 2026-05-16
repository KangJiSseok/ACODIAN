# AI 모듈 협업 가이드

이 문서는 ai 모듈을 여러 사람이 동시에 작업할 때 충돌을 줄이기 위한 가이드다.
새 기능 PR을 시작하기 전에 반드시 한 번 읽고, 자기 기능 행을 아래 매트릭스에 추가한다.

## 처음 시작하는 분께 (셋업 / 테스트)

### 1. Python 3.12 와 venv 준비

이 프로젝트는 Python 3.12 를 기준으로 한다 (CI 가 ``python:3.12-slim`` 으로 검증).

가장 간단한 방법은 [`uv`](https://docs.astral.sh/uv/) 도구다 (sudo 권한이 필요 없다).

```bash
# uv 설치 (한 번만)
curl -LsSf https://astral.sh/uv/install.sh | sh
export PATH="$HOME/.local/bin:$PATH"

# Python 3.12 다운로드 + venv 생성 + 의존성 설치
cd ai
uv python install 3.12
uv venv --python 3.12 .venv
uv pip install -r requirements.txt pytest
```

### 2. 테스트 실행

```bash
cd ai
.venv/bin/pytest tests -q
```

성공하면 `N passed` 같은 메시지가 보인다. 실패하면 메시지에 따라 수정한다.

### 3. 에디터에서 타입 추론을 잘 받으려면

VS Code / PyCharm 모두 인터프리터를 ``ai/.venv/bin/python`` 으로 지정한다.
이렇게 해야 SQLAlchemy / FastAPI / pgvector 의 타입이 자동 인식된다.

### 4. 환경변수

``.env.example`` 을 참고해 ``ai/.env`` 를 만든다 (``.env`` 는 git 에 올리지 않는다).
가장 중요한 키:
- ``GEMINI_API_KEY`` — Gemini API 키
- ``PGVECTOR_DSN`` — PostgreSQL DSN. pgvector 확장이 활성화된 DB 를 가리켜야 한다.

### 5. Alembic (DB 마이그레이션) 명령

```bash
cd ai
.venv/bin/alembic heads             # 현재 head 리비전 확인
.venv/bin/alembic upgrade head      # 미적용 마이그레이션 모두 적용
.venv/bin/alembic revision -m "<설명>"  # 새 빈 리비전 파일 생성
```

처음에는 마이그레이션이 없어서 ``alembic heads`` 출력이 비어 있다 (정상).

### 6. 디렉토리 구조 한눈에

```
ai/
├── app/
│   ├── main.py             # FastAPI 앱 factory (create_app)
│   ├── config/settings.py  # Pydantic Settings (환경변수 단일 진입점)
│   ├── router/             # FastAPI 라우터 (HTTP 계약만, 비즈니스 로직은 service)
│   ├── model/              # 요청/응답 Pydantic 모델
│   ├── service/            # 오케스트레이션 (요약/태그/청킹/검색 등)
│   ├── chain/              # 모델 + 프롬프트 + 파서 조립 (LangChain 등)
│   ├── prompt/             # 프롬프트 문자열 정의
│   ├── client/             # 외부 시스템 클라이언트 (Gemini 등)
│   ├── store/              # DB 접근 계층 (SQLAlchemy 모델/쿼리)
│   └── task/               # 비동기 태스크 (Celery)
├── alembic/                # 마이그레이션 스크립트
├── tests/                  # pytest
├── alembic.ini             # Alembic 설정
└── requirements.txt
```

각 디렉토리의 책임은 ``docs/ai/code-convention.yaml`` 과 ``docs/ai/adr.yaml`` 에 정의되어 있다.


## 파일 소유권 매트릭스

기능마다 자기 파일을 가진다. 같은 파일에 두 사람이 함수를 추가하지 않는다.

| 파일/디렉토리 | 소유 기능 |
|---|---|
| `app/prompt/chunking.py` | 임베딩·검색 |
| `app/prompt/tagging.py` | 태그 |
| `app/chain/chunking_chain.py` | 임베딩·검색 |
| `app/chain/tagging_chain.py` | 태그 |
| `app/service/chunking_service.py` | 임베딩·검색 |
| `app/service/embedding_service.py` | 임베딩·검색 |
| `app/service/search_service.py` | 임베딩·검색 |
| `app/service/tagging_service.py` | 태그 |
| `app/router/embedding.py` | 임베딩·검색 |
| `app/router/search.py` | 임베딩·검색 |
| `app/router/tagging.py` | 태그 |
| `app/model/embedding.py` | 임베딩·검색 |
| `app/model/search.py` | 임베딩·검색 |
| `app/model/tagging.py` | 태그 |
| `app/task/embedding_tasks.py` | 임베딩·검색 |
| `app/task/tagging_tasks.py` | 태그 |
| `app/store/models.py` (Worklog/File Embedding) | 임베딩·검색 |
| `app/store/embedding_store.py` | 임베딩·검색 |
| `app/store/tag_store.py` (필요 시) | 태그 |
| `app/light/v3/**` | LightRAG v3 업무일지 index/search |

## 공유 파일과 수정 규칙

| 파일 | 규칙 |
|---|---|
| `app/client/gemini_client.py` | 단일 클라이언트(ADR-009). 메서드 시그니처 변경은 사전 합의 후 단독 PR. |
| `app/config/settings.py` | 새 키는 끝 줄에 append. 알파벳 정렬 강요하지 않는다. |
| `.env.example` | settings 추가와 같은 PR에서 동기화. |
| `app/router/__init__.py` | 자기 라우터 줄의 주석만 해제. 다른 줄은 건드리지 않는다. |
| `requirements.txt` | 새 의존성은 알파벳 정렬 위치를 유지. |
| `docs/ai/adr.yaml` | 추가는 항상 파일 끝 append. 중간 삽입 금지. 번호는 아래 표에서 예약된 것을 사용. |

## ADR 번호 예약

| 번호 | 주제 | 상태 |
|---|---|---|
| ADR-001 ~ ADR-006 | 기존 결정 | adopted/planned |
| ADR-007 | 임베딩 저장소 — ai 소유 + 단일 PG | adopted |
| ADR-008 | DB 접근 계층 — SQLAlchemy 2.0 ORM + Alembic | adopted |
| ADR-009 | Gemini 클라이언트 단일화 | adopted |
| ADR-010 | (태그 담당 예약) | reserved |
| ADR-011 | (태그 담당 예약) | reserved |

## Alembic 마이그레이션 규칙

- 한 사람이 마이그레이션 추가 시, 다른 사람은 그 머지 후 자기 마이그레이션을 작성한다.
- 동시에 만들면 리비전 트리가 갈라져 `alembic merge`가 필요해진다.
- 현재 방침: 임베딩 작업이 V001~V002 점유 예정. 태그 작업은 ai 측 마이그레이션이 필요 없을 것으로 가정하나, 필요 시 사전 합의 후 추가.
- 마이그레이션 명령: `cd ai && alembic upgrade head`

## 새 기능 PR 시 체크리스트

1. 위 파일 소유권 매트릭스에 자기 기능 행을 추가한다.
2. ADR을 추가하면 끝 줄 append + 위 번호 예약 표를 갱신한다.
3. settings 새 키 추가 시 `.env.example` 도 같이 갱신한다.
4. 라우터를 추가하면 `app/router/__init__.py` 의 자기 줄 주석을 해제한다.
5. 파일 소유권 매트릭스에 없는 공유 파일을 수정해야 하면, 사전 합의 후 단독 PR로 처리한다.
