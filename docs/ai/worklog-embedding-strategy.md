# Worklog Embedding Strategy

## 목적

이 문서는 `tb_worklog`를 어떤 문맥으로 chunking하고 `tb_worklog_embedding`에 저장했는지 기록한다.
후속 MR에서 시맨틱 검색 정합성을 비교할 때, 어떤 임베딩 방식이 어떤 결과를 냈는지 남기는 기준 문서로 사용한다.

현재 MR의 범위는 **기존 업무일지 임베딩 저장 기반 구축**이다.
검색 API와 검색 랭킹 개선은 다음 MR에서 다룬다.

## 현재 입력 데이터 관찰

조사 기준 DB: `ai/.env`의 `PGVECTOR_DSN` (`localhost:15432/postgres`)

- `tb_worklog`: 622건
- `tb_worklog_embedding`: 초기 0건
- `work_content` 길이
  - 중앙값 약 946자
  - p90 약 1,207자
  - 최대 약 1,570자
- 현재 `CHUNK_THRESHOLD_CHARS=3000`을 넘는 업무일지는 없다.
- `tb_worklog_dependency`: 589건
  - 팀 내부 순차 흐름이 대부분이다.
  - 교차 팀 흐름도 존재한다: `101 -> 106`, `102 -> 107`, `103 -> 108`, `104 -> 109`, `105 -> 110`.
- `tb_worklog_tag`: 현재 0건이다.

따라서 현재 더미 데이터는 대부분 `1 worklog = 1 chunk`가 된다.
다만 운영 데이터가 길어질 수 있으므로 문단/문장 경계 기반 분할 로직은 유지한다.

## 임베딩 텍스트 구성

임베딩 입력 chunk는 다음 안정 문맥을 포함한다.

```text
팀: {team_name}
작성자: {user_name}
작성자 역할: {team_role / title_name / position_name}
제목: {title}
선행 업무: {predecessor_title_1, predecessor_title_2, ...}
요청/지시: {request_content}
수행 내용: {work_content}
```

### 포함한 이유

- `팀`: 팀별 업무 색깔이 질의에 드러나는 경우가 많다.
- `작성자 역할`: 팀장/사업부장/본부장 관점 질의가 있어 의미 문맥으로 유효하다.
- `선행 업무`: GraphRAG 전 단계에서도 후속 TF 흐름을 검색 문맥으로 약하게 반영할 수 있다.
- `제목`, `요청/지시`, `수행 내용`: 업무일지의 핵심 의미 본문이다.

## 임베딩 텍스트에서 제외한 정보

다음 값은 임베딩 본문에 넣지 않는다.

- `status_code`
- `importance_code`
- `ai_processing_status`
- `completion_date`, `due_date` 같은 상태성/기간성 판단값

### 제외한 이유

상태와 일정은 상대적으로 자주 바뀐다.
이 값을 임베딩 본문에 넣으면 상태 변경 때마다 벡터를 다시 만들어야 하며,
오래된 상태 문맥이 검색에 남을 수 있다.

후속 검색 MR에서는 이런 값들을 최신 `tb_worklog`와 join해서 구조적 필터로 처리한다.
`tb_worklog_embedding.status_code`, `importance_code` 컬럼은 현재 저장하지 않고 `NULL`로 둔다.

## Chunking 방식

현재 구현: `app/service/chunking_service.py`

1. 안정 문맥 prefix를 만든다.
2. `request_content`, `work_content`로 본문을 만든다.
3. 전체 길이가 `settings.chunk_threshold_chars` 이하이면 단일 chunk로 저장한다.
4. 초과하면 문단 경계를 우선 보존하며 나눈다.
5. 문단 하나가 너무 길면 문장 경계를 우선 사용해 다시 나눈다.
6. 모든 chunk에는 같은 prefix를 붙여, 분할 후에도 검색 문맥이 사라지지 않게 한다.

현재 더미 데이터는 threshold를 넘지 않아 대부분 단일 chunk로 저장된다.

## Embedding 모델과 차원

현재 `.env` 기준:

- `EMBEDDING_MODEL=gemini-embedding-001`
- `EMBEDDING_DIM=768`

`tb_worklog_embedding.embedding`은 `vector(768)`이므로,
`GeminiClient.embed()`는 `output_dimensionality=settings.embedding_dim`을 명시한다.
모델 기본 출력 차원이 바뀌어도 DB vector 차원과 맞추기 위해서다.

## 저장 방식

현재 구현:

- `app/store/models.py`
- `app/store/embedding_store.py`
- `app/service/embedding_service.py`

단일 worklog 임베딩 흐름:

1. `tb_worklog`, `tb_team`, `tb_user`, `tb_user_team`, `tb_worklog_dependency`, `tb_worklog_tag`에서 원본 문맥 조회
2. `ChunkingService`로 chunk 생성
3. `GeminiClient.embed()`로 chunk별 768차원 vector 생성
4. 기존 `tb_worklog_embedding` rows 삭제
5. 새 chunk rows insert
6. service 계층에서 commit

같은 worklog를 재임베딩할 때 중복 chunk가 남지 않도록 delete 후 insert한다.

## 실행 방법

기존 업무일지 전체 임베딩:

```bash
cd /c/ssafy/S14P31S209/ai

./.venv/Scripts/python.exe -m app.util.embed_existing_worklogs \
  --confirm-real-data-export \
  --all
```

대상만 확인:

```bash
./.venv/Scripts/python.exe -m app.util.embed_existing_worklogs \
  --confirm-real-data-export \
  --all \
  --dry-run \
  --limit 3
```

이미 임베딩된 업무일지를 건너뛰기:

```bash
./.venv/Scripts/python.exe -m app.util.embed_existing_worklogs \
  --confirm-real-data-export \
  --all \
  --skip-embedded
```

특정 팀만 처리:

```bash
./.venv/Scripts/python.exe -m app.util.embed_existing_worklogs \
  --confirm-real-data-export \
  --team-id 109 \
  --limit 20
```

`--confirm-real-data-export`는 실제 업무일지 본문을 외부 Gemini API로 전송한다는 명시 승인 플래그다.

## 평가 기록

### 2026-05-06 / 방식 A

방식:

- 안정 문맥 prefix 포함
- 상태/중요도 제외
- 선행 업무 제목 최대 3개 포함
- 대부분 단일 chunk
- Gemini embedding output dimension 768 명시

저장 결과:

- `tb_worklog`: 622건
- `tb_worklog_embedding`: 622건
- `embedded_worklogs`: 622건

정합성 평가는 다음 semantic search MR에서 fixture 질의 세트와 함께 기록한다.

## 후속 비교 후보

후속 MR에서 아래 방식을 비교할 수 있다.

- 방식 B: 선행 업무 제목을 제외한 임베딩
- 방식 C: 선행 업무 제목과 후속 업무 제목을 함께 포함
- 방식 D: 작성자 역할 제거
- 방식 E: 상태/마감 문맥을 별도 가벼운 textual hint로 포함
- 방식 F: 긴 업무일지에 Gemini 기반 지능형 chunking 적용
